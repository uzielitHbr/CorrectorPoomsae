package mx.taekwondo.poomsae.analysis.signal

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.geometry.GeometryResult
import mx.taekwondo.poomsae.analysis.geometry.Vec3
import mx.taekwondo.poomsae.analysis.model.UnavailableReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sin

class SignalFilterTest {
    @Test
    fun oneEuroFilterSignificantlyReducesVarianceOfNoisyStationarySignal() {
        val filter = OneEuroFilter()
        val rawSamples = ArrayList<Double>(60)
        val filteredSamples = ArrayList<Double>(60)

        // Generate 60 samples at 30 Hz (33 ms intervals): constant base 1.0 + deterministic high-freq oscillation
        for (i in 0 until 60) {
            val tMs = i * 33L
            val noise = 0.2 * sin(i * 1.9)
            val raw = 1.0 + noise
            rawSamples.add(raw)
            val filtered = (filter.filter(raw, tMs) as GeometryResult.Success).value
            filteredSamples.add(filtered)
        }

        val rawVar = (SignalStats.variance(rawSamples.drop(10)) as GeometryResult.Success).value
        val filteredVar = (SignalStats.variance(filteredSamples.drop(10)) as GeometryResult.Success).value
        assertTrue(
            "Expected filtered variance ($filteredVar) to be < 25% of raw variance ($rawVar)",
            filteredVar < rawVar * 0.25,
        )
    }

    @Test
    fun oneEuroFilterPreservesRampWithoutExcessiveLag() {
        val filter =
            OneEuroFilter(
                minCutoffHz = AnalysisConfig.ONE_EURO_MIN_CUTOFF_HZ,
                beta = 2.5,
                dCutoffHz = AnalysisConfig.ONE_EURO_D_CUTOFF_HZ,
            )
        // Ramp signal: x(t) = 2.0 * t_seconds sampled at 30 Hz (dt = 33 ms) over 1 second
        var lastRaw = 0.0
        var lastFiltered = 0.0
        for (i in 0..30) {
            val tMs = i * 33L
            lastRaw = 2.0 * (tMs / 1000.0)
            lastFiltered = (filter.filter(lastRaw, tMs) as GeometryResult.Success).value
        }

        val steadyStateLag = abs(lastRaw - lastFiltered)
        // Static low-alpha EMA (alpha=0.1) would lag by ~0.59 units at dt=33ms; adaptive OneEuro keeps lag small
        assertTrue(
            "Expected ramp lag ($steadyStateLag) < 0.15 units at end of 2.0/s ramp",
            steadyStateLag < 0.15,
        )
    }

    @Test
    fun oneEuroFilterRejectsNonFiniteAndNonIncreasingTimestampsWithoutStateCorruption() {
        val filter = OneEuroFilter()
        assertEquals(10.0, (filter.filter(10.0, 100L) as GeometryResult.Success).value, AnalysisConfig.EPS_SCALAR)

        val nanRes = filter.filter(Double.NaN, 133L)
        assertEquals(UnavailableReason.OUT_OF_RANGE, (nanRes as GeometryResult.Failure).reason)

        val sameTs = filter.filter(12.0, 100L)
        assertEquals(UnavailableReason.OUT_OF_RANGE, (sameTs as GeometryResult.Failure).reason)

        val pastTs = filter.filter(12.0, 90L)
        assertEquals(UnavailableReason.OUT_OF_RANGE, (pastTs as GeometryResult.Failure).reason)

        // State must remain valid and continue cleanly at t = 133L
        val recovered = filter.filter(10.0, 133L)
        assertTrue(recovered is GeometryResult.Success)
        assertEquals(10.0, (recovered as GeometryResult.Success).value, AnalysisConfig.EPS_SCALAR)

        filter.reset()
        assertEquals(50.0, (filter.filter(50.0, 0L) as GeometryResult.Success).value, AnalysisConfig.EPS_SCALAR)
    }

    @Test(expected = IllegalArgumentException::class)
    fun oneEuroFilterRejectsNonPositiveMinCutoff() {
        OneEuroFilter(minCutoffHz = 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun oneEuroFilterRejectsNegativeBeta() {
        OneEuroFilter(beta = -0.1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun oneEuroFilterRejectsNonPositiveDCutoff() {
        OneEuroFilter(dCutoffHz = -1.0)
    }

    @Test
    fun oneEuroFilter3DReducesNoiseAndGuardsInvalidVectors() {
        val filter3d = OneEuroFilter3D()
        val rawZ = ArrayList<Double>(40)
        val filteredZ = ArrayList<Double>(40)

        for (i in 0 until 40) {
            val zNoise = 0.25 * sin(i * 2.1)
            val v = Vec3(0.0, 1.0, 2.0 + zNoise)
            rawZ.add(v.z)
            val out = (filter3d.filter(v, i * 33L) as GeometryResult.Success).value
            filteredZ.add(out.z)
        }

        val vRaw = (SignalStats.variance(rawZ.drop(5)) as GeometryResult.Success).value
        val vFilt = (SignalStats.variance(filteredZ.drop(5)) as GeometryResult.Success).value
        assertTrue(vFilt < vRaw * 0.25)

        // NaN vector rejected without corrupting subsequent samples
        val nanErr = filter3d.filter(Vec3(Double.NaN, 1.0, 2.0), 2000L)
        assertEquals(UnavailableReason.OUT_OF_RANGE, (nanErr as GeometryResult.Failure).reason)

        val nonIncErr = filter3d.filter(Vec3(0.0, 1.0, 2.0), 1000L)
        assertEquals(UnavailableReason.OUT_OF_RANGE, (nonIncErr as GeometryResult.Failure).reason)

        filter3d.reset()
        val afterReset = (filter3d.filter(Vec3(3.0, 4.0, 5.0), 10L) as GeometryResult.Success).value
        assertEquals(Vec3(3.0, 4.0, 5.0), afterReset)
    }

    @Test
    fun emaFilterMatchesHandCalculationAndGuardsErrors() {
        // Hand calculation with alpha = 0.5:
        // step 1 (t=0): x=10.0 -> 10.0
        // step 2 (t=33): x=20.0 -> 0.5*20.0 + 0.5*10.0 = 15.0
        // step 3 (t=66): x=20.0 -> 0.5*20.0 + 0.5*15.0 = 17.5
        val ema = ExponentialMovingAverageFilter(alpha = 0.5)
        assertEquals(10.0, (ema.filter(10.0, 0L) as GeometryResult.Success).value, AnalysisConfig.EPS_SCALAR)
        assertEquals(15.0, (ema.filter(20.0, 33L) as GeometryResult.Success).value, AnalysisConfig.EPS_SCALAR)
        assertEquals(17.5, (ema.filter(20.0, 66L) as GeometryResult.Success).value, AnalysisConfig.EPS_SCALAR)

        assertEquals(
            UnavailableReason.OUT_OF_RANGE,
            (ema.filter(Double.POSITIVE_INFINITY, 100L) as GeometryResult.Failure).reason,
        )
        assertEquals(
            UnavailableReason.OUT_OF_RANGE,
            (ema.filter(20.0, 66L) as GeometryResult.Failure).reason,
        )

        ema.reset()
        assertEquals(42.0, (ema.filter(42.0, 0L) as GeometryResult.Success).value, AnalysisConfig.EPS_SCALAR)
    }

    @Test(expected = IllegalArgumentException::class)
    fun emaFilterRejectsInvalidAlphaZero() {
        ExponentialMovingAverageFilter(alpha = 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun emaFilterRejectsInvalidAlphaAboveOne() {
        ExponentialMovingAverageFilter(alpha = 1.1)
    }

    @Test
    fun signalStatsMedianMadAndVarianceMatchHandCalculations() {
        // Odd list: [9, 1, 2, 6, 4, 1, 2] -> sorted: [1, 1, 2, 2, 4, 6, 9] -> median = 2.0
        // Deviations from 2.0: [1, 1, 0, 0, 2, 4, 7] -> sorted: [0, 0, 1, 1, 2, 4, 7] -> MAD = 1.0
        val sample = listOf(9.0, 1.0, 2.0, 6.0, 4.0, 1.0, 2.0)
        assertEquals(2.0, (SignalStats.median(sample) as GeometryResult.Success).value, AnalysisConfig.EPS_SCALAR)
        assertEquals(1.0, (SignalStats.mad(sample) as GeometryResult.Success).value, AnalysisConfig.EPS_SCALAR)

        // Even list: [10, 20, 30, 40] -> (20 + 30) / 2 = 25.0
        assertEquals(
            25.0,
            (SignalStats.median(listOf(40.0, 10.0, 30.0, 20.0)) as GeometryResult.Success).value,
            AnalysisConfig.EPS_SCALAR,
        )

        // Hand-calculated population variance: [2, 4, 4, 4, 5, 5, 7, 9]
        // mean = 40/8 = 5.0; squared diffs = 9+1+1+1+0+0+4+16 = 32; variance = 32/8 = 4.0
        val pop = listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)
        assertEquals(4.0, (SignalStats.variance(pop) as GeometryResult.Success).value, AnalysisConfig.EPS_SCALAR)

        // Empty and NaN error checks
        assertEquals(UnavailableReason.UNSTABLE, (SignalStats.median(emptyList()) as GeometryResult.Failure).reason)
        assertEquals(UnavailableReason.UNSTABLE, (SignalStats.variance(emptyList()) as GeometryResult.Failure).reason)
        assertEquals(UnavailableReason.UNSTABLE, (SignalStats.medianVec3(emptyList()) as GeometryResult.Failure).reason)
        assertEquals(
            UnavailableReason.OUT_OF_RANGE,
            (SignalStats.median(listOf(1.0, Double.NaN)) as GeometryResult.Failure).reason,
        )
        assertEquals(
            UnavailableReason.OUT_OF_RANGE,
            (SignalStats.variance(listOf(1.0, Double.POSITIVE_INFINITY)) as GeometryResult.Failure).reason,
        )
    }
}
