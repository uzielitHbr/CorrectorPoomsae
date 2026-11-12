package mx.taekwondo.poomsae.analysis.segmentation

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.testutil.SkeletonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para el calculador de velocidad articular normalizada (Sección 12.3).
 */
class JointSpeedCalculatorTest {
    @Test
    fun `null previous frame returns empty map and zero active speed`() {
        val frame = SkeletonBuilder().build(1000L)
        val speeds = JointSpeedCalculator.computeJointSpeeds(frame, null)
        assertTrue(speeds.isEmpty())

        val activeSpeed = JointSpeedCalculator.computeActiveSpeed(frame, null, listOf(Joint.RIGHT_WRIST))
        assertEquals(0.0, activeSpeed, AnalysisConfig.EPS_NORM)
    }

    @Test
    fun `non-positive delta time returns empty map`() {
        val f1 = SkeletonBuilder().build(1000L)
        val f2 = SkeletonBuilder().build(1000L)
        val speeds = JointSpeedCalculator.computeJointSpeeds(f2, f1)
        assertTrue(speeds.isEmpty())
    }

    @Test
    fun `stationary skeleton produces near zero joint speeds`() {
        val f1 = SkeletonBuilder().build(1000L)
        val f2 = SkeletonBuilder().build(1033L) // ~30 fps
        val speeds = JointSpeedCalculator.computeJointSpeeds(f2, f1)

        for ((_, speed) in speeds) {
            assertEquals(0.0, speed, AnalysisConfig.EPS_NORM)
        }
    }

    @Test
    fun `moving wrist produces proportional normalized speed in torsos per second`() {
        val f1 = SkeletonBuilder().build(1000L)

        // Mover muñeca derecha 0.05 unidades en 100 ms con torso ~ 0.30
        // Desplazamiento = 0.05, dt = 0.10s, torso = 0.30 -> speed = 0.05 / (0.10 * 0.30) = 1.666 torsos/s
        val f2 =
            SkeletonBuilder()
                .setArmTechnique(isRightArm = true, elbowAngleDeg = 90.0, wristForwardRatio = 1.5)
                .build(1100L)

        val wristSpeed =
            JointSpeedCalculator.computeActiveSpeed(
                currentFrame = f2,
                previousFrame = f1,
                activeJoints = listOf(Joint.RIGHT_WRIST),
            )

        assertTrue("Velocidad de muñeca $wristSpeed debe ser > 0.5", wristSpeed > 0.5)
    }

    @Test
    fun `distance invariance normalized by torso height`() {
        // Si el atleta está cerca (torso = 0.50) y se mueve 0.10 unidades en 100 ms: speed = 0.10 / (0.10 * 0.50) = 2.0
        // Si el atleta está lejos (torso = 0.25) y se mueve 0.05 unidades en 100 ms: speed = 0.05 / (0.10 * 0.25) = 2.0
        val torsoClose = 0.50
        val torsoFar = 0.25

        val dtSec = 0.10
        val distClose = 0.10
        val distFar = 0.05

        val speedClose = distClose / (dtSec * torsoClose)
        val speedFar = distFar / (dtSec * torsoFar)

        assertEquals(speedClose, speedFar, AnalysisConfig.EPS_NORM)
    }
}
