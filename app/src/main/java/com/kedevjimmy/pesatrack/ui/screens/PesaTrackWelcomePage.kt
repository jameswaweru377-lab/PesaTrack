package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens

/** Approved first-run screen: visual introduction followed by two short benefits. */
@Composable
internal fun PesaTrackWelcomePage(onGetStarted: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PremiumWelcomeHero()
        Text(
            "Your money, clearer.",
            style = MaterialTheme.typography.headlineLarge,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = PremiumVisualTokens.ink
        )
        Text(
            "Private money tracking for M-Pesa and bank activity in one place.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        OnboardingBenefit(Icons.Rounded.TrendingUp, "Track money in and out")
        OnboardingBenefit(
            Icons.Rounded.AccountBalanceWallet,
            "Understand balances and transfers"
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(PesaDesignTokens.controlRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = PremiumVisualTokens.coffee, contentColor = Color.White
            )
        ) {
            Text("Get started", fontWeight = FontWeight.SemiBold)
        }
        TextButton(
            onClick = onSkip, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text("Set up later")
        }
    }
}

@Composable
private fun OnboardingBenefit(icon: ImageVector, title: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
        color = PremiumVisualTokens.card,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
                color = PremiumVisualTokens.sand
            ) {
                Icon(
                    icon, contentDescription = null, tint = PremiumVisualTokens.coffee,
                    modifier = Modifier.padding(8.dp).size(24.dp)
                )
            }
            Text(
                title, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = PremiumVisualTokens.ink
            )
        }
    }
}
