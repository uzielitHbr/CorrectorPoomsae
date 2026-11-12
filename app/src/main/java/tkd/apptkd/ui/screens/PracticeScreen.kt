package tkd.apptkd.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import tkd.apptkd.R
import tkd.apptkd.camera.CameraFacing
import tkd.apptkd.camera.CameraManager
import tkd.apptkd.camera.CameraPermissionState
import tkd.apptkd.camera.CameraPermissionViewModel
import tkd.apptkd.pose.MediaPipePoseEstimator
import tkd.apptkd.ui.components.CameraPreviewView
import tkd.apptkd.ui.components.PoseOverlay

private val SemiTransparentDark = Color(0x99000000)

/**
 * Pantalla de Práctica con vista previa de cámara en tiempo real, gestión completa
 * del permiso y proyección del esqueleto anatómico sobre la pose detectada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    onNavigateBack: () -> Unit,
    onFinishPractice: () -> Unit,
    modifier: Modifier = Modifier,
    permissionViewModel: CameraPermissionViewModel = viewModel(),
) {
    val context = LocalContext.current
    val permissionState by permissionViewModel.state.collectAsState()

    var cameraFacing by remember { mutableStateOf(CameraFacing.FRONT) }

    val poseEstimator = remember {
        MediaPipePoseEstimator(context.applicationContext).apply { start() }
    }
    val cameraManager = remember {
        CameraManager(context.applicationContext, poseEstimator)
    }

    DisposableEffect(Unit) {
        onDispose {
            poseEstimator.stop()
            cameraManager.shutdown()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        val shouldShowRationale = (context as? Activity)?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } ?: false
        permissionViewModel.onPermissionResult(isGranted, shouldShowRationale)
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        val shouldShowRationale = (context as? Activity)?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } ?: false
        permissionViewModel.onCheckInitialPermission(hasPermission, shouldShowRationale)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.screen_practice)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text(text = "✕", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (permissionState) {
                is CameraPermissionState.Granted -> {
                    CameraActiveContent(
                        cameraManager = cameraManager,
                        poseEstimator = poseEstimator,
                        cameraFacing = cameraFacing,
                        onToggleCamera = { cameraFacing = cameraFacing.toggle() },
                        onFinishPractice = onFinishPractice,
                    )
                }
                is CameraPermissionState.RationaleRequired -> {
                    PermissionCardContent(
                        message = stringResource(id = R.string.MSG_PERM_RATIONALE),
                        buttonText = stringResource(id = R.string.btn_grant_permission),
                        onButtonClick = {
                            permissionViewModel.onUserRequestsPermission()
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                    )
                }
                is CameraPermissionState.PermanentlyDenied -> {
                    PermissionCardContent(
                        message = stringResource(id = R.string.MSG_PERM_SETTINGS),
                        buttonText = stringResource(id = R.string.btn_open_settings),
                        onButtonClick = { openAppSettings(context) },
                    )
                }
                else -> {
                    // NotDetermined / Requesting / Denied
                    PermissionCardContent(
                        message = stringResource(id = R.string.MSG_PERM_RATIONALE),
                        buttonText = stringResource(id = R.string.btn_grant_permission),
                        onButtonClick = {
                            permissionViewModel.onUserRequestsPermission()
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraActiveContent(
    cameraManager: CameraManager,
    poseEstimator: MediaPipePoseEstimator,
    cameraFacing: CameraFacing,
    onToggleCamera: () -> Unit,
    onFinishPractice: () -> Unit,
) {
    val poseFrame by poseEstimator.frames.collectAsState(initial = null)
    val metrics by poseEstimator.metrics.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreviewView(
            cameraManager = cameraManager,
            cameraFacing = cameraFacing,
        )

        PoseOverlay(
            poseFrame = poseFrame,
            cameraFacing = cameraFacing,
        )

        // Barra superior con métricas de depuración (FPS y cuadros descartados)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(SemiTransparentDark, shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.camera_fps_label, metrics.fps),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(id = R.string.camera_dropped_frames_label, metrics.droppedFrames),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onToggleCamera) {
                Text(text = stringResource(id = R.string.btn_switch_camera))
            }
        }

        // Botón inferior para finalizar práctica
        Button(
            onClick = onFinishPractice,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        ) {
            Text(text = stringResource(id = R.string.screen_summary))
        }
    }
}

@Composable
private fun PermissionCardContent(
    message: String,
    buttonText: String,
    onButtonClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = R.string.camera_permission_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onButtonClick) {
                    Text(text = buttonText)
                }
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
