package tkd.apptkd.data.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad de persistencia para una sesión de práctica o análisis (Sección 12.10).
 *
 * Registra metadatos técnicos, puntaje global orientativo y conteos de evaluabilidad.
 * Clave primaria [id] (UUID en texto), índice por [startedAtMs] y cero datos personales (Regla R-09).
 */
@Entity(
    tableName = "session",
    indices = [Index(value = ["startedAtMs"])],
)
data class SessionEntity(
    @PrimaryKey
    val id: String,
    val startedAtMs: Long,
    val endedAtMs: Long?,
    val status: String,
    val mode: String,
    val strictMode: Boolean,
    val poomsaeStatus: String,
    val configVersion: String,
    val poseModelName: String,
    val appVersion: String,
    val cameraFacing: String,
    val globalScore: Double?,
    val evaluableCount: Int,
    val partialCount: Int,
    val notEvaluableCount: Int,
    val flagsJson: String,
)

