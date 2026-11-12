package tkd.apptkd.ui.screens

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import tkd.apptkd.R
import tkd.apptkd.ui.components.ConfirmDialog
import tkd.apptkd.ui.components.DialogAction
import tkd.apptkd.ui.components.EmptyState
import tkd.apptkd.ui.components.LoadingState
import tkd.apptkd.ui.components.ResultChip
import tkd.apptkd.ui.components.UiResultCategory
import tkd.apptkd.ui.theme.TaegeukCoachTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val CARD_CORNER_RADIUS_DP = 16
private const val BADGE_CORNER_RADIUS_DP = 8
private const val PREVIEW_DATE_MS = 1731427200000L
private const val PREVIEW_SCORE_HIGH = 88.5
private const val PREVIEW_SCORE_MID = 65.0
private const val PREVIEW_AVG_SCORE = 76.7
private const val PREVIEW_EVAL_COUNT_FULL = 18
private const val PREVIEW_EVAL_COUNT_PARTIAL = 10
private const val ONE_DAY_MS = 86_400_000L

/**
 * Pantalla de Historial de sesiones y progreso en el tiempo (Sección 14.8).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSummary: () -> Unit = {},
    onStartPractice: () -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sessionToDeleteId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.screen_history)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text(text = "←", style = MaterialTheme.typography.titleMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.sessions.isEmpty() -> {
                    EmptyState(
                        message = stringResource(id = R.string.empty_no_history),
                        actionLabel = stringResource(id = R.string.btn_start_practice),
                        onActionClick = onStartPractice,
                    )
                }
                else -> {
                    HistoryContent(
                        uiState = uiState,
                        onSessionClick = { id ->
                            viewModel.openSessionSummary(id, onNavigateToSummary)
                        },
                        onExportClick = { id ->
                            scope.launch {
                                val intent = viewModel.getExportIntent(id, isPdf = true)
                                intent?.let {
                                    val chooser = Intent.createChooser(
                                        it,
                                        context.getString(R.string.export_share_chooser_title),
                                    )
                                    context.startActivity(chooser)
                                }
                            }
                        },
                        onDeleteClick = { id -> sessionToDeleteId = id },
                    )
                }
            }
        }
    }

    sessionToDeleteId?.let { id ->
        ConfirmDialog(
            title = stringResource(id = R.string.history_delete_confirm_title),
            message = stringResource(id = R.string.history_delete_confirm_msg),
            actions = listOf(
                DialogAction(
                    title = stringResource(id = R.string.history_delete_session),
                    onClick = {
                        viewModel.deleteSession(id)
                        sessionToDeleteId = null
                    },
                    isPrimary = true,
                    isDestructive = true,
                ),
                DialogAction(
                    title = stringResource(id = R.string.btn_discard),
                    onClick = { sessionToDeleteId = null },
                ),
            ),
            onDismissRequest = { sessionToDeleteId = null },
        )
    }
}

@Composable
internal fun HistoryContent(
    uiState: HistoryUiState,
    onSessionClick: (String) -> Unit,
    onExportClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HistorySummaryHeader(
                totalSessions = uiState.totalSessions,
                averageScore = uiState.averageCompletedScore,
            )
        }

        items(uiState.sessions, key = { it.id }) { item ->
            HistorySessionCard(
                item = item,
                onClick = { onSessionClick(item.id) },
                onExport = { onExportClick(item.id) },
                onDelete = { onDeleteClick(item.id) },
            )
        }
    }
}

@Composable
internal fun HistorySummaryHeader(
    totalSessions: Int,
    averageScore: Double?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.history_total_sessions, totalSessions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            val avgText = if (averageScore != null) {
                stringResource(id = R.string.history_avg_score, averageScore)
            } else {
                stringResource(id = R.string.history_no_completed)
            }
            Text(
                text = avgText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun HistorySessionCard(
    item: HistoryItemUi,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val formattedDate = remember(item.startedAtMs) { dateFormat.format(Date(item.startedAtMs)) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (item.status == "INCOMPLETE") {
                    StatusBadge(
                        text = stringResource(id = R.string.history_session_incomplete_badge),
                        isWarning = true,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    val scoreText = item.globalScore?.let {
                        String.format(Locale.US, "%.1f (orient.)", it)
                    } ?: stringResource(id = R.string.score_ring_no_score)
                    Text(
                        text = scoreText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(
                            id = R.string.summary_counts_format,
                            item.evaluableCount,
                            item.partialCount,
                            item.notEvaluableCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row {
                    IconButton(onClick = onExport) {
                        Text(text = "↗", style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = onDelete) {
                        Text(
                            text = "✕",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun StatusBadge(
    text: String,
    isWarning: Boolean,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isWarning) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val textColor = if (isWarning) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(BADGE_CORNER_RADIUS_DP.dp),
        color = bgColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Preview(name = "History - Light", showBackground = true)
@Composable
internal fun HistoryPreviewLight() {
    TaegeukCoachTheme(darkTheme = false) {
        HistoryContent(
            uiState = HistoryUiState(
                sessions = listOf(
                    HistoryItemUi(
                        id = "1",
                        startedAtMs = PREVIEW_DATE_MS,
                        status = "COMPLETED",
                        mode = "LIVE",
                        globalScore = PREVIEW_SCORE_HIGH,
                        evaluableCount = PREVIEW_EVAL_COUNT_FULL,
                        partialCount = 0,
                        notEvaluableCount = 0,
                    ),
                    HistoryItemUi(
                        id = "2",
                        startedAtMs = PREVIEW_DATE_MS - ONE_DAY_MS,
                        status = "INCOMPLETE",
                        mode = "LIVE",
                        globalScore = PREVIEW_SCORE_MID,
                        evaluableCount = PREVIEW_EVAL_COUNT_PARTIAL,
                        partialCount = 2,
                        notEvaluableCount = 2,
                    ),
                ),
                totalSessions = 2,
                averageCompletedScore = PREVIEW_AVG_SCORE,
            ),
            onSessionClick = {},
            onExportClick = {},
            onDeleteClick = {},
        )
    }
}

@Preview(name = "History - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun HistoryPreviewDark() {
    TaegeukCoachTheme(darkTheme = true) {
        HistoryContent(
            uiState = HistoryUiState(
                sessions = emptyList(),
                totalSessions = 0,
                averageCompletedScore = null,
            ),
            onSessionClick = {},
            onExportClick = {},
            onDeleteClick = {},
        )
    }
}
