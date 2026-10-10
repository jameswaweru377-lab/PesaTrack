package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.PesaCard

/** Approved welcome screen: the user can proceed or skip without granting access. */
@Composable
internal fun PesaTrackWelcomePage(onGetStarted: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(148.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(width = 86.dp, height = 116.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PhoneAndroid, null,
                        modifier = Modifier.size(68.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = 56.dp, bottom = 5.dp).size(58.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Search, null, modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.TopStart)
                    .padding(start = 48.dp, top = 19.dp).size(34.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.Message, null,
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Text(
            "Welcome to\nPesaTrack",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "We'll help you track your income, expenses, debts and savings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Column(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            WelcomeFeature(Icons.AutoMirrored.Rounded.Message, "Scan your SMS (with your permission)")
            WelcomeFeature(Icons.Rounded.AccountBalance, "Find your banks and M-Pesa accounts")
            WelcomeFeature(Icons.Rounded.Search, "Identify your accounts")
            WelcomeFeature(Icons.Rounded.Savings, "Optionally set up salary tracking")
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Get started") }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Skip for now") }
    }
}

@Composable
private fun WelcomeFeature(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp))
            }
        }
        Text(
            title,
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
