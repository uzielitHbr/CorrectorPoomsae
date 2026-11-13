package tkd.apptkd.video

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFrameScalerTest {

    @Test
    fun `computeScaledDimensions with 1080p landscape scales to target max dimension`() {
        val (w, h) = VideoFrameScaler.computeScaledDimensions(
            width = 1920,
            height = 1080,
            targetMaxDim = 640,
        )

        assertEquals(640, w)
        assertEquals(360, h)
    }

    @Test
    fun `computeScaledDimensions with 1080p portrait scales to target max dimension`() {
        val (w, h) = VideoFrameScaler.computeScaledDimensions(
            width = 1080,
            height = 1920,
            targetMaxDim = 640,
        )

        assertEquals(360, w)
        assertEquals(640, h)
    }

    @Test
    fun `computeScaledDimensions with smaller resolution retains original dimensions`() {
        val (w, h) = VideoFrameScaler.computeScaledDimensions(
            width = 320,
            height = 240,
            targetMaxDim = 640,
        )

        assertEquals(320, w)
        assertEquals(240, h)
    }

    @Test
    fun `computeScaledDimensions with square aspect ratio preserves square`() {
        val (w, h) = VideoFrameScaler.computeScaledDimensions(
            width = 1000,
            height = 1000,
            targetMaxDim = 500,
        )

        assertEquals(500, w)
        assertEquals(500, h)
    }

    @Test
    fun `computeScaledDimensions with invalid zero or negative dimensions returns targetMaxDim`() {
        val (wZero, hZero) = VideoFrameScaler.computeScaledDimensions(0, 0, 640)
        assertEquals(640, wZero)
        assertEquals(640, hZero)

        val (wNeg, hNeg) = VideoFrameScaler.computeScaledDimensions(-100, -50, 640)
        assertEquals(640, wNeg)
        assertEquals(640, hNeg)
    }

    @Test
    fun `default targetMaxDim matches AnalysisConfig VIDEO_DOWNSCALE_TARGET_MAX_DIM`() {
        val (w, h) = VideoFrameScaler.computeScaledDimensions(1920, 1080)
        assertEquals(AnalysisConfig.VIDEO_DOWNSCALE_TARGET_MAX_DIM, w)
        assertEquals(360, h)
    }
}
