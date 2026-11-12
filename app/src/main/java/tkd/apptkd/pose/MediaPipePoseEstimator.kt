package tkd.apptkd.pose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.taekwondo.poomsae.analysis.model.PoseEstimator
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import tkd.apptkd.camera.CameraMetrics
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Estimador de pose en tiempo real basado en MediaPipe Pose Landmarker Lite,
 * implementando estrictamente el modelo de sincronización y descarte de la sección 12.9.1.
 */
class MediaPipePoseEstimator(
    private val context: Context,
    private val modelAssetPath: String = DEFAULT_MODEL_PATH,
    private val adapter: MediaPipePoseAdapter = MediaPipePoseAdapter(),
) : PoseEstimator {

    companion object {
        const val DEFAULT_MODEL_PATH: String = "pose_landmarker_lite.task"
        private const val NANOS_PER_MILLI: Long = 1_000_000L
        private const val FPS_WINDOW_MS: Long = 1_000L
        private const val MILLIS_PER_SECOND_DOUBLE: Double = 1000.0
        private const val MIN_CONFIDENCE: Float = 0.5f
    }

    private var poseLandmarker: PoseLandmarker? = null
    private val inFlight = AtomicBoolean(false)
    private val isRunning = AtomicBoolean(false)

    private val droppedFramesCounter = AtomicLong(0L)
    private val processedFramesCounter = AtomicLong(0L)

    private val _frames = MutableStateFlow<PoseFrame?>(null)
    override val frames: Flow<PoseFrame?> = _frames.asStateFlow()

    private val _metrics = MutableStateFlow(CameraMetrics())
    val metrics: StateFlow<CameraMetrics> = _metrics.asStateFlow()

    // Cálculo de FPS
    private var lastFpsTimestampMs = 0L
    private var framesInCurrentWindow = 0

    override fun start() {
        if (isRunning.compareAndSet(false, true)) {
            initLandmarker()
        }
    }

    override fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            close()
        }
    }

    private fun initLandmarker() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(modelAssetPath)
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(MIN_CONFIDENCE)
            .setMinPosePresenceConfidence(MIN_CONFIDENCE)
            .setMinTrackingConfidence(MIN_CONFIDENCE)
            .setResultListener { result: PoseLandmarkerResult, _: MPImage ->
                handleResult(result)
            }
            .setErrorListener { error: RuntimeException ->
                handleError(error)
            }
            .build()

        poseLandmarker = PoseLandmarker.createFromOptions(context, options)
    }

    /**
     * Procesa un cuadro de CameraX según el contrato de 12.9.1:
     * - Si hay un cuadro en vuelo hacia el estimador, el nuevo se descarta inmediatamente y se cuenta.
     * - El ImageProxy SIEMPRE se libera.
     */
    @OptIn(ExperimentalGetImage::class)
    fun processImageProxy(imageProxy: ImageProxy) {
        if (!isRunning.get()) {
            imageProxy.close()
            return
        }

        if (!inFlight.compareAndSet(false, true)) {
            droppedFramesCounter.incrementAndGet()
            updateMetrics(0.0)
            imageProxy.close()
            return
        }

        try {
            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val processedBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            val timestampMs = imageProxy.imageInfo.timestamp / NANOS_PER_MILLI
            val mpImage = BitmapImageBuilder(processedBitmap).build()

            val landmarker = poseLandmarker
            if (landmarker != null) {
                landmarker.detectAsync(mpImage, timestampMs)
            } else {
                inFlight.set(false)
            }
        } catch (_: Exception) {
            droppedFramesCounter.incrementAndGet()
            inFlight.set(false)
        } finally {
            imageProxy.close()
        }
    }

    private fun handleResult(result: PoseLandmarkerResult) {
        try {
            val adaptResult = adapter.adapt(result)
            when (adaptResult) {
                is PoseAdaptationResult.Success -> {
                    _frames.value = adaptResult.frame
                    processedFramesCounter.incrementAndGet()
                    calculateFps(result.timestampMs())
                }
                is PoseAdaptationResult.Rejected -> {
                    droppedFramesCounter.incrementAndGet()
                    updateMetrics(0.0)
                }
            }
        } finally {
            inFlight.set(false)
        }
    }

    private fun handleError(error: RuntimeException) {
        System.err.println("MediaPipe Pose Landmarker error: ${error.message}")
        droppedFramesCounter.incrementAndGet()
        inFlight.set(false)
    }

    private fun calculateFps(currentTimestampMs: Long) {
        framesInCurrentWindow++
        val elapsed = currentTimestampMs - lastFpsTimestampMs
        if (elapsed >= FPS_WINDOW_MS) {
            val measuredFps = (framesInCurrentWindow * MILLIS_PER_SECOND_DOUBLE) / elapsed.toDouble()
            updateMetrics(measuredFps)
            framesInCurrentWindow = 0
            lastFpsTimestampMs = currentTimestampMs
        }
    }

    private fun updateMetrics(fps: Double) {
        val currentFps = if (fps > 0.0) fps else _metrics.value.fps
        _metrics.value = CameraMetrics(
            fps = currentFps,
            droppedFrames = droppedFramesCounter.get(),
            processedFrames = processedFramesCounter.get(),
        )
    }

    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
        adapter.reset()
        inFlight.set(false)
    }
}

