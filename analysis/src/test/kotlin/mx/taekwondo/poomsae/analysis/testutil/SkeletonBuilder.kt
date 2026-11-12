package mx.taekwondo.poomsae.analysis.testutil

import mx.taekwondo.poomsae.analysis.geometry.Vec3
import mx.taekwondo.poomsae.analysis.model.BodyProfile
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.Landmark
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import kotlin.math.cos
import kotlin.math.sin

/**
 * Generador de esqueletos 3D sintéticos para pruebas unitarias deterministas (Sección 13.11).
 *
 * Permite posicionar articulaciones con ángulos exactos de flexión y extensiones métricas
 * para validar la monotonía de puntuación, la simetría y el manejo de baja visibilidad.
 */
class SkeletonBuilder {
    private val worldJoints = mutableMapOf<Joint, Vec3>()
    private val jointVisibilities = mutableMapOf<Joint, Float>()

    init {
        // Postura erguida neutra por defecto (anchos y longitudes humanas estándar)
        // Hombros W = 0.40m, Torso T = 0.50m, Pierna lLeg = 0.90m (muslo 0.45, espinilla 0.45)
        worldJoints[Joint.NOSE] = Vec3(0.0, 0.70, 0.0)
        worldJoints[Joint.LEFT_SHOULDER] = Vec3(-0.20, 0.50, 0.0)
        worldJoints[Joint.RIGHT_SHOULDER] = Vec3(0.20, 0.50, 0.0)
        worldJoints[Joint.LEFT_ELBOW] = Vec3(-0.20, 0.20, 0.0)
        worldJoints[Joint.RIGHT_ELBOW] = Vec3(0.20, 0.20, 0.0)
        worldJoints[Joint.LEFT_WRIST] = Vec3(-0.20, -0.10, 0.0)
        worldJoints[Joint.RIGHT_WRIST] = Vec3(0.20, -0.10, 0.0)

        worldJoints[Joint.LEFT_HIP] = Vec3(-0.12, 0.0, 0.0)
        worldJoints[Joint.RIGHT_HIP] = Vec3(0.12, 0.0, 0.0)
        worldJoints[Joint.LEFT_KNEE] = Vec3(-0.12, -0.45, 0.0)
        worldJoints[Joint.RIGHT_KNEE] = Vec3(0.12, -0.45, 0.0)
        worldJoints[Joint.LEFT_ANKLE] = Vec3(-0.12, -0.90, 0.0)
        worldJoints[Joint.RIGHT_ANKLE] = Vec3(0.12, -0.90, 0.0)
        worldJoints[Joint.LEFT_HEEL] = Vec3(-0.12, -0.90, -0.10)
        worldJoints[Joint.RIGHT_HEEL] = Vec3(0.12, -0.90, -0.10)
        worldJoints[Joint.LEFT_FOOT_INDEX] = Vec3(-0.12, -0.90, 0.15)
        worldJoints[Joint.RIGHT_FOOT_INDEX] = Vec3(0.12, -0.90, 0.15)

        for (joint in Joint.entries) {
            jointVisibilities[joint] = 1.0f
        }
    }

    fun setJointPosition(
        joint: Joint,
        position: Vec3,
    ): SkeletonBuilder {
        worldJoints[joint] = position
        return this
    }

    fun setJointVisibility(
        joint: Joint,
        visibility: Float,
    ): SkeletonBuilder {
        jointVisibilities[joint] = visibility
        return this
    }

    fun clone(): SkeletonBuilder {
        val copy = SkeletonBuilder()
        for ((joint, pos) in worldJoints) {
            copy.setJointPosition(joint, pos)
        }
        for ((joint, vis) in jointVisibilities) {
            copy.setJointVisibility(joint, vis)
        }
        return copy
    }

    /** Configura el paso (separación longitudinal y lateral de tobillos) y alinea rodillas. */
    fun setStanceStep(
        frontLegLeft: Boolean,
        stepLength: Double,
        stepWidth: Double = 0.24,
    ): SkeletonBuilder {
        val halfWidth = stepWidth * 0.5
        val halfLength = stepLength * 0.5
        if (frontLegLeft) {
            worldJoints[Joint.LEFT_ANKLE] = Vec3(-halfWidth, -0.90, halfLength)
            worldJoints[Joint.LEFT_KNEE] = Vec3(-halfWidth, -0.45, halfLength * 0.5 + 0.02)
            worldJoints[Joint.RIGHT_ANKLE] = Vec3(halfWidth, -0.90, -halfLength)
            worldJoints[Joint.RIGHT_KNEE] = Vec3(halfWidth, -0.45, -halfLength * 0.5)
        } else {
            worldJoints[Joint.RIGHT_ANKLE] = Vec3(halfWidth, -0.90, halfLength)
            worldJoints[Joint.RIGHT_KNEE] = Vec3(halfWidth, -0.45, halfLength * 0.5 + 0.02)
            worldJoints[Joint.LEFT_ANKLE] = Vec3(-halfWidth, -0.90, -halfLength)
            worldJoints[Joint.LEFT_KNEE] = Vec3(-halfWidth, -0.45, -halfLength * 0.5)
        }
        return this
    }

    /** Configura el ángulo de flexión de la rodilla delantera. */
    fun setFrontKneeAngle(
        frontLegLeft: Boolean,
        angleDeg: Double,
    ): SkeletonBuilder {
        val hipJoint = if (frontLegLeft) Joint.LEFT_HIP else Joint.RIGHT_HIP
        val kneeJoint = if (frontLegLeft) Joint.LEFT_KNEE else Joint.RIGHT_KNEE
        val ankleJoint = if (frontLegLeft) Joint.LEFT_ANKLE else Joint.RIGHT_ANKLE

        val hip = worldJoints[hipJoint] ?: Vec3.ZERO
        val rad = Math.toRadians(180.0 - angleDeg)
        val knee = hip + Vec3(0.0, -0.45 * cos(rad * 0.5), 0.45 * sin(rad * 0.5))
        val ankle = knee + Vec3(0.0, -0.45 * cos(rad * 0.5), -0.45 * sin(rad * 0.5))

        worldJoints[kneeJoint] = knee
        worldJoints[ankleJoint] = ankle
        return this
    }

    /** Configura el brazo con un ángulo de codo específico y orientación hacia adelante. */
    fun setArmTechnique(
        isRightArm: Boolean,
        elbowAngleDeg: Double,
        wristForwardRatio: Double = 1.0,
    ): SkeletonBuilder {
        val sJoint = if (isRightArm) Joint.RIGHT_SHOULDER else Joint.LEFT_SHOULDER
        val eJoint = if (isRightArm) Joint.RIGHT_ELBOW else Joint.LEFT_ELBOW
        val wJoint = if (isRightArm) Joint.RIGHT_WRIST else Joint.LEFT_WRIST

        val s = worldJoints[sJoint] ?: Vec3.ZERO
        val upperArmLen = 0.30
        val foreArmLen = 0.30
        val elbow = s + Vec3(0.0, -0.10, upperArmLen * 0.95)
        val rad = Math.toRadians(180.0 - elbowAngleDeg)
        val wrist = elbow + Vec3(0.0, -foreArmLen * sin(rad), foreArmLen * cos(rad) * wristForwardRatio)

        worldJoints[eJoint] = elbow
        worldJoints[wJoint] = wrist
        return this
    }

    fun build(timestampMs: Long = 1000L): PoseFrame {
        val worldMap = mutableMapOf<Joint, Landmark>()
        val imageMap = mutableMapOf<Joint, Landmark>()

        for (joint in Joint.entries) {
            val w = worldJoints[joint] ?: Vec3.ZERO
            val vis = jointVisibilities[joint] ?: 1.0f

            worldMap[joint] = Landmark(w.x.toFloat(), w.y.toFloat(), w.z.toFloat(), vis)
            val imgX = (0.5 + w.x * 0.3).toFloat().coerceIn(0.01f, 0.99f)
            val imgY = (0.5 - w.y * 0.3).toFloat().coerceIn(0.01f, 0.99f)
            val imgZ = (w.z * 0.3).toFloat()
            imageMap[joint] = Landmark(imgX, imgY, imgZ, vis)
        }

        return PoseFrame(
            timestampMs = timestampMs,
            image = imageMap,
            world = worldMap,
        )
    }

    companion object {
        fun defaultProfile(): BodyProfile =
            BodyProfile(
                lThigh = 0.45,
                lShank = 0.45,
                lLeg = 0.90,
                torsoLength = 0.50,
                shoulderWidth = 0.40,
                armLength = 0.60,
                dNoseShoulder = 0.20,
                upVector = Vec3(0.0, 1.0, 0.0),
                forwardSign = 1.0,
                referenceForward = Vec3(0.0, 0.0, 1.0),
                referenceHipAxis = Vec3(1.0, 0.0, 0.0),
                tImgMedian = 0.35,
            )
    }
}
