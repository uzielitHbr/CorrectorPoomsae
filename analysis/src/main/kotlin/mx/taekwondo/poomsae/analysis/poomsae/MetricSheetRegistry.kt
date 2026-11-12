package mx.taekwondo.poomsae.analysis.poomsae

import mx.taekwondo.poomsae.analysis.model.Joint

/** Identificadores de fichas de métricas definidos en la sección 13.4. */
enum class MetricId {
    ST_01,
    ST_02,
    ST_03,
    ST_04,
    ST_05,
    ST_06,
    AR_01,
    AR_02,
    AR_03,
    AR_04,
    AR_05,
    AR_06,
    AR_07,
    AR_08,
    KI_01,
    KI_02,
    KI_03,
    KI_04,
    KI_05,
    DI_01,
    DI_02,
}

/** Ficha de métrica con sus articulaciones MediaPipe requeridas para evaluación. */
data class MetricRequirement(
    val id: MetricId,
    val requiredJoints: List<Joint>,
)

/**
 * Registro de fichas de métricas de la sección 13.4 que resuelve las articulaciones
 * requeridas para cada postura, técnica y giro de Taegeuk 1 Jang.
 */
object MetricSheetRegistry {
    /** Resuelve las métricas de postura (ST-01 a ST-06) según la pierna adelantada. */
    fun resolveStanceMetrics(stance: Stance): List<MetricRequirement> {
        val frontHip = sideJoint(stance.frontLeg, Joint.LEFT_HIP, Joint.RIGHT_HIP)
        val frontKnee = sideJoint(stance.frontLeg, Joint.LEFT_KNEE, Joint.RIGHT_KNEE)
        val frontAnkle = sideJoint(stance.frontLeg, Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE)
        val rearSide = stance.frontLeg.opposite()
        val rearHip = sideJoint(rearSide, Joint.LEFT_HIP, Joint.RIGHT_HIP)
        val rearKnee = sideJoint(rearSide, Joint.LEFT_KNEE, Joint.RIGHT_KNEE)
        val rearAnkle = sideJoint(rearSide, Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE)

        return listOf(
            MetricRequirement(
                MetricId.ST_01,
                listOf(Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE, Joint.LEFT_HIP, Joint.RIGHT_HIP),
            ),
            MetricRequirement(
                MetricId.ST_02,
                listOf(Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE, Joint.LEFT_HIP, Joint.RIGHT_HIP),
            ),
            MetricRequirement(MetricId.ST_03, listOf(frontHip, frontKnee, frontAnkle)),
            MetricRequirement(MetricId.ST_04, listOf(rearHip, rearKnee, rearAnkle)),
            MetricRequirement(
                MetricId.ST_05,
                listOf(Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER, Joint.LEFT_HIP, Joint.RIGHT_HIP),
            ),
            MetricRequirement(
                MetricId.ST_06,
                listOf(Joint.LEFT_HIP, Joint.RIGHT_HIP, Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER),
            ),
        )
    }

    /** Resuelve las métricas de técnica (AR-* o KI-*) según tipo y extremidad ejecutora. */
    fun resolveTechniqueMetrics(technique: Technique): List<MetricRequirement> =
        when (technique.type) {
            TechniqueType.ARAE_MAKKI -> araeMakkiMetrics(technique.arm)
            TechniqueType.MOMTONG_JIREUGI -> momtongJireugiMetrics(technique.arm)
            TechniqueType.MOMTONG_AN_MAKKI -> momtongAnMakkiMetrics(technique.arm)
            TechniqueType.EOLGUL_MAKKI -> eolgulMakkiMetrics(technique.arm)
            TechniqueType.AP_CHAGI -> apChagiMetrics(technique.arm)
        }

    /** Resuelve todas las métricas aplicables a un movimiento completo (postura, técnicas y giro). */
    fun resolveMovementMetrics(movement: Movement): List<MetricRequirement> =
        buildList {
            addAll(resolveStanceMetrics(movement.stance))
            addAll(resolveTechniqueMetrics(movement.technique))
            movement.extra?.let { addAll(resolveTechniqueMetrics(it)) }
            if (movement.turnDegrees != 0) {
                add(MetricRequirement(MetricId.DI_01, listOf(Joint.LEFT_HIP, Joint.RIGHT_HIP)))
                add(MetricRequirement(MetricId.DI_02, listOf(Joint.LEFT_HIP, Joint.RIGHT_HIP)))
            }
        }

    private fun araeMakkiMetrics(arm: Side): List<MetricRequirement> {
        val s = sideJoint(arm, Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER)
        val e = sideJoint(arm, Joint.LEFT_ELBOW, Joint.RIGHT_ELBOW)
        val w = sideJoint(arm, Joint.LEFT_WRIST, Joint.RIGHT_WRIST)
        return listOf(
            MetricRequirement(MetricId.AR_01, listOf(s, e, w)),
            MetricRequirement(MetricId.AR_02, listOf(w, Joint.LEFT_HIP, Joint.RIGHT_HIP)),
            MetricRequirement(MetricId.AR_03, listOf(w, Joint.LEFT_HIP, Joint.RIGHT_HIP)),
        )
    }

    private fun momtongJireugiMetrics(arm: Side): List<MetricRequirement> {
        val s = sideJoint(arm, Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER)
        val e = sideJoint(arm, Joint.LEFT_ELBOW, Joint.RIGHT_ELBOW)
        val w = sideJoint(arm, Joint.LEFT_WRIST, Joint.RIGHT_WRIST)
        val opp = arm.opposite()
        val oppS = sideJoint(opp, Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER)
        val oppE = sideJoint(opp, Joint.LEFT_ELBOW, Joint.RIGHT_ELBOW)
        val oppW = sideJoint(opp, Joint.LEFT_WRIST, Joint.RIGHT_WRIST)
        return listOf(
            MetricRequirement(MetricId.AR_01, listOf(s, e, w)),
            MetricRequirement(MetricId.AR_02, listOf(w, Joint.LEFT_HIP, Joint.RIGHT_HIP)),
            MetricRequirement(MetricId.AR_04, listOf(s, w, Joint.LEFT_HIP, Joint.RIGHT_HIP)),
            MetricRequirement(MetricId.AR_05, listOf(s, w, Joint.LEFT_HIP, Joint.RIGHT_HIP)),
            MetricRequirement(MetricId.AR_01, listOf(oppS, oppE, oppW)),
            MetricRequirement(MetricId.AR_02, listOf(oppW, Joint.LEFT_HIP, Joint.RIGHT_HIP)),
        )
    }

    private fun momtongAnMakkiMetrics(arm: Side): List<MetricRequirement> {
        val s = sideJoint(arm, Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER)
        val e = sideJoint(arm, Joint.LEFT_ELBOW, Joint.RIGHT_ELBOW)
        val w = sideJoint(arm, Joint.LEFT_WRIST, Joint.RIGHT_WRIST)
        return listOf(
            MetricRequirement(MetricId.AR_01, listOf(s, e, w)),
            MetricRequirement(MetricId.AR_06, listOf(e, w)),
            MetricRequirement(MetricId.AR_07, listOf(s, w)),
        )
    }

    private fun eolgulMakkiMetrics(arm: Side): List<MetricRequirement> {
        val s = sideJoint(arm, Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER)
        val e = sideJoint(arm, Joint.LEFT_ELBOW, Joint.RIGHT_ELBOW)
        val w = sideJoint(arm, Joint.LEFT_WRIST, Joint.RIGHT_WRIST)
        return listOf(
            MetricRequirement(MetricId.AR_01, listOf(s, e, w)),
            MetricRequirement(MetricId.AR_08, listOf(w, Joint.NOSE)),
        )
    }

    private fun apChagiMetrics(kickingSide: Side): List<MetricRequirement> {
        val kickHip = sideJoint(kickingSide, Joint.LEFT_HIP, Joint.RIGHT_HIP)
        val kickKnee = sideJoint(kickingSide, Joint.LEFT_KNEE, Joint.RIGHT_KNEE)
        val kickAnkle = sideJoint(kickingSide, Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE)
        val pivotSide = kickingSide.opposite()
        return listOf(
            MetricRequirement(MetricId.KI_01, listOf(kickHip, kickKnee)),
            MetricRequirement(MetricId.KI_02, listOf(kickHip, kickAnkle)),
            MetricRequirement(MetricId.KI_03, listOf(kickHip, kickKnee, kickAnkle)),
            MetricRequirement(
                MetricId.KI_04,
                listOf(
                    sideJoint(pivotSide, Joint.LEFT_HIP, Joint.RIGHT_HIP),
                    sideJoint(pivotSide, Joint.LEFT_KNEE, Joint.RIGHT_KNEE),
                    sideJoint(pivotSide, Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE),
                ),
            ),
            MetricRequirement(MetricId.KI_05, listOf(kickHip, kickAnkle)),
        )
    }

    private fun sideJoint(
        side: Side,
        left: Joint,
        right: Joint,
    ): Joint = if (side == Side.LEFT) left else right
}
