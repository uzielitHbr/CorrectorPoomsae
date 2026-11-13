package mx.taekwondo.poomsae.analysis.video

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig

/**
 * Validador anticipado de videos grabados para análisis offline (SC-28).
 */
object VideoValidator {
    private const val MILLIS_PER_SECOND = 1000L

    fun validate(metadata: VideoMetadata): VideoValidationResult {
        val maxDurationMs = AnalysisConfig.VIDEO_MAX_DURATION_SECONDS * MILLIS_PER_SECOND
        return when {
            !metadata.hasVideoTrack -> VideoValidationResult.Rejected(VideoRejectionReason.NO_VIDEO_TRACK)
            metadata.durationMs <= 0L || metadata.width <= 0 || metadata.height <= 0 ->
                VideoValidationResult.Rejected(VideoRejectionReason.UNSUPPORTED_FORMAT)
            metadata.fileSizeBytes > AnalysisConfig.VIDEO_MAX_FILE_SIZE_BYTES ->
                VideoValidationResult.Rejected(VideoRejectionReason.FILE_TOO_LARGE)
            metadata.durationMs > maxDurationMs ->
                VideoValidationResult.Rejected(VideoRejectionReason.DURATION_TOO_LONG)
            metadata.width > AnalysisConfig.VIDEO_MAX_WIDTH || metadata.height > AnalysisConfig.VIDEO_MAX_HEIGHT ->
                VideoValidationResult.Rejected(VideoRejectionReason.RESOLUTION_TOO_HIGH)
            else -> VideoValidationResult.Valid(metadata)
        }
    }
}
