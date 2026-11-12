package mx.taekwondo.poomsae.analysis.geometry

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.model.CoordinateSpace
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.MetricResult
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.model.UnavailableReason

/**
 * Explicit outcome of a geometric or signal operation that cannot fail silently with NaN or Infinity.
 */
sealed interface GeometryResult<out T> {
    /** Valid result containing [value]. */
    data class Success<T>(val value: T) : GeometryResult<T>

    /** Explicit failure indicating [reason] and diagnostic [message]. */
    data class Failure(
        val reason: UnavailableReason,
        val message: String,
    ) : GeometryResult<Nothing>
}

/**
 * Reliable 3D landmark point paired with its visibility score `[0.0, 1.0]`.
 */
data class ReliablePoint(
    val point: Vec3,
    val visibility: Float,
)

/**
 * Extracts a [Joint] from [PoseFrame] in [space], verifying presence, finiteness, and [minVisibility].
 */
fun PoseFrame.extractReliablePoint(
    joint: Joint,
    space: CoordinateSpace = CoordinateSpace.WORLD,
    minVisibility: Float = AnalysisConfig.VIS_MIN,
): GeometryResult<ReliablePoint> {
    val landmark = landmarks(space)[joint]
    return when {
        landmark == null ->
            GeometryResult.Failure(
                reason = UnavailableReason.MISSING_JOINT,
                message = "Missing joint $joint in $space coordinates",
            )
        !landmark.isFinite() ->
            GeometryResult.Failure(
                reason = UnavailableReason.OUT_OF_RANGE,
                message = "Non-finite coordinates or visibility for joint $joint",
            )
        landmark.visibility < minVisibility ->
            GeometryResult.Failure(
                reason = UnavailableReason.LOW_VISIBILITY,
                message = "Visibility ${landmark.visibility} for $joint is below $minVisibility",
            )
        else ->
            GeometryResult.Success(
                ReliablePoint(point = landmark.toVec3(), visibility = landmark.visibility),
            )
    }
}

/**
 * Extracts multiple required [joints] from [PoseFrame], failing with the first invalid joint encountered.
 */
fun PoseFrame.extractReliablePoints(
    vararg joints: Joint,
    space: CoordinateSpace = CoordinateSpace.WORLD,
    minVisibility: Float = AnalysisConfig.VIS_MIN,
): GeometryResult<Map<Joint, ReliablePoint>> {
    val result = LinkedHashMap<Joint, ReliablePoint>(joints.size)
    var failure: GeometryResult.Failure? = null
    for (joint in joints) {
        when (val extracted = extractReliablePoint(joint, space, minVisibility)) {
            is GeometryResult.Success -> result[joint] = extracted.value
            is GeometryResult.Failure -> {
                failure = extracted
                break
            }
        }
    }
    return failure ?: GeometryResult.Success(result)
}

/** Converts a scalar [GeometryResult] into a [MetricResult]. */
fun GeometryResult<Double>.toMetricResult(confidence: Float = 1.0f): MetricResult =
    when (this) {
        is GeometryResult.Success -> MetricResult.Value(value = value, confidence = confidence)
        is GeometryResult.Failure -> MetricResult.Unavailable(reason = reason)
    }
