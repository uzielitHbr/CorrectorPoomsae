package mx.taekwondo.poomsae.analysis.quality

/**
 * Resultado de la evaluación de calidad de encuadre en [FrameQualityGate] (Sección 12.8 / F6).
 */
sealed interface FrameQualityResult {
    /** El cuadro cumple con los requisitos de visibilidad, distancia y márgenes. */
    data object Ok : FrameQualityResult

    /** El cuadro fue rechazado por deficiencias de encuadre o visibilidad. */
    data class Rejected(
        val code: QualityRejectionCode,
        val message: String,
    ) : FrameQualityResult
}

/**
 * Códigos cerrados de rechazo de encuadre correspondientes a los mensajes normativos de 12.8.
 */
enum class QualityRejectionCode {
    /** MSG_LOST: No se detecta ninguna persona en el cuadro. */
    NO_PERSON,

    /** MSG_LOW_CONF: Articulaciones requeridas con baja visibilidad o valores no finitos. */
    LOW_CONFIDENCE,

    /** MSG_FRAME_TOO_CLOSE: El torso ocupa una proporción excesiva del encuadre. */
    FRAME_TOO_CLOSE,

    /** MSG_FRAME_TOO_FAR: El practicante está demasiado lejos de la cámara. */
    FRAME_TOO_FAR,

    /** MSG_FRAME_CUT: Alguna articulación clave sale de los márgenes seguros de la imagen. */
    FRAME_CUT,
}
