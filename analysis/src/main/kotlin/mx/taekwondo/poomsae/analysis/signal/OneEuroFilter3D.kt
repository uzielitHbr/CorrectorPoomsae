package mx.taekwondo.poomsae.analysis.signal

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.geometry.GeometryResult
import mx.taekwondo.poomsae.analysis.geometry.Vec3
import mx.taekwondo.poomsae.analysis.model.UnavailableReason

/**
 * Axis-wise 3D wrapper over [OneEuroFilter] for smoothing [Vec3] coordinates consistently.
 */
class OneEuroFilter3D(
    minCutoffHz: Double = AnalysisConfig.ONE_EURO_MIN_CUTOFF_HZ,
    beta: Double = AnalysisConfig.ONE_EURO_BETA,
    dCutoffHz: Double = AnalysisConfig.ONE_EURO_D_CUTOFF_HZ,
) {
    private val filterX = OneEuroFilter(minCutoffHz, beta, dCutoffHz)
    private val filterY = OneEuroFilter(minCutoffHz, beta, dCutoffHz)
    private val filterZ = OneEuroFilter(minCutoffHz, beta, dCutoffHz)

    /**
     * Filters all three components of [vector] atomically at [timestampMs].
     */
    fun filter(
        vector: Vec3,
        timestampMs: Long,
    ): GeometryResult<Vec3> =
        if (!vector.isFinite()) {
            GeometryResult.Failure(
                reason = UnavailableReason.OUT_OF_RANGE,
                message = "OneEuroFilter3D received non-finite vector: $vector",
            )
        } else {
            filterAxisGroup(vector, timestampMs)
        }

    private fun filterAxisGroup(
        vector: Vec3,
        timestampMs: Long,
    ): GeometryResult<Vec3> {
        val rx = filterX.filter(vector.x, timestampMs)
        val result: GeometryResult<Vec3> =
            if (rx is GeometryResult.Failure) {
                rx
            } else {
                val ry = filterY.filter(vector.y, timestampMs)
                val rz = filterZ.filter(vector.z, timestampMs)
                when {
                    ry is GeometryResult.Failure -> ry
                    rz is GeometryResult.Failure -> rz
                    rx is GeometryResult.Success && ry is GeometryResult.Success && rz is GeometryResult.Success ->
                        GeometryResult.Success(Vec3(rx.value, ry.value, rz.value))
                    else -> GeometryResult.Failure(UnavailableReason.OUT_OF_RANGE, "Unexpected 3D filter failure")
                }
            }
        return result
    }

    /** Resets all three underlying axis filters. */
    fun reset() {
        filterX.reset()
        filterY.reset()
        filterZ.reset()
    }
}
