package mx.taekwondo.poomsae.analysis.evaluation

import mx.taekwondo.poomsae.analysis.poomsae.MetricId

/**
 * Motor de reglas que traduce desviaciones métricas fuera de la banda ideal [a, b]
 * en códigos normativos estructurados [FeedbackCode].
 */
object FeedbackRuleEngine {
    /** Genera un [FeedbackItem] si el valor medido no alcanza la puntuación perfecta. */
    fun generateFeedback(
        metricId: MetricId,
        value: Double,
        band: MetricScoringBand,
    ): FeedbackItem? {
        val code = resolveFeedbackCode(metricId, value, band) ?: return null
        return FeedbackItem(
            code = code,
            metricId = metricId,
            measuredValue = value,
            targetMin = band.a,
            targetMax = band.b,
        )
    }

    private fun resolveFeedbackCode(
        metricId: MetricId,
        value: Double,
        band: MetricScoringBand,
    ): FeedbackCode? =
        when (metricId.name.take(2)) {
            "ST" -> resolveStanceFeedback(metricId, value, band)
            "AR" -> resolveArmFeedback(metricId, value, band)
            "KI" -> resolveKickFeedback(metricId)
            else -> resolveDirectionFeedback(metricId)
        }

    private fun resolveStanceFeedback(
        metricId: MetricId,
        value: Double,
        band: MetricScoringBand,
    ): FeedbackCode =
        when (metricId) {
            MetricId.ST_01 ->
                if (value < band.a) FeedbackCode.STANCE_STEP_TOO_SHORT else FeedbackCode.STANCE_STEP_TOO_LONG
            MetricId.ST_02 ->
                if (value < band.a) FeedbackCode.STANCE_FEET_TOO_NARROW else FeedbackCode.STANCE_FEET_TOO_WIDE
            MetricId.ST_03 ->
                if (value < band.a) FeedbackCode.FRONT_KNEE_OVERBENT else FeedbackCode.FRONT_KNEE_INSUFFICIENT_BEND
            MetricId.ST_04 -> FeedbackCode.REAR_KNEE_NOT_EXTENDED
            MetricId.ST_05 -> FeedbackCode.TORSO_NOT_UPRIGHT
            else -> FeedbackCode.HIP_SWAY_EXCESSIVE
        }

    private fun resolveArmFeedback(
        metricId: MetricId,
        value: Double,
        band: MetricScoringBand,
    ): FeedbackCode =
        when (metricId) {
            MetricId.AR_01 ->
                if (value < band.a) FeedbackCode.PUNCH_ARM_NOT_EXTENDED else FeedbackCode.ELBOW_OVEREXTENDED
            MetricId.AR_02 ->
                if (value < band.a) FeedbackCode.WRIST_TOO_LOW else FeedbackCode.WRIST_TOO_HIGH
            MetricId.AR_03 -> FeedbackCode.WRIST_LATERAL_OFFSET_INCORRECT
            MetricId.AR_04 -> FeedbackCode.PUNCH_NOT_FORWARD_ENOUGH
            MetricId.AR_05 -> FeedbackCode.PUNCH_OFF_TARGET_HORIZONTAL
            MetricId.AR_06 -> FeedbackCode.FOREARM_INCLINATION_INCORRECT
            MetricId.AR_07 -> FeedbackCode.FIST_NOT_AT_SHOULDER_HEIGHT
            else -> FeedbackCode.BLOCK_WRIST_BELOW_HEAD
        }

    private fun resolveKickFeedback(metricId: MetricId): FeedbackCode =
        when (metricId) {
            MetricId.KI_01 -> FeedbackCode.KICK_KNEE_NOT_HIGH_ENOUGH
            MetricId.KI_02 -> FeedbackCode.KICK_HEIGHT_TOO_LOW
            MetricId.KI_03 -> FeedbackCode.KICK_LEG_NOT_EXTENDED
            MetricId.KI_04 -> FeedbackCode.SUPPORT_KNEE_BENT
            else -> FeedbackCode.KICK_NOT_FORWARD
        }

    private fun resolveDirectionFeedback(metricId: MetricId): FeedbackCode =
        when (metricId) {
            MetricId.DI_01 -> FeedbackCode.TURN_ANGLE_INACCURATE
            else -> FeedbackCode.HEADING_DRIFT_EXCESSIVE
        }
}
