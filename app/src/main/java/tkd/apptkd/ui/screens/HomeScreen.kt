package tkd.apptkd.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tkd.apptkd.R
import tkd.apptkd.ui.components.ConfirmDialog
import tkd.apptkd.ui.components.DialogAction
import tkd.apptkd.ui.components.PrimaryButton
import tkd.apptkd.ui.components.SecondaryButton
import tkd.apptkd.ui.components.StatusBanner
import tkd.apptkd.ui.components.VideoProcessingDialog
import tkd.apptkd.ui.components.VideoProgressState
import tkd.apptkd.ui.theme.TaegeukCoachTheme
import tkd.apptkd.video.VideoAnalysisViewModel
import kotlin.math.roundToInt

private const val TOTAL_MOVEMENTS = 18
private const val PREVIEW_HOME_SCORE = 82.0
private const val PREVIEW_DRAFT_INDEX = 5

data class HomeNavigationActions(
    val onNavigateToPractice: () -> Unit,
    val onNavigateToHistory: () -> Unit,
    val onNavigateToSettings: () -> Unit,
    val onNavigateToSummary: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    actions: HomeNavigationActions,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    videoViewModel: VideoAnalysisViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val videoUiState by videoViewModel.uiState.collectAsState()

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            videoViewModel.startAnalysis(uri)
        }
    }

    LaunchedEffect(videoUiState.isCompleted) {
        if (videoUiState.isCompleted) {
            videoViewModel.resetCompleted()
            actions.onNavigateToSummary()
        }
    }

    HomeContent(
        lastScore = uiState.lastScore,
        draftMovementIndex = uiState.draftMovementIndex,
        callbacks = HomeCallbacks(
            onStartPractice = actions.onNavigateToPractice,
            onNavigateToHistory = actions.onNavigateToHistory,
            onNavigateToSettings = actions.onNavigateToSettings,
            onDiscardDraft = { viewModel.onDiscardDraft() },
            onAnalyzeVideo = {
                videoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                )
            },
        ),
        modifier = modifier,
    )

    if (videoUiState.isProcessing) {
        VideoProcessingDialog(
            state = VideoProgressState(
                progress = videoUiState.progress,
                currentSeconds = videoUiState.currentSeconds,
                totalSeconds = videoUiState.totalSeconds,
                movementsDetected = videoUiState.movementsDetected,
                isCancelling = videoUiState.isCancelling,
            ),
            onCancel = { videoViewModel.cancelAnalysis() },
        )
    }

    videoUiState.errorMessageResId?.let { resId ->
        ConfirmDialog(
            title = stringResource(id = R.string.video_analysis_title),
            message = stringResource(id = resId),
            actions = listOf(
                DialogAction(
                    title = stringResource(id = R.string.btn_accept),
                    onClick = { videoViewModel.dismissError() },
                    isPrimary = true,
                ),
            ),
            onDismissRequest = { videoViewModel.dismissError() },
        )
    }

    videoUiState.infoMessageResId?.let { resId ->
        ConfirmDialog(
            title = stringResource(id = R.string.video_analysis_title),
            message = stringResource(id = resId),
            actions = listOf(
                DialogAction(
                    title = stringResource(id = R.string.btn_accept),
                    onClick = { videoViewModel.dismissInfo() },
                    isPrimary = true,
                ),
            ),
            onDismissRequest = { videoViewModel.dismissInfo() },
        )
    }
}

data class HomeCallbacks(
    val onStartPractice: () -> Unit,
    val onNavigateToHistory: () -> Unit,
    val onNavigateToSettings: () -> Unit,
    val onDiscardDraft: () -> Unit,
    val onAnalyzeVideo: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    lastScore: Double?,
    draftMovementIndex: Int?,
    callbacks: HomeCallbacks,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                actions = {
                    IconButton(onClick = callbacks.onNavigateToSettings) {
                        Text(text = "⚙", style = MaterialTheme.typography.titleMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatusBanner(text = stringResource(id = R.string.status_documentally_fixed))

            if (draftMovementIndex != null) {
                DraftNoticeCard(
                    draftIndex = draftMovementIndex,
                    onResume = callbacks.onStartPractice,
                    onDiscard = callbacks.onDiscardDraft,
                )
            }

            TaegeukCardSection(lastScore = lastScore, onStartPractice = callbacks.onStartPractice)

            SecondaryButton(
                text = stringResource(id = R.string.btn_analyze_video),
                onClick = callbacks.onAnalyzeVideo,
                modifier = Modifier.fillMaxWidth(),
            )

            SecondaryButton(
                text = stringResource(id = R.string.btn_history),
                onClick = callbacks.onNavigateToHistory,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(id = R.string.MSG_DISCLAIMER),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TaegeukCardSection(
    lastScore: Double?,
    onStartPractice: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.form_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.form_movements_count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(
                    id = R.string.last_practice_format,
                    lastScore?.roundToInt()?.toString() ?: "--",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                text = stringResource(id = R.string.btn_start_practice),
                onClick = onStartPractice,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DraftNoticeCard(
    draftIndex: Int,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.draft_banner_title),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.draft_movement_format, draftIndex, TOTAL_MOVEMENTS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDiscard) {
                    Text(
                        text = stringResource(id = R.string.btn_discard),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                PrimaryButton(
                    text = stringResource(id = R.string.btn_resume),
                    onClick = onResume,
                )
            }
        }
    }
}

@Preview(name = "Home - Light", showBackground = true)
@Composable
internal fun HomeScreenPreviewLight() {
    TaegeukCoachTheme(darkTheme = false) {
        HomeContent(
            lastScore = PREVIEW_HOME_SCORE,
            draftMovementIndex = PREVIEW_DRAFT_INDEX,
            callbacks = HomeCallbacks({}, {}, {}, {}),
        )
    }
}

@Preview(name = "Home - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun HomeScreenPreviewDark() {
    TaegeukCoachTheme(darkTheme = true) {
        HomeContent(
            lastScore = null,
            draftMovementIndex = null,
            callbacks = HomeCallbacks({}, {}, {}, {}),
        )
    }
}
