package tkd.apptkd.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.evaluation.Evaluability
import mx.taekwondo.poomsae.analysis.evaluation.MovementEvaluation
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.poomsae.Movement
import mx.taekwondo.poomsae.analysis.poomsae.PoomsaeDefinition
import mx.taekwondo.poomsae.analysis.quality.QualityRejectionCode
import mx.taekwondo.poomsae.analysis.segmentation.PauseReason
import mx.taekwondo.poomsae.analysis.segmentation.SessionCommand
import mx.taekwondo.poomsae.analysis.segmentation.SessionEngine
import mx.taekwondo.poomsae.analysis.segmentation.SessionEvent
import mx.taekwondo.poomsae.analysis.segmentation.SessionState
import tkd.apptkd.R
import tkd.apptkd.camera.CameraFacing
import tkd.apptkd.ui.FeedbackMessageMapper
import tkd.apptkd.ui.PracticeSessionHolder
import tkd.apptkd.ui.SessionSummaryData
import tkd.apptkd.ui.VoicePlayer
import tkd.apptkd.ui.components.BannerSeverity
import tkd.apptkd.ui.components.MovementTimelineSegmentState
import javax.inject.Inject

sealed interface PracticeStep {
    data class Framing(val messageResId: Int? = null) : PracticeStep
    data class Calibrating(val progressRatio: Float = 0f, val isRetrying: Boolean = false) : PracticeStep
    data class Ready(val countdownSeconds: Int = 3) : PracticeStep
    data class Running(
        val movementIndex: Int,
        val movementNameEs: String,
        val movementNameKo: String,
        val stanceName: String,
        val nextMovementName: String?,
        val turnDegrees: Int,
        val isKihap: Boolean,
    ) : PracticeStep
    data class Paused(val reason: PauseReason) : PracticeStep
    data class Finished(
        val evaluations: List<MovementEvaluation>,
        val globalScore: Double?,
    ) : PracticeStep
    data class Error(val messageResId: Int) : PracticeStep
}

data class PracticeUiState(
    val step: PracticeStep = PracticeStep.Framing(),
    val cameraFacing: CameraFacing = CameraFacing.FRONT,
    val isVoiceEnabled: Boolean = false,
    val timelineStates: Map<Int, MovementTimelineSegmentState> = emptyMap(),
    val showExitDialog: Boolean = false,
    val hasEvaluatedMovements: Boolean = false,
    val latestFrame: PoseFrame? = null,
    val feedbackResId: Int? = null,
    val feedbackSeverity: BannerSeverity = BannerSeverity.INFO,
    val fps: Double = 0.0,
    val droppedFrames: Long = 0L,
)

sealed interface PracticeAction {
    data class ProcessFrame(val frame: PoseFrame) : PracticeAction
    data object Start : PracticeAction
    data object Pause : PracticeAction
    data object Resume : PracticeAction
    data object RepeatCurrent : PracticeAction
    data object SkipCurrent : PracticeAction
    data object RestartForm : PracticeAction
    data object FinishSession : PracticeAction
    data object DiscardSession : PracticeAction
    data object SaveDraftAndExit : PracticeAction
    data object ToggleVoice : PracticeAction
    data object ToggleCameraFacing : PracticeAction
    data class SetExitDialogVisible(val visible: Boolean) : PracticeAction
    data class UpdateFpsMetrics(val fps: Double, val droppedFrames: Long) : PracticeAction
}

/**
 * ViewModel que orquesta la sesión de práctica en vivo conectando el SessionEngine a Compose (F9).
 */
@HiltViewModel
class PracticeViewModel(
    private val definition: PoomsaeDefinition,
    private val sessionHolder: PracticeSessionHolder,
    private val voicePlayer: VoicePlayer,
    internal val engine: SessionEngine,
) : ViewModel() {

    @Inject
    constructor(
        definition: PoomsaeDefinition,
        sessionHolder: PracticeSessionHolder,
        voicePlayer: VoicePlayer,
    ) : this(definition, sessionHolder, voicePlayer, SessionEngine(definition))

    private val _uiState = MutableStateFlow(
        PracticeUiState(
            step = PracticeViewModelHelpers.mapSessionStateToStep(engine.state.value, definition),
        ),
    )
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    private val snapshots = mutableMapOf<Int, PoseFrame>()
    private var feedbackDismissJob: Job? = null

    init {
        observeEngineState()
        observeEngineEvents()
    }

    fun onAction(action: PracticeAction) {
        if (!PracticeViewModelHandlers.handleSessionAction(action, engine, sessionHolder, _uiState)) {
            feedbackDismissJob = PracticeViewModelHandlers.handleUiAction(
                action = action,
                uiState = _uiState,
                currentDismissJob = feedbackDismissJob,
            )
        }
    }

    private fun observeEngineState() {
        viewModelScope.launch {
            engine.state.collect { sessionState ->
                val nextStep = PracticeViewModelHelpers.mapSessionStateToStep(sessionState, definition)
                _uiState.update { it.copy(step = nextStep) }
            }
        }
    }

    private fun observeEngineEvents() {
        viewModelScope.launch {
            engine.events.collect { event ->
                handleEngineEvent(event)
            }
        }
    }

    private fun handleEngineEvent(event: SessionEvent) {
        when (event) {
            is SessionEvent.MovementEvaluated -> {
                _uiState.value.latestFrame?.let { snapshots[event.n] = it }
                PracticeViewModelHandlers.onMovementEvaluated(event.n, event.evaluation, _uiState)
            }
            is SessionEvent.SessionFinished -> {
                PracticeViewModelHandlers.onSessionFinished(
                    evaluations = event.evaluations,
                    globalScore = event.globalScore,
                    context = SessionFinishContext(
                        definition = definition,
                        snapshots = snapshots,
                        sessionHolder = sessionHolder,
                        uiState = _uiState,
                    ),
                )
            }
            is SessionEvent.FeedbackEmitted -> {
                feedbackDismissJob = PracticeViewModelHandlers.showFeedback(
                    code = event.item.code,
                    uiState = _uiState,
                    scope = viewModelScope,
                    currentJob = feedbackDismissJob,
                )
            }
            is SessionEvent.GuidanceMessageEmitted -> {
                feedbackDismissJob = PracticeViewModelHandlers.showGuidance(
                    code = event.messageCode,
                    uiState = _uiState,
                    scope = viewModelScope,
                    currentJob = feedbackDismissJob,
                )
            }
            is SessionEvent.StateChanged -> Unit
        }
    }

    override fun onCleared() {
        super.onCleared()
        voicePlayer.shutdown()
        feedbackDismissJob?.cancel()
    }
}

internal data class SessionFinishContext(
    val definition: PoomsaeDefinition,
    val snapshots: Map<Int, PoseFrame>,
    val sessionHolder: PracticeSessionHolder,
    val uiState: MutableStateFlow<PracticeUiState>,
)

internal object PracticeViewModelHandlers {

    fun handleSessionAction(
        action: PracticeAction,
        engine: SessionEngine,
        sessionHolder: PracticeSessionHolder,
        uiState: MutableStateFlow<PracticeUiState>,
    ): Boolean =
        when (action) {
            is PracticeAction.ProcessFrame -> {
                uiState.update { it.copy(latestFrame = action.frame) }
                engine.processFrame(action.frame)
                true
            }
            is PracticeAction.Start -> { engine.dispatch(SessionCommand.Start); true }
            is PracticeAction.Pause -> { engine.dispatch(SessionCommand.Pause); true }
            is PracticeAction.Resume -> { engine.dispatch(SessionCommand.Resume); true }
            is PracticeAction.RepeatCurrent -> { engine.dispatch(SessionCommand.RepeatCurrent); true }
            is PracticeAction.SkipCurrent -> { engine.dispatch(SessionCommand.SkipCurrent); true }
            is PracticeAction.RestartForm -> { engine.dispatch(SessionCommand.RestartForm); true }
            is PracticeAction.FinishSession -> { engine.dispatch(SessionCommand.Finish(save = true)); true }
            is PracticeAction.DiscardSession -> {
                engine.dispatch(SessionCommand.Discard)
                sessionHolder.clearDraft()
                uiState.update { it.copy(showExitDialog = false) }
                true
            }
            is PracticeAction.SaveDraftAndExit -> {
                val runningStep = uiState.value.step as? PracticeStep.Running
                sessionHolder.setDraft(runningStep?.movementIndex ?: 1)
                uiState.update { it.copy(showExitDialog = false) }
                true
            }
            else -> false
        }

    fun handleUiAction(
        action: PracticeAction,
        uiState: MutableStateFlow<PracticeUiState>,
        currentDismissJob: Job?,
    ): Job? {
        when (action) {
            is PracticeAction.ToggleVoice -> uiState.update { it.copy(isVoiceEnabled = !it.isVoiceEnabled) }
            is PracticeAction.ToggleCameraFacing -> uiState.update { it.copy(cameraFacing = it.cameraFacing.toggle()) }
            is PracticeAction.SetExitDialogVisible -> uiState.update { it.copy(showExitDialog = action.visible) }
            is PracticeAction.UpdateFpsMetrics -> {
                uiState.update { it.copy(fps = action.fps, droppedFrames = action.droppedFrames) }
            }
            else -> Unit
        }
        return currentDismissJob
    }

    fun onMovementEvaluated(
        index: Int,
        eval: MovementEvaluation,
        uiState: MutableStateFlow<PracticeUiState>,
    ) {
        val segmentState = MovementTimelineSegmentState.Evaluated(eval.category)
        uiState.update { current ->
            val updatedTimeline = current.timelineStates.toMutableMap()
            updatedTimeline[index] = segmentState
            current.copy(timelineStates = updatedTimeline, hasEvaluatedMovements = true)
        }
    }

    fun onSessionFinished(
        evaluations: List<MovementEvaluation>,
        globalScore: Double?,
        context: SessionFinishContext,
    ) {
        val evaluable = evaluations.count { it.evaluability == Evaluability.EVALUABLE }
        val partial = evaluations.count { it.evaluability == Evaluability.PARTIAL }
        val notEvaluable = evaluations.count {
            it.evaluability == Evaluability.NOT_EVALUABLE || it.evaluability == null
        }
        val summary = SessionSummaryData(
            definition = context.definition,
            evaluations = evaluations,
            globalScore = globalScore,
            evaluableCount = evaluable,
            partialCount = partial,
            notEvaluableCount = notEvaluable,
            snapshots = context.snapshots.toMap(),
        )
        context.sessionHolder.setSummary(summary)
        context.uiState.update { it.copy(step = PracticeStep.Finished(evaluations, globalScore)) }
    }

    fun showFeedback(
        code: mx.taekwondo.poomsae.analysis.evaluation.FeedbackCode,
        uiState: MutableStateFlow<PracticeUiState>,
        scope: CoroutineScope,
        currentJob: Job?,
    ): Job {
        val resId = FeedbackMessageMapper.getStringResId(code)
        uiState.update { it.copy(feedbackResId = resId, feedbackSeverity = BannerSeverity.WARNING) }
        currentJob?.cancel()
        return scope.launch {
            delay(AnalysisConfig.T_FEEDBACK_MS)
            uiState.update { it.copy(feedbackResId = null) }
        }
    }

    fun showGuidance(
        code: String,
        uiState: MutableStateFlow<PracticeUiState>,
        scope: CoroutineScope,
        currentJob: Job?,
    ): Job {
        val resId = when (code) {
            "TRACKING_RECOVERED" -> R.string.MSG_RESUME
            QualityRejectionCode.FRAME_TOO_CLOSE.name -> R.string.MSG_FRAME_TOO_CLOSE
            QualityRejectionCode.FRAME_TOO_FAR.name -> R.string.MSG_FRAME_TOO_FAR
            QualityRejectionCode.FRAME_CUT.name -> R.string.MSG_FRAME_CUT
            QualityRejectionCode.NO_PERSON.name -> R.string.MSG_LOST
            QualityRejectionCode.LOW_CONFIDENCE.name -> R.string.MSG_LOW_CONF
            else -> R.string.MSG_LOST
        }
        uiState.update { it.copy(feedbackResId = resId, feedbackSeverity = BannerSeverity.INFO) }
        currentJob?.cancel()
        return scope.launch {
            delay(AnalysisConfig.T_FEEDBACK_MS)
            uiState.update { it.copy(feedbackResId = null) }
        }
    }
}

internal object PracticeViewModelHelpers {
    fun mapSessionStateToStep(
        state: SessionState,
        definition: PoomsaeDefinition,
    ): PracticeStep =
        when (state) {
            is SessionState.Idle, is SessionState.Framing -> PracticeStep.Framing()
            is SessionState.PermissionRequired -> PracticeStep.Framing(R.string.MSG_PERM_RATIONALE)
            is SessionState.Calibrating -> PracticeStep.Calibrating()
            is SessionState.Ready -> PracticeStep.Ready(state.countdownSeconds)
            is SessionState.Running -> {
                val mov = definition.movements.find { it.index == state.movementIndex }
                val nextMov = definition.movements.find { it.index == state.movementIndex + 1 }
                PracticeStep.Running(
                    movementIndex = state.movementIndex,
                    movementNameEs = mov?.nameEs ?: "",
                    movementNameKo = mov?.nameKo ?: "",
                    stanceName = mov?.stance?.type?.name ?: "",
                    nextMovementName = nextMov?.nameEs,
                    turnDegrees = mov?.expectedTurnDeg ?: 0,
                    isKihap = mov?.kihap == true,
                )
            }
            is SessionState.Paused -> PracticeStep.Paused(state.reason)
            is SessionState.Finished -> PracticeStep.Finished(state.evaluations, state.globalScore)
            is SessionState.Aborted -> PracticeStep.Error(R.string.MSG_CAM_ERR)
        }
}
