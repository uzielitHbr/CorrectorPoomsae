package mx.taekwondo.poomsae.analysis.evaluation

import mx.taekwondo.poomsae.analysis.poomsae.MetricId

/**
 * Enumeración cerrada de códigos normativos de retroalimentación técnica (Sección 12.8 / 13.4).
 *
 * El módulo puro `:analysis` emite únicamente estos códigos y sus parámetros numéricos;
 * la traducción a frases en español reside en el módulo `:app` (Regla R-07).
 */
enum class FeedbackCode {
    // Grupo ST: Postura, torso y estabilidad
    STANCE_STEP_TOO_SHORT,
    STANCE_STEP_TOO_LONG,
    STANCE_FEET_TOO_NARROW,
    STANCE_FEET_TOO_WIDE,
    FRONT_KNEE_INSUFFICIENT_BEND,
    FRONT_KNEE_OVERBENT,
    REAR_KNEE_NOT_EXTENDED,
    TORSO_NOT_UPRIGHT,
    HIP_SWAY_EXCESSIVE,

    // Grupo AR: Técnicas de brazo (bloqueos y puñetazos)
    ELBOW_OVERFLEXED,
    ELBOW_OVEREXTENDED,
    PUNCH_ARM_NOT_EXTENDED,
    WRIST_TOO_LOW,
    WRIST_TOO_HIGH,
    WRIST_LATERAL_OFFSET_INCORRECT,
    PUNCH_NOT_FORWARD_ENOUGH,
    PUNCH_OFF_TARGET_HORIZONTAL,
    FOREARM_INCLINATION_INCORRECT,
    FIST_NOT_AT_SHOULDER_HEIGHT,
    BLOCK_WRIST_BELOW_HEAD,

    // Grupo KI: Patada frontal
    KICK_KNEE_NOT_HIGH_ENOUGH,
    KICK_HEIGHT_TOO_LOW,
    KICK_LEG_NOT_EXTENDED,
    SUPPORT_KNEE_BENT,
    KICK_NOT_FORWARD,

    // Grupo DI: Dirección y rumbo
    TURN_ANGLE_INACCURATE,
    HEADING_DRIFT_EXCESSIVE,
}

/**
 * Elemento atómico de retroalimentación técnica estructurada emitido por el evaluador.
 */
data class FeedbackItem(
    val code: FeedbackCode,
    val metricId: MetricId,
    val measuredValue: Double,
    val targetMin: Double,
    val targetMax: Double,
)
