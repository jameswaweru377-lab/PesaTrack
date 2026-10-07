package com.kedevjimmy.pesatrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ConceptEmerald = Color(0xFF23C483)
private val ConceptEmeraldDeep = Color(0xFF0A5F45)
private val ConceptEmeraldDarker = Color(0xFF151D1A)
private val ConceptMint = Color(0xFF8DE7BF)
private val ConceptMintSoft = Color(0xFFE8F8EF)
private val ConceptCream = Color(0xFFF7F7F2)

@Composable
fun ConceptWalletIllustration(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(com.kedevjimmy.pesatrack.R.drawable.wallet_welcome),
        contentDescription = "Emerald wallet on a pedestal with leaves",
        modifier = modifier.size(if (compact) 112.dp else 310.dp),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit
    )
}

@Composable
fun ConceptMessagesIllustration(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(185.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(118.dp)
                .height(166.dp)
                .rotate(-7f),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFE9F7EF),
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.PhoneAndroid,
                    contentDescription = null,
                    tint = ConceptEmerald,
                    modifier = Modifier.size(76.dp)
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-26).dp)
                        .width(72.dp)
                        .height(30.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF48C983)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Message,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 26.dp)
                        .width(66.dp)
                        .height(26.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White
                ) {}
            }
        }
        ConceptWalletIllustration(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = 78.dp, y = 4.dp),
            compact = true
        )
        Icon(
            Icons.Rounded.Eco,
            contentDescription = null,
            tint = Color(0xFF2BA968),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 86.dp, y = 10.dp)
                .size(34.dp)
                .rotate(-25f)
        )
    }
}

@Composable
fun ConceptBrandLockup(
    modifier: Modifier = Modifier,
    light: Boolean = false,
    subtitle: String? = null
) {
    val foreground = if (light) Color.White else MaterialTheme.colorScheme.onBackground
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF80F0A4), Color(0xFF22A865))
                    ),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Icon(
                Icons.Rounded.Eco,
                contentDescription = null,
                tint = Color(0xFFB9FF87),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-7).dp)
                    .size(27.dp)
                    .rotate(-22f)
            )
        }
        Text(
            "PesaTrack",
            modifier = Modifier.padding(top = 8.dp),
            color = foreground,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                modifier = Modifier.padding(top = 2.dp),
                color = foreground.copy(alpha = .75f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ConceptEmeraldHero(
    title: String,
    amount: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    artwork: Boolean = true,
    eyebrow: String? = null,
    footer: String? = null
) {
    val dark =
        MaterialTheme.colorScheme.background
            .luminance() < .45f
    val foreground =
        if (dark) {
            Color(0xFF0B1507)
        } else {
            Color.White
        }
    val secondaryForeground =
        foreground.copy(
            alpha = .78f
        )
    val gradient =
        if (dark) {
            listOf(
                Color(0xFFDDFB5E),
                Color(0xFFC8F43D),
                Color(0xFFA8D81F)
            )
        } else {
            listOf(
                Color(0xFF0A5A43),
                Color(0xFF08724F),
                Color(0xFF0A8A5C)
            )
        }

    Surface(
        modifier =
            modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                22.dp
            ),
        color =
            if (dark) {
                MaterialTheme
                    .colorScheme
                    .primary
            } else {
                ConceptEmeraldDeep
            }
    ) {
        Box(
            modifier =
                Modifier
                    .background(
                        Brush.linearGradient(
                            gradient
                        )
                    )
                    .padding(16.dp)
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            end =
                                if (artwork) {
                                    104.dp
                                } else {
                                    0.dp
                                }
                        )
            ) {
                eyebrow?.let {
                    Text(
                        it,
                        color =
                            secondaryForeground,
                        style =
                            MaterialTheme
                                .typography
                                .labelMedium
                    )
                    Spacer(
                        Modifier.height(
                            3.dp
                        )
                    )
                }
                Text(
                    title,
                    color = foreground,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.SemiBold
                )
                Text(
                    amount,
                    modifier =
                        Modifier.padding(
                            top = 4.dp
                        ),
                    color = foreground,
                    fontSize = 30.sp,
                    lineHeight = 34.sp,
                    fontWeight =
                        FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    modifier =
                        Modifier.padding(
                            top = 3.dp
                        ),
                    color =
                        secondaryForeground,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
                footer?.let {
                    Text(
                        it,
                        modifier =
                            Modifier.padding(
                                top = 6.dp
                            ),
                        color =
                            foreground.copy(
                                alpha = .66f
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .labelSmall
                    )
                }
            }
            if (artwork) {
                ConceptWalletIllustration(
                    modifier =
                        Modifier
                            .align(
                                Alignment.CenterEnd
                            )
                            .offset(
                                x = 14.dp
                            ),
                    compact = true
                )
            }
        }
    }
}

@Composable
fun ConceptNeutralHero(
    title: String,
    amount: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    icon: ImageVector = Icons.Rounded.AccountBalanceWallet,
    tone: String = "green"
) {
    PesaCard(modifier) {
        Column(
            modifier =
                Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                IconBubble(
                    icon = icon,
                    size = 42.dp,
                    background =
                        pesaToneContainer(
                            tone
                        ),
                    tint =
                        pesaToneContent(
                            tone
                        )
                )
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(
                                start = 11.dp
                            )
                ) {
                    eyebrow?.let {
                        Text(
                            it,
                            style =
                                MaterialTheme
                                    .typography
                                    .labelMedium,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )
                    }
                    Text(
                        title,
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }

            Text(
                amount,
                style =
                    MaterialTheme
                        .typography
                        .headlineLarge,
                fontWeight =
                    FontWeight.ExtraBold
            )
            Text(
                subtitle,
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

@Composable
fun ConceptMetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    positive: Boolean = true
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < .45f
    val soft =
        if (dark) {
            MaterialTheme.colorScheme.surface
        } else if (positive) {
            Color(0xFFE6F8EC)
        } else {
            Color(0xFFFFEBE8)
        }
    val accent =
        if (positive) {
            if (dark) Color(0xFF23C483) else Color(0xFF2DBE62)
        } else {
            if (dark) Color(0xFFF87171) else Color(0xFFE2685B)
        }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = soft
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = accent.copy(alpha = .18f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(21.dp))
                }
            }
            Column(Modifier.padding(start = 10.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ConceptProgressCard(
    title: String,
    amount: String,
    helper: String,
    progress: Float,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Savings
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = ConceptEmerald, modifier = Modifier.size(24.dp))
                Text(
                    title,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(9.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor =
                    if (MaterialTheme.colorScheme.background.luminance() < .45f) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        Color(0xFFD8F1E2)
                    }
            )
            Text(
                helper,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ConceptSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        action?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ConceptDarkPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(15.dp), content = content)
    }
}

fun conceptEmerald(): Color = ConceptEmerald
fun conceptEmeraldDeep(): Color = ConceptEmeraldDeep
fun conceptMint(): Color = ConceptMint
fun conceptCream(): Color = ConceptCream
