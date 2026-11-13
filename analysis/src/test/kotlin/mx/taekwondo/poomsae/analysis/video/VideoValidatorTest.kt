package mx.taekwondo.poomsae.analysis.video

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoValidatorTest {
    private companion object {
        private const val VALID_DURATION_MS = 60_000L
        private const val VALID_WIDTH = 1280
        private const val VALID_HEIGHT = 720
        private const val VALID_ROTATION = 0
        private const val VALID_FILE_SIZE = 50L * 1024L * 1024L
    }

    private fun createValidMetadata(
        durationMs: Long = VALID_DURATION_MS,
        width: Int = VALID_WIDTH,
        height: Int = VALID_HEIGHT,
        fileSizeBytes: Long = VALID_FILE_SIZE,
        hasVideoTrack: Boolean = true,
    ): VideoMetadata {
        return VideoMetadata(
            durationMs = durationMs,
            width = width,
            height = height,
            rotationDeg = VALID_ROTATION,
            fileSizeBytes = fileSizeBytes,
            hasVideoTrack = hasVideoTrack,
        )
    }

    @Test
    fun `valid video metadata returns Valid`() {
        val metadata = createValidMetadata()
        val result = VideoValidator.validate(metadata)

        assertTrue(result is VideoValidationResult.Valid)
        assertEquals(metadata, (result as VideoValidationResult.Valid).metadata)
    }

    @Test
    fun `boundary test - exactly max duration is accepted`() {
        val maxDurationMs = AnalysisConfig.VIDEO_MAX_DURATION_SECONDS * 1000L
        val metadata = createValidMetadata(durationMs = maxDurationMs)
        val result = VideoValidator.validate(metadata)

        assertTrue(result is VideoValidationResult.Valid)
    }

    @Test
    fun `video exceeding max duration is rejected with DURATION_TOO_LONG`() {
        val overDurationMs = (AnalysisConfig.VIDEO_MAX_DURATION_SECONDS * 1000L) + 1L
        val metadata = createValidMetadata(durationMs = overDurationMs)
        val result = VideoValidator.validate(metadata)

        assertTrue(result is VideoValidationResult.Rejected)
        val rejected = result as VideoValidationResult.Rejected
        assertEquals(VideoRejectionReason.DURATION_TOO_LONG, rejected.reason)
    }

    @Test
    fun `boundary test - exactly max file size is accepted`() {
        val metadata = createValidMetadata(fileSizeBytes = AnalysisConfig.VIDEO_MAX_FILE_SIZE_BYTES)
        val result = VideoValidator.validate(metadata)

        assertTrue(result is VideoValidationResult.Valid)
    }

    @Test
    fun `video exceeding max file size is rejected with FILE_TOO_LARGE`() {
        val overSize = AnalysisConfig.VIDEO_MAX_FILE_SIZE_BYTES + 1L
        val metadata = createValidMetadata(fileSizeBytes = overSize)
        val result = VideoValidator.validate(metadata)

        assertTrue(result is VideoValidationResult.Rejected)
        val rejected = result as VideoValidationResult.Rejected
        assertEquals(VideoRejectionReason.FILE_TOO_LARGE, rejected.reason)
    }

    @Test
    fun `boundary test - exactly max resolution is accepted`() {
        val metadata =
            createValidMetadata(
                width = AnalysisConfig.VIDEO_MAX_WIDTH,
                height = AnalysisConfig.VIDEO_MAX_HEIGHT,
            )
        val result = VideoValidator.validate(metadata)

        assertTrue(result is VideoValidationResult.Valid)
    }

    @Test
    fun `video exceeding max width is rejected with RESOLUTION_TOO_HIGH`() {
        val metadata = createValidMetadata(width = AnalysisConfig.VIDEO_MAX_WIDTH + 1)
        val result = VideoValidator.validate(metadata)

        assertTrue(result is VideoValidationResult.Rejected)
        val rejected = result as VideoValidationResult.Rejected
        assertEquals(VideoRejectionReason.RESOLUTION_TOO_HIGH, rejected.reason)
    }

    @Test
    fun `video exceeding max height is rejected with RESOLUTION_TOO_HIGH`() {
        val metadata = createValidMetadata(height = AnalysisConfig.VIDEO_MAX_HEIGHT + 1)
        val result = VideoValidator.validate(metadata)

        assertTrue(result is VideoValidationResult.Rejected)
        val rejected = result as VideoValidationResult.Rejected
        assertEquals(VideoRejectionReason.RESOLUTION_TOO_HIGH, rejected.reason)
    }

    @Test
    fun `video with zero or negative duration is rejected with UNSUPPORTED_FORMAT`() {
        val zeroResult = VideoValidator.validate(createValidMetadata(durationMs = 0L))
        val negResult = VideoValidator.validate(createValidMetadata(durationMs = -100L))

        assertTrue(zeroResult is VideoValidationResult.Rejected)
        val zeroRejected = zeroResult as VideoValidationResult.Rejected
        assertEquals(VideoRejectionReason.UNSUPPORTED_FORMAT, zeroRejected.reason)

        assertTrue(negResult is VideoValidationResult.Rejected)
        val negRejected = negResult as VideoValidationResult.Rejected
        assertEquals(VideoRejectionReason.UNSUPPORTED_FORMAT, negRejected.reason)
    }

    @Test
    fun `video with invalid dimensions is rejected with UNSUPPORTED_FORMAT`() {
        val zeroWidthResult = VideoValidator.validate(createValidMetadata(width = 0))
        val negHeightResult = VideoValidator.validate(createValidMetadata(height = -1))

        assertTrue(zeroWidthResult is VideoValidationResult.Rejected)
        val zeroRejected = zeroWidthResult as VideoValidationResult.Rejected
        assertEquals(VideoRejectionReason.UNSUPPORTED_FORMAT, zeroRejected.reason)

        assertTrue(negHeightResult is VideoValidationResult.Rejected)
        val negRejected = negHeightResult as VideoValidationResult.Rejected
        assertEquals(VideoRejectionReason.UNSUPPORTED_FORMAT, negRejected.reason)
    }

    @Test
    fun `video without video track is rejected with NO_VIDEO_TRACK`() {
        val metadata = createValidMetadata(hasVideoTrack = false)
        val result = VideoValidator.validate(metadata)

        assertTrue(result is VideoValidationResult.Rejected)
        val rejected = result as VideoValidationResult.Rejected
        assertEquals(VideoRejectionReason.NO_VIDEO_TRACK, rejected.reason)
    }
}
