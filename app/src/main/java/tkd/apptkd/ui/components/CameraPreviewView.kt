package tkd.apptkd.ui.components

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import tkd.apptkd.camera.CameraFacing
import tkd.apptkd.camera.CameraManager

/**
 * Componente Compose que integra la vista previa de CameraX mediante AndroidView.
 */
@Composable
fun CameraPreviewView(
    cameraManager: CameraManager,
    cameraFacing: CameraFacing,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(cameraFacing) {
        cameraManager.switchCamera(lifecycleOwner, previewView, cameraFacing)
    }

    DisposableEffect(lifecycleOwner) {
        cameraManager.startCamera(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            facing = cameraFacing,
        )

        onDispose {
            cameraManager.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize(),
    )
}

