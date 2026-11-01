package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.data.local.StatementBankArchiveStore
import com.kedevjimmy.pesatrack.domain.statements.StatementSavedAccountVisibility
import com.kedevjimmy.pesatrack.domain.statements.StatementUnifiedOverviewPolicy
import java.text.DateFormatSymbols
import java.util.Locale

/** An all-account statement evidence index with verified gross cash movements. */
@Composable
internal fun StatementUnifiedOverviewPage(
    overview: StatementUnifiedOverviewPolicy.Overview,
    years: List<Int>,
    accountLabels: Map<String, String>,
    bankAccountIds: Set<String>,
    onSelectYear: (Int) -> Unit,
    onSelectAccount: (String) -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
    onOpenReview: () -> Unit = {},
    limitedSources: List<StatementBankArchiveStore.Source> = emptyList(),
    reportAccountIds: List<String>? = null
) {
    val p = HomeReferencePalette
    val shortMonths = remember { DateFormatSymbols(Locale.ENGLISH).shortMonths }
    var yearMenu by remember { mutableStateOf(false) }
    var accountMenu by remember { mutableStateOf(false) }
    val accounts = overview.accountRows
    val accountInventory = remember(accounts, limitedSources, reportAccountIds) {
        StatementSavedAccountVisibility.overview(
            reportAccountIds = reportAccountIds ?: accounts.map { it.accountId },
            limitedAccountIds = limitedSources.map { it.accountId }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(p.canvas)
            .testTag("statements-unified-overview"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Statements", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.6).sp, color = p.ink)
                    Text("Explore your imported history.", color = p.muted, fontSize = 14.sp)
                }
                Surface(shape = CircleShape, color = Color(0xFFEDEBFF)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.Close, "Close Statements", tint = p.ink)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(Modifier.weight(.42f)) {
                    Surface(color = Color.White, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().clickable { yearMenu = true }) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CalendarMonth, null, tint = p.purple,
                                modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(overview.year.toString(), fontWeight = FontWeight.Bold,
                                color = p.ink, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.ExpandMore, "Choose year", tint = p.ink,
                                modifier = Modifier.size(19.dp))
                        }
                    }
                    DropdownMenu(expanded = yearMenu, onDismissRequest = { yearMenu = false }) {
                        years.distinct().sortedDescending().forEach { year ->
                            DropdownMenuItem(text = { Text(year.toString()) }, onClick = {
                                yearMenu = false; onSelectYear(year)
                            })
                        }
                    }
                }
                Box(Modifier.weight(.58f)) {
                    Surface(color = Color.White, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().clickable(enabled = accountInventory.accountIds.isNotEmpty()) {
                            accountMenu = true
                        }) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Layers, null, tint = p.purple,
                                modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("All accounts", color = p.ink, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.ExpandMore, "Choose account", tint = p.ink,
                                modifier = Modifier.size(19.dp))
                        }
                    }
                    DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }) {
                        accountInventory.accountIds.forEach { id ->
                            DropdownMenuItem(text = {
                                val limitedBadge = if (id in accountInventory.limitedOnlyIds) " · Limited" else ""
                                Text(displayStatementAccountLabel(accountLabels[id],
                                    id in bankAccountIds) + limitedBadge)
                            }, onClick = { accountMenu = false; onSelectAccount(id) })
                        }
                    }
                }
            }
        }
        if (limitedSources.isNotEmpty()) item {
            StatementUnifiedWhiteCard {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Limited assurance statements · all years", color = p.ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text("Saved source rows only. Not included in verified cash-flow totals or Activity.",
                        color = p.muted, style = MaterialTheme.typography.bodySmall)
                    limitedSources.forEach { source ->
                        Row(Modifier.fillMaxWidth().clickable { onSelectAccount(source.accountId) }
                            .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AccountBalance, null, tint = p.purple)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(accountLabels[source.accountId] ?: "Bank statement",
                                    color = p.ink, fontWeight = FontWeight.SemiBold)
                                Text("${source.rows} saved rows · ${statementDate(source.lastAt)}",
                                    color = p.muted, style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Rounded.ChevronRight, "Open saved bank statement", tint = p.muted)
                        }
                    }
                }
            }
        }
        if (accounts.isEmpty() && limitedSources.isNotEmpty()) item {
            TextButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                Text("Import another statement")
            }
        }
        if (accounts.isEmpty() && limitedSources.isEmpty()) {
            item {
                StatementUnifiedWhiteCard {
                    Column(Modifier.fillMaxWidth().padding(23.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = CircleShape, color = Color(0xFFEEE7FF)) {
                            Icon(Icons.Rounded.Description, null, tint = p.purple,
                                modifier = Modifier.padding(17.dp).size(31.dp))
                        }
                        Text("No statements imported yet", color = p.ink,
                            fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Import an M-Pesa or supported bank statement to explore your historical money records. Activity remains separate.",
                            color = p.muted, fontSize = 13.sp)
                        Button(onClick = onImport, shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = p.purple)) {
                            Icon(Icons.Rounded.Add, null)
                            Spacer(Modifier.width(7.dp))
                            Text("Import statement")
                        }
                    }
                }
            }
        } else if (accountInventory.accountIds.isNotEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().background(Brush.linearGradient(
                        listOf(Color(0xFF8B43FF), Color(0xFF5014DF), Color(0xFF2941CA))))) {
                        Canvas(Modifier.fillMaxWidth().height(218.dp)) {
                            drawCircle(Color(0xFFBC98FF).copy(alpha = .28f),
                                radius = size.width * .28f,
                                center = Offset(size.width * .96f, size.height * .06f))
                            drawCircle(Color(0xFF63E3FF).copy(alpha = .18f),
                                radius = size.width * .22f,
                                center = Offset(size.width * .8f, size.height * 1.1f))
                        }
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(16.dp),
                                    color = Color.White.copy(alpha = .19f)) {
                                    Icon(Icons.Rounded.Description, null, tint = Color.White,
                                        modifier = Modifier.padding(14.dp).size(24.dp))
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("Your statement history", color = Color.White.copy(alpha = .94f),
                                        fontSize = 13.sp)
                                    Text("${accountInventory.accountIds.size} ${if (accountInventory.accountIds.size == 1) "account" else "accounts"} on file",
                                        color = Color.White, fontWeight = FontWeight.ExtraBold,
                                        fontSize = 23.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (accountInventory.limitedOnlyIds.isNotEmpty())
                                        "${accountInventory.reportCount} in reports · ${accountInventory.limitedOnlyIds.size} limited-only"
                                        else "Imported records · ${overview.year}", color = Color.White.copy(alpha = .86f),
                                        fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.height(17.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = .22f), thickness = .8.dp)
                            Spacer(Modifier.height(13.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                StatementUnifiedCashMetric("Money in", overview.cashSummary.moneyInMinor,
                                    Modifier.weight(1f))
                                StatementUnifiedCashMetric("Money out", overview.cashSummary.moneyOutMinor,
                                    Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(when (overview.cashSummary.status) {
                                StatementUnifiedOverviewPolicy.CashStatus.AVAILABLE ->
                                    "Gross account movements · includes own transfers"
                                StatementUnifiedOverviewPolicy.CashStatus.NEEDS_REVIEW ->
                                    "Amounts on hold · review overlaps"
                                StatementUnifiedOverviewPolicy.CashStatus.UNSUPPORTED_CURRENCY ->
                                    "Amounts on hold · multiple currencies"
                                StatementUnifiedOverviewPolicy.CashStatus.NO_IMPORT ->
                                    "No imported cash movements"
                            }, color = Color.White.copy(alpha = .86f), fontSize = 11.sp,
                                maxLines = 2)
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    StatementUnifiedMetricCard(
                        "Months on file", "${overview.monthsOnFile} / 12 months",
                        "Not necessarily full months", Icons.Rounded.CheckCircle,
                        Color(0xFF07926B), Color(0xFFE0F8F0), Modifier.weight(1f)
                    )
                    StatementUnifiedMetricCard(
                        "Needs review", "${overview.monthsNeedingReview} months",
                        "Potential overlaps", Icons.Rounded.WarningAmber,
                        Color(0xFFE78316), Color(0xFFFFF0DF), Modifier.weight(1f),
                        onClick = onOpenReview.takeIf { overview.monthsNeedingReview > 0 }
                    )
                }
            }
            item {
                StatementUnifiedWhiteCard {
                    Column(Modifier.padding(horizontal = 13.dp, vertical = 15.dp),
                        verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Insights, null, tint = p.purple)
                            Spacer(Modifier.width(8.dp))
                            Text("Monthly statement coverage", fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp, color = p.ink)
                        }
                        Text("Months with imported evidence, not cash totals", fontSize = 12.sp,
                            color = p.muted)
                        overview.accountRows.forEach { row ->
                            val isBank = row.accountId in bankAccountIds
                            val label = displayStatementAccountLabel(accountLabels[row.accountId], isBank)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(label, color = p.ink, fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, modifier = Modifier.width(68.dp))
                                Spacer(Modifier.width(5.dp))
                                (1..12).forEach { month ->
                                    val cell = overview.accountMonths.first { it.accountId == row.accountId && it.month == month }
                                    val color = when (cell.state) {
                                        StatementUnifiedOverviewPolicy.MonthState.NO_IMPORT -> Color(0xFFE6E7F0)
                                        StatementUnifiedOverviewPolicy.MonthState.NEEDS_REVIEW -> Color(0xFFFFAD52)
                                        StatementUnifiedOverviewPolicy.MonthState.ON_FILE ->
                                            if (isBank) Color(0xFF38A1FA) else p.violet
                                    }
                                    Box(Modifier.weight(1f).padding(horizontal = 1.5.dp)
                                        .height(16.dp).clip(RoundedCornerShape(4.dp))
                                        .background(color)
                                        .semantics {
                                            contentDescription = "$label, ${shortMonths[month - 1]} ${overview.year}: ${when (cell.state) {
                                                StatementUnifiedOverviewPolicy.MonthState.NO_IMPORT -> "No import"
                                                StatementUnifiedOverviewPolicy.MonthState.NEEDS_REVIEW -> "Needs review"
                                                StatementUnifiedOverviewPolicy.MonthState.ON_FILE -> "On file"
                                            }}"
                                        }.clickable { onSelectAccount(row.accountId) })
                                }
                            }
                        }
                        Row(Modifier.padding(start = 73.dp)) {
                            (1..12).forEach { month ->
                                Text(shortMonths[month - 1].take(1),
                                    color = p.muted, fontSize = 9.sp,
                                    modifier = Modifier.weight(1f), maxLines = 1)
                            }
                        }
                        Text("Amber = review · Grey = no import · A month on file may be partial",
                            fontSize = 11.sp, color = p.muted)
                    }
                }
            }
            item {
                StatementUnifiedWhiteCard {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
                        Text("Your accounts", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                            color = p.ink)
                        Spacer(Modifier.height(10.dp))
                        accounts.forEachIndexed { index, row ->
                            val bank = row.accountId in bankAccountIds
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectAccount(row.accountId) }
                                .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(13.dp),
                                    color = if (bank) Color(0xFFFFE9E8) else Color(0xFFDDF8EB)) {
                                    Icon(if (bank) Icons.Rounded.AccountBalance else Icons.Rounded.Smartphone,
                                        null, tint = if (bank) Color(0xFFCE3B2D) else Color(0xFF069C63),
                                        modifier = Modifier.padding(13.dp).size(21.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(displayStatementAccountLabel(accountLabels[row.accountId], bank),
                                        color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Months on file: ${row.monthsOnFile} of 12",
                                        color = p.muted, fontSize = 12.sp)
                                }
                                if (row.monthsNeedingReview > 0) {
                                    Surface(color = Color(0xFFFFF0DF), shape = RoundedCornerShape(9.dp)) {
                                        Text("${row.monthsNeedingReview} review", color = Color(0xFFB96008),
                                            fontSize = 10.sp, modifier = Modifier.padding(5.dp))
                                    }
                                }
                                Icon(Icons.Rounded.ChevronRight, null, tint = p.muted)
                            }
                            if (index < accounts.lastIndex)
                                HorizontalDivider(color = p.line, thickness = .6.dp)
                        }
                    }
                }
            }
            if (overview.monthsNeedingReview > 0) item {
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFFFF2E5),
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenReview)) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, null, tint = Color(0xFFE27D0E))
                        Spacer(Modifier.width(10.dp))
                        Text("${overview.monthsNeedingReview} months need review",
                            modifier = Modifier.weight(1f), color = Color(0xFFC96510),
                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFFC96510))
                    }
                }
            }
            item {
                Button(onClick = onImport, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = p.purple)) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import another statement", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatementUnifiedCashMetric(label: String, amount: Long?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = Color.White.copy(alpha = .87f), fontSize = 12.sp,
            fontWeight = FontWeight.Medium)
        Text(amount?.let(::statementMoney) ?: "—", color = Color.White,
            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatementUnifiedWhiteCard(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(19.dp), color = Color.White,
        modifier = Modifier.fillMaxWidth()) { content() }
}

@Composable
private fun StatementUnifiedMetricCard(
    title: String, value: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color, iconBackground: Color, modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick)
        else Modifier),
        color = Color.White, shape = RoundedCornerShape(17.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(11.dp), color = iconBackground) {
                    Icon(icon, null, tint = tint,
                        modifier = Modifier.padding(7.dp).size(19.dp))
                }
                Spacer(Modifier.width(7.dp))
                Text(title, color = HomeReferencePalette.ink, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            Text(value, color = HomeReferencePalette.ink, fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = HomeReferencePalette.muted, fontSize = 10.sp,
                maxLines = 2)
        }
    }
}

private val accountIdentifierPattern = Regex("[0-9]{5,}")

private fun displayStatementAccountLabel(raw: String?, bank: Boolean): String =
    (raw?.takeIf { it.isNotBlank() } ?: if (bank) "Bank account" else "M-Pesa wallet")
        .replace(accountIdentifierPattern, "••••")
