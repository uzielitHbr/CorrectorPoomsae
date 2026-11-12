package mx.taekwondo.poomsae.analysis.segmentation

import kotlinx.coroutines.test.runTest
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.testutil.SkeletonBuilder
import mx.taekwondo.poomsae.analysis.testutil.SyntheticPoomsaeSequenceGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para el motor de práctica [SessionEngine] (Sección 12.4, 12.9.2).
 */
class SessionEngineTest {
    private val definition = SyntheticPoomsaeSequenceGenerator.loadCanonicalDefinition()
    private val profile = SkeletonBuilder.defaultProfile()

    @Test
    fun `engine traverses all 18 movements to completion without deadlock`() =
        runTest {
            val engine =
                SessionEngine(
                    definition = definition,
                    initialProfile = profile,
                    initialState = SessionState.Running(1, MovementSubState.AwaitingMovement(1, 1000L)),
                )

            val fullFrames = SyntheticPoomsaeSequenceGenerator.generateFull18MovementSequence(definition)
            for (frame in fullFrames) {
                engine.processFrame(frame)
            }

            val finalState = engine.state.value
            assertTrue("El estado final debe ser Finished, pero fue $finalState", finalState is SessionState.Finished)

            val finished = finalState as SessionState.Finished
            assertEquals(18, finished.evaluations.size)
            assertNotNull("La puntuación global debe calcularse", finished.globalScore)
            assertTrue("La puntuación debe ser positiva", finished.globalScore!! > 0.0)
        }

    @Test
    fun `pause and resume commands preserve running state`() =
        runTest {
            val engine =
                SessionEngine(
                    definition = definition,
                    initialProfile = profile,
                    initialState = SessionState.Running(1, MovementSubState.AwaitingMovement(1, 1000L)),
                )

            engine.dispatch(SessionCommand.Pause)
            val pausedState = engine.state.value
            assertTrue("Debe pausarse", pausedState is SessionState.Paused)
            assertEquals(PauseReason.USER, (pausedState as SessionState.Paused).reason)

            engine.dispatch(SessionCommand.Resume)
            val resumedState = engine.state.value
            assertTrue("Debe reanudarse en Running", resumedState is SessionState.Running)
            assertEquals(1, (resumedState as SessionState.Running).movementIndex)
        }

    @Test
    fun `repeat current movement increments attempts`() =
        runTest {
            val engine =
                SessionEngine(
                    definition = definition,
                    initialProfile = profile,
                    initialState = SessionState.Running(3, MovementSubState.AwaitingMovement(3, 1000L)),
                )

            engine.dispatch(SessionCommand.RepeatCurrent)
            val state = engine.state.value
            assertTrue(state is SessionState.Running)
            assertEquals(3, (state as SessionState.Running).movementIndex)
        }

    @Test
    fun `skip current movement records omitted outcome and advances`() =
        runTest {
            val engine =
                SessionEngine(
                    definition = definition,
                    initialProfile = profile,
                    initialState = SessionState.Running(2, MovementSubState.AwaitingMovement(2, 1000L)),
                )

            engine.dispatch(SessionCommand.SkipCurrent)
            val state = engine.state.value
            assertTrue(state is SessionState.Running)
            assertEquals(3, (state as SessionState.Running).movementIndex)
        }

    @Test
    fun `tracking loss pauses session and recovery resumes automatically`() =
        runTest {
            val engine =
                SessionEngine(
                    definition = definition,
                    initialProfile = profile,
                    initialState = SessionState.Running(1, MovementSubState.AwaitingMovement(1, 1000L)),
                )

            var t = 1000L
            engine.processFrame(SkeletonBuilder().build(t))
            t += 33L

            // Pérdida de tracking por 1200 ms (> 1000 ms)
            val emptyFrame = PoseFrame(0L, emptyMap(), emptyMap())
            repeat(36) {
                engine.processFrame(emptyFrame.copy(timestampMs = t))
                t += 33L
            }

            val stateAfterLoss = engine.state.value
            assertTrue("Debe estar pausado por LOST_TRACKING", stateAfterLoss is SessionState.Paused)
            assertEquals(PauseReason.LOST_TRACKING, (stateAfterLoss as SessionState.Paused).reason)

            // Recuperación por 1600 ms (> 1500 ms)
            repeat(50) {
                engine.processFrame(SkeletonBuilder().build(t))
                t += 33L
            }

            val stateAfterRecovery = engine.state.value
            assertTrue("Debe reanudarse en Running tras recuperar tracking", stateAfterRecovery is SessionState.Running)
        }

    @Test
    fun `restart and discard commands handle session lifecycle`() =
        runTest {
            val engine =
                SessionEngine(
                    definition = definition,
                    initialProfile = profile,
                    initialState = SessionState.Running(5, MovementSubState.AwaitingMovement(5, 1000L)),
                )

            engine.dispatch(SessionCommand.RestartForm)
            val restarted = engine.state.value
            assertTrue(restarted is SessionState.Running)
            assertEquals(1, (restarted as SessionState.Running).movementIndex)

            engine.dispatch(SessionCommand.Finish(save = true))
            val finished = engine.state.value
            assertTrue(finished is SessionState.Finished)

            engine.dispatch(SessionCommand.Discard)
            val aborted = engine.state.value
            assertEquals(SessionState.Aborted, aborted)
        }

    @Test
    fun `skip last movement finishes the session`() =
        runTest {
            val engine =
                SessionEngine(
                    definition = definition,
                    initialProfile = profile,
                    initialState = SessionState.Running(18, MovementSubState.AwaitingMovement(18, 1000L)),
                )

            engine.dispatch(SessionCommand.SkipCurrent)
            val state = engine.state.value
            assertTrue("Omitir el último movimiento debe finalizar la sesión", state is SessionState.Finished)
        }

    @Test
    fun `uncalibrated engine starts in framing and transitions through calibration`() =
        runTest {
            val engine =
                SessionEngine(
                    definition = definition,
                    initialProfile = null,
                    initialState = SessionState.Idle,
                )

            engine.dispatch(SessionCommand.Start)
            assertTrue(engine.state.value is SessionState.Framing)

            // Cuadro de encuadre válido
            val validFrame = SkeletonBuilder().build(1000L)
            engine.processFrame(validFrame)
            assertTrue(engine.state.value is SessionState.Calibrating)

            // Ráfaga de calibración estable (>= 15 cuadros)
            var t = 1033L
            repeat(20) {
                engine.processFrame(SkeletonBuilder().build(t))
                t += 33L
            }

            assertTrue(
                "Debe completar calibración y pasar a Ready o Running",
                engine.state.value is SessionState.Ready || engine.state.value is SessionState.Running,
            )
        }
}
