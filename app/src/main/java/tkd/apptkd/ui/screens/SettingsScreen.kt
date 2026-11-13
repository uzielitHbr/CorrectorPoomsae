package tkd.apptkd.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tkd.apptkd.R
import tkd.apptkd.camera.CameraFacing
import tkd.apptkd.data.datastore.ExigencyLevel
import tkd.apptkd.data.datastore.UserSettings
import tkd.apptkd.ui.components.ConfirmDialog
import tkd.apptkd.ui.components.DialogAction
import tkd.apptkd.ui.components.SettingsActionRow
import tkd.apptkd.ui.components.SettingsSelectionRow
import tkd.apptkd.ui.components.SettingsSwitchRow
import tkd.apptkd.ui.theme.TaegeukCoachTheme

private const val SECTION_CORNER_RADIUS_DP = 16

/**
 * Pantalla de Ajustes de la aplicación (Sección 14.8 y RF-12).
 *
 * Configuración de cámara predeterminada, voz, exigencia, modo estricto, capturas y borrado total.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.userSettings.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    val backCd = stringResource(id = R.string.cd_navigate_back)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.screen_settings)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = backCd },
                    ) {
                        Text(text = "←", style = MaterialTheme.typography.titleMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SettingsContent(
                settings = settings,
                actions = SettingsActionHandlers(
                    onCameraFacingToggle = {
                        val next = if (settings.defaultCameraFacing == CameraFacing.FRONT) {
                            CameraFacing.BACK
                        } else {
                            CameraFacing.FRONT
                        }
                        viewModel.setDefaultCameraFacing(next)
                    },
                    onVoiceToggle = { viewModel.setVoiceEnabled(it) },
                    onExigencyCycle = {
                        val next = when (settings.exigencyLevel) {
                            ExigencyLevel.RELAXED -> ExigencyLevel.NORMAL
                            ExigencyLevel.NORMAL -> ExigencyLevel.STRICT
                            ExigencyLevel.STRICT -> ExigencyLevel.RELAXED
                        }
                        viewModel.setExigencyLevel(next)
                    },
                    onStrictModeToggle = { viewModel.setStrictMode(it) },
                    onSaveSnapshotsToggle = { viewModel.setSaveSnapshots(it) },
                    onDeleteAllClick = { showDeleteConfirmDialog = true },
                    onPrivacyClick = { showPrivacyDialog = true },
                    onLicensesClick = { showLicensesDialog = true },
                ),
            )
        }
    }

    DeleteConfirmDialog(
        show = showDeleteConfirmDialog,
        onConfirm = {
            showDeleteConfirmDialog = false
            viewModel.deleteAllData(onNavigateBack)
        },
        onDismiss = { showDeleteConfirmDialog = false },
    )
    PrivacyPolicyDialog(
        show = showPrivacyDialog,
        onDismiss = { showPrivacyDialog = false },
    )
    ThirdPartyLicensesDialog(
        show = showLicensesDialog,
        onDismiss = { showLicensesDialog = false },
    )
}

@Composable
private fun DeleteConfirmDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return
    ConfirmDialog(
        title = stringResource(id = R.string.settings_delete_all_confirm_title),
        message = stringResource(id = R.string.MSG_DELETE_ALL),
        actions = listOf(
            DialogAction(
                title = stringResource(id = R.string.settings_delete_all_confirm_btn),
                onClick = onConfirm,
                isPrimary = true,
                isDestructive = true,
            ),
            DialogAction(
                title = stringResource(id = R.string.btn_discard),
                onClick = onDismiss,
            ),
        ),
        onDismissRequest = onDismiss,
    )
}

@Composable
private fun PrivacyPolicyDialog(
    show: Boolean,
    onDismiss: () -> Unit,
) {
    if (!show) return
    ConfirmDialog(
        title = stringResource(id = R.string.settings_privacy_full_title),
        message = stringResource(id = R.string.settings_privacy_full_content),
        actions = listOf(
            DialogAction(
                title = stringResource(id = R.string.btn_accept),
                onClick = onDismiss,
                isPrimary = true,
            ),
        ),
        onDismissRequest = onDismiss,
    )
}

@Composable
private fun ThirdPartyLicensesDialog(
    show: Boolean,
    onDismiss: () -> Unit,
) {
    if (!show) return
    ConfirmDialog(
        title = stringResource(id = R.string.settings_licenses_dialog_title),
        message = stringResource(id = R.string.settings_licenses_dialog_content),
        actions = listOf(
            DialogAction(
                title = stringResource(id = R.string.btn_accept),
                onClick = onDismiss,
                isPrimary = true,
            ),
        ),
        onDismissRequest = onDismiss,
    )
}

data class SettingsActionHandlers(
    val onCameraFacingToggle: () -> Unit,
    val onVoiceToggle: (Boolean) -> Unit,
    val onExigencyCycle: () -> Unit,
    val onStrictModeToggle: (Boolean) -> Unit,
    val onSaveSnapshotsToggle: (Boolean) -> Unit,
    val onDeleteAllClick: () -> Unit,
    val onPrivacyClick: () -> Unit = {},
    val onLicensesClick: () -> Unit = {},
)

@Composable
internal fun SettingsContent(
    settings: UserSettings,
    actions: SettingsActionHandlers,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        PreferencesSection(settings = settings, actions = actions)
        Spacer(modifier = Modifier.height(16.dp))
        AboutSection(actions = actions)
        Spacer(modifier = Modifier.height(16.dp))
        DangerZoneSection(onDeleteAllClick = actions.onDeleteAllClick)
    }
}

@Composable
private fun PreferencesSection(
    settings: UserSettings,
    actions: SettingsActionHandlers,
) {
    SettingsSection(title = stringResource(id = R.string.screen_settings)) {
        val cameraLabel = if (settings.defaultCameraFacing == CameraFacing.FRONT) {
            stringResource(id = R.string.settings_camera_front)
        } else {
            stringResource(id = R.string.settings_camera_back)
        }
        SettingsSelectionRow(
            title = stringResource(id = R.string.settings_camera_facing_title),
            description = stringResource(id = R.string.settings_camera_facing_desc),
            selectedOption = cameraLabel,
            onClick = actions.onCameraFacingToggle,
        )
        HorizontalDivider()
        SettingsSwitchRow(
            title = stringResource(id = R.string.settings_voice_title),
            description = stringResource(id = R.string.settings_voice_desc),
            checked = settings.voiceEnabled,
            onCheckedChange = actions.onVoiceToggle,
        )
        HorizontalDivider()
        val exigencyLabel = when (settings.exigencyLevel) {
            ExigencyLevel.RELAXED -> stringResource(id = R.string.settings_exigency_relaxed)
            ExigencyLevel.NORMAL -> stringResource(id = R.string.settings_exigency_normal)
            ExigencyLevel.STRICT -> stringResource(id = R.string.settings_exigency_strict)
        }
        SettingsSelectionRow(
            title = stringResource(id = R.string.settings_exigency_title),
            description = stringResource(id = R.string.settings_exigency_desc),
            selectedOption = exigencyLabel,
            onClick = actions.onExigencyCycle,
        )
        HorizontalDivider()
        SettingsSwitchRow(
            title = stringResource(id = R.string.settings_strict_mode_title),
            description = stringResource(id = R.string.settings_strict_mode_desc),
            checked = settings.strictMode,
            onCheckedChange = actions.onStrictModeToggle,
        )
        HorizontalDivider()
        SettingsSwitchRow(
            title = stringResource(id = R.string.settings_save_snapshots_title),
            description = stringResource(id = R.string.settings_save_snapshots_desc),
            checked = settings.saveSnapshots,
            onCheckedChange = actions.onSaveSnapshotsToggle,
        )
    }
}

@Composable
private fun AboutSection(actions: SettingsActionHandlers) {
    SettingsSection(title = stringResource(id = R.string.settings_about_title)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.settings_about_version, "1.0.0", "DOCUMENTALLY_FIXED"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.settings_about_privacy_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(id = R.string.settings_about_privacy_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider()
        SettingsActionRow(
            title = stringResource(id = R.string.settings_privacy_full_title),
            description = stringResource(id = R.string.settings_privacy_full_btn),
            onClick = actions.onPrivacyClick,
        )
        HorizontalDivider()
        SettingsActionRow(
            title = stringResource(id = R.string.settings_licenses_title),
            description = stringResource(id = R.string.settings_licenses_desc),
            onClick = actions.onLicensesClick,
        )
    }
}

@Composable
private fun DangerZoneSection(onDeleteAllClick: () -> Unit) {
    SettingsSection(title = stringResource(id = R.string.settings_danger_zone)) {
        SettingsActionRow(
            title = stringResource(id = R.string.settings_delete_all_title),
            description = stringResource(id = R.string.settings_delete_all_desc),
            onClick = onDeleteAllClick,
            isDestructive = true,
        )
    }
}

@Composable
internal fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        )
        Card(
            shape = RoundedCornerShape(SECTION_CORNER_RADIUS_DP.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            content()
        }
    }
}

@Preview(name = "Settings - Light", showBackground = true)
@Preview(name = "Settings - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun SettingsPreview() {
    TaegeukCoachTheme {
        SettingsContent(
            settings = UserSettings(),
            actions = SettingsActionHandlers(
                onCameraFacingToggle = {},
                onVoiceToggle = {},
                onExigencyCycle = {},
                onStrictModeToggle = {},
                onSaveSnapshotsToggle = {},
                onDeleteAllClick = {},
            ),
        )
    }
}
