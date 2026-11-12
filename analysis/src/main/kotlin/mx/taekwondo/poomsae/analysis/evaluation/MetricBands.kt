@file:Suppress("MagicNumber")

package mx.taekwondo.poomsae.analysis.evaluation

import mx.taekwondo.poomsae.analysis.config.ParameterCalibrationStatus
import mx.taekwondo.poomsae.analysis.poomsae.MetricId
import mx.taekwondo.poomsae.analysis.poomsae.Movement
import mx.taekwondo.poomsae.analysis.poomsae.StanceType
import mx.taekwondo.poomsae.analysis.poomsae.TechniqueType

/**
 * Banda de puntuación de 0 a 100 con límites ideales [a, b] y límites exteriores [c, d] (Sección 13.8).
 *
 * S(v) = 100 para v en [a, b]
 * S(v) = 100 * (v - c) / (a - c) para v en (c, a)
 * S(v) = 100 * (d - v) / (d - b) para v en (b, d)
 * S(v) = 0 en cualquier otro caso
 *
 * @property status Estado normativo de calibración (todos PENDIENTE_CALIBRAR en fase actual).
 */
data class MetricScoringBand(
    val a: Double,
    val b: Double,
    val c: Double,
    val d: Double,
    val status: ParameterCalibrationStatus = ParameterCalibrationStatus.PENDIENTE_CALIBRAR,
) {
    init {
        require(c <= a && a <= b && b <= d) {
            "Límites inconsistentes: se requiere c ($c) <= a ($a) <= b ($b) <= d ($d)"
        }
    }

    /** Convierte el valor medido v en una puntuación de 0.0 a 100.0 con monotonía estricta. */
    fun score(value: Double): Double =
        when {
            value in a..b -> SCORE_PERFECT
            value in c..<a -> if (a - c > EPSILON) (SCORE_PERFECT * (value - c) / (a - c)) else SCORE_PERFECT
            value in b..d -> if (d - b > EPSILON) (SCORE_PERFECT * (d - value) / (d - b)) else SCORE_PERFECT
            else -> SCORE_ZERO
        }.coerceIn(SCORE_ZERO, SCORE_PERFECT)

    companion object {
        private const val SCORE_PERFECT: Double = 100.0
        private const val SCORE_ZERO: Double = 0.0
        private const val EPSILON: Double = 1e-9
    }
}

/**
 * Registro central de bandas de puntuación por postura y técnica (Sección 13.4 y 13.8).
 *
 * Todos los umbrales se declaran formalmente con estado PENDIENTE_CALIBRAR (Regla R-05).
 */
object MetricBandRegistry {
    fun resolveBand(
        metricId: MetricId,
        movement: Movement,
    ): MetricScoringBand =
        when (metricId.name.take(2)) {
            "ST" -> resolveStanceBand(metricId, movement.stance.type)
            "AR" -> resolveArmBand(metricId, movement.technique.type)
            "KI" -> resolveKickBand(metricId)
            else -> resolveDirectionBand(metricId)
        }

    private fun resolveStanceBand(
        metricId: MetricId,
        stanceType: StanceType,
    ): MetricScoringBand =
        when (metricId) {
            MetricId.ST_01 -> resolveStanceLengthBand(stanceType)
            MetricId.ST_02 -> MetricScoringBand(0.8, 1.2, 0.5, 1.5)
            MetricId.ST_03 -> resolveFrontKneeBand(stanceType)
            MetricId.ST_04 -> resolveRearKneeBand(stanceType)
            MetricId.ST_05 -> MetricScoringBand(0.0, 10.0, 0.0, 25.0)
            MetricId.ST_06 -> MetricScoringBand(0.0, 0.05, 0.0, 0.15)
            else -> MetricScoringBand(0.0, 0.0, 0.0, 0.0)
        }

    private fun resolveArmBand(
        metricId: MetricId,
        techniqueType: TechniqueType,
    ): MetricScoringBand =
        when (metricId) {
            MetricId.AR_01 -> resolveElbowBand(techniqueType)
            MetricId.AR_02 -> resolveWristHeightBand(techniqueType)
            MetricId.AR_03 -> MetricScoringBand(0.3, 0.7, 0.1, 0.9)
            MetricId.AR_04 -> MetricScoringBand(0.7, 1.0, 0.5, 1.0)
            MetricId.AR_05 -> MetricScoringBand(0.0, 15.0, 0.0, 30.0)
            MetricId.AR_06 -> MetricScoringBand(0.0, 25.0, 0.0, 45.0)
            MetricId.AR_07 -> MetricScoringBand(-0.2, 0.1, -0.4, 0.3)
            MetricId.AR_08 -> MetricScoringBand(0.1, 0.35, -0.05, 0.55)
            else -> MetricScoringBand(0.0, 0.0, 0.0, 0.0)
        }

    private fun resolveKickBand(metricId: MetricId): MetricScoringBand =
        when (metricId) {
            MetricId.KI_01 -> MetricScoringBand(0.0, 0.5, -0.2, 0.7)
            MetricId.KI_02 -> MetricScoringBand(0.1, 0.6, -0.1, 0.8)
            MetricId.KI_03 -> MetricScoringBand(155.0, 180.0, 135.0, 180.0)
            MetricId.KI_04 -> MetricScoringBand(160.0, 180.0, 140.0, 180.0)
            MetricId.KI_05 -> MetricScoringBand(0.5, 0.9, 0.3, 1.0)
            else -> MetricScoringBand(0.0, 0.0, 0.0, 0.0)
        }

    private fun resolveDirectionBand(metricId: MetricId): MetricScoringBand =
        when (metricId) {
            MetricId.DI_01 -> MetricScoringBand(0.0, 15.0, 0.0, 35.0)
            MetricId.DI_02 -> MetricScoringBand(0.0, 15.0, 0.0, 35.0)
            else -> MetricScoringBand(0.0, 0.0, 0.0, 0.0)
        }

    private fun resolveStanceLengthBand(type: StanceType): MetricScoringBand =
        when (type) {
            StanceType.AS -> MetricScoringBand(0.9, 1.1, 0.6, 1.4)
            StanceType.AK -> MetricScoringBand(1.3, 1.6, 1.0, 1.9)
        }

    private fun resolveFrontKneeBand(type: StanceType): MetricScoringBand =
        when (type) {
            StanceType.AS -> MetricScoringBand(160.0, 175.0, 140.0, 180.0)
            StanceType.AK -> MetricScoringBand(85.0, 105.0, 70.0, 120.0)
        }

    private fun resolveRearKneeBand(type: StanceType): MetricScoringBand =
        when (type) {
            StanceType.AS -> MetricScoringBand(165.0, 180.0, 150.0, 180.0)
            StanceType.AK -> MetricScoringBand(165.0, 180.0, 150.0, 180.0)
        }

    private fun resolveElbowBand(type: TechniqueType): MetricScoringBand =
        when (type) {
            TechniqueType.MOMTONG_JIREUGI -> MetricScoringBand(160.0, 175.0, 140.0, 180.0)
            TechniqueType.ARAE_MAKKI -> MetricScoringBand(150.0, 170.0, 130.0, 180.0)
            TechniqueType.MOMTONG_AN_MAKKI -> MetricScoringBand(90.0, 110.0, 70.0, 130.0)
            TechniqueType.EOLGUL_MAKKI -> MetricScoringBand(90.0, 110.0, 70.0, 130.0)
            TechniqueType.AP_CHAGI -> MetricScoringBand(160.0, 175.0, 140.0, 180.0)
        }

    private fun resolveWristHeightBand(type: TechniqueType): MetricScoringBand =
        when (type) {
            TechniqueType.MOMTONG_JIREUGI -> MetricScoringBand(-0.15, 0.15, -0.3, 0.3)
            TechniqueType.ARAE_MAKKI -> MetricScoringBand(-0.6, -0.3, -0.8, -0.15)
            TechniqueType.MOMTONG_AN_MAKKI -> MetricScoringBand(-0.1, 0.2, -0.25, 0.35)
            TechniqueType.EOLGUL_MAKKI -> MetricScoringBand(0.1, 0.35, -0.05, 0.55)
            TechniqueType.AP_CHAGI -> MetricScoringBand(-0.15, 0.15, -0.3, 0.3)
        }
}
