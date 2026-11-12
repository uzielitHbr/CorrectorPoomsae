package tkd.apptkd.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import tkd.apptkd.pose.MediaPipePoseEstimator
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Gestor del ciclo de vida y casos de uso de CameraX según la Fase F4 (Sección 12.9.1).
 *
 * Configura Preview e ImageAnalysis con estrategia STRATEGY_KEEP_ONLY_LATEST
 * y un ejecutor dedicado de un solo hilo para evitar saturar el hilo principal.
 */
class CameraManager(
    private val context: Context,
    private val poseEstimator: MediaPipePoseEstimator,
) {
    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val isBound = AtomicBoolean(false)

    /**
     * Inicia la cámara y enlaza los casos de uso al [lifecycleOwner] y al [previewView].
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        facing: CameraFacing = CameraFacing.FRONT,
        onCameraBound: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                if (cameraExecutor == null || cameraExecutor?.isShutdown == true) {
                    cameraExecutor = Executors.newSingleThreadExecutor()
                }

                bindCameraUseCases(lifecycleOwner, previewView, facing)
                isBound.set(true)
                onCameraBound?.invoke()
            } catch (e: java.util.concurrent.ExecutionException) {
                onError?.invoke(e)
            } catch (e: InterruptedException) {
                onError?.invoke(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        facing: CameraFacing,
    ) {
        val provider = cameraProvider ?: return
        val executor = cameraExecutor ?: return

        val cameraSelector = when (facing) {
            CameraFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
        }

        val preview = Preview.Builder()
            .build()
            .also {
                it.surfaceProvider = previewView.surfaceProvider
            }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(executor) { imageProxy ->
                    poseEstimator.processImageProxy(imageProxy)
                }
            }

        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis,
        )
    }

    /**
     * Cambia la orientación de la cámara (frontal o trasera).
     */
    fun switchCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        newFacing: CameraFacing,
    ) {
        if (isBound.get()) {
            bindCameraUseCases(lifecycleOwner, previewView, newFacing)
        }
    }

    /**
     * Detiene los casos de uso y libera el ejecutor dedicado.
     */
    fun shutdown() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor?.shutdown()
            cameraExecutor = null
            isBound.set(false)
        } catch (_: IllegalStateException) {
            // Cierre seguro
        } catch (_: SecurityException) {
            // Cierre seguro
        }
    }
}

