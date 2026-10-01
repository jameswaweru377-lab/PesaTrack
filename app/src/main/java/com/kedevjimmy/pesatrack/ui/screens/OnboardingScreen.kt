package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.BrandWordmark
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.LandscapeBackdrop
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
            .windowInsetsPadding(
                WindowInsets.safeDrawing
            ),
        contentPadding =
            PaddingValues(
                horizontal = 18.dp,
                vertical = 20.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        item {
            BrandWordmark()
        }

        item {
            LandscapeBackdrop(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Text(
                        "Your money.",
                        style =
                            MaterialTheme.typography
                                .displaySmall,
                        fontWeight =
                            FontWeight.Bold
                    )
                    Text(
                        "In one place.",
                        style =
                            MaterialTheme.typography
                                .displaySmall,
                        color =
                            MaterialTheme.colorScheme
                                .primary,
                        fontWeight =
                            FontWeight.Bold
                    )
                    Text(
                        "PesaTrack turns M-Pesa, bank and other financial messages into one clear picture of your money.",
                        modifier =
                            Modifier.padding(
                                top = 12.dp
                            ),
                        style =
                            MaterialTheme.typography
                                .bodyLarge,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        Modifier.weight(1f)
                    )

                    PesaCard {
                        Row(
                            modifier =
                                Modifier.padding(14.dp)
                        ) {
                            IconBubble(
                                Icons.Rounded
                                    .AutoAwesome,
                                size = 44.dp,
                                background =
                                    pesaToneContainer(
                                        "green"
                                    ),
                                tint =
                                    pesaToneContent(
                                        "green"
                                    )
                            )
                            Column(
                                modifier =
                                    Modifier.padding(
                                        start = 11.dp
                                    )
                            ) {
                                Text(
                                    "Setup that learns",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium,
                                    fontWeight =
                                        FontWeight
                                            .SemiBold
                                )
                                Text(
                                    "PesaTrack finds what it can first, then asks only when it needs your confirmation.",
                                    modifier =
                                        Modifier.padding(
                                            top = 3.dp
                                        ),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            PesaCard {
                Column(
                    modifier =
                        Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(14.dp)
                ) {
                    WelcomeBenefit(
                        icon =
                            Icons.Rounded.Payments,
                        title =
                            "Understand real activity",
                        body =
                            "Import recent money messages or start fresh and let PesaTrack learn over time."
                    )
                    WelcomeBenefit(
                        icon =
                            Icons.Rounded.Lock,
                        title =
                            "Private by design",
                        body =
                            "Financial messages are interpreted on your phone."
                    )
                }
            }
        }

        item {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp),
                shape =
                    RoundedCornerShape(18.dp)
            ) {
                Text("Get started")
                androidx.compose.material3.Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier =
                        Modifier.padding(
                            start = 8.dp
                        )
                )
            }
        }

        item {
            Text(
                "No long form. No fixed payday required. You can change everything later in Settings.",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WelcomeBenefit(
    icon:
        androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        IconBubble(
            icon,
            size = 42.dp
        )
        Column(
            modifier =
                Modifier.padding(start = 11.dp)
        ) {
            Text(
                title,
                style =
                    MaterialTheme.typography
                        .titleMedium,
                fontWeight =
                    FontWeight.SemiBold
            )
            Text(
                body,
                modifier =
                    Modifier.padding(top = 3.dp),
                style =
                    MaterialTheme.typography
                        .bodyMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}
