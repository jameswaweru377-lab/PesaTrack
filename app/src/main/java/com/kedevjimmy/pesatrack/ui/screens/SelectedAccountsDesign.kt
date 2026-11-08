package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.DebtAccountSnapshot
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney
import com.kedevjimmy.pesatrack.domain.model.checkedMoneySumOf
import kotlinx.coroutines.launch

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
    val cards = listOf<AccountDisplayLine?>(null) + accounts
    val pager = rememberPagerState(pageCount = { cards.size })
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text(if (complete) "Total across accounts" else "Known recorded balances",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (hidden) "••••••" else total?.let(::formatKesAmount) ?: "Unavailable",
                style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                color = Color(0xFF431A10))
        }
        HorizontalPager(state = pager, contentPadding = PaddingValues(end = 44.dp),
            pageSpacing = 12.dp) { page ->
            val line = cards[page]
            HeritageAccountCard(
                title = line?.name ?: "All Accounts",
                amount = if (hidden) "••••••" else (line?.balanceMinor ?: if (page == 0) total else null)
                    ?.let(::formatKesAmount) ?: "Balance unavailable",
                subtitle = if (page == 0) "YOUR MONEY AT A GLANCE" else
                    when (line?.quality) {
                        com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality.OBSERVED -> "OBSERVED BALANCE"
                        com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality.USER_ENTERED -> "ENTERED BY YOU"
                        com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality.RECONSTRUCTED -> "RECONSTRUCTED BALANCE"
                        else -> "BALANCE NOT CONFIRMED"
                    },
                names = if (page == 0) accounts.take(3).map { it.name } else listOfNotNull(line?.name),
                index = page
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) } },
                enabled = pager.currentPage > 0) { Text("‹", style = MaterialTheme.typography.headlineSmall) }
            cards.indices.forEach { index ->
                Box(Modifier.padding(horizontal = 4.dp).size(if (index == pager.currentPage) 9.dp else 7.dp)
                    .background(if (index == pager.currentPage) Color(0xFF7B3216) else Color(0xFFD2B6A4),
                        RoundedCornerShape(50)))
            }
            TextButton(onClick = { scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(cards.lastIndex)) } },
                enabled = pager.currentPage < cards.lastIndex) { Text("›", style = MaterialTheme.typography.headlineSmall) }
        }
        Text("Swipe to explore · Recorded balances, not live bank balances",
            modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (!complete) Text("${accounts.size - available.size} balance(s) unavailable; missing values are excluded.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Illustrated, layered cards use no downloaded bank artwork or fictitious financial data. */
@Composable
private fun HeritageAccountCard(title: String, amount: String, subtitle: String,
    names: List<String>, index: Int) {
    val dark = index % 3 == 1
    val ink = if (dark) Color.White else Color(0xFF54240F)
    val top = if (dark) Color(0xFF9B4B25) else Color(0xFFFFF1DC)
    val bottom = if (dark) Color(0xFF54220F) else Color(0xFFDFA66E)
    Surface(modifier = Modifier.fillMaxWidth().height(240.dp),
        shape = RoundedCornerShape(28.dp), shadowElevation = 9.dp,
        color = top) {
        Box(Modifier.background(Brush.linearGradient(listOf(top, bottom)))) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawCircle(color = if (dark) Color(0x22FFFFFF) else Color(0x55FFFFFF),
                    radius = w * .35f, center = Offset(w * .84f, h * .35f))
                val hill = Path().apply {
                    moveTo(0f, h * .83f)
                    cubicTo(w * .26f, h * .61f, w * .52f, h * .94f, w, h * .65f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(hill, Color(0x663F210F), style = Fill)
                val foreground = Path().apply {
                    moveTo(0f, h * .91f)
                    cubicTo(w * .27f, h * .77f, w * .58f, h * 1.03f, w, h * .84f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(foreground, Color(0xAA4C220E))
            }
            Column(Modifier.fillMaxSize().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(subtitle, color = ink.copy(alpha = .78f),
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Text(title, color = ink, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(amount, color = ink, style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    names.forEach { name ->
                        Box(Modifier.size(36.dp).background(Color(0xDFFFFFF2), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center) {
                            AccountProviderMark(name, Modifier.size(30.dp))
                        }
                    }
                }
            }
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
