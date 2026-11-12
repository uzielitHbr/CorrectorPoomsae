package mx.taekwondo.poomsae.analysis.model

/**
 * Reason why a geometric primitive or movement metric could not be computed (Section 12.9.2).
 */
enum class UnavailableReason {
    /** Required anatomical joint landmark is absent from the pose frame. */
    MISSING_JOINT,

    /** Joint visibility score is below [mx.taekwondo.poomsae.analysis.config.AnalysisConfig.VIS_MIN]. */
    LOW_VISIBILITY,

    /** Signal variance or MAD across the evaluation window exceeds the stability threshold. */
    UNSTABLE,

    /** Coordinate or resulting scalar value is non-finite (NaN/Infinite) or outside physical plausibility bounds. */
    OUT_OF_RANGE,

    /** Zero-length vector, coincident points, or collinear geometry makes the computation undefined. */
    DEGENERATE_GEOMETRY,

    /** Body profile calibration has not completed or failed stability checks. */
    NO_CALIBRATION,

    /** Initial heading reference vectors (F_0, r_0) are missing (e.g., resumed session). */
    NO_HEADING_REFERENCE,

    /** Metric was disabled after reliability experiment E3 due to excessive measurement error. */
    DISABLED_LOW_RELIABILITY,
}

/**
 * Explicit outcome of any geometric measurement or biomechanical metric evaluation (Section 12.9.2).
 */
sealed interface MetricResult {
    /**
     * Successfully computed finite metric value paired with its joint visibility confidence.
     *
     * @property value Finite scalar result in the metric's documented unit.
     * @property confidence Minimum visibility score `[0.0, 1.0]` among the participating landmarks.
     */
    data class Value(
        val value: Double,
        val confidence: Float,
    ) : MetricResult {
        init {
            require(value.isFinite()) { "MetricResult.Value cannot hold non-finite value: $value" }
            require(confidence.isFinite()) { "MetricResult.Value cannot hold non-finite confidence: $confidence" }
        }
    }

    /**
     * Indicates the measurement could not be reliably evaluated, specifying [reason].
     */
    data class Unavailable(
        val reason: UnavailableReason,
    ) : MetricResult
}
