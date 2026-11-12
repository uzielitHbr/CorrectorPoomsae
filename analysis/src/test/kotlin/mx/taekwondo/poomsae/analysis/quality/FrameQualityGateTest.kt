package mx.taekwondo.poomsae.analysis.quality

import mx.taekwondo.poomsae.analysis.model.CoordinateSpace
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.Landmark
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.testutil.SkeletonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameQualityGateTest {
    @Test
    fun `valid frame within margins and reasonable distance passes with Ok`() {
        val frame = SkeletonBuilder().build()
        val result = FrameQualityGate.evaluate(frame)
        assertEquals(FrameQualityResult.Ok, result)
    }

    @Test
    fun `null frame is rejected with NO_PERSON`() {
        val result = FrameQualityGate.evaluate(null)
        assertTrue(result is FrameQualityResult.Rejected)
        assertEquals(QualityRejectionCode.NO_PERSON, (result as FrameQualityResult.Rejected).code)
    }

    @Test
    fun `low visibility on required joint is rejected with LOW_CONFIDENCE`() {
        val frame =
            SkeletonBuilder()
                .setJointVisibility(Joint.LEFT_ANKLE, 0.2f)
                .build()

        val result = FrameQualityGate.evaluate(frame, setOf(Joint.LEFT_ANKLE))
        assertTrue(result is FrameQualityResult.Rejected)
        assertEquals(QualityRejectionCode.LOW_CONFIDENCE, (result as FrameQualityResult.Rejected).code)
    }

    @Test
    fun `torso too small in image frame is rejected with FRAME_TOO_FAR`() {
        // Creamos un marco con hombros y caderas extremadamente juntos en el plano de imagen
        val base = SkeletonBuilder().build()
        val modifiedImage = base.landmarks(CoordinateSpace.IMAGE).toMutableMap()

        // Distancia hombros-caderas muy pequeña (< 0.15)
        modifiedImage[Joint.LEFT_SHOULDER] = Landmark(0.48f, 0.49f, 0.0f, 1.0f)
        modifiedImage[Joint.RIGHT_SHOULDER] = Landmark(0.52f, 0.49f, 0.0f, 1.0f)
        modifiedImage[Joint.LEFT_HIP] = Landmark(0.49f, 0.51f, 0.0f, 1.0f)
        modifiedImage[Joint.RIGHT_HIP] = Landmark(0.51f, 0.51f, 0.0f, 1.0f)

        val farFrame =
            PoseFrame(
                timestampMs = base.timestampMs,
                image = modifiedImage,
                world = base.world,
            )

        val result = FrameQualityGate.evaluate(farFrame)
        assertTrue(result is FrameQualityResult.Rejected)
        assertEquals(QualityRejectionCode.FRAME_TOO_FAR, (result as FrameQualityResult.Rejected).code)
    }

    @Test
    fun `torso occupying excessive image frame is rejected with FRAME_TOO_CLOSE`() {
        val base = SkeletonBuilder().build()
        val modifiedImage = base.landmarks(CoordinateSpace.IMAGE).toMutableMap()

        // Distancia hombros-caderas muy grande (> 0.65)
        modifiedImage[Joint.LEFT_SHOULDER] = Landmark(0.3f, 0.10f, 0.0f, 1.0f)
        modifiedImage[Joint.RIGHT_SHOULDER] = Landmark(0.7f, 0.10f, 0.0f, 1.0f)
        modifiedImage[Joint.LEFT_HIP] = Landmark(0.3f, 0.85f, 0.0f, 1.0f)
        modifiedImage[Joint.RIGHT_HIP] = Landmark(0.7f, 0.85f, 0.0f, 1.0f)

        val closeFrame =
            PoseFrame(
                timestampMs = base.timestampMs,
                image = modifiedImage,
                world = base.world,
            )

        val result = FrameQualityGate.evaluate(closeFrame)
        assertTrue(result is FrameQualityResult.Rejected)
        assertEquals(QualityRejectionCode.FRAME_TOO_CLOSE, (result as FrameQualityResult.Rejected).code)
    }

    @Test
    fun `joint cut at image border is rejected with FRAME_CUT`() {
        val base = SkeletonBuilder().build()
        val modifiedImage = base.landmarks(CoordinateSpace.IMAGE).toMutableMap()

        // Tobillo izquierdo cortado en el borde inferior (y = 0.98 > 0.95)
        modifiedImage[Joint.LEFT_ANKLE] = Landmark(0.5f, 0.98f, 0.0f, 1.0f)

        val cutFrame =
            PoseFrame(
                timestampMs = base.timestampMs,
                image = modifiedImage,
                world = base.world,
            )

        val result = FrameQualityGate.evaluate(cutFrame)
        assertTrue(result is FrameQualityResult.Rejected)
        assertEquals(QualityRejectionCode.FRAME_CUT, (result as FrameQualityResult.Rejected).code)
    }
}
