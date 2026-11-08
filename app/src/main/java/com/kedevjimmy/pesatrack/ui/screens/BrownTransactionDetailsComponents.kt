package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import java.text.SimpleDateFormat
import java.util.Date

@Composable
internal fun BrownTransactionHeader(
    item: StoredTransaction, category: String, account: String,
    onBack: () -> Unit, onChooseCategory: () -> Unit, onAddNote: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val p = BrownHomePalette
    Box(Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = p.ink)
        }
        Text("Transaction details", Modifier.align(Alignment.Center), color = p.ink,
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
            fontSize = 24.sp, maxLines = 1)
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
    BrownTransactionPaymentCard(item, category, account)
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onChooseCategory, Modifier.weight(1f).heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = p.coffee)) {
            Icon(Icons.Rounded.Edit, null, Modifier.size(20.dp))
            Spacer(Modifier.width(7.dp))
            Text("Change category", maxLines = 1, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(onClick = onAddNote, Modifier.weight(.84f).heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, p.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = p.coffee)) {
            Icon(Icons.Rounded.NoteAdd, null, Modifier.size(19.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add note", maxLines = 1, fontSize = 13.sp)
        }
    }
}

@Composable
private fun BrownTransactionPaymentCard(item: StoredTransaction, category: String, account: String) {
    val p = BrownHomePalette
    val locale = LocalConfiguration.current.locales[0]
    val signed = when (item.direction) {
        MoneyDirection.EXPENSE.name -> "−"
        MoneyDirection.INCOME.name -> "+"
        else -> ""
    }
    val name = item.counterparty?.takeIf { it.isNotBlank() }
        ?: item.evidenceLabel?.takeIf { it.isNotBlank() } ?: "Transaction"
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, p.outline), color = p.cream) {
        Box {
            Image(painter = painterResource(R.drawable.transaction_coin_receipt_brown),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(144.dp).align(Alignment.TopCenter),
                contentScale = ContentScale.FillBounds, alpha = .54f)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(Modifier.size(48.dp), shape = RoundedCornerShape(14.dp),
                    color = p.outline.copy(alpha = .25f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.PersonOutline, null, Modifier.size(28.dp), tint = p.ink)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(name, color = p.ink, fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold, fontSize = 21.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("$category  ·  $account", color = p.muted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(signed + formatTransactionDetailMoney(item.amountMinor),
                    Modifier.weight(1f), color = p.ink, fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold, fontSize = 32.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Surface(shape = RoundedCornerShape(8.dp), color = p.canvas,
                    border = BorderStroke(1.dp, p.coffee)) {
                    Text(when (item.reviewState) {
                        ReviewState.EXCLUDED.name -> "Excluded"
                        ReviewState.NEEDS_REVIEW.name -> "Needs review"
                        else -> "Recorded"
                    }, Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = p.coffee, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
            Text(SimpleDateFormat("d MMM yyyy  ·  h:mm a", locale)
                .format(Date(item.occurredAtEpochMs)), color = p.muted, fontSize = 12.sp)
            BrownTransactionFunding(item, account)
            }
        }
    }
}

@Composable
private fun BrownTransactionFunding(item: StoredTransaction, account: String) {
    val p = BrownHomePalette
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
    val p = BrownHomePalette
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = p.ink, fontFamily = FontFamily.Serif, fontSize = 13.sp)
        Text(formatTransactionDetailMoney(minor), color = p.ink,
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
internal fun BrownAddReceipt(onClick: () -> Unit, hasReceipt: Boolean) {
    val p = BrownHomePalette
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
    val p = BrownHomePalette
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
