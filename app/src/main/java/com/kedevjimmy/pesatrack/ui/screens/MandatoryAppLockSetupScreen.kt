package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.security.AppLockManager
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens

/** Required after initial financial-source onboarding, or on upgrade for unprotected installs. */
@Composable
fun MandatoryAppLockSetupScreen(onSecured: () -> Unit, recoveringExistingPin: Boolean = false) {
    BackHandler { /* Never reveal the financial UI without an app PIN. */ }
    val context = LocalContext.current
    val manager = remember(context) { AppLockManager(context) }
    val blockerInteraction = remember { MutableInteractionSource() }
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var revealPin by remember { mutableStateOf(false) }
    var revealConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(interactionSource = blockerInteraction, indication = null) {
                /* Prevent taps from reaching financial controls behind this screen. */
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnboardingPinHero(recoveringExistingPin)
        Text(
            if (recoveringExistingPin) "Your phone lock confirmed your identity. Choose a new PIN; your money records will stay on this phone."
            else "Create a 6–8 digit PIN. You can turn on fingerprint unlock later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OnboardingPinField(pin, "Create a 6–8 digit PIN", revealPin,
            onReveal = { revealPin = !revealPin }) {
            pin = it.filter(Char::isDigit).take(8)
            error = null
        }
        OnboardingPinField(confirmation, "Confirm PIN", revealConfirmation,
            onReveal = { revealConfirmation = !revealConfirmation }) {
            confirmation = it.filter(Char::isDigit).take(8)
            error = null
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Button(
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
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
        Text(
            "PesaTrack is offline. A forgotten PIN cannot be reset through email. " +
                "Keep a secure, encrypted backup of your data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OnboardingPinField(
    value: String,
    label: String,
    revealed: Boolean,
    onReveal: () -> Unit,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onReveal) {
                Icon(if (revealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (revealed) "Hide $label" else "Show $label")
            }
        }
    )
}

@Composable
private fun OnboardingPinHero(recoveringExistingPin: Boolean) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .45f),
        shape = RoundedCornerShape(PesaDesignTokens.cardRadius)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(Modifier.size(72.dp), shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Lock, contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Text(if (recoveringExistingPin) "Set a new app PIN" else "Secure your PesaTrack",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center)
            Text("Private money tracking, protected on your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}
