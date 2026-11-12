package mx.taekwondo.poomsae.analysis.geometry

import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.Landmark
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import kotlin.math.cos
import kotlin.math.sin

internal data class TransformSpec(
    val yawDeg: Double = 0.0,
    val pitchDeg: Double = 0.0,
    val rollDeg: Double = 0.0,
    val scale: Double = 1.0,
    val offset: Vec3 = Vec3.ZERO,
)

internal object TestSkeletonHelper {
    /**
     * Builds an upright reference skeleton standing at origin facing +Z with +Y up:
     * - LEFT anatomical side at -X, RIGHT anatomical side at +X
     * - Feet/Toes pointing toward +Z from Heel to FootIndex
     */
    fun uprightWorldFrame(
        timestampMs: Long = 1000L,
        visibility: Float = 0.95f,
    ): PoseFrame {
        val world =
            mapOf(
                Joint.NOSE to landmark(0.0f, 1.65f, 0.05f, visibility),
                Joint.LEFT_SHOULDER to landmark(-0.20f, 1.45f, 0.0f, visibility),
                Joint.RIGHT_SHOULDER to landmark(0.20f, 1.45f, 0.0f, visibility),
                Joint.LEFT_ELBOW to landmark(-0.22f, 1.15f, 0.0f, visibility),
                Joint.RIGHT_ELBOW to landmark(0.22f, 1.15f, 0.0f, visibility),
                Joint.LEFT_WRIST to landmark(-0.22f, 0.85f, 0.0f, visibility),
                Joint.RIGHT_WRIST to landmark(0.22f, 0.85f, 0.0f, visibility),
                Joint.LEFT_HIP to landmark(-0.15f, 0.95f, 0.0f, visibility),
                Joint.RIGHT_HIP to landmark(0.15f, 0.95f, 0.0f, visibility),
                Joint.LEFT_KNEE to landmark(-0.15f, 0.50f, 0.0f, visibility),
                Joint.RIGHT_KNEE to landmark(0.15f, 0.50f, 0.0f, visibility),
                Joint.LEFT_ANKLE to landmark(-0.15f, 0.10f, 0.0f, visibility),
                Joint.RIGHT_ANKLE to landmark(0.15f, 0.10f, 0.0f, visibility),
                Joint.LEFT_HEEL to landmark(-0.15f, 0.02f, -0.05f, visibility),
                Joint.RIGHT_HEEL to landmark(0.15f, 0.02f, -0.05f, visibility),
                Joint.LEFT_FOOT_INDEX to landmark(-0.15f, 0.02f, 0.15f, visibility),
                Joint.RIGHT_FOOT_INDEX to landmark(0.15f, 0.02f, 0.15f, visibility),
            )
        return PoseFrame(timestampMs = timestampMs, image = world, world = world)
    }

    fun landmark(
        x: Float,
        y: Float,
        z: Float,
        visibility: Float = 1.0f,
    ): Landmark = Landmark(x = x, y = y, z = z, visibility = visibility)

    /**
     * Rotates a vector around Y axis by [angleDeg] (right-handed counter-clockwise looking down +Y).
     * Turning right (clockwise looking down +Y) corresponds to angleDeg = -90°.
     */
    fun rotateY(
        v: Vec3,
        angleDeg: Double,
    ): Vec3 {
        val rad = Math.toRadians(angleDeg)
        val c = cos(rad)
        val s = sin(rad)
        return Vec3(
            x = v.x * c + v.z * s,
            y = v.y,
            z = -v.x * s + v.z * c,
        )
    }

    /**
     * Applies an arbitrary rigid 3D Euler rotation (yaw around Y, pitch around X, roll around Z),
     * uniform scale [scale], and translation [offset].
     */
    fun rigidTransform(
        v: Vec3,
        spec: TransformSpec = TransformSpec(),
    ): Vec3 {
        val y1 = rotateY(v, spec.yawDeg)
        val pRad = Math.toRadians(spec.pitchDeg)
        val cp = cos(pRad)
        val sp = sin(pRad)
        val p1 = Vec3(y1.x, y1.y * cp - y1.z * sp, y1.y * sp + y1.z * cp)

        val rRad = Math.toRadians(spec.rollDeg)
        val cr = cos(rRad)
        val sr = sin(rRad)
        val r1 = Vec3(p1.x * cr - p1.y * sr, p1.x * sr + p1.y * cr, p1.z)

        return (r1 * spec.scale) + spec.offset
    }

    fun transformFrame(
        frame: PoseFrame,
        spec: TransformSpec = TransformSpec(),
    ): PoseFrame {
        val transformedWorld =
            frame.world.mapValues { (_, lm) ->
                val p = rigidTransform(lm.toVec3(), spec)
                Landmark(p.x.toFloat(), p.y.toFloat(), p.z.toFloat(), lm.visibility)
            }
        return frame.copy(world = transformedWorld)
    }
}
