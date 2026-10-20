package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

/** Approved first onboarding page. The parent owns step navigation and the real skip path. */
@Composable
internal fun PesaTrackWelcomePage(onGetStarted: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            "Your money,",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 33.sp, lineHeight = 36.sp),
            fontWeight = FontWeight.Bold
        )
        Text(
            "clearer.",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 33.sp, lineHeight = 36.sp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Bring M-Pesa and bank activity together in one private space.",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PremiumOnboardingHero("welcome")
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            PremiumFeatureRow(
                Icons.Rounded.AccountBalance,
                "Track money in and out",
                "See your income and spending in one place."
            )
            PremiumFeatureRow(
                Icons.Rounded.Savings,
                "Understand spending and debts",
                "Get a clear view of your finances."
            )
            PremiumFeatureRow(
                Icons.Rounded.Shield,
                "Plan with confidence",
                "Set up savings and track your goals when ready."
            )
        }
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Get started  →", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Set up later", fontWeight = FontWeight.SemiBold)
        }
    }
}
