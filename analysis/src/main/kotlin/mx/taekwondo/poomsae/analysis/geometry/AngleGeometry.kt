package mx.taekwondo.poomsae.analysis.geometry

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.model.CoordinateSpace
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.MetricResult
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.model.UnavailableReason
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.min

/**
 * Pure 2D and 3D geometric primitives (distances, angles, planar projections, and signed turn angles).
 *
 * All operations validate finiteness and non-degeneracy, returning explicit errors instead of `NaN`.
 */
object AngleGeometry {
    private const val HALF_CIRCLE_DEG: Double = 180.0
    private const val FULL_CIRCLE_DEG: Double = 360.0
    private const val RAD_TO_DEG: Double = HALF_CIRCLE_DEG / PI

    /**
     * Computes Euclidean distance `||a - b||` in 3D space.
     * Returns [MetricResult.Unavailable] with [UnavailableReason.OUT_OF_RANGE] if inputs are non-finite.
     */
    fun distance3D(
        a: Vec3,
        b: Vec3,
        confidence: Float = 1.0f,
    ): MetricResult {
        val dist = (a - b).norm()
        return if (!a.isFinite() || !b.isFinite() || !dist.isFinite()) {
            MetricResult.Unavailable(UnavailableReason.OUT_OF_RANGE)
        } else {
            MetricResult.Value(value = dist, confidence = confidence)
        }
    }

    /**
     * Computes Euclidean distance `||a - b||` in 2D space.
     * Returns [MetricResult.Unavailable] with [UnavailableReason.OUT_OF_RANGE] if inputs are non-finite.
     */
    fun distance2D(
        a: Vec2,
        b: Vec2,
        confidence: Float = 1.0f,
    ): MetricResult {
        val dist = (a - b).norm()
        return if (!a.isFinite() || !b.isFinite() || !dist.isFinite()) {
            MetricResult.Unavailable(UnavailableReason.OUT_OF_RANGE)
        } else {
            MetricResult.Value(value = dist, confidence = confidence)
        }
    }

    /**
     * Computes the interior angle `θ(a, b, c)` in degrees `[0.0, 180.0]` at vertex [b] in 3D (Section 13.2):
     * `arccos(clamp(((a - b) · (c - b)) / (||a - b|| * ||c - b||), -1, 1)) * 180 / π`.
     */
    fun angleBetweenPoints3D(
        a: Vec3,
        b: Vec3,
        c: Vec3,
        confidence: Float = 1.0f,
    ): MetricResult {
        if (!a.isFinite() || !b.isFinite() || !c.isFinite()) {
            return MetricResult.Unavailable(UnavailableReason.OUT_OF_RANGE)
        }
        val uResult = (a - b).normalized()
        val vResult = (c - b).normalized()
        return when {
            uResult is GeometryResult.Failure -> MetricResult.Unavailable(uResult.reason)
            vResult is GeometryResult.Failure -> MetricResult.Unavailable(vResult.reason)
            uResult is GeometryResult.Success && vResult is GeometryResult.Success -> {
                val cosine = uResult.value.dot(vResult.value).coerceIn(-1.0, 1.0)
                val angleDeg = acos(cosine) * RAD_TO_DEG
                MetricResult.Value(value = angleDeg, confidence = confidence)
            }
            else -> MetricResult.Unavailable(UnavailableReason.DEGENERATE_GEOMETRY)
        }
    }

    /**
     * Computes the interior angle `θ(a, b, c)` in degrees `[0.0, 180.0]` at vertex [b] in 2D.
     */
    fun angleBetweenPoints2D(
        a: Vec2,
        b: Vec2,
        c: Vec2,
        confidence: Float = 1.0f,
    ): MetricResult {
        if (!a.isFinite() || !b.isFinite() || !c.isFinite()) {
            return MetricResult.Unavailable(UnavailableReason.OUT_OF_RANGE)
        }
        val uResult = (a - b).normalized()
        val vResult = (c - b).normalized()
        return when {
            uResult is GeometryResult.Failure -> MetricResult.Unavailable(uResult.reason)
            vResult is GeometryResult.Failure -> MetricResult.Unavailable(vResult.reason)
            uResult is GeometryResult.Success && vResult is GeometryResult.Success -> {
                val cosine = uResult.value.dot(vResult.value).coerceIn(-1.0, 1.0)
                val angleDeg = acos(cosine) * RAD_TO_DEG
                MetricResult.Value(value = angleDeg, confidence = confidence)
            }
            else -> MetricResult.Unavailable(UnavailableReason.DEGENERATE_GEOMETRY)
        }
    }

    /**
     * Computes the angle in degrees `[0.0, 180.0]` between [vector] and the vertical unit vector [up].
     * Returns `0°` when [vector] points in the exact direction of [up].
     */
    fun angleWithVertical(
        vector: Vec3,
        up: Vec3,
        confidence: Float = 1.0f,
    ): MetricResult {
        val vNorm = vector.normalized()
        val uNorm = up.normalized()
        return when {
            vNorm is GeometryResult.Failure -> MetricResult.Unavailable(vNorm.reason)
            uNorm is GeometryResult.Failure -> MetricResult.Unavailable(uNorm.reason)
            vNorm is GeometryResult.Success && uNorm is GeometryResult.Success -> {
                val cosine = vNorm.value.dot(uNorm.value).coerceIn(-1.0, 1.0)
                MetricResult.Value(value = acos(cosine) * RAD_TO_DEG, confidence = confidence)
            }
            else -> MetricResult.Unavailable(UnavailableReason.DEGENERATE_GEOMETRY)
        }
    }

    /**
     * Projects [vector] onto the plane perpendicular to [planeNormal] (Section 13.2):
     * `G(v) = v - (v · u) * u`, normalizing [planeNormal] first.
     */
    fun projectOntoPlane(
        vector: Vec3,
        planeNormal: Vec3,
    ): GeometryResult<Vec3> {
        if (!vector.isFinite()) {
            return GeometryResult.Failure(
                reason = UnavailableReason.OUT_OF_RANGE,
                message = "Cannot project non-finite vector: $vector",
            )
        }
        return when (val normalUnit = planeNormal.normalized()) {
            is GeometryResult.Failure -> normalUnit
            is GeometryResult.Success -> {
                val u = normalUnit.value
                GeometryResult.Success(vector - u * vector.dot(u))
            }
        }
    }

    /**
     * Computes the signed angle in degrees `(-180.0, 180.0]` from [from] to [to] projected onto
     * the plane orthogonal to [planeNormal], following the right-hand rule around [planeNormal].
     */
    fun signedAngleInPlane(
        from: Vec3,
        to: Vec3,
        planeNormal: Vec3,
        confidence: Float = 1.0f,
    ): MetricResult {
        val uResult = planeNormal.normalized()
        if (uResult is GeometryResult.Failure) {
            return MetricResult.Unavailable(uResult.reason)
        }
        val u = (uResult as GeometryResult.Success).value
        val fromProj = projectOntoPlane(from, u)
        val toProj = projectOntoPlane(to, u)
        return computeSignedAngleNormalized(fromProj, toProj, u, confidence)
    }

    private fun computeSignedAngleNormalized(
        fromProj: GeometryResult<Vec3>,
        toProj: GeometryResult<Vec3>,
        u: Vec3,
        confidence: Float,
    ): MetricResult =
        when {
            fromProj is GeometryResult.Failure -> MetricResult.Unavailable(fromProj.reason)
            toProj is GeometryResult.Failure -> MetricResult.Unavailable(toProj.reason)
            fromProj is GeometryResult.Success && toProj is GeometryResult.Success -> {
                val aUnit = fromProj.value.normalized()
                val bUnit = toProj.value.normalized()
                when {
                    aUnit is GeometryResult.Failure -> MetricResult.Unavailable(aUnit.reason)
                    bUnit is GeometryResult.Failure -> MetricResult.Unavailable(bUnit.reason)
                    aUnit is GeometryResult.Success && bUnit is GeometryResult.Success -> {
                        val cosTheta = aUnit.value.dot(bUnit.value).coerceIn(-1.0, 1.0)
                        val sinTheta = aUnit.value.cross(bUnit.value).dot(u)
                        val deg = atan2(sinTheta, cosTheta) * RAD_TO_DEG
                        MetricResult.Value(value = wrapDegrees(deg), confidence = confidence)
                    }
                    else -> MetricResult.Unavailable(UnavailableReason.DEGENERATE_GEOMETRY)
                }
            }
            else -> MetricResult.Unavailable(UnavailableReason.DEGENERATE_GEOMETRY)
        }

    /**
     * Computes interior joint angle `θ(a, b, c)` in degrees from [frame] world coordinates,
     * validating presence, finiteness, visibility `>= minVisibility`, and bone lengths.
     */
    fun jointAngle3D(
        frame: PoseFrame,
        a: Joint,
        b: Joint,
        c: Joint,
        minVisibility: Float = AnalysisConfig.VIS_MIN,
    ): MetricResult =
        when (
            val extracted =
                frame.extractReliablePoints(
                    a,
                    b,
                    c,
                    space = CoordinateSpace.WORLD,
                    minVisibility = minVisibility,
                )
        ) {
            is GeometryResult.Failure -> MetricResult.Unavailable(extracted.reason)
            is GeometryResult.Success -> {
                val pA = extracted.value.getValue(a)
                val pB = extracted.value.getValue(b)
                val pC = extracted.value.getValue(c)
                val minConfidence = min(pA.visibility, min(pB.visibility, pC.visibility))
                angleBetweenPoints3D(pA.point, pB.point, pC.point, minConfidence)
            }
        }

    /**
     * Wraps an angle in degrees into the canonical half-open interval `(-180.0, 180.0]` (Section 13.4).
     */
    fun wrapDegrees(angleDeg: Double): Double {
        require(angleDeg.isFinite()) { "Cannot wrap non-finite angle: $angleDeg" }
        val shifted = (angleDeg + HALF_CIRCLE_DEG).mod(FULL_CIRCLE_DEG) - HALF_CIRCLE_DEG
        return if (shifted <= -HALF_CIRCLE_DEG) HALF_CIRCLE_DEG else shifted
    }
}
