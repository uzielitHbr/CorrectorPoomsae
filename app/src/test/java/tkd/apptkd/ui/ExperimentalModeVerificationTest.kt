package tkd.apptkd.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.config.ParameterCalibrationStatus
import mx.taekwondo.poomsae.analysis.config.ValidationState
import mx.taekwondo.poomsae.analysis.config.ValidationStateEvaluator
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tkd.apptkd.ui.screens.SummaryViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class ExperimentalModeVerificationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `system configuration enforces NO_VALIDADO and PENDIENTE_CALIBRAR`() {
        assertEquals(
            "ValidationState must be strictly NO_VALIDADO in Phase F12",
            ValidationState.NO_VALIDADO,
            AnalysisConfig.VALIDATION_STATE,
        )
        assertEquals(
            "Calibration status must remain PENDIENTE_CALIBRAR (Rule R-05)",
            ParameterCalibrationStatus.PENDIENTE_CALIBRAR,
            AnalysisConfig.CALIBRATION_STATUS,
        )
    }

    @Test
    fun `ValidationStateEvaluator forbids validated badge and confirms experimental mode`() {
        assertTrue(
            "System must be in experimental mode under active config",
            ValidationStateEvaluator.isExperimental(),
        )
        assertFalse(
            "System MUST NOT display validated badge under active config",
            ValidationStateEvaluator.canDisplayValidatedBadge(),
        )
    }

    @Test
    fun `SummaryViewModel defaults and emits isExperimental true`() = runTest(testDispatcher) {
        val sessionHolder = PracticeSessionHolder()
        val viewModel = SummaryViewModel(sessionHolder)

        // Initial state
        assertTrue(viewModel.uiState.value.isExperimental)

        // After summary set
        sessionHolder.setSummary(
            SessionSummaryData(
                definition = TestPoomsaeHelper.loadDefinition(),
                evaluations = emptyList(),
                globalScore = 80.0,
                evaluableCount = 0,
                partialCount = 0,
                notEvaluableCount = 18,
            ),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(
            "SummaryUiState.isExperimental must remain true after emitting summary",
            viewModel.uiState.value.isExperimental,
        )
    }
}

