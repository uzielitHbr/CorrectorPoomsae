package tkd.apptkd.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tkd.apptkd.ui.PracticeSessionHolder
import javax.inject.Inject

data class HomeUiState(
    val lastScore: Double? = null,
    val draftMovementIndex: Int? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionHolder: PracticeSessionHolder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionHolder.lastScore.collect { score ->
                _uiState.update { it.copy(lastScore = score) }
            }
        }
        viewModelScope.launch {
            sessionHolder.lastDraftMovementIndex.collect { draftIdx ->
                _uiState.update { it.copy(draftMovementIndex = draftIdx) }
            }
        }
    }

    fun onDiscardDraft() {
        sessionHolder.clearDraft()
    }
}

