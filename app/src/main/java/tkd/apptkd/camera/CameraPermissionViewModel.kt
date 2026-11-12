package tkd.apptkd.camera

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estados del flujo de permisos de cámara en cumplimiento de la Fase F4 (Sección 12.8).
 */
sealed interface CameraPermissionState {
    /** Estado inicial antes de consultar el sistema. */
    data object NotDetermined : CameraPermissionState

    /** El usuario necesita ver la explicación previa (MSG_PERM_RATIONALE). */
    data object RationaleRequired : CameraPermissionState

    /** Solicitud activa al sistema operativo en curso. */
    data object Requesting : CameraPermissionState

    /** Permiso otorgado por el usuario. */
    data object Granted : CameraPermissionState

    /** Permiso denegado por el usuario pero reintentable. */
    data object Denied : CameraPermissionState

    /** Permiso denegado permanentemente ("No volver a preguntar"), requiere ir a ajustes (MSG_PERM_SETTINGS). */
    data object PermanentlyDenied : CameraPermissionState
}

/**
 * ViewModel que gestiona la máquina de estados de permisos de cámara de forma aislada y comprobable.
 */
class CameraPermissionViewModel : ViewModel() {
    private val _state = MutableStateFlow<CameraPermissionState>(CameraPermissionState.NotDetermined)
    val state: StateFlow<CameraPermissionState> = _state.asStateFlow()

    /** Evalúa el estado inicial al entrar a la pantalla de cámara. */
    fun onCheckInitialPermission(hasPermission: Boolean, shouldShowRationale: Boolean) {
        _state.value = when {
            hasPermission -> CameraPermissionState.Granted
            shouldShowRationale -> CameraPermissionState.RationaleRequired
            else -> CameraPermissionState.NotDetermined
        }
    }

    /** El usuario pulsa para iniciar la solicitud formal de permiso. */
    fun onUserRequestsPermission() {
        _state.value = CameraPermissionState.Requesting
    }

    /** Procesa el resultado devuelto por ActivityResultContracts.RequestPermission(). */
    fun onPermissionResult(isGranted: Boolean, shouldShowRationale: Boolean) {
        _state.value = when {
            isGranted -> CameraPermissionState.Granted
            shouldShowRationale -> CameraPermissionState.RationaleRequired
            else -> CameraPermissionState.PermanentlyDenied
        }
    }

    /** El usuario descarta la explicación sin solicitar el permiso. */
    fun onDismissRationale() {
        _state.value = CameraPermissionState.Denied
    }
}

