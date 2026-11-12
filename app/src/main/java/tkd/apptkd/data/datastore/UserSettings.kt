package tkd.apptkd.data.datastore

import tkd.apptkd.camera.CameraFacing

/**
 * Nivel de exigencia para el cálculo y tolerancia de bandas biomecánicas (Sección 12.7 y 13.8).
 */
enum class ExigencyLevel {
    RELAXED,
    NORMAL,
    STRICT,
}

/**
 * Modelo de preferencias y ajustes de usuario persistidos en DataStore (Sección 12.10 y RF-12).
 */
data class UserSettings(
    val defaultCameraFacing: CameraFacing = CameraFacing.FRONT,
    val voiceEnabled: Boolean = true,
    val exigencyLevel: ExigencyLevel = ExigencyLevel.NORMAL,
    val strictMode: Boolean = false,
    val saveSnapshots: Boolean = false,
)

