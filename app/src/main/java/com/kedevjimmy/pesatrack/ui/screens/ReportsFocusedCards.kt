package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.reports.FocusedMovement
import com.kedevjimmy.pesatrack.domain.reports.FocusedMovementPolicy
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A compact summary and an on-demand evidence sheet, scoped to the Reports period. */
@Composable
internal fun ReportsFocusedCards(data: ReportsInsightData, onTransactionClick: (Long) -> Unit) {
    val rows = data.focusedMovements
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    FocusedCard("Borrowing & repayments", rows, data.range,
        listOf("DRAWDOWN" to "Borrowed", "REPAYMENT" to "Repaid", "FEE" to "Recorded fees"),
        "Principal and separately recorded fee events are shown apart. Embedded fees may be missing here. This is not outstanding debt.", onTransactionClick)
    FocusedCard("Money invested", rows, data.range,
        listOf("INVESTED" to "Confirmed purchases"),
        "Confirmed purchase cost only. Pending orders, funding transfers, current value and sale proceeds are excluded.", onTransactionClick)
    FocusedCard("Savings activity", rows, data.range,
        listOf("SAVED" to "Added", "WITHDRAWN" to "Withdrawn"),
        "Partial coverage: grouped by recorded savings goal, not necessarily bank account. Only trusted goal-linked transfers count. Plans and moves between savings accounts are excluded.", onTransactionClick)
    }
}

@Composable
private fun FocusedCard(title: String, allRows: List<FocusedMovement>, range: ReportPeriodRange,
    fields: List<Pair<String, String>>, note: String, onTransactionClick: (Long) -> Unit) {
    var expanded by remember(title, range) { mutableStateOf(false) }
    val summaries = fields.map { (kind, label) -> label to FocusedMovementPolicy.total(
        allRows, kind, range.startEpochMs, range.endExclusiveEpochMs) }
    val hasAny = summaries.any { it.second.hasRecords }
    PesaCard {
        Column(Modifier.fillMaxWidth().clickable { expanded = true }.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Rounded.ChevronRight, contentDescription = "View $title breakdown")
            }
            summaries.forEach { (label, result) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Text(when {
                        !result.hasRecords -> "No records"
                        result.amountMinor == null -> "Unavailable"
                        else -> formatKesAmount(result.amountMinor)
                    }, fontWeight = FontWeight.SemiBold)
                }
            }
            if (title == "Savings activity") {
                val added = summaries[0].second
                val removed = summaries[1].second
                Text("Net contributions: " + if (added.hasRecords || removed.hasRecords) {
                    if (added.amountMinor != null && removed.amountMinor != null)
                        runCatching { formatKesAmount(Math.subtractExact(added.amountMinor, removed.amountMinor)) }
                            .getOrDefault("Unavailable") else "Unavailable"
                } else "No records", style = MaterialTheme.typography.bodyMedium)
                Text("Partial · only confirmed savings-goal transfers",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (hasAny) "View breakdown and records" else "View coverage details",
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        }
    }
    if (expanded) AlertDialog(onDismissRequest = { expanded = false },
        title = { Text(title) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(range.label, style = MaterialTheme.typography.labelMedium)
                Text(note, style = MaterialTheme.typography.bodySmall)
                if (!hasAny) Text("No supported records found in this period; this does not prove zero activity.")
                summaries.forEach { (label, result) ->
                    if (result.hasRecords) {
                        Text(label, fontWeight = FontWeight.SemiBold)
                        result.records.groupBy { it.group }.forEach { (group, records) ->
                            val total = FocusedMovementPolicy.total(records, records.first().kind,
                                range.startEpochMs, range.endExclusiveEpochMs)
                            Text("$group · " + (total.amountMinor?.let(::formatKesAmount) ?: "Amount unavailable"))
                            records.forEach { record ->
                                val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                                    .format(Date(record.atEpochMs))
                                val amount = record.amountMinor?.let(::formatKesAmount) ?: "Unknown amount"
                                Text("$date · $amount" + if (record.feeMinor > 0) " · Fee ${formatKesAmount(record.feeMinor)}" else "",
                                    modifier = Modifier.fillMaxWidth().then(if (record.transactionId != null)
                                        Modifier.clickable { expanded = false; onTransactionClick(record.transactionId) }
                                    else Modifier).padding(vertical = 8.dp),
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { expanded = false }) { Text("Close") } })
}
