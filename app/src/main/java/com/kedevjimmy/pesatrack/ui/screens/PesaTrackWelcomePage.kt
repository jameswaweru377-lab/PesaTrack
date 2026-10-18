package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** First page of the approved three-screen onboarding design. */
@Composable
internal fun PesaTrackWelcomePage(onGetStarted: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(116.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(94.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.TopStart)
                    .padding(start = 8.dp).size(42.dp),
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Message,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = 7.dp).size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Text(
            "Welcome to PesaTrack",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 29.sp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "A clearer view of your money, from the accounts you already use.",
            modifier = Modifier.widthIn(max = 310.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Column(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WelcomeFeature(Icons.AutoMirrored.Rounded.Message, "Bring in M-Pesa and bank activity")
            WelcomeFeature(Icons.Rounded.AccountBalance, "See spending, income and debts together")
            WelcomeFeature(Icons.Rounded.Savings, "Set up savings and salary tracking when ready")
        }

        Spacer(Modifier.height(2.dp))
        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth().heightIn(min = 49.dp),
            shape = RoundedCornerShape(15.dp)
        ) { Text("Get started", fontWeight = FontWeight.Bold) }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("Skip for now", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WelcomeFeature(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(11.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp))
            }
        }
        Text(
            title,
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
