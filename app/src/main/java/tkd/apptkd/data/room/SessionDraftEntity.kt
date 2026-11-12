package tkd.apptkd.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de persistencia para el borrador de una sesión interrumpida (Sección 12.10 y SC-18).
 *
 * Mantiene una sola fila (clave primaria fija [id] = 1), que se actualiza reactivamente tras cada
 * movimiento evaluado y se elimina una vez que la sesión concluye o es descartada.
 */
@Entity(tableName = "session_draft")
data class SessionDraftEntity(
    @PrimaryKey
    val id: Int = DRAFT_PRIMARY_KEY,
    val stateJson: String,
    val lastEvaluatedIndex: Int,
    val updatedAtMs: Long,
    val schemaVersion: Int = CURRENT_DRAFT_SCHEMA_VERSION,
) {
    companion object {
        const val DRAFT_PRIMARY_KEY: Int = 1
        const val CURRENT_DRAFT_SCHEMA_VERSION: Int = 1
    }
}

