package mx.taekwondo.poomsae.analysis.quality

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.model.CoordinateSpace
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.Landmark
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import kotlin.math.hypot

/**
 * Compuerta de calidad de encuadre en cumplimiento de la Fase F6 (Secciones 12.8 y 13.9).
 *
 * Rechaza cuadros con cuerpo incompleto, cortado por bordes o a distancias inadecuadas,
 * emitiendo mensajes de retroalimentación claros.
 */
object FrameQualityGate {
    private const val HALF_RATIO = 0.5

    private val DEFAULT_KEY_JOINTS =
        setOf(
            Joint.NOSE,
            Joint.LEFT_SHOULDER,
            Joint.RIGHT_SHOULDER,
            Joint.LEFT_HIP,
            Joint.RIGHT_HIP,
            Joint.LEFT_ANKLE,
            Joint.RIGHT_ANKLE,
        )

    /** Evalúa la calidad de un cuadro de pose antes de permitir su análisis. */
    fun evaluate(
        frame: PoseFrame?,
        requiredJoints: Set<Joint> = emptySet(),
    ): FrameQualityResult {
        if (frame == null) {
            return FrameQualityResult.Rejected(
                QualityRejectionCode.NO_PERSON,
                "Perdimos de vista tu cuerpo. Vuelve al encuadre para continuar.",
            )
        }

        val imageLandmarks = frame.landmarks(CoordinateSpace.IMAGE)
        val jointsToCheck = if (requiredJoints.isNotEmpty()) requiredJoints else DEFAULT_KEY_JOINTS

        return checkJointConfidence(imageLandmarks, jointsToCheck)
            ?: checkDistance(imageLandmarks)
            ?: checkMargins(imageLandmarks, jointsToCheck)
            ?: FrameQualityResult.Ok
    }

    private fun checkJointConfidence(
        landmarks: Map<Joint, Landmark>,
        joints: Set<Joint>,
    ): FrameQualityResult? {
        val hasLowConfidence =
            joints.any { joint ->
                val lm = landmarks[joint]
                lm == null || lm.visibility < AnalysisConfig.VIS_MIN || !lm.isFinite()
            }
        return if (hasLowConfidence) {
            FrameQualityResult.Rejected(
                QualityRejectionCode.LOW_CONFIDENCE,
                "No pudimos evaluar este movimiento con seguridad.",
            )
        } else {
            null
        }
    }

    private fun checkDistance(landmarks: Map<Joint, Landmark>): FrameQualityResult? {
        val sL = landmarks[Joint.LEFT_SHOULDER]
        val sR = landmarks[Joint.RIGHT_SHOULDER]
        val hL = landmarks[Joint.LEFT_HIP]
        val hR = landmarks[Joint.RIGHT_HIP]
        if (listOf(sL, sR, hL, hR).any { it == null }) return null

        val shoulderMidX = (sL!!.x + sR!!.x) * HALF_RATIO
        val shoulderMidY = (sL.y + sR.y) * HALF_RATIO
        val hipMidX = (hL!!.x + hR!!.x) * HALF_RATIO
        val hipMidY = (hL.y + hR.y) * HALF_RATIO

        val torsoHeight = hypot(shoulderMidX - hipMidX, shoulderMidY - hipMidY)

        return when {
            torsoHeight < AnalysisConfig.TORSO_MIN_HEIGHT_RATIO ->
                FrameQualityResult.Rejected(
                    QualityRejectionCode.FRAME_TOO_FAR,
                    "Acércate un poco al teléfono.",
                )
            torsoHeight > AnalysisConfig.TORSO_MAX_HEIGHT_RATIO ->
                FrameQualityResult.Rejected(
                    QualityRejectionCode.FRAME_TOO_CLOSE,
                    "Aléjate un poco para que se vea tu cuerpo completo.",
                )
            else -> null
        }
    }

    private fun checkMargins(
        landmarks: Map<Joint, Landmark>,
        joints: Set<Joint>,
    ): FrameQualityResult? {
        val minMargin = AnalysisConfig.FRAME_MARGIN_RATIO
        val maxMargin = 1.0 - minMargin

        val isCut =
            joints.any { joint ->
                val lm = landmarks[joint]
                lm != null && (lm.x < minMargin || lm.x > maxMargin || lm.y < minMargin || lm.y > maxMargin)
            }
        return if (isCut) {
            FrameQualityResult.Rejected(
                QualityRejectionCode.FRAME_CUT,
                "Mantén todo tu cuerpo dentro de la pantalla.",
            )
        } else {
            null
        }
    }
}
