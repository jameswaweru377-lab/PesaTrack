package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.PayCycle
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Selected Pay Cycle B. Estimates are explicitly labelled, never invented dates. */
@Composable
internal fun SelectedPayCycleTimeline(cycle: PayCycle, estimatedDays: Int?) {
    val p = HomeMonochromePalette
    val active = cycle.endEpochMs == null
    val headline = when {
        !active -> "Completed pay cycle"
        estimatedDays == null -> "Next payday not confirmed"
        else -> "About $estimatedDays days until payday"
    }
    Surface(shape = RoundedCornerShape(PesaDesignTokens.cardRadius), color = p.softGreen) {
        Column(Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(PesaDesignTokens.iconRadius),
                    color = Color.White) {
                    Box(Modifier.size(PesaDesignTokens.iconBubble), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = p.green)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(headline, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium, color = p.ink)
                    Text(if (!active) "Based on recorded salary dates" else
                        if (estimatedDays == null) "Add a payday estimate in Salary settings" else
                            "Estimated from your completed cycles",
                        style = MaterialTheme.typography.bodySmall, color = p.muted)
                }
            }
            SelectedTimelineStop("Salary received",
                SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(cycle.startEpochMs)),
                completed = true, current = false)
            SelectedTimelineStop(if (active) "Today" else "Cycle ended",
                if (active) SimpleDateFormat("EEE, d MMM", Locale.getDefault())
                    .format(Date()) else SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                    .format(Date(requireNotNull(cycle.endEpochMs))),
                completed = !active, current = active)
            if (active) SelectedTimelineStop("Next payday",
                estimatedDays?.let { "Estimated in about $it days" }
                    ?: "No reliable estimate yet", completed = false, current = false)
        }
    }
}

@Composable
private fun SelectedTimelineStop(title: String, detail: String,
    completed: Boolean, current: Boolean) {
    val p = HomeMonochromePalette
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(shape = CircleShape, color = if (current || completed) p.green else Color.White,
            border = BorderStroke(1.dp, p.green)) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                if (completed) Icon(Icons.Rounded.Check, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(16.dp))
                else if (current) Surface(shape = CircleShape, color = Color.White,
                    modifier = Modifier.size(8.dp)) {}
            }
        }
        Surface(shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
            color = if (current) Color(0xFFD8F2E2) else Color.White,
            modifier = Modifier.weight(1f)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall, color = p.ink)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = p.muted)
            }
        }
    }
}

@Composable
internal fun SelectedPayCycleSnapshot(
    income: String,
    expenses: String,
    setAside: String,
    available: String?,
    footnote: String
) {
    val p = HomeMonochromePalette
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Cycle snapshot", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = p.ink)
        Surface(shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
            border = BorderStroke(1.dp, p.line), color = Color.White) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                SelectedCycleMetric(Icons.Rounded.Wallet, "Income received", income)
                HorizontalDivider(color = p.line)
                SelectedCycleMetric(Icons.Rounded.ShoppingCart, "Expenses", expenses)
                HorizontalDivider(color = p.line)
                SelectedCycleMetric(Icons.Rounded.Savings, "Set aside", setAside)
                HorizontalDivider(color = p.line)
                SelectedCycleMetric(Icons.Rounded.Wallet, "Available to plan",
                    available ?: "Unavailable", important = true)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Info, null, tint = p.green, modifier = Modifier.size(20.dp))
            Text(footnote, color = p.muted, style = MaterialTheme.typography.bodySmall)
        }
        Text("Own-account transfers are excluded from income and expenses.",
            style = MaterialTheme.typography.bodySmall, color = p.muted)
    }
}

@Composable
private fun SelectedCycleMetric(icon: ImageVector, label: String, value: String,
    important: Boolean = false) {
    val p = HomeMonochromePalette
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = p.softGreen, shape = RoundedCornerShape(PesaDesignTokens.iconRadius)) {
            Box(Modifier.size(PesaDesignTokens.iconBubble), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = p.green)
            }
        }
        Text(label, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (important) FontWeight.Bold else FontWeight.Medium,
            color = if (important) p.green else p.ink)
        Text(value, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = if (important) p.green else p.ink)
    }
}
