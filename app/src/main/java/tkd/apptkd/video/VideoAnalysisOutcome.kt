package tkd.apptkd.video

import mx.taekwondo.poomsae.analysis.video.VideoRejectionReason
import tkd.apptkd.ui.SessionSummaryData

/**
 * Resultado final de la ejecución de análisis de video fuera de línea.
 */
sealed interface VideoAnalysisOutcome {
    data class Success(val summary: SessionSummaryData) : VideoAnalysisOutcome
    data class Rejected(val reason: VideoRejectionReason) : VideoAnalysisOutcome
    data object Cancelled : VideoAnalysisOutcome
    data class Error(val message: String) : VideoAnalysisOutcome
}
