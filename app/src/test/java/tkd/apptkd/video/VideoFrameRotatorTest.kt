package tkd.apptkd.video

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VideoFrameRotatorTest {

    private companion object {
        private const val WIDTH = 100
        private const val HEIGHT = 200
    }

    @Test
    fun `rotateIfNeeded with 0 degrees returns same bitmap un-recycled`() {
        val original = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val rotated = VideoFrameRotator.rotateIfNeeded(original, 0)

        assertSame(original, rotated)
        assertFalse(original.isRecycled)
    }

    @Test
    fun `rotateIfNeeded with 360 degrees returns same bitmap un-recycled`() {
        val original = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val rotated = VideoFrameRotator.rotateIfNeeded(original, 360)

        assertSame(original, rotated)
        assertFalse(original.isRecycled)
    }

    @Test
    fun `rotateIfNeeded with 90 degrees rotates dimensions and recycles original`() {
        val original = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val rotated = VideoFrameRotator.rotateIfNeeded(original, 90)

        assertEquals(HEIGHT, rotated.width)
        assertEquals(WIDTH, rotated.height)
        assertTrue(original.isRecycled)
    }

    @Test
    fun `rotateIfNeeded with 180 degrees preserves dimensions and recycles original`() {
        val original = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val rotated = VideoFrameRotator.rotateIfNeeded(original, 180)

        assertEquals(WIDTH, rotated.width)
        assertEquals(HEIGHT, rotated.height)
        assertTrue(original.isRecycled)
    }

    @Test
    fun `rotateIfNeeded with 270 degrees rotates dimensions and recycles original`() {
        val original = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val rotated = VideoFrameRotator.rotateIfNeeded(original, 270)

        assertEquals(HEIGHT, rotated.width)
        assertEquals(WIDTH, rotated.height)
        assertTrue(original.isRecycled)
    }
}
