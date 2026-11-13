package tkd.apptkd.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val DIALOG_ELEVATION_DP = 6
private const val DIALOG_RADIUS_DP = 16
private const val PADDING_LARGE_DP = 24
private const val PADDING_MEDIUM_DP = 12

/**
 * Diálogo de confirmación configurable para 2 o 3 acciones (Sección 14.6 y 14.8).
 *
 * Utilizado para salida (SC-17), pausa (SC-15) y reanudación (SC-18).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    actions: List<DialogAction>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(DIALOG_RADIUS_DP.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = DIALOG_ELEVATION_DP.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PADDING_LARGE_DP.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(PADDING_MEDIUM_DP.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(PADDING_LARGE_DP.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                ) {
                    for (action in actions) {
                        TextButton(
                            onClick = action.onClick,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = action.title,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (action.isPrimary) FontWeight.Bold else FontWeight.Medium,
                                ),
                                color = when {
                                    action.isDestructive -> MaterialTheme.colorScheme.error
                                    action.isPrimary -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

data class DialogAction(
    val title: String,
    val onClick: () -> Unit,
    val isPrimary: Boolean = false,
    val isDestructive: Boolean = false,
)
