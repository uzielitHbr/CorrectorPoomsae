@file:Suppress("MagicNumber")

package mx.taekwondo.poomsae.analysis.evaluation

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.geometry.AngleGeometry
import mx.taekwondo.poomsae.analysis.geometry.GeometryResult
import mx.taekwondo.poomsae.analysis.geometry.Vec3
import mx.taekwondo.poomsae.analysis.model.BodyProfile
import mx.taekwondo.poomsae.analysis.model.CoordinateSpace
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.Landmark
import mx.taekwondo.poomsae.analysis.model.MetricResult
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.model.UnavailableReason
import mx.taekwondo.poomsae.analysis.poomsae.MetricId
import mx.taekwondo.poomsae.analysis.poomsae.Movement
import mx.taekwondo.poomsae.analysis.poomsae.Side
import mx.taekwondo.poomsae.analysis.poomsae.Stance
import mx.taekwondo.poomsae.analysis.poomsae.Technique
import mx.taekwondo.poomsae.analysis.poomsae.TechniqueType
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Calculador de las 21 métricas biomecánicas definidas en la sección 13.4.
 *
 * Funciones puras que reciben [PoseFrame], [Movement] y [BodyProfile],
 * devolviendo [MetricResult.Value] o [MetricResult.Unavailable] con motivo explícito.
 */
object MetricCalculator {
    /** Calcula todas las métricas aplicables para un movimiento dado en un cuadro clave. */
    fun calculateMetrics(
        frame: PoseFrame,
        movement: Movement,
        profile: BodyProfile,
        window: List<PoseFrame> = listOf(frame),
        measuredTurnDeg: Double? = null,
    ): Map<MetricId, MetricResult> =
        buildMap {
            putAll(StanceMetrics.calculate(frame, movement.stance, profile, window))
            putAll(ArmTechniqueMetrics.calculate(frame, movement.technique, profile))
            movement.extra?.let { extra ->
                putAll(ArmTechniqueMetrics.calculate(frame, extra, profile))
            }
            if (movement.turnDegrees != 0 || measuredTurnDeg != null) {
                putAll(TurnMetrics.calculate(measuredTurnDeg, movement.expectedTurnDeg.toDouble()))
            }
        }

    internal fun sideJoint(
        side: Side,
        left: Joint,
        right: Joint,
    ): Joint = if (side == Side.LEFT) left else right
}

internal object StanceMetrics {
    fun calculate(
        frame: PoseFrame,
        stance: Stance,
        profile: BodyProfile,
        window: List<PoseFrame>,
    ): Map<MetricId, MetricResult> {
        val w = frame.landmarks(CoordinateSpace.WORLD)
        val fSide = stance.frontLeg
        val rSide = stance.frontLeg.opposite()

        val pAf = w[MetricCalculator.sideJoint(fSide, Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE)]
        val pAr = w[MetricCalculator.sideJoint(rSide, Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE)]
        val pAl = w[Joint.LEFT_ANKLE]
        val pAright = w[Joint.RIGHT_ANKLE]
        val pHl = w[Joint.LEFT_HIP]
        val pHr = w[Joint.RIGHT_HIP]
        val pSl = w[Joint.LEFT_SHOULDER]
        val pSr = w[Joint.RIGHT_SHOULDER]

        val jHf = MetricCalculator.sideJoint(fSide, Joint.LEFT_HIP, Joint.RIGHT_HIP)
        val jKf = MetricCalculator.sideJoint(fSide, Joint.LEFT_KNEE, Joint.RIGHT_KNEE)
        val jAf = MetricCalculator.sideJoint(fSide, Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE)

        val jHr = MetricCalculator.sideJoint(rSide, Joint.LEFT_HIP, Joint.RIGHT_HIP)
        val jKr = MetricCalculator.sideJoint(rSide, Joint.LEFT_KNEE, Joint.RIGHT_KNEE)
        val jAr = MetricCalculator.sideJoint(rSide, Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE)

        return mapOf(
            MetricId.ST_01 to computeStanceLength(pAf, pAr, pHl, pHr, profile),
            MetricId.ST_02 to computeStanceWidth(pAl, pAright, profile),
            MetricId.ST_03 to AngleGeometry.jointAngle3D(frame, jHf, jKf, jAf),
            MetricId.ST_04 to AngleGeometry.jointAngle3D(frame, jHr, jKr, jAr),
            MetricId.ST_05 to computeTorsoUpright(pSl, pSr, pHl, pHr, profile.upVector),
            MetricId.ST_06 to computeHipSway(window, profile.tImgMedian),
        )
    }

    private fun computeStanceLength(
        pAf: Landmark?,
        pAr: Landmark?,
        pHl: Landmark?,
        pHr: Landmark?,
        profile: BodyProfile,
    ): MetricResult =
        when {
            listOf(pAf, pAr, pHl, pHr).any { it == null } ->
                MetricResult.Unavailable(UnavailableReason.MISSING_JOINT)
            minOf(pAf!!.visibility, pAr!!.visibility, pHl!!.visibility, pHr!!.visibility) < AnalysisConfig.VIS_MIN ->
                MetricResult.Unavailable(UnavailableReason.LOW_VISIBILITY)
            else -> {
                val hipDist = (pHr.toVec3() - pHl.toVec3()).norm()
                val hipRelDiff = abs(hipDist - profile.shoulderWidth * 0.7) / (profile.shoulderWidth * 0.7)
                if (hipRelDiff > AnalysisConfig.HIP_AXIS_REL_TOL + 1.0) {
                    MetricResult.Unavailable(UnavailableReason.DEGENERATE_GEOMETRY)
                } else {
                    val stepVec = pAf.toVec3() - pAr.toVec3()
                    val length = abs(stepVec.dot(profile.referenceForward)) / profile.lLeg
                    val conf = minOf(pAf.visibility, pAr.visibility, pHl.visibility, pHr.visibility)
                    if (length in 0.0..2.5) {
                        MetricResult.Value(length, conf)
                    } else {
                        MetricResult.Unavailable(UnavailableReason.OUT_OF_RANGE)
                    }
                }
            }
        }

    private fun computeStanceWidth(
        pAl: Landmark?,
        pAr: Landmark?,
        profile: BodyProfile,
    ): MetricResult =
        when {
            pAl == null || pAr == null -> MetricResult.Unavailable(UnavailableReason.MISSING_JOINT)
            minOf(pAl.visibility, pAr.visibility) < AnalysisConfig.VIS_MIN ->
                MetricResult.Unavailable(UnavailableReason.LOW_VISIBILITY)
            else -> {
                val stepVec = pAl.toVec3() - pAr.toVec3()
                val width = abs(stepVec.dot(profile.referenceHipAxis)) / profile.shoulderWidth
                val conf = minOf(pAl.visibility, pAr.visibility)
                if (width in 0.0..3.0) {
                    MetricResult.Value(width, conf)
                } else {
                    MetricResult.Unavailable(UnavailableReason.OUT_OF_RANGE)
                }
            }
        }

    private fun computeTorsoUpright(
        sL: Landmark?,
        sR: Landmark?,
        hL: Landmark?,
        hR: Landmark?,
        u: Vec3,
    ): MetricResult =
        when {
            listOf(sL, sR, hL, hR).any { it == null } ->
                MetricResult.Unavailable(UnavailableReason.MISSING_JOINT)
            minOf(sL!!.visibility, sR!!.visibility, hL!!.visibility, hR!!.visibility) < AnalysisConfig.VIS_MIN ->
                MetricResult.Unavailable(UnavailableReason.LOW_VISIBILITY)
            else -> {
                val sMid = (sL.toVec3() + sR.toVec3()) * 0.5
                val hMid = (hL.toVec3() + hR.toVec3()) * 0.5
                val conf = minOf(sL.visibility, sR.visibility, hL.visibility, hR.visibility)
                AngleGeometry.angleWithVertical(sMid - hMid, u, conf)
            }
        }

    private fun computeHipSway(
        window: List<PoseFrame>,
        tImg: Double,
    ): MetricResult {
        if (window.isEmpty() || tImg <= AnalysisConfig.EPS_NORM) {
            return MetricResult.Value(0.0, 1.0f)
        }
        val hipCenters =
            window.mapNotNull { f ->
                val img = f.landmarks(CoordinateSpace.IMAGE)
                val hL = img[Joint.LEFT_HIP]
                val hR = img[Joint.RIGHT_HIP]
                if (hL != null && hR != null) Pair((hL.x + hR.x) * 0.5, (hL.y + hR.y) * 0.5) else null
            }
        val rms =
            if (hipCenters.size >= AnalysisConfig.MIN_WINDOW_FRAMES) {
                val avgX = hipCenters.map { it.first }.average()
                val avgY = hipCenters.map { it.second }.average()
                hypot(hipCenters.map { hypot(it.first - avgX, it.second - avgY) }.average(), 0.0) / tImg
            } else {
                0.0
            }
        return MetricResult.Value(rms, 1.0f)
    }
}

internal object ArmTechniqueMetrics {
    fun calculate(
        frame: PoseFrame,
        technique: Technique,
        profile: BodyProfile,
    ): Map<MetricId, MetricResult> =
        when (technique.type) {
            TechniqueType.ARAE_MAKKI -> calculateAraeMakki(frame, technique.arm, profile)
            TechniqueType.MOMTONG_JIREUGI -> calculateMomtongJireugi(frame, technique.arm, profile)
            TechniqueType.MOMTONG_AN_MAKKI -> calculateMomtongAnMakki(frame, technique.arm, profile)
            TechniqueType.EOLGUL_MAKKI -> calculateEolgulMakki(frame, technique.arm, profile)
            TechniqueType.AP_CHAGI -> KickMetrics.calculate(frame, technique.arm, profile)
        }

    private fun calculateAraeMakki(
        frame: PoseFrame,
        arm: Side,
        profile: BodyProfile,
    ): Map<MetricId, MetricResult> {
        val w = frame.landmarks(CoordinateSpace.WORLD)
        val jS = MetricCalculator.sideJoint(arm, Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER)
        val jE = MetricCalculator.sideJoint(arm, Joint.LEFT_ELBOW, Joint.RIGHT_ELBOW)
        val jW = MetricCalculator.sideJoint(arm, Joint.LEFT_WRIST, Joint.RIGHT_WRIST)
        val wrist = w[jW]
        val hL = w[Joint.LEFT_HIP]
        val hR = w[Joint.RIGHT_HIP]

        return mapOf(
            MetricId.AR_01 to AngleGeometry.jointAngle3D(frame, jS, jE, jW),
            MetricId.AR_02 to ArmBiometrics.computeWristHeightRatio(wrist, hL, hR, profile),
            MetricId.AR_03 to ArmBiometrics.computeWristLateralOffsetRatio(wrist, hL, hR, arm, profile),
        )
    }

    private fun calculateMomtongJireugi(
        frame: PoseFrame,
        arm: Side,
        profile: BodyProfile,
    ): Map<MetricId, MetricResult> {
        val w = frame.landmarks(CoordinateSpace.WORLD)
        val jS = MetricCalculator.sideJoint(arm, Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER)
        val jE = MetricCalculator.sideJoint(arm, Joint.LEFT_ELBOW, Joint.RIGHT_ELBOW)
        val jW = MetricCalculator.sideJoint(arm, Joint.LEFT_WRIST, Joint.RIGHT_WRIST)
        val s = w[jS]
        val wrist = w[jW]
        val hL = w[Joint.LEFT_HIP]
        val hR = w[Joint.RIGHT_HIP]

        return mapOf(
            MetricId.AR_01 to AngleGeometry.jointAngle3D(frame, jS, jE, jW),
            MetricId.AR_02 to ArmBiometrics.computeWristHeightRatio(wrist, hL, hR, profile),
            MetricId.AR_04 to ArmBiometrics.computeWristForwardOffset(wrist, s, profile),
            MetricId.AR_05 to ArmBiometrics.computePunchDirectionAngle(wrist, s, profile),
        )
    }

    private fun calculateMomtongAnMakki(
        frame: PoseFrame,
        arm: Side,
        profile: BodyProfile,
    ): Map<MetricId, MetricResult> {
        val w = frame.landmarks(CoordinateSpace.WORLD)
        val jS = MetricCalculator.sideJoint(arm, Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER)
        val jE = MetricCalculator.sideJoint(arm, Joint.LEFT_ELBOW, Joint.RIGHT_ELBOW)
        val jW = MetricCalculator.sideJoint(arm, Joint.LEFT_WRIST, Joint.RIGHT_WRIST)
        val s = w[jS]
        val e = w[jE]
        val wrist = w[jW]

        return mapOf(
            MetricId.AR_01 to AngleGeometry.jointAngle3D(frame, jS, jE, jW),
            MetricId.AR_06 to ArmBiometrics.computeForearmVerticalAngle(wrist, e, profile.upVector),
            MetricId.AR_07 to ArmBiometrics.computeFistAboveShoulder(wrist, s, profile),
        )
    }

    private fun calculateEolgulMakki(
        frame: PoseFrame,
        arm: Side,
        profile: BodyProfile,
    ): Map<MetricId, MetricResult> {
        val w = frame.landmarks(CoordinateSpace.WORLD)
        val jS = MetricCalculator.sideJoint(arm, Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER)
        val jE = MetricCalculator.sideJoint(arm, Joint.LEFT_ELBOW, Joint.RIGHT_ELBOW)
        val jW = MetricCalculator.sideJoint(arm, Joint.LEFT_WRIST, Joint.RIGHT_WRIST)
        val wrist = w[jW]
        val nose = w[Joint.NOSE]

        return mapOf(
            MetricId.AR_01 to AngleGeometry.jointAngle3D(frame, jS, jE, jW),
            MetricId.AR_08 to ArmBiometrics.computeWristAboveNose(wrist, nose, profile),
        )
    }
}

internal object ArmBiometrics {
    fun computeWristHeightRatio(
        wrist: Landmark?,
        hL: Landmark?,
        hR: Landmark?,
        profile: BodyProfile,
    ): MetricResult =
        when {
            wrist == null || hL == null || hR == null -> MetricResult.Unavailable(UnavailableReason.MISSING_JOINT)
            minOf(wrist.visibility, hL.visibility, hR.visibility) < AnalysisConfig.VIS_MIN ->
                MetricResult.Unavailable(UnavailableReason.LOW_VISIBILITY)
            else -> {
                val hMid = (hL.toVec3() + hR.toVec3()) * 0.5
                val ratio = (wrist.toVec3() - hMid).dot(profile.upVector) / profile.torsoLength
                MetricResult.Value(ratio, minOf(wrist.visibility, hL.visibility, hR.visibility))
            }
        }

    fun computeWristLateralOffsetRatio(
        wrist: Landmark?,
        hL: Landmark?,
        hR: Landmark?,
        arm: Side,
        profile: BodyProfile,
    ): MetricResult =
        when {
            wrist == null || hL == null || hR == null -> MetricResult.Unavailable(UnavailableReason.MISSING_JOINT)
            minOf(wrist.visibility, hL.visibility, hR.visibility) < AnalysisConfig.VIS_MIN ->
                MetricResult.Unavailable(UnavailableReason.LOW_VISIBILITY)
            else -> {
                val sa = if (arm == Side.RIGHT) 1.0 else -1.0
                val hMid = (hL.toVec3() + hR.toVec3()) * 0.5
                val ratio = sa * (wrist.toVec3() - hMid).dot(profile.referenceHipAxis) / profile.shoulderWidth
                MetricResult.Value(ratio, minOf(wrist.visibility, hL.visibility, hR.visibility))
            }
        }

    fun computeWristForwardOffset(
        wrist: Landmark?,
        s: Landmark?,
        profile: BodyProfile,
    ): MetricResult =
        when {
            wrist == null || s == null -> MetricResult.Unavailable(UnavailableReason.MISSING_JOINT)
            minOf(wrist.visibility, s.visibility) < AnalysisConfig.VIS_MIN ->
                MetricResult.Unavailable(UnavailableReason.LOW_VISIBILITY)
            else -> {
                val ratio = (wrist.toVec3() - s.toVec3()).dot(profile.referenceForward) / profile.armLength
                MetricResult.Value(ratio, minOf(wrist.visibility, s.visibility))
            }
        }

    fun computePunchDirectionAngle(
        wrist: Landmark?,
        s: Landmark?,
        profile: BodyProfile,
    ): MetricResult =
        when {
            wrist == null || s == null -> MetricResult.Unavailable(UnavailableReason.MISSING_JOINT)
            minOf(wrist.visibility, s.visibility) < AnalysisConfig.VIS_MIN ->
                MetricResult.Unavailable(UnavailableReason.LOW_VISIBILITY)
            else -> {
                val dir = wrist.toVec3() - s.toVec3()
                val horiz = AngleGeometry.projectOntoPlane(dir, profile.upVector)
                val conf = minOf(wrist.visibility, s.visibility)
                if (horiz !is GeometryResult.Success ||
                    horiz.value.norm() < AnalysisConfig.MIN_HORIZ_RATIO * profile.armLength
                ) {
                    MetricResult.Unavailable(UnavailableReason.DEGENERATE_GEOMETRY)
                } else {
                    AngleGeometry.angleBetweenPoints3D(dir, Vec3.ZERO, profile.referenceForward, conf)
                }
            }
        }

    fun computeForearmVerticalAngle(
        wrist: Landmark?,
        elbow: Landmark?,
        u: Vec3,
    ): MetricResult =
        when {
            wrist == null || elbow == null -> MetricResult.Unavailable(UnavailableReason.MISSING_JOINT)
            minOf(wrist.visibility, elbow.visibility) < AnalysisConfig.VIS_MIN ->
                MetricResult.Unavailable(UnavailableReason.LOW_VISIBILITY)
            else -> {
                val conf = minOf(wrist.visibility, elbow.visibility)
                AngleGeometry.angleWithVertical(wrist.toVec3() - elbow.toVec3(), u, conf)
            }
        }

    fun computeFistAboveShoulder(
        wrist: Landmark?,
        s: Landmark?,
        profile: BodyProfile,
    ): MetricResult =
        when {
            wrist == null || s == null -> MetricResult.Unavailable(UnavailableReason.MISSING_JOINT)
            minOf(wrist.visibility, s.visibility) < AnalysisConfig.VIS_MIN ->
                MetricResult.Unavailable(UnavailableReason.LOW_VISIBILITY)
            else -> {
                val ratio = (wrist.toVec3() - s.toVec3()).dot(profile.upVector) / profile.armLength
                MetricResult.Value(ratio, minOf(wrist.visibility, s.visibility))
            }
        }

    fun computeWristAboveNose(
        wrist: Landmark?,
        nose: Landmark?,
        profile: BodyProfile,
    ): MetricResult =
        when {
            wrist == null || nose == null -> MetricResult.Unavailable(UnavailableReason.MISSING_JOINT)
            minOf(wrist.visibility, nose.visibility) < AnalysisConfig.VIS_MIN ->
                MetricResult.Unavailable(UnavailableReason.LOW_VISIBILITY)
            else -> {
                val ratio = (wrist.toVec3() - nose.toVec3()).dot(profile.upVector) / profile.torsoLength
                MetricResult.Value(ratio, minOf(wrist.visibility, nose.visibility))
            }
        }
}

internal object KickMetrics {
    fun calculate(
        frame: PoseFrame,
        kickingLeg: Side,
        profile: BodyProfile,
    ): Map<MetricId, MetricResult> {
        val w = frame.landmarks(CoordinateSpace.WORLD)
        val pivotLeg = kickingLeg.opposite()

        val jHk = MetricCalculator.sideJoint(kickingLeg, Joint.LEFT_HIP, Joint.RIGHT_HIP)
        val jKk = MetricCalculator.sideJoint(kickingLeg, Joint.LEFT_KNEE, Joint.RIGHT_KNEE)
        val jAk = MetricCalculator.sideJoint(kickingLeg, Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE)

        val jHp = MetricCalculator.sideJoint(pivotLeg, Joint.LEFT_HIP, Joint.RIGHT_HIP)
        val jKp = MetricCalculator.sideJoint(pivotLeg, Joint.LEFT_KNEE, Joint.RIGHT_KNEE)
        val jAp = MetricCalculator.sideJoint(pivotLeg, Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE)

        val hK = w[jHk]
        val kK = w[jKk]
        val aK = w[jAk]

        return mapOf(
            MetricId.KI_01 to computeKneeLift(kK, hK, profile),
            MetricId.KI_02 to computeAnkleHeight(aK, hK, profile),
            MetricId.KI_03 to AngleGeometry.jointAngle3D(frame, jHk, jKk, jAk),
            MetricId.KI_04 to AngleGeometry.jointAngle3D(frame, jHp, jKp, jAp),
            MetricId.KI_05 to computeKickForwardness(aK, hK, profile),
        )
    }

    private fun computeKneeLift(
        kK: Landmark?,
        hK: Landmark?,
        profile: BodyProfile,
    ): MetricResult =
        when {
            kK == null || hK == null -> MetricResult.Unavailable(UnavailableReason.MISSING_JOINT)
            minOf(kK.visibility, hK.visibility) < AnalysisConfig.VIS_MIN ->
                MetricResult.Unavailable(UnavailableReason.LOW_VISIBILITY)
            else -> {
                val ratio = (kK.toVec3() - hK.toVec3()).dot(profile.upVector) / profile.lThigh
                MetricResult.Value(ratio, minOf(kK.visibility, hK.visibility))
            }
        }

    private fun computeAnkleHeight(
        aK: Landmark?,
        hK: Landmark?,
        profile: BodyProfile,
    ): MetricResult =
        when {
            aK == null || hK == null -> MetricResult.Unavailable(UnavailableReason.MISSING_JOINT)
            minOf(aK.visibility, hK.visibility) < AnalysisConfig.VIS_MIN ->
                MetricResult.Unavailable(UnavailableReason.LOW_VISIBILITY)
            else -> {
                val ratio = (aK.toVec3() - hK.toVec3()).dot(profile.upVector) / profile.lLeg
                MetricResult.Value(ratio, minOf(aK.visibility, hK.visibility))
            }
        }

    private fun computeKickForwardness(
        aK: Landmark?,
        hK: Landmark?,
        profile: BodyProfile,
    ): MetricResult =
        when {
            aK == null || hK == null -> MetricResult.Unavailable(UnavailableReason.MISSING_JOINT)
            minOf(aK.visibility, hK.visibility) < AnalysisConfig.VIS_MIN ->
                MetricResult.Unavailable(UnavailableReason.LOW_VISIBILITY)
            else -> {
                val ratio = (aK.toVec3() - hK.toVec3()).dot(profile.referenceForward) / profile.lLeg
                MetricResult.Value(ratio, minOf(aK.visibility, hK.visibility))
            }
        }
}

internal object TurnMetrics {
    fun calculate(
        measuredTurnDeg: Double?,
        expectedTurnDeg: Double,
    ): Map<MetricId, MetricResult> =
        if (measuredTurnDeg == null) {
            mapOf(
                MetricId.DI_01 to MetricResult.Unavailable(UnavailableReason.NO_HEADING_REFERENCE),
                MetricId.DI_02 to MetricResult.Unavailable(UnavailableReason.NO_HEADING_REFERENCE),
            )
        } else {
            val error = abs(AngleGeometry.wrapDegrees(measuredTurnDeg - expectedTurnDeg))
            mapOf(
                MetricId.DI_01 to MetricResult.Value(error, 1.0f),
                MetricId.DI_02 to MetricResult.Value(error, 1.0f),
            )
        }
}
