package tkd.apptkd.video

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.taekwondo.poomsae.analysis.poomsae.PoomsaeDefinition
import tkd.apptkd.R
import tkd.apptkd.data.repository.SessionRepository
import tkd.apptkd.ui.FeedbackMessageMapper
import tkd.apptkd.ui.PracticeSessionHolder
import javax.inject.Inject

data class VideoAnalysisUiState(
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val currentSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val movementsDetected: Int = 0,
    val isCancelling: Boolean = false,
    val errorMessageResId: Int? = null,
    val isCompleted: Boolean = false,
    val infoMessageResId: Int? = null,
)

@HiltViewModel
class VideoAnalysisViewModel @Inject constructor(
    private val definition: PoomsaeDefinition,
    private val sessionHolder: PracticeSessionHolder,
    private val sessionRepository: SessionRepository,
    private val extractorFactory: VideoFrameExtractorFactory,
    private val detectorFactory: VideoPoseDetectorFactory,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoAnalysisUiState())
    val uiState: StateFlow<VideoAnalysisUiState> = _uiState.asStateFlow()

    var analysisDispatcher: CoroutineDispatcher = Dispatchers.Default
    private var analysisJob: Job? = null

    fun startAnalysis(uri: Uri) {
        if (_uiState.value.isProcessing) return
        _uiState.update {
            VideoAnalysisUiState(
                isProcessing = true,
                progress = 0f,
                currentSeconds = 0,
                totalSeconds = 0,
                movementsDetected = 0,
            )
        }

        analysisJob = viewModelScope.launch(analysisDispatcher) {
            val processor = OfflineVideoProcessor(definition)
            val extractor = extractorFactory.create(uri)
            val detector = detectorFactory.create()

            val outcome = processor.process(
                extractor = extractor,
                detector = detector,
                onProgress = { progress, curSec, totSec, moves ->
                    _uiState.update {
                        it.copy(
                            progress = progress,
                            currentSeconds = curSec,
                            totalSeconds = totSec,
                            movementsDetected = moves,
                        )
                    }
                },
            )

            handleOutcome(outcome)
        }

        analysisJob?.invokeOnCompletion { throwable ->
            if (throwable is kotlinx.coroutines.CancellationException) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        isCancelling = false,
                        infoMessageResId = R.string.MSG_VIDEO_CANCEL,
                    )
                }
            }
        }
    }

    private suspend fun handleOutcome(outcome: VideoAnalysisOutcome) {
        when (outcome) {
            is VideoAnalysisOutcome.Success -> {
                sessionHolder.setSummary(outcome.summary)
                sessionRepository.saveCompletedSession(
                    summaryData = outcome.summary,
                    cameraFacing = "VIDEO",
                    mode = "VIDEO",
                )
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        isCompleted = true,
                    )
                }
            }
            is VideoAnalysisOutcome.Rejected -> {
                val resId = FeedbackMessageMapper.mapVideoRejection(outcome.reason)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessageResId = resId,
                    )
                }
            }
            is VideoAnalysisOutcome.Cancelled -> {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        isCancelling = false,
                        infoMessageResId = R.string.MSG_VIDEO_CANCEL,
                    )
                }
            }
            is VideoAnalysisOutcome.Error -> {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessageResId = R.string.MSG_VIDEO_ERR,
                    )
                }
            }
        }
    }

    fun cancelAnalysis() {
        if (!_uiState.value.isProcessing) return
        _uiState.update { it.copy(isCancelling = true) }
        analysisJob?.cancel()
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessageResId = null) }
    }

    fun dismissInfo() {
        _uiState.update { it.copy(infoMessageResId = null) }
    }

    fun resetCompleted() {
        _uiState.update { it.copy(isCompleted = false) }
    }
}
