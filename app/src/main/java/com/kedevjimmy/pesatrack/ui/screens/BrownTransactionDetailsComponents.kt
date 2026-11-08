package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = p.ink)
        }
        Text("Transaction details", Modifier.weight(1f), color = p.ink,
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
            fontSize = 24.sp, maxLines = 1)
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Rounded.MoreHoriz, "More transaction actions", tint = p.ink)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text("Change category") },
                    onClick = { menuExpanded = false; onChooseCategory() })
                DropdownMenuItem(text = { Text("Add note or receipt") },
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
        Column(Modifier.background(Brush.linearGradient(listOf(p.cream, p.canvas, p.cream)))
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        fontWeight = FontWeight.Bold, fontSize = 20.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("$category  ·  $account", color = p.muted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(signed + formatTransactionDetailMoney(item.amountMinor),
                    Modifier.weight(1f), color = p.ink, fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold, fontSize = 29.sp, maxLines = 1,
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
        Row(Modifier.fillMaxWidth()) {
            BrownFundingPortion("M-Pesa portion", portions.first, Modifier.weight(1f))
            BrownFundingPortion("Fuliza portion", portions.second, Modifier.weight(1f))
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
internal fun BrownTransactionInformation(item: StoredTransaction, category: String,
    account: String, onCategory: () -> Unit, onAccount: (() -> Unit)?) {
    val p = BrownHomePalette
    val source = when {
        item.sourceKind.contains("SMS", ignoreCase = true) -> item.sourceKind.replace('_', ' ')
        item.sourceKind.contains("MPESA", ignoreCase = true) -> "M-Pesa SMS"
        else -> item.sourceKind.replace('_', ' ')
    }
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = p.white, border = BorderStroke(1.dp, p.outline.copy(alpha = .55f))) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 10.dp)) {
            Text("Transaction information", color = p.ink,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 21.sp)
            BrownInfoLine("Account", account, onAccount)
            BrownInfoLine("Category", category, onCategory)
            BrownInfoLine("Source", source)
            BrownInfoLine("Reference", item.transactionCode?.let {
                if (it.length > 4) it.take(4) + "••••" else it
            } ?: "Not provided", showLine = false)
        }
    }
}

@Composable
private fun BrownInfoLine(label: String, value: String, onClick: (() -> Unit)? = null,
    showLine: Boolean = true) {
    val p = BrownHomePalette
    if (showLine) Spacer(Modifier.height(3.dp))
    Surface(onClick = onClick ?: {}, enabled = onClick != null,
        color = p.white, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 45.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), color = p.ink, fontSize = 14.sp)
            Text(value, color = p.ink, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    if (showLine) HorizontalDivider(color = p.outline.copy(alpha = .5f))
}

@Composable
internal fun BrownAddReceipt(onClick: () -> Unit, hasReceipt: Boolean) {
    val p = BrownHomePalette
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
        color = p.cream, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Icon(Icons.Rounded.AttachFile, null, tint = p.coffee)
            Text(if (hasReceipt) "View receipt" else "Add receipt", Modifier.weight(1f),
                color = p.coffee, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Icon(Icons.Rounded.ChevronRight, null, tint = p.coffee)
        }
    }
}
