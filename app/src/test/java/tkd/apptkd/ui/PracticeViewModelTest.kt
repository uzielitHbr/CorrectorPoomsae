package tkd.apptkd.ui

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mx.taekwondo.poomsae.analysis.poomsae.PoomsaeDefinition
import mx.taekwondo.poomsae.analysis.segmentation.MovementSubState
import mx.taekwondo.poomsae.analysis.segmentation.PauseReason
import mx.taekwondo.poomsae.analysis.segmentation.SessionEngine
import mx.taekwondo.poomsae.analysis.segmentation.SessionState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tkd.apptkd.camera.CameraFacing
import tkd.apptkd.ui.screens.PracticeAction
import tkd.apptkd.ui.screens.PracticeStep
import tkd.apptkd.ui.screens.PracticeViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var definition: PoomsaeDefinition
    private lateinit var sessionHolder: PracticeSessionHolder
    private lateinit var voicePlayer: VoicePlayer
    private lateinit var viewModel: PracticeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        definition = TestPoomsaeHelper.loadDefinition()
        sessionHolder = PracticeSessionHolder()
        voicePlayer = NoOpVoicePlayer()
        viewModel = PracticeViewModel(definition, sessionHolder, voicePlayer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialPracticeStepIsFraming() = runTest(testDispatcher) {
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.step is PracticeStep.Framing)
            assertEquals(CameraFacing.FRONT, state.cameraFacing)
            assertFalse(state.isVoiceEnabled)
            assertFalse(state.showExitDialog)
        }
    }

    @Test
    fun testTogglesUpdateUiState() = runTest(testDispatcher) {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.onAction(PracticeAction.ToggleVoice)
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(awaitItem().isVoiceEnabled)

            viewModel.onAction(PracticeAction.ToggleCameraFacing)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(CameraFacing.BACK, awaitItem().cameraFacing)

            viewModel.onAction(PracticeAction.SetExitDialogVisible(true))
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(awaitItem().showExitDialog)
        }
    }

    @Test
    fun testPauseAndResumeFlow() = runTest(testDispatcher) {
        val runningEngine = SessionEngine(
            definition = definition,
            initialState = SessionState.Running(1, MovementSubState.AwaitingMovement(1, 0L)),
        )
        val runningViewModel = PracticeViewModel(definition, sessionHolder, voicePlayer, runningEngine)

        runningViewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.step is PracticeStep.Running)

            runningViewModel.onAction(PracticeAction.Pause)
            testDispatcher.scheduler.advanceUntilIdle()

            val paused = awaitItem()
            assertTrue(paused.step is PracticeStep.Paused)
            assertEquals(PauseReason.USER, (paused.step as PracticeStep.Paused).reason)

            runningViewModel.onAction(PracticeAction.Resume)
            testDispatcher.scheduler.advanceUntilIdle()

            val resumed = awaitItem()
            assertTrue(resumed.step is PracticeStep.Running)
        }
    }

    @Test
    fun testSaveDraftAndExitRecordsDraft() = runTest(testDispatcher) {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.onAction(PracticeAction.SaveDraftAndExit)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, sessionHolder.lastDraftMovementIndex.value)
        }
    }

    @Test
    fun testDiscardSessionClearsDraft() = runTest(testDispatcher) {
        sessionHolder.setDraft(5)
        assertEquals(5, sessionHolder.lastDraftMovementIndex.value)

        viewModel.onAction(PracticeAction.DiscardSession)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, sessionHolder.lastDraftMovementIndex.value)
    }

    @Test
    fun testSkipAndRepeatCommands() = runTest(testDispatcher) {
        val runningEngine = SessionEngine(
            definition = definition,
            initialState = SessionState.Running(1, MovementSubState.AwaitingMovement(1, 0L)),
        )
        val runningViewModel = PracticeViewModel(definition, sessionHolder, voicePlayer, runningEngine)

        runningViewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(1, (initial.step as PracticeStep.Running).movementIndex)

            runningViewModel.onAction(PracticeAction.SkipCurrent)
            testDispatcher.scheduler.advanceUntilIdle()

            val skipped = awaitItem()
            assertTrue(skipped.step is PracticeStep.Running)
            assertEquals(2, (skipped.step as PracticeStep.Running).movementIndex)
        }
    }
}
