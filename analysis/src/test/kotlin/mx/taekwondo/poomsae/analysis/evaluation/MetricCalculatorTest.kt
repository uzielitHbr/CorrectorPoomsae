package mx.taekwondo.poomsae.analysis.evaluation

import mx.taekwondo.poomsae.analysis.geometry.Vec3
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.MetricResult
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
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricCalculatorTest {
    private val profile = SkeletonBuilder.defaultProfile()

    private fun movement(
        tech: TechniqueType,
        arm: Side = Side.LEFT,
        stance: StanceType = StanceType.AS,
        turnDeg: Int = 0,
    ): Movement =
        Movement(
            index = 1,
            expectedTurnDeg = turnDeg,
            turnDegrees = turnDeg,
            sharesStanceWithPrevious = false,
            stance = Stance(stance, Side.LEFT),
            technique = Technique(tech, arm),
            extra = null,
            evaluationMode = EvaluationMode.HOLD,
            activeJoints = listOf(Joint.LEFT_WRIST),
            kihap = false,
            nameKo = "tech",
            nameEs = "tech",
        )

    @Test
    fun `calculate metrics for Arae Makki low block`() {
        val mov = movement(TechniqueType.ARAE_MAKKI, arm = Side.LEFT)
        val frame =
            SkeletonBuilder()
                .setJointPosition(Joint.LEFT_WRIST, Vec3(-0.25, -0.20, 0.20))
                .build()
        val metrics = MetricCalculator.calculateMetrics(frame, mov, profile)
        assertTrue(metrics.containsKey(MetricId.AR_01))
        assertTrue(metrics.containsKey(MetricId.AR_02))
        assertTrue(metrics.containsKey(MetricId.AR_03))
    }

    @Test
    fun `calculate metrics for Momtong An Makki inner block`() {
        val mov = movement(TechniqueType.MOMTONG_AN_MAKKI, arm = Side.RIGHT)
        val frame =
            SkeletonBuilder()
                .setJointPosition(Joint.RIGHT_ELBOW, Vec3(0.20, 0.30, 0.25))
                .setJointPosition(Joint.RIGHT_WRIST, Vec3(0.10, 0.55, 0.25))
                .build()
        val metrics = MetricCalculator.calculateMetrics(frame, mov, profile)
        assertTrue(metrics.containsKey(MetricId.AR_01))
        assertTrue(metrics.containsKey(MetricId.AR_06))
        assertTrue(metrics.containsKey(MetricId.AR_07))
    }

    @Test
    fun `calculate metrics for Eolgul Makki high block`() {
        val mov = movement(TechniqueType.EOLGUL_MAKKI, arm = Side.LEFT)
        val frame =
            SkeletonBuilder()
                .setJointPosition(Joint.LEFT_WRIST, Vec3(-0.15, 0.85, 0.20))
                .build()
        val metrics = MetricCalculator.calculateMetrics(frame, mov, profile)
        assertTrue(metrics.containsKey(MetricId.AR_01))
        assertTrue(metrics.containsKey(MetricId.AR_08))
    }

    @Test
    fun `calculate metrics for Ap Chagi front kick`() {
        val mov = movement(TechniqueType.AP_CHAGI, arm = Side.RIGHT)
        val frame =
            SkeletonBuilder()
                .setJointPosition(Joint.RIGHT_KNEE, Vec3(0.12, 0.10, 0.40))
                .setJointPosition(Joint.RIGHT_ANKLE, Vec3(0.12, 0.25, 0.70))
                .build()
        val metrics = MetricCalculator.calculateMetrics(frame, mov, profile)
        assertTrue(metrics.containsKey(MetricId.KI_01))
        assertTrue(metrics.containsKey(MetricId.KI_02))
        assertTrue(metrics.containsKey(MetricId.KI_03))
        assertTrue(metrics.containsKey(MetricId.KI_04))
        assertTrue(metrics.containsKey(MetricId.KI_05))
    }

    @Test
    fun `calculate metrics for Ap kubi forward stance`() {
        val mov = movement(TechniqueType.MOMTONG_JIREUGI, stance = StanceType.AK)
        val frame =
            SkeletonBuilder()
                .setStanceStep(frontLegLeft = true, stepLength = 1.35)
                .build()
        val metrics = MetricCalculator.calculateMetrics(frame, mov, profile)
        assertTrue(metrics.containsKey(MetricId.ST_01))
        assertTrue(metrics.containsKey(MetricId.ST_03))
    }

    @Test
    fun `calculate metrics for turn with measured heading`() {
        val mov = movement(TechniqueType.ARAE_MAKKI, turnDeg = -90)
        val frame = SkeletonBuilder().build()
        val metrics =
            MetricCalculator.calculateMetrics(
                frame = frame,
                movement = mov,
                profile = profile,
                measuredTurnDeg = -88.0,
            )
        val di01 = metrics[MetricId.DI_01]
        assertTrue(di01 is MetricResult.Value)
        assertEquals(2.0, (di01 as MetricResult.Value).value, 0.1)
    }

    @Test
    fun `calculate metrics for turn with missing measured heading returns unavailable`() {
        val mov = movement(TechniqueType.ARAE_MAKKI, turnDeg = -90)
        val frame = SkeletonBuilder().build()
        val metrics =
            MetricCalculator.calculateMetrics(
                frame = frame,
                movement = mov,
                profile = profile,
                measuredTurnDeg = null,
            )
        val di01 = metrics[MetricId.DI_01]
        assertTrue(di01 is MetricResult.Unavailable)
    }

    @Test
    fun `all metric scoring bands produce valid monotonic scores`() {
        val mov = movement(TechniqueType.MOMTONG_JIREUGI)
        for (id in MetricId.entries) {
            val band = MetricBandRegistry.resolveBand(id, mov)
            assertNotNull(band)
            val scoreMid = band.score((band.a + band.b) * 0.5)
            val scoreBelow = band.score(band.c - 10.0)
            val scoreAbove = band.score(band.d + 10.0)
            assertEquals(100.0, scoreMid, 1e-4)
            assertEquals(0.0, scoreBelow, 1e-4)
            assertEquals(0.0, scoreAbove, 1e-4)
        }
    }

    @Test
    fun `feedback rule engine produces valid feedback for deviating values`() {
        val mov = movement(TechniqueType.MOMTONG_JIREUGI)
        for (id in MetricId.entries) {
            val band = MetricBandRegistry.resolveBand(id, mov)
            val lowItem = FeedbackRuleEngine.generateFeedback(id, band.c - 1.0, band)
            val highItem = FeedbackRuleEngine.generateFeedback(id, band.d + 1.0, band)
            assertNotNull("Feedback para $id low", lowItem)
            assertNotNull("Feedback para $id high", highItem)
        }
    }
}
