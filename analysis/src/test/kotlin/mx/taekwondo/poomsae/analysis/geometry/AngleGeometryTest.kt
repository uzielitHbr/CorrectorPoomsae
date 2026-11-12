package mx.taekwondo.poomsae.analysis.geometry

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.MetricResult
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.model.UnavailableReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class AngleGeometryTest {
    @Test
    fun distance3DAnd2DMatchHandCalculatedPythagoreanTriples() {
        // Hand calculation 3D: a=(1, 2, 3), b=(2, 4, 5) -> diff=(-1, -2, -2) -> sqrt(1+4+4) = 3.0
        val d3 = AngleGeometry.distance3D(Vec3(1.0, 2.0, 3.0), Vec3(2.0, 4.0, 5.0), confidence = 0.9f)
        assertEquals(MetricResult.Value(3.0, 0.9f), d3)

        // Hand calculation 2D: a=(1, 1), b=(4, 5) -> diff=(-3, -4) -> sqrt(9+16) = 5.0
        val d2 = AngleGeometry.distance2D(Vec2(1.0, 1.0), Vec2(4.0, 5.0), confidence = 0.8f)
        assertEquals(MetricResult.Value(5.0, 0.8f), d2)

        assertEquals(
            MetricResult.Unavailable(UnavailableReason.OUT_OF_RANGE),
            AngleGeometry.distance3D(Vec3(Double.NaN, 0.0, 0.0), Vec3.ZERO),
        )
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.OUT_OF_RANGE),
            AngleGeometry.distance2D(Vec2(Double.POSITIVE_INFINITY, 0.0), Vec2.ZERO),
        )
    }

    @Test
    fun angleBetweenPoints3DCovers90_180_0_And60Degrees() {
        // 1) 90 degrees (orthogonal): a=(1,0,0), b=(0,0,0), c=(0,1,0) -> cos=0 -> arccos(0)=90 deg
        val res90 =
            AngleGeometry.angleBetweenPoints3D(
                Vec3(1.0, 0.0, 0.0),
                Vec3.ZERO,
                Vec3(0.0, 1.0, 0.0),
            ) as MetricResult.Value
        assertEquals(90.0, res90.value, AnalysisConfig.EPS_ANGLE_DEG)

        // 2) 180 degrees (collinear opposite): a=(-2,0,0), b=(0,0,0), c=(5,0,0) -> cos=-1 -> arccos(-1)=180 deg
        val res180 =
            AngleGeometry.angleBetweenPoints3D(
                Vec3(-2.0, 0.0, 0.0),
                Vec3.ZERO,
                Vec3(5.0, 0.0, 0.0),
            ) as MetricResult.Value
        assertEquals(180.0, res180.value, AnalysisConfig.EPS_ANGLE_DEG)

        // 3) 0 degrees (collinear same side): a=(2,0,0), b=(0,0,0), c=(5,0,0) -> cos=+1 -> arccos(1)=0 deg
        val res0 =
            AngleGeometry.angleBetweenPoints3D(
                Vec3(2.0, 0.0, 0.0),
                Vec3.ZERO,
                Vec3(5.0, 0.0, 0.0),
            ) as MetricResult.Value
        assertEquals(0.0, res0.value, AnalysisConfig.EPS_ANGLE_DEG)

        // 4) 60 degrees (equilateral triangle): a=(1,0,0), b=(0,0,0), c=(0.5, sqrt(3)/2, 0) -> cos=0.5 -> 60 deg
        val res60 =
            AngleGeometry.angleBetweenPoints3D(
                Vec3(1.0, 0.0, 0.0),
                Vec3.ZERO,
                Vec3(0.5, sqrt(3.0) * 0.5, 0.0),
            ) as MetricResult.Value
        assertEquals(60.0, res60.value, AnalysisConfig.EPS_ANGLE_DEG)
    }

    @Test
    fun angleBetweenPoints2DCovers90_180_0_And45Degrees() {
        // 90 deg: (0,1), (0,0), (1,0) -> 90.0
        assertEquals(
            90.0,
            (AngleGeometry.angleBetweenPoints2D(Vec2(0.0, 1.0), Vec2.ZERO, Vec2(1.0, 0.0)) as MetricResult.Value).value,
            AnalysisConfig.EPS_ANGLE_DEG,
        )
        // 180 deg: (-3,-4), (0,0), (3,4) -> 180.0
        assertEquals(
            180.0,
            (
                AngleGeometry.angleBetweenPoints2D(
                    Vec2(-3.0, -4.0),
                    Vec2.ZERO,
                    Vec2(3.0, 4.0),
                ) as MetricResult.Value
            ).value,
            AnalysisConfig.EPS_ANGLE_DEG,
        )
        // 0 deg: (3,4), (0,0), (6,8) -> 0.0
        assertEquals(
            0.0,
            (
                AngleGeometry.angleBetweenPoints2D(
                    Vec2(3.0, 4.0),
                    Vec2.ZERO,
                    Vec2(6.0, 8.0),
                ) as MetricResult.Value
            ).value,
            AnalysisConfig.EPS_ANGLE_DEG,
        )
        // 45 deg: (1,0), (0,0), (1,1) -> cos = 1/sqrt(2) -> 45.0
        assertEquals(
            45.0,
            (
                AngleGeometry.angleBetweenPoints2D(
                    Vec2(1.0, 0.0),
                    Vec2.ZERO,
                    Vec2(1.0, 1.0),
                ) as MetricResult.Value
            ).value,
            AnalysisConfig.EPS_ANGLE_DEG,
        )
    }

    @Test
    fun angleBetweenPointsRejectsDegenerateAndNonFiniteInputs() {
        // Coincident points (a == b or c == b) -> DEGENERATE_GEOMETRY
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.DEGENERATE_GEOMETRY),
            AngleGeometry.angleBetweenPoints3D(Vec3.ZERO, Vec3.ZERO, Vec3(1.0, 0.0, 0.0)),
        )
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.DEGENERATE_GEOMETRY),
            AngleGeometry.angleBetweenPoints3D(Vec3(1.0, 0.0, 0.0), Vec3.ZERO, Vec3.ZERO),
        )
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.DEGENERATE_GEOMETRY),
            AngleGeometry.angleBetweenPoints2D(Vec2.ZERO, Vec2.ZERO, Vec2(1.0, 0.0)),
        )
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.OUT_OF_RANGE),
            AngleGeometry.angleBetweenPoints3D(Vec3(Double.NaN, 0.0, 0.0), Vec3.ZERO, Vec3(1.0, 0.0, 0.0)),
        )
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.OUT_OF_RANGE),
            AngleGeometry.angleBetweenPoints2D(Vec2(Double.POSITIVE_INFINITY, 0.0), Vec2.ZERO, Vec2(1.0, 0.0)),
        )
    }

    @Test
    fun angleWithVerticalMatchesHandCalculatedValues() {
        val up = Vec3(0.0, 1.0, 0.0)

        // Vertical upright: v = (0, 2.5, 0) -> 0 deg
        assertEquals(
            0.0,
            (AngleGeometry.angleWithVertical(Vec3(0.0, 2.5, 0.0), up) as MetricResult.Value).value,
            AnalysisConfig.EPS_ANGLE_DEG,
        )
        // Inclined 30 deg: v = (1, sqrt(3), 0) -> norm=2, cos(theta)=sqrt(3)/2 -> 30.0 deg
        assertEquals(
            30.0,
            (AngleGeometry.angleWithVertical(Vec3(1.0, sqrt(3.0), 0.0), up) as MetricResult.Value).value,
            AnalysisConfig.EPS_ANGLE_DEG,
        )
        // Horizontal: v = (0, 0, -1) -> 90.0 deg
        assertEquals(
            90.0,
            (AngleGeometry.angleWithVertical(Vec3(0.0, 0.0, -1.0), up) as MetricResult.Value).value,
            AnalysisConfig.EPS_ANGLE_DEG,
        )
        // Pointing straight down: v = (0, -3, 0) -> 180.0 deg
        assertEquals(
            180.0,
            (AngleGeometry.angleWithVertical(Vec3(0.0, -3.0, 0.0), up) as MetricResult.Value).value,
            AnalysisConfig.EPS_ANGLE_DEG,
        )
        // Degenerate zero vector or zero up
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.DEGENERATE_GEOMETRY),
            AngleGeometry.angleWithVertical(Vec3.ZERO, up),
        )
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.DEGENERATE_GEOMETRY),
            AngleGeometry.angleWithVertical(Vec3(0.0, 1.0, 0.0), Vec3.ZERO),
        )
    }

    @Test
    fun projectOntoPlaneProducesVectorOrthogonalToNormal() {
        // Hand calculation: v = (3, 5, -4), u = (0, 1, 0) -> G(v) = (3, 0, -4)
        val res = AngleGeometry.projectOntoPlane(Vec3(3.0, 5.0, -4.0), Vec3(0.0, 2.0, 0.0))
        assertTrue(res is GeometryResult.Success)
        val projected = (res as GeometryResult.Success).value
        assertEquals(3.0, projected.x, AnalysisConfig.EPS_SCALAR)
        assertEquals(0.0, projected.y, AnalysisConfig.EPS_SCALAR)
        assertEquals(-4.0, projected.z, AnalysisConfig.EPS_SCALAR)

        // Tilted normal n = (1, 1, 1) -> G(v) · n must equal 0
        val tiltedNormal = Vec3(1.0, 1.0, 1.0)
        val tiltedProj =
            (
                AngleGeometry.projectOntoPlane(
                    Vec3(2.0, -1.0, 5.0),
                    tiltedNormal,
                ) as GeometryResult.Success
            ).value
        assertEquals(0.0, tiltedProj.dot(tiltedNormal), AnalysisConfig.EPS_DOT)

        assertEquals(
            UnavailableReason.OUT_OF_RANGE,
            (
                AngleGeometry.projectOntoPlane(
                    Vec3(Double.NaN, 0.0, 0.0),
                    Vec3(0.0, 1.0, 0.0),
                ) as GeometryResult.Failure
            ).reason,
        )
        assertEquals(
            UnavailableReason.DEGENERATE_GEOMETRY,
            (AngleGeometry.projectOntoPlane(Vec3(1.0, 0.0, 0.0), Vec3.ZERO) as GeometryResult.Failure).reason,
        )
    }

    @Test
    fun signedAngleInPlaneAndWrapDegreesHandleAllQuadrantsAnd180Crossings() {
        val up = Vec3(0.0, 1.0, 0.0)
        val forward = Vec3(0.0, 0.0, 1.0)

        // Hand calculation around +Y:
        // (0,0,1) x (1,0,0) = (0, 1, 0) -> dot(+Y) = +1 -> +90.0 deg
        val plus90 = AngleGeometry.signedAngleInPlane(forward, Vec3(1.0, 2.0, 0.0), up) as MetricResult.Value
        assertEquals(90.0, plus90.value, AnalysisConfig.EPS_ANGLE_DEG)

        // (0,0,1) x (-1,0,0) = (0, -1, 0) -> dot(+Y) = -1 -> -90.0 deg
        val minus90 = AngleGeometry.signedAngleInPlane(forward, Vec3(-1.0, 5.0, 0.0), up) as MetricResult.Value
        assertEquals(-90.0, minus90.value, AnalysisConfig.EPS_ANGLE_DEG)

        // Opposite: (0,0,1) -> (0,0,-1) => 180.0 deg in (-180, 180]
        val deg180 = AngleGeometry.signedAngleInPlane(forward, Vec3(0.0, 0.0, -1.0), up) as MetricResult.Value
        assertEquals(180.0, deg180.value, AnalysisConfig.EPS_ANGLE_DEG)

        // Same vector projected: 0.0 deg
        val deg0 = AngleGeometry.signedAngleInPlane(forward, Vec3(0.0, 3.0, 2.0), up) as MetricResult.Value
        assertEquals(0.0, deg0.value, AnalysisConfig.EPS_ANGLE_DEG)

        // Degenerate: vector parallel to planeNormal projects to zero -> DEGENERATE_GEOMETRY
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.DEGENERATE_GEOMETRY),
            AngleGeometry.signedAngleInPlane(Vec3(0.0, 2.0, 0.0), forward, up),
        )
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.DEGENERATE_GEOMETRY),
            AngleGeometry.signedAngleInPlane(forward, Vec3.ZERO, up),
        )
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.DEGENERATE_GEOMETRY),
            AngleGeometry.signedAngleInPlane(forward, Vec3(1.0, 0.0, 0.0), Vec3.ZERO),
        )

        // wrapDegrees boundary checks
        assertEquals(0.0, AngleGeometry.wrapDegrees(0.0), AnalysisConfig.EPS_ANGLE_DEG)
        assertEquals(180.0, AngleGeometry.wrapDegrees(180.0), AnalysisConfig.EPS_ANGLE_DEG)
        assertEquals(180.0, AngleGeometry.wrapDegrees(-180.0), AnalysisConfig.EPS_ANGLE_DEG)
        assertEquals(-90.0, AngleGeometry.wrapDegrees(270.0), AnalysisConfig.EPS_ANGLE_DEG)
        assertEquals(90.0, AngleGeometry.wrapDegrees(-270.0), AnalysisConfig.EPS_ANGLE_DEG)
        assertEquals(180.0, AngleGeometry.wrapDegrees(540.0), AnalysisConfig.EPS_ANGLE_DEG)
        assertEquals(-90.0, AngleGeometry.wrapDegrees(-450.0), AnalysisConfig.EPS_ANGLE_DEG)
    }

    @Test
    fun anglesAreInvariantUnderTranslationRotationAndUniformScale() {
        val a = Vec3(0.4, 0.9, 0.2)
        val b = Vec3(0.1, 0.5, 0.1)
        val c = Vec3(0.2, 0.1, 0.4)
        val up = Vec3(0.0, 1.0, 0.0)

        val baseJointAngle = (AngleGeometry.angleBetweenPoints3D(a, b, c) as MetricResult.Value).value
        val baseVerticalAngle = (AngleGeometry.angleWithVertical(a - b, up) as MetricResult.Value).value
        val baseSignedAngle = (AngleGeometry.signedAngleInPlane(a - b, c - b, up) as MetricResult.Value).value

        // Apply arbitrary translation + 3D Euler rotation + uniform positive scaling (scale = 3.5)
        val spec =
            TransformSpec(
                yawDeg = 41.0,
                pitchDeg = -17.0,
                rollDeg = 29.0,
                scale = 3.5,
                offset = Vec3(12.5, -7.25, 4.1),
            )
        val rotOnly = TransformSpec(yawDeg = 41.0, pitchDeg = -17.0, rollDeg = 29.0)
        val aT = TestSkeletonHelper.rigidTransform(a, spec)
        val bT = TestSkeletonHelper.rigidTransform(b, spec)
        val cT = TestSkeletonHelper.rigidTransform(c, spec)
        // Rotate 'up' with the same orientation (direction vector: rotation only, no translation)
        val upT = TestSkeletonHelper.rigidTransform(up, rotOnly)

        val transformedJointAngle = (AngleGeometry.angleBetweenPoints3D(aT, bT, cT) as MetricResult.Value).value
        val transformedVerticalAngle = (AngleGeometry.angleWithVertical(aT - bT, upT) as MetricResult.Value).value
        val transformedSignedAngle =
            (AngleGeometry.signedAngleInPlane(aT - bT, cT - bT, upT) as MetricResult.Value).value

        assertEquals(baseJointAngle, transformedJointAngle, AnalysisConfig.EPS_ANGLE_DEG)
        assertEquals(baseVerticalAngle, transformedVerticalAngle, AnalysisConfig.EPS_ANGLE_DEG)
        assertEquals(baseSignedAngle, transformedSignedAngle, AnalysisConfig.EPS_ANGLE_DEG)
    }

    @Test
    fun jointAngle3DOnPoseFrameEnforcesVisibilityMissingAndFiniteChecks() {
        // Right elbow bent exactly 90 degrees: S=(0, 1.4, 0), E=(0, 1.1, 0), W=(0, 1.1, 0.3)
        val validMap =
            mapOf(
                Joint.RIGHT_SHOULDER to TestSkeletonHelper.landmark(0.0f, 1.4f, 0.0f, 0.9f),
                Joint.RIGHT_ELBOW to TestSkeletonHelper.landmark(0.0f, 1.1f, 0.0f, 0.8f),
                Joint.RIGHT_WRIST to TestSkeletonHelper.landmark(0.0f, 1.1f, 0.3f, 0.7f),
            )
        val validFrame = PoseFrame(100L, emptyMap(), validMap)
        val resValid =
            AngleGeometry.jointAngle3D(
                validFrame,
                Joint.RIGHT_SHOULDER,
                Joint.RIGHT_ELBOW,
                Joint.RIGHT_WRIST,
            )
        assertTrue(resValid is MetricResult.Value)
        assertEquals(90.0, (resValid as MetricResult.Value).value, AnalysisConfig.EPS_ANGLE_DEG)
        assertEquals(0.7f, resValid.confidence, AnalysisConfig.EPS_SCALAR.toFloat())

        // Missing joint
        val missingFrame = PoseFrame(100L, emptyMap(), validMap - Joint.RIGHT_WRIST)
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.MISSING_JOINT),
            AngleGeometry.jointAngle3D(missingFrame, Joint.RIGHT_SHOULDER, Joint.RIGHT_ELBOW, Joint.RIGHT_WRIST),
        )

        // Low visibility
        val lowVisMap = validMap + (Joint.RIGHT_ELBOW to TestSkeletonHelper.landmark(0.0f, 1.1f, 0.0f, 0.3f))
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.LOW_VISIBILITY),
            AngleGeometry.jointAngle3D(
                PoseFrame(100L, emptyMap(), lowVisMap),
                Joint.RIGHT_SHOULDER,
                Joint.RIGHT_ELBOW,
                Joint.RIGHT_WRIST,
            ),
        )

        // Non-finite coordinate
        val nanMap = validMap + (Joint.RIGHT_WRIST to TestSkeletonHelper.landmark(Float.NaN, 1.1f, 0.3f, 0.9f))
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.OUT_OF_RANGE),
            AngleGeometry.jointAngle3D(
                PoseFrame(100L, emptyMap(), nanMap),
                Joint.RIGHT_SHOULDER,
                Joint.RIGHT_ELBOW,
                Joint.RIGHT_WRIST,
            ),
        )
    }
}
