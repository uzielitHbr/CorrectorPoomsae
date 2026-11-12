package mx.taekwondo.poomsae.analysis.evaluation

import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.poomsae.EvaluationMode
import mx.taekwondo.poomsae.analysis.poomsae.MetricId
import mx.taekwondo.poomsae.analysis.poomsae.Movement
import mx.taekwondo.poomsae.analysis.poomsae.Side
import mx.taekwondo.poomsae.analysis.poomsae.Stance
import mx.taekwondo.poomsae.analysis.poomsae.StanceType
import mx.taekwondo.poomsae.analysis.poomsae.Technique
import mx.taekwondo.poomsae.analysis.poomsae.TechniqueType
import mx.taekwondo.poomsae.analysis.testutil.SkeletonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MovementEvaluatorTest {
    private val profile = SkeletonBuilder.defaultProfile()

    private fun sampleMovement(
        stanceType: StanceType = StanceType.AS,
        stanceSide: Side = Side.LEFT,
        techniqueType: TechniqueType = TechniqueType.MOMTONG_JIREUGI,
        techniqueSide: Side = Side.RIGHT,
    ): Movement =
        Movement(
            index = 2,
            expectedTurnDeg = 0,
            turnDegrees = 0,
            sharesStanceWithPrevious = false,
            stance = Stance(stanceType, stanceSide),
            technique = Technique(techniqueType, techniqueSide),
            extra = null,
            evaluationMode = EvaluationMode.HOLD,
            activeJoints = listOf(Joint.RIGHT_WRIST, Joint.LEFT_WRIST),
            kihap = false,
            nameKo = "momtong jireugi",
            nameEs = "Puñetazo medio",
        )

    @Test
    fun `ideal execution receives high score and CORRECT category`() {
        val mov = sampleMovement()
        // Paso ideal AS (~0.90m = 1.0 * lLeg), codo derecho casi extendido (170°), torso erguido
        val frame =
            SkeletonBuilder()
                .setStanceStep(frontLegLeft = true, stepLength = 0.90)
                .setArmTechnique(isRightArm = true, elbowAngleDeg = 170.0)
                .build()

        val eval = MovementEvaluator.evaluate(frame, mov, profile)
        assertEquals(Evaluability.EVALUABLE, eval.evaluability)
        assertEquals(MovementCategory.CORRECT, eval.category)
        assertNotNull(eval.score)
        assertTrue("Puntaje esperado >= 75.0, fue ${eval.score}", (eval.score ?: 0.0) >= 75.0)
    }

    @Test
    fun `monotonicity principle is strictly respected as deviation increases`() {
        val mov = sampleMovement()

        // Distancias de paso crecientes:
        // Ideal: 0.90m (ratio 1.0, dentro de [0.9, 1.1])
        // Moderado: 1.15m (ratio ~1.28, dentro de [1.1, 1.4])
        // Excesivo: 1.45m (ratio ~1.61, fuera de banda exterior > 1.4)
        val frameIdeal =
            SkeletonBuilder()
                .setStanceStep(frontLegLeft = true, stepLength = 0.90)
                .setArmTechnique(isRightArm = true, elbowAngleDeg = 170.0)
                .build()

        val frameModerate =
            SkeletonBuilder()
                .setStanceStep(frontLegLeft = true, stepLength = 1.15)
                .setArmTechnique(isRightArm = true, elbowAngleDeg = 170.0)
                .build()

        val frameExcessive =
            SkeletonBuilder()
                .setStanceStep(frontLegLeft = true, stepLength = 1.45)
                .setArmTechnique(isRightArm = true, elbowAngleDeg = 170.0)
                .build()

        val evalIdeal = MovementEvaluator.evaluate(frameIdeal, mov, profile)
        val evalModerate = MovementEvaluator.evaluate(frameModerate, mov, profile)
        val evalExcessive = MovementEvaluator.evaluate(frameExcessive, mov, profile)

        val sIdeal = evalIdeal.score ?: 0.0
        val sModerate = evalModerate.score ?: 0.0
        val sExcessive = evalExcessive.score ?: 0.0

        assertTrue("Monotonía violada: ideal ($sIdeal) debe >= moderado ($sModerate)", sIdeal >= sModerate)
        assertTrue("Monotonía violada: moderado ($sModerate) debe >= excesivo ($sExcessive)", sModerate >= sExcessive)
    }

    @Test
    fun `low visibility on critical metric causes NOT_EVALUABLE with null score`() {
        val mov = sampleMovement()
        // Ocultamos la rodilla delantera (articulación crítica para ST_03)
        val frame =
            SkeletonBuilder()
                .setJointVisibility(Joint.LEFT_KNEE, 0.2f)
                .build()

        val eval = MovementEvaluator.evaluate(frame, mov, profile)
        assertEquals(Evaluability.NOT_EVALUABLE, eval.evaluability)
        assertNull(eval.category)
        assertNull(eval.score)
    }

    @Test
    fun `symmetry principle holds for left and right execution of identical technique`() {
        val movRight = sampleMovement(techniqueType = TechniqueType.MOMTONG_JIREUGI, techniqueSide = Side.RIGHT)
        val movLeft = sampleMovement(techniqueType = TechniqueType.MOMTONG_JIREUGI, techniqueSide = Side.LEFT)

        val frameRight =
            SkeletonBuilder()
                .setArmTechnique(isRightArm = true, elbowAngleDeg = 168.0)
                .build()

        val frameLeft =
            SkeletonBuilder()
                .setArmTechnique(isRightArm = false, elbowAngleDeg = 168.0)
                .build()

        val evalRight = MovementEvaluator.evaluate(frameRight, movRight, profile)
        val evalLeft = MovementEvaluator.evaluate(frameLeft, movLeft, profile)

        assertNotNull(evalRight.score)
        assertNotNull(evalLeft.score)
        // La diferencia de puntaje debe ser mínima (simetría)
        val diff = abs((evalRight.score ?: 0.0) - (evalLeft.score ?: 0.0))
        assertTrue("Simetría violada: diff = $diff", diff < 1.0)
    }

    @Test
    fun `critical metric exceeding outer limits produces INCORRECT category`() {
        val mov = sampleMovement()
        // Paso fuera del límite exterior 1.4 de AS (1.55m = 1.72 * lLeg) con técnica válida de brazo
        val frame =
            SkeletonBuilder()
                .setStanceStep(frontLegLeft = true, stepLength = 1.55)
                .setArmTechnique(isRightArm = true, elbowAngleDeg = 170.0)
                .build()

        val eval = MovementEvaluator.evaluate(frame, mov, profile)
        assertEquals(MovementCategory.INCORRECT, eval.category)
    }

    @Test
    fun `feedback item is generated when technical metric deviates from ideal band`() {
        val mov = sampleMovement()
        // Puñetazo con codo excesivamente flexionado (120° en lugar de 160-178°)
        val frame =
            SkeletonBuilder()
                .setArmTechnique(isRightArm = true, elbowAngleDeg = 120.0)
                .build()

        val eval = MovementEvaluator.evaluate(frame, mov, profile)
        assertTrue(eval.feedback.isNotEmpty())
        val elbowFeedback = eval.feedback.find { it.metricId == MetricId.AR_01 }
        assertNotNull(elbowFeedback)
        assertEquals(FeedbackCode.PUNCH_ARM_NOT_EXTENDED, elbowFeedback?.code)
    }
}
