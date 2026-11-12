package mx.taekwondo.poomsae.analysis.geometry

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.model.UnavailableReason
import kotlin.math.sqrt

/**
 * Immutable 2D Euclidean vector in `Double` precision.
 */
data class Vec2(
    val x: Double,
    val y: Double,
) {
    /** Vector addition (`this + other`). */
    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)

    /** Vector subtraction (`this - other`). */
    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)

    /** Scalar multiplication (`this * scalar`). */
    operator fun times(scalar: Double): Vec2 = Vec2(x * scalar, y * scalar)

    /** Dot product (`this · other`). */
    fun dot(other: Vec2): Double = x * other.x + y * other.y

    /** Signed 2D cross product determinant (`x1 * y2 - y1 * x2`). */
    fun cross(other: Vec2): Double = x * other.y - y * other.x

    /** Squared Euclidean norm (`||this||²`). */
    fun normSquared(): Double = x * x + y * y

    /** Euclidean norm (`||this||`). */
    fun norm(): Double = sqrt(normSquared())

    /** Returns true if both components are finite (not NaN or Infinite). */
    fun isFinite(): Boolean = x.isFinite() && y.isFinite()

    /**
     * Returns the unit-length 2D vector, or [GeometryResult.Failure] when non-finite or zero-length.
     */
    fun normalized(eps: Double = AnalysisConfig.EPS_NORM): GeometryResult<Vec2> {
        val magnitude = norm()
        return when {
            !isFinite() ->
                GeometryResult.Failure(
                    reason = UnavailableReason.OUT_OF_RANGE,
                    message = "Cannot normalize 2D vector with non-finite components: $this",
                )
            !magnitude.isFinite() ->
                GeometryResult.Failure(
                    reason = UnavailableReason.OUT_OF_RANGE,
                    message = "2D vector norm overflowed or is non-finite: $this",
                )
            magnitude <= eps ->
                GeometryResult.Failure(
                    reason = UnavailableReason.DEGENERATE_GEOMETRY,
                    message = "Cannot normalize zero-length 2D vector (norm=$magnitude <= eps=$eps)",
                )
            else -> GeometryResult.Success(Vec2(x / magnitude, y / magnitude))
        }
    }

    /** Companion constants. */
    companion object {
        /** Zero vector `(0, 0)`. */
        val ZERO: Vec2 = Vec2(0.0, 0.0)
    }
}
