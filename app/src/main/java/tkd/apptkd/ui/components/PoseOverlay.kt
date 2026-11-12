package tkd.apptkd.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.model.CoordinateSpace
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.Landmark
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import tkd.apptkd.camera.CameraFacing

private val ColorLeft = Color(0xFF00E5FF) // Cyan para lado izquierdo anatómico
private val ColorRight = Color(0xFFFF9100) // Ámbar/Naranja para lado derecho anatómico
private val ColorCenter = Color(0xFFFFFFFF) // Blanco para conexiones de torso y cabeza

private val LeftBones = listOf(
    Joint.LEFT_SHOULDER to Joint.LEFT_ELBOW,
    Joint.LEFT_ELBOW to Joint.LEFT_WRIST,
    Joint.LEFT_HIP to Joint.LEFT_KNEE,
    Joint.LEFT_KNEE to Joint.LEFT_ANKLE,
    Joint.LEFT_ANKLE to Joint.LEFT_HEEL,
    Joint.LEFT_HEEL to Joint.LEFT_FOOT_INDEX,
    Joint.LEFT_ANKLE to Joint.LEFT_FOOT_INDEX,
)

private val RightBones = listOf(
    Joint.RIGHT_SHOULDER to Joint.RIGHT_ELBOW,
    Joint.RIGHT_ELBOW to Joint.RIGHT_WRIST,
    Joint.RIGHT_HIP to Joint.RIGHT_KNEE,
    Joint.RIGHT_KNEE to Joint.RIGHT_ANKLE,
    Joint.RIGHT_ANKLE to Joint.RIGHT_HEEL,
    Joint.RIGHT_HEEL to Joint.RIGHT_FOOT_INDEX,
    Joint.RIGHT_ANKLE to Joint.RIGHT_FOOT_INDEX,
)

private val TorsoBones = listOf(
    Joint.LEFT_SHOULDER to Joint.RIGHT_SHOULDER,
    Joint.LEFT_HIP to Joint.RIGHT_HIP,
    Joint.LEFT_SHOULDER to Joint.LEFT_HIP,
    Joint.RIGHT_SHOULDER to Joint.RIGHT_HIP,
)

private data class OverlayDrawContext(
    val landmarks: Map<Joint, Landmark>,
    val width: Float,
    val height: Float,
    val isFrontCamera: Boolean,
)

/**
 * Lienzo en Compose que proyecta el esqueleto anatómico sobre la vista previa de la cámara.
 *
 * Aplica el espejado horizontal si la cámara es frontal para que coincida exactamente
 * con el movimiento natural de espejo del usuario, manteniendo la lateralidad anatómica.
 */
@Composable
fun PoseOverlay(
    poseFrame: PoseFrame?,
    cameraFacing: CameraFacing,
    modifier: Modifier = Modifier,
) {
    if (poseFrame == null) return

    val landmarks = poseFrame.landmarks(CoordinateSpace.IMAGE)
    val isFrontCamera = cameraFacing == CameraFacing.FRONT

    Canvas(modifier = modifier.fillMaxSize()) {
        val ctx = OverlayDrawContext(
            landmarks = landmarks,
            width = size.width,
            height = size.height,
            isFrontCamera = isFrontCamera,
        )

        drawSkeletonBones(ctx, TorsoBones, ColorCenter)
        drawSkeletonBones(ctx, LeftBones, ColorLeft)
        drawSkeletonBones(ctx, RightBones, ColorRight)
        drawJointMarkers(ctx)
    }
}

private fun DrawScope.drawSkeletonBones(
    ctx: OverlayDrawContext,
    bones: List<Pair<Joint, Joint>>,
    color: Color,
) {
    val strokePx = 4.dp.toPx()
    for ((fromJoint, toJoint) in bones) {
        val p1 = toCanvasOffset(ctx.landmarks[fromJoint], ctx.width, ctx.height, ctx.isFrontCamera)
        val p2 = toCanvasOffset(ctx.landmarks[toJoint], ctx.width, ctx.height, ctx.isFrontCamera)
        if (p1 != null && p2 != null) {
            drawLine(
                color = color,
                start = p1,
                end = p2,
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawJointMarkers(ctx: OverlayDrawContext) {
    val radiusPx = 5.dp.toPx()
    for (joint in Joint.entries) {
        val offset = toCanvasOffset(ctx.landmarks[joint], ctx.width, ctx.height, ctx.isFrontCamera) ?: continue
        val pointColor = when {
            joint.name.startsWith("LEFT_") -> ColorLeft
            joint.name.startsWith("RIGHT_") -> ColorRight
            else -> ColorCenter
        }
        drawCircle(
            color = pointColor,
            radius = radiusPx,
            center = offset,
        )
    }
}

private fun toCanvasOffset(
    landmark: Landmark?,
    width: Float,
    height: Float,
    isFrontCamera: Boolean,
): Offset? =
    if (landmark != null && landmark.visibility >= AnalysisConfig.VIS_MIN) {
        val rawX = landmark.x.toFloat()
        val canvasX = if (isFrontCamera) (1.0f - rawX) * width else rawX * width
        Offset(canvasX, landmark.y.toFloat() * height)
    } else {
        null
    }
