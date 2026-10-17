package com.kedevjimmy.pesatrack.ui.screens

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.security.AppLockManager
import com.kedevjimmy.pesatrack.security.BackupRecoveryStatus
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.security.BackupPreview
import com.kedevjimmy.pesatrack.security.PesaBackupManager
import com.kedevjimmy.pesatrack.ui.components.PesaLogoMark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun BackupRecoveryBlockingScreen(onRetry: () -> Unit) {
    BackHandler { /* Recovery must finish before returning to the ledger. */ }
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.backup_recovery_retry_message))
        Spacer(Modifier.size(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.backup_recovery_retry_action))
        }
    }
}

@Composable
fun AppLockScreen(
    biometricAvailable: Boolean,
    onPinUnlock: (String) -> Boolean,
    pinLockoutRemainingMs: () -> Long,
    onBiometricUnlock: () -> Unit
) {
    var pin by remember {
        mutableStateOf("")
    }
    var error by remember {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF24213F),
                            Color(0xFF111827),
                            Color(0xFF090D18)
                        )
                    )
                )
                .padding(
                    horizontal = 28.dp,
                    vertical = 36.dp
                ),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        PesaLogoMark(
            size = 72.dp
        )

        Surface(
            modifier =
                Modifier
                    .padding(top = 26.dp)
                    .size(66.dp),
            shape = CircleShape,
            color =
                Color.White.copy(
                    alpha = .1f
                )
        ) {
            androidx.compose.foundation.layout.Box(
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription =
                        null,
                    tint =
                        Color(0xFFA69CFF),
                    modifier =
                        Modifier.size(
                            30.dp
                        )
                )
            }
        }

        Text(
            "PesaTrack is locked",
            modifier =
                Modifier.padding(
                    top = 18.dp
                ),
            style =
                MaterialTheme
                    .typography
                    .headlineMedium,
            fontWeight =
                FontWeight.Bold,
            color = Color.White
        )
        Text(
            "Unlock to view your private money data.",
            modifier =
                Modifier.padding(
                    top = 5.dp,
                    bottom = 22.dp
                ),
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            color =
                Color.White.copy(
                    alpha = .72f
                ),
            textAlign =
                TextAlign.Center
        )

        OutlinedTextField(
            value = pin,
            onValueChange = {
                pin =
                    it.filter(
                        Char::isDigit
                    )
                        .take(8)
                error = null
            },
            modifier =
                Modifier.fillMaxWidth(),
            label = {
                Text("PIN")
            },
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType
                            .NumberPassword
                ),
            visualTransformation =
                PasswordVisualTransformation(),
            isError =
                error != null
        )

        error?.let {
            Text(
                it,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 8.dp
                        ),
                color =
                    MaterialTheme
                        .colorScheme
                        .error,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }

        Button(
            onClick = {
                if (
                    onPinUnlock(pin)
                ) {
                    pin = ""
                    error = null
                } else {
                    val remainingMs =
                        pinLockoutRemainingMs()
                    error =
                        if (remainingMs > 0L) {
                            "Too many attempts. Try again in " +
                                lockoutSeconds(
                                    remainingMs
                                ) +
                                " seconds."
                        } else {
                            "That PIN is not correct."
                        }
                }
            },
            enabled =
                pin.length >= 4,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 16.dp
                    ),
            shape =
                RoundedCornerShape(
                    18.dp
                )
        ) {
            Icon(
                Icons.Rounded.LockOpen,
                contentDescription = null
            )
            Spacer(
                Modifier.width(8.dp)
            )
            Text("Unlock")
        }

        if (biometricAvailable) {
            OutlinedButton(
                onClick =
                    onBiometricUnlock,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 10.dp
                        ),
                shape =
                    RoundedCornerShape(
                        18.dp
                    )
            ) {
                Icon(
                    Icons.Rounded.Fingerprint,
                    contentDescription =
                        null
                )
                Spacer(
                    Modifier.width(8.dp)
                )
                Text("Use biometrics")
            }
        }

        Text(
            "Your PIN is stored only on this device and is not included in backups.",
            modifier =
                Modifier.padding(
                    top = 18.dp
                ),
            style =
                MaterialTheme
                    .typography
                    .bodySmall,
            color =
                Color.White.copy(
                    alpha = .58f
                ),
            textAlign =
                TextAlign.Center
        )
    }
}

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun AppLockSettingsSheet(
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    val context =
        LocalContext.current
    val manager =
        remember {
            AppLockManager(context)
        }
    val enabled =
        manager.isEnabled()

    var currentPin by remember {
        mutableStateOf("")
    }
    var newPin by remember {
        mutableStateOf("")
    }
    var confirmPin by remember {
        mutableStateOf("")
    }
    var error by remember {
        mutableStateOf<String?>(null)
    }

    ModalBottomSheet(
        onDismissRequest =
            onDismiss,
        sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded =
                    true
            ),
        shape =
            RoundedCornerShape(
                topStart = 30.dp,
                topEnd = 30.dp
            )
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 8.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {
            Text(
                if (enabled) {
                    "App lock"
                } else {
                    "Protect PesaTrack"
                },
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,
                fontWeight =
                    FontWeight.Bold
            )
            Text(
                if (enabled) {
                    "A PIN is required after PesaTrack has been in the background for 30 seconds. You can also use device biometrics where available."
                } else {
                    "Add a 6–8 digit local PIN so opening PesaTrack requires authentication after it has been in the background."
                },
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            if (enabled) {
                SecurityPinField(
                    value =
                        currentPin,
                    label =
                        "Current PIN",
                    onValueChange = {
                        currentPin = it
                        error = null
                    }
                )
            }

            SecurityPinField(
                value =
                    newPin,
                label =
                    if (enabled) {
                        "New PIN (optional)"
                    } else {
                        "New PIN"
                    },
                onValueChange = {
                    newPin = it
                    error = null
                }
            )
            SecurityPinField(
                value =
                    confirmPin,
                label =
                    if (enabled) {
                        "Confirm new PIN"
                    } else {
                        "Confirm PIN"
                    },
                onValueChange = {
                    confirmPin = it
                    error = null
                }
            )

            error?.let {
                Text(
                    it,
                    color =
                        MaterialTheme
                            .colorScheme
                            .error,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }

            if (enabled) {
                OutlinedButton(
                    onClick = {
                        if (
                            manager.disable(
                                currentPin
                            )
                        ) {
                            syncSecureWindow(
                                context = context,
                                enabled = false
                            )
                            onChanged()
                            onDismiss()
                        } else {
                            error =
                                lockoutError(
                                    manager =
                                        manager,
                                    fallback =
                                        "Enter the current PIN before disabling app lock."
                                )
                        }
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text("Disable app lock")
                }
            }

            Button(
                onClick = {
                    runCatching {
                        if (
                            enabled &&
                            !manager.verifyPin(
                                currentPin
                            )
                        ) {
                            error =
                                lockoutError(
                                    manager =
                                        manager,
                                    fallback =
                                        "The current PIN is not correct."
                                )
                            return@Button
                        }
                        if (
                            newPin !=
                            confirmPin
                        ) {
                            error =
                                "The new PINs do not match."
                            return@Button
                        }
                        AppLockManager
                            .requireValidPin(
                                newPin
                            )
                        manager.setPin(
                            newPin
                        )
                        syncSecureWindow(
                            context = context,
                            enabled = true
                        )
                        onChanged()
                        onDismiss()
                    }.onFailure {
                        error =
                            it.message
                                ?: "Could not update app lock."
                    }
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    if (enabled) {
                        "Change PIN"
                    } else {
                        "Enable app lock"
                    }
                )
            }

            Text(
                "PesaTrack cannot recover a forgotten app-lock PIN. Your financial database is not uploaded to recover it.",
                modifier =
                    Modifier.padding(
                        bottom = 16.dp
                    ),
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

private fun lockoutSeconds(
    remainingMs: Long
): Long =
    (
        remainingMs +
            999L
        ) / 1_000L

private fun lockoutError(
    manager: AppLockManager,
    fallback: String
): String {
    val remainingMs =
        manager.remainingLockoutMs()
    return if (remainingMs > 0L) {
        "Too many attempts. Try again in " +
            lockoutSeconds(
                remainingMs
            ) +
            " seconds."
    } else {
        fallback
    }
}

private fun syncSecureWindow(
    context: Context,
    enabled: Boolean
) {
    val activity =
        context as? Activity
            ?: return
    if (enabled) {
        activity.window.addFlags(
            WindowManager.LayoutParams
                .FLAG_SECURE
        )
    } else {
        activity.window.clearFlags(
            WindowManager.LayoutParams
                .FLAG_SECURE
        )
    }
}

@Composable
private fun SecurityPinField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(
                it.filter(
                    Char::isDigit
                )
                    .take(8)
            )
        },
        modifier =
            Modifier.fillMaxWidth(),
        label = {
            Text(label)
        },
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                keyboardType =
                    KeyboardType
                        .NumberPassword
            ),
        visualTransformation =
            PasswordVisualTransformation()
    )
}

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun BackupRestoreSheet(
    onDismiss: () -> Unit
) {
    val context =
        LocalContext.current
    val locale =
        LocalConfiguration
            .current
            .locales[0]
    val scope =
        rememberCoroutineScope()
    val manager =
        remember {
            PesaBackupManager(context)
        }

    var passphrase by remember {
        mutableStateOf("")
    }
    var confirm by remember {
        mutableStateOf("")
    }
    var pendingPassphrase by remember {
        mutableStateOf("")
    }
    var busy by remember {
        mutableStateOf(false)
    }
    var message by remember {
        mutableStateOf<String?>(null)
    }
    var error by remember {
        mutableStateOf<String?>(null)
    }
    var pendingRestoreUri by remember {
        mutableStateOf<android.net.Uri?>(null)
    }
    var pendingRestorePreview by remember {
        mutableStateOf<BackupPreview?>(null)
    }

    val createBackup =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .CreateDocument(
                    "application/octet-stream"
                )
        ) { uri ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }
            busy = true
            scope.launch {
                runCatching {
                    withContext(
                        Dispatchers.IO
                    ) {
                        manager.exportEncrypted(
                            uri =
                                uri,
                            passphrase =
                                pendingPassphrase
                        )
                    }
                }.onSuccess {
                    message =
                        "Encrypted backup created • " +
                            it.rowCount +
                            " ledger-table rows and " +
                            it.statementRows +
                            " saved statement rows."
                    error = null
                }.onFailure {
                    error =
                        it.message
                            ?: "Could not create backup."
                }
                busy = false
            }
        }

    val restoreBackup =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .OpenDocument()
        ) { uri ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }
            busy = true
            scope.launch {
                runCatching {
                    withContext(
                        Dispatchers.IO
                    ) {
                        manager.previewEncrypted(
                            uri = uri,
                            passphrase =
                                pendingPassphrase
                        )
                    }
                }.onSuccess {
                    pendingRestoreUri = uri
                    pendingRestorePreview = it
                    message = null
                    error = null
                }.onFailure {
                    pendingRestoreUri = null
                    pendingRestorePreview = null
                    error =
                        it.message
                            ?: "Could not inspect this backup."
                }
                busy = false
            }
        }

    pendingRestorePreview
        ?.let { preview ->
            val restoreUri =
                pendingRestoreUri
            if (restoreUri != null) {
                AlertDialog(
                    onDismissRequest = {
                        if (!busy) {
                            pendingRestoreUri =
                                null
                            pendingRestorePreview =
                                null
                        }
                    },
                    title = {
                        Text(
                            "Restore this backup?"
                        )
                    },
                    text = {
                        val created =
                            if (
                                preview.createdAtEpochMs >
                                0L
                            ) {
                                SimpleDateFormat(
                                    "d MMM yyyy, HH:mm",
                                    locale
                                ).format(
                                    Date(
                                        preview
                                            .createdAtEpochMs
                                    )
                                )
                            } else {
                                "Unknown"
                            }
                        Text(
                            "Created: " +
                                created +
                                "\nSchema: v" +
                                preview.schemaVersion +
                                "\nLedger-table rows: " +
                                preview.rowCount +
                                "\nSaved statement rows: " +
                                preview.statementRows +
                                (if (preview.statementHistoryIncluded) "" else
                                    "\n\nOlder backup: it contains no Statement Activity. If this phone has saved statements, restore will be refused to protect them.") +
                                "\n\nRestoring replaces the backed-up local ledger, statement archive (when included), and preferences. Your app-lock PIN stays on this device."
                        )
                    },
                    confirmButton = {
                        Button(
                            enabled = !busy,
                            onClick = {
                                pendingRestoreUri =
                                    null
                                pendingRestorePreview =
                                    null
                                busy = true
                                scope.launch {
                                    runCatching {
                                        withContext(
                                            Dispatchers.IO
                                        ) {
                                            manager
                                                .restoreEncrypted(
                                                    uri =
                                                        restoreUri,
                                                    passphrase =
                                                        pendingPassphrase
                                                )
                                        }
                                    }.onSuccess {
                                        message =
                                            "Restore complete • " +
                                                it.rowCount +
                                                " ledger-table rows and " +
                                                it.statementRows +
                                                " saved statement rows restored."
                                        error = null
                                        delay(450)
                                        (
                                            context as?
                                                Activity
                                            )?.recreate()
                                    }.onFailure {
                                        error =
                                            it.message
                                                ?: "Could not restore this backup."
                                        if (BackupRecoveryStatus.failure != null) {
                                            (context as? Activity)?.recreate()
                                        }
                                    }
                                    busy = BackupRecoveryStatus.failure != null
                                }
                            }
                        ) {
                            Text(
                                "Restore and replace data"
                            )
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            enabled = !busy,
                            onClick = {
                                pendingRestoreUri =
                                    null
                                pendingRestorePreview =
                                    null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

    ModalBottomSheet(
        onDismissRequest = {
            if (!busy) {
                onDismiss()
            }
        },
        sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded =
                    true
            ),
        shape =
            RoundedCornerShape(
                topStart = 30.dp,
                topEnd = 30.dp
            )
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 8.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {
            Text(
                "Encrypted backup & restore",
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,
                fontWeight =
                    FontWeight.Bold
            )
            Text(
                "Backups contain your local PesaTrack financial database and app preferences. Your app-lock PIN is deliberately excluded.",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            OutlinedTextField(
                value = passphrase,
                onValueChange = {
                    passphrase = it
                    error = null
                },
                modifier =
                    Modifier.fillMaxWidth(),
                label = {
                    Text(
                        "Backup passphrase"
                    )
                },
                singleLine = true,
                visualTransformation =
                    PasswordVisualTransformation()
            )
            OutlinedTextField(
                value = confirm,
                onValueChange = {
                    confirm = it
                    error = null
                },
                modifier =
                    Modifier.fillMaxWidth(),
                label = {
                    Text(
                        "Confirm passphrase"
                    )
                },
                singleLine = true,
                visualTransformation =
                    PasswordVisualTransformation()
            )

            message?.let {
                Text(
                    it,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
            error?.let {
                Text(
                    it,
                    color =
                        MaterialTheme
                            .colorScheme
                            .error,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }

            if (busy) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                28.dp
                            )
                    )
                }
            } else {
                Button(
                    onClick = {
                        when {
                            passphrase.length <
                                8 ->
                                error =
                                    "Use at least 8 characters."
                            passphrase !=
                                confirm ->
                                error =
                                    "The passphrases do not match."
                            else -> {
                                pendingPassphrase =
                                    passphrase
                                val date =
                                    SimpleDateFormat(
                                        "yyyyMMdd-HHmm",
                                        Locale.getDefault()
                                    ).format(
                                        Date()
                                    )
                                createBackup
                                    .launch(
                                        "PesaTrack-" +
                                            date +
                                            ".ptb"
                                    )
                            }
                        }
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Rounded.Backup,
                        contentDescription = null
                    )
                    Spacer(
                        Modifier.width(8.dp)
                    )
                    Text(
                        "Create encrypted backup"
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (
                            passphrase.length <
                            8
                        ) {
                            error =
                                "Enter the backup passphrase first."
                        } else {
                            pendingPassphrase =
                                passphrase
                            restoreBackup
                                .launch(
                                    arrayOf(
                                        "application/octet-stream",
                                        "application/zip",
                                        "*/*"
                                    )
                                )
                        }
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Rounded.Restore,
                        contentDescription = null
                    )
                    Spacer(
                        Modifier.width(8.dp)
                    )
                    Text(
                        "Preview backup to restore"
                    )
                }
            }

            Text(
                "Restore always shows backup details and asks for confirmation before replacing local data. Files larger than 64 MB are rejected. Keep the passphrase somewhere safe; PesaTrack cannot decrypt the backup without it.",
                modifier =
                    Modifier.padding(
                        bottom = 16.dp
                    ),
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}
