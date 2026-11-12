package tkd.apptkd.data.room

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Modelo relacional 1:N entre [SessionEntity] y sus [MovementResultEntity] evaluados (Sección 12.10).
 */
data class SessionWithMovements(
    @Embedded
    val session: SessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val movements: List<MovementResultEntity>,
)

