@file:Suppress("MagicNumber")

package mx.taekwondo.poomsae.analysis.quality

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.geometry.BodyPrimitives
import mx.taekwondo.poomsae.analysis.geometry.GeometryResult
import mx.taekwondo.poomsae.analysis.geometry.Vec3
import mx.taekwondo.poomsae.analysis.model.BodyProfile
import mx.taekwondo.poomsae.analysis.model.CoordinateSpace
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.signal.SignalStats
import kotlin.math.hypot

/** Motivos de rechazo de la calibración inicial de perfil corporal. */
enum class CalibrationRejectionReason {
    INSUFFICIENT_FRAMES,
    UNSTABLE_MEASUREMENTS,
    DEGENERATE_GEOMETRY,
}

/** Resultado formal de la calibración del atleta. */
sealed interface CalibrationResult {
    data class Success(val profile: BodyProfile) : CalibrationResult

    data class Rejected(
        val reason: CalibrationRejectionReason,
        val userHelpMessage: String,
    ) : CalibrationResult
}

private data class SegmentMeasurements(
    val lThigh: Double,
    val lShank: Double,
    val torsoLength: Double,
    val shoulderWidth: Double,
    val armLength: Double,
    val dNoseShoulder: Double,
)

/**
 * Calibrador antropométrico que extrae el [BodyProfile] del usuario (Sección 13.3).
 *
 * Utiliza la mediana estadística de una ráfaga de cuadros estables en posición junbi seogi
 * y verifica la estabilidad temporal exigiendo que MAD(S) / Mediana(S) <= CAL_MAX_REL_MAD.
 */
object BodyProfileCalibrator {
    fun calibrate(frames: List<PoseFrame>): CalibrationResult {
        if (frames.size < AnalysisConfig.CAL_MIN_FRAMES) {
            return CalibrationResult.Rejected(
                CalibrationRejectionReason.INSUFFICIENT_FRAMES,
                "Se requieren al menos ${AnalysisConfig.CAL_MIN_FRAMES} cuadros estables para calibrar.",
            )
        }
        return performCalibration(frames)
    }

    private fun performCalibration(frames: List<PoseFrame>): CalibrationResult {
        val thighs =
            extractSegmentLengths(frames) { f ->
                val w = f.landmarks(CoordinateSpace.WORLD)
                val l = CalibrationGeometryHelpers.dist3D(w[Joint.LEFT_HIP]?.toVec3(), w[Joint.LEFT_KNEE]?.toVec3())
                val r = CalibrationGeometryHelpers.dist3D(w[Joint.RIGHT_HIP]?.toVec3(), w[Joint.RIGHT_KNEE]?.toVec3())
                CalibrationGeometryHelpers.validAverage(l, r)
            }

        val shanks =
            extractSegmentLengths(frames) { f ->
                val w = f.landmarks(CoordinateSpace.WORLD)
                val l = CalibrationGeometryHelpers.dist3D(w[Joint.LEFT_KNEE]?.toVec3(), w[Joint.LEFT_ANKLE]?.toVec3())
                val r = CalibrationGeometryHelpers.dist3D(w[Joint.RIGHT_KNEE]?.toVec3(), w[Joint.RIGHT_ANKLE]?.toVec3())
                CalibrationGeometryHelpers.validAverage(l, r)
            }

        val torsos =
            extractSegmentLengths(frames) { f ->
                val w = f.landmarks(CoordinateSpace.WORLD)
                val midShoulder =
                    CalibrationGeometryHelpers.midPoint(
                        w[Joint.LEFT_SHOULDER]?.toVec3(),
                        w[Joint.RIGHT_SHOULDER]?.toVec3(),
                    )
                val midHip =
                    CalibrationGeometryHelpers.midPoint(
                        w[Joint.LEFT_HIP]?.toVec3(),
                        w[Joint.RIGHT_HIP]?.toVec3(),
                    )
                CalibrationGeometryHelpers.dist3D(midShoulder, midHip)
            }

        val shoulders =
            extractSegmentLengths(frames) { f ->
                val w = f.landmarks(CoordinateSpace.WORLD)
                CalibrationGeometryHelpers.dist3D(w[Joint.LEFT_SHOULDER]?.toVec3(), w[Joint.RIGHT_SHOULDER]?.toVec3())
            }

        val arms = extractSegmentLengths(frames) { extractArmLength(it) }

        val dNoseShoulders =
            extractSegmentLengths(frames) { f ->
                val w = f.landmarks(CoordinateSpace.WORLD)
                val n = w[Joint.NOSE]?.toVec3()
                val l = CalibrationGeometryHelpers.dist3D(n, w[Joint.LEFT_SHOULDER]?.toVec3())
                val r = CalibrationGeometryHelpers.dist3D(n, w[Joint.RIGHT_SHOULDER]?.toVec3())
                CalibrationGeometryHelpers.validAverage(l, r)
            }

        val check = validateSeriesStability(listOf(thighs, shanks, torsos, shoulders, arms, dNoseShoulders))
        if (check != null) return check

        val lThigh = medianOf(thighs)
        val lShank = medianOf(shanks)
        val torsoLength = medianOf(torsos)
        val shoulderWidth = medianOf(shoulders)
        val armLength = medianOf(arms)
        val dNoseShoulder = medianOf(dNoseShoulders)

        return if (listOf(lThigh, lShank, torsoLength, shoulderWidth, armLength, dNoseShoulder).any { it == null }) {
            CalibrationResult.Rejected(
                CalibrationRejectionReason.DEGENERATE_GEOMETRY,
                "No se pudieron calcular las medianas de los segmentos.",
            )
        } else {
            val segments =
                SegmentMeasurements(
                    lThigh = lThigh!!,
                    lShank = lShank!!,
                    torsoLength = torsoLength!!,
                    shoulderWidth = shoulderWidth!!,
                    armLength = armLength!!,
                    dNoseShoulder = dNoseShoulder!!,
                )
            buildProfile(frames, segments)
        }
    }

    private fun extractArmLength(frame: PoseFrame): Double? {
        val w = frame.landmarks(CoordinateSpace.WORLD)
        val l1 = CalibrationGeometryHelpers.dist3D(w[Joint.LEFT_SHOULDER]?.toVec3(), w[Joint.LEFT_ELBOW]?.toVec3())
        val l2 = CalibrationGeometryHelpers.dist3D(w[Joint.LEFT_ELBOW]?.toVec3(), w[Joint.LEFT_WRIST]?.toVec3())
        val r1 = CalibrationGeometryHelpers.dist3D(w[Joint.RIGHT_SHOULDER]?.toVec3(), w[Joint.RIGHT_ELBOW]?.toVec3())
        val r2 = CalibrationGeometryHelpers.dist3D(w[Joint.RIGHT_ELBOW]?.toVec3(), w[Joint.RIGHT_WRIST]?.toVec3())
        val leftArm = if (l1 != null && l2 != null) l1 + l2 else null
        val rightArm = if (r1 != null && r2 != null) r1 + r2 else null
        return CalibrationGeometryHelpers.validAverage(leftArm, rightArm)
    }

    private fun extractSegmentLengths(
        frames: List<PoseFrame>,
        extractor: (PoseFrame) -> Double?,
    ): List<Double> = frames.mapNotNull { extractor(it) }

    private fun validateSeriesStability(seriesList: List<List<Double>>): CalibrationResult? =
        seriesList
            .asSequence()
            .mapNotNull { series ->
                if (series.size < AnalysisConfig.CAL_MIN_FRAMES) {
                    CalibrationResult.Rejected(
                        CalibrationRejectionReason.INSUFFICIENT_FRAMES,
                        "Datos insuficientes para completar la calibración.",
                    )
                } else {
                    val medRes = SignalStats.median(series)
                    val madRes = SignalStats.mad(series)
                    when {
                        medRes !is GeometryResult.Success || madRes !is GeometryResult.Success ->
                            CalibrationResult.Rejected(
                                CalibrationRejectionReason.DEGENERATE_GEOMETRY,
                                "Geometría corporal degenerada durante la calibración.",
                            )
                        medRes.value <= AnalysisConfig.EPS_NORM ->
                            CalibrationResult.Rejected(
                                CalibrationRejectionReason.DEGENERATE_GEOMETRY,
                                "Geometría corporal degenerada durante la calibración.",
                            )
                        (madRes.value / medRes.value) > AnalysisConfig.CAL_MAX_REL_MAD ->
                            CalibrationResult.Rejected(
                                CalibrationRejectionReason.UNSTABLE_MEASUREMENTS,
                                "No pudimos medir tu cuerpo con claridad. Mejora la luz y quédate quieto.",
                            )
                        else -> null
                    }
                }
            }.firstOrNull()

    private fun buildProfile(
        frames: List<PoseFrame>,
        segments: SegmentMeasurements,
    ): CalibrationResult {
        val midFrame = frames[frames.size / 2]
        val u = (BodyPrimitives.estimateUpVector(frames) as? GeometryResult.Success)?.value
        val r0 = u?.let { (BodyPrimitives.hipAxisHorizontal(midFrame, it) as? GeometryResult.Success)?.value?.point }
        val sigma = u?.let { (BodyPrimitives.estimateForwardSign(frames, it) as? GeometryResult.Success)?.value } ?: 1.0
        val f0 =
            u?.let {
                (BodyPrimitives.bodyForwardDirection(midFrame, it, sigma) as? GeometryResult.Success)?.value?.point
            }

        return if (u == null || r0 == null || f0 == null) {
            CalibrationResult.Rejected(
                CalibrationRejectionReason.DEGENERATE_GEOMETRY,
                "No se pudo estimar la orientación corporal de referencia.",
            )
        } else {
            CalibrationResult.Success(
                BodyProfile(
                    lThigh = segments.lThigh,
                    lShank = segments.lShank,
                    lLeg = segments.lThigh + segments.lShank,
                    torsoLength = segments.torsoLength,
                    shoulderWidth = segments.shoulderWidth,
                    armLength = segments.armLength,
                    dNoseShoulder = segments.dNoseShoulder,
                    upVector = u,
                    forwardSign = sigma,
                    referenceForward = f0,
                    referenceHipAxis = r0,
                    tImgMedian = computeTorsoImageMedian(frames),
                ),
            )
        }
    }

    private fun computeTorsoImageMedian(frames: List<PoseFrame>): Double {
        val values =
            frames.mapNotNull { f ->
                val img = f.landmarks(CoordinateSpace.IMAGE)
                val sL = img[Joint.LEFT_SHOULDER] ?: return@mapNotNull null
                val sR = img[Joint.RIGHT_SHOULDER] ?: return@mapNotNull null
                val hL = img[Joint.LEFT_HIP] ?: return@mapNotNull null
                val hR = img[Joint.RIGHT_HIP] ?: return@mapNotNull null
                hypot((sL.x + sR.x) * 0.5 - (hL.x + hR.x) * 0.5, (sL.y + sR.y) * 0.5 - (hL.y + hR.y) * 0.5)
            }
        return if (values.isNotEmpty()) medianOf(values) ?: 0.35 else 0.35
    }

    private fun medianOf(values: List<Double>): Double? = (SignalStats.median(values) as? GeometryResult.Success)?.value
}

private object CalibrationGeometryHelpers {
    fun dist3D(
        a: Vec3?,
        b: Vec3?,
    ): Double? = if (a != null && b != null) (a - b).norm() else null

    fun validAverage(
        a: Double?,
        b: Double?,
    ): Double? =
        when {
            a != null && b != null -> (a + b) * 0.5
            a != null -> a
            b != null -> b
            else -> null
        }

    fun midPoint(
        a: Vec3?,
        b: Vec3?,
    ): Vec3? = if (a != null && b != null) (a + b) * 0.5 else null
}
