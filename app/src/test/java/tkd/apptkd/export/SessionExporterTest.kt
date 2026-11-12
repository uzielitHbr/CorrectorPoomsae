package tkd.apptkd.export

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tkd.apptkd.data.room.MovementResultEntity
import tkd.apptkd.data.room.SessionEntity
import tkd.apptkd.data.room.SessionWithMovements
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowPdfDocument::class])
class SessionExporterTest {

    private lateinit var context: Context
    private lateinit var exporter: SessionExporter

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        exporter = SessionExporter(context)
    }

    @Test
    fun testExportAsTextContainsRequiredHeadersAndDisclaimers() {
        val sessionWithMovements = createTestSessionWithMovements()
        val uri = exporter.exportAsText(sessionWithMovements)
        assertNotNull(uri)

        val exportFile = File(context.cacheDir, "exports/sesion_${sessionWithMovements.session.id}.txt")
        assertTrue(exportFile.exists())
        val text = exportFile.readText()

        assertTrue(text.contains("TAEGEUK COACH"))
        assertTrue(text.contains("Taegeuk 1 Jang"))
        assertTrue(text.contains("La puntuación es orientativa y no es una calificación oficial"))
        assertTrue(text.contains("Secuencia fijada documentalmente; no certificada oficialmente"))
        assertTrue(text.contains("87.5 / 100"))
        assertTrue(text.contains("Movimiento 1:"))
    }

    @Test
    fun testExportAsPdfCreatesValidFile() {
        val sessionWithMovements = createTestSessionWithMovements()
        val uri = exporter.exportAsPdf(sessionWithMovements)
        assertNotNull(uri)

        val pdfFile = File(context.cacheDir, "exports/sesion_${sessionWithMovements.session.id}.pdf")
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)
    }

    @Test
    fun testCreateShareIntent() {
        val sessionWithMovements = createTestSessionWithMovements()
        val uri = exporter.exportAsText(sessionWithMovements)
        val intent = exporter.createShareIntent(uri, "text/plain")

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals(uri, intent.getParcelableExtra(Intent.EXTRA_STREAM))
        assertTrue((intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
    }

    private fun createTestSessionWithMovements(): SessionWithMovements {
        val session = SessionEntity(
            id = "test-export-id-1234",
            startedAtMs = 1731427200000L,
            endedAtMs = 1731427260000L,
            status = "COMPLETED",
            mode = "LIVE",
            strictMode = false,
            poomsaeStatus = "DOCUMENTALLY_FIXED",
            configVersion = "0.2.0-f2",
            poseModelName = "pose_landmarker_lite",
            appVersion = "1.0",
            cameraFacing = "FRONT",
            globalScore = 87.5,
            evaluableCount = 18,
            partialCount = 0,
            notEvaluableCount = 0,
            flagsJson = "{}",
        )
        val movements = (1..18).map { idx ->
            MovementResultEntity(
                sessionId = "test-export-id-1234",
                movementIndex = idx,
                outcome = "EVALUATED",
                evaluability = "EVALUABLE",
                category = "CORRECT",
                score = 90.0,
                attempts = 1,
                feedbackJson = "{}",
                metricsJson = "{}",
                snapshotPath = null,
            )
        }
        return SessionWithMovements(session, movements)
    }
}

