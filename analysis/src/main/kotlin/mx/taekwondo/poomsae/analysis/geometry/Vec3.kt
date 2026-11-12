package mx.taekwondo.poomsae.analysis.geometry

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.model.UnavailableReason
import kotlin.math.sqrt

/**
 * Immutable 3D Euclidean vector in `Double` precision for pure biomechanical geometry.
 */
data class Vec3(
    val x: Double,
    val y: Double,
    val z: Double,
) {
    /** Vector addition (`this + other`). */
    operator fun plus(other: Vec3): Vec3 = Vec3(x + other.x, y + other.y, z + other.z)

    /** Vector subtraction (`this - other`). */
    operator fun minus(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)

    /** Scalar multiplication (`this * scalar`). */
    operator fun times(scalar: Double): Vec3 = Vec3(x * scalar, y * scalar, z * scalar)

    /** Dot product (`this · other`). */
    fun dot(other: Vec3): Double = x * other.x + y * other.y + z * other.z

    /** Cross product (`this × other`). */
    fun cross(other: Vec3): Vec3 =
        Vec3(
            x = y * other.z - z * other.y,
            y = z * other.x - x * other.z,
            z = x * other.y - y * other.x,
        )

    /** Squared Euclidean norm (`||this||²`). */
    fun normSquared(): Double = x * x + y * y + z * z

    /** Euclidean norm (`||this||`). */
    fun norm(): Double = sqrt(normSquared())

    /** Returns true if all three components are finite (not NaN or Infinite). */
    fun isFinite(): Boolean = x.isFinite() && y.isFinite() && z.isFinite()

    /**
     * Returns the unit-length vector in the same direction, or an explicit [GeometryResult.Failure]
     * when the vector contains non-finite values or has norm `<= eps` (never returns silent NaN).
     */
    fun normalized(eps: Double = AnalysisConfig.EPS_NORM): GeometryResult<Vec3> {
        val magnitude = norm()
        return when {
            !isFinite() ->
                GeometryResult.Failure(
                    reason = UnavailableReason.OUT_OF_RANGE,
                    message = "Cannot normalize vector with non-finite components: $this",
                )
            !magnitude.isFinite() ->
                GeometryResult.Failure(
                    reason = UnavailableReason.OUT_OF_RANGE,
                    message = "Vector norm overflowed or is non-finite: $this",
                )
            magnitude <= eps ->
                GeometryResult.Failure(
                    reason = UnavailableReason.DEGENERATE_GEOMETRY,
                    message = "Cannot normalize zero-length or near-zero vector (norm=$magnitude <= eps=$eps)",
                )
            else -> GeometryResult.Success(Vec3(x / magnitude, y / magnitude, z / magnitude))
        }
    }

    /** Companion constants and midpoint constructor. */
    companion object {
        private const val HALF: Double = 0.5

        /** Zero vector `(0, 0, 0)`. */
        val ZERO: Vec3 = Vec3(0.0, 0.0, 0.0)

        /** Computes the midpoint `(a + b) * 0.5`. */
        fun midpoint(
            a: Vec3,
            b: Vec3,
        ): Vec3 = Vec3((a.x + b.x) * HALF, (a.y + b.y) * HALF, (a.z + b.z) * HALF)
    }
}
