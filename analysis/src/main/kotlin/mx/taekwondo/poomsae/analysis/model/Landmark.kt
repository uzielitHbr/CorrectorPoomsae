package mx.taekwondo.poomsae.analysis.model

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.geometry.Vec3

/**
 * Immutable representation of a detected 3D body landmark (Section 5.4).
 *
 * @property x Coordinate along X (normalized image space `[0, 1]` or world space in meters).
 * @property y Coordinate along Y (normalized image space `[0, 1]` or world space in meters).
 * @property z Coordinate along Z (depth relative to hip origin or normalized image depth).
 * @property visibility Estimated confidence `[0.0, 1.0]` that the landmark is visible and unoccluded.
 */
data class Landmark(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float,
) {
    /** Returns true if all coordinates and visibility are finite (not NaN or Infinite). */
    fun isFinite(): Boolean = x.isFinite() && y.isFinite() && z.isFinite() && visibility.isFinite()

    /** Returns true if coordinates are finite and [visibility] meets [minVisibility]. */
    fun isReliable(minVisibility: Float = AnalysisConfig.VIS_MIN): Boolean = isFinite() && visibility >= minVisibility

    /** Converts the `(x, y, z)` coordinates to a high-precision [Vec3] in `Double` space. */
    fun toVec3(): Vec3 = Vec3(x.toDouble(), y.toDouble(), z.toDouble())
}
