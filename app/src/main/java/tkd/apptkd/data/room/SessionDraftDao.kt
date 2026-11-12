package tkd.apptkd.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO para persistir y consultar el borrador de sesión interrumpida (Sección 12.10 y SC-18).
 */
@Dao
interface SessionDraftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: SessionDraftEntity)

    @Query("SELECT * FROM session_draft WHERE id = 1 LIMIT 1")
    suspend fun getDraft(): SessionDraftEntity?

    @Query("SELECT * FROM session_draft WHERE id = 1 LIMIT 1")
    fun getDraftFlow(): Flow<SessionDraftEntity?>

    @Query("DELETE FROM session_draft WHERE id = 1")
    suspend fun clearDraft()

    @Query("DELETE FROM session_draft")
    suspend fun deleteAllDrafts()
}

