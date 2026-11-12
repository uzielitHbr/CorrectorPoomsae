package tkd.apptkd.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tkd.apptkd.data.room.AppDatabase
import tkd.apptkd.data.room.MovementResultEntity
import tkd.apptkd.data.room.SessionDraftEntity
import tkd.apptkd.data.room.SessionEntity
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class RoomDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertSessionAndRetrieveFlow() = runBlocking {
        val session = createTestSession("session-1")
        db.sessionDao().insertSession(session)

        val sessions = db.sessionDao().getAllSessionsFlow().first()
        assertEquals(1, sessions.size)
        assertEquals("session-1", sessions[0].id)
        assertEquals(85.0, sessions[0].globalScore)
    }

    @Test
    fun testInsertSessionWithMovementsAndCascadeDelete() = runBlocking {
        val session = createTestSession("session-cascade")
        val movements = listOf(
            createTestMovement("session-cascade", 1),
            createTestMovement("session-cascade", 2),
        )

        db.sessionDao().insertSessionWithMovements(session, movements)

        val sessionWithMovements = db.sessionDao().getSessionWithMovements("session-cascade")
        assertNotNull(sessionWithMovements)
        assertEquals(2, sessionWithMovements?.movements?.size)

        // Verificamos borrado en cascada
        db.sessionDao().deleteSessionById("session-cascade")

        val deletedSession = db.sessionDao().getSessionWithMovements("session-cascade")
        assertNull(deletedSession)

        val remainingMovements = db.sessionDao().getMovementsForSession("session-cascade")
        assertTrue(remainingMovements.isEmpty())
    }

    @Test
    fun testSessionDraftLifecycle() = runBlocking {
        val draft1 = SessionDraftEntity(
            id = 1,
            stateJson = "{\"test\": 1}",
            lastEvaluatedIndex = 5,
            updatedAtMs = 1000L,
        )
        db.sessionDraftDao().saveDraft(draft1)

        val retrieved1 = db.sessionDraftDao().getDraft()
        assertNotNull(retrieved1)
        assertEquals(5, retrieved1?.lastEvaluatedIndex)

        // Se reemplaza en la misma fila (id=1)
        val draft2 = SessionDraftEntity(
            id = 1,
            stateJson = "{\"test\": 2}",
            lastEvaluatedIndex = 6,
            updatedAtMs = 2000L,
        )
        db.sessionDraftDao().saveDraft(draft2)

        val retrieved2 = db.sessionDraftDao().getDraft()
        assertEquals(6, retrieved2?.lastEvaluatedIndex)

        // Borrado
        db.sessionDraftDao().clearDraft()
        assertNull(db.sessionDraftDao().getDraft())
    }

    @Test
    fun testDeleteAllWipesData() = runBlocking {
        db.sessionDao().insertSession(createTestSession("s1"))
        db.sessionDao().insertMovementResults(listOf(createTestMovement("s1", 1)))
        db.sessionDraftDao().saveDraft(
            SessionDraftEntity(id = 1, stateJson = "{}", lastEvaluatedIndex = 1, updatedAtMs = 0L),
        )

        assertEquals(1, db.sessionDao().getSessionCount())
        assertNotNull(db.sessionDraftDao().getDraft())

        db.sessionDao().deleteAllSessions()
        db.sessionDraftDao().deleteAllDrafts()

        assertEquals(0, db.sessionDao().getSessionCount())
        assertNull(db.sessionDraftDao().getDraft())
    }

    private fun createTestSession(id: String): SessionEntity =
        SessionEntity(
            id = id,
            startedAtMs = System.currentTimeMillis(),
            endedAtMs = System.currentTimeMillis() + 60000L,
            status = "COMPLETED",
            mode = "LIVE",
            strictMode = false,
            poomsaeStatus = "DOCUMENTALLY_FIXED",
            configVersion = "0.2.0-f2",
            poseModelName = "pose_landmarker_lite",
            appVersion = "1.0",
            cameraFacing = "FRONT",
            globalScore = 85.0,
            evaluableCount = 18,
            partialCount = 0,
            notEvaluableCount = 0,
            flagsJson = "{\"schemaVersion\":1,\"flags\":[]}",
        )

    private fun createTestMovement(sessionId: String, index: Int): MovementResultEntity =
        MovementResultEntity(
            sessionId = sessionId,
            movementIndex = index,
            outcome = "EVALUATED",
            evaluability = "EVALUABLE",
            category = "CORRECT",
            score = 90.0,
            attempts = 1,
            feedbackJson = "{\"schemaVersion\":1,\"items\":[]}",
            metricsJson = "{\"schemaVersion\":1,\"metrics\":[]}",
            snapshotPath = null,
        )
}

