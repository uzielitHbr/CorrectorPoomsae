package tkd.apptkd.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tkd.apptkd.data.repository.SessionRepository
import tkd.apptkd.ui.PracticeSessionHolder
import javax.inject.Inject

data class HomeUiState(
    val lastScore: Double? = null,
    val draftMovementIndex: Int? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionHolder: PracticeSessionHolder,
    private val sessionRepository: SessionRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (sessionRepository != null) {
                combine(
                    sessionHolder.lastScore,
                    sessionRepository.allSessionsFlow,
                ) { holderScore, sessions ->
                    holderScore ?: sessions.firstOrNull { it.status == "COMPLETED" }?.globalScore
                }.collect { resolvedScore ->
                    _uiState.update { it.copy(lastScore = resolvedScore) }
                }
            } else {
                sessionHolder.lastScore.collect { score ->
                    _uiState.update { it.copy(lastScore = score) }
                }
            }
        }

        viewModelScope.launch {
            if (sessionRepository != null) {
                combine(
                    sessionHolder.lastDraftMovementIndex,
                    sessionRepository.getDraftFlow(),
                ) { holderDraft, dbDraft ->
                    holderDraft ?: dbDraft?.lastEvaluatedIndex
                }.collect { resolvedDraft ->
                    _uiState.update { it.copy(draftMovementIndex = resolvedDraft) }
                }
            } else {
                sessionHolder.lastDraftMovementIndex.collect { draftIdx ->
                    _uiState.update { it.copy(draftMovementIndex = draftIdx) }
                }
            }
        }
    }

    fun onDiscardDraft() {
        sessionHolder.clearDraft()
        sessionRepository?.let { repo ->
            viewModelScope.launch {
                repo.clearDraft()
            }
        }
    }
}
