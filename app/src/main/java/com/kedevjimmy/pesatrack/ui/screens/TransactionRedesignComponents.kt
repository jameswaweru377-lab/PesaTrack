package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.sp
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

internal fun canMakePersonalDebtLink(
    direction: String,
    relationType: String?,
    reversalCode: String?,
    reviewState: String
): Boolean =
    direction == MoneyDirection.EXPENSE.name &&
        relationType == null &&
        reversalCode == null &&
        reviewState != ReviewState.EXCLUDED.name &&
        reviewState != ReviewState.NEEDS_REVIEW.name

internal fun canMakePersonalDebtLink(item: StoredTransaction): Boolean =
    canMakePersonalDebtLink(
        item.direction, item.relationType, item.reversalOfCode, item.reviewState
    )

@Composable
internal fun TransactionRedesignHeader(
    item: StoredTransaction,
    category: String,
    account: String,
    onBack: () -> Unit,
    onMore: () -> Unit
) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val outgoing = item.direction == MoneyDirection.EXPENSE.name
    val transfer = item.direction == MoneyDirection.TRANSFER.name
    val incoming = item.direction == MoneyDirection.INCOME.name
    val name = item.counterparty?.takeIf(String::isNotBlank)
        ?: item.evidenceLabel?.takeIf(String::isNotBlank) ?: "Transaction"
    val foreground = Color(0xFF0B103A)
    val muted = Color(0xFF626B98)
    val violet = Color(0xFF5D00EB)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = foreground)
            }
            Text("Transaction Details", modifier = Modifier.weight(1f),
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = foreground)
            IconButton(onClick = onMore) {
                Icon(Icons.Rounded.MoreHoriz, "More transaction actions", tint = foreground)
            }
        }
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
            color = Color.White, shadowElevation = 2.dp) {
            Box(Modifier.background(Brush.linearGradient(listOf(
                Color(0xFFFFFFFF), Color(0xFFF3F4FF), Color(0xFFF5FEFF)
            ))).padding(14.dp)) {
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFDAF9F2), modifier = Modifier.size(58.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ReceiptLong, null,
                                Modifier.size(29.dp), tint = Color(0xFF008867))
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                            color = foreground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(category, fontSize = 13.sp, color = muted, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text(SimpleDateFormat("d MMM yyyy · HH:mm", locale)
                            .format(Date(item.occurredAtEpochMs)), fontSize = 12.sp, color = muted)
                        Surface(color = Color(0xFFEDEAFF), shape = RoundedCornerShape(50)) {
                            Text(account, Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                fontSize = 11.sp, color = violet, maxLines = 1)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            (if (outgoing) "− " else if (incoming) "+ " else "") +
                                formatTransactionDetailMoney(item.amountMinor),
                            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = foreground,
                            maxLines = 2
                        )
                        val chipText = if (item.reviewState == ReviewState.NEEDS_REVIEW.name)
                            "Needs review" else if (transfer) "Transfer"
                            else if (outgoing) "Money out" else if (incoming) "Money in" else "Recorded"
                        val chipColor = if (outgoing) Color(0xFFDE155B)
                            else if (incoming) Color(0xFF087E54) else violet
                        Surface(shape = RoundedCornerShape(50), color = chipColor.copy(alpha = .10f)) {
                            Text(chipText,
                                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp, fontWeight = FontWeight.Medium, color = chipColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TransactionVerificationStatus(item: StoredTransaction, hasReceiptMatch: Boolean) {
    val flagged = item.reviewState == ReviewState.NEEDS_REVIEW.name
    val label = if (flagged) "Needs your review" else if (hasReceiptMatch)
        "Receipt-linked transaction" else "Recorded transaction"
    val help = if (flagged) "Please confirm this record before treating it as final."
        else if (hasReceiptMatch) "A matching source reference is attached to this transaction."
        else "Recorded from the available financial source. No cross-source verification claimed."
    val accent = if (flagged) Color(0xFFBB6B13) else Color(0xFF007A61)
    val background = if (flagged) Color(0xFFFFF1DF) else Color(0xFFE5FAF3)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = background, border = BorderStroke(1.dp, Color.White)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.size(45.dp), shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = .10f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (flagged) Icons.Rounded.Info else Icons.Rounded.VerifiedUser,
                        null, Modifier.size(27.dp), tint = accent)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, fontWeight = FontWeight.Bold, color = accent, fontSize = 15.sp)
                Text(help, fontSize = 12.sp, lineHeight = 17.sp, color = Color(0xFF526886))
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
    val violet = Color(0xFF5E00EB)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp),
        color = Color(0xFFF4F0FF), border = BorderStroke(1.dp, Color.White),
        shadowElevation = 1.dp) {
        Column(Modifier.background(Brush.linearGradient(listOf(
            Color(0xFFF9F5FF), Color(0xFFF1F1FF), Color(0xFFF8F0FF)
        ))).padding(13.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                PesaRobot(Modifier.size(75.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Let's keep your spending organized",
                        color = violet, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("How should PesaTrack treat this payment?",
                        color = Color(0xFF0B103A), fontSize = 17.sp,
                        fontWeight = FontWeight.Bold, lineHeight = 21.sp)
                    Text("Confirm its meaning before teaching similar payments.",
                        color = Color(0xFF626B98), fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(onClick = onKeep, enabled = !isBusy,
                    modifier = Modifier.weight(1f).heightIn(min = 58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = violet,
                        contentColor = Color.White),
                    shape = RoundedCornerShape(15.dp),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 8.dp)) {
                    Icon(Icons.Rounded.CheckCircle, null, Modifier.size(19.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Yes, keep as\n" + suggested, fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, lineHeight = 16.sp, maxLines = 3)
                }
                OutlinedButton(onClick = onChoose, enabled = !isBusy,
                    modifier = Modifier.weight(1f).heightIn(min = 58.dp),
                    border = BorderStroke(1.dp, Color(0xFF9A77FF)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = violet),
                    shape = RoundedCornerShape(15.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
                    Icon(Icons.Rounded.Label, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("No, choose a different category", fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, lineHeight = 16.sp, maxLines = 3)
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
    val dark = com.kedevjimmy.pesatrack.ui.components.isPesaDarkTheme()
    val surface = if (dark) MaterialTheme.colorScheme.surface else Color.White
    val ink = if (dark) MaterialTheme.colorScheme.onSurface else Color(0xFF0B103A)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = surface, border = BorderStroke(1.dp,
            if (dark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFF0EFF8))) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).heightIn(min = 43.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(Modifier.size(35.dp), shape = RoundedCornerShape(11.dp),
                    color = Color(0xFFEDE3FF)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, Modifier.size(19.dp), tint = Color(0xFF7540EC))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, color = ink, fontSize = 16.sp)
                    if (!expanded && !supporting.isNullOrBlank()) {
                        Text(supporting, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    if (expanded) "Collapse " + title else "Expand " + title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=.30f))
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
    onSharedBill: () -> Unit,
    allowSpecial: Boolean = true
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
                                if(allowSpecial && canMakePersonalDebtLink(item)) {
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
