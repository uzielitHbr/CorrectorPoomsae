package tkd.apptkd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mx.taekwondo.poomsae.analysis.evaluation.MovementCategory
import tkd.apptkd.ui.theme.ExtendedColors
import tkd.apptkd.ui.theme.TaegeukTheme

private const val SEGMENT_CORNER_DP = 3
private const val SEGMENT_HEIGHT_CURRENT_DP = 10
private const val SEGMENT_HEIGHT_NORMAL_DP = 6
private const val PENDING_ALPHA = 0.25f
private const val OMITTED_ALPHA = 0.5f

/**
 * Línea de tiempo compuesta por los 18 segmentos de la forma Taegeuk (Sección 14.6 y 14.8).
 *
 * Muestra visualmente el avance exacto, resaltando el movimiento en curso y coloreando
 * los movimientos ya evaluados u omitidos.
 */
@Composable
fun MovementTimeline(
    currentIndex: Int,
    evaluationsByMovement: Map<Int, MovementTimelineSegmentState>,
    modifier: Modifier = Modifier,
    totalMovements: Int = 18,
) {
    val extendedColors = TaegeukTheme.extendedColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (idx in 1..totalMovements) {
            val segmentState = when {
                idx == currentIndex -> MovementTimelineSegmentState.Current
                evaluationsByMovement.containsKey(idx) -> evaluationsByMovement.getValue(idx)
                idx < currentIndex -> MovementTimelineSegmentState.Evaluated(null)
                else -> MovementTimelineSegmentState.Pending
            }

            val (bgColor, borderColor) = resolveSegmentColors(segmentState, extendedColors)
            val heightDp = if (segmentState is MovementTimelineSegmentState.Current) {
                SEGMENT_HEIGHT_CURRENT_DP
            } else {
                SEGMENT_HEIGHT_NORMAL_DP
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(heightDp.dp)
                    .clip(RoundedCornerShape(SEGMENT_CORNER_DP.dp))
                    .background(bgColor)
                    .then(
                        if (borderColor != null) {
                            Modifier.border(1.dp, borderColor, RoundedCornerShape(SEGMENT_CORNER_DP.dp))
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

sealed interface MovementTimelineSegmentState {
    data object Pending : MovementTimelineSegmentState
    data object Current : MovementTimelineSegmentState
    data class Evaluated(val category: MovementCategory?) : MovementTimelineSegmentState
    data object Omitted : MovementTimelineSegmentState
}

private fun resolveSegmentColors(
    state: MovementTimelineSegmentState,
    extendedColors: ExtendedColors,
): Pair<Color, Color?> =
    when (state) {
        is MovementTimelineSegmentState.Current -> Pair(Color.White, Color.Yellow)
        is MovementTimelineSegmentState.Pending -> Pair(Color.White.copy(alpha = PENDING_ALPHA), null)
        is MovementTimelineSegmentState.Omitted ->
            Pair(extendedColors.resultNotEvaluable.copy(alpha = OMITTED_ALPHA), null)
        is MovementTimelineSegmentState.Evaluated -> {
            val col = when (state.category) {
                MovementCategory.CORRECT -> extendedColors.resultCorrect
                MovementCategory.IMPROVABLE -> extendedColors.resultImprovable
                MovementCategory.INCORRECT -> extendedColors.resultIncorrect
                null -> extendedColors.resultNotEvaluable
            }
            Pair(col, null)
        }
    }
