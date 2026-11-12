package tkd.apptkd.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.taekwondo.poomsae.analysis.evaluation.MovementEvaluation
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.poomsae.PoomsaeDefinition
import javax.inject.Inject
import javax.inject.Singleton

data class SessionSummaryData(
    val definition: PoomsaeDefinition,
    val evaluations: List<MovementEvaluation>,
    val globalScore: Double?,
    val evaluableCount: Int,
    val partialCount: Int,
    val notEvaluableCount: Int,
    val snapshots: Map<Int, PoseFrame> = emptyMap(),
)

/**
 * Contenedor en memoria del resultado de la última sesión para navegación inmediata (F9).
 *
 * En F10 este flujo se complementará con persistencia en base de datos Room.
 */
@Singleton
class PracticeSessionHolder @Inject constructor() {
    private val _currentSummary = MutableStateFlow<SessionSummaryData?>(null)
    val currentSummary: StateFlow<SessionSummaryData?> = _currentSummary.asStateFlow()

    private val _lastDraftMovementIndex = MutableStateFlow<Int?>(null)
    val lastDraftMovementIndex: StateFlow<Int?> = _lastDraftMovementIndex.asStateFlow()

    private val _lastScore = MutableStateFlow<Double?>(null)
    val lastScore: StateFlow<Double?> = _lastScore.asStateFlow()

    fun setSummary(data: SessionSummaryData) {
        _currentSummary.value = data
        _lastScore.value = data.globalScore
        _lastDraftMovementIndex.value = null
    }

    fun setDraft(movementIndex: Int) {
        _lastDraftMovementIndex.value = movementIndex
    }

    fun clearDraft() {
        _lastDraftMovementIndex.value = null
    }

    fun clear() {
        _currentSummary.value = null
    }
}

