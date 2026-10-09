package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.ingestion.TransactionMessageEvidenceResult
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun detailDate(epochMs: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(epochMs))

@Composable
internal fun TransactionDetailSummary(item: StoredTransaction, icon: ImageVector) {
    PesaCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(icon, null, Modifier.padding(14.dp).size(28.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.counterparty?.takeIf { it.isNotBlank() } ?: "Transaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(item.type.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatTransactionDetailMoney(item.amountMinor), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(detailDate(item.occurredAtEpochMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val state = when (item.reviewState) {
                    ReviewState.NEEDS_REVIEW.name -> "Needs review"
                    ReviewState.EXCLUDED.name -> "Excluded from budget"
                    else -> "Recorded"
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(if (item.reviewState == ReviewState.NEEDS_REVIEW.name) Icons.Rounded.Info else Icons.Rounded.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(state, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
internal fun TransactionDetailFacts(
    item: StoredTransaction, accountLabel: String, categoryLabel: String,
    onAccount: (() -> Unit)?, onCategory: (() -> Unit)?
) {
    val direction = runCatching { MoneyDirection.valueOf(item.direction) }.getOrDefault(MoneyDirection.NEUTRAL)
    val incomingAdjustment = transactionDetailIsIncoming(item.type, item.relationType)
    val amounts = transactionDetailAmounts(item.amountMinor, item.feeMinor, item.fulizaAccessFeeMinor, direction, incomingAdjustment)
    PesaCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            DetailConceptRow("Account", accountLabel, onAccount)
            HorizontalDivider()
            DetailConceptRow("Category", categoryLabel, onCategory, "Change")
            HorizontalDivider()
            DetailConceptRow(if (direction == MoneyDirection.EXPENSE && !incomingAdjustment) "Payment" else "Amount", formatTransactionDetailMoney(item.amountMinor))
            DetailConceptRow("Transaction fee", amounts.feesMinor?.let(::formatTransactionDetailMoney) ?: "Amount unavailable")
            if (amounts.feesMinor == null || (direction in listOf(MoneyDirection.EXPENSE, MoneyDirection.TRANSFER) && !incomingAdjustment)) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                DetailConceptRow("Total outflow", amounts.outflowMinor?.let(::formatTransactionDetailMoney) ?: "Amount unavailable", emphasized = true)
            }
            DetailConceptRow("Balance after", formatTransactionDetailBalance(item.balanceMinor))
            Text("Balance from the recorded message; may not be current.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}

@Composable
internal fun TransactionDetailMessage(
    item: StoredTransaction, evidence: TransactionMessageEvidenceResult?,
    expanded: Boolean, onExpanded: () -> Unit, hasAnnotation: Boolean, onAnnotation: () -> Unit,
    categoryMemory: @Composable () -> Unit = {}
) {
    val clipboard = LocalClipboardManager.current
    PesaCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClickLabel = if (expanded) "Hide message evidence" else "Show message evidence", onClick = onExpanded).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Message evidence", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
            }
            if (expanded) {
                val message = evidence?.message
                DetailConceptRow("Source", message?.let { "SMS (${it.sender})" } ?: item.sourceKind.replace('_', ' ').replaceFirstChar { it.titlecase() })
                DetailConceptRow("Received", detailDate(message?.receivedAtEpochMs ?: item.receivedAtEpochMs))
                Text("Message", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                    SelectionContainer {
                        Text(message?.body ?: evidence?.unavailableReason ?: "Loading original message…", Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Reference", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.transactionCode ?: "Not provided", style = MaterialTheme.typography.bodyMedium)
                    item.transactionCode?.takeIf { it.isNotBlank() }?.let { code ->
                        IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }) { Icon(Icons.Rounded.ContentCopy, "Copy reference", Modifier.size(18.dp)) }
                    }
                }
            }
            HorizontalDivider()
            categoryMemory()
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().clickable(onClick = onAnnotation).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null)
                Text(if (hasAnnotation) "View or edit note and receipt" else "Add note or receipt", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Icon(Icons.Rounded.ChevronRight, null)
            }
        }
    }
}

@Composable
private fun DetailConceptRow(label: String, value: String, onClick: (() -> Unit)? = null, action: String? = null, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(0.9f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1.1f)) {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal, color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            if (onClick != null && action != null) Text(action, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        if (onClick != null) Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp))
    }
}
