package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.data.local.ObservedInvestmentPosition
import com.kedevjimmy.pesatrack.data.local.ObservedInvestmentEvent
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val coffee = Color(0xFF552510)
private val mutedCoffee = Color(0xFF846554)
private val thinBorder = Color(0xFFF0DCCE)
private val ivory = Color(0xFFFCF6F0)

/** Short screen label only; the full identifier is never shown in this list. */
internal fun compactAccountLabel(raw: String): String = raw
    .replace(Regex("(?i)\\s+Kenya(?=\\s*(?:\\(|$))"), "")
    .replace(Regex("\\(\\s*•+\\s*([^)]*)\\)")) { match ->
        val suffix = match.groupValues[1].trim()
        if (suffix.matches(Regex("[A-Za-z0-9]{1,4}"))) "··· ${suffix.takeLast(3)}"
        else ""
    }.trimEnd()

@Composable
internal fun ApprovedBalanceHero(lines: List<AccountDisplayLine>, groupCount: Int,
    hidden: Boolean, onToggleHidden: () -> Unit) {
    val summary = accountVerifiedBalanceSummary(lines)
    val total = summary.totalMinor
    val complete = summary.complete
    Box(Modifier.fillMaxWidth().height(204.dp)) {
        Image(painterResource(R.drawable.accounts_balance_building), null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Column(Modifier.align(Alignment.CenterStart).padding(start = 12.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (complete) "TOTAL ACROSS ACCOUNTS" else "RECORDED ACCOUNT BALANCES",
                color = coffee, style = MaterialTheme.typography.labelMedium,
                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
                fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (hidden) "••••••" else total?.let(::formatKesAmount) ?: "Unavailable",
                    color = coffee, fontFamily = FontFamily.Serif,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold, maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false))
                IconButton(onClick = onToggleHidden,
                    modifier = Modifier.padding(start = 5.dp)
                        .background(Color.White.copy(alpha = .86f), CircleShape)) {
                    Icon(if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        if (hidden) "Show balances" else "Hide balances", tint = coffee)
                }
            }
            Text(if (complete) "Balances from $groupCount accounts"
                else "Recorded: ${summary.recordedCount} of ${lines.size} accounts · estimates excluded",
                color = mutedCoffee,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        }
    }
}

@Composable
internal fun ApprovedAccountsList(lines: List<AccountDisplayLine>, savings: List<AccountDisplayLine>,
    hidden: Boolean, onAccountClick: ((String) -> Unit)?, onAdd: (() -> Unit)?,
    onBalanceClick: ((String) -> Unit)?) {
    var expanded by remember { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AccountHeading("Your accounts", Modifier.weight(1f))
            if (onAdd != null) OutlinedButton(onClick = onAdd,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                border = BorderStroke(1.dp, coffee)) {
                Text("Add account", color = coffee)
                Icon(Icons.Rounded.Add, null, Modifier.padding(start = 4.dp).size(18.dp), tint = coffee)
            }
        }
        Surface(shape = RoundedCornerShape(22.dp), color = Color.White,
            border = BorderStroke(1.dp, thinBorder)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 5.dp)) {
                val hasMpesa = lines.any { it.name.contains("M-Pesa", true) }
                lines.forEachIndexed { index, line ->
                    if (index > 0) HorizontalDivider(color = thinBorder)
                    AccountMainRow(line, hidden,
                        expandable = line.name.contains("M-Pesa", true) && savings.isNotEmpty(),
                        onBalanceClick = line.accountId?.let { id ->
                            onBalanceClick?.let { callback -> { callback(id) } }
                        },
                        onClick = {
                            if (line.name.contains("M-Pesa", true) && savings.isNotEmpty())
                                expanded = !expanded
                            else line.accountId?.let { onAccountClick?.invoke(it) }
                        })
                    if (line.name.contains("M-Pesa", true) && savings.isNotEmpty() && expanded)
                        AccountSavingsBranches(savings, hidden, onAccountClick)
                }
                if (!hasMpesa) savings.forEach { line ->
                    HorizontalDivider(color = thinBorder)
                    AccountMainRow(line, hidden, false,
                        onBalanceClick = line.accountId?.let { id ->
                            onBalanceClick?.let { callback -> { callback(id) } }
                        }) {
                        line.accountId?.let { onAccountClick?.invoke(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountMainRow(line: AccountDisplayLine, hidden: Boolean, expandable: Boolean,
    onBalanceClick: (() -> Unit)?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically) {
        AccountProviderMark(line.name, Modifier.size(40.dp))
        Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(30.dp)
            .background(thinBorder))
        Text(compactAccountLabel(line.name), Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium, color = coffee,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        Text(if (hidden) "••••" else line.balanceMinor?.let {
            if (line.quality == com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality.RECONSTRUCTED)
                "Est. ${formatKesAmount(it)}" else formatKesAmount(it)
        } ?: "—",
            style = MaterialTheme.typography.bodySmall, color = coffee,
            fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (onBalanceClick != null) IconButton(onClick = onBalanceClick,
            modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.Edit, contentDescription = "Enter ${line.name} balance",
                modifier = Modifier.size(20.dp), tint = coffee)
        }
        Icon(if (expandable) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
            null, Modifier.padding(start = 4.dp).size(22.dp), tint = mutedCoffee)
    }
}

@Composable
private fun AccountSavingsBranches(savings: List<AccountDisplayLine>, hidden: Boolean,
    onAccountClick: ((String) -> Unit)?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.width(32.dp).height((savings.size * 57).dp)) {
            val x = 15.dp.toPx()
            val start = 0f
            val lastY = size.height - 28.dp.toPx()
            drawLine(Color(0xFFFFC39C), Offset(x, start), Offset(x, lastY), 1.7.dp.toPx())
            savings.indices.forEach { i ->
                val y = (i * 57 + 28).dp.toPx()
                drawLine(Color(0xFFFFC39C), Offset(x, y), Offset(size.width, y), 1.7.dp.toPx())
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            savings.forEach { child ->
                Row(Modifier.fillMaxWidth().heightIn(min = 53.dp)
                    .clip(RoundedCornerShape(12.dp)).background(ivory)
                    .clickable { child.accountId?.let { onAccountClick?.invoke(it) } }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (child.name.contains("lock", true)) Icons.Rounded.Lock else Icons.Rounded.Savings,
                        null, Modifier.size(21.dp), tint = Color(0xFF9C5629))
                    Text(child.name, Modifier.weight(1f).padding(start = 10.dp),
                        style = MaterialTheme.typography.bodyMedium, color = coffee,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (hidden) "••••" else child.balanceMinor?.let(::formatKesAmount) ?: "—",
                        style = MaterialTheme.typography.bodyMedium, color = coffee)
                }
            }
        }
    }
}

@Composable
internal fun ApprovedInvestmentEntry(positions: List<ObservedInvestmentPosition>, onOpen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        AccountHeading("Investments")
        Surface(onClick = onOpen, shape = RoundedCornerShape(19.dp), color = Color.White,
            border = BorderStroke(1.dp, thinBorder)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(43.dp).background(Color(0xFFE3E8E9), RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center) {
                    Text("NSE", color = Color(0xFF30444E), fontWeight = FontWeight.ExtraBold)
                }
                Box(Modifier.padding(horizontal = 12.dp).width(1.dp).height(30.dp).background(thinBorder))
                Text(if (positions.isEmpty()) "Shares & investments" else "Investment portfolio",
                    modifier = Modifier.weight(1f), color = coffee,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("View portfolio", color = mutedCoffee,
                    style = MaterialTheme.typography.bodyMedium)
                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(22.dp), tint = coffee)
            }
        }
    }
}

@Composable
internal fun ApprovedAccountActivity(rows: List<StoredTransaction>, accountNames: Map<String, String>,
    hidden: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        AccountHeading("Recent account activity")
        Surface(shape = RoundedCornerShape(19.dp), color = Color.White,
            border = BorderStroke(1.dp, thinBorder)) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 5.dp)) {
                if (rows.isEmpty()) Text("No account transfers or salary receipts yet",
                    Modifier.padding(vertical = 14.dp),
                    color = mutedCoffee, style = MaterialTheme.typography.bodyMedium)
                rows.take(3).forEachIndexed { index, transaction ->
                    if (index > 0) HorizontalDivider(color = thinBorder)
                    val salary = transaction.direction == "INCOME"
                    val origin = transaction.accountId?.let(accountNames::get) ?: "Account"
                    val destination = transaction.linkedAccountId?.let(accountNames::get) ?: "Account"
                    val label = if (salary) "Salary — $origin" else "$origin to $destination"
                    Row(Modifier.fillMaxWidth().heightIn(min = 67.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(43.dp).background(
                            if (salary) Color(0xFFEAF1D9) else Color(0xFFFFEADA), CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(if (salary) Icons.Rounded.ArrowDownward else Icons.Rounded.SwapHoriz,
                                null, tint = if (salary) Color(0xFF2D501C) else Color(0xFFA04E23))
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(label, color = coffee, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text((if (salary) "Money in" else "Transfer") + " · " +
                                SimpleDateFormat("d MMM yyyy", locale).format(Date(transaction.occurredAtEpochMs)),
                                color = mutedCoffee, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(if (hidden) "••••" else formatKesAmount(transaction.amountMinor),
                            color = coffee, fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ApprovedInvestmentsPage(padding: PaddingValues,
    positions: List<ObservedInvestmentPosition>, events: List<ObservedInvestmentEvent>, onBack: () -> Unit) {
    InvestmentPortfolioContent(padding, positions, events, onBack)
}

internal fun investmentCompanyName(label: String): String = when {
    label.equals("SCOM", true) || label.contains("Safaricom", true) -> "Safaricom"
    label.equals("KPLC", true) || label.contains("Kenya Power", true) -> "Kenya Power"
    label.equals("ZIIDI", true) || label.contains("Ziidi", true) -> "Ziidi Money Market Fund"
    label.equals("KCB", true) || label.contains("KCB Group", true) -> "KCB Group"
    else -> label
}

internal fun investmentTicker(label: String): String = when {
    label.equals("SCOM", true) || label.contains("Safaricom", true) -> "SCOM"
    label.equals("KPLC", true) || label.contains("Kenya Power", true) -> "KPLC"
    label.equals("ZIIDI", true) || label.contains("Ziidi", true) -> "ZIIDI"
    label.equals("KCB", true) || label.contains("KCB Group", true) -> "KCB"
    else -> "INV"
}

@Composable
private fun AccountHeading(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier = modifier, fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold, color = coffee,
        style = MaterialTheme.typography.headlineSmall)
}
