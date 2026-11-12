package mx.taekwondo.poomsae.analysis.model

private const val MP_NOSE = 0
private const val MP_LEFT_SHOULDER = 11
private const val MP_RIGHT_SHOULDER = 12
private const val MP_LEFT_ELBOW = 13
private const val MP_RIGHT_ELBOW = 14
private const val MP_LEFT_WRIST = 15
private const val MP_RIGHT_WRIST = 16
private const val MP_LEFT_HIP = 23
private const val MP_RIGHT_HIP = 24
private const val MP_LEFT_KNEE = 25
private const val MP_RIGHT_KNEE = 26
private const val MP_LEFT_ANKLE = 27
private const val MP_RIGHT_ANKLE = 28
private const val MP_LEFT_HEEL = 29
private const val MP_RIGHT_HEEL = 30
private const val MP_LEFT_FOOT_INDEX = 31
private const val MP_RIGHT_FOOT_INDEX = 32

/**
 * Canonical anatomical joints mapped to their MediaPipe Pose Landmarker indices (Section 5.4).
 *
 * Note: "LEFT" and "RIGHT" always denote the practitioner's anatomical side,
 * never screen-left or screen-right.
 */
enum class Joint(
    /** Official 0-based landmark index in MediaPipe Pose (33-landmark topology). */
    val mediaPipeIndex: Int,
) {
    NOSE(MP_NOSE),
    LEFT_SHOULDER(MP_LEFT_SHOULDER),
    RIGHT_SHOULDER(MP_RIGHT_SHOULDER),
    LEFT_ELBOW(MP_LEFT_ELBOW),
    RIGHT_ELBOW(MP_RIGHT_ELBOW),
    LEFT_WRIST(MP_LEFT_WRIST),
    RIGHT_WRIST(MP_RIGHT_WRIST),
    LEFT_HIP(MP_LEFT_HIP),
    RIGHT_HIP(MP_RIGHT_HIP),
    LEFT_KNEE(MP_LEFT_KNEE),
    RIGHT_KNEE(MP_RIGHT_KNEE),
    LEFT_ANKLE(MP_LEFT_ANKLE),
    RIGHT_ANKLE(MP_RIGHT_ANKLE),
    LEFT_HEEL(MP_LEFT_HEEL),
    RIGHT_HEEL(MP_RIGHT_HEEL),
    LEFT_FOOT_INDEX(MP_LEFT_FOOT_INDEX),
    RIGHT_FOOT_INDEX(MP_RIGHT_FOOT_INDEX),
}
