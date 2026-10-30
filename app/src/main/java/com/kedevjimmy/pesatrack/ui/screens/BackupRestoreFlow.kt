package com.kedevjimmy.pesatrack.ui.screens

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kedevjimmy.pesatrack.security.AppLockManager
import com.kedevjimmy.pesatrack.security.BackupRecoveryStatus
import com.kedevjimmy.pesatrack.security.PesaBackupManager
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The entry remains a bottom sheet, but each sensitive job has its own screen.
 * This flow never handles plaintext backup contents or changes the portable format.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val manager = remember(context) { PesaBackupManager(context) }
    val appLock = remember(context) { AppLockManager(context) }
    val state: BackupRestoreStateViewModel = viewModel()
    var page by state.page
    var restoreStep by state.restoreStep
    var createPassphrase by state.createPassphrase
    var confirmPassphrase by state.confirmPassphrase
    var pendingCreatePassphrase by state.pendingCreatePassphrase
    var restorePassphrase by state.restorePassphrase
    var appPin by state.appPin
    var selectedUri by state.selectedUri
    var approvedFile by state.approvedFile
    var created by state.created
    var createdFileName by state.createdFileName
    var createdProvider by state.createdProvider
    var selectedFileName by state.selectedFileName
    var acknowledgedReplacement by state.acknowledgedReplacement
    var acknowledgedNoDeviceLock by state.acknowledgedNoDeviceLock
    var busy by state.busy
    var awaitingCredential by state.awaitingCredential
    var credentialFallbackLaunched by state.credentialFallbackLaunched
    var error by state.error

    LaunchedEffect(activity) {
        if (activity != null) state.observeActivity(activity)
    }

    fun clearSensitiveInputs() {
        state.clearSecrets()
        created = null
    }

    fun leaveFlow() {
        if (!busy && !awaitingCredential) {
            clearSensitiveInputs()
            restoreStep = RestoreStep.SELECT
            page = BackupPage.HOME
        }
    }

    fun finishRestore() {
        val uri = selectedUri ?: return
        val passphrase = restorePassphrase
        val approved = approvedFile ?: return
        if (busy || !state.claimRestore()) return
        awaitingCredential = false
        error = null
        state.viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    manager.restoreApprovedFile(uri, passphrase, approved)
                }
            }.onSuccess {
                clearSensitiveInputs()
                restoreStep = RestoreStep.COMPLETE
                state.scheduleLedgerReload()
            }.onFailure {
                error = it.message ?: "Could not restore this backup. Your existing data was not confirmed replaced."
                if (BackupRecoveryStatus.failure != null) state.scheduleLedgerReload()
            }
            state.finishRestoreJob(keepBlocked = BackupRecoveryStatus.failure != null)
        }
    }

    val credentialLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        credentialFallbackLaunched = false
        if (result.resultCode == Activity.RESULT_OK) {
            finishRestore()
        } else {
            awaitingCredential = false
            error = "Phone unlock cancelled. Nothing was restored."
        }
    }

    fun requestPhoneCredential(keyguard: KeyguardManager) {
        @Suppress("DEPRECATION")
        val intent = keyguard.createConfirmDeviceCredentialIntent(
            "Confirm PesaTrack restore",
            "Unlock your phone before replacing its local money data."
        )
        if (intent != null) {
            credentialFallbackLaunched = true
            credentialLauncher.launch(intent)
        } else {
            awaitingCredential = false
            error = "Phone confirmation is not available. No data was restored."
        }
    }

    fun authorizeRestore() {
        if (!BackupFlowRules.canRestore(approvedFile != null, acknowledgedReplacement, busy) || awaitingCredential) return
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguard?.isDeviceSecure == true && activity != null) {
            awaitingCredential = true
            error = null
            val canUseBiometric = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                activity.getSystemService(BiometricManager::class.java)
                    ?.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
            if (canUseBiometric && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val prompt = BiometricPrompt.Builder(activity)
                    .setTitle("Confirm PesaTrack restore")
                    .setSubtitle("Verify it's you before replacing local money data.")
                    .setNegativeButton("Use phone lock", activity.mainExecutor) { _, _ ->
                        requestPhoneCredential(keyguard)
                    }
                    .build()
                prompt.authenticate(CancellationSignal(), activity.mainExecutor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            finishRestore()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            if (!credentialFallbackLaunched) {
                                awaitingCredential = false
                                error = "Authentication cancelled. Nothing was restored."
                            }
                        }
                    })
            } else {
                requestPhoneCredential(keyguard)
            }
        } else if (appLock.isEnabled()) {
            appPin = ""
            restoreStep = RestoreStep.APP_PIN
        } else {
            acknowledgedNoDeviceLock = false
            restoreStep = RestoreStep.NO_DEVICE_LOCK
        }
    }

    val createPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val passphrase = pendingCreatePassphrase
        pendingCreatePassphrase = ""
        if (uri != null && passphrase.isNotEmpty()) {
            if (!state.claimBackup()) {
                error = "Another backup operation is already running. Please wait and try again."
            } else {
                error = null
                state.viewModelScope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val summary = manager.exportEncrypted(uri, passphrase)
                            val reread = manager.previewEncrypted(uri, passphrase)
                            check(reread.rowCount == summary.rowCount &&
                                reread.statementRows == summary.statementRows) {
                                "The saved file could not be verified. Please retry with another location."
                            }
                            summary
                        }
                    }.onSuccess {
                        created = it
                        createdFileName = backupDisplayName(context.applicationContext, uri)
                        createdProvider = backupProviderName(context.applicationContext, uri)
                        createPassphrase = ""
                        confirmPassphrase = ""
                    }.onFailure {
                        error = "Backup creation or verification failed. If a file was saved, remove it before retrying. " +
                            (it.message ?: "")
                    }
                    state.finishRestoreJob()
                }
            }
        } else if (uri != null) {
            error = "Save was interrupted. The selected file may be empty. Remove it before retrying."
        } else {
            error = "Backup save cancelled. Nothing was exported."
        }
    }

    val openPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            page = BackupPage.RESTORE
            selectedUri = uri
            selectedFileName = backupDisplayName(context, uri)
            restorePassphrase = ""
            approvedFile = null
            acknowledgedReplacement = false
            restoreStep = RestoreStep.PASSWORD
            error = null
        }
    }

    if (page == BackupPage.HOME) {
        ModalBottomSheet(
            onDismissRequest = { if (!busy) onDismiss() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(horizontal = PesaDesignTokens.screenHorizontalPadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp))
                Text("Encrypted backups", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Text("Keep your money data safe and private.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                BackupError(error)
                BackupActionCard(Icons.Rounded.Backup, "Create backup", "Save a protected .ptb file") {
                    clearSensitiveInputs()
                    page = BackupPage.CREATE
                }
                BackupActionCard(Icons.Rounded.Restore, "Restore backup", "Open an existing .ptb file") {
                    clearSensitiveInputs()
                    restoreStep = RestoreStep.SELECT
                    page = BackupPage.RESTORE
                }
                Text("Your file is encrypted before saving. You can choose phone storage or an installed file provider. Your app-lock PIN isn't included.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp))
            }
        }
    } else {
        Dialog(
            onDismissRequest = { leaveFlow() },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
        ) {
            BackupFullScreen(
                title = if (page == BackupPage.CREATE) "Create backup" else "Restore backup",
                onBack = { leaveFlow() }
            ) {
                if (page == BackupPage.CREATE) {
                    if (created == null) {
                        BackupStepLabel("1  Protect     •     2  Save     •     3  Verify")
                        Icon(Icons.Rounded.Lock, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(46.dp))
                        Text("Protect your money data", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text("Choose a strong backup passphrase. You will need it even if you restore on another phone.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BackupPasswordField(createPassphrase, "Backup passphrase") {
                            createPassphrase = it
                            error = null
                        }
                        BackupPasswordField(confirmPassphrase, "Confirm passphrase") {
                            confirmPassphrase = it
                            error = null
                        }
                        Text("Use at least 12 characters. Keep the passphrase somewhere safe—PesaTrack cannot recover it.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                        BackupError(error)
                        if (busy) BackupBusy("Encrypting and verifying your saved backup…")
                        Button(
                            onClick = {
                                error = BackupFlowRules.createError(createPassphrase, confirmPassphrase)
                                if (error == null) {
                                    pendingCreatePassphrase = createPassphrase
                                    val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                                    createPicker.launch("PesaTrack-$stamp.ptb")
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Choose where to save")
                        }
                    } else {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
                        Text("Backup saved and verified", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text("Encrypted file verified by opening it again. Keep your passphrase separate from the file.")
                        BackupFact("Saved file", createdFileName.ifBlank { "PesaTrack .ptb" })
                        BackupFact("File provider", createdProvider.ifBlank { "Chosen provider" })
                        BackupFact("Ledger-table rows", created!!.rowCount.toString())
                        BackupFact("Saved statement rows", created!!.statementRows.toString())
                        Button(onClick = {
                            clearSensitiveInputs()
                            page = BackupPage.HOME
                            restoreStep = RestoreStep.SELECT
                            onDismiss()
                        },
                            modifier = Modifier.fillMaxWidth()) { Text("Done") }
                    }
                } else {
                    when (restoreStep) {
                        RestoreStep.SELECT -> {
                            BackupStepLabel("1  Select     •     2  Unlock     •     3  Review")
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(46.dp))
                            Text("Choose your encrypted file", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                            Text("PesaTrack accepts its encrypted .ptb backup files. Choose a file from your phone or a connected file provider.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { openPicker.launch(arrayOf("*/*")) },
                                modifier = Modifier.fillMaxWidth()) { Text("Choose .ptb backup") }
                        }
                        RestoreStep.PASSWORD -> {
                            BackupStepLabel("1  Selected     •     2  Unlock     •     3  Review")
                            Text("Backup file selected", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                            BackupFact("File", selectedFileName.ifBlank { "Selected .ptb file" })
                            Text("Enter the passphrase used when this file was created.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            BackupPasswordField(restorePassphrase, "Backup passphrase") {
                                restorePassphrase = it
                                error = null
                            }
                            BackupError(error)
                            if (busy) BackupBusy("Checking the encrypted backup…")
                            Button(
                                enabled = !busy,
                                onClick = {
                                    error = BackupFlowRules.restoreError(restorePassphrase)
                                    val uri = selectedUri
                                    if (error == null && uri != null) {
                                        busy = true
                                        val passphrase = restorePassphrase
                                        state.viewModelScope.launch {
                                            runCatching {
                                                withContext(Dispatchers.IO) {
                                                    manager.previewEncryptedForRestore(uri, passphrase)
                                                }
                                            }.onSuccess {
                                                approvedFile = it
                                                acknowledgedReplacement = false
                                                restoreStep = RestoreStep.REVIEW
                                                error = null
                                            }.onFailure {
                                                error = it.message ?: "This file couldn't be opened. Check its passphrase."
                                            }
                                            busy = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Preview backup") }
                            OutlinedButton(onClick = {
                                selectedUri = null
                                restorePassphrase = ""
                                approvedFile = null
                                restoreStep = RestoreStep.SELECT
                                error = null
                                openPicker.launch(arrayOf("*/*"))
                            },
                                enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                                Text("Choose another file")
                            }
                        }
                        RestoreStep.REVIEW -> {
                            val details = approvedFile?.preview
                            if (details != null) {
                                BackupStepLabel("1  Selected     •     2  Unlocked     •     3  Review")
                                Text("Review backup", style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold)
                                BackupFact("Created", formatBackupDate(details.createdAtEpochMs))
                                BackupFact("Ledger-table rows", details.rowCount.toString())
                                BackupFact("Saved statement rows", details.statementRows.toString())
                                BackupFact("Statement history", if (details.statementHistoryIncluded) "Included" else "Not included")
                                BackupFact("Schema", "v${details.schemaVersion}")
                                if (!details.statementHistoryIncluded) {
                                    BackupWarning("Older file: statement history is not included. Restore is refused when that would erase statements already saved on this phone.")
                                }
                                BackupWarning("This replaces your current backed-up ledger, statement archive and preferences. It does not merge transactions. Your app-lock PIN stays on this phone.")
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = acknowledgedReplacement,
                                        onCheckedChange = { acknowledgedReplacement = it })
                                    Text("I understand this replaces existing data.")
                                }
                                BackupError(error)
                                if (busy || awaitingCredential) BackupBusy("Preparing a secure restore…")
                                Button(
                                    enabled = BackupFlowRules.canRestore(true, acknowledgedReplacement, busy) && !awaitingCredential,
                                    onClick = { authorizeRestore() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                                    Spacer(Modifier.size(8.dp))
                                    Text("Restore and replace")
                                }
                            }
                        }
                        RestoreStep.APP_PIN -> {
                            Text("Confirm with PesaTrack PIN", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                            Text("Your phone has no available screen-lock confirmation. Enter your existing PesaTrack app-lock PIN to authorize replacing data.")
                            BackupPasswordField(appPin, "PesaTrack PIN") {
                                appPin = it.filter(Char::isDigit).take(8)
                                error = null
                            }
                            BackupError(error)
                            Button(onClick = {
                                if (appLock.verifyPin(appPin)) {
                                    appPin = ""
                                    finishRestore()
                                } else {
                                    error = "PIN incorrect or temporarily locked. Nothing was restored."
                                }
                            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Confirm and restore") }
                        }
                        RestoreStep.NO_DEVICE_LOCK -> {
                            Text("No phone lock is configured", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                            BackupWarning("This phone cannot confirm your identity with a fingerprint, device lock or PesaTrack PIN. Your backup passphrase was verified, but this restore will replace current data without a second identity check.")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = acknowledgedNoDeviceLock,
                                    onCheckedChange = { acknowledgedNoDeviceLock = it })
                                Text("I understand and want to replace the data.")
                            }
                            Button(onClick = { finishRestore() },
                                enabled = acknowledgedNoDeviceLock && !busy,
                                modifier = Modifier.fillMaxWidth()) { Text("Replace data anyway") }
                        }
                        RestoreStep.COMPLETE -> {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
                            Text("Restore complete", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                            Text("PesaTrack is reloading your restored local data.")
                            Button(onClick = {
                                clearSensitiveInputs()
                                restoreStep = RestoreStep.SELECT
                                page = BackupPage.HOME
                                onDismiss()
                            }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupFullScreen(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PesaDesignTokens.screenHorizontalPadding, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun BackupActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BackupPasswordField(value: String, label: String, onChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value = value, onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (visible) "Hide passphrase" else "Show passphrase")
            }
        })
}

@Composable
private fun BackupFact(label: String, value: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BackupStepLabel(text: String) {
    Text(text, color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun BackupWarning(text: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFFFF4DD)) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(16.dp), color = Color(0xFF6A4300),
            style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BackupError(text: String?) {
    if (text != null) Text(text, color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun BackupBusy(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatBackupDate(createdAtEpochMs: Long): String =
    if (createdAtEpochMs > 0L) {
        SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(createdAtEpochMs))
    } else "Unknown"

private fun backupDisplayName(context: Context, uri: android.net.Uri): String =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
            }.orEmpty()
    }.getOrDefault("").ifBlank { uri.lastPathSegment?.substringAfterLast('/') ?: "Backup file" }

@Suppress("DEPRECATION")
private fun backupProviderName(context: Context, uri: android.net.Uri): String =
    if (uri.scheme == "file") "Device storage" else runCatching {
        uri.authority?.let { authority ->
            context.packageManager.resolveContentProvider(authority, 0)
                ?.loadLabel(context.packageManager)?.toString()
        }
    }.getOrNull().orEmpty().ifBlank { "Selected files provider" }
