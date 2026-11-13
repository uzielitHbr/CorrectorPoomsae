package tkd.apptkd.video

import android.graphics.Bitmap
import android.net.Uri
import mx.taekwondo.poomsae.analysis.video.VideoMetadata

/**
 * Contrato de extracción de metadatos y decodificación de cuadros de video muestreados.
 */
interface VideoFrameExtractor : AutoCloseable {
    fun extractMetadata(): VideoMetadata
    fun getScaledFrameAt(timeMs: Long, targetWidth: Int, targetHeight: Int): Bitmap?
    override fun close()
}

/**
 * Fábrica para instanciar extractores de video a partir de un [Uri].
 */
fun interface VideoFrameExtractorFactory {
    fun create(uri: Uri): VideoFrameExtractor
}
