package tkd.apptkd.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.taekwondo.poomsae.analysis.evaluation.MovementCategory

private const val CARD_CORNER_RADIUS_DP = 16
private const val CARD_PADDING_DP = 14
private const val INDEX_WIDTH_DP = 32
private const val OUTLINE_ALPHA = 0.2f
private const val ELEVATION_SELECTED_DP = 4
private const val ELEVATION_NORMAL_DP = 1

data class MovementCardItem(
    val index: Int,
    val nameEs: String,
    val nameKo: String,
    val stanceName: String,
    val category: MovementCategory?,
    val isSelected: Boolean = false,
)

/**
 * Tarjeta individual de movimiento para la lista de resumen (Sección 14.6 y 14.8).
 *
 * Muestra el número ordinal, la técnica en español y coreano (Hangul), la postura
 * y el chip con la categoría evaluada.
 */
@Composable
fun MovementCard(
    item: MovementCardItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (item.isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = OUTLINE_ALPHA)
    }

    Card(
        shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (item.isSelected) ELEVATION_SELECTED_DP.dp else ELEVATION_NORMAL_DP.dp,
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                if (item.isSelected) 2.dp else 1.dp,
                borderColor,
                RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp),
            )
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CARD_PADDING_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${item.index}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(INDEX_WIDTH_DP.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.nameEs,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.nameKo} · ${item.stanceName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            ResultChip(category = item.category.toUiCategory())
        }
    }
}
