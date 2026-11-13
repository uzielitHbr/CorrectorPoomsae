package tkd.apptkd.video

import android.graphics.Bitmap
import android.net.Uri
import app.cash.turbine.test
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.video.VideoMetadata
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tkd.apptkd.R
import tkd.apptkd.data.repository.SessionRepository
import tkd.apptkd.ui.PracticeSessionHolder
import tkd.apptkd.ui.TestPoomsaeHelper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VideoAnalysisViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val definition = TestPoomsaeHelper.loadDefinition()
    private lateinit var sessionHolder: PracticeSessionHolder
    private lateinit var sessionRepository: SessionRepository

    private class TestExtractor(private val metadata: VideoMetadata) : VideoFrameExtractor {
        var isClosed = false
        override fun extractMetadata(): VideoMetadata = metadata
        override fun getScaledFrameAt(timeMs: Long, targetWidth: Int, targetHeight: Int): Bitmap? =
            Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        override fun close() {
            isClosed = true
        }
    }

    private class TestDetector : VideoPoseDetector {
        var isClosed = false
        override fun processFrame(bitmap: Bitmap, timestampMs: Long): PoseFrame? = null
        override fun close() {
            isClosed = true
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionHolder = PracticeSessionHolder()
        sessionRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is idle`() = runTest(testDispatcher) {
        val viewModel = VideoAnalysisViewModel(
            definition = definition,
            sessionHolder = sessionHolder,
            sessionRepository = sessionRepository,
            extractorFactory = { TestExtractor(createMetadata()) },
            detectorFactory = { TestDetector() },
        ).apply { analysisDispatcher = testDispatcher }

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertFalse(state.isCompleted)
            assertNull(state.errorMessageResId)
            assertNull(state.infoMessageResId)
        }
    }

    @Test
    fun `valid video completes and persists session as VIDEO`() = runTest(testDispatcher) {
        val metadata = createMetadata(durationMs = 200L)
        val viewModel = VideoAnalysisViewModel(
            definition = definition,
            sessionHolder = sessionHolder,
            sessionRepository = sessionRepository,
            extractorFactory = { TestExtractor(metadata) },
            detectorFactory = { TestDetector() },
        ).apply { analysisDispatcher = testDispatcher }

        val uri = mockk<Uri>(relaxed = true)
        viewModel.startAnalysis(uri)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertTrue(state.isCompleted)
            assertNotNull(sessionHolder.currentSummary.value)
        }

        coVerify {
            sessionRepository.saveCompletedSession(
                summaryData = any(),
                cameraFacing = "VIDEO",
                mode = "VIDEO",
            )
        }
    }

    @Test
    fun `video exceeding max duration is rejected with error message (SC-28)`() = runTest(testDispatcher) {
        val metadata = createMetadata(
            durationMs = (AnalysisConfig.VIDEO_MAX_DURATION_SECONDS * 1000L) + 1000L,
        )
        val viewModel = VideoAnalysisViewModel(
            definition = definition,
            sessionHolder = sessionHolder,
            sessionRepository = sessionRepository,
            extractorFactory = { TestExtractor(metadata) },
            detectorFactory = { TestDetector() },
        ).apply { analysisDispatcher = testDispatcher }

        val uri = mockk<Uri>(relaxed = true)
        viewModel.startAnalysis(uri)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertFalse(state.isCompleted)
            assertEquals(R.string.video_err_too_long, state.errorMessageResId)
        }

        viewModel.dismissError()
        assertEquals(null, viewModel.uiState.value.errorMessageResId)
    }

    @Test
    fun `cancelling analysis stops processing and sets cancel message (SC-29)`() = runTest(testDispatcher) {
        val metadata = createMetadata(durationMs = 10_000L)
        val viewModel = VideoAnalysisViewModel(
            definition = definition,
            sessionHolder = sessionHolder,
            sessionRepository = sessionRepository,
            extractorFactory = { TestExtractor(metadata) },
            detectorFactory = { TestDetector() },
        ).apply { analysisDispatcher = testDispatcher }

        val uri = mockk<Uri>(relaxed = true)
        viewModel.startAnalysis(uri)
        viewModel.cancelAnalysis()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertEquals(R.string.MSG_VIDEO_CANCEL, state.infoMessageResId)
            assertNull(sessionHolder.currentSummary.value)
        }

        viewModel.dismissInfo()
        assertEquals(null, viewModel.uiState.value.infoMessageResId)
    }

    private fun createMetadata(
        durationMs: Long = 1000L,
        width: Int = 1280,
        height: Int = 720,
    ) = VideoMetadata(
        durationMs = durationMs,
        width = width,
        height = height,
        rotationDeg = 0,
        fileSizeBytes = 5L * 1024L * 1024L,
        hasVideoTrack = true,
    )
}
