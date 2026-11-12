package mx.taekwondo.poomsae.analysis.quality

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.geometry.Vec3
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.testutil.SkeletonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyProfileCalibratorTest {
    @Test
    fun `insufficient frames count fails with INSUFFICIENT_FRAMES`() {
        val shortList =
            (1..5).map { idx ->
                SkeletonBuilder().build(idx * 33L)
            }
        val result = BodyProfileCalibrator.calibrate(shortList)
        assertTrue(result is CalibrationResult.Rejected)
        assertEquals(CalibrationRejectionReason.INSUFFICIENT_FRAMES, (result as CalibrationResult.Rejected).reason)
    }

    @Test
    fun `unstable frames with severe jitter fail with UNSTABLE_MEASUREMENTS`() {
        val count = AnalysisConfig.CAL_MIN_FRAMES + 5
        val unstableList =
            (1..count).map { idx ->
                val jitter = ((idx % 5) - 2) * 0.20
                SkeletonBuilder()
                    .setJointPosition(Joint.LEFT_KNEE, Vec3(-0.12, -0.45 + jitter, 0.0))
                    .setJointPosition(Joint.RIGHT_KNEE, Vec3(0.12, -0.45 + jitter, 0.0))
                    .build(idx * 33L)
            }

        val result = BodyProfileCalibrator.calibrate(unstableList)
        assertTrue("Se esperaba rechazo pero fue: $result", result is CalibrationResult.Rejected)
        assertEquals(CalibrationRejectionReason.UNSTABLE_MEASUREMENTS, (result as CalibrationResult.Rejected).reason)
    }

    @Test
    fun `stable standing frames produce valid BodyProfile with reference headings`() {
        val count = AnalysisConfig.CAL_MIN_FRAMES + 5
        val stableFrames =
            (1..count).map { idx ->
                SkeletonBuilder().build(idx * 33L)
            }

        val result = BodyProfileCalibrator.calibrate(stableFrames)
        assertTrue("Se esperaba éxito pero falló: $result", result is CalibrationResult.Success)
        val profile = (result as CalibrationResult.Success).profile
        assertTrue("lLeg esperado cercano a 0.90, fue ${profile.lLeg}", profile.lLeg in 0.85..0.95)
        assertTrue("torsoLength esperado cercano a 0.50, fue ${profile.torsoLength}", profile.torsoLength in 0.45..0.55)
        assertTrue(
            "shoulderWidth esperado cercano a 0.40, fue ${profile.shoulderWidth}",
            profile.shoulderWidth in 0.35..0.45,
        )
    }
}
