package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.data.local.SmsAccessConsentStore
import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferDetector
import com.kedevjimmy.pesatrack.security.AppLockManager
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens

/** Local-only setup. Identity hints must never automatically confirm account ownership. */
@Composable
internal fun OnboardingLocalProfilePage(onProceed: () -> Unit) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var accepted by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().background(PremiumVisualTokens.darkCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnboardingBrandHeader()
        Spacer(Modifier.height(30.dp))
        Text("Your money, your space", color = PremiumVisualTokens.darkText,
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
        Text("Let's make this yours. Everything stays on this phone.",
            Modifier.padding(top = 9.dp, bottom = 24.dp), color = PremiumVisualTokens.darkMuted,
            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)

        DarkSetupField("Full name", fullName, { fullName = it.take(90); error = null },
            KeyboardType.Text)
        Spacer(Modifier.height(12.dp))
        DarkSetupField("M-PESA phone number", phone,
            { phone = it.filter { c -> c.isDigit() || c == '+' }.take(14); error = null },
            KeyboardType.Phone)
        Spacer(Modifier.height(12.dp))
        DarkSetupField("Create PIN", pin,
            { pin = it.filter(Char::isDigit).take(8); error = null },
            KeyboardType.NumberPassword, secret = true)
        Spacer(Modifier.height(12.dp))
        DarkSetupField("Confirm PIN", confirmPin,
            { confirmPin = it.filter(Char::isDigit).take(8); error = null },
            KeyboardType.NumberPassword, secret = true)

        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = accepted, onCheckedChange = { accepted = it; error = null },
                colors = CheckboxDefaults.colors(checkedColor = PremiumVisualTokens.darkCoffee,
                    uncheckedColor = PremiumVisualTokens.darkMuted,
                    checkmarkColor = PremiumVisualTokens.ink))
            Text("I agree to PesaTrack checking SMS sender names and reading messages only from recognised or approved financial senders to find my transactions.",
                color = PremiumVisualTokens.darkText, style = MaterialTheme.typography.bodySmall)
        }
        Text("You'll choose which accounts are yours after scanning. You can manage access in Android settings.",
            Modifier.fillMaxWidth().padding(start = 13.dp, top = 6.dp),
            color = PremiumVisualTokens.darkMuted, style = MaterialTheme.typography.labelSmall)

        error?.let {
            Text(it, Modifier.fillMaxWidth().padding(top = 12.dp),
                color = PremiumVisualTokens.darkMoneyOut, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val normalized = OwnAccountTransferDetector.normalizeKenyanPhone(phone)
                error = when {
                    fullName.trim().split(Regex("\\s+")).size < 2 -> "Enter at least your first and last name."
                    normalized == null -> "Enter a valid Kenyan mobile phone number."
                    pin.length !in 6..8 -> "Choose a 6–8 digit PIN."
                    pin != confirmPin -> "Your PINs do not match."
                    !accepted -> "Please agree to the financial SMS disclosure to start scanning."
                    else -> null
                }
                if (error == null && normalized != null) {
                    val saved = runCatching {
                        val preferences = context.getSharedPreferences("pesatrack_preferences", Context.MODE_PRIVATE)
                        check(preferences.edit()
                            .putString("profile_name", fullName.trim().replace(Regex("\\s+"), " "))
                            .putString("profile_mpesa_phone", normalized).commit())
                        AppLockManager(context).setPin(pin)
                        check(SmsAccessConsentStore.setAccepted(context, true))
                    }
                    if (saved.isSuccess) onProceed()
                    else error = "Could not securely save your setup. Please try again."
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PremiumVisualTokens.darkCoffee,
                contentColor = PremiumVisualTokens.ink
            )
        ) { Text("Start scanning", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DarkSetupField(
    title: String,
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType,
    secret: Boolean = false
) {
    TextField(
        value = value, onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        label = { Text(title) }, singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(15.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = PremiumVisualTokens.darkCard,
            unfocusedContainerColor = PremiumVisualTokens.darkCard,
            focusedTextColor = PremiumVisualTokens.darkText,
            unfocusedTextColor = PremiumVisualTokens.darkText,
            focusedLabelColor = PremiumVisualTokens.darkCoffee,
            unfocusedLabelColor = PremiumVisualTokens.darkMuted,
            cursorColor = PremiumVisualTokens.darkCoffee,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}
