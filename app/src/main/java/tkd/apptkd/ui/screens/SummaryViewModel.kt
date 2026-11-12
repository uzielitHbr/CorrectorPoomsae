package tkd.apptkd.ui.screens

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.taekwondo.poomsae.analysis.evaluation.MovementCategory
import tkd.apptkd.data.room.SessionEntityMapper
import tkd.apptkd.data.room.SessionWithMovements
import tkd.apptkd.export.SessionExporter
import tkd.apptkd.ui.PracticeSessionHolder
import javax.inject.Inject

data class MovementCardData(
    val index: Int,
    val nameEs: String,
    val nameKo: String,
    val stanceName: String,
    val category: MovementCategory?,
    val score: Double?,
)

data class SummaryUiState(
    val globalScore: Double? = null,
    val evaluableCount: Int = 0,
    val partialCount: Int = 0,
    val notEvaluableCount: Int = 0,
    val movements: List<MovementCardData> = emptyList(),
    val isExperimental: Boolean = true,
)

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val sessionHolder: PracticeSessionHolder,
    private val sessionExporter: SessionExporter? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionHolder.currentSummary.collect { summary ->
                if (summary != null) {
                    val movementCards = summary.definition.movements.map { mov ->
                        val eval = summary.evaluations.find { it.index == mov.index }
                        MovementCardData(
                            index = mov.index,
                            nameEs = mov.nameEs,
                            nameKo = mov.nameKo,
                            stanceName = mov.stance.type.name,
                            category = eval?.category,
                            score = eval?.score,
                        )
                    }
                    _uiState.update {
                        it.copy(
                            globalScore = summary.globalScore,
                            evaluableCount = summary.evaluableCount,
                            partialCount = summary.partialCount,
                            notEvaluableCount = summary.notEvaluableCount,
                            movements = movementCards,
                            isExperimental = true,
                        )
                    }
                }
            }
        }
    }

    fun getShareIntent(isPdf: Boolean = true): Intent? {
        val summary = sessionHolder.currentSummary.value
        val exporter = sessionExporter
        if (summary == null || exporter == null) {
            return null
        }
        val (sessionEntity, movementEntities) = SessionEntityMapper.toEntities(summary)
        val sessionWithMovements = SessionWithMovements(sessionEntity, movementEntities)
        val uri = if (isPdf) {
            exporter.exportAsPdf(sessionWithMovements)
        } else {
            exporter.exportAsText(sessionWithMovements)
        }
        val mime = if (isPdf) "application/pdf" else "text/plain"
        return exporter.createShareIntent(uri, mime)
    }
}
