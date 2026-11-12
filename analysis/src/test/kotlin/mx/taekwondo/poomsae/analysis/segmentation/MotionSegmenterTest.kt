package mx.taekwondo.poomsae.analysis.segmentation

import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.testutil.SkeletonBuilder
import mx.taekwondo.poomsae.analysis.testutil.SyntheticPoomsaeSequenceGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para el segmentador temporal [MotionSegmenter] (Sección 12.4).
 */
class MotionSegmenterTest {
    private val definition = SyntheticPoomsaeSequenceGenerator.loadCanonicalDefinition()
    private val profile = SkeletonBuilder.defaultProfile()

    @Test
    fun `segmenter transitions from awaiting to inMovement and holding`() {
        val segmenter = MotionSegmenter(definition, profile)
        segmenter.startMovement(1, 1000L, applyLockout = false)

        val mov1 = definition.movements[0]
        val frames = SyntheticPoomsaeSequenceGenerator.generateMovementFrames(mov1, 1000L)

        val collectedEvents = mutableListOf<SegmenterEvent>()
        for (frame in frames) {
            collectedEvents.addAll(segmenter.processFrame(frame))
        }

        val started = collectedEvents.filterIsInstance<SegmenterEvent.MovementStarted>()
        assertEquals(1, started.size)
        assertEquals(1, started.first().n)

        val held = collectedEvents.filterIsInstance<SegmenterEvent.MovementHeld>()
        assertEquals(1, held.size)
        assertEquals(1, held.first().n)
        assertTrue("La ventana de sostén debe contener cuadros", held.first().holdWindow.isNotEmpty())
    }

    @Test
    fun `kick movement detects elevation peak frame`() {
        val segmenter = MotionSegmenter(definition, profile)
        val mov14 = definition.movements[13] // Movimiento 14: Ap chagi
        segmenter.startMovement(14, 1000L, applyLockout = false)

        val frames = SyntheticPoomsaeSequenceGenerator.generateMovementFrames(mov14, 1000L)
        val collectedEvents = mutableListOf<SegmenterEvent>()
        for (frame in frames) {
            collectedEvents.addAll(segmenter.processFrame(frame))
        }

        val held = collectedEvents.filterIsInstance<SegmenterEvent.MovementHeld>().firstOrNull()
        assertNotNull("Debe emitir MovementHeld para movimiento 14", held)
        assertNotNull("Movimiento con PEAK_THEN_HOLD debe tener peakFrame", held?.peakFrame)
    }

    @Test
    fun `stationary stance movements 5 to 6 do not deadlock or confuse feet`() {
        val segmenter = MotionSegmenter(definition, profile)
        val mov6 = definition.movements[5] // Movimiento 6: Momtong baro jireugi (sharesStanceWithPrevious = true)
        assertTrue(mov6.sharesStanceWithPrevious)

        segmenter.startMovement(6, 1000L, applyLockout = false)
        val frames = SyntheticPoomsaeSequenceGenerator.generateMovementFrames(mov6, 1000L)

        val collectedEvents = mutableListOf<SegmenterEvent>()
        for (frame in frames) {
            collectedEvents.addAll(segmenter.processFrame(frame))
        }

        val started = collectedEvents.filterIsInstance<SegmenterEvent.MovementStarted>()
        assertEquals(
            "Movimiento 6 debe iniciar con desplazamiento de brazos aunque los pies no se muevan",
            1,
            started.size,
        )

        val held = collectedEvents.filterIsInstance<SegmenterEvent.MovementHeld>()
        assertEquals("Movimiento 6 debe completarse y sostenerse", 1, held.size)
    }

    @Test
    fun `inactivity produces timeout event`() {
        val segmenter = MotionSegmenter(definition, profile)
        segmenter.startMovement(1, 1000L, applyLockout = false)

        val collectedEvents = mutableListOf<SegmenterEvent>()
        var t = 1000L

        // Cuadros inmóviles durante 8500 ms (> T_MOVEMENT_TIMEOUT_MS = 8000 ms)
        while (t <= 9600L) {
            val f = SkeletonBuilder().build(t)
            collectedEvents.addAll(segmenter.processFrame(f))
            t += 200L
        }

        val timeouts = collectedEvents.filterIsInstance<SegmenterEvent.TimedOut>()
        assertTrue("Debe emitir TimedOut tras inactividad prolongada", timeouts.isNotEmpty())
    }

    @Test
    fun `tracking loss and recovery are detected accurately`() {
        val segmenter = MotionSegmenter(definition, profile)
        segmenter.startMovement(1, 1000L, applyLockout = false)

        val collectedEvents = mutableListOf<SegmenterEvent>()
        var t = 1000L

        // Cuadro válido inicial
        collectedEvents.addAll(segmenter.processFrame(SkeletonBuilder().build(t)))
        t += 33L

        // Cuadros inválidos durante 1200 ms (> T_LOSS_SHORT_MS = 1000 ms)
        val invalidFrame = PoseFrame(timestampMs = 0L, image = emptyMap(), world = emptyMap())
        repeat(36) {
            collectedEvents.addAll(segmenter.processFrame(invalidFrame.copy(timestampMs = t)))
            t += 33L
        }

        val lostEvents = collectedEvents.filterIsInstance<SegmenterEvent.TrackingLost>()
        assertEquals(1, lostEvents.size)

        // Cuadros válidos recuperados durante 1600 ms (> T_RECOVER_MS = 1500 ms)
        repeat(50) {
            collectedEvents.addAll(segmenter.processFrame(SkeletonBuilder().build(t)))
            t += 33L
        }

        val recoveredEvents = collectedEvents.filterIsInstance<SegmenterEvent.TrackingRecovered>()
        assertEquals(1, recoveredEvents.size)
    }
}
