package mx.taekwondo.poomsae.analysis.signal

import mx.taekwondo.poomsae.analysis.geometry.GeometryResult
import mx.taekwondo.poomsae.analysis.geometry.Vec3
import mx.taekwondo.poomsae.analysis.model.UnavailableReason
import kotlin.math.abs

/**
 * Pure statistical utilities for signal windows, calibration medians, and noise metrics.
 */
object SignalStats {
    private const val HALF: Double = 0.5

    /**
     * Computes the exact median of a non-empty list of finite [values].
     * Returns [GeometryResult.Failure] if [values] is empty or contains non-finite values.
     */
    fun median(values: List<Double>): GeometryResult<Double> =
        when {
            values.isEmpty() ->
                GeometryResult.Failure(
                    reason = UnavailableReason.UNSTABLE,
                    message = "Cannot compute median of an empty list",
                )
            values.any { !it.isFinite() } ->
                GeometryResult.Failure(
                    reason = UnavailableReason.OUT_OF_RANGE,
                    message = "Cannot compute median when values contain NaN or Infinity",
                )
            else -> {
                val sorted = values.sorted()
                val mid = sorted.size / 2
                val result =
                    if (sorted.size % 2 == 1) {
                        sorted[mid]
                    } else {
                        (sorted[mid - 1] + sorted[mid]) * HALF
                    }
                GeometryResult.Success(result)
            }
        }

    /**
     * Computes the component-wise median of a non-empty list of finite [Vec3] vectors.
     */
    fun medianVec3(vectors: List<Vec3>): GeometryResult<Vec3> =
        if (vectors.isEmpty()) {
            GeometryResult.Failure(
                reason = UnavailableReason.UNSTABLE,
                message = "Cannot compute median Vec3 of an empty list",
            )
        } else {
            val mx = median(vectors.map { it.x })
            val my = median(vectors.map { it.y })
            val mz = median(vectors.map { it.z })
            when {
                mx is GeometryResult.Failure -> mx
                my is GeometryResult.Failure -> my
                mz is GeometryResult.Failure -> mz
                mx is GeometryResult.Success && my is GeometryResult.Success && mz is GeometryResult.Success ->
                    GeometryResult.Success(Vec3(mx.value, my.value, mz.value))
                else -> GeometryResult.Failure(UnavailableReason.OUT_OF_RANGE, "Unexpected median state")
            }
        }

    /**
     * Computes the Median Absolute Deviation (MAD): `median(|x_i - median(x)|)`.
     */
    fun mad(values: List<Double>): GeometryResult<Double> =
        when (val med = median(values)) {
            is GeometryResult.Failure -> med
            is GeometryResult.Success -> median(values.map { abs(it - med.value) })
        }

    /**
     * Computes the population variance `sum((x_i - mean)^2) / N` of a non-empty list of finite [values].
     */
    fun variance(values: List<Double>): GeometryResult<Double> =
        when {
            values.isEmpty() ->
                GeometryResult.Failure(
                    reason = UnavailableReason.UNSTABLE,
                    message = "Cannot compute variance of an empty list",
                )
            values.any { !it.isFinite() } ->
                GeometryResult.Failure(
                    reason = UnavailableReason.OUT_OF_RANGE,
                    message = "Cannot compute variance when values contain NaN or Infinity",
                )
            else -> {
                val mean = values.sum() / values.size
                val sumSq =
                    values.sumOf { value ->
                        val diff = value - mean
                        diff * diff
                    }
                GeometryResult.Success(sumSq / values.size)
            }
        }
}
