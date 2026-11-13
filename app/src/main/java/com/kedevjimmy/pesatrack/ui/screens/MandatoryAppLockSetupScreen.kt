package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.security.AppLockManager

/** Required after initial financial-source onboarding, or on upgrade for unprotected installs. */
@Composable
fun MandatoryAppLockSetupScreen(onSecured: () -> Unit) {
    BackHandler { /* Never reveal the financial UI without an app PIN. */ }
    val context = LocalContext.current
    val manager = remember(context) { AppLockManager(context) }
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Secure your PesaTrack", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "Protect your personal financial information with an app PIN. " +
                "Biometric unlock is available later on supported devices.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(8); error = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Create a 6–8 digit PIN") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = confirmation,
            onValueChange = { confirmation = it.filter(Char::isDigit).take(8); error = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Confirm PIN") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation()
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = pin.length >= 6 && confirmation.length >= 6,
            onClick = {
                if (pin != confirmation) {
                    error = "The PINs do not match."
                } else {
                    runCatching { manager.setPin(pin) }
                        .onSuccess {
                            pin = ""
                            confirmation = ""
                            onSecured()
                        }
                        .onFailure { error = "Could not save your PIN. Try again." }
                }
            }
        ) { Text("Continue securely") }
        Spacer(Modifier.height(16.dp))
        Text(
            "PesaTrack is offline. A forgotten PIN cannot be reset through email. " +
                "Keep a secure, encrypted backup of your data.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
