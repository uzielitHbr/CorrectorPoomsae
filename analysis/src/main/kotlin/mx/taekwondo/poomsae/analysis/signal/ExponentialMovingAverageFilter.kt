package mx.taekwondo.poomsae.analysis.signal

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.geometry.GeometryResult
import mx.taekwondo.poomsae.analysis.model.UnavailableReason

/**
 * Exponential Moving Average (EMA) low-pass filter for scalar signals.
 *
 * @property alpha Smoothing factor in `(0.0, 1.0]`. Smaller values produce stronger smoothing.
 */
class ExponentialMovingAverageFilter(
    private val alpha: Double = AnalysisConfig.EMA_DEFAULT_ALPHA,
) {
    private var lastFiltered: Double? = null
    private var lastTimestampMs: Long? = null

    init {
        require(alpha.isFinite() && alpha > 0.0 && alpha <= 1.0) {
            "EMA alpha must be finite in (0.0, 1.0], got $alpha"
        }
    }

    /**
     * Filters [value] at [timestampMs]. Rejects non-finite inputs and non-increasing timestamps
     * without corrupting internal filter state.
     */
    fun filter(
        value: Double,
        timestampMs: Long,
    ): GeometryResult<Double> {
        val prevTimestamp = lastTimestampMs
        val prevFiltered = lastFiltered
        return when {
            !value.isFinite() ->
                GeometryResult.Failure(
                    reason = UnavailableReason.OUT_OF_RANGE,
                    message = "EMA filter received non-finite value: $value",
                )
            prevTimestamp != null && timestampMs <= prevTimestamp ->
                GeometryResult.Failure(
                    reason = UnavailableReason.OUT_OF_RANGE,
                    message = "Timestamp $timestampMs must be strictly greater than $prevTimestamp",
                )
            else -> {
                val next = if (prevFiltered == null) value else alpha * value + (1.0 - alpha) * prevFiltered
                lastFiltered = next
                lastTimestampMs = timestampMs
                GeometryResult.Success(next)
            }
        }
    }

    /** Clears internal filter state. */
    fun reset() {
        lastFiltered = null
        lastTimestampMs = null
    }
}
