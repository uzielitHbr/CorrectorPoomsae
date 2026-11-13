package tkd.apptkd.video

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.evaluation.Evaluability
import mx.taekwondo.poomsae.analysis.poomsae.PoomsaeDefinition
import mx.taekwondo.poomsae.analysis.segmentation.SessionCommand
import mx.taekwondo.poomsae.analysis.segmentation.SessionEngine
import mx.taekwondo.poomsae.analysis.segmentation.SessionState
import mx.taekwondo.poomsae.analysis.video.VideoMetadata
import mx.taekwondo.poomsae.analysis.video.VideoValidationResult
import mx.taekwondo.poomsae.analysis.video.VideoValidator
import tkd.apptkd.ui.SessionSummaryData
import java.io.IOException

/**
 * Notificador de progreso de análisis de cuadros de video muestreados.
 */
fun interface VideoProgressCallback {
    fun onProgress(progress: Float, currentSec: Int, totalSec: Int, movementsDetected: Int)
}

private data class VideoProcessingContext(
    val extractor: VideoFrameExtractor,
    val detector: VideoPoseDetector,
    val sessionEngine: SessionEngine,
    val metadata: VideoMetadata,
    val scaledDims: Pair<Int, Int>,
)

/**
 * Orquestador fuera de línea que decodifica y evalúa cuadros secuenciales sin presión de tiempo real.
 *
 * Aplica reducción previa de escala, corrección de orientación, marcas de tiempo intrínsecas
 * y contención estricta de memoria (reciclaje inmediato de mapas de bits, SC-28, SC-29).
 */
class OfflineVideoProcessor(
    private val definition: PoomsaeDefinition,
) {
    suspend fun process(
        extractor: VideoFrameExtractor,
        detector: VideoPoseDetector,
        onProgress: VideoProgressCallback = VideoProgressCallback { _, _, _, _ -> },
    ): VideoAnalysisOutcome = try {
        val metadata = extractor.extractMetadata()
        val validation = VideoValidator.validate(metadata)
        if (validation is VideoValidationResult.Rejected) {
            VideoAnalysisOutcome.Rejected(validation.reason)
        } else {
            executeProcessing(extractor, detector, metadata, onProgress)
        }
    } catch (_: CancellationException) {
        VideoAnalysisOutcome.Cancelled
    } catch (e: IllegalStateException) {
        VideoAnalysisOutcome.Error(e.message ?: UNKNOWN_ERROR_MSG)
    } catch (e: IllegalArgumentException) {
        VideoAnalysisOutcome.Error(e.message ?: UNKNOWN_ERROR_MSG)
    } catch (e: IOException) {
        VideoAnalysisOutcome.Error(e.message ?: UNKNOWN_ERROR_MSG)
    } finally {
        extractor.close()
        detector.close()
    }

    private suspend fun executeProcessing(
        extractor: VideoFrameExtractor,
        detector: VideoPoseDetector,
        metadata: VideoMetadata,
        onProgress: VideoProgressCallback,
    ): VideoAnalysisOutcome {
        val scaledDims = VideoFrameScaler.computeScaledDimensions(
            width = metadata.width,
            height = metadata.height,
        )
        val sessionEngine = SessionEngine(definition).apply {
            dispatch(SessionCommand.Start)
        }
        val context = VideoProcessingContext(
            extractor = extractor,
            detector = detector,
            sessionEngine = sessionEngine,
            metadata = metadata,
            scaledDims = scaledDims,
        )

        val completed = runFrameLoop(context, onProgress)
        return if (!completed) {
            VideoAnalysisOutcome.Cancelled
        } else {
            finalizeSession(sessionEngine)
        }
    }

    private suspend fun runFrameLoop(
        context: VideoProcessingContext,
        onProgress: VideoProgressCallback,
    ): Boolean {
        val durationMs = context.metadata.durationMs
        val totalSec = (durationMs / MILLIS_PER_SECOND).toInt()

        for (timeMs in 0L until durationMs step AnalysisConfig.VIDEO_FRAME_INTERVAL_MS) {
            if (!currentCoroutineContext().isActive) {
                return false
            }
            processSingleFrame(context, timeMs)

            val currentSec = (timeMs / MILLIS_PER_SECOND).toInt()
            val progress = (timeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            val movementsCount = computeMovementsCount(context.sessionEngine.state.value)
            onProgress.onProgress(progress, currentSec, totalSec, movementsCount)
        }
        return currentCoroutineContext().isActive
    }

    private fun processSingleFrame(
        context: VideoProcessingContext,
        timeMs: Long,
    ) {
        val rawBitmap = context.extractor.getScaledFrameAt(
            timeMs,
            context.scaledDims.first,
            context.scaledDims.second,
        ) ?: return
        val uprightBitmap = VideoFrameRotator.rotateIfNeeded(rawBitmap, context.metadata.rotationDeg)
        try {
            val poseFrame = context.detector.processFrame(uprightBitmap, timeMs)
            if (poseFrame != null) {
                context.sessionEngine.processFrame(poseFrame)
            }
        } finally {
            if (uprightBitmap !== rawBitmap && !rawBitmap.isRecycled) {
                rawBitmap.recycle()
            }
            if (!uprightBitmap.isRecycled) {
                uprightBitmap.recycle()
            }
        }
    }

    private fun finalizeSession(sessionEngine: SessionEngine): VideoAnalysisOutcome.Success {
        if (sessionEngine.state.value is SessionState.Running) {
            sessionEngine.dispatch(SessionCommand.Finish(save = true))
        }
        val finishedState = sessionEngine.state.value as? SessionState.Finished
        val evaluations = finishedState?.evaluations ?: emptyList()
        val globalScore = finishedState?.globalScore

        val evaluable = evaluations.count { it.evaluability == Evaluability.EVALUABLE }
        val partial = evaluations.count { it.evaluability == Evaluability.PARTIAL }
        val notEvaluable = evaluations.count {
            it.evaluability == Evaluability.NOT_EVALUABLE || it.evaluability == null
        }

        return VideoAnalysisOutcome.Success(
            SessionSummaryData(
                definition = definition,
                evaluations = evaluations,
                globalScore = globalScore,
                evaluableCount = evaluable,
                partialCount = partial,
                notEvaluableCount = notEvaluable,
                snapshots = emptyMap(),
            ),
        )
    }

    private fun computeMovementsCount(state: SessionState): Int = when (state) {
        is SessionState.Running -> (state.movementIndex - 1).coerceAtLeast(0)
        is SessionState.Finished -> state.evaluations.size
        else -> 0
    }

    private companion object {
        private const val MILLIS_PER_SECOND = 1000L
        private const val UNKNOWN_ERROR_MSG = "Error desconocido durante el análisis de video"
    }
}
