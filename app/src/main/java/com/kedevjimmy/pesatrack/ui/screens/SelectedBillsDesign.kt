package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney
import com.kedevjimmy.pesatrack.planning.ScheduledBill
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Selected Bills B: 14 actual local-calendar dates and sourced bill markers. */
@Composable
internal fun SelectedBillsCalendar(
    viewedMonthEpochMs: Long,
    bills: List<ScheduledBill>,
    locale: Locale,
    onOpenCalendar: () -> Unit
) {
    val p = HomeMonochromePalette
    val days = selectedBillsFortnight(viewedMonthEpochMs, System.currentTimeMillis(),
        bills.filter { it.remainingMinor > 0L }.mapNotNull { it.nextDueEpochMs },
        locale, TimeZone.getDefault())
    Surface(shape = RoundedCornerShape(PesaDesignTokens.cardRadius), color = p.softGreen,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenCalendar)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { weekday ->
                    Text(weekday, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall, color = p.muted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            days.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { day ->
                        Surface(shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
                            modifier = Modifier.weight(1f).heightIn(min = 84.dp),
                            color = if (day.isToday) p.green else Color.Transparent) {
                            Column(Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(day.dayOfMonth,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (day.isToday) Color.White else p.ink)
                                Text(day.month, style = MaterialTheme.typography.labelSmall,
                                    color = if (day.isToday) Color.White else p.muted)
                                if (day.hasBill) {
                                    Surface(shape = CircleShape, color = Color(0xFFD7A33C),
                                        modifier = Modifier.size(8.dp)) {}
                                }
                            }
                        }
                    }
                }
            }
            TextButton(onClick = onOpenCalendar,
                modifier = Modifier.align(Alignment.End)) { Text("See full calendar") }
        }
    }
}

@Composable
internal fun SelectedBillsSummary(bills: List<ScheduledBill>, money: (Long) -> String) {
    val pending = bills.filter { it.remainingMinor > 0L }
    val outstandingTotal = CheckedMoney.orNull {
        CheckedMoney.sum(pending.asSequence().map { it.remainingMinor })
    }
    Surface(shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
        color = HomeMonochromePalette.softGreen) {
        Row(Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.CalendarMonth, null, tint = HomeMonochromePalette.green)
            Text("${pending.size} upcoming · ${outstandingTotal?.let(money) ?: "Total unavailable"}",
                fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
internal fun SelectedBillsUpcomingRow(
    bill: ScheduledBill,
    locale: Locale,
    money: (Long) -> String,
    onClick: () -> Unit
) {
    val p = HomeMonochromePalette
    val title = bill.rule.displayLabel ?: bill.rule.name
    val icon = when {
        "rent" in title.lowercase(Locale.ROOT) -> Icons.Rounded.Home
        listOf("wifi", "wi-fi", "internet").any { it in title.lowercase(Locale.ROOT) } ->
            Icons.Rounded.Wifi
        else -> Icons.Rounded.ReceiptLong
    }
    val due = bill.nextDueEpochMs
    val dueDate = due?.let { SimpleDateFormat("EEE, d MMM", locale).format(Date(it)) }
        ?: "Due date not set"
    val days = due?.let { selectedBillDaysUntil(it, System.currentTimeMillis(),
        TimeZone.getDefault()) }
    val dueText = when {
        days == null -> "Schedule needed"
        days < 0L -> "Overdue"
        days == 0L -> "Due today"
        days == 1L -> "Due tomorrow"
        else -> "Due in $days days"
    }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(PesaDesignTokens.iconRadius), color = p.softGreen) {
                Box(Modifier.size(PesaDesignTokens.iconBubble), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = p.green)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(dueDate, style = MaterialTheme.typography.bodySmall, color = p.muted)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(shape = CircleShape, color = Color(0xFFFFF0E3)) {
                    Text(dueText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color(0xFF8A450B), style = MaterialTheme.typography.labelSmall)
                }
                Text(money(bill.remainingMinor), fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Edit bill", tint = p.ink)
        }
        HorizontalDivider(color = p.line)
    }
}
