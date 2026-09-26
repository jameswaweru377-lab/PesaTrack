package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.BrandWordmark
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.LandscapeBackdrop
import com.kedevjimmy.pesatrack.ui.components.SmallStatusPill
import com.kedevjimmy.pesatrack.ui.theme.AmberSoft
import com.kedevjimmy.pesatrack.ui.theme.MintSoft
import com.kedevjimmy.pesatrack.ui.theme.SkySoft

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    LazyColumn(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BrandWordmark()
        }

        item {
            LandscapeBackdrop(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1.05f)) {
                        Text(
                            "Connect",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "your money",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "We read your M-Pesa messages privately on your phone to help you track spending, save more and stay in control.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        Modifier
                            .weight(.95f)
                            .padding(start = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(width = 112.dp, height = 190.dp),
                            shape = RoundedCornerShape(28.dp),
                            color = Color(0xFF0B6C56),
                            shadowElevation = 5.dp
                        ) {
                            Box(Modifier.padding(10.dp)) {
                                Surface(
                                    modifier = Modifier.matchParentSize(),
                                    shape = RoundedCornerShape(22.dp),
                                    color = Color.White
                                ) {}
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 22.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MintSoft
                                ) {
                                    Text(
                                        "M-PESA",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFF0B7A4B),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(76.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 6.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { SetupAction(Icons.Rounded.Sms, "Import M-Pesa messages", "Allow access so we can read and process your M-Pesa messages on your phone.", "green") }
        item { SetupAction(Icons.Rounded.Notifications, "Enable notifications", "Get helpful alerts for unusual spending, bills and upcoming payments.", "amber") }
        item { SetupAction(Icons.Rounded.AccountBalanceWallet, "Set your monthly income", "Help us understand your money and show personalised insights.", "blue") }
        item { SetupAction(Icons.Rounded.Settings, "Create smart rules", "We'll automatically categorise your transactions using simple rules.", "green") }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MintSoft
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBubble(Icons.Rounded.Shield, size = 48.dp, background = Color.White.copy(alpha = .72f))
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 11.dp)
                    ) {
                        Text("Your privacy comes first", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Your M-Pesa messages are read and processed locally on your phone. They never leave your device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        Text("100%", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text("On-device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = AmberSoft
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(Icons.Rounded.Lightbulb, size = 42.dp, background = Color.White.copy(alpha = .65f))
                        Column(Modifier.padding(start = 10.dp)) {
                            Text("Example smart rules", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Payments like these can be categorised automatically:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    RuleExample(Icons.Rounded.Groups, "Family Wings", "KES 6,000 · 12 Sep", "Rent", Icons.Rounded.AccountBalanceWallet, "amber")
                    RuleExample(Icons.Rounded.Wifi, "Planet Internet", "KES 1,500 · 10 Sep", "Internet", Icons.Rounded.Wifi, "blue")
                    RuleExample(Icons.Rounded.Restaurant, "Elizabeth", "KES 2,900 · 5 Sep", "Lunch", Icons.Rounded.Restaurant, "green")
                }
            }
        }

        item {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Get started")
                Icon(Icons.Rounded.ArrowForward, null, modifier = Modifier.padding(start = 8.dp))
            }
        }

        item {
            OutlinedButton(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("I'll do this later")
            }
        }
    }
}

@Composable
private fun SetupAction(
    icon: ImageVector,
    title: String,
    body: String,
    tone: String
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                icon,
                size = 48.dp,
                background = when (tone) {
                    "amber" -> AmberSoft
                    "blue" -> SkySoft
                    else -> MintSoft
                }
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RuleExample(
    icon: ImageVector,
    name: String,
    meta: String,
    category: String,
    categoryIcon: ImageVector,
    tone: String
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(
            icon,
            size = 38.dp,
            background = when (tone) {
                "amber" -> AmberSoft
                "blue" -> SkySoft
                else -> MintSoft
            }
        )
        Column(
            Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(name, style = MaterialTheme.typography.titleSmall)
            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.padding(horizontal = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(categoryIcon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(
                category,
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
