package tkd.apptkd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tkd.apptkd.ui.theme.TaegeukTheme

private const val CONTAINER_ALPHA = 0.25f

/**
 * Banner flotante para mensajes de encuadre, correcciones y avisos breves (Sección 14.6).
 */
@Composable
fun GuidanceBanner(
    message: String,
    modifier: Modifier = Modifier,
    severity: BannerSeverity = BannerSeverity.INFO,
) {
    val extendedColors = TaegeukTheme.extendedColors
    val (containerColor, contentColor, iconSymbol) = when (severity) {
        BannerSeverity.INFO -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "ℹ",
        )
        BannerSeverity.WARNING -> Triple(
            extendedColors.resultImprovable.copy(alpha = CONTAINER_ALPHA),
            extendedColors.resultImprovable,
            "⚠",
        )
        BannerSeverity.ERROR -> Triple(
            extendedColors.resultIncorrect.copy(alpha = CONTAINER_ALPHA),
            extendedColors.resultIncorrect,
            "✕",
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = iconSymbol,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = contentColor,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = contentColor,
            modifier = Modifier.weight(1f),
        )
    }
}

enum class BannerSeverity {
    INFO,
    WARNING,
    ERROR,
}
