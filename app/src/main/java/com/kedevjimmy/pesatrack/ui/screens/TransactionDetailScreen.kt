package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.LandscapeBackdrop
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.SmallStatusPill
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailScreen(
    scaffoldPadding: PaddingValues,
    transactionId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { TransactionStore.get(context) }
    val ruleStore = remember { RuleStore.get(context) }
    var transaction by remember(transactionId) {
        mutableStateOf(store.getById(transactionId))
    }
    var pendingCategory by remember(transactionId) {
        mutableStateOf<TransactionCategory?>(null)
    }
    var pendingMatchCount by remember(transactionId) {
        mutableStateOf(0)
    }
    var learnedMessage by remember(transactionId) {
        mutableStateOf<String?>(null)
    }

    fun refresh() {
        transaction = store.getById(transactionId)
    }

    fun classifyOnly(
        current: StoredTransaction,
        category: TransactionCategory
    ) {
        val direction = directionForCategory(
            category = category,
            existingDirection = runCatching {
                MoneyDirection.valueOf(current.direction)
            }.getOrDefault(MoneyDirection.NEUTRAL)
        )
        store.classifyManually(
            id = current.id,
            category = category,
            direction = direction,
            markTransfer = category == TransactionCategory.TRANSFER
        )
        refresh()
    }

    fun learningRule(
        current: StoredTransaction,
        category: TransactionCategory,
        id: String = "preview"
    ): SmartRule {
        val direction = directionForCategory(
            category = category,
            existingDirection = runCatching {
                MoneyDirection.valueOf(current.direction)
            }.getOrDefault(MoneyDirection.NEUTRAL)
        )
        return SmartRule(
            id = id,
            counterpartyContains = "",
            counterpartyExact = current.counterparty.orEmpty(),
            counterpartyPhoneEquals = current.counterpartyPhone.orEmpty(),
            category = category,
            direction = direction,
            name = current.counterparty?.let { "Always classify " + it } ?: "Learned rule",
            displayLabel = current.counterparty,
            accountId = current.accountId
        )
    }

    fun applyCategory(category: TransactionCategory) {
        val current = transaction ?: return
        val shouldOfferLearning =
            current.ruleId == null &&
                current.reviewState == ReviewState.NEEDS_REVIEW.name &&
                category != TransactionCategory.UNCATEGORIZED &&
                canLearnIdentity(current)

        if (shouldOfferLearning) {
            val preview = learningRule(current, category)
            pendingMatchCount = store.matchingPendingCount(
                rule = preview,
                excludingId = current.id
            )
            pendingCategory = category
        } else {
            classifyOnly(current, category)
        }
    }

    val current = transaction

    if (current != null && pendingCategory != null) {
        val category = pendingCategory!!
        AlertDialog(
            onDismissRequest = { pendingCategory = null },
            title = {
                Text("Remember this for next time?")
            },
            text = {
                Text(
                    buildString {
                        append("Use “")
                        append(current.counterparty ?: current.counterpartyPhone ?: "this identity")
                        append("” as ")
                        append(category.prettyName())
                        append(".")
                        if (pendingMatchCount > 0) {
                            append(" This can also classify ")
                            append(pendingMatchCount)
                            append(" matching transaction")
                            if (pendingMatchCount != 1) append("s")
                            append(" already waiting for review.")
                        }
                    }
                )
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        classifyOnly(current, category)
                        pendingCategory = null
                    }
                ) {
                    Text("Only this one")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val direction = directionForCategory(
                            category = category,
                            existingDirection = runCatching {
                                MoneyDirection.valueOf(current.direction)
                            }.getOrDefault(MoneyDirection.NEUTRAL)
                        )
                        store.classifyManually(
                            id = current.id,
                            category = category,
                            direction = direction,
                            markTransfer = category == TransactionCategory.TRANSFER
                        )
                        val savedRule = ruleStore.save(
                            name = current.counterparty?.let { "Remember " + it } ?: "Learned rule",
                            counterpartyContains = "",
                            counterpartyExact = current.counterparty.orEmpty(),
                            counterpartyPhoneEquals = current.counterpartyPhone.orEmpty(),
                            category = category,
                            direction = direction,
                            displayLabel = current.counterparty,
                            accountId = current.accountId
                        )
                        val changed = store.reapplyRule(savedRule)
                        learnedMessage = if (changed > 0) {
                            "Rule saved • " + changed + " matching pending transaction" +
                                if (changed == 1) " classified." else "s classified."
                        } else {
                            "Rule saved for future matching transactions."
                        }
                        pendingCategory = null
                        refresh()
                    }
                ) {
                    Text("Remember")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Transaction details",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = ::refresh) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                }
            }
        }

        if (current == null) {
            item {
                PesaCard {
                    Text(
                        "This transaction is no longer available.",
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@LazyColumn
        }

        item {
            TransactionHero(current)
        }

        item {
            SectionHeader("Classification")
            Spacer(Modifier.height(8.dp))
            ClassificationCard(current)
        }

        learnedMessage?.let { message ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = pesaToneContainer("green")
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = pesaToneContent("green")
                        )
                        Text(
                            message,
                            modifier = Modifier.padding(start = 10.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item {
            SectionHeader("Choose a category")
            Spacer(Modifier.height(8.dp))
            CategoryGrid(
                current = runCatching {
                    TransactionCategory.valueOf(current.category)
                }.getOrDefault(TransactionCategory.UNCATEGORIZED),
                onSelected = ::applyCategory
            )
        }

        item {
            SectionHeader("Transaction information")
            Spacer(Modifier.height(8.dp))
            PesaCard {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    DetailRow(Icons.Rounded.AccountCircle, "Name", current.counterparty ?: "Not identified")
                    DetailRow(Icons.Rounded.ReceiptLong, "Type", pretty(current.type))
                    DetailRow(Icons.Rounded.ReceiptLong, "M-Pesa code", current.transactionCode ?: "Not provided")
                    DetailRow(Icons.Rounded.CalendarMonth, "Transaction date", formatDateTime(current.occurredAtEpochMs))
                    if (current.receivedAtEpochMs != current.occurredAtEpochMs) {
                        DetailRow(Icons.Rounded.CalendarMonth, "Message received", formatDateTime(current.receivedAtEpochMs))
                    }
                    DetailRow(Icons.Rounded.Paid, "Fee", formatMoney(current.feeMinor))
                    current.planningMonth?.let {
                        DetailRow(Icons.Rounded.CalendarMonth, "Planning month", it)
                    }
                    DetailRow(Icons.Rounded.Tune, "Source", pretty(current.sourceKind))
                }
            }
        }

        item {
            SectionHeader("Actions")
            Spacer(Modifier.height(8.dp))
            PesaCard {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    ActionRow(
                        icon = Icons.Rounded.SwapHoriz,
                        label = "Mark as own-account transfer",
                        onClick = {
                            store.classifyManually(
                                id = current.id,
                                category = TransactionCategory.TRANSFER,
                                direction = MoneyDirection.TRANSFER,
                                markTransfer = true
                            )
                            refresh()
                        }
                    )
                    ActionRow(
                        icon = Icons.Rounded.Block,
                        label = "Exclude from budget",
                        tintError = true,
                        onClick = {
                            store.excludeFromBudget(current.id)
                            refresh()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionHero(item: StoredTransaction) {
    val direction = runCatching {
        MoneyDirection.valueOf(item.direction)
    }.getOrDefault(MoneyDirection.NEUTRAL)

    LandscapeBackdrop(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        showSun = false
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Rounded.AccountCircle, size = 54.dp)
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        item.counterparty ?: pretty(item.type),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        pretty(item.type),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    signedMoney(item.amountMinor, direction, item.type),
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Spacer(Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = .96f)
            ) {
                Row(
                    Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroMetric(
                        "Code",
                        item.transactionCode ?: "—",
                        Icons.Rounded.ReceiptLong,
                        Modifier.weight(1f)
                    )
                    HeroMetric(
                        "Fee",
                        formatMoney(item.feeMinor),
                        Icons.Rounded.Paid,
                        Modifier.weight(1f)
                    )
                    HeroMetric(
                        "Transaction date",
                        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(item.occurredAtEpochMs)),
                        Icons.Rounded.CalendarMonth,
                        Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMetric(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier
) {
    Column(modifier) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp)
        )
        Text(
            label,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 2)
    }
}

@Composable
private fun ClassificationCard(item: StoredTransaction) {
    val needsReview = item.reviewState == ReviewState.NEEDS_REVIEW.name
    val excluded = item.reviewState == ReviewState.EXCLUDED.name
    val category = runCatching {
        TransactionCategory.valueOf(item.category)
    }.getOrDefault(TransactionCategory.UNCATEGORIZED)

    val tone = when {
        needsReview -> "amber"
        excluded -> "red"
        item.direction == MoneyDirection.INCOME.name -> "green"
        item.direction == MoneyDirection.TRANSFER.name -> "blue"
        else -> "green"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = pesaToneContainer(tone)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                if (needsReview) Icons.Rounded.Tune else Icons.Rounded.CheckCircle,
                size = 46.dp,
                background = MaterialTheme.colorScheme.surface.copy(alpha = .7f),
                tint = pesaToneContent(tone)
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    when {
                        needsReview -> "Needs review"
                        excluded -> "Excluded from budget"
                        else -> category.prettyName()
                    },
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    when {
                        needsReview -> "Choose what this transaction was. PesaTrack will not guess."
                        item.ruleId != null -> "Automatically classified by a saved smart rule."
                        item.reviewState == ReviewState.USER_CLASSIFIED.name -> "Classified by you."
                        else -> "Classified automatically."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.ruleId != null) {
                SmallStatusPill("Smart rule")
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    current: TransactionCategory,
    onSelected: (TransactionCategory) -> Unit
) {
    val categories = listOf(
        TransactionCategory.LUNCH,
        TransactionCategory.SHOPPING,
        TransactionCategory.FAMILY,
        TransactionCategory.TRANSPORT,
        TransactionCategory.RENT,
        TransactionCategory.INTERNET,
        TransactionCategory.ELECTRICITY,
        TransactionCategory.PHONE_AND_DATA,
        TransactionCategory.SAVINGS,
        TransactionCategory.INCOME,
        TransactionCategory.OTHER
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { category ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelected(category) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (current == category) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ) {
                        Text(
                            category.prettyName(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (current == category) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            label,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    tintError: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (tintError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            color = if (tintError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun directionForCategory(
    category: TransactionCategory,
    existingDirection: MoneyDirection
): MoneyDirection =
    when {
        category == TransactionCategory.INCOME -> MoneyDirection.INCOME
        category == TransactionCategory.TRANSFER ||
            category == TransactionCategory.CASH ||
            category == TransactionCategory.SAVINGS -> MoneyDirection.TRANSFER
        existingDirection == MoneyDirection.INCOME -> MoneyDirection.INCOME
        else -> MoneyDirection.EXPENSE
    }

private fun signedMoney(
    minor: Long,
    direction: MoneyDirection,
    type: String
): String {
    val sign = when {
        direction == MoneyDirection.INCOME -> "+"
        type == "REVERSAL" -> "+"
        direction == MoneyDirection.EXPENSE -> "-"
        else -> ""
    }
    return (sign + " " + formatMoney(minor)).trim()
}

private fun formatMoney(minor: Long): String =
    "KES " + NumberFormat.getNumberInstance(Locale.US).format(minor / 100.0)

private fun formatDateTime(epochMs: Long): String =
    SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(epochMs))

private fun pretty(value: String): String =
    value.lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }

private fun TransactionCategory.prettyName(): String =
    name.lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }

private fun canLearnIdentity(item: StoredTransaction): Boolean {
    if (!item.counterpartyPhone.isNullOrBlank()) return true

    val name = item.counterparty?.trim().orEmpty()
    if (name.isBlank()) return false

    val tokenCount = name.split(Regex("""\s+""")).count { it.isNotBlank() }
    val merchantLike = item.type == "PAY_BILL" || item.type == "BUY_GOODS"

    return tokenCount >= 2 || merchantLike
}
