package tkd.apptkd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.taekwondo.poomsae.analysis.segmentation.PauseReason
import tkd.apptkd.R
import tkd.apptkd.ui.components.ConfirmDialog
import tkd.apptkd.ui.components.DialogAction
import tkd.apptkd.ui.components.PrimaryButton
import tkd.apptkd.ui.theme.OverlayScrim

private const val SILHOUETTE_WIDTH_RATIO = 0.65f
private const val SILHOUETTE_HEIGHT_DP = 480
private const val SILHOUETTE_BORDER_ALPHA = 0.6f
private const val TOUCH_TARGET_PRACTICE_DP = 56
private const val COUNTDOWN_FONT_SIZE_SP = 72
private const val DIM_BACKGROUND_ALPHA = 0.45f
private const val ERROR_BACKGROUND_ALPHA = 0.7f

@Composable
fun FramingStepOverlay(
    step: PracticeStep.Framing,
    onAction: (PracticeAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(SILHOUETTE_WIDTH_RATIO)
                .height(SILHOUETTE_HEIGHT_DP.dp)
                .border(2.dp, Color.White.copy(alpha = SILHOUETTE_BORDER_ALPHA), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(id = R.string.label_framing_silhouette_guide),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(OverlayScrim, RoundedCornerShape(16.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.step_framing_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = step.messageResId ?: R.string.MSG_FRAME_TOO_CLOSE),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                text = stringResource(id = R.string.btn_continue),
                onClick = { onAction(PracticeAction.Start) },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = TOUCH_TARGET_PRACTICE_DP.dp),
            )
        }
    }
}

@Composable
fun CalibratingStepOverlay(step: PracticeStep.Calibrating) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = DIM_BACKGROUND_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .background(OverlayScrim, RoundedCornerShape(16.dp))
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(id = R.string.step_calibration_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (step.isRetrying) {
                    stringResource(id = R.string.MSG_CAL_RETRY)
                } else {
                    stringResource(id = R.string.label_stay_still)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun ReadyStepOverlay(countdownSeconds: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = DIM_BACKGROUND_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$countdownSeconds",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = COUNTDOWN_FONT_SIZE_SP.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.label_ready_pose_instruction),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun PausedStepDialog(
    reason: PauseReason,
    onAction: (PracticeAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val message = when (reason) {
        PauseReason.USER, PauseReason.APP_BACKGROUND -> stringResource(id = R.string.MSG_PAUSED)
        PauseReason.LOST_TRACKING -> stringResource(id = R.string.MSG_LOST)
        PauseReason.CAMERA_INTERRUPTED -> stringResource(id = R.string.MSG_CAM_ERR)
        PauseReason.PERMISSION_REVOKED -> stringResource(id = R.string.MSG_PERM_SETTINGS)
    }

    ConfirmDialog(
        title = stringResource(id = R.string.dialog_pause_title),
        message = message,
        actions = listOf(
            DialogAction(
                title = stringResource(id = R.string.btn_resume),
                onClick = { onAction(PracticeAction.Resume) },
                isPrimary = true,
            ),
            DialogAction(
                title = stringResource(id = R.string.btn_repeat_movement),
                onClick = { onAction(PracticeAction.RepeatCurrent) },
            ),
            DialogAction(
                title = stringResource(id = R.string.btn_restart_form),
                onClick = { onAction(PracticeAction.RestartForm) },
            ),
            DialogAction(
                title = stringResource(id = R.string.btn_finish),
                onClick = {
                    onAction(PracticeAction.FinishSession)
                    onNavigateBack()
                },
                isDestructive = true,
            ),
        ),
        onDismissRequest = { onAction(PracticeAction.Resume) },
    )
}

@Composable
fun ExitConfirmDialog(
    hasEvaluatedMovements: Boolean,
    onContinue: () -> Unit,
    onSaveAndExit: () -> Unit,
    onDiscard: () -> Unit,
) {
    val actions = if (hasEvaluatedMovements) {
        listOf(
            DialogAction(title = stringResource(id = R.string.btn_continue), onClick = onContinue, isPrimary = true),
            DialogAction(title = stringResource(id = R.string.btn_save_and_exit), onClick = onSaveAndExit),
            DialogAction(title = stringResource(id = R.string.btn_discard), onClick = onDiscard, isDestructive = true),
        )
    } else {
        listOf(
            DialogAction(title = stringResource(id = R.string.btn_continue), onClick = onContinue, isPrimary = true),
            DialogAction(title = stringResource(id = R.string.btn_discard), onClick = onDiscard, isDestructive = true),
        )
    }

    val messageResId = if (hasEvaluatedMovements) {
        R.string.dialog_exit_message_evaluated
    } else {
        R.string.dialog_exit_message_clean
    }

    ConfirmDialog(
        title = stringResource(id = R.string.dialog_exit_title),
        message = stringResource(id = messageResId),
        actions = actions,
        onDismissRequest = onContinue,
    )
}

@Composable
fun ErrorStepOverlay(
    messageResId: Int,
    onAction: (PracticeAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = ERROR_BACKGROUND_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = messageResId),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(
                    text = stringResource(id = R.string.btn_retry),
                    onClick = { onAction(PracticeAction.Start) },
                )
            }
        }
    }
}
