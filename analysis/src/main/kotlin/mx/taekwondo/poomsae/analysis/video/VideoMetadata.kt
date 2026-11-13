package mx.taekwondo.poomsae.analysis.video

/**
 * Metadatos técnicos de un archivo de video para análisis fuera de línea.
 */
data class VideoMetadata(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotationDeg: Int,
    val fileSizeBytes: Long,
    val hasVideoTrack: Boolean,
)
