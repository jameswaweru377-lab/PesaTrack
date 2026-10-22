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
private data class CategoryIconVisual(val icon: ImageVector, val tint: Color, val backing: Color)

private fun categoryIconVisual(label: String): CategoryIconVisual {
    val v = Color(0xFF5C17E9); val green = Color(0xFF08784E)
    val pink = Color(0xFFEF1761); val blue = Color(0xFF1459EA)
    val orange = Color(0xFFE87115)
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
    val ink = Color(0xFF0B103A)
    Surface(
        modifier = modifier.clickable(onClick = onSelect),
        color = if (selected) Color(0xFFF6F0FF) else Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(if (selected) 1.5.dp else .7.dp,
            if (selected) Color(0xFF6620FA) else Color(0xFFE9E8F1))
    ) {
        Box {
            Column(
                Modifier.fillMaxWidth().padding(
                    horizontal = if (compact) 5.dp else 7.dp,
                    vertical = if (compact) 10.dp else 13.dp
                ), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(Modifier.size(if (compact) 40.dp else 43.dp),
                    color = v.backing, shape = CircleShape) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(v.icon, null, Modifier.size(if (compact) 23.dp else 25.dp),
                            tint = v.tint)
                    }
                }
                Text(category.label, fontSize = if (compact) 11.sp else 12.sp,
                    color = if (selected) Color(0xFF5D00EB) else ink,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2, lineHeight = 14.sp, overflow = TextOverflow.Ellipsis)
            }
            if (selected) {
                Surface(Modifier.align(Alignment.TopEnd).padding(5.dp).size(19.dp),
                    color = Color(0xFF5D00EB), shape = CircleShape) {
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
    val ink = if (destructive) Color(0xFFBF3451) else Color(0xFF302278)
    val background = if (destructive) Color(0xFFFFF5F7) else Color.White
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = background,
        border = BorderStroke(1.dp,
            if (destructive) Color(0xFFF3D6DD) else Color(0xFFE7E1F4))
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
                    color = Color(0xFF626B98))
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
    val purple = Color(0xFF6100E8)
    val ink = Color(0xFF0B103A)
    val muted = Color(0xFF626B98)
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val current = categories.firstOrNull { it.id == initialCategoryId }
    val suggested = (listOfNotNull(current) + categories.filter { c ->
        listOf("Phone", "Food", "Utilities", "Groceries", "Housing").any {
            c.label.contains(it, ignoreCase = true)
        }
    }).distinctBy { it.id }.take(6)
    val matching = categories.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
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
        containerColor = Color(0xFFFCFBFF),
        scrimColor = Color(0xB50A1030),
        dragHandle = {
            Surface(Modifier.padding(top = 9.dp, bottom = 11.dp).size(58.dp, 5.dp),
                color = Color(0xFFA7A6B7), shape = CircleShape) {}
        }
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.91f),
            verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(Modifier.size(54.dp), color = Color(0xFFECE4FF),
                    shape = RoundedCornerShape(19.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ReceiptLong, null, Modifier.size(27.dp), tint = purple)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("Choose a category", fontSize = 22.sp,
                        fontWeight = FontWeight.Bold, color = ink)
                    Text("How should PesaTrack treat this payment?",
                        fontSize = 13.sp, color = muted, maxLines = 2)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, "Close category picker", tint = ink)
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(Modifier.fillMaxWidth().padding(horizontal = 19.dp),
                color = Color.White, shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color(0xFFE9E7F3))) {
                Row(Modifier.heightIn(min = 52.dp).padding(horizontal = 14.dp),
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
                contentPadding = PaddingValues(start = 19.dp, end = 19.dp, top = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)) {
                if (query.isBlank() && suggested.isNotEmpty()) {
                    item {
                        Text("Suggested for you", fontWeight = FontWeight.Bold,
                            color = ink, fontSize = 16.sp)
                    }
                    item {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            suggested.forEach { c ->
                                CategoryReferenceTile(
                                    category = c, selected = selectedId == c.id,
                                    modifier = Modifier.width(115.dp),
                                    onSelect = { selectedId = c.id }
                                )
                            }
                        }
                    }
                    item { HorizontalDivider(color = Color(0xFFEAE8F4)) }
                }
                item {
                    Text("All categories", fontWeight = FontWeight.Bold,
                        color = ink, fontSize = 16.sp)
                }
                item {
                    BoxWithConstraints {
                        val cols = if (maxWidth >= 650.dp) 5 else if (maxWidth >= 425.dp) 4 else 3
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            matching.chunked(cols).forEach { row ->
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    row.forEach { c ->
                                        CategoryReferenceTile(c, selectedId == c.id,
                                            Modifier.weight(1f), compact = true,
                                            onSelect = { selectedId = c.id })
                                    }
                                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                                }
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
                // These are alternative ledger treatments, not regular spending
                // categories. Keep them visually apart from selectable tiles.
                if (query.isBlank() && allowSpecial && canMakePersonalDebtLink(item)) {
                    item {
                        Text("Other ways to classify", color = ink, fontSize = 15.sp,
                            fontWeight = FontWeight.Bold)
                    }
                    item {
                        CategoryTreatmentAction(
                            "Money lent", "Record money someone owes you",
                            Icons.Rounded.VolunteerActivism, enabled = !busy,
                            onClick = onMoneyLent)
                    }
                    item {
                        CategoryTreatmentAction(
                            "Shared bill", "Record your share and money others owe",
                            Icons.Rounded.Groups, enabled = !busy,
                            onClick = onSharedBill)
                    }
                }
                if (query.isBlank() && allowCorrections) {
                    val acceptIdentity = item.counterparty.isNullOrBlank() &&
                        !item.evidenceLabel.isNullOrBlank()
                    val refundLink = item.relationType == "REFUND"
                    val canOwnTransfer = item.relationType == null &&
                        item.direction != MoneyDirection.TRANSFER.name &&
                        item.reviewState != ReviewState.EXCLUDED.name
                    val canExclude = item.reviewState != ReviewState.EXCLUDED.name
                    if (acceptIdentity || refundLink || canOwnTransfer || canExclude) {
                        item {
                            HorizontalDivider(color = Color(0xFFEAE8F4))
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
                    if (canOwnTransfer) item {
                        CategoryTreatmentAction(
                            "Mark as own-account transfer",
                            "Only for movements between accounts you own",
                            Icons.Rounded.SwapHoriz, enabled = !busy,
                            onClick = { onCorrection(TransactionCorrectionAction.MARK_OWN_TRANSFER) })
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
            Surface(color = Color(0xFFFCFBFF), shadowElevation = 4.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 19.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(Modifier.fillMaxWidth().clickable { onlyThis = !onlyThis }
                        .heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = onlyThis, onCheckedChange = { onlyThis = it })
                        Column {
                            Text("Only this transaction", fontSize = 12.sp, color = ink)
                            Text("Otherwise PesaTrack may learn from similar payments",
                                fontSize = 10.sp, color = muted)
                        }
                    }
                    Button(onClick = { onSave(selectedId, !onlyThis) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                        enabled = !busy && categories.any { it.id == selectedId },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = purple, contentColor = Color.White)
                    ) {
                        Text("Save Category", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
