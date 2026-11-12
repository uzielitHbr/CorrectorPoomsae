package tkd.apptkd.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.evaluation.FeedbackItem
import mx.taekwondo.poomsae.analysis.evaluation.MovementCategory
import mx.taekwondo.poomsae.analysis.evaluation.MovementEvaluation
import mx.taekwondo.poomsae.analysis.model.MetricResult
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import tkd.apptkd.ui.PracticeSessionHolder
import tkd.apptkd.ui.components.MetricStatus
import javax.inject.Inject

data class UiMetricDetail(
    val id: String,
    val name: String,
    val value: Double?,
    val idealMin: Double,
    val idealMax: Double,
    val status: MetricStatus,
)

data class MovementDetailUiState(
    val index: Int = 1,
    val nameEs: String = "",
    val nameKo: String = "",
    val stanceName: String = "",
    val category: MovementCategory? = null,
    val score: Double? = null,
    val feedbackItems: List<FeedbackItem> = emptyList(),
    val metrics: List<UiMetricDetail> = emptyList(),
    val snapshotFrame: PoseFrame? = null,
)

@HiltViewModel
class MovementDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionHolder: PracticeSessionHolder,
) : ViewModel() {

    private val movementIndex: Int = checkNotNull(savedStateHandle["movementIndex"]).toString().toInt()
    private val _uiState = MutableStateFlow(buildState(movementIndex, sessionHolder))
    val uiState: StateFlow<MovementDetailUiState> = _uiState.asStateFlow()

    private fun buildState(
        idx: Int,
        sessionHolder: PracticeSessionHolder,
    ): MovementDetailUiState {
        val summary = sessionHolder.currentSummary.value
        val movement = summary?.definition?.movements?.find { it.index == idx }
        if (summary == null || movement == null) {
            return MovementDetailUiState(index = idx)
        }
        val evaluation = summary.evaluations.find { it.index == idx }
        val snapshot = summary.snapshots[idx]

        val metricDetails = evaluation?.metrics?.map { (metricId, result) ->
            resolveMetricDetail(metricId.name, result)
        } ?: emptyList()

        return MovementDetailUiState(
            index = idx,
            nameEs = movement.nameEs,
            nameKo = movement.nameKo,
            stanceName = movement.stance.type.name,
            category = evaluation?.category,
            score = evaluation?.score,
            feedbackItems = evaluation?.feedback ?: emptyList(),
            metrics = metricDetails,
            snapshotFrame = snapshot,
        )
    }

    private fun resolveMetricDetail(name: String, result: MetricResult): UiMetricDetail {
        val (value, status) = when (result) {
            is MetricResult.Value -> result.value to MetricStatus.WITHIN_BAND
            is MetricResult.Unavailable -> null to MetricStatus.UNAVAILABLE
        }
        return UiMetricDetail(
            id = name,
            name = name,
            value = value,
            idealMin = 0.0,
            idealMax = 1.0,
            status = status,
        )
    }
}

