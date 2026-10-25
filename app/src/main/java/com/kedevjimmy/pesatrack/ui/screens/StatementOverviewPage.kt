package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.everyday.*

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
    val largestExpenses = visible.asSequence()
        .filter { it.flow == StatementFlow.EXPENSE }
        .sortedByDescending { it.reportAmountMinor }
        .take(5)
        .toList()
    val linkedRows = visible.count { it.activityReceiptVerified }
    val linkedProgress = if (visible.isEmpty()) 0f else linkedRows.toFloat() / visible.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(21.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "Coverage: " +
                                (coverageStart?.let { statementDate(it, "d MMM yyyy") } ?: "No dates") +
                                " – " + (coverageEnd?.let { statementDate(it, "d MMM yyyy") } ?: "No dates"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text("${sourceMonths.size} months · $statementCount saved statements",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatementSmallMetric("Money in", sourceCashFlow.moneyInMinor,
                    androidx.compose.ui.graphics.Color(0xFF16866B), Modifier.weight(1f),
                    detail = "${visible.count { it.line.paidInMinor > 0L }} transactions") {
                    onNavigate(StatementPage.TRANSACTIONS, StatementFlow.INCOME, null)
                }
                StatementSmallMetric("Money out", sourceCashFlow.moneyOutMinor,
                    androidx.compose.ui.graphics.Color(0xFFE85D68), Modifier.weight(1f),
                    detail = "${visible.count { it.line.withdrawnMinor > 0L }} transactions") {
                    onNavigate(StatementPage.TRANSACTIONS, null, null)
                }
                StatementSmallMetric("Net flow", sourceCashFlow.netFlowMinor,
                    MaterialTheme.colorScheme.primary, Modifier.weight(1f), detail = "In − out") {
                    onNavigate(StatementPage.TRANSACTIONS, null, null)
                }
            }
        }
        item {
            Text("Money out can include transfers, savings, fees and loans.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            val top = breakdown.firstOrNull()
            val share = if (top == null || expenseTotal == 0L) 0
                else (top.second.toDouble() * 100.0 / expenseTotal).toInt()
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth().clickable {
                    onNavigate(StatementPage.CATEGORIES, null, null)
                }
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Lightbulb, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Insight", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            if (top != null) "${top.first} made up $share% of your spending."
                            else "No classified expenses in this date range yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            if (top != null) "Your largest recognized expense category."
                            else "Imported statement rows stay available for review.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "View categories",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    Modifier.weight(1f).heightIn(min = 174.dp)
                        .clickable { onNavigate(StatementPage.CATEGORIES, null, null) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Category trends", Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                        }
                        if (breakdown.isEmpty()) Text("No category data yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else breakdown.take(5).forEachIndexed { index, (category, amount) ->
                            val percent = if (expenseTotal == 0L) 0
                                else (amount.toDouble() * 100.0 / expenseTotal).toInt()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Category, contentDescription = null,
                                    tint = statementPalette[index % statementPalette.size],
                                    modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(category, Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("$percent%", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Surface(
                    Modifier.weight(1f).heightIn(min = 174.dp)
                        .clickable { onNavigate(StatementPage.TRANSACTIONS, StatementFlow.EXPENSE, null) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Largest expenses", Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                        }
                        if (largestExpenses.isEmpty()) Text("No recognized expenses yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else largestExpenses.forEach { expense ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Receipt, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(expense.line.description, style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(statementDate(expense.line.occurredAt, "d MMM"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(statementMoney(expense.reportAmountMinor),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        item {
            val monthly = sourceMonths.take(6).reversed()
            val maxMonthly = maxOf(1L, monthly.maxOfOrNull {
                maxOf(it.usage.paidInMinor, it.usage.paidOutMinor)
            } ?: 0L)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Income vs spending", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text("Monthly statement totals · tap a month to inspect",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.ShowChart, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(8.dp), shape = RoundedCornerShape(50),
                                color = androidx.compose.ui.graphics.Color(0xFF159A78)) {}
                            Spacer(Modifier.width(5.dp))
                            Text("Received", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(8.dp), shape = RoundedCornerShape(50),
                                color = androidx.compose.ui.graphics.Color(0xFFE85D68)) {}
                            Spacer(Modifier.width(5.dp))
                            Text("Spent", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (monthly.isEmpty()) {
                        Text("Monthly trends will appear when statement history is available.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else monthly.forEach { entry ->
                        Column(Modifier.fillMaxWidth().clickable {
                            onSelectMonth(entry.key)
                        }.padding(vertical = 3.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(entry.usage.label, Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium)
                                Text("Net \${statementMoney(entry.usage.netMinor)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (entry.usage.netMinor >= 0L)
                                        androidx.compose.ui.graphics.Color(0xFF16866B)
                                    else MaterialTheme.colorScheme.error)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.weight((entry.usage.paidInMinor.toFloat() / maxMonthly)
                                    .coerceAtLeast(0.025f)).height(6.dp)
                                    .background(androidx.compose.ui.graphics.Color(0xFF159A78),
                                        RoundedCornerShape(8.dp)))
                                Box(Modifier.weight((entry.usage.paidOutMinor.toFloat() / maxMonthly)
                                    .coerceAtLeast(0.025f)).height(6.dp)
                                    .background(androidx.compose.ui.graphics.Color(0xFFE85D68),
                                        RoundedCornerShape(8.dp)))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("In \${statementMoney(entry.usage.paidInMinor)}",
                                    Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Out \${statementMoney(entry.usage.paidOutMinor)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (sourceMonths.size > 6) {
                        TextButton(onClick = onArchive, modifier = Modifier.align(Alignment.End)) {
                            Text("Browse all \${sourceMonths.size} months")
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                        }
                    }
                    Text("Bars show statement cash movements, not verified account balances or classified spending.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            val incomeSources = visible.asSequence()
                .filter { it.flow == StatementFlow.INCOME && it.reportAmountMinor > 0L }
                .groupBy { it.categoryId }
                .map { (categoryId, rows) ->
                    val label = categoryId?.let { categoryLabels[it] }
                        ?.takeIf { it.isNotBlank() } ?: "Unclassified income"
                    label to rows.fold(0L) { total, row ->
                        Math.addExact(total, row.reportAmountMinor)
                    }
                }
                .sortedByDescending { it.second }
                .take(4)
            if (incomeSources.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("Income sources", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text("Based on transactions classified as income. Own-account transfers are excluded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        incomeSources.forEach { (label, amount) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.SouthWest, contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color(0xFF16866B),
                                    modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(9.dp))
                                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(statementMoney(amount), style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                        TextButton(onClick = {
                            onNavigate(StatementPage.TRANSACTIONS, StatementFlow.INCOME, null)
                        }) {
                            Text("View income transactions")
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth().clickable(onClick = onReconciliationToggle)
                        .heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(9.dp))
                        Text("How is this calculated?", Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Icon(if (reconciliationExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (reconciliationExpanded)
                                "Hide calculation details" else "Show calculation details")
                    }
                    if (reconciliationExpanded) {
                        Text(
                            "Money in, money out and net flow come from the selected saved statement rows. " +
                                "Money out can include transfers, savings, fees and loans; classified expenses " +
                                "include only recognized purchases. Overlapping statements are not added together.",
                            Modifier.padding(bottom = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(50.dp),
                            color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(42.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Description, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(21.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Activity evidence", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                            Text("$linkedRows of ${visible.size} statement rows linked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (visible.isNotEmpty()) Text("${(linkedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                    LinearProgressIndicator(progress = { linkedProgress }, modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary)
                    Text("Matched Activity is linked with evidence. Statement rows remain separate from your Activity totals.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Button(onClick = onImport, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Rounded.FileUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Import statement")
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onArchive) {
                    Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Statement archive")
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text("Evidence-linked", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
