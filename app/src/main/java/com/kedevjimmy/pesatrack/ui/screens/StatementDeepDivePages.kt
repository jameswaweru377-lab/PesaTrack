package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.domain.everyday.*
import com.kedevjimmy.pesatrack.domain.statements.StatementSourceCoveragePolicy
import java.text.DateFormatSymbols
import java.util.Locale

@Composable
internal fun StatementCategoryInsightPage(
    categoryLabel: String,
    rows: List<StatementClassified>,
    categoryLabels: Map<String, String>,
    onOpenTransactions: (String) -> Unit,
    onOpenRow: (StatementClassified) -> Unit,
    onBack: () -> Unit
) {
    val p = HomeReferencePalette
    val matches = remember(rows, categoryLabel, categoryLabels) {
        StatementExpenseCatalog.matchingExpenseRows(rows, categoryLabel, categoryLabels)
    }
    val spent = matches.fold(0L) { acc, row -> Math.addExact(acc, row.reportAmountMinor) }
    val byDescription = matches.groupBy {
        StatementFinancialPolicy.merchantKey(it.line.description) ?: it.line.description
    }.mapValues { (_, group) -> group.fold(0L) { acc, row ->
        Math.addExact(acc, row.reportAmountMinor)
    } }.toList().sortedByDescending { it.second }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { StatementDetailHeading(categoryLabel, "Spending explained by statement records", onBack) }
        item { StatementDetailHero("Spent in this selection", spent, "Classified expenses only") }
        item {
            StatementInsightCard {
                Text("What makes up this category", color = p.ink,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (matches.isEmpty()) Text("No matching spending entries.", color = p.muted)
                byDescription.take(5).forEach { (description, amount) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(description, Modifier.weight(1f), maxLines = 2,
                            overflow = TextOverflow.Ellipsis, color = p.ink)
                        Spacer(Modifier.width(10.dp))
                        Text(statementMoney(amount), color = p.ink,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text("Labels are from statement evidence; a description is not proof of merchant identity.",
                    color = p.muted, style = MaterialTheme.typography.labelSmall)
            }
        }
        item {
            StatementInsightCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Matching statement entries", Modifier.weight(1f),
                        color = p.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onOpenTransactions(categoryLabel) }) {
                        Text("See all", color = p.purple)
                    }
                }
                matches.take(6).forEach { entry ->
                    StatementSourceRow(entry, onClick = { onOpenRow(entry) })
                }
            }
        }
    }
}

@Composable
internal fun StatementPeriodPage(
    month: StatementHistoryReport.SelectedMonth,
    rows: List<StatementClassified>,
    onOpenRow: (StatementClassified) -> Unit,
    onBack: () -> Unit,
    sourceLabel: String = "M-Pesa"
) {
    val p = HomeReferencePalette
    val included = remember(month, rows) {
        rows.filter { StatementMonthlyUsage.monthKey(it.line.occurredAt) == month.key }
    }
    val metrics = remember(included) { StatementInsightProjection.metrics(included) }
    var selection by remember(month.key) { mutableStateOf<StatementFlow?>(null) }
    var search by remember(month.key) { mutableStateOf("") }
    val filtered = included.filter { row ->
        (selection == null || row.flow == selection) &&
            (search.isBlank() || row.line.description.contains(search, ignoreCase = true) ||
                row.line.reference.contains(search, ignoreCase = true))
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { StatementDetailHeading(month.usage.label,
            "$sourceLabel · Imported statement evidence", onBack) }
        item { StatementDetailHero("Classified spending · ${month.usage.label}",
            metrics.spentMinor, "Transfers and loan principal excluded") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatementPeriodMoneyTile("Money in", metrics.moneyInMinor, Modifier.weight(1f))
                StatementPeriodMoneyTile("Money out", metrics.moneyOutMinor, Modifier.weight(1f))
            }
        }
        item {
            Surface(color = Color(0xFFEDE7FF), shape = RoundedCornerShape(15.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (month.source.independentlyVerified) "Checked imported PDF"
                        else "Saved statement source", color = p.purple,
                        fontWeight = FontWeight.Bold)
                    Text("${month.usage.sourceRows} source entries · " +
                        "${statementDate(month.source.firstTransactionAt)} – " +
                        statementDate(month.source.lastTransactionAt),
                        color = p.muted, style = MaterialTheme.typography.bodySmall)
                    if (month.sourceCount > 1) Text(
                        "${month.sourceCount} overlapping sources; the report counts one selected file.",
                        color = p.muted, style = MaterialTheme.typography.bodySmall)
                    Text("Observed dates do not guarantee a complete calendar month.",
                        color = p.muted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item {
            Text("Statement entries", color = p.ink, fontSize = 19.sp,
                fontWeight = FontWeight.Bold)
            OutlinedTextField(value = search, onValueChange = { search = it },
                label = { Text("Search description or receipt") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(null to "All", StatementFlow.EXPENSE to "Spent",
                    StatementFlow.INCOME to "Income", StatementFlow.TRANSFER to "Transfers")
                    .forEach { (flow, label) ->
                        FilterChip(selected = selection == flow, onClick = { selection = flow },
                            label = { Text(label) })
                    }
            }
        }
        if (filtered.isEmpty()) item {
            Text("No statement entries match these filters.", color = p.muted)
        }
        items(filtered, key = { "${it.batchId}:${it.line.page}:${it.line.row}" }) { entry ->
            StatementInsightCard { StatementSourceRow(entry) { onOpenRow(entry) } }
        }
    }
}

@Composable
internal fun StatementCoveragePage(
    year: Int,
    months: List<StatementHistoryReport.SelectedMonth>,
    onOpenMonth: (String) -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
    sourceLabel: String = "M-Pesa",
    overlapByMonth: Map<String, StatementSourceCoveragePolicy.MonthCoverage> = emptyMap(),
    checkingOverlap: Boolean = false,
    overlapUnavailable: Boolean = false
) {
    val p = HomeReferencePalette
    val yearMonths = months.filter { it.usage.year == year }.associateBy { it.usage.month }
    LazyColumn(modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { StatementDetailHeading("Statement coverage", "$year · $sourceLabel wallet", onBack) }
        item {
            Surface(color = Color(0xFFFFF3E8), shape = RoundedCornerShape(15.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${yearMonths.size} of 12 months imported", color = p.ink,
                        fontWeight = FontWeight.Bold)
                    Text("A missing month is unknown, not KES 0 spent. Imported partial months are not necessarily complete.",
                        color = p.muted, style = MaterialTheme.typography.bodySmall)
                    if (checkingOverlap) Text("Checking repeated entries across saved PDFs…",
                        color = p.muted, style = MaterialTheme.typography.bodySmall)
                    if (overlapUnavailable) Text("Overlap verification is unavailable. The report still uses one selected PDF per month.",
                        color = p.red, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            StatementInsightCard {
                Text("$year calendar", color = p.ink, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold)
                (1..12).chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { monthNumber ->
                            val source = yearMonths[monthNumber]
                            val overlap = source?.let { overlapByMonth[it.key] }
                            Surface(
                                modifier = Modifier.weight(1f).clickable(enabled = source != null) {
                                    source?.let { onOpenMonth(it.key) }
                                },
                                color = if (source == null) Color(0xFFF4F2F8)
                                    else if (overlap?.status == StatementSourceCoveragePolicy.Status.NEEDS_REVIEW)
                                        Color(0xFFFFF3E8)
                                    else Color(0xFFE7F8EE),
                                shape = RoundedCornerShape(13.dp)
                            ) {
                                Column(Modifier.padding(vertical = 12.dp, horizontal = 5.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(DateFormatSymbols(Locale.ENGLISH).shortMonths[monthNumber - 1],
                                        color = p.ink, fontWeight = FontWeight.Bold)
                                    Text(if (source == null) "Not imported" else "Imported",
                                        color = if (source == null) p.muted else p.green,
                                        style = MaterialTheme.typography.labelSmall)
                                    if (overlap != null && overlap.sourceCount > 1) Text(
                                        if (overlap.status == StatementSourceCoveragePolicy.Status.NEEDS_REVIEW)
                                            "Review overlap" else "${overlap.sourceCount} PDFs",
                                        color = if (overlap.status == StatementSourceCoveragePolicy.Status.NEEDS_REVIEW)
                                            p.red else p.muted,
                                        style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        val overlapping = overlapByMonth.values.filter { it.sourceCount > 1 }
            .sortedByDescending { it.monthKey }
        if (overlapping.isNotEmpty()) item {
            StatementInsightCard {
                Text("Repeated statement entries", color = p.ink,
                    fontWeight = FontWeight.Bold)
                Text("Exact verified repeats are recognized once in this coverage check. The current month reports still show one selected PDF, not an all-source total.",
                    color = p.muted, style = MaterialTheme.typography.bodySmall)
                overlapping.forEach { entry ->
                    val needsReview = entry.status ==
                        StatementSourceCoveragePolicy.Status.NEEDS_REVIEW
                    Text("${entry.monthKey} · ${entry.sourceCount} PDFs · " +
                        "${entry.confirmedRepeatCopies} exact repeat(s)",
                        color = p.ink, fontWeight = FontWeight.SemiBold)
                    Text(if (needsReview)
                            "${entry.unresolvedCandidateCount} entries need evidence review (overlap or source verification); no combined cash total is claimed."
                        else "No unresolved near-duplicate found in the saved evidence. Partial month coverage may still be missing.",
                        color = if (needsReview) p.red else p.muted,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Button(onClick = onImport, modifier = Modifier.fillMaxWidth().heightIn(min = 49.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.purple)) {
                Icon(Icons.Rounded.UploadFile, null)
                Spacer(Modifier.width(8.dp))
                Text("Import M-Pesa statement")
            }
        }
    }
}

@Composable
internal fun StatementEvidenceDetailPage(
    row: StatementClassified,
    sourceLabel: String,
    categoryLabel: String?,
    linkedActivityId: Long?,
    onReviewCategory: () -> Unit,
    onOpenActivity: (Long) -> Unit,
    onBack: () -> Unit
) {
    val p = HomeReferencePalette
    val outgoing = row.line.withdrawnMinor > 0L
    val masked = row.line.reference.take(4) + "••" + row.line.reference.takeLast(2)
    var expanded by remember(row.batchId, row.line.page, row.line.row) { mutableStateOf(false) }
    LazyColumn(modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { StatementDetailHeading("Transaction evidence", "From your imported statement", onBack) }
        item {
            StatementInsightCard {
                Text((if (outgoing) "− " else if (row.line.paidInMinor > 0L) "+ " else "") +
                    statementMoney(row.amountMinor), fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (outgoing) p.ink else p.green)
                Text(statementDate(row.line.occurredAt, "dd MMM yyyy · h:mm a"), color = p.muted)
                Text(row.line.description, color = p.ink,
                    style = MaterialTheme.typography.bodyMedium)
                if (row.activityReceiptVerified && linkedActivityId != null)
                    Text("Matched to one Activity record", color = p.green,
                        style = MaterialTheme.typography.labelLarge)
            }
        }
        item {
            StatementInsightCard {
                Text("Details from statement", color = p.ink, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold)
                StatementEvidenceField("Source", sourceLabel)
                StatementEvidenceField("Category", categoryLabel ?: "Needs review")
                StatementEvidenceField("Movement", row.flow.name.lowercase()
                    .replaceFirstChar { it.uppercase() })
                StatementEvidenceField("Receipt", masked)
                StatementEvidenceField("Statement", StatementMonthlyUsage.monthKey(row.line.occurredAt))
                StatementEvidenceField("Source location", "Page ${row.line.page} · Row ${row.line.row}")
                if (row.reportAmountMinor != row.amountMinor)
                    StatementEvidenceField("Your expense share", statementMoney(row.reportAmountMinor))
            }
        }
        item {
            Surface(color = Color(0xFFF0ECFF), shape = RoundedCornerShape(14.dp)) {
                Text(if (linkedActivityId != null)
                    "A unique matching receipt was confirmed in Activity. Category sharing still follows your reviewed decisions."
                else "No unique Activity match is available. This source entry remains independently reviewable.",
                    modifier = Modifier.padding(14.dp), color = p.muted,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (row.flow == StatementFlow.EXPENSE || row.flow == StatementFlow.UNKNOWN) {
                    OutlinedButton(onClick = onReviewCategory,
                        modifier = Modifier.weight(1f).heightIn(min = 49.dp)) {
                        Text("Review category", maxLines = 1)
                    }
                }
                if (linkedActivityId != null) {
                    Button(onClick = { onOpenActivity(linkedActivityId) },
                        modifier = Modifier.weight(1f).heightIn(min = 49.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = p.purple)) {
                        Text("View Activity record", maxLines = 2)
                    }
                }
            }
        }
        if (row.line.sourceCells.isNotEmpty()) item {
            StatementInsightCard {
                TextButton(onClick = { expanded = !expanded }) {
                    Text("Original statement entry", color = p.purple)
                    Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        null, tint = p.purple)
                }
                if (expanded) {
                    Text("Private source cells as saved on this device:", color = p.muted,
                        style = MaterialTheme.typography.labelSmall)
                    row.line.sourceCells.forEachIndexed { index, value ->
                        StatementEvidenceField("Column ${index + 1}", value)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatementDetailHeading(title: String, subtitle: String, onBack: () -> Unit) {
    val p = HomeReferencePalette
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack,
            "Back", tint = p.ink) }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                color = p.ink, maxLines = 2)
            Text(subtitle, color = p.muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StatementDetailHero(label: String, amount: Long, note: String) {
    val p = HomeReferencePalette
    Surface(color = p.purple, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().heightIn(min = 115.dp)
            .padding(17.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(label, color = Color.White.copy(alpha = .9f))
            Text(statementMoney(amount), color = Color.White, fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(note, color = Color.White.copy(alpha = .85f),
                style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun StatementPeriodMoneyTile(title: String, amount: Long, modifier: Modifier = Modifier) {
    val p = HomeReferencePalette
    Surface(modifier = modifier, color = Color.White, shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = p.muted, style = MaterialTheme.typography.labelMedium)
            Text(statementMoney(amount), color = p.ink, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatementSourceRow(row: StatementClassified, onClick: () -> Unit) {
    val p = HomeReferencePalette
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick)
        .padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = if (row.line.withdrawnMinor > 0L) Color(0xFFFFEBDC)
            else Color(0xFFE1F8EF), shape = RoundedCornerShape(11.dp)) {
            Icon(if (row.line.withdrawnMinor > 0L) Icons.Rounded.ShoppingCart
                else Icons.Rounded.ArrowDownward, null,
                tint = if (row.line.withdrawnMinor > 0L) Color(0xFFF16C13) else p.green,
                modifier = Modifier.padding(9.dp).size(19.dp))
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(row.line.description, color = p.ink,
                style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(statementDate(row.line.occurredAt, "dd MMM yyyy · h:mm a"), color = p.muted,
                style = MaterialTheme.typography.labelSmall)
        }
        Text((if (row.line.withdrawnMinor > 0L) "− " else "+ ") +
            statementMoney(row.amountMinor), color = p.ink,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold)
        Icon(Icons.Rounded.ChevronRight, null, tint = p.muted,
            modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun StatementEvidenceField(label: String, value: String) {
    val p = HomeReferencePalette
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.Top) {
        Text(label, Modifier.weight(.9f), color = p.muted,
            style = MaterialTheme.typography.bodySmall)
        Text(value, Modifier.weight(1.2f), color = p.ink,
            style = MaterialTheme.typography.bodySmall)
    }
    HorizontalDivider(color = p.line)
}
