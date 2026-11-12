package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.PaydayAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.rules.TrackedAccountGroup
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens
import java.text.SimpleDateFormat
import java.util.Date

/** Approved November 2026 Home: flat sections and one gently contrasting balance surface. */
internal object HomeMonochromePalette {
    val canvas = PremiumVisualTokens.cream
    val surface = PremiumVisualTokens.card
    val ink = PremiumVisualTokens.ink
    val muted = PremiumVisualTokens.muted
    val line = PremiumVisualTokens.sandBorder
    val green = PremiumVisualTokens.leafReadable
    val softGreen = Color(0xFFE7EFD9)
    val dark = PremiumVisualTokens.coffee
}

@Composable
internal fun MonochromeAvailableMoney(
    available: String,
    netFlowMinor: Long?,
    netFlowCaption: String,
    formatMoney: (Long) -> String,
    moneyIn: String,
    expenses: String,
    month: String,
    onMonth: () -> Unit
) {
    val p = HomeMonochromePalette
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = p.surface) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Text("Your available money", color = p.ink, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(available, color = p.ink, fontSize = when {
                available.length > 17 -> 23.sp
                available.length > 13 -> 25.sp
                else -> 29.sp
            }, lineHeight = 34.sp, letterSpacing = (-0.8).sp,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            if (netFlowMinor != null) {
                Text("${if (netFlowMinor > 0) "+" else ""}${formatMoney(netFlowMinor)} $netFlowCaption",
                    color = if (netFlowMinor >= 0) p.green else Color(0xFFAB4239),
                    fontSize = 12.sp, fontWeight = FontWeight.Medium)
            } else {
                Text("From your confirmed pay cycle and records", color = p.muted,
                    fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Monthly activity", color = p.muted, fontSize = 12.sp,
                    fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Box(Modifier.heightIn(min = 48.dp)
                    .semantics { contentDescription = "Choose monthly activity month: $month" }
                    .clickable(role = Role.Button, onClickLabel = "Choose month", onClick = onMonth),
                    contentAlignment = Alignment.Center) {
                    Row(Modifier.clip(CircleShape).background(Color(0xFFEBEEEB))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(month, color = p.ink, fontSize = 12.sp,
                            fontWeight = FontWeight.Medium, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(18.dp))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(p.dark).padding(horizontal = 10.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically) {
                MonochromeMoneyMetric(Icons.Rounded.ArrowUpward, "Money in", moneyIn,
                    Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(43.dp).background(Color(0xFF555955)))
                MonochromeMoneyMetric(Icons.Rounded.ArrowDownward, "Expenses", expenses,
                    Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MonochromeMoneyMetric(icon: ImageVector, label: String, amount: String, modifier: Modifier) {
    Row(modifier.padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(35.dp).background(Color.White, CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(21.dp), tint = HomeMonochromePalette.ink)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = Color(0xFFC3C8C3), fontSize = 12.sp)
            Text(amount, color = Color.White, fontSize = when {
                amount.length > 15 -> 12.sp
                amount.length > 12 -> 13.sp
                else -> 15.sp
            }, fontWeight = FontWeight.SemiBold, maxLines = 2,
                lineHeight = 17.sp)
        }
    }
}

@Composable
private fun MonochromeSectionTitle(text: String, count: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp, color = HomeMonochromePalette.ink,
            modifier = Modifier.weight(1f))
        if (count != null) Text(count, fontSize = 13.sp, color = HomeMonochromePalette.muted)
    }
}

@Composable
private fun MonochromeRowIcon(icon: ImageVector, iconColor: Color = HomeMonochromePalette.ink) {
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
        .background(HomeMonochromePalette.softGreen), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(21.dp), tint = iconColor)
    }
}

@Composable
internal fun MonochromeRadar(pending: Int?, onClick: () -> Unit) {
    val count = pending ?: 0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MonochromeSectionTitle("On your radar", if (pending == null) "—" else count.toString().padStart(2, '0'))
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MonochromeRowIcon(Icons.Rounded.ReceiptLong)
            Column(Modifier.weight(1f)) {
                Text(if (count > 0) "Review a transaction" else "You're all caught up",
                    color = HomeMonochromePalette.ink, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold)
                Text(if (pending == null) "Review status is unavailable"
                     else if (count > 0) "$count item${if (count == 1) "" else "s"} need your attention"
                     else "Nothing currently needs review",
                    color = HomeMonochromePalette.muted, fontSize = 12.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.ChevronRight, "Open review", Modifier.size(22.dp),
                tint = HomeMonochromePalette.ink)
        }
        HorizontalDivider(color = HomeMonochromePalette.line)
    }
}

@Composable
internal fun MonochromeAccounts(
    groups: List<TrackedAccountGroup>,
    balances: List<PaydayAccountSnapshot>,
    money: (Long) -> String,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MonochromeSectionTitle("Your accounts")
        Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically) {
            if (groups.isEmpty()) {
                MonochromeRowIcon(Icons.Rounded.AccountBalanceWallet)
                Spacer(Modifier.width(12.dp))
                Text("Connect or confirm your accounts", Modifier.weight(1f),
                    fontSize = 13.sp, color = HomeMonochromePalette.muted)
            } else {
                groups.take(2).forEachIndexed { index, group ->
                    val snapshots = balances.filter { it.accountId in group.accountIds }
                    val displayBalance = if (snapshots.size != group.accounts.size ||
                        snapshots.any { it.quality == PaydayBalanceQuality.UNKNOWN || it.balanceMinor == null }) {
                        "Balance unavailable"
                    } else {
                        CheckedMoney.orNull {
                            snapshots.fold(0L) { sum, snapshot ->
                                CheckedMoney.add(sum, requireNotNull(snapshot.balanceMinor))
                            }
                        }?.let(money) ?: "Balance unavailable"
                    }
                    if (index > 0) Box(Modifier.width(1.dp).height(40.dp)
                        .background(HomeMonochromePalette.line))
                    Row(Modifier.weight(1f).padding(end = 3.dp, start = if (index == 0) 0.dp else 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AccountProviderMark(group.displayName, modifier = Modifier.size(40.dp))
                        Column(Modifier.weight(1f)) {
                            val shortName = when {
                                group.displayName.contains("Equity", true) -> "Equity"
                                group.displayName.contains("M-Pesa", true) -> "M-Pesa"
                                else -> group.displayName
                            }
                            Text(shortName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = HomeMonochromePalette.ink, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            Text(displayBalance, fontSize = 11.sp,
                                color = HomeMonochromePalette.muted)
                        }
                    }
                }
            }
            Icon(Icons.Rounded.ChevronRight, "Open accounts", Modifier.size(21.dp),
                tint = HomeMonochromePalette.ink)
        }
        HorizontalDivider(color = HomeMonochromePalette.line)
    }
}

@Composable
internal fun MonochromeUpcoming(
    items: List<ReferenceUpcomingEntry>, onBills: () -> Unit, onSalary: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MonochromeSectionTitle("Coming up")
        if (items.isEmpty()) {
            Text("Nothing scheduled yet", color = HomeMonochromePalette.muted,
                modifier = Modifier.padding(vertical = 12.dp), fontSize = 13.sp)
        }
        items.take(2).forEach { item ->
            MonochromeDetailRow(
                title = if (item.isSalary) "Expected salary" else item.title,
                subtitle = item.whenText, amount = item.amount,
                icon = if (item.isSalary) Icons.Rounded.CalendarMonth else Icons.Rounded.Wifi,
                positive = item.isSalary,
                onClick = if (item.isSalary) onSalary else onBills
            )
        }
        HorizontalDivider(color = HomeMonochromePalette.line)
    }
}

@Composable
private fun MonochromeDetailRow(
    title: String, subtitle: String, amount: String, icon: ImageVector,
    positive: Boolean, onClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        MonochromeRowIcon(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = HomeMonochromePalette.ink, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 12.sp, color = HomeMonochromePalette.muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(amount, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = if (positive) HomeMonochromePalette.green else HomeMonochromePalette.ink,
            maxLines = 2)
    }
}

@Composable
internal fun MonochromeRecent(
    rows: List<StoredTransaction>, money: (Long) -> String,
    onOpen: (Long) -> Unit, onSeeAll: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { SimpleDateFormat("d MMM", locale) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MonochromeSectionTitle("Latest activity")
        if (rows.isEmpty()) {
            Text("No recorded activity in this period", color = HomeMonochromePalette.muted,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSeeAll)
                    .padding(vertical = 14.dp), fontSize = 13.sp)
        }
        rows.take(2).forEach { transaction ->
            val isIncome = transaction.direction == MoneyDirection.INCOME.name
            val isExpense = transaction.direction == MoneyDirection.EXPENSE.name
            val title = transaction.counterparty?.takeIf(String::isNotBlank)
                ?: if (isIncome) "Income" else if (isExpense) "Expense" else "Transfer"
            val icon = when {
                title.contains("shop", true) || title.contains("market", true) ||
                    title.contains("grocery", true) -> Icons.Rounded.ShoppingCart
                title.contains("transport", true) || title.contains("taxi", true) -> Icons.Rounded.DirectionsCar
                isIncome -> Icons.Rounded.ArrowDownward
                else -> Icons.Rounded.ReceiptLong
            }
            MonochromeDetailRow(title, format.format(Date(transaction.occurredAtEpochMs)),
                (if (isIncome) "+" else if (isExpense) "−" else "") + money(transaction.amountMinor),
                icon, isIncome, { onOpen(transaction.id) })
        }
    }
}
