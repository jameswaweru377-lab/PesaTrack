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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kedevjimmy.pesatrack.data.local.RememberCategoryRepository
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.PersonalLearningStore
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.identity.IdentityAliasType
import com.kedevjimmy.pesatrack.domain.identity.IdentityEvidenceSource
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningAction
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningCorrectionAction
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningPolicy
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.rules.SmartRuleDecisionExplanation
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
    val learningRepository = remember { RememberCategoryRepository(context) }
    val scope = rememberCoroutineScope()
    var learningBusy by remember(transactionId) { mutableStateOf(false) }
    var learningError by remember(transactionId) { mutableStateOf<String?>(null) }
    val ruleStore = remember { RuleStore.get(context) }
    val categoryStore = remember { CategoryStore.get(context) }
    val personalLearningStore =
        remember {
            PersonalLearningStore.get(context)
        }
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
    var learningSuggestion by remember(transactionId) {
        mutableStateOf(
            transaction?.let(
                personalLearningStore::
                    suggestionFor
            )
        )
    }
    var learningExplanation by remember(transactionId) {
        mutableStateOf(
            transaction?.let(
                personalLearningStore::
                    explanationFor
            )
        )
    }
    var learningPreference by remember(transactionId) {
        mutableStateOf(
            transaction?.let(
                personalLearningStore::
                    preferenceFor
            )
        )
    }

    fun refresh() {
        val updated =
            store.getById(transactionId)
        transaction = updated
        learningSuggestion =
            updated?.let(
                personalLearningStore::
                    suggestionFor
            )
        learningExplanation =
            updated?.let(
                personalLearningStore::
                    explanationFor
            )
        learningPreference =
            updated?.let(
                personalLearningStore::
                    preferenceFor
            )
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
        val hadLearningApplication =
            personalLearningStore
                .applicationFor(
                    current.id
                ) != null
        val correctionAction =
            if (hadLearningApplication) {
                PersonalLearningPolicy
                    .correctionAction(
                        categoryId =
                            canonicalId,
                        direction =
                            direction
                    )
            } else {
                null
            }

        if (hadLearningApplication) {
            personalLearningStore
                .correctLearnedPreference(
                    transaction =
                        current,
                    categoryId =
                        canonicalId,
                    direction =
                        direction
                )
        }

        store.classifyManually(
            id = current.id,
            categoryId = canonicalId,
            direction = direction,
            markTransfer =
                canonicalId == TransactionCategory.TRANSFER.name
        )

        if (hadLearningApplication) {
            learnedMessage =
                when (correctionAction) {
                    PersonalLearningCorrectionAction
                        .UPDATE_PREFERENCE ->
                        "Correction saved. PesaTrack updated what it learned for this identity."

                    PersonalLearningCorrectionAction
                        .FORGET_PREFERENCE ->
                        "Correction saved. The old learned category was removed for this identity."

                    null -> null
                }
        }
        refresh()
    }

    fun applyCategory(categoryId: String) {
        val current = transaction ?: return
        if (learningBusy) return
        val canonicalId = categoryStore.canonicalId(categoryId)
        val direction = categoryStore.directionFor(canonicalId, MoneyDirection.EXPENSE)
        learningBusy = true
        learningError = null
        scope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    if (current.ruleId == null && current.relationType == null &&
                        current.direction == MoneyDirection.EXPENSE.name &&
                        current.category in setOf(TransactionCategory.OTHER.name, TransactionCategory.UNCATEGORIZED.name) &&
                        current.reviewState in setOf(ReviewState.NEEDS_REVIEW.name, ReviewState.AUTO_CLASSIFIED.name) &&
                        PersonalLearningPolicy.isLearnableCategory(canonicalId, direction) && canLearnIdentity(current)) {
                        learningRepository.matchingCount(current)
                    } else 0
                }
                if (count >= 2) {
                    pendingMatchCount = count
                    pendingCategoryId = canonicalId
                } else {
                    withContext(Dispatchers.IO) { classifyOnly(current, canonicalId) }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                learningError = failure.message ?: "Could not update this category. Please try again."
            } finally { learningBusy = false }
        }
    }

    val current = transaction
    val appliedRule =
        current?.ruleId
            ?.let(
                ruleStore::getById
            )
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
        AlertDialog(
            onDismissRequest = { if (!learningBusy) pendingCategoryId = null },
            title = { Text("Remember this category?") },
            text = { Column {
                Text("Use “${current.counterparty ?: current.counterpartyPhone}” as ${categoryStore.labelFor(categoryId)}? " +
                    "This updates $pendingMatchCount other matching transactions across all imported months, " +
                    "and remembers future matches. Your other manual categories stay unchanged.")
                learningError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            dismissButton = { TextButton(enabled = !learningBusy, onClick = {
                learningBusy = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { classifyOnly(current, categoryId) }
                        pendingCategoryId = null
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        learningError = failure.message ?: "Could not save. Please try again."
                    } finally { learningBusy = false }
                }
            }) { Text("Only this one") } },
            confirmButton = { Button(enabled = !learningBusy, onClick = {
                learningBusy = true
                learningError = null
                scope.launch {
                    try {
                        val changed = withContext(Dispatchers.IO) { learningRepository.remember(current.id, categoryId) }
                        learnedMessage = "Remembered • $changed other transactions updated across imported months."
                        pendingCategoryId = null
                        refresh()
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        learningError = failure.message ?: "Could not save. Please try again."
                    } finally { learningBusy = false }
                }
            }) { Text(if (learningBusy) "Saving…" else "Remember") } }
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
            if (learningBusy) Text("Updating category…")
            if (pendingCategoryId == null) learningError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
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
            ClassificationCard(
                item = current,
                appliedRule = appliedRule,
                onDisableRule = { rule ->
                    if (
                        ruleStore.setEnabled(
                            id = rule.id,
                            enabled = false
                        )
                    ) {
                        learnedMessage =
                            "Smart rule disabled for future transactions. This transaction keeps its current classification."
                    }
                }
            )
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

        learningExplanation
            ?.let { explanation ->
                item {
                    PesaCard {
                        Column(
                            Modifier.padding(
                                16.dp
                            ),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    10.dp
                                )
                        ) {
                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Text(
                                    "Learned by PesaTrack",
                                    modifier =
                                        Modifier.weight(1f),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium
                                )
                                SmallStatusPill(
                                    if (
                                        learningPreference !=
                                        null
                                    ) {
                                        "Auto-applied"
                                    } else {
                                        "Learning off"
                                    },
                                    if (
                                        learningPreference !=
                                        null
                                    ) {
                                        "green"
                                    } else {
                                        "amber"
                                    }
                                )
                            }
                            Text(
                                buildString {
                                    append(
                                        categoryStore
                                            .labelFor(
                                                explanation
                                                    .application
                                                    .categoryId
                                            )
                                    )
                                    append(
                                        " was applied automatically because "
                                    )
                                    append(
                                        explanation
                                            .identityLabel
                                    )
                                    append(
                                        " matched a learned identity."
                                    )
                                },
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                            Text(
                                buildString {
                                    append(
                                        learningMatchReason(
                                            explanation
                                                .matchedAliasType,
                                            explanation
                                                .matchedAliasSource
                                        )
                                    )
                                    append(" • ")
                                    append(
                                        explanation
                                            .application
                                            .confidence
                                            .name
                                            .lowercase()
                                            .replaceFirstChar {
                                                it.uppercase()
                                            }
                                    )
                                    append(" confidence")
                                    append(" • ")
                                    append(
                                        explanation
                                            .application
                                            .confirmationCount
                                    )
                                    append(
                                        if (
                                            explanation
                                                .application
                                                .confirmationCount ==
                                            1
                                        ) {
                                            " confirmation"
                                        } else {
                                            " confirmations"
                                        }
                                    )
                                },
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    if (
                                        personalLearningStore
                                            .undoAndForget(
                                                current
                                            )
                                    ) {
                                        learnedMessage =
                                            "Learned classification undone. This identity will not be auto-classified from that preference again."
                                    }
                                    refresh()
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Undo & forget"
                                )
                            }
                            if (
                                learningPreference != null
                            ) {
                                TextButton(
                                    onClick = {
                                        if (
                                            personalLearningStore
                                                .forgetPreferenceFor(
                                                    current
                                                )
                                        ) {
                                            learnedMessage =
                                                "Future learning for this identity was removed. This transaction stays unchanged."
                                        }
                                        refresh()
                                    },
                                    modifier =
                                        Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Keep this, forget future learning"
                                    )
                                }
                            }
                        }
                    }
                }
            }

        learningSuggestion
            ?.takeIf {
                it.action ==
                    PersonalLearningAction.SUGGEST
            }
            ?.let { suggestion ->
                item {
                    PesaCard {
                        Column(
                            Modifier.padding(
                                16.dp
                            )
                        ) {
                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Text(
                                    "PesaTrack suggestion",
                                    modifier =
                                        Modifier.weight(1f),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium
                                )
                                SmallStatusPill(
                                    "Needs confirmation",
                                    "amber"
                                )
                            }
                            Text(
                                "You previously taught " +
                                    suggestion.identityLabel +
                                    " as " +
                                    categoryStore.labelFor(
                                        suggestion.categoryId
                                    ) +
                                    ". " +
                                    learningMatchReason(
                                        suggestion.matchedAliasType,
                                        suggestion.matchedAliasSource
                                    ) +
                                    " is only medium confidence, so PesaTrack is asking before changing the transaction.",
                                modifier =
                                    Modifier.padding(
                                        top = 6.dp
                                    ),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    classifyOnly(
                                        current,
                                        suggestion.categoryId
                                    )
                                    learningSuggestion =
                                        null
                                },
                                modifier =
                                    Modifier.padding(
                                        top = 12.dp
                                    )
                            ) {
                                Text(
                                    "Use " +
                                        categoryStore.labelFor(
                                            suggestion.categoryId
                                        )
                                )
                            }
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
    val currentLocale =
        androidx.compose.ui.platform
            .LocalConfiguration
            .current
            .locales[0]
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
                        SimpleDateFormat("d MMM", currentLocale).format(Date(item.occurredAtEpochMs)),
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
private fun ClassificationCard(
    item: StoredTransaction,
    appliedRule: SmartRule?,
    onDisableRule: (SmartRule) -> Unit
) {
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
        Column {
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
                        item.ruleId != null &&
                            appliedRule != null ->
                            SmartRuleDecisionExplanation
                                .describe(
                                    appliedRule
                                )
                        item.ruleId != null ->
                            "Automatically classified by a saved smart rule. The original rule record is no longer available."
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
                SmallStatusPill(
                    if (
                        appliedRule?.enabled ==
                        false
                    ) {
                        "Rule disabled"
                    } else {
                        "Smart rule"
                    }
                )
            }
        }

            if (
                appliedRule != null &&
                appliedRule.enabled
            ) {
                TextButton(
                    onClick = {
                        onDisableRule(
                            appliedRule
                        )
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                ) {
                    Text(
                        "Disable rule for future"
                    )
                }
            } else if (
                item.ruleId != null &&
                appliedRule?.enabled ==
                false
            ) {
                Text(
                    "This saved rule is disabled. You can still change this transaction manually.",
                    modifier =
                        Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 14.dp
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
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

private fun learningMatchReason(
    aliasType: IdentityAliasType?,
    source: IdentityEvidenceSource?
): String {
    val match =
        when (aliasType) {
            IdentityAliasType.PHONE ->
                "A matching phone number"

            IdentityAliasType.NAME ->
                "A matching name"

            IdentityAliasType.ACCOUNT_ID,
            IdentityAliasType.ACCOUNT_IDENTIFIER ->
                "A matching account identifier"

            IdentityAliasType.PROVIDER_KEY ->
                "A matching provider identity"

            IdentityAliasType.SMS_SENDER ->
                "A matching message sender"

            null ->
                "The stable identity"
        }

    val evidence =
        when (source) {
            IdentityEvidenceSource
                .USER_CONFIRMATION ->
                "you confirmed"

            IdentityEvidenceSource
                .LOCAL_ENTITY_CATALOG ->
                "PesaTrack already recognizes"

            IdentityEvidenceSource
                .REMOTE_ENRICHMENT ->
                "entity enrichment recognized"

            IdentityEvidenceSource
                .ACCOUNT_STORE ->
                "your account setup confirms"

            IdentityEvidenceSource
                .FINANCIAL_ENTITY ->
                "financial account evidence confirms"

            IdentityEvidenceSource
                .TRANSACTION_COUNTERPARTY ->
                "transaction history matched"

            IdentityEvidenceSource
                .MESSAGE_SOURCE ->
                "message-source evidence matched"

            null ->
                "PesaTrack matched"
        }

    return "$match that $evidence"
}

private fun canLearnIdentity(item: StoredTransaction): Boolean {
    if (!item.counterpartyPhone.isNullOrBlank()) return true

    val name = item.counterparty?.trim().orEmpty()
    if (name.isBlank()) return false

    return name.length >= 2

}

