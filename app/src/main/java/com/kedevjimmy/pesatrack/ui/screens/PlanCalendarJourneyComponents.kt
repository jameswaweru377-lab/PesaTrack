package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens

internal data class PlanJourneyBill(
    val label: String,
    val dueLabel: String,
    val amount: String,
    val dueEpochMs: Long?
)

internal data class PlanJourneyView(
    val month: String,
    val period: String,
    val hasCycle: Boolean,
    val available: String,
    val starting: String,
    val committed: String,
    val saved: String,
    val received: String,
    val spent: String,
    val fees: String,
    val safeToday: String?,
    val billsPaidPercent: Int?,
    val budgetUsed: String?,
    val budgetLimit: String?,
    val budgetPercent: Int?,
    val goalSaved: String?,
    val goalTarget: String?,
    val goalPercent: Int?,
    val displayedShares: List<Int>?,
    val bills: List<PlanJourneyBill>,
    val week: List<PlanCalendarDay>
)

@Composable
internal fun PlanJourneyOverview(
    data: PlanJourneyView,
    selectedPeriod: String,
    onPeriod: (String) -> Unit,
    onOtherMonths: () -> Unit,
    onBudget: () -> Unit,
    onGoals: () -> Unit,
    onBills: () -> Unit,
    onDetails: () -> Unit,
    onSalary: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Plan your month", modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = HomeMonochromePalette.ink)
            PlanJourneyPeriodToggle(selectedPeriod, onPeriod)
        }
        PlanJourneyMonthMenu(data.month, onOtherMonths)
        PlanJourneyWeek(data.week)
        if (data.hasCycle) {
            PlanJourneyAvailableCard(data, onDetails)
        } else {
            Surface(color = HomeMonochromePalette.softGreen,
                shape = RoundedCornerShape(PesaDesignTokens.cardRadius)) {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Start your financial plan", fontWeight = FontWeight.Bold)
                    Text("Add a confirmed salary to calculate your available money.",
                        style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onSalary) { Text("Add confirmed salary") }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlanJourneyProgressTile("Budget", data.budgetUsed, data.budgetLimit,
                data.budgetPercent, Icons.Rounded.Wallet, onBudget, Modifier.weight(1f))
            PlanJourneyProgressTile("Goals", data.goalSaved, data.goalTarget,
                data.goalPercent, Icons.Rounded.Flag, onGoals, Modifier.weight(1f))
        }
        PlanJourneyBillsButton(data.bills, data.billsPaidPercent, onBills)
        PlanJourneyBillSection("Coming up next", data.bills.take(2), onBills)
        if (data.hasCycle) {
            Surface(color = HomeMonochromePalette.softGreen,
                shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onDetails)) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Plan details", fontWeight = FontWeight.SemiBold,
                        color = HomeMonochromePalette.ink)
                    Icon(Icons.Rounded.ChevronRight, null, tint = HomeMonochromePalette.ink)
                }
            }
        }
    }
}

@Composable
internal fun PlanJourneyDetails(
    data: PlanJourneyView,
    onBack: () -> Unit,
    onOtherMonths: () -> Unit,
    onBills: () -> Unit,
    onPayCycle: () -> Unit
) {
    val p = HomeMonochromePalette
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to Plan", tint = p.ink)
            }
            Text("${data.month.substringBefore(' ')} plan", modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = p.ink)
        }
        PlanJourneyMonthMenu(data.month, onOtherMonths)
        PlanJourneyWeek(data.week)
        PlanJourneyBreakdown(data)
        PlanJourneyAllocations(data)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(data.period, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = p.muted)
            TextButton(onClick = onPayCycle) { Text("Manage pay cycle") }
        }
        PlanJourneyBillSection("Bills for this plan", data.bills, onBills)
    }
}

@Composable
private fun PlanJourneyMonthMenu(month: String, onOtherMonths: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(shape = RoundedCornerShape(PesaDesignTokens.controlRadius),
            color = HomeMonochromePalette.surface,
            border = BorderStroke(1.dp, HomeMonochromePalette.line),
            modifier = Modifier.heightIn(min = 48.dp)
                .clickable(role = Role.Button) { expanded = true }) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(month, color = HomeMonochromePalette.ink)
                Icon(Icons.Rounded.KeyboardArrowDown, null,
                    tint = HomeMonochromePalette.ink)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("$month · current month") },
                onClick = { expanded = false })
            DropdownMenuItem(text = { Text("View other months in Reports") },
                onClick = { expanded = false; onOtherMonths() })
        }
    }
}

@Composable
private fun PlanJourneyPeriodToggle(selected: String, onPeriod: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(PesaDesignTokens.controlRadius),
        border = BorderStroke(1.dp, HomeMonochromePalette.line),
        color = HomeMonochromePalette.surface) {
        Row {
            listOf("Month", "Pay cycle").forEach { mode ->
                val active = selected == mode
                Surface(shape = RoundedCornerShape(PesaDesignTokens.controlRadius),
                    color = if (active) HomeMonochromePalette.green else Color.Transparent,
                    modifier = Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) {
                        onPeriod(mode)
                    }) {
                    Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center) {
                        Text(mode, style = MaterialTheme.typography.labelSmall,
                            color = if (active) Color.White else HomeMonochromePalette.ink,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanJourneyWeek(days: List<PlanCalendarDay>) {
    var chosen by remember(days) { mutableStateOf(days.firstOrNull()?.startEpochMs) }
    val p = HomeMonochromePalette
    Surface(shape = RoundedCornerShape(PesaDesignTokens.cardRadius), color = p.softGreen) {
        Row(Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            days.forEach { day ->
                val selected = chosen == day.startEpochMs
                Surface(modifier = Modifier.weight(1f)
                    .heightIn(min = PesaDesignTokens.bottomBarHeight + 16.dp)
                    .clickable(role = Role.Button) { chosen = day.startEpochMs },
                    color = if (selected) p.green else Color.Transparent,
                    shape = RoundedCornerShape(PesaDesignTokens.innerRadius)) {
                    Column(Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val textColor = if (selected) Color.White else p.ink
                        Text(day.weekday, style = MaterialTheme.typography.labelSmall,
                            color = textColor, maxLines = 1)
                        Text(day.dayOfMonth, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = textColor)
                        Text(day.month, style = MaterialTheme.typography.labelSmall,
                            color = textColor, maxLines = 1)
                        if (day.hasBill) {
                            Box(Modifier.size(8.dp).background(
                                if (selected) Color.White else p.green,
                                RoundedCornerShape(PesaDesignTokens.controlRadius)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanJourneyAvailableCard(data: PlanJourneyView, onDetails: () -> Unit) {
    val p = HomeMonochromePalette
    Surface(shape = RoundedCornerShape(PesaDesignTokens.cardRadius), color = p.softGreen,
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onDetails)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.Spa, null, Modifier.size(32.dp), tint = p.green)
                Column {
                    Text("Available", color = p.ink,
                        style = MaterialTheme.typography.bodyMedium)
                    Text(data.available, color = p.green,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = p.line)
            Row(Modifier.fillMaxWidth()) {
                PlanJourneyMiniFact("Starting fund", data.starting, Modifier.weight(1f))
                PlanJourneyMiniFact("Committed", data.committed, Modifier.weight(1f))
                PlanJourneyMiniFact("Saved", data.saved, Modifier.weight(1f))
            }
            Text("Available refers to your current confirmed pay cycle, not a bank balance.",
                style = MaterialTheme.typography.labelSmall, color = p.muted)
        }
    }
}

@Composable
private fun PlanJourneyMiniFact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.labelMedium,
            color = HomeMonochromePalette.ink, fontWeight = FontWeight.SemiBold,
            maxLines = 2)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = HomeMonochromePalette.muted)
    }
}

@Composable
private fun PlanJourneyProgressTile(
    title: String, used: String?, limit: String?, percent: Int?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit, modifier: Modifier
) {
    val p = HomeMonochromePalette
    Surface(modifier = modifier.heightIn(min = PesaDesignTokens.bottomBarHeight + 48.dp)
        .clickable(role = Role.Button, onClick = onClick),
        color = p.surface, shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
        border = BorderStroke(1.dp, p.line)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, Modifier.size(24.dp), tint = p.green)
                Text(title, fontWeight = FontWeight.Bold, color = p.ink,
                    style = MaterialTheme.typography.bodyMedium)
            }
            Text(if (used != null && limit != null) "$used / $limit" else "No progress yet",
                style = MaterialTheme.typography.labelSmall, color = p.muted,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            LinearProgressIndicator(progress = { (percent ?: 0).coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp), color = p.green,
                trackColor = p.softGreen)
            Text(if (percent == null) "Set up" else "${percent.coerceAtLeast(0)}%",
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelSmall, color = p.ink)
        }
    }
}

@Composable
private fun PlanJourneyBillsButton(
    bills: List<PlanJourneyBill>, paidPercent: Int?, onClick: () -> Unit
) {
    Surface(shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
        border = BorderStroke(1.dp, HomeMonochromePalette.line),
        color = HomeMonochromePalette.surface,
        modifier = Modifier.fillMaxWidth().heightIn(min = PesaDesignTokens.bottomBarHeight)
            .clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.CalendarMonth, null, tint = HomeMonochromePalette.green)
            Column(Modifier.weight(1f)) {
                Text("Bills", fontWeight = FontWeight.Bold, color = HomeMonochromePalette.ink)
                Text(if (bills.isEmpty()) "No upcoming payments"
                    else "${bills.size} upcoming payment${if (bills.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = HomeMonochromePalette.muted)
                if (paidPercent != null) {
                    Text("${paidPercent.coerceIn(0, 100)}% of this month's scheduled bills paid",
                        style = MaterialTheme.typography.labelSmall,
                        color = HomeMonochromePalette.muted)
                }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = HomeMonochromePalette.ink)
        }
    }
}

@Composable
private fun PlanJourneyBillSection(
    title: String, bills: List<PlanJourneyBill>, onClick: () -> Unit
) {
    val p = HomeMonochromePalette
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium,
            color = p.ink, fontWeight = FontWeight.Bold)
        if (bills.isEmpty()) {
            Text("No upcoming bills. You're all clear for now.",
                style = MaterialTheme.typography.bodyMedium, color = p.muted)
        } else bills.forEachIndexed { index, bill ->
            Row(Modifier.fillMaxWidth().heightIn(min = PesaDesignTokens.bottomBarHeight)
                .clickable(role = Role.Button, onClick = onClick),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Surface(color = p.softGreen,
                    shape = RoundedCornerShape(PesaDesignTokens.iconRadius)) {
                    Box(Modifier.size(PesaDesignTokens.minimumTouchTarget),
                        contentAlignment = Alignment.Center) {
                        Icon(if (bill.label.contains("rent", ignoreCase = true))
                            Icons.Rounded.Home else Icons.Rounded.ReceiptLong,
                            null, tint = p.green)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(bill.label, color = p.ink, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text(bill.dueLabel, color = p.muted,
                        style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                Text(bill.amount, color = p.ink, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Icon(Icons.Rounded.ChevronRight, null, tint = p.ink)
            }
            if (index != bills.lastIndex) HorizontalDivider(color = p.line)
        }
    }
}

@Composable
private fun PlanJourneyBreakdown(data: PlanJourneyView) {
    val p = HomeMonochromePalette
    Surface(shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
        color = p.surface, border = BorderStroke(1.dp, p.line)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (LocalConfiguration.current.fontScale > 1.15f) {
                listOf(
                    "Starting fund" to data.starting, "Committed" to data.committed,
                    "Saved" to data.saved, "Available" to data.available
                ).chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth()) {
                        pair.forEach { (label, amount) ->
                            PlanJourneyDetailMetric(label, amount, Modifier.weight(1f))
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth()) {
                    listOf(
                        "Starting fund" to data.starting, "Committed" to data.committed,
                        "Saved" to data.saved, "Available" to data.available
                    ).forEach { (label, amount) ->
                        PlanJourneyDetailMetric(label, amount, Modifier.weight(1f))
                    }
                }
            }
            Text("Figures from your confirmed pay cycle; other income, expenses and fees are shown below.",
                style = MaterialTheme.typography.labelSmall, color = p.muted)
        }
    }
}

@Composable
private fun PlanJourneyDetailMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(when (label) {
            "Starting fund" -> Icons.Rounded.Wallet
            "Committed" -> Icons.Rounded.CalendarMonth
            "Saved" -> Icons.Rounded.Savings
            else -> Icons.Rounded.Spa
        }, null, tint = HomeMonochromePalette.green, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = HomeMonochromePalette.ink, maxLines = 2)
        Text(value, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            color = if (label == "Available") HomeMonochromePalette.green
                else HomeMonochromePalette.ink, maxLines = 3)
    }
}

@Composable
private fun PlanJourneyAllocations(data: PlanJourneyView) {
    val p = HomeMonochromePalette
    Surface(shape = RoundedCornerShape(PesaDesignTokens.cardRadius), color = p.surface,
        border = BorderStroke(1.dp, p.line)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your allocations", style = MaterialTheme.typography.titleMedium,
                color = p.ink, fontWeight = FontWeight.Bold)
            Text("Current pay-cycle amounts", style = MaterialTheme.typography.bodySmall,
                color = p.muted)
            val labels = listOf("Committed" to data.committed, "Saved" to data.saved,
                "Available" to data.available)
            val tints = listOf(p.green, p.green.copy(alpha = 0.45f), p.softGreen)
            if (data.displayedShares != null) {
                Row(Modifier.fillMaxWidth().height(16.dp)
                    .clip(RoundedCornerShape(PesaDesignTokens.controlRadius))) {
                    data.displayedShares.forEachIndexed { index, share ->
                        if (share > 0) Box(Modifier.weight(share.toFloat())
                            .fillMaxHeight().background(tints[index]))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEachIndexed { index, (label, amount) ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = p.muted)
                        Text(amount, style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold, color = p.ink, maxLines = 2)
                        data.displayedShares?.get(index)?.let { share ->
                            Text("$share%", style = MaterialTheme.typography.labelSmall,
                                color = p.muted)
                        }
                    }
                }
            }
            Text("Percentages compare these three amounts only; other money movements are below.",
                style = MaterialTheme.typography.labelSmall, color = p.muted)
            HorizontalDivider(color = p.line)
            PlanJourneyMoneyRow("Income received", data.received)
            PlanJourneyMoneyRow("Spent", data.spent)
            PlanJourneyMoneyRow("Fees", data.fees)
            if (data.safeToday != null) PlanJourneyMoneyRow("Safe today", data.safeToday)
        }
    }
}

@Composable
private fun PlanJourneyMoneyRow(label: String, amount: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
            color = HomeMonochromePalette.muted)
        Text(amount, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold, color = HomeMonochromePalette.ink)
    }
}
