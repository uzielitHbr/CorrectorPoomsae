package tkd.apptkd.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tkd.apptkd.camera.CameraFacing
import tkd.apptkd.data.datastore.UserSettingsRepository
import tkd.apptkd.data.repository.SessionRepository
import tkd.apptkd.data.room.AppDatabase
import tkd.apptkd.data.room.SessionDraftEntity
import tkd.apptkd.data.room.SessionEntity
import tkd.apptkd.data.service.DataErasureService
import tkd.apptkd.ui.PracticeSessionHolder
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DataErasureServiceTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var sessionRepository: SessionRepository
    private lateinit var userSettingsRepository: UserSettingsRepository
    private lateinit var sessionHolder: PracticeSessionHolder
    private lateinit var erasureService: DataErasureService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionRepository = SessionRepository(db)
        userSettingsRepository = UserSettingsRepository(context)
        sessionHolder = PracticeSessionHolder()
        erasureService = DataErasureService(
            context = context,
            sessionRepository = sessionRepository,
            userSettingsRepository = userSettingsRepository,
            practiceSessionHolder = sessionHolder,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testDeleteAllUserDataLeavesEmptyState() = runBlocking {
        // 1. Poblamos datos en base de datos
        val session = SessionEntity(
            id = "sess-1",
            startedAtMs = 1000L,
            endedAtMs = 2000L,
            status = "COMPLETED",
            mode = "LIVE",
            strictMode = false,
            poomsaeStatus = "DOCUMENTALLY_FIXED",
            configVersion = "0.2.0-f2",
            poseModelName = "pose_landmarker_lite",
            appVersion = "1.0",
            cameraFacing = "FRONT",
            globalScore = 80.0,
            evaluableCount = 18,
            partialCount = 0,
            notEvaluableCount = 0,
            flagsJson = "{}",
        )
        db.sessionDao().insertSession(session)
        db.sessionDraftDao().saveDraft(
            SessionDraftEntity(id = 1, stateJson = "{}", lastEvaluatedIndex = 3, updatedAtMs = 1000L),
        )

        // 2. Modificamos DataStore
        userSettingsRepository.setDefaultCameraFacing(CameraFacing.BACK)
        userSettingsRepository.setStrictMode(true)

        // 3. Poblamos holder en memoria
        sessionHolder.setDraft(3)

        // 4. Creamos archivos físicos en carpetas privadas
        val snapshotsDir = File(context.filesDir, "snapshots").apply { mkdirs() }
        val testSnapshot = File(snapshotsDir, "snap1.jpg").apply { writeText("dummy-image") }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val testExport = File(exportDir, "export1.txt").apply { writeText("dummy-export") }

        assertTrue(testSnapshot.exists())
        assertTrue(testExport.exists())

        // 5. Ejecutamos el borrado total (SC-30)
        val success = erasureService.deleteAllUserData()
        assertTrue(success)

        // 6. Verificamos que Room está completamente vacío
        assertEquals(0, db.sessionDao().getSessionCount())
        assertNull(db.sessionDraftDao().getDraft())

        // 7. Verificamos que DataStore fue limpiado
        val settings = userSettingsRepository.userSettingsFlow.first()
        assertEquals(CameraFacing.FRONT, settings.defaultCameraFacing)
        assertFalse(settings.strictMode)

        // 8. Verificamos memoria limpia
        assertNull(sessionHolder.currentSummary.value)
        assertNull(sessionHolder.lastDraftMovementIndex.value)

        // 9. Verificamos que los archivos físicos fueron eliminados
        assertFalse(testSnapshot.exists())
        assertFalse(testExport.exists())
    }
}

