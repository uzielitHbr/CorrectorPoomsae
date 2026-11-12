package mx.taekwondo.poomsae.analysis.signal

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.geometry.GeometryResult
import mx.taekwondo.poomsae.analysis.model.UnavailableReason
import kotlin.math.PI
import kotlin.math.abs

/**
 * Adaptive One Euro low-pass filter (Casiez et al., 2012) for real-time landmark smoothing.
 *
 * Balances jitter suppression at low velocity with minimal phase lag during rapid movements
 * by raising the cutoff frequency proportionally to signal speed.
 *
 * @property minCutoffHz Minimum cutoff frequency in Hz at near-zero speed.
 * @property beta Speed coefficient (`>= 0`) increasing cutoff proportionally to speed.
 * @property dCutoffHz Cutoff frequency in Hz for derivative smoothing.
 */
class OneEuroFilter(
    private val minCutoffHz: Double = AnalysisConfig.ONE_EURO_MIN_CUTOFF_HZ,
    private val beta: Double = AnalysisConfig.ONE_EURO_BETA,
    private val dCutoffHz: Double = AnalysisConfig.ONE_EURO_D_CUTOFF_HZ,
) {
    private var lastXHat: Double? = null
    private var lastDxHat: Double = 0.0
    private var lastTimestampMs: Long? = null

    init {
        require(minCutoffHz.isFinite() && minCutoffHz > 0.0) {
            "minCutoffHz must be positive and finite, got $minCutoffHz"
        }
        require(beta.isFinite() && beta >= 0.0) {
            "beta must be non-negative and finite, got $beta"
        }
        require(dCutoffHz.isFinite() && dCutoffHz > 0.0) {
            "dCutoffHz must be positive and finite, got $dCutoffHz"
        }
    }

    /**
     * Filters [value] sampled at [timestampMs].
     *
     * Rejects non-finite inputs and non-increasing timestamps without corrupting internal state.
     */
    fun filter(
        value: Double,
        timestampMs: Long,
    ): GeometryResult<Double> {
        val prevTimestamp = lastTimestampMs
        val prevXHat = lastXHat
        return when {
            !value.isFinite() ->
                GeometryResult.Failure(
                    reason = UnavailableReason.OUT_OF_RANGE,
                    message = "OneEuroFilter received non-finite value: $value",
                )
            prevTimestamp != null && timestampMs <= prevTimestamp ->
                GeometryResult.Failure(
                    reason = UnavailableReason.OUT_OF_RANGE,
                    message = "Timestamp $timestampMs must be strictly greater than $prevTimestamp",
                )
            else -> {
                val next = computeFilteredStep(value, timestampMs, prevTimestamp, prevXHat)
                GeometryResult.Success(next)
            }
        }
    }

    /** Clears all internal filter state. */
    fun reset() {
        lastXHat = null
        lastDxHat = 0.0
        lastTimestampMs = null
    }

    private fun computeFilteredStep(
        value: Double,
        timestampMs: Long,
        prevTimestamp: Long?,
        prevXHat: Double?,
    ): Double {
        if (prevTimestamp == null || prevXHat == null) {
            lastXHat = value
            lastDxHat = 0.0
            lastTimestampMs = timestampMs
            return value
        }
        val dtSec = (timestampMs - prevTimestamp) / AnalysisConfig.MILLIS_PER_SECOND
        val alphaD = smoothingAlpha(dtSec, dCutoffHz)
        val dx = (value - prevXHat) / dtSec
        val dxHat = lowPass(alphaD, dx, lastDxHat)
        val cutoff = minCutoffHz + beta * abs(dxHat)
        val alpha = smoothingAlpha(dtSec, cutoff)
        val xHat = lowPass(alpha, value, prevXHat)

        lastXHat = xHat
        lastDxHat = dxHat
        lastTimestampMs = timestampMs
        return xHat
    }

    private fun smoothingAlpha(
        dtSec: Double,
        cutoffHz: Double,
    ): Double {
        val tau = 1.0 / (2.0 * PI * cutoffHz)
        return 1.0 / (1.0 + tau / dtSec)
    }

    private fun lowPass(
        alpha: Double,
        x: Double,
        prevX: Double,
    ): Double = alpha * x + (1.0 - alpha) * prevX
}
