package tkd.apptkd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tkd.apptkd.R
import tkd.apptkd.ui.theme.TaegeukTheme

private const val BAR_HEIGHT_DP = 6
private const val BAR_CORNER_DP = 3
private const val MIN_PROGRESS = 0.05
private const val MAX_PROGRESS = 0.95
private const val SCALE_MIN_FACTOR = 0.7
private const val SCALE_MAX_FACTOR = 1.3

data class MetricDisplayData(
    val name: String,
    val measuredValue: Double?,
    val idealMin: Double,
    val idealMax: Double,
    val status: MetricStatus,
)

enum class MetricStatus {
    WITHIN_BAND,
    NEAR_BAND,
    OUTSIDE_BAND,
    UNAVAILABLE,
    INFORMATIVE,
}

/**
 * Fila de visualización de métrica individual (Sección 14.6 y 14.8).
 *
 * Muestra el nombre de la métrica, el valor numérico, el rango ideal con barra visual
 * y el estado evaluativo (Dentro, Cerca, Fuera, No disponible, Informativa).
 */
@Composable
fun MetricRow(
    data: MetricDisplayData,
    modifier: Modifier = Modifier,
) {
    val extendedColors = TaegeukTheme.extendedColors

    val (statusTextRes, statusColor) = when (data.status) {
        MetricStatus.WITHIN_BAND -> R.string.metric_status_within to extendedColors.resultCorrect
        MetricStatus.NEAR_BAND -> R.string.metric_status_near to extendedColors.resultImprovable
        MetricStatus.OUTSIDE_BAND -> R.string.metric_status_outside to extendedColors.resultIncorrect
        MetricStatus.UNAVAILABLE -> R.string.metric_status_unavailable to extendedColors.resultNotEvaluable
        MetricStatus.INFORMATIVE -> R.string.metric_status_informative to extendedColors.resultNotEvaluable
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = data.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(id = statusTextRes),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = statusColor,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Valores numéricos y rango ideal
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (data.measuredValue != null) {
                    stringResource(id = R.string.metric_value_format, data.measuredValue)
                } else {
                    "--"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(id = R.string.metric_range_format, data.idealMin, data.idealMax),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Barra gráfica indicadora
        MetricVisualBar(
            value = data.measuredValue,
            min = data.idealMin,
            max = data.idealMax,
            statusColor = statusColor,
        )
    }
}

@Composable
private fun MetricVisualBar(
    value: Double?,
    min: Double,
    max: Double,
    statusColor: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT_DP.dp)
            .clip(RoundedCornerShape(BAR_CORNER_DP.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (value != null && max > min) {
            val minBound = min * SCALE_MIN_FACTOR
            val maxBound = max * SCALE_MAX_FACTOR
            val progress = ((value - minBound) / (maxBound - minBound))
                .coerceIn(MIN_PROGRESS, MAX_PROGRESS)
                .toFloat()
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .height(BAR_HEIGHT_DP.dp)
                    .clip(RoundedCornerShape(BAR_CORNER_DP.dp))
                    .background(statusColor),
            )
        }
    }
}
