package tkd.apptkd.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mx.taekwondo.poomsae.analysis.evaluation.FeedbackCode
import mx.taekwondo.poomsae.analysis.evaluation.FeedbackItem
import mx.taekwondo.poomsae.analysis.evaluation.MovementCategory
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.poomsae.MetricId
import tkd.apptkd.R
import tkd.apptkd.camera.CameraFacing
import tkd.apptkd.ui.FeedbackMessageMapper
import tkd.apptkd.ui.components.MetricDisplayData
import tkd.apptkd.ui.components.MetricRow
import tkd.apptkd.ui.components.MetricStatus
import tkd.apptkd.ui.components.ResultChip
import tkd.apptkd.ui.components.SkeletonOverlay
import tkd.apptkd.ui.components.toUiCategory
import tkd.apptkd.ui.theme.TaegeukCoachTheme
import kotlin.math.roundToInt

private const val SNAPSHOT_HEIGHT_DP = 260
private const val CARD_CORNER_DP = 16
private const val PREVIEW_INDEX_5 = 5
private const val PREVIEW_SCORE_74 = 74.0
private const val PREVIEW_KNEE_VAL = 125.0
private const val PREVIEW_KNEE_MIN = 130.0
private const val PREVIEW_KNEE_MAX = 140.0
private const val PREVIEW_TORSO_VAL = 28.0
private const val PREVIEW_TORSO_MAX = 15.0
private const val PREVIEW_STEP_VAL = 1.45
private const val PREVIEW_STEP_MIN = 1.3
private const val PREVIEW_STEP_MAX = 1.7
private const val PREVIEW_SCORE_98 = 98.0

/**
 * Pantalla de Detalle de Movimiento individual (Sección 14.8).
 *
 * Muestra el momento clave con esqueleto anatómico, la técnica, postura, desglose
 * de correcciones técnicas accionables y las métricas con rangos ideales.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementDetailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MovementDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    MovementDetailContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementDetailContent(
    uiState: MovementDetailUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.detail_title, uiState.index),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { MovementSnapshotSection(uiState.snapshotFrame) }
            item { MovementHeaderSection(uiState.nameEs, uiState.nameKo, uiState.stanceName) }
            item { MovementScoreSection(uiState.category, uiState.score) }
            item { MovementCorrectionsSection(uiState.feedbackItems) }
            item {
                Text(
                    text = stringResource(id = R.string.detail_metrics_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            items(uiState.metrics, key = { it.id }) { metric ->
                MetricRow(
                    data = MetricDisplayData(
                        name = metric.name,
                        measuredValue = metric.value,
                        idealMin = metric.idealMin,
                        idealMax = metric.idealMax,
                        status = metric.status,
                    ),
                )
            }
        }
    }
}

@Composable
private fun MovementSnapshotSection(snapshotFrame: PoseFrame?) {
    Card(
        shape = RoundedCornerShape(CARD_CORNER_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .height(SNAPSHOT_HEIGHT_DP.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(CARD_CORNER_DP.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (snapshotFrame != null) {
                SkeletonOverlay(
                    poseFrame = snapshotFrame,
                    cameraFacing = CameraFacing.FRONT,
                )
            } else {
                Text(
                    text = stringResource(id = R.string.detail_snapshot_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun MovementHeaderSection(nameEs: String, nameKo: String, stanceName: String) {
    Column {
        Text(
            text = "$nameEs ($nameKo)",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stanceName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MovementScoreSection(category: MovementCategory?, score: Double?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ResultChip(category = category.toUiCategory())
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(
                id = R.string.detail_score_format,
                score?.roundToInt() ?: 0,
            ),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun MovementCorrectionsSection(feedbackItems: List<FeedbackItem>) {
    Card(
        shape = RoundedCornerShape(CARD_CORNER_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.detail_corrections_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (feedbackItems.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.detail_no_corrections),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                feedbackItems.forEachIndexed { i, item ->
                    val messageResId = FeedbackMessageMapper.getStringResId(item.code)
                    Text(
                        text = "${i + 1}. ${stringResource(id = messageResId)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Preview(name = "Detail - Light", showBackground = true)
@Composable
internal fun MovementDetailPreviewLight() {
    TaegeukCoachTheme(darkTheme = false) {
        MovementDetailContent(
            uiState = MovementDetailUiState(
                index = PREVIEW_INDEX_5,
                nameEs = "Bloqueo bajo",
                nameKo = "arae makki",
                stanceName = "Posición de frente (ap kubi)",
                category = MovementCategory.IMPROVABLE,
                score = PREVIEW_SCORE_74,
                feedbackItems = listOf(
                    FeedbackItem(
                        code = FeedbackCode.FRONT_KNEE_INSUFFICIENT_BEND,
                        metricId = MetricId.ST_03,
                        measuredValue = PREVIEW_KNEE_VAL,
                        targetMin = PREVIEW_KNEE_MIN,
                        targetMax = PREVIEW_KNEE_MAX,
                    ),
                    FeedbackItem(
                        code = FeedbackCode.TORSO_NOT_UPRIGHT,
                        metricId = MetricId.ST_05,
                        measuredValue = PREVIEW_TORSO_VAL,
                        targetMin = 0.0,
                        targetMax = PREVIEW_TORSO_MAX,
                    ),
                ),
                metrics = listOf(
                    UiMetricDetail(
                        id = "ST-01",
                        name = "Largo del paso",
                        value = PREVIEW_STEP_VAL,
                        idealMin = PREVIEW_STEP_MIN,
                        idealMax = PREVIEW_STEP_MAX,
                        status = MetricStatus.WITHIN_BAND,
                    ),
                    UiMetricDetail(
                        id = "ST-03",
                        name = "Rodilla delantera",
                        value = PREVIEW_KNEE_VAL,
                        idealMin = PREVIEW_KNEE_MIN,
                        idealMax = PREVIEW_KNEE_MAX,
                        status = MetricStatus.NEAR_BAND,
                    ),
                ),
            ),
            onNavigateBack = {},
        )
    }
}

@Preview(name = "Detail - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun MovementDetailPreviewDark() {
    TaegeukCoachTheme(darkTheme = true) {
        MovementDetailContent(
            uiState = MovementDetailUiState(
                index = 1,
                nameEs = "Bloqueo bajo",
                nameKo = "arae makki",
                stanceName = "Posición de caminar (ap seogi)",
                category = MovementCategory.CORRECT,
                score = PREVIEW_SCORE_98,
            ),
            onNavigateBack = {},
        )
    }
}
