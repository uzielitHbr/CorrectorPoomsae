package tkd.apptkd.data.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import tkd.apptkd.data.datastore.UserSettingsRepository
import tkd.apptkd.data.repository.SessionRepository
import tkd.apptkd.ui.PracticeSessionHolder
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio encargado del borrado total e irreversible de datos de la aplicación (Sección 12.5, SC-30).
 *
 * Elimina:
 * 1. Base de datos Room (todas las sesiones y resultados por movimiento).
 * 2. Borradores de sesiones activas o incompletas.
 * 3. Preferencias de usuario en DataStore.
 * 4. Capturas privadas de momentos clave y archivos de exportación en caché.
 * 5. Estados retenidos en memoria de la sesión activa.
 */
@Singleton
class DataErasureService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val practiceSessionHolder: PracticeSessionHolder,
) {

    suspend fun deleteAllUserData(): Boolean {
        return try {
            // 1 & 2. Limpieza de Room (sesiones, resultados y borradores)
            sessionRepository.deleteAll()

            // 3. Limpieza de DataStore
            userSettingsRepository.clearAll()

            // 4. Limpieza de memoria en holder
            practiceSessionHolder.clear()
            practiceSessionHolder.clearDraft()

            // 5. Limpieza de capturas privadas y archivos exportados
            deleteDirectoryContents(File(context.filesDir, "snapshots"))
            deleteDirectoryContents(File(context.cacheDir, "exports"))

            true
        } catch (_: Exception) {
            false
        }
    }

    private fun deleteDirectoryContents(directory: File) {
        if (!directory.exists()) return
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                deleteDirectoryContents(file)
            }
            file.delete()
        }
    }
}

