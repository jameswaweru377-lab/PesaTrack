package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.everyday.*

private val StatementGreen = Color(0xFF159A78)
private val StatementRed = Color(0xFFEF5365)
private val StatementPurple = Color(0xFF7542E8)
private val StatementBlue = Color(0xFF3286D9)
private val StatementAmber = Color(0xFFF2A62A)

@Composable
internal fun StatementOverviewPage(
    sourceMonths: List<StatementHistoryReport.SelectedMonth>,
    visible: List<StatementClassified>,
    sourceCashFlow: StatementCashFlowTotals,
    breakdown: List<Pair<String, Long>>,
    categoryLabels: Map<String, String>,
    reconciliationExpanded: Boolean,
    onReconciliationToggle: () -> Unit,
    onNavigate: (StatementPage, StatementFlow?, String?) -> Unit,
    onImport: () -> Unit,
    onArchive: () -> Unit,
    onSelectMonth: (String) -> Unit
) {
    val statementCount = sourceMonths.sumOf { it.sourceCount }
    val coverageStart = sourceMonths.minOfOrNull { it.source.firstTransactionAt }
    val coverageEnd = sourceMonths.maxOfOrNull { it.source.lastTransactionAt }
    val expenseTotal = breakdown.fold(0L) { total, row -> Math.addExact(total, row.second) }
    val incomeRows = visible.filter { it.flow == StatementFlow.INCOME && it.reportAmountMinor > 0L }
    val incomeTotal = incomeRows.fold(0L) { total, row -> Math.addExact(total, row.reportAmountMinor) }
    val largestExpenses = visible.asSequence().filter { it.flow == StatementFlow.EXPENSE }
        .sortedByDescending { it.reportAmountMinor }.take(5).toList()
    val incomeSources = incomeRows.groupBy { row ->
        row.categoryId?.let { categoryLabels[it] }?.takeIf(String::isNotBlank) ?: "Other income"
    }.map { (name, rows) ->
        name to rows.fold(0L) { total, row -> Math.addExact(total, row.reportAmountMinor) }
    }.sortedByDescending { it.second }.take(4)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = StatementPurple, modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ReceiptLong, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(23.dp))
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Statements", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    Text("Understand your money over time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onArchive) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = "Browse statement history",
                        tint = StatementPurple)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier.weight(1f).clickable { onArchive() },
                    shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CalendarMonth, null, tint = StatementPurple,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (sourceMonths.isEmpty()) "Date range" else
                                "${sourceMonths.size} months", style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text("Available history", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.ExpandMore, null, modifier = Modifier.size(17.dp))
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f).clickable { onArchive() },
                    shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccountBalance, null, tint = StatementPurple,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Column(Modifier.weight(1f)) {
                            Text("All accounts", style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text("$statementCount saved sources", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.ExpandMore, null, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Financial Summary", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text(
                                if (coverageStart != null && coverageEnd != null)
                                    "${statementDate(coverageStart, "d MMM yyyy")} – ${statementDate(coverageEnd, "d MMM yyyy")}"
                                else "Import a statement to start building your history",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (sourceMonths.isNotEmpty()) {
                            Surface(shape = RoundedCornerShape(50), color = Color(0xFFEDE5FF)) {
                                Text("${sourceMonths.size} months", Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelSmall, color = StatementPurple)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatementSummaryMetric("Money received", sourceCashFlow.moneyInMinor,
                            StatementGreen, Icons.Rounded.ArrowUpward, Modifier.weight(1f),
                            "${visible.count { it.line.paidInMinor > 0L }} rows") {
                            onNavigate(StatementPage.TRANSACTIONS, StatementFlow.INCOME, null)
                        }
                        StatementSummaryMetric("Money spent", sourceCashFlow.moneyOutMinor,
                            StatementRed, Icons.Rounded.ArrowDownward, Modifier.weight(1f),
                            "${visible.count { it.line.withdrawnMinor > 0L }} rows") {
                            onNavigate(StatementPage.TRANSACTIONS, null, null)
                        }
                        StatementSummaryMetric("Net cash flow", sourceCashFlow.netFlowMinor,
                            StatementPurple, Icons.Rounded.ShowChart, Modifier.weight(1f), "In − out") {
                            onNavigate(StatementPage.TRANSACTIONS, null, null)
                        }
                    }
                    Text("Statement cash movement may include transfers, savings, fees and loans.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            val monthlyRows = visible.groupBy { StatementMonthlyUsage.monthKey(it.line.occurredAt) }
            val monthly = sourceMonths.filter { it.key in monthlyRows.keys }.take(6).reversed()
            val values = monthly.associate { entry ->
                val rows = monthlyRows[entry.key].orEmpty()
                val received = rows.filter { it.flow == StatementFlow.INCOME }
                    .fold(0L) { a, b -> Math.addExact(a, b.reportAmountMinor) }
                val spent = rows.filter { it.flow == StatementFlow.EXPENSE }
                    .fold(0L) { a, b -> Math.addExact(a, b.reportAmountMinor) }
                entry.key to (received to spent)
            }
            val maxAmount = maxOf(1L, values.values.maxOfOrNull { maxOf(it.first, it.second) } ?: 0L)
            StatementPanel {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Income vs spending trend", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text("Tap a month to open its details", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.ShowChart, null, tint = StatementPurple)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                        StatementLegend(StatementGreen, "Money received")
                        StatementLegend(StatementRed, "Money spent")
                    }
                    if (monthly.isEmpty()) {
                        StatementEmptyInline("No monthly trend yet",
                            "Import a statement to compare recognized income and spending.")
                    } else {
                        Row(Modifier.fillMaxWidth().height(150.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom) {
                            monthly.forEach { entry ->
                                val amount = values[entry.key] ?: (0L to 0L)
                                Column(
                                    Modifier.weight(1f).fillMaxHeight().clickable { onSelectMonth(entry.key) }
                                        .padding(horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Row(Modifier.fillMaxWidth().height(112.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.Bottom) {
                                        Box(Modifier.weight(1f).fillMaxHeight(
                                            (amount.first.toDouble() / maxAmount).toFloat().coerceIn(0.025f, 1f)
                                        ).padding(horizontal = 2.dp).background(StatementGreen, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                                        Box(Modifier.weight(1f).fillMaxHeight(
                                            (amount.second.toDouble() / maxAmount).toFloat().coerceIn(0.025f, 1f)
                                        ).padding(horizontal = 2.dp).background(StatementRed, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                                    }
                                    Spacer(Modifier.height(7.dp))
                                    Text(entry.usage.label.take(3), style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1)
                                }
                            }
                        }
                        Text("Only classified income and recognized purchases are charted. Transfers, loans and unclassified rows are excluded.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Monthly history", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = onArchive) { Text("View all"); Icon(Icons.Rounded.ChevronRight, null) }
                    }
                    monthly.asReversed().take(4).forEach { entry ->
                        val amount = values[entry.key] ?: (0L to 0L)
                        Row(Modifier.fillMaxWidth().clickable { onSelectMonth(entry.key) }
                            .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.usage.label, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Text("Received ${statementMoney(amount.first)}",
                                    style = MaterialTheme.typography.labelSmall, color = StatementGreen)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(statementMoney(amount.second), style = MaterialTheme.typography.bodyMedium,
                                    color = StatementRed, fontWeight = FontWeight.SemiBold)
                                Text("spent", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.padding(start = 5.dp))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
        item {
            StatementPanel {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Spending breakdown", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text("Where recognized spending went", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { onNavigate(StatementPage.CATEGORIES, null, null) }) {
                            Text("See all")
                        }
                    }
                    if (breakdown.isEmpty()) {
                        StatementEmptyInline("No classified spending yet",
                            "Unclassified movements remain available in the transaction list.")
                    } else breakdown.take(5).forEachIndexed { index, (category, amount) ->
                        val share = if (expenseTotal == 0L) 0f else amount.toFloat() / expenseTotal
                        val tint = listOf(StatementPurple, StatementBlue, StatementGreen, StatementAmber, StatementRed)[index % 5]
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = tint.copy(alpha = .12f), modifier = Modifier.size(34.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Category, null, tint = tint, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(category, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${(share * 100).toInt()}%", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Box(Modifier.fillMaxWidth().height(6.dp).background(
                                    MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                                    Box(Modifier.fillMaxWidth(share.coerceIn(0f, 1f)).fillMaxHeight()
                                        .background(tint, CircleShape))
                                }
                            }
                            Spacer(Modifier.width(9.dp))
                            Text(statementMoney(amount), style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }
        }
        item {
            StatementPanel {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Income sources", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text("Only records classified as income", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.SouthWest, null, tint = StatementGreen)
                    }
                    if (incomeSources.isEmpty()) StatementEmptyInline("No income sources yet",
                        "Income sources appear when imported rows are classified as income.")
                    else incomeSources.forEach { (label, amount) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = StatementGreen.copy(alpha = .12f),
                                modifier = Modifier.size(34.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.SouthWest, null, tint = StatementGreen,
                                        modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.width(9.dp))
                            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(statementMoney(amount), style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                    TextButton(onClick = { onNavigate(StatementPage.TRANSACTIONS, StatementFlow.INCOME, null) }) {
                        Text("View income transactions")
                        Icon(Icons.Rounded.ChevronRight, null)
                    }
                }
            }
        }
        item {
            StatementPanel {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = StatementPurple.copy(alpha = .12f),
                            modifier = Modifier.size(38.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Lightbulb, null, tint = StatementPurple)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Financial insights", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text("Helpful signals from available records",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val top = breakdown.firstOrNull()
                    Text(if (top != null && expenseTotal > 0L)
                        "${top.first} is your largest recognized spending category at ${(top.second.toDouble() * 100 / expenseTotal).toInt()}% of spending."
                    else "Insights will appear when enough transactions have been classified.",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Received ${statementMoney(incomeTotal)} across classified income records.",
                        style = MaterialTheme.typography.bodySmall, color = StatementGreen)
                    TextButton(onClick = { onNavigate(StatementPage.TRENDS, null, null) }) {
                        Text("Explore trends"); Icon(Icons.Rounded.ChevronRight, null)
                    }
                }
            }
        }
        item {
            StatementPanel {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, null, tint = StatementPurple)
                        Spacer(Modifier.width(8.dp))
                        Text("Imported statement context", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                    }
                    StatementContextRow(Icons.Rounded.CalendarMonth, "Reporting period",
                        if (coverageStart != null && coverageEnd != null)
                            "${statementDate(coverageStart, "d MMM yyyy")} – ${statementDate(coverageEnd, "d MMM yyyy")}"
                        else "No imported dates yet")
                    StatementContextRow(Icons.Rounded.AccountBalance, "Saved history",
                        "${sourceMonths.size} months · $statementCount saved statement sources")
                    StatementContextRow(Icons.Rounded.ReceiptLong, "Rows included",
                        "${visible.size} rows in the selected report")
                    Row(Modifier.fillMaxWidth().clickable(onClick = onReconciliationToggle)
                        .padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("How figures are calculated", Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Icon(if (reconciliationExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (reconciliationExpanded) "Hide calculation details" else "Show calculation details")
                    }
                    if (reconciliationExpanded) Text(
                        "Money received, money spent and net cash flow reflect the selected saved statement rows. Spending breakdowns include recognized expenses only. Own-account transfers, loans and unclassified movements are not re-labelled as purchases. Overlapping statements are not added together.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Button(onClick = onImport, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Rounded.UploadFile, null)
                Spacer(Modifier.width(8.dp))
                Text("Import statement (PDF or CSV)")
            }
            OutlinedButton(onClick = onArchive, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Rounded.Inventory2, null)
                Spacer(Modifier.width(8.dp))
                Text("Browse monthly history")
            }
        }
    }
}

@Composable
private fun StatementSummaryMetric(
    label: String,
    amount: Long,
    tint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    detail: String,
    onClick: () -> Unit
) {
    Surface(modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp), color = tint.copy(alpha = .07f),
        border = BorderStroke(1.dp, tint.copy(alpha = .18f))) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(shape = CircleShape, color = tint, modifier = Modifier.size(29.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp))
                }
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            Text(statementMoney(amount), style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = tint, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatementPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun StatementLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatementEmptyInline(title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatementContextRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = StatementPurple, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}
