package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.PesaCategory
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.ingestion.TransactionMessageEvidenceResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val teal = Color(0xFF4EE5D0)
private val lilac = Color(0xFFBEA6FF)
private val deepCard = Color(0xFF142039)
private val baseNavy = Color(0xFF0A1223)
private val borderNavy = Color(0xFF324164)

internal fun canMakePersonalDebtLink(item: StoredTransaction): Boolean =
    item.direction == MoneyDirection.EXPENSE.name &&
        item.relationType == null &&
        item.reversalOfCode == null &&
        item.reviewState != ReviewState.EXCLUDED.name &&
        item.reviewState != ReviewState.NEEDS_REVIEW.name

@Composable
internal fun TransactionRedesignHeader(
    item: StoredTransaction,
    category: String,
    account: String,
    onBack: () -> Unit,
    onMore: () -> Unit
) {
    val sent = item.direction == MoneyDirection.EXPENSE.name ||
        item.direction == MoneyDirection.TRANSFER.name
    val title = item.counterparty?.takeIf(String::isNotBlank)
        ?: item.evidenceLabel?.takeIf(String::isNotBlank) ?: "Transaction"
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text("Transaction Details", modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onMore) {
                Icon(Icons.Rounded.MoreHoriz, contentDescription = "More transaction actions")
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = deepCard) {
                Icon(Icons.Rounded.ReceiptLong, null,
                    modifier = Modifier.padding(12.dp).size(30.dp), tint = teal)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(category, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(SimpleDateFormat("d MMM yyyy · HH:mm", Locale.getDefault())
                    .format(Date(item.occurredAtEpochMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text((if (sent) "− " else if (item.direction == MoneyDirection.INCOME.name) "+ " else "") +
                    formatTransactionDetailMoney(item.amountMinor),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(if (item.reviewState == ReviewState.NEEDS_REVIEW.name) "Needs review"
                    else if (sent) "Money out" else "Money in",
                    style = MaterialTheme.typography.labelSmall, color = lilac)
            }
        }
        Text("Account: " + account, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun TransactionVerificationStatus(item: StoredTransaction, hasReceiptMatch: Boolean) {
    val flagged = item.reviewState == ReviewState.NEEDS_REVIEW.name
    val text = if (flagged) "Needs your review" else if (hasReceiptMatch)
        "Receipt-linked transaction" else "Recorded transaction"
    val help = if (flagged) "PesaTrack needs confirmation before treating this as final."
        else if (hasReceiptMatch) "A matching source reference is attached to this transaction."
        else "Recorded from the available financial source. No cross-source verification claimed."
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F2A2D),
        border = BorderStroke(1.dp, teal.copy(alpha = .45f))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if (flagged) Icons.Rounded.Info else Icons.Rounded.VerifiedUser,
                null, Modifier.size(30.dp), tint = teal)
            Column(Modifier.weight(1f)) {
                Text(text, color = teal, fontWeight = FontWeight.SemiBold)
                Text(help, style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC1D4D6), maxLines = 2)
            }
        }
    }
}

/** Vector-only helper; no external illustration SDK, images or user financial data. */
@Composable
private fun PesaRobot(modifier: Modifier = Modifier) {
    Canvas(modifier.size(78.dp)) {
        val w = size.width; val h = size.height
        drawCircle(Color(0xFFDFDDFF), radius = w * .40f, center = Offset(w * .5f, h * .43f))
        drawRoundRect(color = Color(0xFF1A244C), topLeft = Offset(w*.17f, h*.25f),
            size = Size(w*.66f, h*.36f), cornerRadius = CornerRadius(w*.17f))
        drawArc(teal, 5f, 170f, useCenter = false,
            topLeft = Offset(w*.30f, h*.39f), size = Size(w*.15f,h*.10f),
            style = Stroke(width = w*.038f))
        drawArc(teal, 5f, 170f, useCenter = false,
            topLeft = Offset(w*.56f, h*.39f), size = Size(w*.15f,h*.10f),
            style = Stroke(width = w*.038f))
        drawRoundRect(color = Color(0xFFC5C3FA), topLeft = Offset(w*.34f,h*.75f),
            size = Size(w*.32f,h*.22f), cornerRadius = CornerRadius(w*.12f))
    }
}

@Composable
internal fun TransactionDecisionCard(
    suggested: String,
    isBusy: Boolean,
    onKeep: () -> Unit,
    onChoose: () -> Unit
) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        color = deepCard,
        border = BorderStroke(1.dp, lilac.copy(alpha = .70f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PesaRobot()
                Column(Modifier.weight(1f)) {
                    Text("Let's keep your spending organized",
                        color = lilac, style = MaterialTheme.typography.labelMedium)
                    Text("How should PesaTrack treat this payment?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text("Confirm its meaning before teaching similar payments.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onKeep, enabled = !isBusy,
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = teal,
                        contentColor = baseNavy),
                    shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Rounded.LocalCafe, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Yes, keep as", style = MaterialTheme.typography.labelSmall)
                        Text(suggested, fontWeight = FontWeight.Bold,
                            maxLines = 2, style = MaterialTheme.typography.labelMedium)
                    }
                }
                OutlinedButton(onClick = onChoose, enabled = !isBusy,
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, lilac)) {
                    Icon(Icons.Rounded.Label, null, Modifier.size(20.dp), tint = lilac)
                    Spacer(Modifier.width(6.dp))
                    Text("No, choose a different category",
                        color = lilac, style = MaterialTheme.typography.labelSmall,
                        maxLines = 3)
                }
            }
        }
    }
}

@Composable
internal fun RedesignDetailAccordion(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    supporting: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=.40f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onToggle)
                .heightIn(min=42.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(icon, null, Modifier.size(21.dp), tint = lilac)
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall)
                    if (!expanded && !supporting.isNullOrBlank()) {
                        Text(supporting, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    if (expanded) "Collapse " + title else "Expand " + title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=.45f))
                content()
            }
        }
    }
}

@Composable
internal fun TransactionOriginalMessage(
    evidence: TransactionMessageEvidenceResult?,
    source: String
) {
    val body = evidence?.message?.body
    if (body == null) Text(evidence?.unavailableReason ?: "Original message is not yet available for " + source + ".",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall)
    else SelectionContainer {
        Text(body, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 6.dp))
    }
}

@Composable
private fun CategoryOption(
    label: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier.clickable(onClick = onClick),
        color = if (selected) Color(0xFF10434C) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp,
            if (selected) teal else MaterialTheme.colorScheme.outlineVariant.copy(alpha=.38f))) {
        Row(Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icon, null, Modifier.size(23.dp), tint = if (selected) teal else lilac)
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold, maxLines = 2)
                if (subtitle.isNotBlank()) Text(subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Rounded.CheckCircle, "Selected", Modifier.size(18.dp), tint=teal)
        }
    }
}

/**
 * Page-like modal intentionally preserves the original Activity navigation state.
 * "Money lent" and "Shared bill" are distinct ledger flows, never ordinary categories.
 */
@Composable
internal fun TransactionCategoryChooser(
    item: StoredTransaction,
    initialCategoryId: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Boolean) -> Unit,
    onMoneyLent: () -> Unit,
    onSharedBill: () -> Unit
) {
    val context = LocalContext.current
    val categoryStore = remember { CategoryStore.get(context) }
    var categories by remember(item.id) {
        mutableStateOf(categoryStore.activityCategories())
    }
    var selectedId by remember(item.id) { mutableStateOf(initialCategoryId) }
    var query by remember(item.id) { mutableStateOf("") }
    var onlyThis by remember(item.id) { mutableStateOf(false) }
    var addingCategory by remember(item.id) { mutableStateOf(false) }
    var newCategoryName by remember(item.id) { mutableStateOf("") }
    var customError by remember(item.id) { mutableStateOf<String?>(null) }
    if (addingCategory) {
        AlertDialog(
            onDismissRequest = { addingCategory = false },
            title = { Text("Add category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This category will also be available in your budgets.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(newCategoryName, { newCategoryName = it },
                        label = { Text("Category name") },
                        singleLine = true)
                    customError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    runCatching { categoryStore.addCustom(newCategoryName) }
                        .onSuccess { added ->
                            categories = categoryStore.activityCategories()
                            selectedId = added.id
                            customError = null
                            addingCategory = false
                        }
                        .onFailure { customError = "Choose a valid, unique category name." }
                }) { Text("Add category") }
            },
            dismissButton = {
                TextButton(onClick = { addingCategory = false }) { Text("Cancel") }
            }
        )
    }
    val suggested = categories.firstOrNull { it.id == initialCategoryId }
        ?: categories.firstOrNull { it.id == TransactionCategory.EATING_OUT.name }
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column {
                Row(Modifier.fillMaxWidth().padding(horizontal=12.dp, vertical=8.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick=onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                    Text("Choose category", Modifier.weight(1f),
                        style=MaterialTheme.typography.titleLarge,
                        fontWeight=FontWeight.Bold)
                }
                LazyColumn(Modifier.weight(1f),
                    contentPadding=PaddingValues(horizontal=14.dp, vertical=8.dp),
                    verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    item {
                        Surface(shape=RoundedCornerShape(16.dp),
                            color=MaterialTheme.colorScheme.surface) {
                            Row(Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ReceiptLong,null,tint=teal)
                                Column(Modifier.weight(1f).padding(start=12.dp)) {
                                    Text(item.counterparty?.takeIf(String::isNotBlank) ?: "Transaction",
                                        fontWeight=FontWeight.Bold)
                                    Text("Recorded transaction · " + item.sourceKind,
                                        style=MaterialTheme.typography.labelSmall,
                                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(formatTransactionDetailMoney(item.amountMinor),
                                    fontWeight=FontWeight.Bold)
                            }
                        }
                    }
                    item {
                        OutlinedTextField(query, { query=it },
                            Modifier.fillMaxWidth(), singleLine=true,
                            leadingIcon={Icon(Icons.Rounded.Search,null)},
                            placeholder={Text("Search categories")},
                            shape=RoundedCornerShape(16.dp))
                    }
                    if(query.isBlank()) {
                        item {
                            Text("Suggested for this payment",
                                fontWeight=FontWeight.SemiBold,
                                style=MaterialTheme.typography.titleMedium)
                        }
                        item {
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                CategoryOption(suggested?.label ?: "Choose category",
                                    "Current suggestion", Icons.Rounded.LocalCafe,
                                    selectedId == suggested?.id,
                                    onClick={if(suggested!=null) selectedId=suggested.id},
                                    modifier=Modifier.weight(1f))
                                if(canMakePersonalDebtLink(item)) {
                                    CategoryOption("Money lent", "Loan to a person",
                                        Icons.Rounded.VolunteerActivism, false,
                                        onClick=onMoneyLent, modifier=Modifier.weight(1f))
                                    CategoryOption("Shared bill", "Split costs",
                                        Icons.Rounded.Groups, false,
                                        onClick=onSharedBill, modifier=Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    item {
                        Text("All categories", style=MaterialTheme.typography.titleMedium,
                            fontWeight=FontWeight.SemiBold)
                    }
                    val matching = categories.filter {
                        query.isBlank() || it.label.contains(query, ignoreCase=true)
                    }
                    items(matching.chunked(2)) { pair ->
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            pair.forEach { c ->
                                CategoryOption(c.label, "", Icons.Rounded.Label,
                                    c.id==selectedId,
                                    onClick={selectedId=c.id},
                                    modifier=Modifier.weight(1f))
                            }
                            if(pair.size==1) Spacer(Modifier.weight(1f))
                        }
                    }
                    if(matching.isEmpty()) item {
                        Text("No categories match your search.")
                    }
                    item {
                        TextButton(onClick = { addingCategory = true }) {
                            Icon(Icons.Rounded.Add, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add custom category")
                        }
                    }
                    item {
                        Text("Remembering similar payments is optional. Source transactions remain separate.",
                            color=MaterialTheme.colorScheme.onSurfaceVariant,
                            style=MaterialTheme.typography.bodySmall)
                    }
                }
                Surface(color=MaterialTheme.colorScheme.surface,
                    shadowElevation=6.dp) {
                    Column(Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Button(onClick={onSave(selectedId, !onlyThis)},
                            modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),
                            enabled=!busy && categories.any { it.id==selectedId },
                            colors=ButtonDefaults.buttonColors(containerColor=teal,
                                contentColor=baseNavy)) {
                            Text("Save category", fontWeight=FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth().clickable { onlyThis=!onlyThis }
                            .heightIn(min=42.dp), verticalAlignment=Alignment.CenterVertically) {
                            Checkbox(checked=onlyThis, onCheckedChange={onlyThis=it})
                            Column {
                                Text("Only this transaction",
                                    style=MaterialTheme.typography.labelLarge)
                                Text("Do not apply to similar payments",
                                    style=MaterialTheme.typography.labelSmall,
                                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
