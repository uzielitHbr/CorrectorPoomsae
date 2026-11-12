package mx.taekwondo.poomsae.analysis.geometry

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.model.CoordinateSpace
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.MetricResult
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.model.UnavailableReason
import mx.taekwondo.poomsae.analysis.signal.SignalStats
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min

/**
 * Calibration orientation basis for projecting and measuring body heading (Section 13.2).
 *
 * @property up Unit vector pointing vertically upward (`u`).
 * @property forwardSign Orientation sign (`sigma in {-1.0, +1.0}`) aligning `u x h` with toe-heel direction.
 * @property forwardZero Initial reference forward unit vector (`F_0`), or `null` if unavailable (e.g., resumed).
 * @property rightZero Initial reference lateral unit vector (`r_0 = h_g_hat`), or `null` if unavailable.
 */
data class OrientationBasis(
    val up: Vec3,
    val forwardSign: Double,
    val forwardZero: Vec3? = null,
    val rightZero: Vec3? = null,
)

/**
 * Biomechanical geometric primitives specified in Section 13.2 of the specification.
 */
object BodyPrimitives {
    private const val HALF_CIRCLE_DEG: Double = 180.0
    private const val RAD_TO_DEG: Double = HALF_CIRCLE_DEG / PI

    /**
     * Estimates the unit vertical "up" vector `u` from upright calibration [frames] (Section 13.2):
     * normalized component-wise median of `mid(S_L, S_R) - mid(A_L, A_R)`.
     */
    fun estimateUpVector(
        frames: List<PoseFrame>,
        minVisibility: Float = AnalysisConfig.VIS_MIN,
    ): GeometryResult<Vec3> {
        if (frames.isEmpty()) {
            return GeometryResult.Failure(
                reason = UnavailableReason.NO_CALIBRATION,
                message = "No calibration frames supplied to estimate up vector",
            )
        }
        val collected = collectUpVectors(frames, minVisibility)
        return when (collected) {
            is GeometryResult.Failure -> collected
            is GeometryResult.Success ->
                when (val med = SignalStats.medianVec3(collected.value)) {
                    is GeometryResult.Failure -> med
                    is GeometryResult.Success -> med.value.normalized()
                }
        }
    }

    /**
     * Computes the horizontal hip axis unit vector `h_g_hat = G(h) / ||G(h)||` where `h = P_{R_H} - P_{L_H}`.
     */
    fun hipAxisHorizontal(
        frame: PoseFrame,
        up: Vec3,
        minVisibility: Float = AnalysisConfig.VIS_MIN,
    ): GeometryResult<ReliablePoint> =
        when (
            val pts =
                frame.extractReliablePoints(
                    Joint.LEFT_HIP,
                    Joint.RIGHT_HIP,
                    space = CoordinateSpace.WORLD,
                    minVisibility = minVisibility,
                )
        ) {
            is GeometryResult.Failure -> pts
            is GeometryResult.Success -> {
                val left = pts.value.getValue(Joint.LEFT_HIP)
                val right = pts.value.getValue(Joint.RIGHT_HIP)
                val h = right.point - left.point
                val minVis = min(left.visibility, right.visibility)
                normalizeProjected(h, up, minVis)
            }
        }

    /**
     * Estimates the forward orientation sign `sigma in {-1.0, +1.0}` from upright [frames] (Section 13.2):
     * `sigma = sign( median(G(u x h)) · median(G(P_Toe - P_Heel)) )`.
     */
    fun estimateForwardSign(
        frames: List<PoseFrame>,
        up: Vec3,
        minVisibility: Float = AnalysisConfig.VIS_MIN,
    ): GeometryResult<Double> =
        if (frames.isEmpty()) {
            GeometryResult.Failure(
                reason = UnavailableReason.NO_CALIBRATION,
                message = "No calibration frames supplied to estimate forward sign",
            )
        } else {
            when (val upUnitResult = up.normalized()) {
                is GeometryResult.Failure -> upUnitResult
                is GeometryResult.Success ->
                    computeForwardSignFromValidFrames(frames, upUnitResult.value, minVisibility)
            }
        }

    /**
     * Computes the forward body direction unit vector `F = sigma * G(u x h) / ||G(u x h)||` (Section 13.2).
     */
    fun bodyForwardDirection(
        frame: PoseFrame,
        up: Vec3,
        forwardSign: Double,
        minVisibility: Float = AnalysisConfig.VIS_MIN,
    ): GeometryResult<ReliablePoint> {
        if (abs(abs(forwardSign) - 1.0) > AnalysisConfig.EPS_SCALAR) {
            return GeometryResult.Failure(
                reason = UnavailableReason.OUT_OF_RANGE,
                message = "forwardSign must be +1.0 or -1.0, got $forwardSign",
            )
        }
        return when (
            val pts =
                frame.extractReliablePoints(
                    Joint.LEFT_HIP,
                    Joint.RIGHT_HIP,
                    space = CoordinateSpace.WORLD,
                    minVisibility = minVisibility,
                )
        ) {
            is GeometryResult.Failure -> pts
            is GeometryResult.Success -> {
                val left = pts.value.getValue(Joint.LEFT_HIP)
                val right = pts.value.getValue(Joint.RIGHT_HIP)
                val h = right.point - left.point
                val minVis = min(left.visibility, right.visibility)
                val crossUnit = normalizeProjected(up.cross(h), up, minVis)
                when (crossUnit) {
                    is GeometryResult.Failure -> crossUnit
                    is GeometryResult.Success ->
                        GeometryResult.Success(
                            ReliablePoint(crossUnit.value.point * forwardSign, crossUnit.value.visibility),
                        )
                }
            }
        }
    }

    /**
     * Computes body heading `psi` in degrees `(-180, 180]` (positive = turn to person's right, Section 13.2):
     * `psi = wrap(atan2(F · r_0, F · F_0) * 180 / pi)`.
     */
    fun computeHeadingDeg(
        forward: Vec3,
        referenceForward: Vec3,
        referenceRight: Vec3,
        confidence: Float = 1.0f,
    ): MetricResult {
        val fNorm = forward.normalized()
        val f0Norm = referenceForward.normalized()
        val r0Norm = referenceRight.normalized()
        return when {
            fNorm is GeometryResult.Failure -> MetricResult.Unavailable(fNorm.reason)
            f0Norm is GeometryResult.Failure -> MetricResult.Unavailable(f0Norm.reason)
            r0Norm is GeometryResult.Failure -> MetricResult.Unavailable(r0Norm.reason)
            fNorm is GeometryResult.Success &&
                f0Norm is GeometryResult.Success &&
                r0Norm is GeometryResult.Success -> {
                val psiRad =
                    atan2(
                        fNorm.value.dot(r0Norm.value),
                        fNorm.value.dot(f0Norm.value),
                    )
                MetricResult.Value(
                    value = AngleGeometry.wrapDegrees(psiRad * RAD_TO_DEG),
                    confidence = confidence,
                )
            }
            else -> MetricResult.Unavailable(UnavailableReason.DEGENERATE_GEOMETRY)
        }
    }

    /**
     * Computes body heading `psi` from a [PoseFrame] against [basis], returning
     * [UnavailableReason.NO_HEADING_REFERENCE] when `forwardZero` or `rightZero` is absent.
     */
    fun computeHeadingFromFrame(
        frame: PoseFrame,
        basis: OrientationBasis,
        minVisibility: Float = AnalysisConfig.VIS_MIN,
    ): MetricResult {
        val f0 = basis.forwardZero
        val r0 = basis.rightZero
        return if (f0 == null || r0 == null) {
            MetricResult.Unavailable(UnavailableReason.NO_HEADING_REFERENCE)
        } else {
            when (val forwardRes = bodyForwardDirection(frame, basis.up, basis.forwardSign, minVisibility)) {
                is GeometryResult.Failure -> MetricResult.Unavailable(forwardRes.reason)
                is GeometryResult.Success ->
                    computeHeadingDeg(
                        forward = forwardRes.value.point,
                        referenceForward = f0,
                        referenceRight = r0,
                        confidence = forwardRes.value.visibility,
                    )
            }
        }
    }

    private fun collectUpVectors(
        frames: List<PoseFrame>,
        minVisibility: Float,
    ): GeometryResult<List<Vec3>> {
        val valid = ArrayList<Vec3>(frames.size)
        var firstFailure: GeometryResult.Failure? = null
        for (frame in frames) {
            val pts =
                frame.extractReliablePoints(
                    Joint.LEFT_SHOULDER,
                    Joint.RIGHT_SHOULDER,
                    Joint.LEFT_ANKLE,
                    Joint.RIGHT_ANKLE,
                    space = CoordinateSpace.WORLD,
                    minVisibility = minVisibility,
                )
            when (pts) {
                is GeometryResult.Failure -> if (firstFailure == null) firstFailure = pts
                is GeometryResult.Success -> {
                    val midShoulder =
                        Vec3.midpoint(
                            pts.value.getValue(Joint.LEFT_SHOULDER).point,
                            pts.value.getValue(Joint.RIGHT_SHOULDER).point,
                        )
                    val midAnkle =
                        Vec3.midpoint(
                            pts.value.getValue(Joint.LEFT_ANKLE).point,
                            pts.value.getValue(Joint.RIGHT_ANKLE).point,
                        )
                    valid.add(midShoulder - midAnkle)
                }
            }
        }
        return if (valid.isEmpty()) {
            firstFailure ?: GeometryResult.Failure(UnavailableReason.NO_CALIBRATION, "No valid frames")
        } else {
            GeometryResult.Success(valid)
        }
    }

    private fun computeForwardSignFromValidFrames(
        frames: List<PoseFrame>,
        upUnit: Vec3,
        minVisibility: Float,
    ): GeometryResult<Double> {
        val crossVectors = ArrayList<Vec3>(frames.size)
        val footVectors = ArrayList<Vec3>(frames.size * 2)
        var firstFailure: GeometryResult.Failure? = null

        for (frame in frames) {
            val pts =
                frame.extractReliablePoints(
                    Joint.LEFT_HIP,
                    Joint.RIGHT_HIP,
                    Joint.LEFT_HEEL,
                    Joint.RIGHT_HEEL,
                    Joint.LEFT_FOOT_INDEX,
                    Joint.RIGHT_FOOT_INDEX,
                    space = CoordinateSpace.WORLD,
                    minVisibility = minVisibility,
                )
            if (pts is GeometryResult.Failure) {
                if (firstFailure == null) firstFailure = pts
                continue
            }
            val map = (pts as GeometryResult.Success).value
            val h = map.getValue(Joint.RIGHT_HIP).point - map.getValue(Joint.LEFT_HIP).point
            val crossProj = AngleGeometry.projectOntoPlane(upUnit.cross(h), upUnit)
            val leftFootProj =
                AngleGeometry.projectOntoPlane(
                    map.getValue(Joint.LEFT_FOOT_INDEX).point - map.getValue(Joint.LEFT_HEEL).point,
                    upUnit,
                )
            val rightFootProj =
                AngleGeometry.projectOntoPlane(
                    map.getValue(Joint.RIGHT_FOOT_INDEX).point - map.getValue(Joint.RIGHT_HEEL).point,
                    upUnit,
                )
            if (crossProj is GeometryResult.Success &&
                leftFootProj is GeometryResult.Success &&
                rightFootProj is GeometryResult.Success
            ) {
                crossVectors.add(crossProj.value)
                footVectors.add(leftFootProj.value)
                footVectors.add(rightFootProj.value)
            }
        }
        return resolveSignFromMedians(crossVectors, footVectors, firstFailure)
    }

    private fun resolveSignFromMedians(
        crossVectors: List<Vec3>,
        footVectors: List<Vec3>,
        firstFailure: GeometryResult.Failure?,
    ): GeometryResult<Double> {
        if (crossVectors.isEmpty() || footVectors.isEmpty()) {
            return firstFailure ?: GeometryResult.Failure(
                reason = UnavailableReason.DEGENERATE_GEOMETRY,
                message = "Insufficient foot/hip vectors to determine forward sign",
            )
        }
        val medCross = SignalStats.medianVec3(crossVectors)
        val medFoot = SignalStats.medianVec3(footVectors)
        return when {
            medCross is GeometryResult.Failure -> medCross
            medFoot is GeometryResult.Failure -> medFoot
            medCross is GeometryResult.Success && medFoot is GeometryResult.Success -> {
                val dot = medCross.value.dot(medFoot.value)
                if (abs(dot) <= AnalysisConfig.EPS_DOT) {
                    GeometryResult.Failure(
                        reason = UnavailableReason.DEGENERATE_GEOMETRY,
                        message = "Foot direction orthogonal or zero relative to u x h (dot=$dot)",
                    )
                } else {
                    GeometryResult.Success(if (dot > 0.0) 1.0 else -1.0)
                }
            }
            else -> GeometryResult.Failure(UnavailableReason.DEGENERATE_GEOMETRY, "Unexpected sign failure")
        }
    }

    private fun normalizeProjected(
        vector: Vec3,
        up: Vec3,
        visibility: Float,
    ): GeometryResult<ReliablePoint> =
        when (val proj = AngleGeometry.projectOntoPlane(vector, up)) {
            is GeometryResult.Failure -> proj
            is GeometryResult.Success ->
                when (val unit = proj.value.normalized()) {
                    is GeometryResult.Failure -> unit
                    is GeometryResult.Success -> GeometryResult.Success(ReliablePoint(unit.value, visibility))
                }
        }
}
