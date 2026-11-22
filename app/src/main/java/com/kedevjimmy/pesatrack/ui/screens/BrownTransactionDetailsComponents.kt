package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import java.text.SimpleDateFormat
import java.util.Date

@Composable
internal fun BrownTransactionHeader(
    item: StoredTransaction, category: String, account: String,
    onBack: () -> Unit, onChooseCategory: () -> Unit, onAddNote: () -> Unit,
    onReceipt: () -> Unit, hasReceipt: Boolean
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val p = homeColors()
    Box(Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = p.ink)
        }
        Text("Transaction", Modifier.align(Alignment.Center), color = p.ink,
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
            fontSize = 21.sp, maxLines = 1)
        Box(Modifier.align(Alignment.CenterEnd)) {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Rounded.MoreHoriz, "More transaction actions", tint = p.ink)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false },
                containerColor = p.canvas) {
                DropdownMenuItem(text = { Text("Change category") },
                    colors = MenuDefaults.itemColors(textColor = p.ink),
                    onClick = { menuExpanded = false; onChooseCategory() })
                DropdownMenuItem(text = { Text("Add note or receipt") },
                    colors = MenuDefaults.itemColors(textColor = p.ink),
                    onClick = { menuExpanded = false; onAddNote() })
            }
        }
    }
    BrownTransactionLedgerSummary(item, account, category)
    Spacer(Modifier.height(16.dp))
    Text("Manage", color = p.ink, fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TransactionManageAction("Category", Icons.Rounded.ShoppingBasket,
            Modifier.weight(1f), onChooseCategory)
        TransactionManageAction("Add note", Icons.Rounded.ChatBubbleOutline,
            Modifier.weight(1f), onAddNote)
        TransactionManageAction(if (hasReceipt) "Receipt" else "Add receipt",
            Icons.AutoMirrored.Rounded.ReceiptLong, Modifier.weight(1f), onReceipt)
    }
    Spacer(Modifier.height(16.dp))
    Text("At a glance", color = p.ink, fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    TransactionGlanceRow("Account", account)
    TransactionGlanceRow("Transaction type", item.type.replace('_', ' ').lowercase()
        .replaceFirstChar { it.titlecase() })
    TransactionGlanceRow("Reference", item.transactionCode?.takeIf { it.isNotBlank() } ?: "Not recorded")
    if (item.fulizaFinancedMinor > 0L || item.fulizaAccessFeeMinor > 0L) {
        Spacer(Modifier.height(8.dp))
        BrownTransactionFunding(item, account)
    }
}

@Composable
private fun BrownTransactionLedgerSummary(item: StoredTransaction, account: String, category: String) {
    val p = homeColors()
    val locale = LocalConfiguration.current.locales[0]
    val movement = when (item.direction) {
        MoneyDirection.EXPENSE.name -> "MONEY OUT"
        MoneyDirection.INCOME.name -> "MONEY IN"
        MoneyDirection.TRANSFER.name -> "TRANSFER"
        else -> "TRANSACTION"
    }
    val status = when (item.reviewState) {
        ReviewState.EXCLUDED.name -> "Excluded"
        ReviewState.NEEDS_REVIEW.name -> "Needs review"
        else -> "Recorded"
    }
    val signed = when (item.direction) {
        MoneyDirection.EXPENSE.name -> "−"
        MoneyDirection.INCOME.name -> "+"
        else -> ""
    }
    val counterparty = item.counterparty?.takeIf { it.isNotBlank() }
        ?: item.evidenceLabel?.takeIf { it.isNotBlank() }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$movement  ·  ${status.uppercase()}", color = p.coffee,
            fontWeight = FontWeight.SemiBold, letterSpacing = .7.sp, fontSize = 11.sp)
        Text(signed + formatTransactionDetailMoney(item.amountMinor), color = p.ink,
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
            fontSize = 29.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(transactionCounterpartySummary(item.direction, counterparty),
            color = p.ink, fontFamily = FontFamily.Serif, fontSize = 16.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(SimpleDateFormat("d MMM yyyy  ·  h:mm a", locale)
            .format(Date(item.occurredAtEpochMs)), color = p.muted, fontSize = 12.sp)
        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = p.outline.copy(alpha = .6f))
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = p.white) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.AccountBalanceWallet, null, Modifier.size(20.dp), tint = p.coffee)
                Text(account, Modifier.weight(1f), color = p.ink, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Rounded.ArrowForward, null, Modifier.size(17.dp), tint = p.muted)
                Text(category, Modifier.weight(1f), color = p.ink, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(status, color = if (status == "Recorded") MaterialTheme.colorScheme.tertiary else p.coffee,
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1)
            }
        }
    }
}

/** A transfer counterparty is not necessarily its destination (for example, incoming M-Pesa). */
internal fun transactionCounterpartySummary(direction: String, counterparty: String?): String {
    val name = counterparty?.takeIf { it.isNotBlank() } ?: return "Transaction"
    return when (direction) {
        MoneyDirection.EXPENSE.name -> "Paid to $name"
        MoneyDirection.INCOME.name -> "Received from $name"
        MoneyDirection.TRANSFER.name -> "Transfer involving $name"
        else -> "With $name"
    }
}

@Composable
private fun TransactionManageAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier, onClick: () -> Unit) {
    val p = homeColors()
    Surface(modifier.heightIn(min = 72.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp), color = p.white) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = p.coffee, modifier = Modifier.size(22.dp))
            Text(label, color = p.ink, fontSize = 12.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TransactionGlanceRow(label: String, value: String) {
    val p = homeColors()
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(.45f), color = p.muted, fontSize = 12.sp)
        Text(value, Modifier.weight(.55f), color = p.ink, fontSize = 12.sp,
            maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.End,
            overflow = TextOverflow.Ellipsis)
    }
    HorizontalDivider(color = p.outline.copy(alpha = .55f))
}

@Composable
private fun BrownTransactionFunding(item: StoredTransaction, account: String) {
    val p = homeColors()
    val portions = transactionDetailFulizaPortions(item.amountMinor, item.fulizaFinancedMinor)
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(if (item.direction == MoneyDirection.EXPENSE.name) "Paid via $account" else account,
            color = p.ink, fontFamily = FontFamily.Serif, fontSize = 14.sp)
        if (portions != null) {
            Text("│", color = p.outline)
            Surface(shape = RoundedCornerShape(50), color = p.outline.copy(alpha = .27f)) {
                Text("Partly with Fuliza", Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    color = p.coffee, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
    }
    if (portions != null) {
        HorizontalDivider(color = p.outline)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BrownFundingPortion("M-Pesa portion", portions.first, Modifier.weight(1f))
            VerticalDivider(Modifier.height(36.dp), color = p.outline)
            BrownFundingPortion("Fuliza portion", portions.second,
                Modifier.weight(1f).padding(start = 12.dp))
        }
        if (item.fulizaAccessFeeMinor > 0L) {
            Text("Fuliza access fee: ${formatTransactionDetailMoney(item.fulizaAccessFeeMinor)}",
                color = p.muted, fontSize = 12.sp)
        }
    } else if (item.fulizaFinancedMinor > 0L) {
        Text("Fuliza funding needs review: recorded portions do not reconcile.",
            color = p.coffee, fontSize = 12.sp)
    }
}

@Composable
private fun BrownFundingPortion(title: String, minor: Long, modifier: Modifier) {
    val p = homeColors()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = p.ink, fontFamily = FontFamily.Serif, fontSize = 13.sp)
        Text(formatTransactionDetailMoney(minor), color = p.ink,
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
internal fun BrownAddReceipt(onClick: () -> Unit, hasReceipt: Boolean) {
    val p = homeColors()
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
        color = p.cream, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(Modifier.size(35.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.AttachFile, null, tint = p.coffee)
            }
            VerticalDivider(Modifier.height(32.dp), color = p.outline.copy(alpha = .7f))
            Text(if (hasReceipt) "View receipt" else "Add receipt", Modifier.weight(1f),
                color = p.coffee, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Icon(Icons.Rounded.ChevronRight, null, tint = p.coffee)
        }
    }
}

@Composable
internal fun BrownMerchantHistory(
    merchant: String,
    transactions: List<StoredTransaction>,
    categoryStore: CategoryStore,
    onTransactionClick: (Long) -> Unit
) {
    val p = homeColors()
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(merchant, color = p.coffee, fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium)
        if (transactions.isEmpty()) {
            Text("No other recent transactions with this merchant.",
                color = p.muted, style = MaterialTheme.typography.bodySmall)
        } else {
            transactions.take(8).forEach { transaction ->
                Surface(Modifier.fillMaxWidth().clickable {
                    onTransactionClick(transaction.id)
                }, shape = RoundedCornerShape(12.dp), color = p.cream) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(formatTransactionDetailMoney(transaction.amountMinor),
                                color = p.ink, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium)
                            Text(SimpleDateFormat("d MMM yyyy · h:mm a", locale)
                                .format(Date(transaction.occurredAtEpochMs)),
                                color = p.muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(categoryStore.labelFor(
                            categoryStore.canonicalId(transaction.category)),
                            color = p.coffee, style = MaterialTheme.typography.bodySmall,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Rounded.ChevronRight, null, tint = p.coffee,
                            modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
