package mx.taekwondo.poomsae.analysis.geometry

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.MetricResult
import mx.taekwondo.poomsae.analysis.model.UnavailableReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyPrimitivesTest {
    @Test
    fun estimateUpVectorMatchesHandCalculationAndRejectsOutliers() {
        // Hand calculation:
        // shoulders mid = (0, 1.45, 0), ankles mid = (0, 0.10, 0)
        // diff = (0, 1.35, 0) -> normalized = (0, 1, 0)
        val f1 = TestSkeletonHelper.uprightWorldFrame(timestampMs = 100L)
        val f2 = TestSkeletonHelper.uprightWorldFrame(timestampMs = 133L)
        // Outlier frame pointing tilted; median of 3 frames picks (0, 1.35, 0) -> (0, 1, 0)
        val outlier = TestSkeletonHelper.transformFrame(f1, TransformSpec(pitchDeg = 60.0)).copy(timestampMs = 166L)

        val res = BodyPrimitives.estimateUpVector(listOf(f1, f2, outlier))
        assertTrue(res is GeometryResult.Success)
        val up = (res as GeometryResult.Success).value
        assertEquals(0.0, up.x, AnalysisConfig.EPS_SCALAR)
        assertEquals(1.0, up.y, AnalysisConfig.EPS_SCALAR)
        assertEquals(0.0, up.z, AnalysisConfig.EPS_SCALAR)
    }

    @Test
    fun estimateUpVectorFailsExplicitlyOnEmptyMissingLowVisAndDegenerate() {
        assertEquals(
            UnavailableReason.NO_CALIBRATION,
            (BodyPrimitives.estimateUpVector(emptyList()) as GeometryResult.Failure).reason,
        )

        val base = TestSkeletonHelper.uprightWorldFrame()
        val missing = base.copy(world = base.world - Joint.LEFT_ANKLE)
        assertEquals(
            UnavailableReason.MISSING_JOINT,
            (BodyPrimitives.estimateUpVector(listOf(missing)) as GeometryResult.Failure).reason,
        )

        val lowVis =
            base.copy(
                world = base.world + (Joint.RIGHT_SHOULDER to TestSkeletonHelper.landmark(0.2f, 1.45f, 0.0f, 0.1f)),
            )
        assertEquals(
            UnavailableReason.LOW_VISIBILITY,
            (BodyPrimitives.estimateUpVector(listOf(lowVis)) as GeometryResult.Failure).reason,
        )

        // Coincident shoulders and ankles -> zero length up vector -> DEGENERATE_GEOMETRY
        val zeroHeight =
            base.copy(
                world =
                    base.world +
                        (Joint.LEFT_SHOULDER to base.world.getValue(Joint.LEFT_ANKLE)) +
                        (Joint.RIGHT_SHOULDER to base.world.getValue(Joint.RIGHT_ANKLE)),
            )
        assertEquals(
            UnavailableReason.DEGENERATE_GEOMETRY,
            (BodyPrimitives.estimateUpVector(listOf(zeroHeight)) as GeometryResult.Failure).reason,
        )
    }

    @Test
    fun hipAxisForwardSignAndHeadingMatchHandCalculation() {
        // Hand calculation:
        // u = (0, 1, 0), R_H = (0.15, 0.95, 0), L_H = (-0.15, 0.95, 0) -> h = (0.30, 0, 0) -> h_g_hat = (1, 0, 0) = r_0
        // u x h = (0, 1, 0) x (0.30, 0, 0) = (0, 0, -0.30)
        // Toe - Heel = (0, 0, +0.20)
        // (u x h) · (Toe - Heel) = -0.06 < 0 => sigma = -1.0
        // F_0 = sigma * normalized(G(u x h)) = -1 * (0, 0, -1) = (0, 0, +1)
        val f0Frame = TestSkeletonHelper.uprightWorldFrame(timestampMs = 1000L)
        val up = (BodyPrimitives.estimateUpVector(listOf(f0Frame)) as GeometryResult.Success).value
        val r0 = (BodyPrimitives.hipAxisHorizontal(f0Frame, up) as GeometryResult.Success).value.point
        assertEquals(1.0, r0.x, AnalysisConfig.EPS_SCALAR)
        assertEquals(0.0, r0.y, AnalysisConfig.EPS_SCALAR)
        assertEquals(0.0, r0.z, AnalysisConfig.EPS_SCALAR)

        val sigma = (BodyPrimitives.estimateForwardSign(listOf(f0Frame), up) as GeometryResult.Success).value
        assertEquals(-1.0, sigma, AnalysisConfig.EPS_SCALAR)

        val f0 = (BodyPrimitives.bodyForwardDirection(f0Frame, up, sigma) as GeometryResult.Success).value.point
        assertEquals(0.0, f0.x, AnalysisConfig.EPS_SCALAR)
        assertEquals(0.0, f0.y, AnalysisConfig.EPS_SCALAR)
        assertEquals(1.0, f0.z, AnalysisConfig.EPS_SCALAR)

        val basis = OrientationBasis(up = up, forwardSign = sigma, forwardZero = f0, rightZero = r0)

        // Straight ahead (0 deg)
        val res0 = BodyPrimitives.computeHeadingFromFrame(f0Frame, basis) as MetricResult.Value
        assertEquals(0.0, res0.value, AnalysisConfig.EPS_ANGLE_DEG)

        // Turn 90 deg to the person's RIGHT (facing +r_0 = +X, which is rotateY(+90 deg) from +Z)
        val turnedRight90 = TestSkeletonHelper.transformFrame(f0Frame, TransformSpec(yawDeg = 90.0))
        val resRight90 = BodyPrimitives.computeHeadingFromFrame(turnedRight90, basis) as MetricResult.Value
        assertEquals(90.0, resRight90.value, AnalysisConfig.EPS_ANGLE_DEG)

        // Turn 90 deg to the person's LEFT (facing -r_0 = -X, which is rotateY(-90 deg) from +Z)
        val turnedLeft90 = TestSkeletonHelper.transformFrame(f0Frame, TransformSpec(yawDeg = -90.0))
        val resLeft90 = BodyPrimitives.computeHeadingFromFrame(turnedLeft90, basis) as MetricResult.Value
        assertEquals(-90.0, resLeft90.value, AnalysisConfig.EPS_ANGLE_DEG)

        // Turn 180 deg
        val turned180 = TestSkeletonHelper.transformFrame(f0Frame, TransformSpec(yawDeg = 180.0))
        val res180 = BodyPrimitives.computeHeadingFromFrame(turned180, basis) as MetricResult.Value
        assertEquals(180.0, res180.value, AnalysisConfig.EPS_ANGLE_DEG)
    }

    @Test
    fun headingIsInvariantUnderArbitraryCameraTiltTranslationAndScale() {
        // Tilt the entire world coordinate system (e.g. phone tilted 25 deg pitch, -18 deg roll) + translate + scale
        val base = TestSkeletonHelper.uprightWorldFrame()
        val tiltSpec =
            TransformSpec(
                yawDeg = 35.0,
                pitchDeg = 25.0,
                rollDeg = -18.0,
                scale = 2.0,
                offset = Vec3(3.0, -1.5, 4.0),
            )
        val f0Tilted = TestSkeletonHelper.transformFrame(base, tiltSpec)

        val up = (BodyPrimitives.estimateUpVector(listOf(f0Tilted)) as GeometryResult.Success).value
        val sigma = (BodyPrimitives.estimateForwardSign(listOf(f0Tilted), up) as GeometryResult.Success).value
        val r0 = (BodyPrimitives.hipAxisHorizontal(f0Tilted, up) as GeometryResult.Success).value.point
        val f0 = (BodyPrimitives.bodyForwardDirection(f0Tilted, up, sigma) as GeometryResult.Success).value.point
        val basis = OrientationBasis(up = up, forwardSign = sigma, forwardZero = f0, rightZero = r0)

        // Turn person 90 deg to their RIGHT in body space, then apply the exact same camera tilt/translation/scale
        val turnedRightLocal = TestSkeletonHelper.transformFrame(base, TransformSpec(yawDeg = 90.0))
        val turnedRightTilted = TestSkeletonHelper.transformFrame(turnedRightLocal, tiltSpec)

        val heading = BodyPrimitives.computeHeadingFromFrame(turnedRightTilted, basis) as MetricResult.Value
        assertEquals(90.0, heading.value, AnalysisConfig.EPS_ANGLE_DEG)
    }

    @Test
    fun headingAndOrientationPrimitivesReportExplicitErrorsOnInvalidInputs() {
        val base = TestSkeletonHelper.uprightWorldFrame()
        val up = Vec3(0.0, 1.0, 0.0)

        // Missing reference heading (e.g., resumed session SC-18) -> NO_HEADING_REFERENCE
        val noRefBasis = OrientationBasis(up = up, forwardSign = -1.0, forwardZero = null, rightZero = null)
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.NO_HEADING_REFERENCE),
            BodyPrimitives.computeHeadingFromFrame(base, noRefBasis),
        )

        // Invalid forwardSign
        assertEquals(
            UnavailableReason.OUT_OF_RANGE,
            (BodyPrimitives.bodyForwardDirection(base, up, forwardSign = 0.0) as GeometryResult.Failure).reason,
        )

        // Empty frames for forwardSign -> NO_CALIBRATION
        assertEquals(
            UnavailableReason.NO_CALIBRATION,
            (BodyPrimitives.estimateForwardSign(emptyList(), up) as GeometryResult.Failure).reason,
        )

        // Zero up vector for forwardSign -> DEGENERATE_GEOMETRY
        assertEquals(
            UnavailableReason.DEGENERATE_GEOMETRY,
            (BodyPrimitives.estimateForwardSign(listOf(base), Vec3.ZERO) as GeometryResult.Failure).reason,
        )

        // Coincident feet (heel == toe) -> dot is 0 -> DEGENERATE_GEOMETRY
        val zeroFoot =
            base.copy(
                world =
                    base.world +
                        (Joint.LEFT_FOOT_INDEX to base.world.getValue(Joint.LEFT_HEEL)) +
                        (Joint.RIGHT_FOOT_INDEX to base.world.getValue(Joint.RIGHT_HEEL)),
            )
        assertEquals(
            UnavailableReason.DEGENERATE_GEOMETRY,
            (BodyPrimitives.estimateForwardSign(listOf(zeroFoot), up) as GeometryResult.Failure).reason,
        )

        // Missing hip -> MISSING_JOINT
        val missingHip = base.copy(world = base.world - Joint.LEFT_HIP)
        assertEquals(
            UnavailableReason.MISSING_JOINT,
            (BodyPrimitives.hipAxisHorizontal(missingHip, up) as GeometryResult.Failure).reason,
        )
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.MISSING_JOINT),
            BodyPrimitives.computeHeadingFromFrame(
                missingHip,
                OrientationBasis(up, -1.0, Vec3(0.0, 0.0, 1.0), Vec3(1.0, 0.0, 0.0)),
            ),
        )

        // Zero vector passed to computeHeadingDeg -> DEGENERATE_GEOMETRY
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.DEGENERATE_GEOMETRY),
            BodyPrimitives.computeHeadingDeg(Vec3.ZERO, Vec3(0.0, 0.0, 1.0), Vec3(1.0, 0.0, 0.0)),
        )
    }

    @Test
    fun forwardSignFlipsCleanlyForOppositeFacingWorldConvention() {
        // Flip person 180 deg (now facing -Z while hips stay as-is -> toe-heel is -Z)
        val base = TestSkeletonHelper.uprightWorldFrame()
        val flippedFeet =
            base.copy(
                world =
                    base.world +
                        (Joint.LEFT_FOOT_INDEX to TestSkeletonHelper.landmark(-0.15f, 0.02f, -0.25f)) +
                        (Joint.RIGHT_FOOT_INDEX to TestSkeletonHelper.landmark(0.15f, 0.02f, -0.25f)),
            )
        val up = Vec3(0.0, 1.0, 0.0)
        val sigma = (BodyPrimitives.estimateForwardSign(listOf(flippedFeet), up) as GeometryResult.Success).value
        assertEquals(1.0, sigma, AnalysisConfig.EPS_SCALAR)

        val forward =
            (
                BodyPrimitives.bodyForwardDirection(
                    flippedFeet,
                    up,
                    sigma,
                ) as GeometryResult.Success
            ).value.point
        assertEquals(0.0, forward.x, AnalysisConfig.EPS_SCALAR)
        assertEquals(0.0, forward.y, AnalysisConfig.EPS_SCALAR)
        assertEquals(-1.0, forward.z, AnalysisConfig.EPS_SCALAR)
    }
}
