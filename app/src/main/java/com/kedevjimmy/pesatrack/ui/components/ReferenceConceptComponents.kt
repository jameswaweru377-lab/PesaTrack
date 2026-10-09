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

private val ConceptEmerald = Color(0xFF5B55E7)
private val ConceptEmeraldDeep = Color(0xFF24213F)
private val ConceptEmeraldDarker = Color(0xFF121725)
private val ConceptMint = Color(0xFF55E6D1)
private val ConceptMintSoft = Color(0xFFF0EFFF)
private val ConceptCream = Color(0xFFF6F7FB)

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
            color = Color(0xFFF0EFFF),
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
                    color = Color(0xFF7C6FF2)
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
            tint = Color(0xFF14B8A6),
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
                        listOf(Color(0xFFA69CFF), Color(0xFF5B55E7))
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
                tint = Color(0xFF55E6D1),
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
                            Color(0xFF4F46D8),
                            Color(0xFF3933A8),
                            Color(0xFF14B8A6)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            val showArtwork = artwork && androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 380 && androidx.compose.ui.platform.LocalDensity.current.fontScale <= 1.1f
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (showArtwork) 104.dp else 0.dp)
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
                    maxLines = 3,
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
            if (showArtwork) {
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
            if (dark) Color(0xFF252243) else Color(0xFFF0EFFF)
        } else {
            if (dark) Color(0xFF3A2330) else Color(0xFFFFE9EE)
        }
    val accent =
        if (positive) {
            if (dark) Color(0xFFA69CFF) else Color(0xFF5B55E7)
        } else {
            if (dark) Color(0xFFFF92A3) else Color(0xFFEF5F78)
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
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
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
                trackColor = MaterialTheme.colorScheme.primaryContainer
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
