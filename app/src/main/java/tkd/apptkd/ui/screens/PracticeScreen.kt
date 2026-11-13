package tkd.apptkd.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import tkd.apptkd.R
import tkd.apptkd.camera.CameraManager
import tkd.apptkd.camera.CameraPermissionState
import tkd.apptkd.camera.CameraPermissionViewModel
import tkd.apptkd.pose.MediaPipePoseEstimator
import tkd.apptkd.ui.components.CameraPreviewView
import tkd.apptkd.ui.components.GuidanceBanner
import tkd.apptkd.ui.components.MovementTimeline
import tkd.apptkd.ui.components.PrimaryButton
import tkd.apptkd.ui.components.SecondaryButton
import tkd.apptkd.ui.components.SkeletonOverlay
import tkd.apptkd.ui.components.TurnIndicator
import tkd.apptkd.ui.theme.OverlayScrim
import tkd.apptkd.ui.theme.TaegeukCoachTheme

private const val PRACTICE_TOUCH_DP = 56
private const val VOICE_BUTTON_WIDTH_DP = 90
private const val TOTAL_MOVEMENTS = 18

data class PracticeScaffoldCallbacks(
    val onAction: (PracticeAction) -> Unit,
    val onNavigateBack: () -> Unit,
    val onRequestPermission: () -> Unit,
    val onOpenSettings: () -> Unit,
)

/**
 * Pantalla de práctica en vivo con orientación vertical bloqueada (D-03, Sección 14.8).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    onNavigateBack: () -> Unit,
    onFinishPractice: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PracticeViewModel = hiltViewModel(),
    permissionViewModel: CameraPermissionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    val permissionState by permissionViewModel.state.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

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

    LaunchedEffect(poseEstimator) {
        poseEstimator.frames.collect { frame ->
            if (frame != null) viewModel.onAction(PracticeAction.ProcessFrame(frame))
        }
    }

    LaunchedEffect(poseEstimator) {
        poseEstimator.metrics.collect { metrics ->
            viewModel.onAction(PracticeAction.UpdateFpsMetrics(metrics.fps, metrics.droppedFrames))
        }
    }

    LaunchedEffect(uiState.step) {
        if (uiState.step is PracticeStep.Finished) onFinishPractice()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } ?: false
        permissionViewModel.onPermissionResult(isGranted, shouldShowRationale)
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } ?: false
        permissionViewModel.onCheckInitialPermission(hasPermission, shouldShowRationale)
    }

    BackHandler { viewModel.onAction(PracticeAction.SetExitDialogVisible(true)) }

    PracticeScaffoldContent(
        permissionState = permissionState,
        uiState = uiState,
        cameraManager = cameraManager,
        callbacks = PracticeScaffoldCallbacks(
            onAction = { viewModel.onAction(it) },
            onNavigateBack = onNavigateBack,
            onRequestPermission = {
                permissionViewModel.onUserRequestsPermission()
                permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onOpenSettings = { openAppSettings(context) },
        ),
        modifier = modifier,
    )
}

@Composable
private fun PracticeScaffoldContent(
    permissionState: CameraPermissionState,
    uiState: PracticeUiState,
    cameraManager: CameraManager,
    callbacks: PracticeScaffoldCallbacks,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (permissionState) {
                is CameraPermissionState.Granted -> {
                    CameraActivePractice(
                        uiState = uiState,
                        cameraManager = cameraManager,
                        onAction = callbacks.onAction,
                        onNavigateBack = callbacks.onNavigateBack,
                    )
                }
                is CameraPermissionState.RationaleRequired -> {
                    PermissionDialogContent(
                        message = stringResource(id = R.string.MSG_PERM_RATIONALE),
                        buttonText = stringResource(id = R.string.btn_grant_permission),
                        onButtonClick = callbacks.onRequestPermission,
                    )
                }
                is CameraPermissionState.PermanentlyDenied -> {
                    PermissionDialogContent(
                        message = stringResource(id = R.string.MSG_PERM_SETTINGS),
                        buttonText = stringResource(id = R.string.btn_open_settings),
                        onButtonClick = callbacks.onOpenSettings,
                    )
                }
                else -> {
                    PermissionDialogContent(
                        message = stringResource(id = R.string.MSG_PERM_RATIONALE),
                        buttonText = stringResource(id = R.string.btn_grant_permission),
                        onButtonClick = callbacks.onRequestPermission,
                    )
                }
            }

            if (uiState.showExitDialog) {
                ExitConfirmDialog(
                    hasEvaluatedMovements = uiState.hasEvaluatedMovements,
                    onContinue = { callbacks.onAction(PracticeAction.SetExitDialogVisible(false)) },
                    onSaveAndExit = {
                        callbacks.onAction(PracticeAction.SaveDraftAndExit)
                        callbacks.onNavigateBack()
                    },
                    onDiscard = {
                        callbacks.onAction(PracticeAction.DiscardSession)
                        callbacks.onNavigateBack()
                    },
                )
            }
        }
    }
}

@Composable
private fun CameraActivePractice(
    uiState: PracticeUiState,
    cameraManager: CameraManager,
    onAction: (PracticeAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreviewView(cameraManager = cameraManager, cameraFacing = uiState.cameraFacing)
        SkeletonOverlay(poseFrame = uiState.latestFrame, cameraFacing = uiState.cameraFacing)

        when (val step = uiState.step) {
            is PracticeStep.Framing -> FramingStepOverlay(step, onAction)
            is PracticeStep.Calibrating -> CalibratingStepOverlay(step)
            is PracticeStep.Ready -> ReadyStepOverlay(step.countdownSeconds)
            is PracticeStep.Running -> RunningPracticeHud(step, uiState, onAction)
            is PracticeStep.Paused -> PausedStepDialog(step.reason, onAction, onNavigateBack)
            is PracticeStep.Finished -> Unit
            is PracticeStep.Error -> ErrorStepOverlay(step.messageResId, onAction)
        }
    }
}

@Composable
private fun RunningPracticeHud(
    step: PracticeStep.Running,
    uiState: PracticeUiState,
    onAction: (PracticeAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        PracticeTopBar(
            movementIndex = step.movementIndex,
            timelineStates = uiState.timelineStates,
            onAction = onAction,
        )

        if (uiState.feedbackResId != null) {
            Spacer(modifier = Modifier.height(10.dp))
            GuidanceBanner(
                message = stringResource(id = uiState.feedbackResId),
                severity = uiState.feedbackSeverity,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PracticeMovementHudCard(step = step)

        Spacer(modifier = Modifier.height(12.dp))

        PracticeBottomControls(
            isVoiceEnabled = uiState.isVoiceEnabled,
            onAction = onAction,
        )
    }
}

@Composable
private fun PracticeTopBar(
    movementIndex: Int,
    timelineStates: Map<Int, tkd.apptkd.ui.components.MovementTimelineSegmentState>,
    onAction: (PracticeAction) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OverlayScrim, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val closeCd = stringResource(id = R.string.cd_close)
            val pauseCd = stringResource(id = R.string.cd_pause_practice)
            IconButton(
                onClick = { onAction(PracticeAction.SetExitDialogVisible(true)) },
                modifier = Modifier.semantics { contentDescription = closeCd },
            ) {
                Text(text = "✕", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = stringResource(id = R.string.movement_progress_label, movementIndex, TOTAL_MOVEMENTS),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            IconButton(
                onClick = { onAction(PracticeAction.Pause) },
                modifier = Modifier.semantics { contentDescription = pauseCd },
            ) {
                Text(text = "⏸", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        MovementTimeline(currentIndex = movementIndex, evaluationsByMovement = timelineStates)
    }
}

@Composable
private fun PracticeMovementHudCard(step: PracticeStep.Running) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OverlayScrim),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            TurnIndicator(turnDegrees = step.turnDegrees)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${step.movementNameEs} (${step.movementNameKo})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = step.stanceName,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
            if (step.nextMovementName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.next_movement_prefix, step.nextMovementName),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
            if (step.isKihap) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(id = R.string.MSG_KIHAP_REMINDER),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun PracticeBottomControls(
    isVoiceEnabled: Boolean,
    onAction: (PracticeAction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PrimaryButton(
            text = stringResource(id = R.string.btn_repeat),
            onClick = { onAction(PracticeAction.RepeatCurrent) },
            modifier = Modifier.weight(1f).defaultMinSize(minHeight = PRACTICE_TOUCH_DP.dp),
        )
        SecondaryButton(
            text = stringResource(id = R.string.btn_skip),
            onClick = { onAction(PracticeAction.SkipCurrent) },
            modifier = Modifier.weight(1f).defaultMinSize(minHeight = PRACTICE_TOUCH_DP.dp),
        )
        SecondaryButton(
            text = stringResource(id = if (isVoiceEnabled) R.string.btn_voice_on else R.string.btn_voice_off),
            onClick = { onAction(PracticeAction.ToggleVoice) },
            modifier = Modifier.width(VOICE_BUTTON_WIDTH_DP.dp).defaultMinSize(minHeight = PRACTICE_TOUCH_DP.dp),
        )
    }
}

@Composable
private fun PermissionDialogContent(
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
                PrimaryButton(
                    text = buttonText,
                    onClick = onButtonClick,
                )
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

@Preview(name = "Practice - Running State")
@Composable
internal fun PracticeRunningPreview() {
    TaegeukCoachTheme {
        RunningPracticeHud(
            step = PracticeStep.Running(
                movementIndex = 5,
                movementNameEs = "Bloqueo bajo",
                movementNameKo = "arae makki",
                stanceName = "Posición de frente",
                nextMovementName = "Puñetazo medio",
                turnDegrees = -90,
                isKihap = false,
            ),
            uiState = PracticeUiState(step = PracticeStep.Framing()),
            onAction = {},
        )
    }
}
