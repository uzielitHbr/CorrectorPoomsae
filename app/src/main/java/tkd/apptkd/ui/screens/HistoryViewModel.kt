package tkd.apptkd.ui.screens

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.taekwondo.poomsae.analysis.evaluation.Evaluability
import mx.taekwondo.poomsae.analysis.evaluation.MovementCategory
import mx.taekwondo.poomsae.analysis.evaluation.MovementEvaluation
import mx.taekwondo.poomsae.analysis.evaluation.MovementOutcome
import mx.taekwondo.poomsae.analysis.poomsae.PoomsaeDefinition
import tkd.apptkd.data.repository.SessionRepository
import tkd.apptkd.data.room.SessionEntity
import tkd.apptkd.data.room.SessionWithMovements
import tkd.apptkd.export.SessionExporter
import tkd.apptkd.ui.PracticeSessionHolder
import tkd.apptkd.ui.SessionSummaryData
import javax.inject.Inject

data class HistoryItemUi(
    val id: String,
    val startedAtMs: Long,
    val status: String,
    val mode: String,
    val globalScore: Double?,
    val evaluableCount: Int,
    val partialCount: Int,
    val notEvaluableCount: Int,
)

data class HistoryUiState(
    val sessions: List<HistoryItemUi> = emptyList(),
    val totalSessions: Int = 0,
    val averageCompletedScore: Double? = null,
    val isLoading: Boolean = false,
)

private const val SUBSCRIPTION_TIMEOUT_MS = 5000L

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val sessionExporter: SessionExporter,
    private val sessionHolder: PracticeSessionHolder,
    private val poomsaeDefinition: PoomsaeDefinition,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = sessionRepository.allSessionsFlow
        .map { list ->
            val items = list.map { it.toUi() }
            val completedScores = list
                .filter { it.status == "COMPLETED" && it.globalScore != null }
                .mapNotNull { it.globalScore }
            val avg = if (completedScores.isNotEmpty()) completedScores.average() else null
            HistoryUiState(
                sessions = items,
                totalSessions = items.size,
                averageCompletedScore = avg,
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = HistoryUiState(isLoading = true),
        )

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId)
        }
    }

    fun openSessionSummary(sessionId: String, onLoaded: () -> Unit) {
        viewModelScope.launch {
            val sessionWithMovements = sessionRepository.getSessionWithMovements(sessionId) ?: return@launch
            val summaryData = reconstructSummary(sessionWithMovements)
            sessionHolder.setSummary(summaryData)
            onLoaded()
        }
    }

    suspend fun getExportIntent(sessionId: String, isPdf: Boolean): Intent? {
        val sessionWithMovements = sessionRepository.getSessionWithMovements(sessionId) ?: return null
        val uri = if (isPdf) {
            sessionExporter.exportAsPdf(sessionWithMovements)
        } else {
            sessionExporter.exportAsText(sessionWithMovements)
        }
        val mime = if (isPdf) "application/pdf" else "text/plain"
        return sessionExporter.createShareIntent(uri, mime)
    }

    private fun reconstructSummary(data: SessionWithMovements): SessionSummaryData {
        val evals = data.movements.map { m ->
            MovementEvaluation(
                index = m.movementIndex,
                outcome = runCatching { MovementOutcome.valueOf(m.outcome) }.getOrDefault(MovementOutcome.EVALUATED),
                evaluability = m.evaluability?.let { runCatching { Evaluability.valueOf(it) }.getOrNull() },
                category = m.category?.let { runCatching { MovementCategory.valueOf(it) }.getOrNull() },
                score = m.score,
                metrics = emptyMap(),
                feedback = emptyList(),
                attempts = m.attempts,
                flags = emptySet(),
            )
        }
        return SessionSummaryData(
            definition = poomsaeDefinition,
            evaluations = evals,
            globalScore = data.session.globalScore,
            evaluableCount = data.session.evaluableCount,
            partialCount = data.session.partialCount,
            notEvaluableCount = data.session.notEvaluableCount,
            snapshots = emptyMap(),
        )
    }
}

private fun SessionEntity.toUi(): HistoryItemUi =
    HistoryItemUi(
        id = id,
        startedAtMs = startedAtMs,
        status = status,
        mode = mode,
        globalScore = globalScore,
        evaluableCount = evaluableCount,
        partialCount = partialCount,
        notEvaluableCount = notEvaluableCount,
    )

