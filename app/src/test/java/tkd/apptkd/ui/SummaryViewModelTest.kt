package tkd.apptkd.ui

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mx.taekwondo.poomsae.analysis.evaluation.Evaluability
import mx.taekwondo.poomsae.analysis.evaluation.MovementCategory
import mx.taekwondo.poomsae.analysis.evaluation.MovementEvaluation
import mx.taekwondo.poomsae.analysis.evaluation.MovementOutcome
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tkd.apptkd.ui.screens.SummaryViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionHolder: PracticeSessionHolder
    private lateinit var viewModel: SummaryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionHolder = PracticeSessionHolder()
        viewModel = SummaryViewModel(sessionHolder)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialSummaryStateIsEmpty() = runTest(testDispatcher) {
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.globalScore)
            assertEquals(0, state.evaluableCount)
            assertTrue(state.movements.isEmpty())
        }
    }

    @Test
    fun testSummaryPopulatedWhenSessionHolderEmits() = runTest(testDispatcher) {
        val definition = TestPoomsaeHelper.loadDefinition()
        val mockEvaluations = listOf(
            MovementEvaluation(
                index = 1,
                outcome = MovementOutcome.EVALUATED,
                evaluability = Evaluability.EVALUABLE,
                category = MovementCategory.CORRECT,
                score = 92.0,
                metrics = emptyMap(),
                feedback = emptyList(),
                attempts = 1,
            ),
        )

        viewModel.uiState.test {
            awaitItem() // Initial empty state

            sessionHolder.setSummary(
                SessionSummaryData(
                    definition = definition,
                    evaluations = mockEvaluations,
                    globalScore = 92.0,
                    evaluableCount = 1,
                    partialCount = 0,
                    notEvaluableCount = 17,
                ),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertEquals(92.0, state.globalScore)
            assertEquals(1, state.evaluableCount)
            assertEquals(17, state.notEvaluableCount)
            assertEquals(18, state.movements.size)
            assertEquals(MovementCategory.CORRECT, state.movements[0].category)
            assertNull(state.movements[1].category)
        }
    }
}

