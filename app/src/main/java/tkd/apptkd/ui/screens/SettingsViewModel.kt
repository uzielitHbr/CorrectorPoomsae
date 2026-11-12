package tkd.apptkd.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tkd.apptkd.camera.CameraFacing
import tkd.apptkd.data.datastore.ExigencyLevel
import tkd.apptkd.data.datastore.UserSettings
import tkd.apptkd.data.datastore.UserSettingsRepository
import tkd.apptkd.data.service.DataErasureService
import javax.inject.Inject

private const val SUBSCRIPTION_TIMEOUT_MS = 5000L

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
    private val dataErasureService: DataErasureService,
) : ViewModel() {

    val userSettings: StateFlow<UserSettings> = userSettingsRepository.userSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = UserSettings(),
        )

    fun setDefaultCameraFacing(facing: CameraFacing) {
        viewModelScope.launch {
            userSettingsRepository.setDefaultCameraFacing(facing)
        }
    }

    fun setVoiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.setVoiceEnabled(enabled)
        }
    }

    fun setExigencyLevel(level: ExigencyLevel) {
        viewModelScope.launch {
            userSettingsRepository.setExigencyLevel(level)
        }
    }

    fun setStrictMode(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.setStrictMode(enabled)
        }
    }

    fun setSaveSnapshots(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.setSaveSnapshots(enabled)
        }
    }

    fun deleteAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            dataErasureService.deleteAllUserData()
            onComplete()
        }
    }
}

