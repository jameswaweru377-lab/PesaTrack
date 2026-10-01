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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
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
    val categoryStore = remember { CategoryStore.get(context) }
    var transaction by remember(transactionId) {
        mutableStateOf(store.getById(transactionId))
    }
    var pendingCategoryId by remember(transactionId) {
        mutableStateOf<String?>(null)
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
        categoryId: String
    ) {
        val existingDirection = runCatching {
            MoneyDirection.valueOf(current.direction)
        }.getOrDefault(MoneyDirection.NEUTRAL)
        val canonicalId = categoryStore.canonicalId(categoryId)
        val direction = categoryStore.directionFor(
            categoryId = canonicalId,
            existingDirection = existingDirection
        )
        store.classifyManually(
            id = current.id,
            categoryId = canonicalId,
            direction = direction,
            markTransfer =
                canonicalId == TransactionCategory.TRANSFER.name
        )
        refresh()
    }

    fun learningRule(
        current: StoredTransaction,
        category: TransactionCategory,
        id: String = "preview"
    ): SmartRule {
        val direction = categoryStore.directionFor(
            categoryId = category.name,
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
            name = current.counterparty?.let {
                "Always classify " + it
            } ?: "Learned rule",
            displayLabel = current.counterparty,
            accountId = current.accountId
        )
    }

    fun applyCategory(categoryId: String) {
        val current = transaction ?: return
        val canonicalId = categoryStore.canonicalId(categoryId)
        val builtIn = runCatching {
            TransactionCategory.valueOf(canonicalId)
        }.getOrNull()

        val teachableState =
            current.reviewState == ReviewState.NEEDS_REVIEW.name ||
                (
                    current.reviewState ==
                        ReviewState.AUTO_CLASSIFIED.name &&
                        current.category ==
                            TransactionCategory.OTHER.name
                )
        val shouldOfferLearning =
            builtIn != null &&
                current.ruleId == null &&
                teachableState &&
                builtIn != TransactionCategory.UNCATEGORIZED &&
                canLearnIdentity(current)

        if (shouldOfferLearning) {
            val preview = learningRule(current, builtIn)
            pendingMatchCount = store.matchingPendingCount(
                rule = preview,
                excludingId = current.id
            )
            pendingCategoryId = canonicalId
        } else {
            classifyOnly(current, canonicalId)
        }
    }

    val current = transaction
    val linkedRefundOriginal =
        current
            ?.takeIf {
                it.relationType ==
                    TransactionStore.RELATION_REFUND
            }
            ?.relatedTransactionId
            ?.let(store::getById)

    if (current != null && pendingCategoryId != null) {
        val categoryId = pendingCategoryId!!
        val category = runCatching {
            TransactionCategory.valueOf(categoryId)
        }.getOrNull()
        if (category != null) {
        AlertDialog(
            onDismissRequest = { pendingCategoryId = null },
            title = {
                Text("Remember this for next time?")
            },
            text = {
                Text(
                    buildString {
                        append("Use “")
                        append(current.counterparty ?: current.counterpartyPhone ?: "this identity")
                        append("” as ")
                        append(categoryStore.labelFor(categoryId))
                        append(".")
                        if (pendingMatchCount > 0) {
                            append(" This can also classify ")
                            append(pendingMatchCount)
                            append(" matching transaction")
                            if (pendingMatchCount != 1) append("s")
                            append(" already in your history.")
                        }
                    }
                )
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        classifyOnly(current, categoryId)
                        pendingCategoryId = null
                    }
                ) {
                    Text("Only this one")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val direction = categoryStore.directionFor(
                            categoryId = category.name,
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
                        pendingCategoryId = null
                        refresh()
                    }
                ) {
                    Text("Remember")
                }
            }
        )
        }
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
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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

        if (current.fulizaFinancedMinor > 0L) {
            item {
                FulizaFundingCard(current)
            }
        }

        if (
            current.relationType ==
            TransactionStore.RELATION_REFUND
        ) {
            item {
                RefundLinkCard(
                    item = current,
                    original = linkedRefundOriginal
                )
            }
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

        if (
            current.relationType !=
            TransactionStore.RELATION_REFUND
        ) {
            item {
                SectionHeader("Choose a category")
                Spacer(Modifier.height(8.dp))
                CategoryGrid(
                    currentCategoryId =
                        categoryStore.canonicalId(current.category),
                    onSelected = ::applyCategory
                )
            }
        }

        item {
            SectionHeader("Transaction information")
            Spacer(Modifier.height(8.dp))
            PesaCard {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    DetailRow(Icons.Rounded.AccountCircle, "Name", current.counterparty ?: "Not identified")
                    current.evidenceLabel?.let { evidenceLabel ->
                        val entityIntelligence =
                            current.evidenceSource
                                ?.startsWith(
                                    "entity_intelligence:"
                                ) == true
                        DetailRow(
                            Icons.Rounded.Tune,
                            when {
                                entityIntelligence ->
                                    "Recognized entity"
                                current.counterparty ==
                                    evidenceLabel ->
                                    "Identified from nearby message"
                                else ->
                                    "Possible match"
                            },
                            evidenceLabel
                        )
                        DetailRow(
                            Icons.Rounded.Tune,
                            "Evidence",
                            buildString {
                                if (entityIntelligence) {
                                    append(
                                        "PesaTrack entity intelligence"
                                    )
                                } else {
                                    append(
                                        current.evidenceSource
                                            ?: "Nearby financial message"
                                    )
                                }
                                current.evidenceConfidence?.let {
                                    append(" • ")
                                    append(
                                        it.lowercase()
                                            .replace('_', ' ')
                                    )
                                }
                                current.evidenceTimeDeltaMs?.let {
                                    append(" • ")
                                    append(it / 1000)
                                    append("s apart")
                                }
                            }
                        )
                    }
                    DetailRow(Icons.AutoMirrored.Rounded.ReceiptLong, "Type", pretty(current.type))
                    DetailRow(Icons.AutoMirrored.Rounded.ReceiptLong, "M-Pesa code", current.transactionCode ?: "Not provided")
                    DetailRow(Icons.Rounded.CalendarMonth, "Transaction date", formatDateTime(current.occurredAtEpochMs))
                    if (current.receivedAtEpochMs != current.occurredAtEpochMs) {
                        DetailRow(Icons.Rounded.CalendarMonth, "Message received", formatDateTime(current.receivedAtEpochMs))
                    }
                    DetailRow(Icons.Rounded.Paid, "M-Pesa fee", formatMoney(current.feeMinor))
                    if (current.fulizaAccessFeeMinor > 0L) {
                        DetailRow(
                            Icons.Rounded.Paid,
                            "Fuliza access fee",
                            formatMoney(current.fulizaAccessFeeMinor)
                        )
                    }
                    current.fulizaOutstandingMinor?.let { outstanding ->
                        DetailRow(
                            Icons.Rounded.Paid,
                            "Fuliza outstanding after",
                            formatMoney(outstanding)
                        )
                    }
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
                    if (
                        current.counterparty.isNullOrBlank() &&
                        !current.evidenceLabel.isNullOrBlank()
                    ) {
                        ActionRow(
                            icon = Icons.Rounded.CheckCircle,
                            label = "Use suggested identity",
                            onClick = {
                                store.acceptEvidenceIdentity(current.id)
                                refresh()
                            }
                        )
                    }
                    if (
                        current.relationType ==
                        TransactionStore.RELATION_REFUND
                    ) {
                        ActionRow(
                            icon = Icons.Rounded.SwapHoriz,
                            label = "This was not a refund",
                            onClick = {
                                store.unlinkRefund(current.id)
                                refresh()
                            }
                        )
                    }
                    if (
                        current.relationType !=
                        TransactionStore.RELATION_REFUND
                    ) {
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
                    }
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
                    style = MaterialTheme.typography.headlineMedium,
                    color = when {
                        direction == MoneyDirection.INCOME ||
                            item.type == MpesaTransactionType.REVERSAL.name ->
                            MaterialTheme.colorScheme.primary
                        direction == MoneyDirection.EXPENSE ->
                            MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
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
                        Icons.AutoMirrored.Rounded.ReceiptLong,
                        Modifier.weight(1f)
                    )
                    HeroMetric(
                        "Fees",
                        formatMoney(item.feeMinor + item.fulizaAccessFeeMinor),
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
private fun FulizaFundingCard(item: StoredTransaction) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = pesaToneContainer("amber")
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(
                    Icons.Rounded.Paid,
                    size = 44.dp,
                    background = MaterialTheme.colorScheme.surface.copy(alpha = .7f),
                    tint = pesaToneContent("amber")
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 11.dp)
                ) {
                    Text(
                        "Paid partly with Fuliza",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "The payment is counted once. Fuliza is financing, not another purchase.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroMetric(
                    "Financed",
                    formatMoney(item.fulizaFinancedMinor),
                    Icons.Rounded.Paid,
                    Modifier.weight(1f)
                )
                HeroMetric(
                    "Access fee",
                    formatMoney(item.fulizaAccessFeeMinor),
                    Icons.AutoMirrored.Rounded.ReceiptLong,
                    Modifier.weight(1f)
                )
                HeroMetric(
                    "Outstanding",
                    item.fulizaOutstandingMinor?.let(::formatMoney) ?: "—",
                    Icons.Rounded.AccountCircle,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RefundLinkCard(
    item: StoredTransaction,
    original: StoredTransaction?
) {
    val context = LocalContext.current
    val categoryStore = remember {
        CategoryStore.get(context)
    }
    val categoryLabel =
        categoryStore.labelFor(item.category)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = pesaToneContainer("green")
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBubble(
                    Icons.Rounded.SwapHoriz,
                    size = 44.dp,
                    background =
                        MaterialTheme.colorScheme.surface
                            .copy(alpha = .7f),
                    tint = pesaToneContent("green")
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        "Refund linked to purchase",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (
                            original != null &&
                            original.reviewState !=
                                ReviewState.NEEDS_REVIEW.name &&
                            original.reviewState !=
                                ReviewState.EXCLUDED.name
                        ) {
                            "Not counted as income. It reduces " +
                                categoryLabel +
                                " spending."
                        } else {
                            "Not counted as income. Review the original purchase before this refund changes trusted spending totals."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            original?.let {
                Text(
                    buildString {
                        append("Original: ")
                        append(
                            it.counterparty
                                ?: "transaction"
                        )
                        append(" • ")
                        append(formatMoney(it.amountMinor))
                        append(" • ")
                        append(formatDateTime(it.occurredAtEpochMs))
                    },
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ClassificationCard(item: StoredTransaction) {
    val context = LocalContext.current
    val categoryStore = remember { CategoryStore.get(context) }
    val needsReview = item.reviewState == ReviewState.NEEDS_REVIEW.name
    val excluded = item.reviewState == ReviewState.EXCLUDED.name
    val categoryLabel = categoryStore.labelFor(item.category)

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
                if (needsReview) {
                    Icons.Rounded.Tune
                } else {
                    Icons.Rounded.CheckCircle
                },
                size = 46.dp,
                background =
                    MaterialTheme.colorScheme.surface.copy(alpha = .7f),
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
                        else -> categoryLabel
                    },
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    when {
                        needsReview ->
                            "Choose what this transaction was. PesaTrack will not guess."
                        item.ruleId != null ->
                            "Automatically classified by a saved smart rule."
                        item.reviewState ==
                            ReviewState.USER_CLASSIFIED.name ->
                            "Classified by you."
                        item.evidenceSource
                            ?.startsWith(
                                "entity_intelligence:"
                            ) == true ->
                            "Auto-reviewed from a high-confidence entity match. You can change this category at any time."
                        else ->
                            "Classified automatically."
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
    currentCategoryId: String,
    onSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val categoryStore = remember { CategoryStore.get(context) }
    val ledgerState by com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
        .state
        .collectAsState()
    val categories = remember(ledgerState.revision) {
        categoryStore.activityCategories()
    }
    var showAddCategory by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }

    if (showAddCategory) {
        AlertDialog(
            onDismissRequest = {
                showAddCategory = false
                customName = ""
            },
            title = { Text("Add category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "It will also be available when you create a budget.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Category name") },
                        placeholder = { Text("Example: Pets") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddCategory = false
                        customName = ""
                    }
                ) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        runCatching {
                            categoryStore.addCustom(customName)
                        }.onSuccess {
                            showAddCategory = false
                            customName = ""
                            onSelected(it.id)
                        }
                    }
                ) {
                    Text("Add category")
                }
            }
        )
    }

    val compactCategories = categories.filter {
        it.label.length <= 11
    }
    val wideCategories = categories.filter {
        it.label.length > 11
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryGridRows(
            categories = compactCategories,
            columns = 3,
            currentCategoryId = currentCategoryId,
            onSelected = onSelected
        )

        if (wideCategories.isNotEmpty()) {
            CategoryGridRows(
                categories = wideCategories,
                columns = 2,
                currentCategoryId = currentCategoryId,
                onSelected = onSelected
            )
        }

        OutlinedButton(
            onClick = { showAddCategory = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Text(
                "Add category",
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun CategoryGridRows(
    categories: List<com.kedevjimmy.pesatrack.data.local.PesaCategory>,
    columns: Int,
    currentCategoryId: String,
    onSelected: (String) -> Unit
) {
    categories.chunked(columns).forEach { rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rowItems.forEach { category ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelected(category.id) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (
                        currentCategoryId == category.id
                    ) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ) {
                    Text(
                        category.label,
                        modifier = Modifier.padding(
                            horizontal = if (columns == 3) {
                                10.dp
                            } else {
                                12.dp
                            },
                            vertical = 12.dp
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        color = if (
                            currentCategoryId == category.id
                        ) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            repeat(columns - rowItems.size) {
                Spacer(Modifier.weight(1f))
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
            category == TransactionCategory.SAVINGS ||
            category == TransactionCategory.INVESTMENT ->
            MoneyDirection.TRANSFER
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
