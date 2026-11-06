package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.DebtAccountSnapshot
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney
import com.kedevjimmy.pesatrack.domain.model.checkedMoneySumOf

/** The selected Accounts concept uses black controls, with mint only as a quiet background. */
internal val AccountBlack = Color(0xFF151716)
internal val AccountMint = Color(0xFFE7F5E9)
internal val AccountCream = Color(0xFFFAF7F0)
private val AccountLine = Color(0xFFE4E6E2)

@Composable
internal fun SelectedAccountsCompass(
    accounts: List<AccountDisplayLine>, hidden: Boolean
) {
    val available = accounts.filter { it.balanceMinor != null }
    val total = CheckedMoney.orNull { available.checkedMoneySumOf { requireNotNull(it.balanceMinor) } }
    val complete = available.size == accounts.size && total != null
    val positive = available.filter { (it.balanceMinor ?: 0) > 0 }
    val positiveTotal = CheckedMoney.orNull { positive.checkedMoneySumOf { requireNotNull(it.balanceMinor) } }
    Surface(shape = RoundedCornerShape(26.dp), color = AccountCream) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Balance Compass", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, color = AccountBlack)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(70.dp).background(AccountMint, RoundedCornerShape(19.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AccountBalanceWallet, null, tint = AccountBlack, modifier = Modifier.size(35.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(if (complete) "Money across accounts" else "Known recorded balances",
                        style = MaterialTheme.typography.bodyMedium)
                    Text(if (hidden) "••••••" else total?.let(::formatKesAmount) ?: "Unavailable",
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        color = AccountBlack)
                }
            }
            if (positiveTotal != null && positiveTotal > 0L) {
                Row(Modifier.fillMaxWidth().height(17.dp).background(AccountMint, RoundedCornerShape(50)),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val colors = listOf(AccountBlack, Color(0xFF7D8980), Color(0xFFC0D8C2))
                    positive.forEachIndexed { index, account ->
                        val amount = (account.balanceMinor ?: 0L).coerceAtLeast(0L)
                        // Weight is visual-only. Financial calculations remain in checked integer minor units.
                        if (amount > 0) Box(Modifier.weight(amount.toFloat() / positiveTotal.toFloat())
                            .fillMaxHeight().background(colors[index % colors.size]))
                    }
                }
            }
            accounts.take(5).forEachIndexed { index, line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(
                        listOf(AccountBlack, Color.Gray, Color(0xFFB4D7B9))[index % 3],
                        RoundedCornerShape(3.dp)))
                    Text(line.name, Modifier.weight(1f).padding(start = 9.dp),
                        style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text(if (hidden) "••••" else line.balanceMinor?.let(::formatKesAmount) ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
            if (!complete) Text("${accounts.size - available.size} balance(s) unavailable; no estimates added to the total.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall)
            Text("Balances are saved or reconstructed on this phone, not live bank balances.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun SelectedAccountTiles(
    lines: List<AccountDisplayLine>, hidden: Boolean, onAccountClick: ((String) -> Unit)?
) {
    lines.chunked(2).forEach { pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            pair.forEach { line ->
                Surface(modifier = Modifier.weight(1f).heightIn(min = 140.dp),
                    onClick = { line.accountId?.let { onAccountClick?.invoke(it) } },
                    enabled = line.accountId != null && onAccountClick != null,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, AccountLine), color = Color.White) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccountProviderMark(line.name)
                        Text(line.name, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (hidden) "••••••" else line.balanceMinor?.let(::formatKesAmount)
                            ?: "Balance unavailable", fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Text(when (line.quality) {
                            com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality.OBSERVED -> "Observed"
                            com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality.RECONSTRUCTED -> "Estimated"
                            com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality.USER_ENTERED -> "Entered by you"
                            else -> "Needs balance evidence"
                        }, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
internal fun SelectedDebtsHero(
    debts: List<DebtAccountSnapshot>, known: Long?, unknownCount: Int, isCurrent: Boolean
) {
    Surface(shape = RoundedCornerShape(24.dp), color = AccountBlack) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("R E P A Y M E N T   H O R I Z O N", style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFBBD9C2))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).background(Color(0xFF343C38), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Layers, null, tint = Color.White)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(if (isCurrent) "TOTAL RECORDED DEBT" else "LAST VERIFIED DEBT",
                        style = MaterialTheme.typography.labelMedium, color = Color(0xFFD7E4D9))
                    Text(known?.let(::formatKesAmount) ?: "Unavailable",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Text("${debts.size} borrowing source(s) · $unknownCount unconfirmed balance(s)",
                color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (unknownCount > 0) Surface(shape = RoundedCornerShape(14.dp), color = AccountMint) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Info, null, tint = AccountBlack)
            Text("Unknown balances excluded from total", Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun SelectedDebtLenders(
    debts: List<DebtAccountSnapshot>, onOpen: (DebtAccountSnapshot) -> Unit
) {
    Text("Your lenders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    debts.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            pair.forEach { debt ->
                Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), color = AccountMint) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccountProviderMark(debt.name)
                        Text(debt.name, style = MaterialTheme.typography.titleMedium,
                            maxLines = 1, fontWeight = FontWeight.Bold)
                        Text(if (hasUnknownDebtBalance(debt)) "Confirm balance"
                            else formatKesAmount(debt.outstandingMinor), fontWeight = FontWeight.SemiBold)
                        OutlinedButton(onClick = { onOpen(debt) }, modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, AccountBlack)) {
                            Text("View", color = AccountBlack)
                        }
                    }
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
internal fun SelectedAccountDebtsLink(onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(18.dp), color = Color.White) {
        Row(Modifier.fillMaxWidth().padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(AccountMint, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Layers, null, tint = AccountBlack)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Borrowing & debts", fontWeight = FontWeight.Bold)
                Text("View balances and repayments", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = AccountBlack)
        }
    }
}
