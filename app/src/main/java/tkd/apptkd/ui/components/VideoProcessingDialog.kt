package tkd.apptkd.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import tkd.apptkd.R
import tkd.apptkd.ui.theme.TaegeukCoachTheme
import kotlin.math.roundToInt

private const val PERCENT_SCALE = 100f
private const val CARD_CORNER_RADIUS_DP = 16

data class VideoProgressState(
    val progress: Float,
    val currentSeconds: Int,
    val totalSeconds: Int,
    val movementsDetected: Int,
    val isCancelling: Boolean,
)

/**
 * Diálogo modal de progreso con barra indicadora y botón de cancelación (SC-29).
 */
@Composable
fun VideoProcessingDialog(
    state: VideoProgressState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Card(
            shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.video_analysis_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                LinearProgressIndicator(
                    progress = { state.progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                )

                val percent = (state.progress * PERCENT_SCALE).roundToInt()
                Text(
                    text = stringResource(
                        id = R.string.video_analysis_progress_format,
                        percent,
                        state.currentSeconds,
                        state.totalSeconds,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = stringResource(
                        id = R.string.video_analysis_movements_count,
                        state.movementsDetected,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                if (state.isCancelling) {
                    Text(
                        text = stringResource(id = R.string.video_analysis_cancelling),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onCancel,
                        enabled = !state.isCancelling,
                    ) {
                        Text(
                            text = stringResource(id = R.string.btn_cancel),
                            color = if (state.isCancelling) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                }
            }
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Video Processing Dialog - In Progress")
@Composable
private fun VideoProcessingDialogProgressPreview() {
    TaegeukCoachTheme {
        VideoProcessingDialog(
            state = VideoProgressState(
                progress = 0.45f,
                currentSeconds = 27,
                totalSeconds = 60,
                movementsDetected = 4,
                isCancelling = false,
            ),
            onCancel = {},
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Video Processing Dialog - Cancelling")
@Composable
private fun VideoProcessingDialogCancellingPreview() {
    TaegeukCoachTheme {
        VideoProcessingDialog(
            state = VideoProgressState(
                progress = 0.80f,
                currentSeconds = 48,
                totalSeconds = 60,
                movementsDetected = 12,
                isCancelling = true,
            ),
            onCancel = {},
        )
    }
}
