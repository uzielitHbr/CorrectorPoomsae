package mx.taekwondo.poomsae.analysis.segmentation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.taekwondo.poomsae.analysis.evaluation.EvaluationOptions
import mx.taekwondo.poomsae.analysis.evaluation.MovementEvaluation
import mx.taekwondo.poomsae.analysis.evaluation.MovementEvaluator
import mx.taekwondo.poomsae.analysis.evaluation.MovementOutcome
import mx.taekwondo.poomsae.analysis.geometry.AngleGeometry
import mx.taekwondo.poomsae.analysis.geometry.BodyPrimitives
import mx.taekwondo.poomsae.analysis.geometry.GeometryResult
import mx.taekwondo.poomsae.analysis.geometry.OrientationBasis
import mx.taekwondo.poomsae.analysis.model.BodyProfile
import mx.taekwondo.poomsae.analysis.model.MetricResult
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.poomsae.PoomsaeDefinition
import mx.taekwondo.poomsae.analysis.quality.BodyProfileCalibrator
import mx.taekwondo.poomsae.analysis.quality.CalibrationResult
import mx.taekwondo.poomsae.analysis.quality.FrameQualityGate
import mx.taekwondo.poomsae.analysis.quality.FrameQualityResult
import mx.taekwondo.poomsae.analysis.signal.SignalStats

/**
 * Motor integral de sesión de práctica y evaluador en tiempo real (Sección 12.4 y 12.9.2).
 *
 * Coordina la secuencia completa de la forma Taegeuk:
 * 1. Encuadre (Framing) y Calibración antropométrica (Calibrating).
 * 2. Posición lista (Ready) y avance de movimientos 1 a 18 (Running).
 * 3. Detección de sostén (Holding), picos en patadas y cálculo del rumbo acumulado para giros.
 * 4. Recuperación de errores (pausa, repetición, omisión y reconexión por pérdida de tracking).
 * 5. Consolidación de evaluaciones y puntuación global de sesión (Finished).
 */
class SessionEngine(
    val definition: PoomsaeDefinition,
    initialProfile: BodyProfile? = null,
    initialState: SessionState = SessionState.Idle,
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    var bodyProfile: BodyProfile? = initialProfile
        private set

    private val segmenter = MotionSegmenter(definition, initialProfile)
    private val evaluations = mutableListOf<MovementEvaluation>()
    private val attemptsPerMovement = mutableMapOf<Int, Int>()
    private val calibrationFramesBuffer = mutableListOf<PoseFrame>()
    private var lastKnownHeading: Double? = 0.0

    init {
        if (initialState is SessionState.Running) {
            segmenter.startMovement(initialState.movementIndex, 0L, applyLockout = false)
        }
    }

    /** Procesa comandos de control de la sesión (Sección 12.9.2). */
    fun dispatch(command: SessionCommand) {
        when (command) {
            is SessionCommand.Start -> {
                val current = _state.value
                if (current is SessionState.Idle || current is SessionState.Ready) {
                    if (bodyProfile != null) startRunning(1, 0L) else updateState(SessionState.Framing)
                }
            }
            is SessionCommand.Pause -> {
                val current = _state.value
                if (current is SessionState.Running) updateState(SessionState.Paused(PauseReason.USER, current))
            }
            is SessionCommand.Resume -> {
                val current = _state.value
                if (current is SessionState.Paused) updateState(current.previousState)
            }
            is SessionCommand.RepeatCurrent -> handleRepeat()
            is SessionCommand.SkipCurrent -> handleSkip()
            is SessionCommand.RestartForm -> {
                evaluations.clear()
                attemptsPerMovement.clear()
                calibrationFramesBuffer.clear()
                lastKnownHeading = 0.0
                segmenter.reset(1)
                startRunning(1, 0L)
            }
            is SessionCommand.Finish -> finishSession()
            is SessionCommand.Discard -> updateState(SessionState.Aborted)
        }
    }

    /** Procesa un cuadro de pose entrante del flujo de análisis. */
    fun processFrame(frame: PoseFrame) {
        when (val currentState = _state.value) {
            is SessionState.Framing -> {
                if (SessionEngineHelpers.processFraming(frame, _events)) {
                    calibrationFramesBuffer.clear()
                    updateState(SessionState.Calibrating)
                }
            }
            is SessionState.Calibrating -> {
                val profile = SessionEngineHelpers.processCalibrating(frame, calibrationFramesBuffer)
                if (profile != null) {
                    bodyProfile = profile
                    segmenter.setBodyProfile(profile)
                    lastKnownHeading = 0.0
                    updateState(SessionState.Ready(countdownSeconds = 3))
                }
            }
            is SessionState.Ready -> startRunning(1, frame.timestampMs)
            is SessionState.Running -> processRunning(frame, currentState)
            is SessionState.Paused -> {
                if (currentState.reason == PauseReason.LOST_TRACKING) {
                    val events = segmenter.processFrame(frame)
                    if (events.any { it is SegmenterEvent.TrackingRecovered }) {
                        updateState(currentState.previousState)
                        _events.tryEmit(SessionEvent.GuidanceMessageEmitted("TRACKING_RECOVERED"))
                    }
                }
            }
            is SessionState.Idle,
            is SessionState.PermissionRequired,
            is SessionState.Finished,
            is SessionState.Aborted,
            -> Unit
        }
    }

    private fun handleRepeat() {
        val current = _state.value
        if (current is SessionState.Running) {
            val idx = current.movementIndex
            attemptsPerMovement[idx] = (attemptsPerMovement[idx] ?: 1) + 1
            segmenter.repeatCurrent(0L)
            updateState(SessionState.Running(idx, segmenter.subState))
        }
    }

    private fun handleSkip() {
        val current = _state.value
        if (current is SessionState.Running) {
            val idx = current.movementIndex
            val omitted =
                MovementEvaluation(
                    index = idx,
                    outcome = MovementOutcome.OMITTED,
                    evaluability = null,
                    category = null,
                    score = null,
                    metrics = emptyMap(),
                    feedback = emptyList(),
                    attempts = attemptsPerMovement[idx] ?: 1,
                )
            evaluations.add(omitted)
            _events.tryEmit(SessionEvent.MovementEvaluated(idx, omitted))

            val nextIdx = idx + 1
            if (nextIdx <= definition.movements.size) {
                segmenter.skipCurrent(0L)
                updateState(SessionState.Running(nextIdx, segmenter.subState))
            } else {
                finishSession()
            }
        }
    }

    private fun processRunning(
        frame: PoseFrame,
        runningState: SessionState.Running,
    ) {
        val segmenterEvents = segmenter.processFrame(frame)
        if (runningState.subState != segmenter.subState) {
            updateState(SessionState.Running(segmenter.currentMovementIndex, segmenter.subState))
        }
        for (event in segmenterEvents) {
            when (event) {
                is SegmenterEvent.MovementStarted ->
                    updateState(SessionState.Running(event.n, segmenter.subState))
                is SegmenterEvent.MovementHeld ->
                    evaluateAndAdvance(event)
                is SegmenterEvent.TimedOut ->
                    _events.tryEmit(SessionEvent.GuidanceMessageEmitted("MOVEMENT_TIMEOUT"))
                is SegmenterEvent.OmissionSuspected ->
                    _events.tryEmit(SessionEvent.GuidanceMessageEmitted("OMISSION_SUSPECTED"))
                is SegmenterEvent.TrackingLost -> {
                    val current = _state.value
                    if (current is SessionState.Running) {
                        updateState(SessionState.Paused(PauseReason.LOST_TRACKING, current))
                        _events.tryEmit(SessionEvent.GuidanceMessageEmitted("TRACKING_LOST"))
                    }
                }
                is SegmenterEvent.TrackingRecovered,
                is SegmenterEvent.MovementPeakDetected,
                -> Unit
            }
        }
    }

    private fun evaluateAndAdvance(event: SegmenterEvent.MovementHeld) {
        val movement = definition.movements.firstOrNull { it.index == event.n } ?: return
        val profile = bodyProfile ?: return

        val turnAngle = SessionTurnHelper.computeMeasuredTurn(event.holdWindow, profile, lastKnownHeading)
        if (turnAngle != null) {
            val prev = lastKnownHeading ?: 0.0
            lastKnownHeading = AngleGeometry.wrapDegrees(prev + turnAngle)
        }

        val eval =
            MovementEvaluator.evaluate(
                frame = event.holdWindow.last(),
                movement = movement,
                profile = profile,
                options =
                    EvaluationOptions(
                        window = event.holdWindow,
                        measuredTurnDeg = turnAngle,
                        peakFrame = event.peakFrame,
                        attempts = attemptsPerMovement[event.n] ?: 1,
                    ),
            )

        evaluations.add(eval)
        updateState(SessionState.Running(event.n, MovementSubState.Evaluated(event.n, eval)))
        _events.tryEmit(SessionEvent.MovementEvaluated(event.n, eval))
        eval.feedback.forEach { _events.tryEmit(SessionEvent.FeedbackEmitted(it)) }

        val nextIndex = event.n + 1
        if (nextIndex <= definition.movements.size) {
            segmenter.startMovement(nextIndex, event.holdWindow.last().timestampMs)
            updateState(SessionState.Running(nextIndex, segmenter.subState))
        } else {
            finishSession()
        }
    }

    private fun startRunning(
        movementIndex: Int,
        timestampMs: Long,
    ) {
        segmenter.startMovement(movementIndex, timestampMs, applyLockout = false)
        updateState(SessionState.Running(movementIndex, segmenter.subState))
    }

    private fun finishSession() {
        val evaluableScores = evaluations.mapNotNull { it.score }
        val globalScore = if (evaluableScores.isNotEmpty()) evaluableScores.average() else null
        val finishedState = SessionState.Finished(evaluations.toList(), globalScore)
        updateState(finishedState)
        _events.tryEmit(SessionEvent.SessionFinished(evaluations.toList(), globalScore))
    }

    private fun updateState(newState: SessionState) {
        val oldState = _state.value
        _state.value = newState
        _events.tryEmit(SessionEvent.StateChanged(oldState, newState))
    }
}

private object SessionEngineHelpers {
    fun processFraming(
        frame: PoseFrame,
        events: MutableSharedFlow<SessionEvent>,
    ): Boolean {
        val result = FrameQualityGate.evaluate(frame)
        return if (result is FrameQualityResult.Rejected) {
            events.tryEmit(SessionEvent.GuidanceMessageEmitted(result.code.name))
            false
        } else {
            true
        }
    }

    fun processCalibrating(
        frame: PoseFrame,
        buffer: MutableList<PoseFrame>,
    ): BodyProfile? {
        buffer.add(frame)
        val calibResult = BodyProfileCalibrator.calibrate(buffer)
        return if (calibResult is CalibrationResult.Success) calibResult.profile else null
    }
}

private object SessionTurnHelper {
    fun computeMeasuredTurn(
        window: List<PoseFrame>,
        profile: BodyProfile,
        lastHeading: Double?,
    ): Double? {
        val basis =
            OrientationBasis(
                up = profile.upVector,
                forwardSign = profile.forwardSign,
                forwardZero = profile.referenceForward,
                rightZero = profile.referenceHipAxis,
            )

        val headings =
            window.mapNotNull { frame ->
                val res = BodyPrimitives.computeHeadingFromFrame(frame, basis)
                if (res is MetricResult.Value) res.value else null
            }

        val currentHeading = (SignalStats.median(headings) as? GeometryResult.Success)?.value
        return if (currentHeading != null) {
            val prev = lastHeading ?: 0.0
            AngleGeometry.wrapDegrees(currentHeading - prev)
        } else {
            null
        }
    }
}
