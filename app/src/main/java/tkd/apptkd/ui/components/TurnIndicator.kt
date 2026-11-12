package tkd.apptkd.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tkd.apptkd.R

private const val TURN_LEFT_90_DEG = -90
private const val TURN_RIGHT_90_DEG = 90
private const val TURN_HALF_180_DEG = 180
private const val TURN_HALF_MINUS_180_DEG = -180

enum class TurnType {
    LEFT_90,
    RIGHT_90,
    HALF_TURN_180,
    NONE,
}

fun resolveTurnType(turnDegrees: Int): TurnType =
    when (turnDegrees) {
        TURN_LEFT_90_DEG -> TurnType.LEFT_90
        TURN_RIGHT_90_DEG -> TurnType.RIGHT_90
        TURN_HALF_180_DEG, TURN_HALF_MINUS_180_DEG -> TurnType.HALF_TURN_180
        else -> TurnType.NONE
    }

/**
 * Indicador visual y accesible del giro requerido (Sección 14.5 y 14.6).
 *
 * Muestra el glifo de giro correspondiente y la indicación textual completa.
 */
@Composable
fun TurnIndicator(
    turnDegrees: Int,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
) {
    val turnType = resolveTurnType(turnDegrees)
    val (glyph, textRes) = when (turnType) {
        TurnType.LEFT_90 -> "↰" to R.string.turn_left_90
        TurnType.RIGHT_90 -> "↱" to R.string.turn_right_90
        TurnType.HALF_TURN_180 -> "⤾" to R.string.turn_half_180
        TurnType.NONE -> "↑" to R.string.turn_none
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(id = textRes),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = textColor,
        )
    }
}
