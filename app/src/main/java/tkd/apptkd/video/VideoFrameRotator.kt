package tkd.apptkd.video

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * Corrección de orientación de cuadros de video según metadatos de rotación (Sección 12.9.1).
 */
object VideoFrameRotator {

    private const val FULL_ROTATION_DEG = 360

    fun rotateIfNeeded(bitmap: Bitmap, rotationDeg: Int): Bitmap {
        val normalized = ((rotationDeg % FULL_ROTATION_DEG) + FULL_ROTATION_DEG) % FULL_ROTATION_DEG
        if (normalized == 0) {
            return bitmap
        }

        val matrix = Matrix().apply { postRotate(normalized.toFloat()) }
        val rotated = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true,
        )

        if (rotated !== bitmap && !bitmap.isRecycled) {
            bitmap.recycle()
        }
        return rotated
    }
}
