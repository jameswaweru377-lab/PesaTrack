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

private val ConceptEmerald = Color(0xFF087A58)
private val ConceptEmeraldDeep = Color(0xFF053D32)
private val ConceptEmeraldDarker = Color(0xFF032E27)
private val ConceptMint = Color(0xFF72F1B3)
private val ConceptMintSoft = Color(0xFFE8F8EF)
private val ConceptCream = Color(0xFFF7F7F2)

@Composable
fun ConceptWalletIllustration(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val width = if (compact) 112.dp else 190.dp
    val height = if (compact) 92.dp else 150.dp
    Box(
        modifier = modifier
            .size(width = width, height = height),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(if (compact) 92.dp else 155.dp)
                .height(if (compact) 55.dp else 92.dp),
            shape = RoundedCornerShape(if (compact) 14.dp else 22.dp),
            color = ConceptEmeraldDeep,
            tonalElevation = 8.dp
        ) {}
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = if (compact) (-6).dp else (-10).dp)
                .width(if (compact) 76.dp else 126.dp)
                .height(if (compact) 42.dp else 67.dp)
                .rotate(-8f),
            shape = RoundedCornerShape(if (compact) 9.dp else 14.dp),
            color = Color(0xFF4AC785)
        ) {}
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = if (compact) 26.dp else 42.dp, y = if (compact) 4.dp else 8.dp)
                .size(if (compact) 22.dp else 34.dp),
            shape = CircleShape,
            color = Color(0xFFC99A4B)
        ) {}
        Icon(
            imageVector = Icons.Rounded.Eco,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = if (compact) 20.dp else 32.dp)
                .size(if (compact) 34.dp else 54.dp)
                .rotate(-18f),
            tint = Color(0xFF7AE27F)
        )
        Icon(
            imageVector = Icons.Rounded.LocalFlorist,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = if (compact) 1.dp else 4.dp, y = if (compact) 3.dp else 8.dp)
                .size(if (compact) 30.dp else 44.dp),
            tint = Color(0xFF2E9E63)
        )
    }
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = ConceptEmeraldDeep
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF0A6F50),
                            Color(0xFF066344),
                            Color(0xFF0A8560)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (artwork) 104.dp else 0.dp)
            ) {
                eyebrow?.let {
                    Text(
                        it,
                        color = Color.White.copy(alpha = .82f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(3.dp))
                }
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    amount,
                    modifier = Modifier.padding(top = 4.dp),
                    color = Color.White,
                    fontSize = 30.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    modifier = Modifier.padding(top = 3.dp),
                    color = Color.White.copy(alpha = .8f),
                    style = MaterialTheme.typography.bodySmall
                )
                footer?.let {
                    Text(
                        it,
                        modifier = Modifier.padding(top = 6.dp),
                        color = Color.White.copy(alpha = .72f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            if (artwork) {
                ConceptWalletIllustration(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 14.dp),
                    compact = true
                )
            }
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
        if (positive) {
            if (dark) Color(0xFF0A4B3C) else Color(0xFFE6F8EC)
        } else {
            if (dark) Color(0xFF563D34) else Color(0xFFFFEBE8)
        }
    val accent =
        if (positive) {
            if (dark) Color(0xFF79F2B1) else Color(0xFF2DBE62)
        } else {
            if (dark) Color(0xFFFF9D82) else Color(0xFFE2685B)
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
                color = Color(0xFF31BE73),
                trackColor = Color(0xFFD8F1E2)
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
        color = ConceptEmeraldDarker
    ) {
        Column(Modifier.padding(15.dp), content = content)
    }
}

fun conceptEmerald(): Color = ConceptEmerald
fun conceptEmeraldDeep(): Color = ConceptEmeraldDeep
fun conceptMint(): Color = ConceptMint
fun conceptCream(): Color = ConceptCream
