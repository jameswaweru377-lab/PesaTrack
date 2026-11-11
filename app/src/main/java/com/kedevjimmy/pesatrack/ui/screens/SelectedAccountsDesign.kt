package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.ArrowDownward
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.data.local.ObservedInvestmentPosition
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
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
internal fun HeritageAccountList(lines: List<AccountDisplayLine>, savings: List<AccountDisplayLine>,
    hidden: Boolean, onAccountClick: ((String) -> Unit)?, onAdd: (() -> Unit)?) {
    var expanded by remember { mutableStateOf(true) }
    val hasMobile = lines.any { it.name.contains("M-Pesa", true) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Your accounts", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = Color(0xFF4C200F))
            if (onAdd != null) OutlinedButton(onClick = onAdd,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                border = BorderStroke(1.dp, Color(0xFF8D4B2B))) {
                Text("Add account", color = Color(0xFF713316))
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.Add, null, Modifier.size(17.dp), tint = Color(0xFF713316))
            }
        }
        Surface(shape = RoundedCornerShape(23.dp), color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFEDD9C7))) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                lines.forEachIndexed { index, line ->
                    if (index > 0) HorizontalDivider(color = Color(0xFFF0DED3))
                    Row(Modifier.fillMaxWidth().heightIn(min = 76.dp)
                        .clickable {
                            if (line.name.contains("M-Pesa", true) && savings.isNotEmpty()) expanded = !expanded
                            else line.accountId?.let { onAccountClick?.invoke(it) }
                        }, verticalAlignment = Alignment.CenterVertically) {
                        AccountProviderMark(line.name, Modifier.size(49.dp))
                        Text(line.name, Modifier.weight(1f).padding(start = 12.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text(if (hidden) "••••" else line.balanceMinor?.let(::formatKesAmount) ?: "Unknown",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Icon(if (line.name.contains("M-Pesa", true) && savings.isNotEmpty())
                                Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                            null, Modifier.padding(start = 5.dp).size(22.dp),
                            tint = Color(0xFF713316))
                    }
                    if (line.name.contains("M-Pesa", true) && savings.isNotEmpty() && expanded) {
                        savings.forEach { child ->
                            Row(Modifier.fillMaxWidth().padding(start = 38.dp, bottom = 3.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF6EE))
                                .clickable { child.accountId?.let { onAccountClick?.invoke(it) } }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Savings, null, Modifier.size(18.dp),
                                    tint = Color(0xFFA35A2E))
                                Text(child.name, Modifier.weight(1f).padding(start = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (hidden) "••••" else child.balanceMinor?.let(::formatKesAmount) ?: "Unknown",
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                if (!hasMobile) savings.forEach { child ->
                    HorizontalDivider(color = Color(0xFFF0DED3))
                    Row(Modifier.fillMaxWidth().clickable { child.accountId?.let { onAccountClick?.invoke(it) } }
                        .padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        AccountProviderMark(child.name)
                        Text(child.name, Modifier.weight(1f).padding(start = 12.dp))
                        Text(if (hidden) "••••" else child.balanceMinor?.let(::formatKesAmount) ?: "Unknown")
                    }
                }
            }
        }
    }
}

@Composable
internal fun HeritageInvestments(positions: List<ObservedInvestmentPosition>, onOpen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("Investments", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = Color(0xFF4C200F))
        Surface(onClick = onOpen, shape = RoundedCornerShape(19.dp), color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFEDD9C7))) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.TrendingUp, null, Modifier.size(31.dp), tint = Color(0xFF9D4A22))
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(if (positions.isEmpty()) "Shares & investments" else
                        "${positions.size} observed investment position(s)",
                        fontWeight = FontWeight.SemiBold)
                    Text("Recorded units · No assumed market value",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("View", color = Color(0xFF9D4A22), style = MaterialTheme.typography.labelMedium)
                Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFF9D4A22))
            }
        }
    }
}

@Composable
internal fun HeritageRecentAccountActivity(rows: List<StoredTransaction>, hidden: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("Recent account activity", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = Color(0xFF4C200F))
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFEDD9C7))) {
            Column(Modifier.padding(horizontal = 15.dp, vertical = 9.dp)) {
                if (rows.isEmpty()) Text("No confirmed own-account transfers or salary receipts yet.",
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
                rows.take(3).forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = Color(0xFFF0DED3))
                    val salary = item.direction == "INCOME"
                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (salary) Icons.Rounded.ArrowDownward else Icons.Rounded.SwapHoriz,
                            contentDescription = null, modifier = Modifier.size(31.dp),
                            tint = if (salary) Color(0xFF395B20) else Color(0xFF9D4A22))
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(if (salary) "Salary received" else "Own-account transfer",
                                fontWeight = FontWeight.SemiBold)
                            Text((item.counterparty?.takeIf { it.isNotBlank() } ?: item.category) +
                                " · " + java.text.SimpleDateFormat("d MMM yyyy", locale)
                                    .format(java.util.Date(item.occurredAtEpochMs)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(if (hidden) "••••" else formatKesAmount(item.amountMinor),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold)
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
