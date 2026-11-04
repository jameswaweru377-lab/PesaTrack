package com.kedevjimmy.pesatrack.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SmallStatusPill
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max

/** Approved Reports & Insights Design 2; data stays owned by the existing Activity ledger. */
@Composable
fun ReportsInsightsScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit,
    onReviewClick: () -> Unit = {},
    onPaycheckClick: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val revision by LedgerUpdateBus.state.collectAsState()
    var period by remember { mutableStateOf(ReportPeriod.MONTH) }
    var selectedEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var data by remember { mutableStateOf<ReportsInsightData?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var reviewType by remember { mutableStateOf<ReportReviewType?>(null) }
    var expandedCategories by remember { mutableStateOf(false) }

    LaunchedEffect(revision.revision, period, selectedEpochMs, retry) {
        data = null
        loadError = null
        try {
            data = withContext(Dispatchers.IO) {
                ReportsInsightsReader(context.applicationContext).load(period, selectedEpochMs)
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            android.util.Log.e("PesaTrackReports", "Could not load reports")
            loadError = "Couldn't load the report. Your saved records were not changed."
        }
    }

    val current = data
    val largest = remember(current) { reportLargestExpenses(current?.reviewCandidates.orEmpty()) }
    val uncategorized = remember(current) { reportUncategorizedExpenses(current?.reviewCandidates.orEmpty()) }
    val broad = remember(current) { reportBroadCategoryExpenses(current?.reviewCandidates.orEmpty()) }
    if (reviewType != null) {
        ReportExpenseReviewSheet(
            type = reviewType!!,
            rows = when (reviewType) {
                ReportReviewType.LARGEST -> largest
                ReportReviewType.UNCATEGORIZED -> uncategorized
                else -> broad
            },
            periodLabel = current?.range?.label.orEmpty(),
            onDismiss = { reviewType = null },
            onTransactionClick = { id -> reviewType = null; onTransactionClick(id) }
        )
    }
    LazyColumn(
        modifier = Modifier.padding(scaffoldPadding).background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ReportsPageTitle(onBack) }
        item {
            ReportsPeriodSelector(period, current?.range?.label, selectedEpochMs,
                onPeriod = { period = it; expandedCategories = false },
                onDate = { selectedEpochMs = it })
        }
        if (loadError != null) {
            item {
                PesaCard { Column(Modifier.padding(16.dp)) {
                    Text(loadError!!, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { retry++ }) { Text("Try again") }
                } }
            }
        } else if (current == null) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            item { Text("Preparing this report from your saved Activity…",
                style = MaterialTheme.typography.bodySmall) }
        } else {
            if (current.current.trustedTransactions.isEmpty()) item {
                PesaCard {
                    Column(Modifier.padding(16.dp)) {
                        Text("No trusted Activity entries in this period.",
                            style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onReviewClick,
                            modifier = Modifier.heightIn(min = 48.dp)) { Text("Open Activity") }
                    }
                }
            }
            item { ReportsTopMetrics(current) }
            item { ReportsTrendCard(current.trend, period) }
            item { ReportsWhatChanged(current, period) }
            item {
                ReportsCategoryCard(current.spending, expandedCategories,
                    toggleExpanded = { expandedCategories = !expandedCategories },
                    onReviewBroad = { reviewType = ReportReviewType.BROAD },
                    broadCount = broad.size)
            }
            item {
                ReportsReviewActions(
                    uncategorized.size,
                    onLargest = { reviewType = ReportReviewType.LARGEST },
                    onUncategorized = { reviewType = ReportReviewType.UNCATEGORIZED }
                )
            }
            if (period == ReportPeriod.YEAR) {
                item { SharedAccountingReportCard(current.annualAccounting) }
            }
            item { ReportsPaycheckEntry(onPaycheckClick) }
            item { ReportExportButton(current.pdfContent, current.current.trustedTransactions) }
            item {
                Text("From trusted Activity entries. Imported statements remain separate " +
                    "until reviewed; transactions needing review are not silently counted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReportsPageTitle(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Text("Reports & insights", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReportsPeriodSelector(
    selected: ReportPeriod,
    label: String?,
    selectedEpochMs: Long,
    onPeriod: (ReportPeriod) -> Unit,
    onDate: (Long) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedEpochMs }
            DatePickerDialog(context, { _, year, month, day ->
                onDate(Calendar.getInstance().apply {
                    clear(); set(year, month, day)
                }.timeInMillis)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .apply { datePicker.maxDate = System.currentTimeMillis() }.show()
        }, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(label ?: "Choose reporting period")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ExpandMore, contentDescription = null)
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ReportPeriod.entries.forEachIndexed { index, candidate ->
                SegmentedButton(
                    selected = candidate == selected,
                    onClick = { onPeriod(candidate) },
                    shape = SegmentedButtonDefaults.itemShape(index, ReportPeriod.entries.size),
                    label = { Text(candidate.label, maxLines = 2) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
    }
}

@Composable
private fun ReportsTopMetrics(data: ReportsInsightData) {
    var showCashContext by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            modifier = Modifier.weight(1.25f).heightIn(min = 112.dp),
            shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Money spent", color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge)
                Text(formatKesAmount(data.currentSpent),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge)
            }
        }
        Card(
            modifier = Modifier.weight(1f).heightIn(min = 112.dp),
            shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("vs previous period", color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelMedium)
                val change = data.spendingChange
                Text(if (change == null) "—" else "${abs(change.toLong())}%",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(when { change == null -> "No comparison"
                    change < 0 -> "Lower spending"
                    change > 0 -> "Higher spending"
                    else -> "Unchanged" },
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    val fees = data.current.summary.feesMinor
    if (fees != 0L) Text("${formatKesAmount(fees)} in fees tracked separately",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton(onClick = { showCashContext = !showCashContext },
        modifier = Modifier.heightIn(min = 48.dp)) {
        Text(if (showCashContext) "Hide other cash movements" else "See money in & transfers")
    }
    if (showCashContext) {
        PesaCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Other recorded cash movements", style = MaterialTheme.typography.titleSmall)
                Text("Money in ${formatKesAmount(data.current.summary.incomeMinor)}")
                Text("Transfers ${formatKesAmount(data.current.summary.transferMinor)}")
                Text("Transfers are movements between accounts, not earned income or spending.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReportsTrendCard(values: List<ReportTrendValue>, period: ReportPeriod) {
    PesaCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Rounded.ShowChart)
                Text("Spending trend", Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(if (period == ReportPeriod.WEEK) "Daily spending this week"
                else if (period == ReportPeriod.YEAR) "Monthly spending this year"
                else "Monthly spending · last 6 months",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (values.isEmpty() || values.all { it.spentMinor == 0L }) {
                Text("No recorded spending to chart yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                ReportsTrendPlot(values)
            }
        }
    }
}

@Composable
private fun ReportsTrendPlot(values: List<ReportTrendValue>) {
    val primary = MaterialTheme.colorScheme.primary
    val rule = MaterialTheme.colorScheme.outlineVariant
    val largest = max(1L, values.maxOf { it.spentMinor.coerceAtLeast(0L) })
    Column {
        Text("Up to ${formatKesAmount(largest)}", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Canvas(Modifier.fillMaxWidth().height(136.dp).semantics {
            contentDescription = "Spending trend: " + values.joinToString { "${it.label}, ${formatKesAmount(it.spentMinor)}" }
        }) {
            val left = 4.dp.toPx()
            val right = size.width - 4.dp.toPx()
            val top = 8.dp.toPx()
            val bottom = size.height - 8.dp.toPx()
            val step = if (values.size > 1) (right - left) / (values.size - 1) else 0f
            val stroke = 3.dp.toPx()
            val points = values.mapIndexed { index, value ->
                androidx.compose.ui.geometry.Offset(
                    left + step * index,
                    bottom - (value.spentMinor.coerceAtLeast(0L).toDouble() / largest.toDouble() *
                        (bottom - top)).toFloat()
                )
            }
            drawLine(rule, start = androidx.compose.ui.geometry.Offset(left, bottom),
                end = androidx.compose.ui.geometry.Offset(right, bottom), strokeWidth = 1.dp.toPx())
            val area = Path().apply {
                moveTo(points.first().x, bottom)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, bottom)
                close()
            }
            drawPath(area, primary.copy(alpha = 0.10f))
            val line = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(line, primary, style = Stroke(width = stroke))
            points.forEach { point -> drawCircle(primary, radius = 4.dp.toPx(), center = point) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            values.forEachIndexed { index, value ->
                if (values.size <= 7 || index % 2 == 0 || index == values.lastIndex) {
                    Text(value.label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ReportsWhatChanged(data: ReportsInsightData, period: ReportPeriod) {
    PesaCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("What changed", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            val change = data.spendingChange
            ReportsInsightLine(
                icon = if (change != null && change > 0) Icons.Rounded.TrendingUp
                    else Icons.Rounded.TrendingDown,
                text = when {
                    change == null -> "No comparable spending in the previous ${period.label.lowercase()}."
                    change < 0 -> "Spending fell by ${abs(change.toLong())}%"
                    change > 0 -> "Spending rose by ${abs(change.toLong())}%"
                    else -> "Spending is unchanged from the previous period."
                }
            )
            data.topCategory?.let { ReportsInsightLine(Icons.Rounded.BarChart,
                "${it.first} is your largest category · ${formatKesAmount(it.second)}") }
        }
    }
}

@Composable
private fun ReportsInsightLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBubble(icon, size = 40.dp)
        Text(text, Modifier.weight(1f).padding(start = 12.dp),
            style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ReportsCategoryCard(
    spending: List<Pair<String, Long>>,
    expanded: Boolean,
    toggleExpanded: () -> Unit,
    onReviewBroad: () -> Unit,
    broadCount: Int
) {
    var showHelp by remember { mutableStateOf(false) }
    PesaCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Top spending categories", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            if (spending.isEmpty()) {
                Text("No categorized spending for this period.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val total = spending.fold(java.math.BigDecimal.ZERO) { sum, entry ->
                    sum.add(java.math.BigDecimal.valueOf(entry.second))
                }.max(java.math.BigDecimal.ONE)
                spending.take(if (expanded) spending.size else 4).forEach { (label, minor) ->
                    val ratio = java.math.BigDecimal.valueOf(minor)
                        .divide(total, 6, java.math.RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
                    ReportsCategoryBar(label, minor, ratio)
                }
                if (spending.size > 4) TextButton(onClick = toggleExpanded,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (expanded) "Show top 4" else "See all ${spending.size} categories")
                }
            }
            TextButton(onClick = { showHelp = !showHelp }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (showHelp) "Hide category explanation" else "About categories")
                Icon(Icons.Rounded.Info, contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp))
            }
            if (showHelp) Text("Other is a broad spending label. Uncategorized means no " +
                "category was assigned. Both are included in the relevant totals; " +
                "choosing a category changes the label, never the transaction amount.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (showHelp && broadCount > 0) TextButton(onClick = onReviewBroad,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Review Other / Uncategorized entries ($broadCount)")
            }
        }
    }
}

@Composable
private fun ReportsCategoryBar(label: String, amountMinor: Long, ratio: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), maxLines = 2,
                overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            Column(horizontalAlignment = Alignment.End) {
                Text(formatKesAmount(amountMinor), style = MaterialTheme.typography.labelLarge)
                Text("${(ratio * 100).toInt()}%", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LinearProgressIndicator(
            progress = { ratio }, modifier = Modifier.fillMaxWidth().height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private enum class ReportReviewType { LARGEST, UNCATEGORIZED, BROAD }

@Composable
private fun ReportsReviewActions(
    uncategorizedCount: Int,
    onLargest: () -> Unit,
    onUncategorized: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Review transactions", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportsReviewTile("Largest expenses", Icons.Rounded.BarChart,
                Modifier.weight(1f), onLargest)
            ReportsReviewTile("Uncategorized", Icons.Rounded.LabelOutline,
                Modifier.weight(1f), onUncategorized, uncategorizedCount)
        }
    }
}

@Composable
private fun ReportsReviewTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit,
    count: Int? = null
) {
    PesaCard(modifier.clickable(onClick = onClick)) {
        Column(Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.weight(1f))
                if (count != null && count > 0) SmallStatusPill(count.toString(), tone = "amber")
                Icon(Icons.Rounded.ChevronRight, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 2)
        }
    }
}

@Composable
private fun ReportsPaycheckEntry(onClick: () -> Unit) {
    PesaCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Rounded.ReceiptLong)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Paycheck analysis", style = MaterialTheme.typography.titleMedium)
                Text("Salary, deductions & net pay",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportExpenseReviewSheet(
    type: ReportReviewType,
    rows: List<ReportReviewCandidate>,
    periodLabel: String,
    onDismiss: () -> Unit,
    onTransactionClick: (Long) -> Unit
) {
    val title = when (type) {
        ReportReviewType.LARGEST -> "Largest expenses"
        ReportReviewType.UNCATEGORIZED -> "Uncategorized expenses"
        ReportReviewType.BROAD -> "Other / Uncategorized expenses"
    }
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(periodLabel, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (type == ReportReviewType.LARGEST)
                "Highest recorded expense entries. Fees, refunds and split payments are not included here."
            else "Open any transaction to choose a category. The amount will not change.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (rows.isEmpty()) {
                Text(if (type == ReportReviewType.LARGEST)
                    "No eligible recorded expenses in this period."
                else "All eligible recorded expenses already have a category.",
                    style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rows, key = { it.id }) { entry ->
                        PesaCard(Modifier.fillMaxWidth().clickable { onTransactionClick(entry.id) }) {
                            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.counterparty?.takeIf { it.isNotBlank() }
                                        ?: "Expense without a name", maxLines = 2,
                                        style = MaterialTheme.typography.bodyMedium)
                                    Text(entry.category.replace('_', ' '),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(formatKesAmount(entry.amountMinor),
                                    style = MaterialTheme.typography.labelLarge)
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}
