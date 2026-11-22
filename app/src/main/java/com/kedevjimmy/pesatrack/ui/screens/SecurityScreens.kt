package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.security.AppLockManager
import com.kedevjimmy.pesatrack.security.LockKeypadEntryPolicy
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.security.PinAutoSubmitPolicy
import com.kedevjimmy.pesatrack.security.PinEntryAction

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
    storedPinLength: Int?,
    onPinUnlock: (String) -> Boolean,
    pinLockoutRemainingMs: () -> Long,
    onBiometricUnlock: () -> Unit,
    deviceCredentialRecoveryAvailable: Boolean,
    onRecoverWithDeviceCredential: () -> Unit
) {
    BackHandler { /* Locked app never passes back navigation to the hidden app. */ }
    var pin by remember {
        mutableStateOf("")
    }
    var error by remember {
        mutableStateOf<String?>(null)
    }
    var checking by remember { mutableStateOf(false) }
    var legacyHint by remember { mutableStateOf(false) }
    var keyboardMode by remember { mutableStateOf(false) }
    var lastEditWasDeletion by remember { mutableStateOf(false) }

    LaunchedEffect(pin, storedPinLength, lastEditWasDeletion) {
        if (PinAutoSubmitPolicy.onChange(pin, storedPinLength, lastEditWasDeletion) == PinEntryAction.WAIT) {
            checking = false
            return@LaunchedEffect
        }
        checking = true
        delay(100) // Give the in-field progress state time to render before hashing.
        val verified = onPinUnlock(pin)
        checking = false
        if (verified) {
            pin = ""
            error = null
            legacyHint = false
        } else if (storedPinLength != null || pin.length == 8) {
            val remainingMs = pinLockoutRemainingMs()
            error = if (remainingMs > 0L) {
                "Too many attempts. Try again in ${lockoutSeconds(remainingMs)} seconds."
            } else "That PIN is not correct."
            pin = ""
            legacyHint = false
        } else {
            // Pre-update PINs have no saved length. A single typing sequence is
            // checked at 4–8 digits; only its first wrong prefix counts as a
            // failed attempt, and success stores the length permanently.
            legacyHint = true
        }
    }

    AppLockKeypadSurface(
        pinText = pin,
        enteredDigits = pin.length,
        expectedDigits = LockKeypadEntryPolicy.indicatorCount(storedPinLength),
        checking = checking,
        error = error,
        legacyHint = legacyHint,
        biometricAvailable = biometricAvailable,
        recoveryAvailable = deviceCredentialRecoveryAvailable,
        keyboardMode = keyboardMode,
        onDigit = { digit ->
            error = null
            legacyHint = false
            lastEditWasDeletion = false
            pin = LockKeypadEntryPolicy.appendDigit(pin, digit, storedPinLength)
        },
        onDelete = {
            error = null
            legacyHint = false
            lastEditWasDeletion = true
            pin = LockKeypadEntryPolicy.removeLast(pin)
        },
        onKeyboardPinChange = { text ->
            error = null
            legacyHint = false
            val nextPin = LockKeypadEntryPolicy.fromKeyboard(text, storedPinLength)
            lastEditWasDeletion = nextPin.length < pin.length
            pin = nextPin
        },
        onToggleKeyboard = {
            keyboardMode = !keyboardMode
            lastEditWasDeletion = false
            pin = ""
            error = null
            legacyHint = false
        },
        onFingerprint = onBiometricUnlock,
        onRecover = onRecoverWithDeviceCredential
    )
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
                    "A PIN is required after your chosen background lock delay. You can also use device biometrics where available."
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
