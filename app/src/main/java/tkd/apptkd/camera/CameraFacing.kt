package tkd.apptkd.camera

/**
 * Selección de cámara frontal o trasera conforme a la tarea 3 de la Fase F4.
 */
enum class CameraFacing {
    FRONT,
    BACK,
    ;

    /** Alterna entre la cámara frontal y la trasera. */
    fun toggle(): CameraFacing = if (this == FRONT) BACK else FRONT
}

/**
 * Métricas de rendimiento y descarte de cuadros de la cámara y el estimador (Sección 12.9.1).
 */
data class CameraMetrics(
    val fps: Double = 0.0,
    val droppedFrames: Long = 0L,
    val processedFrames: Long = 0L,
)

