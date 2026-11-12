package tkd.apptkd.data.room

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import java.io.IOException

/**
 * Registro de migraciones y política de preservación de datos ante fallos (Sección 8.1).
 *
 * Se prohíbe terminantemente el uso de `fallbackToDestructiveMigration()` para evitar pérdida
 * silenciosa de datos de práctica.
 */
object DatabaseMigrations {

    const val DATABASE_NAME: String = "taegeuk_coach.db"

    /**
     * Migración de prueba de versión 1 a versión 2 (agrega columna informativa de notas técnicas).
     */
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE session ADD COLUMN notes TEXT DEFAULT NULL")
        }
    }

    val ALL_MIGRATIONS: Array<Migration> = arrayOf(
        MIGRATION_1_2,
    )

    /**
     * Respalda el archivo físico de la base de datos antes de cualquier acción de recuperación,
     * cumpliendo la directriz de la sección 8.1.
     *
     * @return Ruta absoluta del archivo de respaldo si se creó con éxito, o null en caso contrario.
     */
    fun backupDatabaseFile(context: Context, databaseName: String = DATABASE_NAME): String? {
        val dbFile = context.getDatabasePath(databaseName)
        if (!dbFile.exists()) return null
        return try {
            val backupFile = File(
                dbFile.parentFile,
                "${databaseName}.backup_${System.currentTimeMillis()}",
            )
            dbFile.copyTo(backupFile, overwrite = true)
            backupFile.absolutePath
        } catch (_: IOException) {
            null
        }
    }
}

