package com.opencontacts.app

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.opencontacts.core.model.BackupRecordSummary
import com.opencontacts.core.vault.VaultSessionManager
import com.opencontacts.domain.vaults.VaultTransferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun BackupRoute(
    onBack: () -> Unit,
    appViewModel: AppViewModel,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val settings by appViewModel.appLockSettings.collectAsStateWithLifecycle()
    val storageMessage by appViewModel.storageMessage.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val backupFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) Toast.makeText(context, "Backup folder selection cancelled", Toast.LENGTH_SHORT).show()
        else appViewModel.setBackupFolder(uri)
    }
    val exportFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) Toast.makeText(context, "Export folder selection cancelled", Toast.LENGTH_SHORT).show()
        else appViewModel.setExportFolder(uri)
    }

    SettingsScaffold(title = "Backup & Export", onBack = onBack) { modifier ->
        LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SettingsSection(title = "Storage locations", subtitle = "Use the Storage Access Framework to keep folder access safe, user-controlled, and persistent.") {
                    storageMessage?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.primary)
                        SettingsSpacer()
                    }
                    LocationCard(
                        title = "Backup location",
                        currentValue = settings.backupFolderName ?: "App storage/${settings.backupPath}",
                        chooseLabel = if (settings.backupFolderUri == null) "Choose backup folder" else "Change backup folder",
                        onChoose = { backupFolderPicker.launch(null) },
                        onReset = if (settings.backupFolderUri != null) appViewModel::resetBackupFolder else null,
                    )
                    SettingsSpacer()
                    LocationCard(
                        title = "Export location",
                        currentValue = settings.exportFolderName ?: "App storage/${settings.exportPath}",
                        chooseLabel = if (settings.exportFolderUri == null) "Choose export folder" else "Change export folder",
                        onChoose = { exportFolderPicker.launch(null) },
                        onReset = if (settings.exportFolderUri != null) appViewModel::resetExportFolder else null,
                    )
                }
            }
            item {
                SettingsSection(title = "Backup actions", subtitle = "Create encrypted local backups, stage them to cloud handoff adapters, or restore the latest available backup.") {
                    ProgressCard(progress)
                    SettingsSpacer()
                    ActionButtonRow(
                        primaryLabel = "Create backup",
                        secondaryLabel = "Restore latest",
                        onPrimary = viewModel::createBackup,
                        onSecondary = viewModel::restoreLatest,
                    )
                    SettingsSpacer()
                    ActionButtonRow(
                        primaryLabel = "Stage Google Drive",
                        secondaryLabel = "Stage OneDrive",
                        onPrimary = viewModel::stageGoogleDrive,
                        onSecondary = viewModel::stageOneDrive,
                    )
                }
            }
            item {
                SettingsSection(title = "Backup history", subtitle = "Most recent local and staged backup records for the active vault.") {
                    if (records.isEmpty()) {
                        Text("No backup records yet.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            records.forEach { record -> BackupRecordCard(record) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationCard(
    title: String,
    currentValue: String,
    chooseLabel: String,
    onChoose: () -> Unit,
    onReset: (() -> Unit)?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(currentValue, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onChoose) { Text(chooseLabel) }
            onReset?.let { TextButton(onClick = it) { Text("Reset to app storage") } }
        }
    }
}

@Composable
private fun ActionButtonRow(
    primaryLabel: String,
    secondaryLabel: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        androidx.compose.material3.Button(modifier = Modifier.weight(1f), onClick = onPrimary) { Text(primaryLabel) }
        androidx.compose.material3.OutlinedButton(modifier = Modifier.weight(1f), onClick = onSecondary) { Text(secondaryLabel) }
    }
}

@Composable
private fun ProgressCard(progress: TransferProgressUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(progress.label, style = MaterialTheme.typography.titleMedium)
            if (progress.indeterminate) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(progress = { progress.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            }
            Text(progress.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BackupRecordCard(record: BackupRecordSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(record.status, style = MaterialTheme.typography.titleMedium)
            Text("Provider: ${record.provider}")
            Text("File: ${record.filePath}")
            Text("Size: ${record.fileSizeBytes} bytes")
            Text(formatTimestamp(record.createdAt), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val vaultSessionManager: VaultSessionManager,
    private val transferRepository: VaultTransferRepository,
) : ViewModel() {
    val records: StateFlow<List<BackupRecordSummary>> = vaultSessionManager.activeVaultId
        .combine(vaultSessionManager.isLocked) { vaultId, locked -> vaultId to locked }
        .flatMapLatest { (vaultId, locked) ->
            if (vaultId == null || locked) flowOf(emptyList()) else transferRepository.observeBackupRecords(vaultId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _progress = MutableStateFlow(TransferProgressUiState.idle("Backup is idle"))
    val progress: StateFlow<TransferProgressUiState> = _progress

    private fun activeVaultId(): String? = vaultSessionManager.activeVaultId.value

    fun createBackup() = launchTransfer(
        preparing = "Preparing backup…",
        running = "Creating encrypted backup…",
        completed = "Backup completed",
    ) { transferRepository.createLocalBackup(checkNotNull(activeVaultId())) }

    fun restoreLatest() = launchTransfer(
        preparing = "Preparing restore…",
        running = "Restoring latest backup…",
        completed = "Restore completed",
    ) { transferRepository.restoreLatestLocalBackup(checkNotNull(activeVaultId())) }

    fun stageGoogleDrive() = launchTransfer(
        preparing = "Preparing Google Drive staging…",
        running = "Staging encrypted backup for Google Drive…",
        completed = "Google Drive staging completed",
    ) { transferRepository.stageLatestBackupToGoogleDrive(checkNotNull(activeVaultId())) }

    fun stageOneDrive() = launchTransfer(
        preparing = "Preparing OneDrive staging…",
        running = "Staging encrypted backup for OneDrive…",
        completed = "OneDrive staging completed",
    ) { transferRepository.stageLatestBackupToOneDrive(checkNotNull(activeVaultId())) }

    private fun launchTransfer(
        preparing: String,
        running: String,
        completed: String,
        block: suspend () -> Any,
    ) {
        val vaultId = activeVaultId() ?: return
        viewModelScope.launch {
            runCatching {
                _progress.value = TransferProgressUiState(indeterminate = true, progress = 0f, label = preparing, message = "Preparing…")
                delay(120)
                _progress.value = TransferProgressUiState(indeterminate = false, progress = 0.35f, label = running, message = "Working in the background-safe flow…")
                val result = block()
                _progress.value = TransferProgressUiState(indeterminate = false, progress = 1f, label = completed, message = result.toString())
            }.onFailure { error ->
                _progress.value = TransferProgressUiState.failed(error.message ?: "Backup flow failed")
            }
        }
    }
}

data class TransferProgressUiState(
    val indeterminate: Boolean,
    val progress: Float,
    val label: String,
    val message: String,
) {
    companion object {
        fun idle(message: String) = TransferProgressUiState(indeterminate = false, progress = 0f, label = "Idle", message = message)
        fun failed(message: String) = TransferProgressUiState(indeterminate = false, progress = 1f, label = "Failed", message = message)
    }
}

private fun formatTimestamp(value: Long): String = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(value))
