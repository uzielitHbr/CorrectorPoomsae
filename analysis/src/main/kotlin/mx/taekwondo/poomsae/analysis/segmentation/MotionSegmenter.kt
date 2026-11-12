package mx.taekwondo.poomsae.analysis.segmentation

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.geometry.Vec3
import mx.taekwondo.poomsae.analysis.model.BodyProfile
import mx.taekwondo.poomsae.analysis.model.CoordinateSpace
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.poomsae.EvaluationMode
import mx.taekwondo.poomsae.analysis.poomsae.Movement
import mx.taekwondo.poomsae.analysis.poomsae.PoomsaeDefinition
import mx.taekwondo.poomsae.analysis.poomsae.Side

/** Contexto para la verificación de omisión de movimientos. */
internal data class OmissionContext(
    val frame: PoseFrame,
    val previousFrame: PoseFrame?,
    val currentMovement: Movement,
    val currentSpeed: Double,
    val definition: PoomsaeDefinition,
    val profileTorso: Double?,
)

/** Estado interno de monitoreo de pérdida y recuperación de tracking. */
internal data class TrackingState(
    val isLost: Boolean = false,
    val lostSinceMs: Long? = null,
    val recoveredSinceMs: Long? = null,
)

/**
 * Segmentador temporal de movimientos y máquina de estados de ejecución (Sección 12.4).
 *
 * Monitorea las velocidades articulares normalizadas de las articulaciones activas de cada movimiento:
 * - AwaitingMovement -> InMovement: cuando v_active >= V_START durante >= T_START_MS (con lockout).
 * - InMovement -> Holding: cuando v_active < V_STILL durante >= T_HOLD_MS.
 * - Para patadas (PEAK_THEN_HOLD): rastrea el cuadro de máxima elevación del tobillo pateador.
 * - Detección de omisión: cuando las articulaciones del siguiente movimiento se activan antes que el actual.
 * - Pérdida y recuperación de tracking temporal.
 */
class MotionSegmenter(
    private val definition: PoomsaeDefinition,
    private var bodyProfile: BodyProfile? = null,
) {
    var currentMovementIndex: Int = 1
        private set

    var subState: MovementSubState = MovementSubState.AwaitingMovement(1, 0L)
        private set

    private var previousFrame: PoseFrame? = null
    private var lockoutUntilMs: Long = 0L

    // Temporizadores de estado AwaitingMovement
    private var speedAboveStartSinceMs: Long? = null
    private var nextSpeedAboveStartSinceMs: Long? = null

    // Temporizadores y buffers de InMovement
    private var speedBelowStillSinceMs: Long? = null
    private val holdFramesBuffer = mutableListOf<PoseFrame>()
    private var peakFrame: PoseFrame? = null
    private var peakElevation: Double = Double.NEGATIVE_INFINITY

    // Seguimiento de pérdida de tracking
    private var trackingState = TrackingState()

    fun setBodyProfile(profile: BodyProfile) {
        this.bodyProfile = profile
    }

    fun startMovement(
        movementIndex: Int,
        timestampMs: Long,
        applyLockout: Boolean = true,
    ) {
        currentMovementIndex = movementIndex.coerceIn(1, definition.movements.size)
        subState = MovementSubState.AwaitingMovement(currentMovementIndex, timestampMs)
        lockoutUntilMs = if (applyLockout) timestampMs + AnalysisConfig.T_LOCKOUT_MS else 0L
        resetMovementTracking()
    }

    fun repeatCurrent(timestampMs: Long) {
        startMovement(currentMovementIndex, timestampMs, applyLockout = false)
    }

    fun skipCurrent(timestampMs: Long) {
        val nextIndex = currentMovementIndex + 1
        if (nextIndex <= definition.movements.size) {
            startMovement(nextIndex, timestampMs, applyLockout = false)
        }
    }

    fun reset(initialMovementIndex: Int = 1) {
        currentMovementIndex = initialMovementIndex.coerceIn(1, definition.movements.size)
        subState = MovementSubState.AwaitingMovement(currentMovementIndex, 0L)
        previousFrame = null
        lockoutUntilMs = 0L
        trackingState = TrackingState()
        resetMovementTracking()
    }

    /** Procesa un cuadro de pose entrante y retorna la lista de eventos detectados. */
    fun processFrame(frame: PoseFrame): List<SegmenterEvent> {
        val events = mutableListOf<SegmenterEvent>()
        trackingState = MotionSegmenterHelpers.checkTrackingState(frame, trackingState, events)

        val movement = definition.movements.getOrNull(currentMovementIndex - 1)
        if (movement != null) {
            val profileTorso = bodyProfile?.tImgMedian
            val activeSpeed =
                JointSpeedCalculator.computeActiveSpeed(
                    currentFrame = frame,
                    previousFrame = previousFrame,
                    activeJoints = movement.activeJoints,
                    profileTorsoHeight = profileTorso,
                )

            when (subState) {
                is MovementSubState.AwaitingMovement ->
                    handleAwaitingMovement(frame, movement, activeSpeed, events)
                is MovementSubState.InMovement ->
                    handleInMovement(frame, movement, activeSpeed, events)
                is MovementSubState.Holding,
                is MovementSubState.Evaluated,
                -> Unit
            }
        }

        previousFrame = frame
        return events
    }

    private fun handleAwaitingMovement(
        frame: PoseFrame,
        movement: Movement,
        activeSpeed: Double,
        events: MutableList<SegmenterEvent>,
    ) {
        val timestamp = frame.timestampMs
        val awaitingState = subState as MovementSubState.AwaitingMovement

        if (timestamp - awaitingState.awaitingSinceMs >= AnalysisConfig.T_MOVEMENT_TIMEOUT_MS) {
            events.add(SegmenterEvent.TimedOut(movement.index, timestamp - awaitingState.awaitingSinceMs))
        }

        val omissionCtx =
            OmissionContext(
                frame = frame,
                previousFrame = previousFrame,
                currentMovement = movement,
                currentSpeed = activeSpeed,
                definition = definition,
                profileTorso = bodyProfile?.tImgMedian,
            )
        nextSpeedAboveStartSinceMs =
            MotionSegmenterHelpers.checkOmission(omissionCtx, nextSpeedAboveStartSinceMs, events)

        if (timestamp < lockoutUntilMs) {
            speedAboveStartSinceMs = null
            return
        }

        if (activeSpeed >= AnalysisConfig.V_START) {
            val since = speedAboveStartSinceMs ?: timestamp
            speedAboveStartSinceMs = since
            if (timestamp - since >= AnalysisConfig.T_START_MS) {
                subState = MovementSubState.InMovement(movement.index, since)
                events.add(SegmenterEvent.MovementStarted(movement.index, since))
                resetMovementTracking()
                checkKickPeak(frame, movement)
            }
        } else {
            speedAboveStartSinceMs = null
        }
    }

    private fun handleInMovement(
        frame: PoseFrame,
        movement: Movement,
        activeSpeed: Double,
        events: MutableList<SegmenterEvent>,
    ) {
        val timestamp = frame.timestampMs
        val inMovementState = subState as MovementSubState.InMovement
        checkKickPeak(frame, movement)

        if (activeSpeed < AnalysisConfig.V_STILL) {
            val since = speedBelowStillSinceMs ?: timestamp
            speedBelowStillSinceMs = since
            holdFramesBuffer.add(frame)

            if (timestamp - since >= AnalysisConfig.T_HOLD_MS) {
                val window = MotionSegmenterHelpers.extractEvaluationWindow(holdFramesBuffer, timestamp)
                val peak = peakFrame
                subState = MovementSubState.Holding(movement.index, since, window)
                events.add(SegmenterEvent.MovementHeld(movement.index, window, peak))
            }
        } else {
            speedBelowStillSinceMs = null
            if (holdFramesBuffer.size > BUFFER_TAIL_SIZE) {
                holdFramesBuffer.subList(0, holdFramesBuffer.size - BUFFER_TAIL_SIZE).clear()
            }
        }

        if (timestamp - inMovementState.startedAtMs >= AnalysisConfig.T_MOVEMENT_TIMEOUT_MS) {
            events.add(SegmenterEvent.TimedOut(movement.index, timestamp - inMovementState.startedAtMs))
        }
    }

    private fun checkKickPeak(
        frame: PoseFrame,
        movement: Movement,
    ) {
        if (movement.evaluationMode != EvaluationMode.PEAK_THEN_HOLD) return

        val kickingLeg = movement.technique.arm
        val elevation = MotionSegmenterHelpers.computeAnkleElevation(frame, kickingLeg, bodyProfile)
        if (elevation > peakElevation || peakFrame == null) {
            peakElevation = elevation
            peakFrame = frame
        }
    }

    private fun resetMovementTracking() {
        speedAboveStartSinceMs = null
        nextSpeedAboveStartSinceMs = null
        speedBelowStillSinceMs = null
        holdFramesBuffer.clear()
        peakFrame = null
        peakElevation = Double.NEGATIVE_INFINITY
    }

    private companion object {
        private const val BUFFER_TAIL_SIZE = 5
    }
}

private object MotionSegmenterHelpers {
    fun computeAnkleElevation(
        frame: PoseFrame,
        kickingLeg: Side,
        profile: BodyProfile?,
    ): Double {
        val kickingAnkle = if (kickingLeg == Side.LEFT) Joint.LEFT_ANKLE else Joint.RIGHT_ANKLE
        val kickingHip = if (kickingLeg == Side.LEFT) Joint.LEFT_HIP else Joint.RIGHT_HIP

        val world = frame.landmarks(CoordinateSpace.WORLD)
        val aWorld = world[kickingAnkle]
        val hWorld = world[kickingHip]
        return if (aWorld != null && hWorld != null) {
            val up = profile?.upVector ?: Vec3(0.0, 1.0, 0.0)
            (aWorld.toVec3() - hWorld.toVec3()).dot(up)
        } else {
            val img = frame.landmarks(CoordinateSpace.IMAGE)
            val aImg = img[kickingAnkle]
            val hImg = img[kickingHip]
            if (aImg != null && hImg != null) (hImg.y - aImg.y).toDouble() else 0.0
        }
    }

    fun isFrameValidForTracking(frame: PoseFrame): Boolean {
        val img = frame.landmarks(CoordinateSpace.IMAGE)
        if (img.isEmpty()) return false

        val keyJoints = listOf(Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER, Joint.LEFT_HIP, Joint.RIGHT_HIP)
        return keyJoints.all { joint ->
            val lm = img[joint]
            lm != null && lm.visibility >= AnalysisConfig.VIS_MIN
        }
    }

    fun extractEvaluationWindow(
        frames: List<PoseFrame>,
        currentTimestamp: Long,
    ): List<PoseFrame> {
        val cutoffMs = currentTimestamp - AnalysisConfig.T_EVAL_WINDOW_MS
        val window = frames.filter { it.timestampMs >= cutoffMs }
        return if (window.size >= AnalysisConfig.MIN_WINDOW_FRAMES) {
            window
        } else {
            frames.takeLast(AnalysisConfig.MIN_WINDOW_FRAMES.coerceAtMost(frames.size))
        }
    }

    fun checkOmission(
        ctx: OmissionContext,
        nextSpeedSinceMs: Long?,
        events: MutableList<SegmenterEvent>,
    ): Long? {
        val nextIndex = ctx.currentMovement.index + 1
        val nextMovement = ctx.definition.movements.getOrNull(nextIndex - 1) ?: return null
        val nextSpeed =
            JointSpeedCalculator.computeActiveSpeed(
                currentFrame = ctx.frame,
                previousFrame = ctx.previousFrame,
                activeJoints = nextMovement.activeJoints,
                profileTorsoHeight = ctx.profileTorso,
            )
        val timestamp = ctx.frame.timestampMs
        return if (ctx.currentSpeed < AnalysisConfig.V_STILL && nextSpeed >= AnalysisConfig.V_START) {
            val since = nextSpeedSinceMs ?: timestamp
            if (timestamp - since >= AnalysisConfig.T_START_MS) {
                events.add(SegmenterEvent.OmissionSuspected(ctx.currentMovement.index, nextIndex))
                null
            } else {
                since
            }
        } else {
            null
        }
    }

    fun checkTrackingState(
        frame: PoseFrame,
        state: TrackingState,
        events: MutableList<SegmenterEvent>,
    ): TrackingState {
        val isValid = isFrameValidForTracking(frame)
        val timestamp = frame.timestampMs

        return if (!isValid) {
            val lostSince = state.lostSinceMs ?: timestamp
            if (timestamp - lostSince >= AnalysisConfig.T_LOSS_SHORT_MS && !state.isLost) {
                events.add(SegmenterEvent.TrackingLost)
                TrackingState(isLost = true, lostSinceMs = lostSince, recoveredSinceMs = null)
            } else {
                state.copy(lostSinceMs = lostSince, recoveredSinceMs = null)
            }
        } else {
            if (state.isLost) {
                val recSince = state.recoveredSinceMs ?: timestamp
                if (timestamp - recSince >= AnalysisConfig.T_RECOVER_MS) {
                    events.add(SegmenterEvent.TrackingRecovered)
                    TrackingState(isLost = false, lostSinceMs = null, recoveredSinceMs = null)
                } else {
                    state.copy(recoveredSinceMs = recSince)
                }
            } else {
                TrackingState(isLost = false, lostSinceMs = null, recoveredSinceMs = null)
            }
        }
    }
}
