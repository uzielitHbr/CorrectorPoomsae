package tkd.apptkd.data.repository

import kotlinx.coroutines.flow.Flow
import tkd.apptkd.data.room.AppDatabase
import tkd.apptkd.data.room.SessionDraftEntity
import tkd.apptkd.data.room.SessionEntity
import tkd.apptkd.data.room.SessionEntityMapper
import tkd.apptkd.data.room.SessionWithMovements
import tkd.apptkd.ui.SessionSummaryData
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio central de persistencia de sesiones de práctica e historial (Sección 12.10).
 */
@Singleton
class SessionRepository @Inject constructor(
    private val database: AppDatabase,
) {
    private val sessionDao = database.sessionDao()
    private val sessionDraftDao = database.sessionDraftDao()

    val allSessionsFlow: Flow<List<SessionEntity>> = sessionDao.getAllSessionsFlow()

    suspend fun saveCompletedSession(
        summaryData: SessionSummaryData,
        cameraFacing: String = "FRONT",
        strictMode: Boolean = false,
        snapshotPaths: Map<Int, String> = emptyMap(),
    ): String {
        val sessionId = UUID.randomUUID().toString()
        val (sessionEntity, movementEntities) = SessionEntityMapper.toEntities(
            summaryData = summaryData,
            params = tkd.apptkd.data.room.SessionPersistParams(
                sessionId = sessionId,
                status = "COMPLETED",
                cameraFacing = cameraFacing,
                strictMode = strictMode,
                snapshotPaths = snapshotPaths,
            ),
        )
        sessionDao.insertSessionWithMovements(sessionEntity, movementEntities)
        sessionDraftDao.clearDraft()
        return sessionId
    }

    suspend fun saveIncompleteSession(
        summaryData: SessionSummaryData,
        cameraFacing: String = "FRONT",
        strictMode: Boolean = false,
        snapshotPaths: Map<Int, String> = emptyMap(),
    ): String {
        val sessionId = UUID.randomUUID().toString()
        val (sessionEntity, movementEntities) = SessionEntityMapper.toEntities(
            summaryData = summaryData,
            params = tkd.apptkd.data.room.SessionPersistParams(
                sessionId = sessionId,
                status = "INCOMPLETE",
                cameraFacing = cameraFacing,
                strictMode = strictMode,
                snapshotPaths = snapshotPaths,
            ),
        )
        sessionDao.insertSessionWithMovements(sessionEntity, movementEntities)
        return sessionId
    }

    suspend fun getSessionWithMovements(sessionId: String): SessionWithMovements? =
        sessionDao.getSessionWithMovements(sessionId)

    suspend fun getSessionById(sessionId: String): SessionEntity? =
        sessionDao.getSessionById(sessionId)

    suspend fun deleteSession(sessionId: String) {
        sessionDao.deleteSessionById(sessionId)
    }

    suspend fun saveDraft(movementIndex: Int, stateJson: String) {
        val draft = SessionDraftEntity(
            id = SessionDraftEntity.DRAFT_PRIMARY_KEY,
            stateJson = stateJson,
            lastEvaluatedIndex = movementIndex,
            updatedAtMs = System.currentTimeMillis(),
        )
        sessionDraftDao.saveDraft(draft)
    }

    suspend fun getDraft(): SessionDraftEntity? = sessionDraftDao.getDraft()

    fun getDraftFlow(): Flow<SessionDraftEntity?> = sessionDraftDao.getDraftFlow()

    suspend fun clearDraft() {
        sessionDraftDao.clearDraft()
    }

    suspend fun deleteAll() {
        sessionDao.deleteAllSessions()
        sessionDraftDao.deleteAllDrafts()
    }
}

