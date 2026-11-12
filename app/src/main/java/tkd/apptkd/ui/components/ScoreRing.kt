package tkd.apptkd.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tkd.apptkd.R
import tkd.apptkd.ui.theme.TaegeukTheme
import kotlin.math.roundToInt

private const val SCORE_MIN = 0.0
private const val SCORE_MAX = 100.0
private const val SCORE_DIVISOR = 100.0
private const val SCORE_THRESHOLD_HIGH = 80.0
private const val SCORE_THRESHOLD_MEDIUM = 60.0
private const val START_ANGLE_DEG = -90f
private const val FULL_SWEEP_DEG = 360f
private const val DEFAULT_DIAMETER_DP = 180
private const val DEFAULT_STROKE_DP = 12

/**
 * Anillo indicador de puntuación global orientativa (Sección 14.6 y 14.8).
 *
 * Muestra el valor numérico en el centro, el arco de progreso proporcional y la leyenda
 * «Puntaje orientativo» obligatoria de no oficialidad (R-14).
 */
@Composable
fun ScoreRing(
    score: Double?,
    modifier: Modifier = Modifier,
    diameter: Dp = DEFAULT_DIAMETER_DP.dp,
    strokeWidth: Dp = DEFAULT_STROKE_DP.dp,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val extendedColors = TaegeukTheme.extendedColors

    val progress = if (score != null) {
        (score.coerceIn(SCORE_MIN, SCORE_MAX) / SCORE_DIVISOR).toFloat()
    } else {
        0f
    }

    val arcColor = when {
        score == null -> extendedColors.resultNotEvaluable
        score >= SCORE_THRESHOLD_HIGH -> extendedColors.resultCorrect
        score >= SCORE_THRESHOLD_MEDIUM -> extendedColors.resultImprovable
        else -> extendedColors.resultIncorrect
    }

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val strokePx = strokeWidth.toPx()
            drawCircle(
                color = trackColor,
                radius = (size.minDimension - strokePx) / 2f,
                style = Stroke(width = strokePx),
            )
            if (score != null) {
                drawArc(
                    color = arcColor,
                    startAngle = START_ANGLE_DEG,
                    sweepAngle = progress * FULL_SWEEP_DEG,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = score?.roundToInt()?.toString() ?: "--",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(id = R.string.score_ring_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
        }
    }
}
