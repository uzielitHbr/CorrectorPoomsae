package tkd.apptkd.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tkd.apptkd.data.repository.SessionRepository
import tkd.apptkd.data.room.AppDatabase
import tkd.apptkd.export.SessionExporter
import tkd.apptkd.ui.screens.HistoryViewModel

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: SessionRepository
    private lateinit var exporter: SessionExporter
    private lateinit var holder: PracticeSessionHolder
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SessionRepository(db)
        exporter = SessionExporter(context)
        holder = PracticeSessionHolder()
        val definition = TestPoomsaeHelper.loadDefinition()

        viewModel = HistoryViewModel(
            sessionRepository = repository,
            sessionExporter = exporter,
            sessionHolder = holder,
            poomsaeDefinition = definition,
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testHistoryStateEmitsSessionsAndCalculatesAverage() = runTest(testDispatcher) {
        repository.saveCompletedSession(
            summaryData = SessionSummaryData(
                definition = TestPoomsaeHelper.loadDefinition(),
                evaluations = emptyList(),
                globalScore = 80.0,
                evaluableCount = 18,
                partialCount = 0,
                notEvaluableCount = 0,
            ),
        )
        repository.saveCompletedSession(
            summaryData = SessionSummaryData(
                definition = TestPoomsaeHelper.loadDefinition(),
                evaluations = emptyList(),
                globalScore = 90.0,
                evaluableCount = 18,
                partialCount = 0,
                notEvaluableCount = 0,
            ),
        )

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.sessions.size < 2) {
                state = awaitItem()
            }
            assertEquals(2, state.totalSessions)
            assertEquals(85.0, state.averageCompletedScore)
        }
    }

    @Test
    fun testDeleteSessionRemovesFromState() = runTest(testDispatcher) {
        val sessionId = repository.saveCompletedSession(
            summaryData = SessionSummaryData(
                definition = TestPoomsaeHelper.loadDefinition(),
                evaluations = emptyList(),
                globalScore = 75.0,
                evaluableCount = 18,
                partialCount = 0,
                notEvaluableCount = 0,
            ),
        )

        viewModel.deleteSession(sessionId)

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.sessions.isNotEmpty()) {
                state = awaitItem()
            }
            assertEquals(0, state.sessions.size)
        }
    }

    @Test
    fun testOpenSessionSummaryLoadsHolder() = runTest(testDispatcher) {
        val sessionId = repository.saveCompletedSession(
            summaryData = SessionSummaryData(
                definition = TestPoomsaeHelper.loadDefinition(),
                evaluations = emptyList(),
                globalScore = 82.0,
                evaluableCount = 18,
                partialCount = 0,
                notEvaluableCount = 0,
            ),
        )

        var loaded = false
        viewModel.openSessionSummary(sessionId) {
            loaded = true
        }

        repeat(50) {
            advanceUntilIdle()
            if (loaded) return@repeat
            Thread.sleep(20L)
        }
        assertTrue(loaded)
        assertNotNull(holder.currentSummary.value)
        assertEquals(82.0, holder.currentSummary.value?.globalScore)
    }
}

