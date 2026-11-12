package mx.taekwondo.poomsae.analysis.geometry

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.model.CoordinateSpace
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.Landmark
import mx.taekwondo.poomsae.analysis.model.MetricResult
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.model.UnavailableReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Vec3AndVec2Test {
    @Test
    fun vec3ArithmeticAndProductsMatchHandCalculation() {
        // Hand calculation:
        // a = (1, 2, 3), b = (4, -5, 6)
        // a + b = (5, -3, 9)
        // a - b = (-3, 7, -3)
        // a * 2 = (2, 4, 6)
        // a · b = 1*4 + 2*(-5) + 3*6 = 4 - 10 + 18 = 12
        // a x b = (2*6 - 3*(-5), 3*4 - 1*6, 1*(-5) - 2*4) = (27, 6, -13)
        val a = Vec3(1.0, 2.0, 3.0)
        val b = Vec3(4.0, -5.0, 6.0)

        assertEquals(Vec3(5.0, -3.0, 9.0), a + b)
        assertEquals(Vec3(-3.0, 7.0, -3.0), a - b)
        assertEquals(Vec3(2.0, 4.0, 6.0), a * 2.0)
        assertEquals(12.0, a.dot(b), AnalysisConfig.EPS_SCALAR)
        assertEquals(Vec3(27.0, 6.0, -13.0), a.cross(b))
        assertEquals(Vec3(2.5, -1.5, 4.5), Vec3.midpoint(a, b))
    }

    @Test
    fun vec3NormAndNormalizationReturnExactUnitVector() {
        // Hand calculation: (1, 2, 2) -> normSquared = 1 + 4 + 4 = 9 -> norm = 3.0
        // normalized = (1/3, 2/3, 2/3)
        val v = Vec3(1.0, 2.0, 2.0)
        assertEquals(9.0, v.normSquared(), AnalysisConfig.EPS_SCALAR)
        assertEquals(3.0, v.norm(), AnalysisConfig.EPS_SCALAR)

        val normResult = v.normalized()
        assertTrue(normResult is GeometryResult.Success)
        val unit = (normResult as GeometryResult.Success).value
        assertEquals(1.0 / 3.0, unit.x, AnalysisConfig.EPS_SCALAR)
        assertEquals(2.0 / 3.0, unit.y, AnalysisConfig.EPS_SCALAR)
        assertEquals(2.0 / 3.0, unit.z, AnalysisConfig.EPS_SCALAR)
        assertEquals(1.0, unit.norm(), AnalysisConfig.EPS_SCALAR)
    }

    @Test
    fun vec3ZeroAndNearZeroReturnExplicitDegenerateErrorNeverNaN() {
        val zeroResult = Vec3.ZERO.normalized()
        assertTrue(zeroResult is GeometryResult.Failure)
        assertEquals(UnavailableReason.DEGENERATE_GEOMETRY, (zeroResult as GeometryResult.Failure).reason)

        val tiny = Vec3(1e-8, 0.0, 0.0)
        val tinyResult = tiny.normalized()
        assertTrue(tinyResult is GeometryResult.Failure)
        assertEquals(UnavailableReason.DEGENERATE_GEOMETRY, (tinyResult as GeometryResult.Failure).reason)
    }

    @Test
    fun vec3NonFiniteAndOverflowReturnExplicitOutOfRangeError() {
        val nanVec = Vec3(Double.NaN, 1.0, 0.0)
        assertFalse(nanVec.isFinite())
        val nanRes = nanVec.normalized()
        assertTrue(nanRes is GeometryResult.Failure)
        assertEquals(UnavailableReason.OUT_OF_RANGE, (nanRes as GeometryResult.Failure).reason)

        val infVec = Vec3(Double.POSITIVE_INFINITY, 0.0, 0.0)
        val infRes = infVec.normalized()
        assertTrue(infRes is GeometryResult.Failure)
        assertEquals(UnavailableReason.OUT_OF_RANGE, (infRes as GeometryResult.Failure).reason)

        // norm = sqrt(Double.MAX_VALUE^2 + Double.MAX_VALUE^2) overflows to Infinity
        val overflowVec = Vec3(Double.MAX_VALUE, Double.MAX_VALUE, 0.0)
        val overflowRes = overflowVec.normalized()
        assertTrue(overflowRes is GeometryResult.Failure)
        assertEquals(UnavailableReason.OUT_OF_RANGE, (overflowRes as GeometryResult.Failure).reason)
    }

    @Test
    fun vec2OperationsAndDegenerateGuardsMatchHandCalculation() {
        // Hand calculation: a = (3, 4), b = (1, 2)
        // a + b = (4, 6), a - b = (2, 2), a * 0.5 = (1.5, 2.0)
        // a · b = 3*1 + 4*2 = 11, cross = 3*2 - 4*1 = 2, ||a|| = 5
        val a = Vec2(3.0, 4.0)
        val b = Vec2(1.0, 2.0)
        assertEquals(Vec2(4.0, 6.0), a + b)
        assertEquals(Vec2(2.0, 2.0), a - b)
        assertEquals(Vec2(1.5, 2.0), a * 0.5)
        assertEquals(11.0, a.dot(b), AnalysisConfig.EPS_SCALAR)
        assertEquals(2.0, a.cross(b), AnalysisConfig.EPS_SCALAR)
        assertEquals(25.0, a.normSquared(), AnalysisConfig.EPS_SCALAR)
        assertEquals(5.0, a.norm(), AnalysisConfig.EPS_SCALAR)

        val unit = (a.normalized() as GeometryResult.Success).value
        assertEquals(0.6, unit.x, AnalysisConfig.EPS_SCALAR)
        assertEquals(0.8, unit.y, AnalysisConfig.EPS_SCALAR)

        assertEquals(
            UnavailableReason.DEGENERATE_GEOMETRY,
            (Vec2.ZERO.normalized() as GeometryResult.Failure).reason,
        )
        assertEquals(
            UnavailableReason.OUT_OF_RANGE,
            (Vec2(Double.NaN, 1.0).normalized() as GeometryResult.Failure).reason,
        )
        assertEquals(
            UnavailableReason.OUT_OF_RANGE,
            (Vec2(Double.MAX_VALUE, Double.MAX_VALUE).normalized() as GeometryResult.Failure).reason,
        )
    }

    @Test
    fun landmarkPoseFrameAndMetricResultContractsWorkProperly() {
        val good = Landmark(1.0f, 2.0f, 3.0f, 0.9f)
        val lowVis = Landmark(1.0f, 2.0f, 3.0f, 0.2f)
        val nanLm = Landmark(Float.NaN, 0.0f, 0.0f, 0.9f)

        assertTrue(good.isReliable())
        assertFalse(lowVis.isReliable())
        assertFalse(nanLm.isReliable())

        val frame =
            PoseFrame(
                timestampMs = 500L,
                image = mapOf(Joint.NOSE to good),
                world = mapOf(Joint.NOSE to lowVis, Joint.LEFT_SHOULDER to nanLm),
            )
        assertEquals(good, frame.landmarks(CoordinateSpace.IMAGE)[Joint.NOSE])
        assertEquals(
            UnavailableReason.MISSING_JOINT,
            (frame.extractReliablePoint(Joint.RIGHT_HIP) as GeometryResult.Failure).reason,
        )
        assertEquals(
            UnavailableReason.LOW_VISIBILITY,
            (frame.extractReliablePoint(Joint.NOSE) as GeometryResult.Failure).reason,
        )
        assertEquals(
            UnavailableReason.OUT_OF_RANGE,
            (frame.extractReliablePoint(Joint.LEFT_SHOULDER) as GeometryResult.Failure).reason,
        )

        val metricOk = GeometryResult.Success(42.0).toMetricResult(0.88f)
        assertEquals(MetricResult.Value(42.0, 0.88f), metricOk)
        val metricErr = GeometryResult.Failure(UnavailableReason.UNSTABLE, "noisy").toMetricResult()
        assertEquals(MetricResult.Unavailable(UnavailableReason.UNSTABLE), metricErr)
    }

    @Test(expected = IllegalArgumentException::class)
    fun metricResultValueRejectsNaN() {
        MetricResult.Value(Double.NaN, 1.0f)
    }
}
