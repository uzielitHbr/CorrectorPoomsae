package tkd.apptkd.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO para la gestión y consulta de sesiones y resultados por movimiento (Sección 12.10).
 */
@Dao
abstract class SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSession(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMovementResults(results: List<MovementResultEntity>)

    /**
     * Guarda atómicamente la sesión y todos sus resultados en una sola transacción.
     */
    @Transaction
    open suspend fun insertSessionWithMovements(
        session: SessionEntity,
        movements: List<MovementResultEntity>,
    ) {
        insertSession(session)
        insertMovementResults(movements)
    }

    @Query("SELECT * FROM session ORDER BY startedAtMs DESC")
    abstract fun getAllSessionsFlow(): Flow<List<SessionEntity>>

    @Transaction
    @Query("SELECT * FROM session WHERE id = :sessionId LIMIT 1")
    abstract suspend fun getSessionWithMovements(sessionId: String): SessionWithMovements?

    @Query("SELECT * FROM session WHERE id = :sessionId LIMIT 1")
    abstract suspend fun getSessionById(sessionId: String): SessionEntity?

    @Query("SELECT * FROM movement_result WHERE sessionId = :sessionId ORDER BY movementIndex ASC")
    abstract suspend fun getMovementsForSession(sessionId: String): List<MovementResultEntity>

    @Query("DELETE FROM session WHERE id = :sessionId")
    abstract suspend fun deleteSessionById(sessionId: String)

    @Query("DELETE FROM session")
    abstract suspend fun deleteAllSessions()

    @Query("SELECT COUNT(*) FROM session")
    abstract suspend fun getSessionCount(): Int
}

