package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens

/** First-run welcome: one action, three plain-language benefits, no decorative charts. */
@Composable
internal fun PesaTrackWelcomePage(onGetStarted: () -> Unit, onSkip: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your money, clearer.",
            style = MaterialTheme.typography.headlineLarge,
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
            color = PremiumVisualTokens.ink)
        Text("M-Pesa and bank activity, privately organised on your phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        OnboardingBenefit(Icons.Rounded.TrendingUp, "Track money in and out")
        OnboardingBenefit(Icons.Rounded.AccountBalanceWallet, "Understand balances and transfers")
        OnboardingBenefit(Icons.Rounded.CalendarMonth, "Plan with confidence")
        Spacer(Modifier.height(8.dp))
        Button(onClick = onGetStarted, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(PesaDesignTokens.controlRadius)) {
            Text("Get started", fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Set up later")
        }
        Text("Your data stays on this phone.",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OnboardingBenefit(icon: ImageVector, title: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
        color = PremiumVisualTokens.card,
        border = BorderStroke(1.dp, PremiumVisualTokens.border)
    ) {
        Row(
            Modifier.padding(12.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = PremiumVisualTokens.coffee,
                modifier = Modifier.size(24.dp))
            Text(title, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = PremiumVisualTokens.ink)
        }
    }
}
