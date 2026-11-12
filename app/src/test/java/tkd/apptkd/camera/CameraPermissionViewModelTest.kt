package tkd.apptkd.camera

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CameraPermissionViewModelTest {
    private lateinit var viewModel: CameraPermissionViewModel

    @Before
    fun setUp() {
        viewModel = CameraPermissionViewModel()
    }

    @Test
    fun `initial state is NotDetermined`() {
        assertEquals(CameraPermissionState.NotDetermined, viewModel.state.value)
    }

    @Test
    fun `onCheckInitialPermission transitions to Granted when permission is present`() {
        viewModel.onCheckInitialPermission(hasPermission = true, shouldShowRationale = false)
        assertEquals(CameraPermissionState.Granted, viewModel.state.value)
    }

    @Test
    fun `onCheckInitialPermission transitions to RationaleRequired when rationale is true`() {
        viewModel.onCheckInitialPermission(hasPermission = false, shouldShowRationale = true)
        assertEquals(CameraPermissionState.RationaleRequired, viewModel.state.value)
    }

    @Test
    fun `onCheckInitialPermission stays NotDetermined when permission is false and no rationale`() {
        viewModel.onCheckInitialPermission(hasPermission = false, shouldShowRationale = false)
        assertEquals(CameraPermissionState.NotDetermined, viewModel.state.value)
    }

    @Test
    fun `onUserRequestsPermission transitions to Requesting`() {
        viewModel.onUserRequestsPermission()
        assertEquals(CameraPermissionState.Requesting, viewModel.state.value)
    }

    @Test
    fun `onPermissionResult with granted true transitions to Granted`() {
        viewModel.onPermissionResult(isGranted = true, shouldShowRationale = false)
        assertEquals(CameraPermissionState.Granted, viewModel.state.value)
    }

    @Test
    fun `onPermissionResult with granted false and rationale true transitions to RationaleRequired`() {
        viewModel.onPermissionResult(isGranted = false, shouldShowRationale = true)
        assertEquals(CameraPermissionState.RationaleRequired, viewModel.state.value)
    }

    @Test
    fun `onPermissionResult with granted false and rationale false transitions to PermanentlyDenied`() {
        viewModel.onPermissionResult(isGranted = false, shouldShowRationale = false)
        assertEquals(CameraPermissionState.PermanentlyDenied, viewModel.state.value)
    }

    @Test
    fun `onDismissRationale transitions to Denied`() {
        viewModel.onDismissRationale()
        assertEquals(CameraPermissionState.Denied, viewModel.state.value)
    }
}

