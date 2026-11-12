package tkd.apptkd.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.model.CoordinateSpace
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.Landmark
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import tkd.apptkd.camera.CameraFacing

private const val BONE_STROKE_DP = 4
private const val OUTLINE_EXTRA_DP = 2
private const val JOINT_RADIUS_DP = 6
private const val JOINT_OUTLINE_EXTRA_DP = 1
private const val DIMMED_ALPHA = 0.4f
private const val OUTLINE_ALPHA = 0.8f
private const val LOW_VIS_FACTOR = 0.5f
private const val DASH_ON = 12f
private const val DASH_OFF = 8f
private val COLOR_CYAN = Color(0xFF00E5FF)
private val COLOR_ORANGE = Color(0xFFFF9100)

enum class SkeletonDisplayState {
    VISIBLE,
    DIMMED,
    HIDDEN,
}

private val SkeletonBones = listOf(
    // Torso
    Joint.LEFT_SHOULDER to Joint.RIGHT_SHOULDER,
    Joint.LEFT_HIP to Joint.RIGHT_HIP,
    Joint.LEFT_SHOULDER to Joint.LEFT_HIP,
    Joint.RIGHT_SHOULDER to Joint.RIGHT_HIP,
    // Brazo izquierdo
    Joint.LEFT_SHOULDER to Joint.LEFT_ELBOW,
    Joint.LEFT_ELBOW to Joint.LEFT_WRIST,
    // Brazo derecho
    Joint.RIGHT_SHOULDER to Joint.RIGHT_ELBOW,
    Joint.RIGHT_ELBOW to Joint.RIGHT_WRIST,
    // Pierna izquierda
    Joint.LEFT_HIP to Joint.LEFT_KNEE,
    Joint.LEFT_KNEE to Joint.LEFT_ANKLE,
    Joint.LEFT_ANKLE to Joint.LEFT_HEEL,
    Joint.LEFT_HEEL to Joint.LEFT_FOOT_INDEX,
    Joint.LEFT_ANKLE to Joint.LEFT_FOOT_INDEX,
    // Pierna derecha
    Joint.RIGHT_HIP to Joint.RIGHT_KNEE,
    Joint.RIGHT_KNEE to Joint.RIGHT_ANKLE,
    Joint.RIGHT_ANKLE to Joint.RIGHT_HEEL,
    Joint.RIGHT_HEEL to Joint.RIGHT_FOOT_INDEX,
    Joint.RIGHT_ANKLE to Joint.RIGHT_FOOT_INDEX,
)

/**
 * Superposición anatómica del esqueleto sobre la vista previa (Sección 14.2, 14.4 y 14.6).
 *
 * Utiliza líneas blancas de 4 dp con borde oscuro fino, articulaciones de 6 dp de radio,
 * y trazo atenuado y discontinuo para articulaciones con visibilidad por debajo de VIS_MIN.
 */
@Composable
fun SkeletonOverlay(
    poseFrame: PoseFrame?,
    cameraFacing: CameraFacing,
    modifier: Modifier = Modifier,
    state: SkeletonDisplayState = SkeletonDisplayState.VISIBLE,
) {
    if (state == SkeletonDisplayState.HIDDEN || poseFrame == null) return

    val landmarks = poseFrame.landmarks(CoordinateSpace.IMAGE)
    val isFrontCamera = cameraFacing == CameraFacing.FRONT
    val alphaFactor = if (state == SkeletonDisplayState.DIMMED) DIMMED_ALPHA else 1.0f

    Canvas(modifier = modifier.fillMaxSize()) {
        val renderContext = SkeletonRenderContext(
            drawScope = this,
            width = size.width,
            height = size.height,
            isFrontCamera = isFrontCamera,
            alphaFactor = alphaFactor,
        )
        SkeletonDrawHelpers.drawAllBones(renderContext, landmarks)
        SkeletonDrawHelpers.drawAllJoints(renderContext, landmarks)
    }
}

internal data class SkeletonRenderContext(
    val drawScope: DrawScope,
    val width: Float,
    val height: Float,
    val isFrontCamera: Boolean,
    val alphaFactor: Float,
)

internal object SkeletonDrawHelpers {

    fun drawAllBones(
        ctx: SkeletonRenderContext,
        landmarks: Map<Joint, Landmark>,
    ) {
        val strokeWidth = with(ctx.drawScope) { BONE_STROKE_DP.dp.toPx() }
        val outlineWidth = with(ctx.drawScope) { strokeWidth + OUTLINE_EXTRA_DP.dp.toPx() }
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_ON, DASH_OFF), 0f)

        for ((fromJoint, toJoint) in SkeletonBones) {
            val lm1 = landmarks[fromJoint]
            val lm2 = landmarks[toJoint]
            val p1 = resolveOffset(lm1, ctx.width, ctx.height, ctx.isFrontCamera)
            val p2 = resolveOffset(lm2, ctx.width, ctx.height, ctx.isFrontCamera)

            if (p1 != null && p2 != null) {
                val isLowVis = (lm1?.visibility ?: 0f) < AnalysisConfig.VIS_MIN ||
                    (lm2?.visibility ?: 0f) < AnalysisConfig.VIS_MIN

                val pathEffect = if (isLowVis) dashEffect else null
                val effectiveAlpha = if (isLowVis) ctx.alphaFactor * LOW_VIS_FACTOR else ctx.alphaFactor

                ctx.drawScope.drawLine(
                    color = Color.Black.copy(alpha = effectiveAlpha * OUTLINE_ALPHA),
                    start = p1,
                    end = p2,
                    strokeWidth = outlineWidth,
                    cap = StrokeCap.Round,
                    pathEffect = pathEffect,
                )
                ctx.drawScope.drawLine(
                    color = Color.White.copy(alpha = effectiveAlpha),
                    start = p1,
                    end = p2,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                    pathEffect = pathEffect,
                )
            }
        }
    }

    fun drawAllJoints(
        ctx: SkeletonRenderContext,
        landmarks: Map<Joint, Landmark>,
    ) {
        val jointRadius = with(ctx.drawScope) { JOINT_RADIUS_DP.dp.toPx() }
        val outlineRadius = with(ctx.drawScope) { jointRadius + JOINT_OUTLINE_EXTRA_DP.dp.toPx() }

        for (joint in Joint.entries) {
            val lm = landmarks[joint]
            val offset = resolveOffset(lm, ctx.width, ctx.height, ctx.isFrontCamera) ?: continue
            val isLowVis = (lm?.visibility ?: 0f) < AnalysisConfig.VIS_MIN
            val effectiveAlpha = if (isLowVis) ctx.alphaFactor * LOW_VIS_FACTOR else ctx.alphaFactor

            ctx.drawScope.drawCircle(
                color = Color.Black.copy(alpha = effectiveAlpha * OUTLINE_ALPHA),
                radius = outlineRadius,
                center = offset,
            )
            val fillColor = resolveJointColor(joint.name, effectiveAlpha)
            ctx.drawScope.drawCircle(
                color = fillColor,
                radius = jointRadius,
                center = offset,
            )
        }
    }

    private fun resolveJointColor(name: String, alpha: Float): Color =
        when {
            name.startsWith("LEFT_") -> COLOR_CYAN.copy(alpha = alpha)
            name.startsWith("RIGHT_") -> COLOR_ORANGE.copy(alpha = alpha)
            else -> Color.White.copy(alpha = alpha)
        }

    fun resolveOffset(
        landmark: Landmark?,
        width: Float,
        height: Float,
        isFrontCamera: Boolean,
    ): Offset? =
        if (landmark != null && landmark.isFinite()) {
            val rawX = landmark.x.toFloat()
            val canvasX = if (isFrontCamera) (1.0f - rawX) * width else rawX * width
            Offset(canvasX, landmark.y.toFloat() * height)
        } else {
            null
        }
}
