package tkd.apptkd.data.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Entidad de persistencia para el resultado de un movimiento individual de la sesión (Sección 12.10).
 *
 * Clave primaria compuesta por [sessionId] y [movementIndex].
 * Clave foránea referenciando a [SessionEntity] con eliminación en cascada.
 */
@Entity(
    tableName = "movement_result",
    primaryKeys = ["sessionId", "movementIndex"],
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sessionId"])],
)
data class MovementResultEntity(
    val sessionId: String,
    val movementIndex: Int,
    val outcome: String,
    val evaluability: String?,
    val category: String?,
    val score: Double?,
    val attempts: Int,
    val feedbackJson: String,
    val metricsJson: String,
    val snapshotPath: String?,
)

