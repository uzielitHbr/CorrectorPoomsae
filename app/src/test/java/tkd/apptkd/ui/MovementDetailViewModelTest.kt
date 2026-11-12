package tkd.apptkd.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mx.taekwondo.poomsae.analysis.evaluation.Evaluability
import mx.taekwondo.poomsae.analysis.evaluation.MovementCategory
import mx.taekwondo.poomsae.analysis.evaluation.MovementEvaluation
import mx.taekwondo.poomsae.analysis.evaluation.MovementOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import tkd.apptkd.ui.screens.MovementDetailViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class MovementDetailViewModelTest {

    @Test
    fun testMovementDetailBuildsCorrectState() {
        val definition = TestPoomsaeHelper.loadDefinition()
        val sessionHolder = PracticeSessionHolder()
        val mockEvaluation = MovementEvaluation(
            index = 1,
            outcome = MovementOutcome.EVALUATED,
            evaluability = Evaluability.EVALUABLE,
            category = MovementCategory.CORRECT,
            score = 96.0,
            metrics = emptyMap(),
            feedback = emptyList(),
            attempts = 1,
        )

        sessionHolder.setSummary(
            SessionSummaryData(
                definition = definition,
                evaluations = listOf(mockEvaluation),
                globalScore = 96.0,
                evaluableCount = 1,
                partialCount = 0,
                notEvaluableCount = 17,
            ),
        )

        val savedStateHandle = SavedStateHandle(mapOf("movementIndex" to 1))
        val viewModel = MovementDetailViewModel(savedStateHandle, sessionHolder)

        val state = viewModel.uiState.value
        assertEquals(1, state.index)
        assertEquals("Bloqueo bajo", state.nameEs)
        assertEquals(MovementCategory.CORRECT, state.category)
        assertEquals(96.0, state.score)
    }
}

