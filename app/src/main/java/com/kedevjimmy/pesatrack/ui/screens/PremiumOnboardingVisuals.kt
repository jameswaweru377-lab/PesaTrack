package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Single design language for the approved four-page onboarding treatment.
 * Reads MaterialTheme colors, so the exact geometry also adapts to light mode.
 */
@Composable
internal fun PremiumOnboardingHero(kind: String, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.colorScheme
    val lavender = palette.primary
    val turquoise = palette.secondary
    val surface = palette.surface
    val isWelcome = kind == "welcome"
    val isPermission = kind == "permission"
    Box(modifier.fillMaxWidth().height(if (isWelcome) 186.dp else 162.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.align(Alignment.Center).size(width = 224.dp, height = 148.dp)
                .background(
                    Brush.radialGradient(listOf(palette.primaryContainer.copy(alpha = 0.9f), Color.Transparent)),
                    RoundedCornerShape(90.dp)
                )
        )
        Surface(
            modifier = Modifier.align(Alignment.Center).size(
                width = if (isWelcome) 104.dp else 110.dp,
                height = if (isWelcome) 142.dp else 132.dp
            ).shadow(14.dp, RoundedCornerShape(24.dp)),
            color = palette.primaryContainer, shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, lavender.copy(alpha = .58f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PhoneAndroid, null, modifier = Modifier.size(68.dp),
                    tint = palette.onPrimaryContainer)
                if (isPermission) {
                    Surface(
                        Modifier.align(Alignment.BottomEnd).padding(4.dp).size(36.dp),
                        shape = CircleShape, color = palette.surface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Shield, null, tint = turquoise, modifier = Modifier.size(25.dp))
                        }
                    }
                } else if (!isWelcome) {
                    Surface(
                        Modifier.align(Alignment.BottomEnd).padding(4.dp).size(32.dp),
                        shape = CircleShape, color = palette.surface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Search, null, tint = lavender, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
        // These side cards are illustrations, not real financial data.
        val leftIcon = if (isPermission) Icons.AutoMirrored.Rounded.Message else Icons.Rounded.AccountBalance
        Surface(
            modifier = Modifier.align(Alignment.CenterStart).offset(y = 22.dp).size(width = 112.dp, height = 67.dp)
                .shadow(8.dp, RoundedCornerShape(14.dp)),
            color = palette.surface, shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, turquoise.copy(alpha = .65f))
        ) {
            Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(leftIcon, null, tint = turquoise, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(5.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (isPermission) "SMS" else "M-Pesa", style = MaterialTheme.typography.labelSmall,
                        color = palette.onSurface, fontWeight = FontWeight.Bold)
                    Box(Modifier.width(43.dp).height(5.dp).background(turquoise.copy(alpha = .45f), CircleShape))
                    Box(Modifier.width(28.dp).height(4.dp).background(turquoise.copy(alpha = .25f), CircleShape))
                }
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.CenterEnd).offset(y = 5.dp).size(width = 106.dp, height = 65.dp)
                .shadow(8.dp, RoundedCornerShape(14.dp)),
            color = palette.primaryContainer, shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, lavender.copy(alpha = .7f))
        ) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isPermission) Icons.Rounded.Shield else Icons.Rounded.AccountBalance,
                    null, tint = palette.onPrimaryContainer, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(5.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (isPermission) "Private" else "Bank", style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold, color = palette.onPrimaryContainer)
                    Box(Modifier.width(41.dp).height(5.dp).background(lavender.copy(alpha = .55f), CircleShape))
                    Box(Modifier.width(26.dp).height(4.dp).background(lavender.copy(alpha = .3f), CircleShape))
                }
            }
        }
    }
}

@Composable
internal fun PremiumFeatureRow(icon: ImageVector, title: String, description: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(46.dp), shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(23.dp))
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
        }
    }
}

@Composable
internal fun PremiumInfoPanel(icon: ImageVector, title: String, detail: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(40.dp), shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall, lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun PremiumHistoryTimeline() {
    val labels = (2 downTo 0).map { ago ->
        val calendar = Calendar.getInstance().apply { add(Calendar.MONTH, -ago) }
        val label = SimpleDateFormat("MMM", Locale.ENGLISH).format(calendar.time)
        Pair(label, calendar.get(Calendar.YEAR).toString())
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        labels.forEachIndexed { index, pair ->
            Surface(
                modifier = Modifier.weight(1f).height(90.dp),
                shape = RoundedCornerShape(15.dp),
                color = if (index == 2) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, if (index == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(5.dp))
                    Text(pair.first, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(if (index == 2) "To date" else pair.second,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun PremiumRangeOption(
    title: String, description: String, selected: Boolean, onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
