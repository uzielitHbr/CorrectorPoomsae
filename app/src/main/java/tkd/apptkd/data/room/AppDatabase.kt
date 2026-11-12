package tkd.apptkd.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos Room principal de Taegeuk Coach (Sección 12.10).
 *
 * Contratos:
 * - Entidades: [SessionEntity], [MovementResultEntity], [SessionDraftEntity].
 * - Exportación de esquemas JSON habilitada (Sección 8.1 / Ampliación 2.0).
 * - Prohibido el borrado destructivo automático en migraciones.
 */
@Database(
    entities = [
        SessionEntity::class,
        MovementResultEntity::class,
        SessionDraftEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun sessionDraftDao(): SessionDraftDao

    companion object {
        fun buildDatabase(
            context: Context,
            databaseName: String = DatabaseMigrations.DATABASE_NAME,
        ): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                databaseName,
            )
                .addMigrations(DatabaseMigrations.MIGRATION_1_2)
                .build()
    }
}

