package mx.taekwondo.poomsae.analysis.model

import kotlinx.coroutines.flow.Flow

/**
 * Coordinate space selector within a [PoseFrame].
 */
enum class CoordinateSpace {
    /** Normalized 2D+depth coordinates in camera image frame. */
    IMAGE,

    /** Metric 3D coordinates in world space (meters, origin at hip midpoint). */
    WORLD,
}

/**
 * Snapshot of body landmarks extracted at a single deterministic instant [timestampMs] (Section 5.4).
 *
 * Analysis functions must use [timestampMs] strictly and never wall-clock time (Section 12.9.1).
 *
 * @property timestampMs Frame timestamp in milliseconds (monotonic).
 * @property image Landmarks in normalized image coordinates.
 * @property world Landmarks in metric 3D world coordinates (meters).
 */
data class PoseFrame(
    val timestampMs: Long,
    val image: Map<Joint, Landmark>,
    val world: Map<Joint, Landmark>,
) {
    /** Returns the landmark map for the specified [space]. */
    fun landmarks(space: CoordinateSpace): Map<Joint, Landmark> =
        when (space) {
            CoordinateSpace.IMAGE -> image
            CoordinateSpace.WORLD -> world
        }
}

/**
 * Abstraction over a live or offline pose detection source producing [PoseFrame] emissions (Section 5.4).
 * A `null` emission indicates that no person was detected in that frame.
 */
interface PoseEstimator {
    /** Hot or cold stream of detected pose frames; `null` when no person is present. */
    val frames: Flow<PoseFrame?>

    /** Starts pose estimation emissions. */
    fun start()

    /** Stops pose estimation and releases processing resources. */
    fun stop()
}
