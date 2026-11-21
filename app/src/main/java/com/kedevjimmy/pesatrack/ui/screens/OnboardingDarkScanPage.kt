package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens

/** Full-bleed black import view; the actual work determines when it ends. */
@Composable
internal fun OnboardingDarkScanPage(importing: Boolean, phase: String) {
    Box(Modifier.fillMaxSize().background(PremiumVisualTokens.darkCanvas)
        .windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(Modifier.align(Alignment.TopCenter).padding(top = 34.dp, start = 24.dp, end = 24.dp)) {
            OnboardingBrandHeader()
        }
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 5.dp,
                    color = PremiumVisualTokens.darkCoffee,
                    trackColor = PremiumVisualTokens.darkSurface
                )
                Icon(Icons.Rounded.ReceiptLong, contentDescription = "Financial messages",
                    modifier = Modifier.size(55.dp), tint = PremiumVisualTokens.darkCoffee)
                Icon(Icons.Rounded.MonetizationOn, contentDescription = null,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 38.dp, bottom = 38.dp).size(27.dp),
                    tint = PremiumVisualTokens.darkText)
            }
            Spacer(Modifier.height(30.dp))
            Text("Reading your messages", color = PremiumVisualTokens.darkText,
                fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(15.dp))
            Text(phase, color = PremiumVisualTokens.darkCoffee,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(13.dp))
            Text(
                if (importing) "Finding M-PESA and bank transactions on this phone. This may take a moment."
                else "Checking financial records already saved on this phone.",
                color = PremiumVisualTokens.darkMuted, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
