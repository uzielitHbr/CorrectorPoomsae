package tkd.apptkd.pose

import mx.taekwondo.poomsae.analysis.model.CoordinateSpace
import mx.taekwondo.poomsae.analysis.model.Joint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Optional
import com.google.mediapipe.tasks.components.containers.Landmark as MpWorldLandmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark as MpImageLandmark

class MediaPipePoseAdapterTest {
    private companion object {
        private const val TOTAL_MP_LANDMARKS = 33
        private const val RIGHT_WRIST_MP_INDEX = 16
        private const val BASE_TIMESTAMP_MS = 1000L
        private const val NEXT_TIMESTAMP_MS = 1033L
        private const val EPS = 1e-5f
    }

    private lateinit var adapter: MediaPipePoseAdapter

    @Before
    fun setUp() {
        adapter = MediaPipePoseAdapter()
    }

    private fun createSyntheticLandmarks(
        modifyImageIndex: Int? = null,
        modifiedImageLandmark: MpImageLandmark? = null,
        modifyWorldIndex: Int? = null,
        modifiedWorldLandmark: MpWorldLandmark? = null,
    ): Pair<List<MpImageLandmark>, List<MpWorldLandmark>> {
        val imageList = (0 until TOTAL_MP_LANDMARKS).map { idx ->
            if (idx == modifyImageIndex && modifiedImageLandmark != null) {
                modifiedImageLandmark
            } else {
                MpImageLandmark.create(
                    idx * 0.02f + 0.1f,
                    idx * 0.02f + 0.2f,
                    idx * 0.01f - 0.5f,
                    Optional.of(0.95f),
                    Optional.of(0.99f),
                )
            }
        }
        val worldList = (0 until TOTAL_MP_LANDMARKS).map { idx ->
            if (idx == modifyWorldIndex && modifiedWorldLandmark != null) {
                modifiedWorldLandmark
            } else {
                MpWorldLandmark.create(
                    idx * 0.05f - 0.8f,
                    idx * 0.05f + 0.1f,
                    idx * 0.05f + 1.2f,
                    Optional.of(0.90f),
                    Optional.of(0.98f),
                )
            }
        }
        return imageList to worldList
    }

    @Test
    fun `valid landmarks map correctly to all 17 canonical joints in both coordinate spaces`() {
        val (images, worlds) = createSyntheticLandmarks()
        val result = adapter.adaptRaw(images, worlds, BASE_TIMESTAMP_MS)

        assertTrue("Se esperaba Success, pero fue $result", result is PoseAdaptationResult.Success)
        val frame = (result as PoseAdaptationResult.Success).frame
        assertNotNull(frame)
        requireNotNull(frame)

        assertEquals(BASE_TIMESTAMP_MS, frame.timestampMs)
        assertEquals(Joint.entries.size, frame.image.size)
        assertEquals(Joint.entries.size, frame.world.size)

        for (joint in Joint.entries) {
            val imgLm = frame.landmarks(CoordinateSpace.IMAGE)[joint]
            val wrdLm = frame.landmarks(CoordinateSpace.WORLD)[joint]

            assertNotNull("Joint $joint debe existir en image", imgLm)
            assertNotNull("Joint $joint debe existir en world", wrdLm)
            requireNotNull(imgLm)
            requireNotNull(wrdLm)

            val mpIdx = joint.mediaPipeIndex
            assertEquals(images[mpIdx].x(), imgLm.x, EPS)
            assertEquals(images[mpIdx].y(), imgLm.y, EPS)
            assertEquals(images[mpIdx].z(), imgLm.z, EPS)
            assertEquals(0.95f, imgLm.visibility, EPS)

            assertEquals(worlds[mpIdx].x(), wrdLm.x, EPS)
            assertEquals(worlds[mpIdx].y(), wrdLm.y, EPS)
            assertEquals(worlds[mpIdx].z(), wrdLm.z, EPS)
            assertEquals(0.90f, wrdLm.visibility, EPS)
        }
    }

    @Test
    fun `right wrist mapping matches MediaPipe landmark index 16 per F5 criteria`() {
        assertEquals(RIGHT_WRIST_MP_INDEX, Joint.RIGHT_WRIST.mediaPipeIndex)

        val (images, worlds) = createSyntheticLandmarks()
        val result = adapter.adaptRaw(images, worlds, BASE_TIMESTAMP_MS) as PoseAdaptationResult.Success
        val frame = requireNotNull(result.frame)

        val rightWristImage = requireNotNull(frame.image[Joint.RIGHT_WRIST])
        assertEquals(images[RIGHT_WRIST_MP_INDEX].x(), rightWristImage.x, EPS)
        assertEquals(images[RIGHT_WRIST_MP_INDEX].y(), rightWristImage.y, EPS)
    }

    @Test
    fun `empty or null landmarks list returns Success with null frame`() {
        val (images, worlds) = createSyntheticLandmarks()

        val emptyResult = adapter.adaptRaw(emptyList(), emptyList(), BASE_TIMESTAMP_MS)
        assertTrue(emptyResult is PoseAdaptationResult.Success)
        assertNull((emptyResult as PoseAdaptationResult.Success).frame)

        val nullResult = adapter.adaptRaw(null, worlds, NEXT_TIMESTAMP_MS)
        assertTrue(nullResult is PoseAdaptationResult.Success)
        assertNull((nullResult as PoseAdaptationResult.Success).frame)
    }

    @Test
    fun `strictly increasing timestamps are accepted`() {
        val (images, worlds) = createSyntheticLandmarks()

        val r1 = adapter.adaptRaw(images, worlds, 100L)
        val r2 = adapter.adaptRaw(images, worlds, 133L)
        val r3 = adapter.adaptRaw(images, worlds, 166L)

        assertTrue(r1 is PoseAdaptationResult.Success)
        assertTrue(r2 is PoseAdaptationResult.Success)
        assertTrue(r3 is PoseAdaptationResult.Success)
    }

    @Test
    fun `non increasing or equal timestamp is rejected with NON_INCREASING_TIMESTAMP`() {
        val (images, worlds) = createSyntheticLandmarks()

        val r1 = adapter.adaptRaw(images, worlds, 200L)
        assertTrue(r1 is PoseAdaptationResult.Success)

        val rEqual = adapter.adaptRaw(images, worlds, 200L)
        assertTrue(rEqual is PoseAdaptationResult.Rejected)
        assertEquals(PoseDropReason.NON_INCREASING_TIMESTAMP, (rEqual as PoseAdaptationResult.Rejected).reason)

        val rEarlier = adapter.adaptRaw(images, worlds, 150L)
        assertTrue(rEarlier is PoseAdaptationResult.Rejected)
        assertEquals(PoseDropReason.NON_INCREASING_TIMESTAMP, (rEarlier as PoseAdaptationResult.Rejected).reason)
    }

    @Test
    fun `negative timestamp is rejected with NEGATIVE_TIMESTAMP`() {
        val (images, worlds) = createSyntheticLandmarks()
        val result = adapter.adaptRaw(images, worlds, -5L)
        assertTrue(result is PoseAdaptationResult.Rejected)
        assertEquals(PoseDropReason.NEGATIVE_TIMESTAMP, (result as PoseAdaptationResult.Rejected).reason)
    }

    @Test
    fun `truncated landmark list is rejected with CORRUPTED_COORDINATES`() {
        val (images, worlds) = createSyntheticLandmarks()
        val truncatedImages = images.take(15) // no contiene index 16 (RIGHT_WRIST)
        val result = adapter.adaptRaw(truncatedImages, worlds, BASE_TIMESTAMP_MS)
        assertTrue(result is PoseAdaptationResult.Rejected)
        assertEquals(PoseDropReason.CORRUPTED_COORDINATES, (result as PoseAdaptationResult.Rejected).reason)
    }

    @Test
    fun `non-finite coordinates in image or world space are rejected with CORRUPTED_COORDINATES`() {
        val nanImageLandmark = MpImageLandmark.create(Float.NaN, 0.5f, 0.5f)
        val (imagesWithNan, worlds) = createSyntheticLandmarks(
            modifyImageIndex = RIGHT_WRIST_MP_INDEX,
            modifiedImageLandmark = nanImageLandmark,
        )
        val resNan = adapter.adaptRaw(imagesWithNan, worlds, BASE_TIMESTAMP_MS)
        assertTrue(resNan is PoseAdaptationResult.Rejected)
        assertEquals(PoseDropReason.CORRUPTED_COORDINATES, (resNan as PoseAdaptationResult.Rejected).reason)

        val infWorldLandmark = MpWorldLandmark.create(0.1f, Float.POSITIVE_INFINITY, 0.5f)
        val (images, worldsWithInf) = createSyntheticLandmarks(
            modifyWorldIndex = RIGHT_WRIST_MP_INDEX,
            modifiedWorldLandmark = infWorldLandmark,
        )
        val resInf = adapter.adaptRaw(images, worldsWithInf, NEXT_TIMESTAMP_MS)
        assertTrue(resInf is PoseAdaptationResult.Rejected)
        assertEquals(PoseDropReason.CORRUPTED_COORDINATES, (resInf as PoseAdaptationResult.Rejected).reason)
    }

    @Test
    fun `reset clears last timestamp state`() {
        val (images, worlds) = createSyntheticLandmarks()
        adapter.adaptRaw(images, worlds, 500L)

        adapter.reset()
        // Después de reset, un timestamp menor o igual que 500 debe ser admitido
        val resAfterReset = adapter.adaptRaw(images, worlds, 100L)
        assertTrue(resAfterReset is PoseAdaptationResult.Success)
    }
}

