package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.domain.everyday.*

/** The account-specific Phase 1 reference layout, populated solely from local statement data. */
@Composable
internal fun StatementOverviewPage(
    model: StatementInsightViewModel,
    onBack: () -> Unit,
    onSelectScope: (StatementReportScope) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenTrends: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenCoverage: () -> Unit,
    onImport: () -> Unit,
    onOpenTransactions: (StatementFlow?, String?) -> Unit,
    onOpenMonth: (String) -> Unit,
    onOpenCategory: (String) -> Unit,
    availableAccounts: List<Pair<String, String>> = emptyList(),
    onSelectAccount: (String) -> Unit = {},
    onOpenCashMovement: (Boolean) -> Unit = {},
    sourceLabel: String = "M-Pesa"
) {
    val p = HomeReferencePalette
    val selectedYear = model.chartYear
    val sourceYears = model.sourceMonths.map { it.usage.year }.distinct().sortedDescending()
    val displayedMonths = model.sourceMonths.filter { it.usage.year == selectedYear }
    val scopeTitle = when (val period = model.scope) {
        StatementReportScope.AllHistory -> "All history"
        is StatementReportScope.Year -> period.value.toString()
        is StatementReportScope.Month -> period.key
        is StatementReportScope.Week -> "Week of ${statementDate(period.anchorMs)}"
        is StatementReportScope.Day -> statementDate(period.anchorMs)
    }
    var dateMenu by remember { mutableStateOf(false) }
    var walletMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(p.canvas).testTag("statements-overview"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFE9E7FE),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back to previous screen",
                            tint = p.ink)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(sourceLabel, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.7).sp, color = p.ink)
                    Text("Statement insights", fontSize = 15.sp, color = p.muted)
                    Spacer(Modifier.height(8.dp))
                    Box {
                        Surface(color = Color.White, shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.clickable(enabled = availableAccounts.size > 1) {
                                walletMenu = true
                            }) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Smartphone, null, tint = p.green,
                                    modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(7.dp))
                                Text(model.accountLabel, color = p.ink,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (availableAccounts.size > 1)
                                    Icon(Icons.Rounded.ExpandMore, "Choose wallet", tint = p.muted)
                            }
                        }
                        DropdownMenu(expanded = walletMenu,
                            onDismissRequest = { walletMenu = false }) {
                            availableAccounts.forEach { (id, name) ->
                                DropdownMenuItem(text = { Text(name) }, onClick = {
                                    walletMenu = false; onSelectAccount(id)
                                })
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Surface(color = Color.White, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.clickable { dateMenu = true }) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CalendarMonth, null, tint = p.muted,
                                modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(when (model.scope) {
                                StatementReportScope.AllHistory -> "All history"
                                is StatementReportScope.Month -> model.scope.key
                                is StatementReportScope.Week -> "Week"
                                is StatementReportScope.Day -> "Day"
                                is StatementReportScope.Year -> selectedYear.toString()
                            }, color = p.ink,
                                fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Rounded.ExpandMore, "Choose reporting year", tint = p.ink)
                        }
                    }
                    DropdownMenu(expanded = dateMenu, onDismissRequest = { dateMenu = false }) {
                        sourceYears.forEach { year ->
                            DropdownMenuItem(text = { Text(year.toString()) }, onClick = {
                                dateMenu = false; onSelectScope(StatementReportScope.Year(year))
                            })
                        }
                        DropdownMenuItem(text = { Text("All imported dates") }, onClick = {
                            dateMenu = false; onSelectScope(StatementReportScope.AllHistory)
                        })
                    }
                }
                Surface(color = Color.White, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).clickable(onClick = onOpenCoverage)) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (model.hasMissingMonths) Icons.Rounded.Info
                            else Icons.Rounded.CheckCircle, null,
                            tint = if (model.hasMissingMonths) Color(0xFFD68119) else p.green,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${displayedMonths.size}/12 months", color = p.muted,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Surface(color = Color.White, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.clickable(onClick = onOpenArchive)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Archive", color = p.purple, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(5.dp))
                        Icon(Icons.Rounded.ArrowForward, null, tint = p.purple,
                            modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(23.dp), color = p.purple,
                modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().background(Brush.linearGradient(
                    listOf(Color(0xFF6515F6), Color(0xFF2712B0), Color(0xFF003BCD))))) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                        drawCircle(Color(0xFF00CFF5).copy(alpha = .37f),
                            radius = size.width * .28f,
                            center = androidx.compose.ui.geometry.Offset(size.width * .93f, 0f))
                        drawCircle(Color(0xFF845AFF).copy(alpha = .30f),
                            radius = size.width * .25f,
                            center = androidx.compose.ui.geometry.Offset(size.width * .82f, size.height * .98f))
                    }
                    Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("$sourceLabel spending · $scopeTitle",
                            color = Color.White.copy(alpha = .95f),
                            style = MaterialTheme.typography.titleMedium)
                        Text(statementMoney(model.metrics.spentMinor),
                            color = Color.White, fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text("Classified expenses · transfers excluded",
                            color = Color.White.copy(alpha = .91f),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatementOverviewCashTile("Money in", model.metrics.moneyInMinor,
                    "Cash received", true, Modifier.weight(1f)) {
                    onOpenCashMovement(true)
                }
                StatementOverviewCashTile("Money out", model.metrics.moneyOutMinor,
                    "Cash sent", false, Modifier.weight(1f)) {
                    onOpenCashMovement(false)
                }
            }
        }
        if (model.hasUnclassifiedIncome) item {
            Text("Some incoming money still needs classification. Cash received is not necessarily income.",
                color = p.muted, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 5.dp))
        }
        item {
            StatementInsightCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Monthly spending", fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp, color = p.ink, modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenTrends) { Text("See trends", color = p.purple) }
                }
                Text("$selectedYear · Only imported months have data",
                    color = p.muted, style = MaterialTheme.typography.labelSmall)
                StatementMonthlyLineChart(model.monthlySpending) { monthNumber ->
                    model.sourceMonths.firstOrNull { it.usage.year == selectedYear &&
                        it.usage.month == monthNumber }?.let { onOpenMonth(it.key) }
                }
                if (model.hasMissingMonths) {
                    TextButton(onClick = onOpenCoverage) {
                        Icon(Icons.Rounded.Info, null, tint = p.purple,
                            modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("View coverage", color = p.purple)
                    }
                }
            }
        }
        item {
            StatementInsightCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Top spending categories", fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold, color = p.ink,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenCategories) { Text("View all", color = p.purple) }
                }
                val total = model.metrics.spentMinor.coerceAtLeast(1L)
                if (model.categories.isEmpty()) {
                    Text("No classified spending in the selected period.",
                        color = p.muted, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { onOpenTransactions(StatementFlow.UNKNOWN, null) }) {
                        Text("Review unclassified entries")
                    }
                } else model.categories.take(3).forEachIndexed { index, (category, amount) ->
                    val tint = listOf(Color(0xFFFF9E58), Color(0xFF269EFF), Color(0xFFAA6DFB))[index]
                    Row(Modifier.fillMaxWidth().clickable { onOpenCategory(category) }
                        .padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Surface(color = tint.copy(alpha = .15f), shape = RoundedCornerShape(11.dp)) {
                            Icon(when (index) {
                                0 -> Icons.Rounded.Restaurant
                                1 -> Icons.Rounded.DirectionsCar
                                else -> Icons.Rounded.ReceiptLong
                            }, null, tint = tint, modifier = Modifier.padding(10.dp).size(20.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(category, color = p.ink,
                                style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            LinearProgressIndicator(
                                progress = { (amount.toDouble() / total).toFloat().coerceIn(0f, 1f) },
                                color = tint, trackColor = Color(0xFFEEEDFC),
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
                        }
                        Text(statementMoney(amount), color = p.ink,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, maxLines = 1)
                        Icon(Icons.Rounded.ChevronRight, null, tint = p.muted,
                            modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
        item {
            StatementInsightCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Statement archive", fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold, color = p.ink,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenArchive) {
                        Text("All statements", color = p.purple)
                        Icon(Icons.Rounded.ArrowForward, null, tint = p.purple,
                            modifier = Modifier.size(15.dp))
                    }
                }
                if (displayedMonths.isEmpty()) Text("No statements for this year.",
                    color = p.muted, style = MaterialTheme.typography.bodySmall)
                else displayedMonths.take(3).forEach { entry ->
                    Row(Modifier.fillMaxWidth().clickable { onOpenMonth(entry.key) }
                        .padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFFEDE6FF), shape = RoundedCornerShape(11.dp)) {
                            Icon(Icons.Rounded.Description, null, tint = p.purple,
                                modifier = Modifier.padding(10.dp).size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.usage.label, color = p.ink,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text("${entry.usage.sourceRows} entries" +
                                (if (entry.sourceCount > 1) " · overlapping sources" else ""),
                                style = MaterialTheme.typography.labelSmall, color = p.muted)
                        }
                        Text(if (entry.source.independentlyVerified) "Checked" else "Saved",
                            color = if (entry.source.independentlyVerified) p.green else p.muted,
                            style = MaterialTheme.typography.labelSmall)
                        Icon(Icons.Rounded.ChevronRight, null, tint = p.muted,
                            modifier = Modifier.size(19.dp))
                    }
                }
                HorizontalDivider(color = p.line)
                TextButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, null, tint = p.purple)
                    Text("Import another statement", color = p.purple)
                }
            }
        }
    }
}

@Composable
internal fun StatementInsightCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White,
        shape = RoundedCornerShape(22.dp), shadowElevation = 2.dp) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp),
            content = content)
    }
}

@Composable
private fun StatementOverviewCashTile(
    title: String, amount: Long, detail: String, incoming: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val p = HomeReferencePalette
    val tint = if (incoming) p.green else p.red
    Surface(modifier = modifier.clickable(onClick = onClick), color = Color.White,
        shape = RoundedCornerShape(19.dp), shadowElevation = 1.dp) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = tint.copy(alpha = .10f),
                    shape = RoundedCornerShape(12.dp)) {
                    Icon(if (incoming) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                        null, tint = tint, modifier = Modifier.padding(9.dp).size(19.dp))
                }
                Spacer(Modifier.width(7.dp))
                Text(title, color = p.ink, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge)
            }
            Text(statementMoney(amount), color = p.ink, fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail + " · not classified spending", color = p.muted,
                style = MaterialTheme.typography.labelSmall, maxLines = 2)
        }
    }
}
