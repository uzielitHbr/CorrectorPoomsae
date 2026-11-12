package mx.taekwondo.poomsae.analysis.segmentation

import mx.taekwondo.poomsae.analysis.evaluation.FeedbackItem
import mx.taekwondo.poomsae.analysis.evaluation.MovementEvaluation
import mx.taekwondo.poomsae.analysis.model.PoseFrame

/**
 * Motivos normativos de pausa de la sesión de práctica (Sección 12.4).
 */
enum class PauseReason {
    USER,
    CAMERA_INTERRUPTED,
    APP_BACKGROUND,
    LOST_TRACKING,
    PERMISSION_REVOKED,
}

/**
 * Subestado del movimiento activo durante la ejecución de la forma (Sección 12.4).
 */
sealed interface MovementSubState {
    data class AwaitingMovement(val n: Int, val awaitingSinceMs: Long) : MovementSubState

    data class InMovement(val n: Int, val startedAtMs: Long, val peakFrame: PoseFrame? = null) : MovementSubState

    data class Holding(val n: Int, val holdStartedMs: Long, val window: List<PoseFrame>) : MovementSubState

    data class Evaluated(val n: Int, val evaluation: MovementEvaluation) : MovementSubState
}

/**
 * Estados formales de la máquina de estados de la sesión según la sección 12.4.
 */
sealed interface SessionState {
    data object Idle : SessionState

    data object PermissionRequired : SessionState

    data object Framing : SessionState

    data object Calibrating : SessionState

    data class Ready(val countdownSeconds: Int = 3) : SessionState

    data class Running(
        val movementIndex: Int,
        val subState: MovementSubState,
    ) : SessionState

    data class Paused(val reason: PauseReason, val previousState: SessionState) : SessionState

    data class Finished(
        val evaluations: List<MovementEvaluation>,
        val globalScore: Double?,
    ) : SessionState

    data object Aborted : SessionState
}

/**
 * Comandos de control enviados al [SessionEngine] (Sección 12.9.2).
 */
sealed interface SessionCommand {
    data object Start : SessionCommand

    data object Pause : SessionCommand

    data object Resume : SessionCommand

    data object RepeatCurrent : SessionCommand

    data object SkipCurrent : SessionCommand

    data object RestartForm : SessionCommand

    data class Finish(val save: Boolean) : SessionCommand

    data object Discard : SessionCommand
}

/**
 * Eventos detectados por el [MotionSegmenter] a partir del análisis de cuadros (Sección 12.9.2).
 */
sealed interface SegmenterEvent {
    data class MovementStarted(val n: Int, val timestampMs: Long) : SegmenterEvent

    data class MovementPeakDetected(
        val n: Int,
        val peakFrame: PoseFrame,
        val window: List<PoseFrame>,
    ) : SegmenterEvent

    data class MovementHeld(
        val n: Int,
        val holdWindow: List<PoseFrame>,
        val peakFrame: PoseFrame? = null,
    ) : SegmenterEvent

    data class TimedOut(val n: Int, val elapsedMs: Long) : SegmenterEvent

    data class OmissionSuspected(val n: Int, val suspectedNextIndex: Int) : SegmenterEvent

    data object TrackingLost : SegmenterEvent

    data object TrackingRecovered : SegmenterEvent
}

/**
 * Eventos producidos por el motor de sesión [SessionEngine] hacia la UI y repositorios.
 */
sealed interface SessionEvent {
    data class MovementEvaluated(val n: Int, val evaluation: MovementEvaluation) : SessionEvent

    data class SessionFinished(
        val evaluations: List<MovementEvaluation>,
        val globalScore: Double?,
    ) : SessionEvent

    data class StateChanged(val oldState: SessionState, val newState: SessionState) : SessionEvent

    data class FeedbackEmitted(val item: FeedbackItem) : SessionEvent

    data class GuidanceMessageEmitted(val messageCode: String) : SessionEvent
}
