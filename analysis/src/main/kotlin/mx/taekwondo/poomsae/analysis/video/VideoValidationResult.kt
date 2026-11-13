package mx.taekwondo.poomsae.analysis.video

/**
 * Motivos de rechazo anticipado de un archivo de video según SC-28.
 */
enum class VideoRejectionReason {
    FILE_TOO_LARGE,
    DURATION_TOO_LONG,
    RESOLUTION_TOO_HIGH,
    UNSUPPORTED_FORMAT,
    NO_VIDEO_TRACK,
}

/**
 * Resultado de la validación estática de un video grabado.
 */
sealed interface VideoValidationResult {
    data class Valid(val metadata: VideoMetadata) : VideoValidationResult

    data class Rejected(val reason: VideoRejectionReason) : VideoValidationResult
}
