package tkd.apptkd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.taekwondo.poomsae.analysis.evaluation.MovementCategory
import tkd.apptkd.R
import tkd.apptkd.ui.theme.ExtendedColors
import tkd.apptkd.ui.theme.TaegeukTheme

private const val CHIP_BG_ALPHA = 0.15f
private const val CHIP_RADIUS_DP = 12

/**
 * Chip normativo de resultado por movimiento (Sección 14.5 y 14.6).
 *
 * Muestra icono textual/vectorial accesible y texto traducido, sin depender únicamente del color.
 */
@Composable
fun ResultChip(
    category: UiResultCategory,
    modifier: Modifier = Modifier,
) {
    val extendedColors = TaegeukTheme.extendedColors
    val style = resolveChipStyle(category, extendedColors)

    Row(
        modifier = modifier
            .background(style.bgColor, shape = RoundedCornerShape(CHIP_RADIUS_DP.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = style.iconText,
            color = style.textColor,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(id = style.textRes),
            color = style.textColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private data class ChipStyle(
    val bgColor: Color,
    val textColor: Color,
    val iconText: String,
    val textRes: Int,
)

private fun resolveChipStyle(
    category: UiResultCategory,
    extendedColors: ExtendedColors,
): ChipStyle =
    when (category) {
        UiResultCategory.CORRECT -> ChipStyle(
            bgColor = extendedColors.resultCorrect.copy(alpha = CHIP_BG_ALPHA),
            textColor = extendedColors.resultCorrect,
            iconText = "✓",
            textRes = R.string.category_correct,
        )
        UiResultCategory.IMPROVABLE -> ChipStyle(
            bgColor = extendedColors.resultImprovable.copy(alpha = CHIP_BG_ALPHA),
            textColor = extendedColors.resultImprovable,
            iconText = "▲",
            textRes = R.string.category_improvable,
        )
        UiResultCategory.INCORRECT -> ChipStyle(
            bgColor = extendedColors.resultIncorrect.copy(alpha = CHIP_BG_ALPHA),
            textColor = extendedColors.resultIncorrect,
            iconText = "✕",
            textRes = R.string.category_incorrect,
        )
        UiResultCategory.NOT_EVALUABLE -> ChipStyle(
            bgColor = extendedColors.resultNotEvaluable.copy(alpha = CHIP_BG_ALPHA),
            textColor = extendedColors.resultNotEvaluable,
            iconText = "?",
            textRes = R.string.category_not_evaluable,
        )
    }

enum class UiResultCategory {
    CORRECT,
    IMPROVABLE,
    INCORRECT,
    NOT_EVALUABLE,
}

fun MovementCategory?.toUiCategory(): UiResultCategory =
    when (this) {
        MovementCategory.CORRECT -> UiResultCategory.CORRECT
        MovementCategory.IMPROVABLE -> UiResultCategory.IMPROVABLE
        MovementCategory.INCORRECT -> UiResultCategory.INCORRECT
        null -> UiResultCategory.NOT_EVALUABLE
    }
