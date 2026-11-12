package tkd.apptkd.ui.screens

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mx.taekwondo.poomsae.analysis.evaluation.MovementCategory
import tkd.apptkd.R
import tkd.apptkd.ui.components.MovementCard
import tkd.apptkd.ui.components.MovementCardItem
import tkd.apptkd.ui.components.PrimaryButton
import tkd.apptkd.ui.components.ScoreRing
import tkd.apptkd.ui.components.SecondaryButton
import tkd.apptkd.ui.components.StatusBanner
import tkd.apptkd.ui.theme.TaegeukCoachTheme

private const val PREVIEW_SUMMARY_SCORE = 82.0
private const val PREVIEW_EVALUATED_COUNT = 14
private const val PREVIEW_PARTIAL_COUNT = 2
private const val PREVIEW_NOT_EVALUABLE_COUNT = 2
private const val PREVIEW_CARD_INDEX_1 = 1
private const val PREVIEW_CARD_INDEX_2 = 2
private const val PREVIEW_CARD_INDEX_3 = 3
private const val PREVIEW_SCORE_1 = 95.0
private const val PREVIEW_SCORE_2 = 70.0
private const val PREVIEW_SCORE_3 = 90.0
private const val TOTAL_FORM_MOVEMENTS = 18

/**
 * Pantalla de Resumen de la sesión completada (Sección 14.8).
 *
 * Muestra el ScoreRing global orientativo, el desglose de los 18 movimientos con sus
 * respectivas tarjetas clicables para inspección técnica detallada, y los avisos normativos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToDetail: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Regla de navegación: al presionar atrás desde Resumen se vuelve a Inicio (Sección 14.7)
    BackHandler {
        onNavigateToHome()
    }

    SummaryContent(
        uiState = uiState,
        onNavigateToHome = onNavigateToHome,
        onNavigateToDetail = onNavigateToDetail,
        onShareClick = {
            val intent = viewModel.getShareIntent(isPdf = true)
            intent?.let {
                val chooser = Intent.createChooser(
                    it,
                    context.getString(R.string.export_share_chooser_title),
                )
                context.startActivity(chooser)
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryContent(
    uiState: SummaryUiState,
    onNavigateToHome: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onShareClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.screen_summary)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToHome) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    ScoreRing(score = uiState.globalScore)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(
                            id = R.string.summary_counts_format,
                            uiState.evaluableCount,
                            uiState.partialCount,
                            uiState.notEvaluableCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.isExperimental) {
                        StatusBanner(text = stringResource(id = R.string.MSG_EXPERIMENTAL))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(uiState.movements, key = { it.index }) { mov ->
                    MovementCard(
                        item = MovementCardItem(
                            index = mov.index,
                            nameEs = mov.nameEs,
                            nameKo = mov.nameKo,
                            stanceName = mov.stanceName,
                            category = mov.category,
                        ),
                        onClick = { onNavigateToDetail(mov.index) },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.MSG_DISCLAIMER),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Barra inferior fija con acciones principales
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SecondaryButton(
                    text = stringResource(id = R.string.btn_share),
                    onClick = onShareClick,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = stringResource(id = R.string.btn_practice_again),
                    onClick = onNavigateToHome,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview(name = "Summary - Light", showBackground = true)
@Composable
internal fun SummaryPreviewLight() {
    TaegeukCoachTheme(darkTheme = false) {
        SummaryContent(
            uiState = SummaryUiState(
                globalScore = PREVIEW_SUMMARY_SCORE,
                evaluableCount = PREVIEW_EVALUATED_COUNT,
                partialCount = PREVIEW_PARTIAL_COUNT,
                notEvaluableCount = PREVIEW_NOT_EVALUABLE_COUNT,
                movements = listOf(
                    MovementCardData(
                        PREVIEW_CARD_INDEX_1,
                        "Bloqueo bajo",
                        "arae makki",
                        "AS",
                        MovementCategory.CORRECT,
                        PREVIEW_SCORE_1,
                    ),
                    MovementCardData(
                        PREVIEW_CARD_INDEX_2,
                        "Puñetazo medio",
                        "momtong jireugi",
                        "AS",
                        MovementCategory.IMPROVABLE,
                        PREVIEW_SCORE_2,
                    ),
                    MovementCardData(
                        PREVIEW_CARD_INDEX_3,
                        "Bloqueo bajo",
                        "arae makki",
                        "AS",
                        MovementCategory.CORRECT,
                        PREVIEW_SCORE_3,
                    ),
                ),
            ),
            onNavigateToHome = {},
            onNavigateToDetail = {},
        )
    }
}

@Preview(name = "Summary - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun SummaryPreviewDark() {
    TaegeukCoachTheme(darkTheme = true) {
        SummaryContent(
            uiState = SummaryUiState(
                globalScore = null,
                evaluableCount = 0,
                partialCount = 0,
                notEvaluableCount = TOTAL_FORM_MOVEMENTS,
                movements = emptyList(),
            ),
            onNavigateToHome = {},
            onNavigateToDetail = {},
        )
    }
}
