package tkd.apptkd.video

import android.graphics.Bitmap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.video.VideoMetadata
import mx.taekwondo.poomsae.analysis.video.VideoRejectionReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tkd.apptkd.ui.TestPoomsaeHelper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineVideoProcessorTest {

    private companion object {
        private const val SHORT_DURATION_MS = 200L
        private const val WIDTH = 1280
        private const val HEIGHT = 720
        private const val FILE_SIZE = 10L * 1024L * 1024L
    }

    private val definition = TestPoomsaeHelper.loadDefinition()

    private class FakeExtractor(
        private val metadata: VideoMetadata,
        private val onFrame: ((Long) -> Unit)? = null,
    ) : VideoFrameExtractor {
        var isClosed = false
        val requestedTimes = mutableListOf<Long>()
        val createdBitmaps = mutableListOf<Bitmap>()

        override fun extractMetadata(): VideoMetadata = metadata

        override fun getScaledFrameAt(timeMs: Long, targetWidth: Int, targetHeight: Int): Bitmap? {
            requestedTimes.add(timeMs)
            onFrame?.invoke(timeMs)
            val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            createdBitmaps.add(bmp)
            return bmp
        }

        override fun close() {
            isClosed = true
        }
    }

    private class FakeDetector : VideoPoseDetector {
        var isClosed = false
        val processedTimestamps = mutableListOf<Long>()

        override fun processFrame(bitmap: Bitmap, timestampMs: Long): PoseFrame? {
            processedTimestamps.add(timestampMs)
            return null
        }

        override fun close() {
            isClosed = true
        }
    }

    @Test
    fun `rejected video stops immediately without extracting frames and closes resources (SC-28)`() = runTest {
        val processor = OfflineVideoProcessor(definition)
        val invalidMetadata = VideoMetadata(
            durationMs = (AnalysisConfig.VIDEO_MAX_DURATION_SECONDS * 1000L) + 5000L,
            width = WIDTH,
            height = HEIGHT,
            rotationDeg = 0,
            fileSizeBytes = FILE_SIZE,
            hasVideoTrack = true,
        )
        val extractor = FakeExtractor(invalidMetadata)
        val detector = FakeDetector()

        val outcome = processor.process(extractor, detector)

        assertTrue(outcome is VideoAnalysisOutcome.Rejected)
        assertEquals(VideoRejectionReason.DURATION_TOO_LONG, (outcome as VideoAnalysisOutcome.Rejected).reason)
        assertTrue(extractor.requestedTimes.isEmpty())
        assertTrue(detector.processedTimestamps.isEmpty())
        assertTrue(extractor.isClosed)
        assertTrue(detector.isClosed)
    }

    @Test
    fun `successful processing samples frames with intrinsic timestamps and recycles bitmaps`() = runTest {
        val processor = OfflineVideoProcessor(definition)
        val validMetadata = VideoMetadata(
            durationMs = SHORT_DURATION_MS,
            width = WIDTH,
            height = HEIGHT,
            rotationDeg = 0,
            fileSizeBytes = FILE_SIZE,
            hasVideoTrack = true,
        )
        val extractor = FakeExtractor(validMetadata)
        val detector = FakeDetector()

        val progressList = mutableListOf<Float>()
        val outcome = processor.process(
            extractor = extractor,
            detector = detector,
            onProgress = { progress, _, _, _ ->
                progressList.add(progress)
            },
        )

        assertTrue("Expected Success, but got $outcome", outcome is VideoAnalysisOutcome.Success)
        val summary = (outcome as VideoAnalysisOutcome.Success).summary
        assertNotNull(summary)
        assertEquals(definition, summary.definition)

        // Verificación de timestamps intrínsecos crecientes
        assertTrue(extractor.requestedTimes.isNotEmpty())
        assertEquals(extractor.requestedTimes, detector.processedTimestamps)
        for (i in 1 until extractor.requestedTimes.size) {
            assertTrue(extractor.requestedTimes[i] > extractor.requestedTimes[i - 1])
        }

        // Verificación de memoria acotada (todos los bitmaps fueron reciclados)
        assertTrue(extractor.createdBitmaps.isNotEmpty())
        for (bmp in extractor.createdBitmaps) {
            assertTrue("Bitmap at index should be recycled", bmp.isRecycled)
        }

        assertTrue(progressList.isNotEmpty())
        assertTrue(extractor.isClosed)
        assertTrue(detector.isClosed)
    }

    @Test
    fun `cancellation aborts processing immediately and releases resources (SC-29)`() {
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)

        testScope.runTest {
            val processor = OfflineVideoProcessor(definition)
            val validMetadata = VideoMetadata(
                durationMs = 10_000L,
                width = WIDTH,
                height = HEIGHT,
                rotationDeg = 0,
                fileSizeBytes = FILE_SIZE,
                hasVideoTrack = true,
            )

            var frameCount = 0
            val extractor = FakeExtractor(validMetadata) {
                frameCount++
            }
            val detector = FakeDetector()

            var finalOutcome: VideoAnalysisOutcome? = null
            val job = launch {
                finalOutcome = processor.process(extractor, detector)
            }

            testDispatcher.scheduler.advanceUntilIdle()
            job.cancelAndJoin()

            assertTrue(extractor.isClosed)
            assertTrue(detector.isClosed)
        }
    }
}
