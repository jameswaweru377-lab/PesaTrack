package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.ui.components.PesaLogoMark
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens

/** Daily unlock surface; the existing PIN and biometric policies remain in AppLockScreen. */
@Composable
internal fun AppLockKeypadSurface(
    enteredDigits: Int,
    expectedDigits: Int,
    checking: Boolean,
    error: String?,
    legacyHint: Boolean,
    biometricAvailable: Boolean,
    recoveryAvailable: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onFingerprint: () -> Unit,
    onRecover: () -> Unit
) {
    val blockerInteraction = remember { MutableInteractionSource() }
    Column(
        Modifier.fillMaxSize().background(PremiumVisualTokens.darkCanvas)
            .clickable(interactionSource = blockerInteraction, indication = null) {}
            .navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LockBrandHeader()
        LockPinPrompt(enteredDigits, expectedDigits, checking, error, legacyHint)
        LockNumberPad(biometricAvailable, onDigit, onDelete, onFingerprint)
        LockRecoveryFooter(recoveryAvailable, onRecover)
    }
}

@Composable
private fun LockBrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PesaLogoMark(size = PesaDesignTokens.minimumTouchTarget)
        Text("PesaTrack", modifier = Modifier.padding(top = 8.dp),
            color = PremiumVisualTokens.darkText, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("YOUR MONEY, YOUR PLANS", modifier = Modifier.padding(top = 4.dp),
            color = PremiumVisualTokens.darkCoffee, fontSize = 10.sp, letterSpacing = 2.sp)
    }
}

@Composable
private fun LockPinPrompt(
    enteredDigits: Int, expectedDigits: Int, checking: Boolean,
    error: String?, legacyHint: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Welcome back", color = PremiumVisualTokens.darkText,
            fontFamily = FontFamily.Serif, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text("Enter your PIN to continue", modifier = Modifier.padding(top = 8.dp),
            color = PremiumVisualTokens.darkMuted, fontSize = 14.sp)
        LockPinIndicators(enteredDigits, expectedDigits)
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
            when {
                checking -> CircularProgressIndicator(modifier = Modifier.size(16.dp),
                    color = PremiumVisualTokens.darkCoffee, strokeWidth = 2.dp)
                error != null -> Text(error, color = PremiumVisualTokens.darkMoneyOut,
                    fontSize = 12.sp, textAlign = TextAlign.Center)
                legacyHint -> Text("Longer PIN? Keep entering digits.",
                    color = PremiumVisualTokens.darkMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LockPinIndicators(enteredDigits: Int, expectedDigits: Int) {
    Row(Modifier.padding(top = 24.dp).semantics {
        contentDescription = "$enteredDigits of $expectedDigits PIN digits entered"
    }, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(expectedDigits) { index ->
            Box(Modifier.size(12.dp).background(
                if (index < enteredDigits) PremiumVisualTokens.darkCoffee
                else PremiumVisualTokens.darkSurface,
                androidx.compose.foundation.shape.CircleShape))
        }
    }
}

@Composable
private fun LockNumberPad(
    biometricAvailable: Boolean,
    onDigit: (Char) -> Unit, onDelete: () -> Unit, onFingerprint: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("123", "456", "789").forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { digit -> LockDigitKey(digit) { onDigit(digit) } }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (biometricAvailable) IconButton(onClick = onFingerprint,
                    modifier = Modifier.size(PesaDesignTokens.lockKeypadKeySize)) {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = "Unlock with fingerprint",
                        tint = PremiumVisualTokens.darkCoffee, modifier = Modifier.size(24.dp))
                }
            }
            LockDigitKey('0') { onDigit('0') }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                IconButton(onClick = onDelete, modifier = Modifier.size(PesaDesignTokens.lockKeypadKeySize)) {
                    Icon(Icons.Rounded.Backspace, contentDescription = "Delete last PIN digit",
                        tint = PremiumVisualTokens.darkMuted, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun RowScope.LockDigitKey(
    digit: Char, onClick: () -> Unit
) {
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Box(Modifier.size(PesaDesignTokens.lockKeypadKeySize).clickable(role = Role.Button,
            onClickLabel = "Enter $digit", onClick = onClick), contentAlignment = Alignment.Center) {
            Text(digit.toString(), fontSize = 24.sp, color = PremiumVisualTokens.darkText)
        }
    }
}

@Composable
private fun LockRecoveryFooter(recoveryAvailable: Boolean, onRecover: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.heightIn(min = PesaDesignTokens.minimumTouchTarget)
            .then(if (recoveryAvailable) Modifier.clickable(onClickLabel =
                "Recover PIN using phone lock", onClick = onRecover) else Modifier)
            .padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text("Forgot PIN?", fontSize = 14.sp,
                color = if (recoveryAvailable) PremiumVisualTokens.darkCoffee
                else PremiumVisualTokens.darkMuted.copy(alpha = .65f))
        }
        Spacer(Modifier.height(8.dp))
        Text("Your financial data stays on this phone.",
            fontSize = 11.sp, textAlign = TextAlign.Center,
            color = PremiumVisualTokens.darkMuted.copy(alpha = .72f))
    }
}
