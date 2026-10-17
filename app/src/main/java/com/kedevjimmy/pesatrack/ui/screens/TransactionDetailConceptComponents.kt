package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
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

/**
 * Approved Transaction Facts card. Category lives exclusively in the
 * categorization flow above; the facts are read-only source-backed details.
 */
@Composable
internal fun TransactionDetailFacts(
    item: StoredTransaction,
    accountLabel: String,
    note: String,
    onAccount: (() -> Unit)?,
    onNote: () -> Unit
) {
    val direction = runCatching { MoneyDirection.valueOf(item.direction) }
        .getOrDefault(MoneyDirection.NEUTRAL)
    val incomingAdjustment = transactionDetailIsIncoming(item.type, item.relationType)
    val amounts = transactionDetailAmounts(
        item.amountMinor, item.feeMinor, item.fulizaAccessFeeMinor,
        direction, incomingAdjustment
    )
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val clipboard = LocalClipboardManager.current
    val payee = transactionFactsPayee(item.counterparty)
    val reference = item.transactionCode?.trim()?.takeIf { it.isNotBlank() }
    val recordedNote = note.trim()
    PesaCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                .testTag("transaction-facts-approved"),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                "Here are the key details about this transaction.",
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TransactionFactVisualRow(
                icon = Icons.Rounded.AccountBalance,
                title = "Account",
                help = "The account this transaction came from.",
                value = accountLabel,
                onClick = onAccount,
                showChevron = onAccount != null
            )
            TransactionFactSeparator()
            TransactionFactVisualRow(
                icon = Icons.Rounded.Storefront,
                title = "Merchant / Payee",
                help = "Name recorded for this payment.",
                value = payee
            )
            TransactionFactSeparator()
            TransactionFactVisualRow(
                icon = Icons.Rounded.CalendarMonth,
                title = "Date & time",
                help = "When this transaction happened.",
                value = transactionFactsDate(item.occurredAtEpochMs, locale)
            )
            TransactionFactSeparator()
            TransactionFactVisualRow(
                icon = Icons.Rounded.SwapHoriz,
                title = "Transaction type",
                help = "Nature of this transaction.",
                value = transactionFactsKind(item.type, item.relationType),
                valueTone = TransactionFactValueTone.ACCENT
            )
            TransactionFactSeparator()
            TransactionFactVisualRow(
                icon = Icons.Rounded.CreditCard,
                title = transactionFactsAmountTitle(item.direction, incomingAdjustment),
                help = "Recorded amount before transaction fees.",
                value = formatTransactionDetailMoney(item.amountMinor)
            )
            TransactionFactSeparator()
            TransactionFactVisualRow(
                icon = Icons.Rounded.Payments,
                title = "Transaction fee",
                help = "Recorded fee including eligible access fees.",
                value = amounts.feesMinor?.let(::formatTransactionDetailMoney)
                    ?: "Amount unavailable"
            )
            val isOutgoing = !incomingAdjustment &&
                direction in listOf(MoneyDirection.EXPENSE, MoneyDirection.TRANSFER)
            if (isOutgoing || amounts.feesMinor == null) {
                TransactionFactSeparator()
                TransactionFactVisualRow(
                    icon = Icons.Rounded.NorthEast,
                    title = "Total outflow",
                    help = "Payment plus recorded fees.",
                    value = amounts.outflowMinor?.let(::formatTransactionDetailMoney)
                        ?: "Amount unavailable",
                    valueTone = TransactionFactValueTone.EMPHASIS
                )
            }
            TransactionFactSeparator()
            TransactionFactVisualRow(
                icon = Icons.Rounded.AccountBalanceWallet,
                title = "Balance after",
                help = transactionFactsBalanceHelp(item.balanceMinor),
                value = formatTransactionDetailBalance(item.balanceMinor)
            )
            TransactionFactSeparator()
            TransactionFactVisualRow(
                icon = Icons.Rounded.Tag,
                title = "Reference",
                help = "Transaction receipt or ID.",
                value = reference ?: "Not provided",
                onClick = reference?.let { receipt ->
                    { clipboard.setText(AnnotatedString(receipt)) }
                },
                showCopy = reference != null
            )
            TransactionFactSeparator()
            TransactionFactVisualRow(
                icon = Icons.Rounded.Smartphone,
                title = "Channel",
                help = "Recorded account or import source.",
                value = transactionFactsChannel(accountLabel, item.sourceKind)
            )
            TransactionFactSeparator()
            TransactionFactVisualRow(
                icon = Icons.Rounded.StickyNote2,
                title = "Note",
                help = if (recordedNote.isNotEmpty()) "Edit your saved note."
                    else "Add your own note (optional).",
                value = if (recordedNote.isEmpty()) "Add note" else "View note",
                onClick = onNote,
                valueTone = TransactionFactValueTone.ACCENT
            )
        }
    }
}

private enum class TransactionFactValueTone { NORMAL, ACCENT, EMPHASIS }

@Composable
private fun TransactionFactSeparator() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 47.dp, end = 2.dp),
        thickness = .5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .46f)
    )
}

@Composable
private fun TransactionFactVisualRow(
    icon: ImageVector,
    title: String,
    help: String,
    value: String,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = false,
    showCopy: Boolean = false,
    valueTone: TransactionFactValueTone = TransactionFactValueTone.NORMAL
) {
    val tint = when (valueTone) {
        TransactionFactValueTone.NORMAL -> MaterialTheme.colorScheme.primary
        TransactionFactValueTone.ACCENT -> MaterialTheme.colorScheme.tertiary
        TransactionFactValueTone.EMPHASIS -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier = Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 58.dp)
            .padding(horizontal = 4.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(39.dp),
            color = tint.copy(alpha = .13f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, modifier = Modifier.size(21.dp), tint = tint)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title, fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                help, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp, lineHeight = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            value, modifier = Modifier.weight(.78f),
            color = if (valueTone == TransactionFactValueTone.NORMAL)
                MaterialTheme.colorScheme.onSurface else tint,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (valueTone == TransactionFactValueTone.EMPHASIS)
                FontWeight.Bold else FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (showCopy) {
            Icon(
                Icons.Rounded.ContentCopy,
                contentDescription = "Copy transaction reference",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (showChevron) {
            Icon(
                Icons.Rounded.ChevronRight, contentDescription = "Open account",
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
