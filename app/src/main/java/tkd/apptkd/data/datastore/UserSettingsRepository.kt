package tkd.apptkd.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import tkd.apptkd.camera.CameraFacing
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

/**
 * Repositorio de configuración y preferencias del usuario respaldado por Jetpack DataStore (Sección 12.10).
 */
@Singleton
class UserSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore: DataStore<Preferences> = context.userSettingsDataStore

    companion object {
        val KEY_DEFAULT_CAMERA_FACING = stringPreferencesKey("default_camera_facing")
        val KEY_VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val KEY_EXIGENCY_LEVEL = stringPreferencesKey("exigency_level")
        val KEY_STRICT_MODE = booleanPreferencesKey("strict_mode")
        val KEY_SAVE_SNAPSHOTS = booleanPreferencesKey("save_snapshots")
    }

    val userSettingsFlow: Flow<UserSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val cameraFacingName = preferences[KEY_DEFAULT_CAMERA_FACING] ?: CameraFacing.FRONT.name
            val cameraFacing = runCatching { CameraFacing.valueOf(cameraFacingName) }.getOrDefault(CameraFacing.FRONT)
            val voiceEnabled = preferences[KEY_VOICE_ENABLED] ?: true
            val exigencyName = preferences[KEY_EXIGENCY_LEVEL] ?: ExigencyLevel.NORMAL.name
            val exigencyLevel = runCatching { ExigencyLevel.valueOf(exigencyName) }.getOrDefault(ExigencyLevel.NORMAL)
            val strictMode = preferences[KEY_STRICT_MODE] ?: false
            val saveSnapshots = preferences[KEY_SAVE_SNAPSHOTS] ?: false

            UserSettings(
                defaultCameraFacing = cameraFacing,
                voiceEnabled = voiceEnabled,
                exigencyLevel = exigencyLevel,
                strictMode = strictMode,
                saveSnapshots = saveSnapshots,
            )
        }

    suspend fun setDefaultCameraFacing(facing: CameraFacing) {
        dataStore.edit { it[KEY_DEFAULT_CAMERA_FACING] = facing.name }
    }

    suspend fun setVoiceEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_VOICE_ENABLED] = enabled }
    }

    suspend fun setExigencyLevel(level: ExigencyLevel) {
        dataStore.edit { it[KEY_EXIGENCY_LEVEL] = level.name }
    }

    suspend fun setStrictMode(enabled: Boolean) {
        dataStore.edit { it[KEY_STRICT_MODE] = enabled }
    }

    suspend fun setSaveSnapshots(enabled: Boolean) {
        dataStore.edit { it[KEY_SAVE_SNAPSHOTS] = enabled }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}

