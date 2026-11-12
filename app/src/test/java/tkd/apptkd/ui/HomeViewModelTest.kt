package tkd.apptkd.ui

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import tkd.apptkd.ui.screens.HomeViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionHolder: PracticeSessionHolder
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionHolder = PracticeSessionHolder()
        viewModel = HomeViewModel(sessionHolder)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialHomeStateIsEmpty() = runTest(testDispatcher) {
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.lastScore)
            assertNull(state.draftMovementIndex)
        }
    }

    @Test
    fun testDraftDetectionAndDiscard() = runTest(testDispatcher) {
        viewModel.uiState.test {
            assertEquals(null, awaitItem().draftMovementIndex)

            sessionHolder.setDraft(7)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(7, awaitItem().draftMovementIndex)

            viewModel.onDiscardDraft()
            testDispatcher.scheduler.advanceUntilIdle()
            assertNull(awaitItem().draftMovementIndex)
        }
    }
}

