package tkd.apptkd.video

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import tkd.apptkd.pose.MediaPipePoseAdapter
import tkd.apptkd.pose.MediaPipePoseEstimator
import tkd.apptkd.pose.PoseAdaptationResult

/**
 * Detector de pose basado en MediaPipe Pose Landmarker configurado en [RunningMode.VIDEO].
 *
 * Procesa cuadros con marcas de tiempo monótonamente crecientes de forma síncrona en hilo secundario.
 */
class MediaPipeVideoPoseDetector(
    private val context: Context,
    private val modelAssetPath: String = MediaPipePoseEstimator.DEFAULT_MODEL_PATH,
    private val adapter: MediaPipePoseAdapter = MediaPipePoseAdapter(),
) : VideoPoseDetector {

    private companion object {
        private const val MIN_CONFIDENCE = 0.5f
    }

    private var poseLandmarker: PoseLandmarker? = null

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(modelAssetPath)
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.VIDEO)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(MIN_CONFIDENCE)
            .setMinPosePresenceConfidence(MIN_CONFIDENCE)
            .setMinTrackingConfidence(MIN_CONFIDENCE)
            .build()

        poseLandmarker = PoseLandmarker.createFromOptions(context, options)
    }

    override fun processFrame(bitmap: Bitmap, timestampMs: Long): PoseFrame? {
        val landmarker = poseLandmarker ?: return null
        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = landmarker.detectForVideo(mpImage, timestampMs)
        return when (val adapted = adapter.adapt(result)) {
            is PoseAdaptationResult.Success -> adapted.frame
            is PoseAdaptationResult.Rejected -> null
        }
    }

    override fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
        adapter.reset()
    }
}
