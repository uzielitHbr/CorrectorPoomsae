package tkd.apptkd.ui

import androidx.annotation.StringRes
import mx.taekwondo.poomsae.analysis.evaluation.FeedbackCode
import mx.taekwondo.poomsae.analysis.video.VideoRejectionReason
import tkd.apptkd.R

/**
 * Mapeador de códigos normativos de dominio a recursos de texto en español de México (Sección 12.8).
 */
object FeedbackMessageMapper {

    private val RES_MAP: Map<FeedbackCode, Int> =
        mapOf(
            FeedbackCode.STANCE_STEP_TOO_SHORT to R.string.FB_STANCE_STEP_TOO_SHORT,
            FeedbackCode.STANCE_STEP_TOO_LONG to R.string.FB_STANCE_STEP_TOO_LONG,
            FeedbackCode.STANCE_FEET_TOO_NARROW to R.string.FB_STANCE_FEET_TOO_NARROW,
            FeedbackCode.STANCE_FEET_TOO_WIDE to R.string.FB_STANCE_FEET_TOO_WIDE,
            FeedbackCode.FRONT_KNEE_INSUFFICIENT_BEND to R.string.FB_FRONT_KNEE_INSUFFICIENT_BEND,
            FeedbackCode.FRONT_KNEE_OVERBENT to R.string.FB_FRONT_KNEE_OVERBENT,
            FeedbackCode.REAR_KNEE_NOT_EXTENDED to R.string.FB_REAR_KNEE_NOT_EXTENDED,
            FeedbackCode.TORSO_NOT_UPRIGHT to R.string.FB_TORSO_NOT_UPRIGHT,
            FeedbackCode.HIP_SWAY_EXCESSIVE to R.string.FB_HIP_SWAY_EXCESSIVE,
            FeedbackCode.ELBOW_OVERFLEXED to R.string.FB_ELBOW_OVERFLEXED,
            FeedbackCode.ELBOW_OVEREXTENDED to R.string.FB_ELBOW_OVEREXTENDED,
            FeedbackCode.PUNCH_ARM_NOT_EXTENDED to R.string.FB_PUNCH_ARM_NOT_EXTENDED,
            FeedbackCode.WRIST_TOO_LOW to R.string.FB_WRIST_TOO_LOW,
            FeedbackCode.WRIST_TOO_HIGH to R.string.FB_WRIST_TOO_HIGH,
            FeedbackCode.WRIST_LATERAL_OFFSET_INCORRECT to R.string.FB_WRIST_LATERAL_OFFSET_INCORRECT,
            FeedbackCode.PUNCH_NOT_FORWARD_ENOUGH to R.string.FB_PUNCH_NOT_FORWARD_ENOUGH,
            FeedbackCode.PUNCH_OFF_TARGET_HORIZONTAL to R.string.FB_PUNCH_OFF_TARGET_HORIZONTAL,
            FeedbackCode.FOREARM_INCLINATION_INCORRECT to R.string.FB_FOREARM_INCLINATION_INCORRECT,
            FeedbackCode.FIST_NOT_AT_SHOULDER_HEIGHT to R.string.FB_FIST_NOT_AT_SHOULDER_HEIGHT,
            FeedbackCode.BLOCK_WRIST_BELOW_HEAD to R.string.FB_BLOCK_WRIST_BELOW_HEAD,
            FeedbackCode.KICK_KNEE_NOT_HIGH_ENOUGH to R.string.FB_KICK_KNEE_NOT_HIGH_ENOUGH,
            FeedbackCode.KICK_HEIGHT_TOO_LOW to R.string.FB_KICK_HEIGHT_TOO_LOW,
            FeedbackCode.KICK_LEG_NOT_EXTENDED to R.string.FB_KICK_LEG_NOT_EXTENDED,
            FeedbackCode.SUPPORT_KNEE_BENT to R.string.FB_SUPPORT_KNEE_BENT,
            FeedbackCode.KICK_NOT_FORWARD to R.string.FB_KICK_NOT_FORWARD,
            FeedbackCode.TURN_ANGLE_INACCURATE to R.string.FB_TURN_ANGLE_INACCURATE,
            FeedbackCode.HEADING_DRIFT_EXCESSIVE to R.string.FB_HEADING_DRIFT_EXCESSIVE,
        )

    @StringRes
    fun getStringResId(code: FeedbackCode): Int =
        RES_MAP[code] ?: error("No string resource mapped for code: $code")

    @StringRes
    fun mapVideoRejection(reason: VideoRejectionReason): Int = when (reason) {
        VideoRejectionReason.FILE_TOO_LARGE -> R.string.video_err_too_large
        VideoRejectionReason.DURATION_TOO_LONG -> R.string.video_err_too_long
        VideoRejectionReason.RESOLUTION_TOO_HIGH -> R.string.video_err_resolution_too_high
        VideoRejectionReason.UNSUPPORTED_FORMAT,
        VideoRejectionReason.NO_VIDEO_TRACK,
        -> R.string.video_err_unsupported_format
    }
}
