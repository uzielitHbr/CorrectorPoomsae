package tkd.apptkd.video

import android.graphics.Bitmap
import mx.taekwondo.poomsae.analysis.model.PoseFrame

/**
 * Contrato de detección de postura en video fuera de línea con marcas de tiempo crecientes.
 */
interface VideoPoseDetector : AutoCloseable {
    fun processFrame(bitmap: Bitmap, timestampMs: Long): PoseFrame?
    override fun close() {}
}

/**
 * Fábrica para instanciar detectores de postura en modo video.
 */
fun interface VideoPoseDetectorFactory {
    fun create(): VideoPoseDetector
}
