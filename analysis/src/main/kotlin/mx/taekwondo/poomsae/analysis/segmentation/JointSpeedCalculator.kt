package mx.taekwondo.poomsae.analysis.segmentation

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.model.CoordinateSpace
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import kotlin.math.hypot

/**
 * Calculador de velocidad articular normalizada según la especificación (Sección 12.3).
 *
 * La velocidad de cada articulación se computa en el plano de imagen normalizada por el tiempo
 * transcurrido y la altura instantánea del torso:
 * speed(j, t) = ||Q_j(t) - Q_j(t - dt)|| / (dt * T_img(t))
 *
 * Expresada en torsos/segundo, la velocidad es adimensional e invariante a la distancia del
 * atleta respecto a la cámara.
 */
object JointSpeedCalculator {
    private const val HALF = 0.5

    /**
     * Calcula las velocidades normalizadas para todas las articulaciones presentes en ambos cuadros.
     */
    fun computeJointSpeeds(
        currentFrame: PoseFrame,
        previousFrame: PoseFrame?,
        profileTorsoHeight: Double? = null,
    ): Map<Joint, Double> {
        val dtMs = currentFrame.timestampMs - (previousFrame?.timestampMs ?: 0L)
        val tImg = resolveTorsoHeight(currentFrame, previousFrame, profileTorsoHeight)
        if (previousFrame == null || dtMs <= 0L || tImg <= AnalysisConfig.EPS_NORM) {
            return emptyMap()
        }

        val dtSec = dtMs / AnalysisConfig.MILLIS_PER_SECOND
        val currentLandmarks = currentFrame.landmarks(CoordinateSpace.IMAGE)
        val previousLandmarks = previousFrame.landmarks(CoordinateSpace.IMAGE)

        val normalizationFactor = dtSec * tImg
        val speeds = mutableMapOf<Joint, Double>()

        for (joint in Joint.entries) {
            val pCurr = currentLandmarks[joint]
            val pPrev = previousLandmarks[joint]
            if (pCurr != null && pPrev != null) {
                val dx = pCurr.x - pPrev.x
                val dy = pCurr.y - pPrev.y
                val distance = hypot(dx, dy)
                val speed = distance / normalizationFactor
                speeds[joint] = if (speed.isFinite()) speed else 0.0
            }
        }
        return speeds
    }

    /**
     * Calcula la velocidad máxima instantánea sobre el conjunto de articulaciones activas del movimiento.
     */
    fun computeActiveSpeed(
        currentFrame: PoseFrame,
        previousFrame: PoseFrame?,
        activeJoints: Collection<Joint>,
        profileTorsoHeight: Double? = null,
    ): Double {
        if (activeJoints.isEmpty() || previousFrame == null) return 0.0
        val jointSpeeds = computeJointSpeeds(currentFrame, previousFrame, profileTorsoHeight)
        return activeJoints.mapNotNull { jointSpeeds[it] }.maxOrNull() ?: 0.0
    }

    /**
     * Resuelve la altura del torso en coordenadas de imagen (T_img) entre el punto medio biacromial
     * y el punto medio de caderas.
     */
    fun resolveTorsoHeight(
        currentFrame: PoseFrame,
        previousFrame: PoseFrame?,
        fallbackTorsoHeight: Double? = null,
    ): Double {
        val currentTorso = estimateTorsoHeightFromImage(currentFrame)
        if (currentTorso > AnalysisConfig.EPS_NORM) return currentTorso

        val prevTorso = previousFrame?.let { estimateTorsoHeightFromImage(it) } ?: 0.0
        return when {
            prevTorso > AnalysisConfig.EPS_NORM -> prevTorso
            fallbackTorsoHeight != null && fallbackTorsoHeight > AnalysisConfig.EPS_NORM -> fallbackTorsoHeight
            else -> 1.0
        }
    }

    private fun estimateTorsoHeightFromImage(frame: PoseFrame): Double {
        val img = frame.landmarks(CoordinateSpace.IMAGE)
        val sL = img[Joint.LEFT_SHOULDER]
        val sR = img[Joint.RIGHT_SHOULDER]
        val hL = img[Joint.LEFT_HIP]
        val hR = img[Joint.RIGHT_HIP]

        val required = listOf(sL, sR, hL, hR)
        if (required.any { it == null }) return 0.0

        val sMidX = (sL!!.x + sR!!.x) * HALF
        val sMidY = (sL.y + sR.y) * HALF
        val hMidX = (hL!!.x + hR!!.x) * HALF
        val hMidY = (hL.y + hR.y) * HALF

        return hypot(sMidX - hMidX, sMidY - hMidY)
    }
}
