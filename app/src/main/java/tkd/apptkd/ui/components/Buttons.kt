package tkd.apptkd.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val PILL_RADIUS_DP = 24
private const val INDICATOR_SIZE_DP = 20
private const val INDICATOR_STROKE_DP = 2
private const val DEFAULT_TOUCH_HEIGHT_DP = 48

/**
 * Botón primario según el diseño cerrado de la Sección 14.4 y 14.6.
 *
 * Forma de píldora (radio 24dp), altura táctil mínima (48dp por defecto, extensible con Modifier).
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = DEFAULT_TOUCH_HEIGHT_DP.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(PILL_RADIUS_DP.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(INDICATOR_SIZE_DP.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = INDICATOR_STROKE_DP.dp,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * Botón secundario con borde y estilo de píldora (Sección 14.6).
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = DEFAULT_TOUCH_HEIGHT_DP.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(PILL_RADIUS_DP.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(INDICATOR_SIZE_DP.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = INDICATOR_STROKE_DP.dp,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
