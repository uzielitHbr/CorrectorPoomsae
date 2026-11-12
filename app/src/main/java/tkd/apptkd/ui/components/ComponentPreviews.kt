package tkd.apptkd.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.taekwondo.poomsae.analysis.evaluation.MovementCategory
import tkd.apptkd.ui.theme.TaegeukCoachTheme

private const val PREVIEW_SCORE = 88.0
private const val PREVIEW_INDEX_1 = 1
private const val PREVIEW_INDEX_2 = 2
private const val PREVIEW_INDEX_3 = 3
private const val PREVIEW_INDEX_4 = 4
private const val PREVIEW_CURRENT_INDEX = 5
private const val PREVIEW_TURN_LEFT = -90
private const val PREVIEW_TURN_RIGHT = 90
private const val PREVIEW_TURN_HALF = 180
private const val PREVIEW_METRIC_VAL = 1.4
private const val PREVIEW_METRIC_MIN = 1.2
private const val PREVIEW_METRIC_MAX = 1.6

@Preview(name = "Components - Buttons & Chips (Light)", showBackground = true)
@Composable
internal fun ButtonsAndChipsPreviewLight() {
    TaegeukCoachTheme(darkTheme = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PrimaryButton(text = "Botón Primario", onClick = {})
            PrimaryButton(text = "Cargando...", onClick = {}, isLoading = true)
            PrimaryButton(text = "Deshabilitado", onClick = {}, enabled = false)
            SecondaryButton(text = "Botón Secundario", onClick = {})
            ResultChip(category = UiResultCategory.CORRECT)
            ResultChip(category = UiResultCategory.IMPROVABLE)
            ResultChip(category = UiResultCategory.INCORRECT)
            ResultChip(category = UiResultCategory.NOT_EVALUABLE)
        }
    }
}

@Preview(name = "Components - Score & Timeline (Light)", showBackground = true)
@Composable
internal fun ScoreAndTimelinePreviewLight() {
    TaegeukCoachTheme(darkTheme = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScoreRing(score = PREVIEW_SCORE)
            ScoreRing(score = null)
            MovementTimeline(
                currentIndex = PREVIEW_CURRENT_INDEX,
                evaluationsByMovement = mapOf(
                    PREVIEW_INDEX_1 to MovementTimelineSegmentState.Evaluated(MovementCategory.CORRECT),
                    PREVIEW_INDEX_2 to MovementTimelineSegmentState.Evaluated(MovementCategory.IMPROVABLE),
                    PREVIEW_INDEX_3 to MovementTimelineSegmentState.Evaluated(MovementCategory.INCORRECT),
                    PREVIEW_INDEX_4 to MovementTimelineSegmentState.Omitted,
                ),
            )
            TurnIndicator(turnDegrees = PREVIEW_TURN_LEFT, textColor = MaterialTheme.colorScheme.onSurface)
            TurnIndicator(turnDegrees = PREVIEW_TURN_RIGHT, textColor = MaterialTheme.colorScheme.onSurface)
            TurnIndicator(turnDegrees = PREVIEW_TURN_HALF, textColor = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Preview(name = "Components - Status Views (Light)", showBackground = true)
@Composable
internal fun StatusViewsPreviewLight() {
    TaegeukCoachTheme(darkTheme = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GuidanceBanner(message = "Información de encuadre", severity = BannerSeverity.INFO)
            GuidanceBanner(message = "Advertencia técnica", severity = BannerSeverity.WARNING)
            GuidanceBanner(message = "Error de postura", severity = BannerSeverity.ERROR)
            StatusBanner(text = "Secuencia fijada documentalmente")
            MetricRow(
                data = MetricDisplayData(
                    name = "Largo de paso",
                    measuredValue = PREVIEW_METRIC_VAL,
                    idealMin = PREVIEW_METRIC_MIN,
                    idealMax = PREVIEW_METRIC_MAX,
                    status = MetricStatus.WITHIN_BAND,
                ),
            )
        }
    }
}

@Preview(name = "Components - Dark Theme", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun ComponentsDarkPreview() {
    TaegeukCoachTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PrimaryButton(text = "Botón Primario", onClick = {})
            SecondaryButton(text = "Botón Secundario", onClick = {})
            ResultChip(category = UiResultCategory.CORRECT)
            ResultChip(category = UiResultCategory.IMPROVABLE)
            StatusBanner(text = "Aviso normativo en modo oscuro")
        }
    }
}

@Preview(name = "State - Loading State", showBackground = true)
@Composable
internal fun LoadingStatePreview() {
    TaegeukCoachTheme {
        LoadingState()
    }
}

@Preview(name = "State - Empty State", showBackground = true)
@Composable
internal fun EmptyStatePreview() {
    TaegeukCoachTheme {
        EmptyState(
            message = "No hay registros disponibles",
            actionLabel = "Comenzar práctica",
            onActionClick = {},
        )
    }
}

@Preview(name = "State - Error State", showBackground = true)
@Composable
internal fun ErrorStatePreview() {
    TaegeukCoachTheme {
        ErrorState(
            message = "Ocurrió un error al cargar el modelo",
            actionLabel = "Reintentar",
            onActionClick = {},
        )
    }
}
