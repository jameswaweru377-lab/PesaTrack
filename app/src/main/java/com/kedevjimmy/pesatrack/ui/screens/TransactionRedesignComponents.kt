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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.PesaCategory
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.TransactionStore
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
    onChooseCategory: () -> Unit,
    onAddNote: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
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
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Rounded.MoreHoriz, "Transaction menu", tint = foreground)
                }
                DropdownMenu(expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Choose category") },
                        leadingIcon = { Icon(Icons.Rounded.Label, null) },
                        onClick = { menuExpanded = false; onChooseCategory() }
                    )
                    DropdownMenuItem(
                        text = { Text("Add note or receipt") },
                        leadingIcon = { Icon(Icons.Rounded.EditNote, null) },
                        onClick = { menuExpanded = false; onAddNote() }
                    )
                }
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
        ))).padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onKeep, enabled = !isBusy,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = violet,
                        contentColor = Color.White),
                    shape = RoundedCornerShape(13.dp),
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp)) {
                    Icon(Icons.Rounded.CheckCircle, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Yes, keep as\n" + suggested, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, lineHeight = 14.sp, maxLines = 3)
                }
                OutlinedButton(onClick = onChoose, enabled = !isBusy,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    border = BorderStroke(1.dp, Color(0xFF9A77FF)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = violet),
                    shape = RoundedCornerShape(13.dp),
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp)) {
                    Icon(Icons.Rounded.Label, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("No, choose a different category", fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, lineHeight = 14.sp, maxLines = 3)
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
    val p = BrownHomePalette
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = p.white, border = BorderStroke(1.dp, p.outline.copy(alpha = .55f))) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(35.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(24.dp), tint = p.coffee)
                }
                VerticalDivider(Modifier.height(32.dp), color = p.outline.copy(alpha = .7f))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, color = p.ink, fontSize = 16.sp)
                    if (!expanded && !supporting.isNullOrBlank()) {
                        Text(supporting, fontSize = 12.sp,
                            color = p.muted,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    if (expanded) "Collapse " + title else "Expand " + title,
                    tint = p.coffee)
            }
            if (expanded) {
                HorizontalDivider(color = p.outline.copy(alpha = .5f))
                CompositionLocalProvider(LocalContentColor provides p.ink) { content() }
            }
        }
    }
}

@Composable
internal fun TransactionOriginalMessage(
    evidence: TransactionMessageEvidenceResult?,
    source: String
) {
    val p = BrownHomePalette
    val body = evidence?.message?.body
    if (body == null) Text(evidence?.unavailableReason ?: "Original message is not yet available for " + source + ".",
        color = p.muted,
        style = MaterialTheme.typography.bodySmall)
    else SelectionContainer {
        Text(body, style = MaterialTheme.typography.bodyMedium,
            color = p.ink,
            modifier = Modifier.padding(vertical = 6.dp))
    }
}

@Composable
private fun BrownSpecialClassification(
    label: String, icon: ImageVector, modifier: Modifier,
    enabled: Boolean, onClick: () -> Unit
) {
    val p = BrownHomePalette
    Surface(modifier.clickable(enabled = enabled, onClick = onClick),
        color = p.cream, shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, p.outline)) {
        Column(Modifier.fillMaxWidth().heightIn(min = 68.dp)
            .padding(horizontal = 3.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, null, Modifier.size(24.dp),
                tint = if (enabled) p.coffee else p.muted)
            Text(label, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp, color = if (enabled) p.coffee else p.muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Page-like modal intentionally preserves the original Activity navigation state.
 * "Money lent" and "Shared bill" are distinct ledger flows, never ordinary categories.
 */
private data class CategoryIconVisual(val icon: ImageVector, val tint: Color, val backing: Color)

private fun categoryIconVisual(label: String): CategoryIconVisual {
    val v = BrownHomePalette.coffee; val green = v
    val pink = v; val blue = v; val orange = v
    return when {
        label.contains("phone", true) || label.contains("airtime", true) ||
            label.contains("data", true) || label.contains("internet", true) ->
            CategoryIconVisual(Icons.Rounded.Smartphone, v, Color(0xFFECE5FF))
        label.contains("food", true) || label.contains("dining", true) ||
            label.contains("eat", true) ->
            CategoryIconVisual(Icons.Rounded.Restaurant, orange, Color(0xFFFFEBDD))
        label.contains("elect", true) || label.contains("utilit", true) ->
            CategoryIconVisual(Icons.Rounded.Bolt, blue, Color(0xFFE2EFFF))
        label.contains("grocer", true) || label.contains("shop", true) ->
            CategoryIconVisual(Icons.Rounded.ShoppingCart, pink, Color(0xFFFDE6F0))
        label.contains("hous", true) || label.contains("rent", true) ->
            CategoryIconVisual(Icons.Rounded.Home, green, Color(0xFFDDF9EF))
        label.contains("transport", true) || label.contains("travel", true) ->
            CategoryIconVisual(Icons.Rounded.DirectionsCar, blue, Color(0xFFE2EFFF))
        label.contains("health", true) || label.contains("medic", true) ->
            CategoryIconVisual(Icons.Rounded.Favorite, pink, Color(0xFFFCE8F1))
        label.contains("educat", true) || label.contains("school", true) ->
            CategoryIconVisual(Icons.Rounded.School, v, Color(0xFFECE5FF))
        label.contains("entertain", true) || label.contains("game", true) ->
            CategoryIconVisual(Icons.Rounded.SportsEsports, pink, Color(0xFFFDE6F0))
        label.contains("personal", true) || label.contains("beaut", true) ->
            CategoryIconVisual(Icons.Rounded.Spa, pink, Color(0xFFFDE6F0))
        label.contains("income", true) || label.contains("salary", true) ->
            CategoryIconVisual(Icons.Rounded.Work, green, Color(0xFFDDF9EF))
        label.contains("transf", true) ->
            CategoryIconVisual(Icons.Rounded.SwapHoriz, v, Color(0xFFECE5FF))
        label.contains("bank", true) || label.contains("financial", true) ->
            CategoryIconVisual(Icons.Rounded.AccountBalance, blue, Color(0xFFE2EFFF))
        else -> CategoryIconVisual(Icons.Rounded.Label, v, Color(0xFFECE5FF))
    }
}

@Composable
private fun CategoryReferenceTile(
    category: PesaCategory,
    selected: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onSelect: () -> Unit
) {
    val v = categoryIconVisual(category.label)
    val p = BrownHomePalette
    Surface(
        modifier = modifier.clickable(onClick = onSelect),
        color = if (selected) p.cream else p.white,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (selected) 1.5.dp else .7.dp,
            if (selected) p.coffee else p.outline)
    ) {
        Box {
            Column(
                Modifier.fillMaxWidth().padding(
                    horizontal = if (compact) 5.dp else 7.dp,
                    vertical = if (compact) 7.dp else 11.dp
                ), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(v.icon, null, Modifier.size(if (compact) 25.dp else 27.dp),
                    tint = p.coffee)
                Text(category.label, fontSize = if (compact) 12.sp else 13.sp,
                    color = if (selected) p.coffee else p.ink,
                    fontFamily = FontFamily.Serif,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2, minLines = 1, lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis)
            }
            if (selected) {
                Surface(Modifier.align(Alignment.TopEnd).padding(5.dp).size(19.dp),
                    color = p.coffee, shape = CircleShape) {
                    Icon(Icons.Rounded.Check, "Selected", Modifier.padding(3.dp), tint = Color.White)
                }
            }
        }
    }
}

/**
 * Approved vibrant category selection bottom sheet. Only the presentation
 * changes: canonical category saving, optional learning, custom categories,
 * money lending and shared-bill actions retain their existing callbacks.
 */
@Composable
private fun CategoryTreatmentAction(
    title: String,
    description: String,
    icon: ImageVector,
    destructive: Boolean = false,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val p = BrownHomePalette
    val ink = if (destructive) MaterialTheme.colorScheme.error else p.coffee
    val background = if (destructive) p.cream else p.white
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = background,
        border = BorderStroke(1.dp,
            p.outline)
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 53.dp)
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Surface(Modifier.size(35.dp), color = ink.copy(alpha = .09f),
                shape = RoundedCornerShape(10.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(21.dp), tint = ink)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(description, fontSize = 11.sp, lineHeight = 14.sp,
                    color = p.muted)
            }
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = ink)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionCategoryChooser(
    item: StoredTransaction,
    initialCategoryId: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Boolean) -> Unit,
    onMoneyLent: () -> Unit,
    onSharedBill: () -> Unit,
    onCorrection: (TransactionCorrectionAction) -> Unit = {},
    allowCorrections: Boolean = false,
    allowSpecial: Boolean = true
) {
    val context = LocalContext.current
    val categoryStore = remember { CategoryStore.get(context) }
    var categories by remember(item.id) { mutableStateOf(categoryStore.activityCategories()) }
    var selectedId by remember(item.id) { mutableStateOf(initialCategoryId) }
    var query by remember(item.id) { mutableStateOf("") }
    var onlyThis by remember(item.id) { mutableStateOf(false) }
    var addingCategory by remember(item.id) { mutableStateOf(false) }
    var newCategoryName by remember(item.id) { mutableStateOf("") }
    var customError by remember(item.id) { mutableStateOf<String?>(null) }
    val palette = BrownHomePalette
    val ink = palette.ink
    val muted = palette.muted
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val matching = categories.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
    val canDebtLink = allowSpecial && canMakePersonalDebtLink(item)
    val canOwnTransfer = allowCorrections && item.relationType == null &&
        item.direction != MoneyDirection.TRANSFER.name &&
        item.reviewState != ReviewState.EXCLUDED.name
    if (addingCategory) {
        AlertDialog(
            onDismissRequest = { addingCategory = false },
            title = { Text("Add category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This category will also be available in your budgets.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(newCategoryName, { newCategoryName = it },
                        label = { Text("Category name") }, singleLine = true)
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = palette.canvas,
        scrimColor = ink.copy(alpha = .65f),
        dragHandle = {
            Surface(Modifier.padding(top = 9.dp, bottom = 11.dp).size(58.dp, 5.dp),
                color = palette.outline, shape = CircleShape) {}
        }
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.91f),
            verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Choose a category", fontSize = 25.sp,
                        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = ink)
                    Text("How should PesaTrack treat this payment?",
                        fontSize = 13.sp, color = muted, maxLines = 2)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, "Close category picker", tint = ink)
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                color = palette.white, shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, palette.outline)) {
                Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    Icon(Icons.Rounded.Search, null, Modifier.size(23.dp), tint = muted)
                    TextField(value = query, onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search categories...", fontSize = 14.sp, color = muted) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent))
                    if (query.isNotBlank()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Close, "Clear search")
                    }
                }
            }
            LazyColumn(Modifier.weight(1f).testTag("category-selection-list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 13.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (query.isBlank() && (allowSpecial || allowCorrections)) {
                    item {
                        Column {
                            Text("Other ways to classify", color = ink,
                                fontFamily = FontFamily.Serif, fontSize = 18.sp,
                                fontWeight = FontWeight.Bold)
                            Text("Not regular spending categories", color = palette.coffee,
                                fontSize = 12.sp)
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            BrownSpecialClassification("Money lent", Icons.Rounded.VolunteerActivism,
                                Modifier.weight(1f), enabled = canDebtLink && !busy,
                                onClick = onMoneyLent)
                            BrownSpecialClassification("Shared bill", Icons.Rounded.Groups,
                                Modifier.weight(1f), enabled = canDebtLink && !busy,
                                onClick = onSharedBill)
                            BrownSpecialClassification("Own transfer", Icons.Rounded.SwapHoriz,
                                Modifier.weight(1f), enabled = canOwnTransfer && !busy,
                                onClick = { onCorrection(TransactionCorrectionAction.MARK_OWN_TRANSFER) })
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Categories", modifier = Modifier.weight(1f),
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold, color = ink, fontSize = 19.sp)
                        Text("Scroll for more  ⌄", color = palette.coffee, fontSize = 12.sp)
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        matching.chunked(3).forEach { row ->
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { c ->
                                    CategoryReferenceTile(c, selectedId == c.id,
                                        Modifier.weight(1f), compact = true,
                                        onSelect = { selectedId = c.id })
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                if (matching.isEmpty()) item {
                    Text("No categories match your search.", color = muted, fontSize = 13.sp)
                }
                item {
                    TextButton(onClick = { addingCategory = true }) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(5.dp))
                        Text("Add category")
                    }
                }
                // Special classifications stay above the category grid so users
                // can discover them without scrolling. These are not categories.
                if (query.isBlank() && allowCorrections) {
                    val acceptIdentity = item.counterparty.isNullOrBlank() &&
                        !item.evidenceLabel.isNullOrBlank()
                    val refundLink = item.relationType == TransactionStore.RELATION_REFUND
                    val canExclude = item.reviewState != ReviewState.EXCLUDED.name
                    if (acceptIdentity || refundLink || canExclude) {
                        item {
                            HorizontalDivider(color = palette.outline)
                            Spacer(Modifier.height(7.dp))
                            Text("Transaction treatment", color = ink, fontSize = 15.sp,
                                fontWeight = FontWeight.Bold)
                            Text("These changes affect how this entry is understood or counted. A confirmation is required.",
                                color = muted, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }
                    if (acceptIdentity) item {
                        CategoryTreatmentAction(
                            "Use suggested identity", "Confirm the name from the original message",
                            Icons.Rounded.CheckCircle, enabled = !busy,
                            onClick = { onCorrection(TransactionCorrectionAction.ACCEPT_IDENTITY) })
                    }
                    if (refundLink) item {
                        CategoryTreatmentAction(
                            "This was not a refund", "Remove its existing refund relationship",
                            Icons.Rounded.LinkOff, enabled = !busy,
                            onClick = { onCorrection(TransactionCorrectionAction.REMOVE_REFUND_LINK) })
                    }
                    if (canExclude) item {
                        CategoryTreatmentAction(
                            "Exclude from tracked activity",
                            "Remove from Activity totals; not just a category change",
                            Icons.Rounded.Block, destructive = true, enabled = !busy,
                            onClick = { onCorrection(TransactionCorrectionAction.EXCLUDE_ENTRY) })
                    }
                }
            }
            Surface(color = palette.canvas, shadowElevation = 4.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth().clickable { onlyThis = !onlyThis }
                        .heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = onlyThis, onCheckedChange = { onlyThis = it },
                            colors = CheckboxDefaults.colors(checkedColor = palette.coffee))
                        Column {
                            Text("Only this transaction", fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold, color = ink)
                            Text("Otherwise remember for similar payments.",
                                fontSize = 11.sp, color = muted)
                        }
                    }
                    Button(onClick = { onSave(selectedId, !onlyThis) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        enabled = !busy && categories.any { it.id == selectedId },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.coffee, contentColor = Color.White)
                    ) {
                        Text("Save category", fontFamily = FontFamily.Serif,
                            fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
