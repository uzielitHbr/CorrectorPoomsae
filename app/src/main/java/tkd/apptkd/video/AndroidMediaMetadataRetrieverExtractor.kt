package tkd.apptkd.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import mx.taekwondo.poomsae.analysis.video.VideoMetadata
import java.io.IOException

/**
 * Implementación de [VideoFrameExtractor] basada en la API oficial de Android [MediaMetadataRetriever].
 *
 * Utiliza decodificación nativa con escalado previo (API 27+) y acotamiento de memoria.
 */
class AndroidMediaMetadataRetrieverExtractor(
    private val context: Context,
    private val uri: Uri,
) : VideoFrameExtractor {

    private companion object {
        private const val MICROS_PER_MILLI = 1000L
    }

    private val retriever = MediaMetadataRetriever().apply {
        setDataSource(context, uri)
    }

    override fun extractMetadata(): VideoMetadata {
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull() ?: 0
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull() ?: 0
        val rotationDeg = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            ?.toIntOrNull() ?: 0
        val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            ?.equals("yes", ignoreCase = true) ?: (width > 0 && height > 0)
        val fileSizeBytes = queryFileSize(context, uri)

        return VideoMetadata(
            durationMs = durationMs,
            width = width,
            height = height,
            rotationDeg = rotationDeg,
            fileSizeBytes = fileSizeBytes,
            hasVideoTrack = hasVideo,
        )
    }

    override fun getScaledFrameAt(timeMs: Long, targetWidth: Int, targetHeight: Int): Bitmap? {
        val timeUs = timeMs * MICROS_PER_MILLI
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST,
                targetWidth,
                targetHeight,
            )
        } else {
            scaleLegacyFrame(timeUs, targetWidth, targetHeight)
        }
    }

    private fun scaleLegacyFrame(timeUs: Long, targetWidth: Int, targetHeight: Int): Bitmap? {
        val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST) ?: return null
        return if (frame.width == targetWidth && frame.height == targetHeight) {
            frame
        } else {
            val scaled = Bitmap.createScaledBitmap(frame, targetWidth, targetHeight, true)
            if (scaled !== frame && !frame.isRecycled) {
                frame.recycle()
            }
            scaled
        }
    }

    override fun close() {
        try {
            retriever.release()
        } catch (_: IOException) {
            // Ignorar excepción en cierre de recursos nativos
        } catch (_: RuntimeException) {
            // Ignorar excepción en cierre de recursos nativos
        }
    }

    private fun queryFileSize(context: Context, uri: Uri): Long =
        queryPfdSize(context, uri) ?: queryCursorSize(context, uri) ?: 0L

    private fun queryPfdSize(context: Context, uri: Uri): Long? = try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            pfd.statSize.takeIf { it > 0L }
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }

    private fun queryCursorSize(context: Context, uri: Uri): Long? = try {
        val projection = arrayOf(OpenableColumns.SIZE)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                cursor.getLong(sizeIndex).takeIf { it > 0L }
            } else {
                null
            }
        }
    } catch (_: SecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
