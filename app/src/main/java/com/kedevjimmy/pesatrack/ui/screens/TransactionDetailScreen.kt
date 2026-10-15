package com.kedevjimmy.pesatrack.ui.screens

import android.content.Intent
import com.kedevjimmy.pesatrack.ingestion.TransactionMessageEvidenceReader
import com.kedevjimmy.pesatrack.ingestion.TransactionMessageEvidenceResult
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.EvidenceJournalStore
import com.kedevjimmy.pesatrack.data.local.TransactionAnnotation
import com.kedevjimmy.pesatrack.data.local.TransactionAnnotationStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kedevjimmy.pesatrack.data.local.RememberCategoryRepository
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
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
    onBack: () -> Unit,
    onTransactionClick: (Long) -> Unit = {},
    onAccountClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val store = remember { TransactionStore.get(context) }
    val learningRepository = remember { RememberCategoryRepository(context) }
    val scope = rememberCoroutineScope()
    var learningBusy by remember(transactionId) { mutableStateOf(false) }
    var learningError by remember(transactionId) { mutableStateOf<String?>(null) }
    val ruleStore = remember { RuleStore.get(context) }
    val categoryStore = remember { CategoryStore.get(context) }
    val ledgerUpdateState by LedgerUpdateBus.state.collectAsState()
    val annotationStore =
        remember {
            TransactionAnnotationStore.get(context)
        }
    val accountStore =
        remember {
            AccountStore.get(context)
        }
    var annotation by remember(transactionId) {
        mutableStateOf(
            annotationStore.get(transactionId)
        )
    }
    var showAnnotationDialog by remember(transactionId) {
        mutableStateOf(false)
    }
    var showMerchantHistory by remember(transactionId) {
        mutableStateOf(false)
    }
    val receiptPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                }
                annotationStore.saveReceipt(
                    transactionId,
                    uri.toString()
                )
                annotation =
                    annotationStore.get(
                        transactionId
                    )
            }
        }
    val personalLearningStore =
        remember {
            PersonalLearningStore.get(context)
        }
    var debtCategoryOpen by remember(transactionId) { mutableStateOf(false) }
    var receivablePeople by remember { mutableStateOf<List<com.kedevjimmy.pesatrack.data.local.ReceivablePerson>>(emptyList()) }
    var receivableLinks by remember { mutableStateOf<Map<Long,ReceivableActivityLink>>(emptyMap()) }
    LaunchedEffect(transactionId,ledgerUpdateState.revision) {
        try {
            val data = withContext(Dispatchers.IO) {
                com.kedevjimmy.pesatrack.data.local.PersonalReceivableStore(context).people() to loadReceivableActivityLinks(context)
            }
            receivablePeople = data.first; receivableLinks = data.second
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { receivablePeople = emptyList(); receivableLinks = emptyMap() }
    }
    var transaction by remember(transactionId) {
        mutableStateOf(store.getById(transactionId))
    }
    val transferReviewStore = remember { com.kedevjimmy.pesatrack.data.local.OwnedTransferReviewStore(context) }
    var confirmedTransfer by remember(transactionId) { mutableStateOf(transferReviewStore.isConfirmedPair(transactionId)) }
    var pendingCategoryId by remember(transactionId) {
        mutableStateOf<String?>(null)
    }
    var pendingMatchCount by remember(transactionId) {
        mutableStateOf(0)
    }
    var pendingStatementMatchCount by remember(transactionId) {
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
        confirmedTransfer = transferReviewStore.isConfirmedPair(transactionId)
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

    LaunchedEffect(
        transactionId,
        ledgerUpdateState.revision
    ) {
        refresh()
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
                        learningRepository.matchingPreview(current, canonicalId)
                    } else RememberCategoryRepository.MatchingPreview(0, 0)
                }
                if (PersonalLearningPolicy.isLearnableCategory(canonicalId, direction) &&
                    canLearnIdentity(current) && current.ruleId == null &&
                    current.relationType == null &&
                    current.direction == MoneyDirection.EXPENSE.name &&
                    current.category in setOf(TransactionCategory.OTHER.name, TransactionCategory.UNCATEGORIZED.name) &&
                    current.reviewState in setOf(ReviewState.NEEDS_REVIEW.name, ReviewState.AUTO_CLASSIFIED.name)) {
                    // Explicit choice also matters when zero history matches:
                    // it teaches future PDFs rather than silently changing one row.
                    pendingMatchCount = count.activity
                    pendingStatementMatchCount = count.statements
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

    var showCategoryPicker by remember(transactionId) { mutableStateOf(false) }
    var pendingCorrection by remember(transactionId) {
        mutableStateOf<TransactionCorrectionAction?>(null)
    }
    var correctionBusy by remember(transactionId) { mutableStateOf(false) }
    var correctionError by remember(transactionId) { mutableStateOf<String?>(null) }
    var correctionResult by remember(transactionId) { mutableStateOf<String?>(null) }
    var messageExpanded by remember(transactionId) { mutableStateOf(true) }
    var messageEvidence by remember(transactionId) { mutableStateOf<TransactionMessageEvidenceResult?>(null) }
    var evidenceRefresh by remember(transactionId) { mutableStateOf(0) }
    val current = transaction
    LaunchedEffect(current?.id, current?.transactionCode, current?.amountMinor, messageExpanded, evidenceRefresh) {
        if (current != null && messageExpanded) {
            messageEvidence = null
            messageEvidence = withContext(Dispatchers.IO) {
                TransactionMessageEvidenceReader(context.applicationContext).read(current)
            }
        }
    }
    if (current != null && pendingCorrection != null) {
        val selectedAction = pendingCorrection!!
        val prompt = transactionCorrectionPrompt(selectedAction)
        AlertDialog(
            onDismissRequest = { if (!correctionBusy) pendingCorrection = null },
            title = { Text(prompt.title) },
            text = {
                Column {
                    Text(prompt.explanation)
                    Text(
                        "Current entry: " + formatTransactionDetailMoney(current.amountMinor),
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                    correctionError?.let {
                        Text(
                            it,
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !correctionBusy,
                    onClick = { pendingCorrection = null; correctionError = null }
                ) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    enabled = !correctionBusy,
                    onClick = {
                        correctionBusy = true
                        correctionError = null
                        scope.launch {
                            try {
                                val applied = withContext(Dispatchers.IO) {
                                    val latest = store.getById(current.id)
                                    when {
                                        latest == null -> false
                                        selectedAction == TransactionCorrectionAction.ACCEPT_IDENTITY ->
                                            latest.counterparty.isNullOrBlank() &&
                                                !latest.evidenceLabel.isNullOrBlank() &&
                                                store.acceptEvidenceIdentity(latest.id)
                                        selectedAction == TransactionCorrectionAction.REMOVE_REFUND_LINK ->
                                            latest.relationType == TransactionStore.RELATION_REFUND &&
                                                store.unlinkRefund(latest.id)
                                        selectedAction == TransactionCorrectionAction.MARK_OWN_TRANSFER ->
                                            if (latest.relationType != null ||
                                                latest.direction == MoneyDirection.TRANSFER.name ||
                                                latest.reviewState == ReviewState.EXCLUDED.name
                                            ) false else {
                                                store.classifyManually(
                                                    id = latest.id,
                                                    category = TransactionCategory.TRANSFER,
                                                    direction = MoneyDirection.TRANSFER,
                                                    markTransfer = true
                                                )
                                                true
                                            }
                                        selectedAction == TransactionCorrectionAction.EXCLUDE_ENTRY ->
                                            if (latest.reviewState == ReviewState.EXCLUDED.name) false
                                            else {
                                                store.excludeFromBudget(latest.id)
                                                LedgerUpdateBus.notifyChanged()
                                                true
                                            }
                                        else -> false
                                    }
                                }
                                if (applied) {
                                    pendingCorrection = null
                                    correctionResult = "Correction saved. Review the updated transaction and totals."
                                    refresh()
                                } else {
                                    correctionError = "This correction no longer applies. Refresh and review the current entry."
                                }
                            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                correctionError = "Couldn't save this correction. Your entry is unchanged."
                            } finally {
                                correctionBusy = false
                            }
                        }
                    }
                ) { Text(if (correctionBusy) "Saving…" else prompt.confirmLabel) }
            }
        )
    }

    if (showCategoryPicker && current != null) {
        AlertDialog(
            onDismissRequest = { showCategoryPicker = false },
            title = { Text("Choose a category") },
            text = {
                Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                if (current.direction in setOf("EXPENSE","INCOME") && receivableLinks[current.id] == null) {
                    OutlinedButton(onClick = { showCategoryPicker = false; debtCategoryOpen = true },
                        modifier = Modifier.fillMaxWidth()) {
                        Text(if (current.direction == "EXPENSE") "Debts → Owed to me" else "Debts → Repayment received")
                    }
                }
                CategoryGrid(
                    currentCategoryId = categoryStore.canonicalId(current.category),
                    onSelected = { category -> showCategoryPicker = false; applyCategory(category) }
                )
                }
            },
            confirmButton = { TextButton(onClick = { showCategoryPicker = false }) { Text("Close") } }
        )
    }
    if (debtCategoryOpen && current != null) ReceivableEntryDialog(
        people = receivablePeople,initialPersonId = null,
        kind = if (current.direction == "EXPENSE") "LENT" else "REPAID",
        linkedTransaction = current,
        onDismiss = { debtCategoryOpen = false },
        onSaved = { debtCategoryOpen = false; refresh() })
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
                Text("Remember “${current.counterparty ?: current.counterpartyPhone}” " +
                    "as ${categoryStore.labelFor(categoryId)}?")
                Text("Activity: $pendingMatchCount other eligible transactions.")
                Text("Statements: $pendingStatementMatchCount eligible transactions " +
                    "in saved reports.")
                Text("Choosing Remember saves one category preference across Activity " +
                    "and Statements. Historical and newly imported PDFs use it " +
                    "automatically when the identity matches safely. " +
                    "The original PDF records and manual exceptions remain unchanged.")
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
                        learnedMessage = "Remembered in Activity and Statements • " +
                            "$changed other Activity entries updated. " +
                            "Up to $pendingStatementMatchCount eligible Statement entries will " +
                            "show this category. Original PDF transactions stay independent."
                        pendingCategoryId = null
                        refresh()
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        learningError = failure.message ?: "Could not save. Please try again."
                    } finally { learningBusy = false }
                }
            }) { Text(if (learningBusy) "Saving…" else "Remember in both") } }
        )
    }

    if (current != null && showAnnotationDialog) {
        TransactionAnnotationDialog(
            transaction = current,
            annotation = annotation,
            onDismiss = {
                showAnnotationDialog = false
            },
            onSaveNote = { note ->
                annotationStore.saveNote(
                    current.id,
                    note
                )
                annotation =
                    annotationStore.get(
                        current.id
                    )
                showAnnotationDialog =
                    false
            },
            onAttachReceipt = {
                receiptPicker.launch(
                    arrayOf(
                        "image/*",
                        "application/pdf"
                    )
                )
            },
            onRemoveReceipt = {
                annotationStore.saveReceipt(
                    current.id,
                    null
                )
                annotation =
                    annotationStore.get(
                        current.id
                    )
            }
        )
    }

    if (current != null && showMerchantHistory) {
        MerchantHistoryDialog(
            merchant =
                current.counterparty
                    ?: pretty(current.type),
            transactions =
                store.recent(250)
                    .filter {
                        it.id != current.id &&
                            sameMerchant(
                                current,
                                it
                            )
                    }
                    .take(12),
            categoryStore = categoryStore,
            onDismiss = {
                showMerchantHistory = false
            },
            onTransactionClick = {
                showMerchantHistory = false
                onTransactionClick(it)
            }
        )
    }

    LazyColumn(
        modifier = Modifier.testTag("transaction-details-list")
            .padding(scaffoldPadding)
            .background(
                MaterialTheme
                    .colorScheme
                    .background
            ),
        contentPadding =
            PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(
                16.dp
            )
    ) {
        item {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack
                ) {
                    Icon(
                        Icons.AutoMirrored
                            .Rounded
                            .ArrowBack,
                        contentDescription =
                            "Back"
                    )
                }
                Text(
                    "Transaction details",
                    modifier =
                        Modifier.weight(1f),
                    style =
                        MaterialTheme
                            .typography
                            .headlineMedium,
                    fontWeight =
                        FontWeight.Bold
                )
                IconButton(
                    onClick = { refresh(); evidenceRefresh++ }
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription =
                            "Refresh transaction"
                    )
                }
            }
        }

        if (current == null) {
            item {
                PesaCard {
                    Text(
                        "This transaction is no longer available.",
                        modifier =
                            Modifier.padding(
                                20.dp
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }
            return@LazyColumn
        }

        item {
            TransactionDetailSummary(current, categoryVisual(categoryStore.canonicalId(current.category)).icon)
        }
        if (confirmedTransfer) item { OwnedTransferUndoAction(current.id, onChanged = { refresh() }) }
        if (!confirmedTransfer) item { SplitPaymentAction(current, onChanged = { refresh() }) }
        if (!confirmedTransfer) item { PersonalReceivableTransactionAction(current, onChanged = { refresh() }) }
        item(key = "transaction-provenance") {
            val linkedSources = remember(current, evidenceRefresh) {
                EvidenceJournalStore.sourcesFor(context, current.id)
            }
            if (linkedSources.isNotEmpty()) {
                SectionHeader("Linked evidence")
                PesaCard {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        linkedSources.forEach { source ->
                            val label = when (source.sourceKind) {
                                "STATEMENT" -> "Statement"
                                "MANUAL" -> "Manual entry"
                                "NOTIFICATION" -> "Notification"
                                else -> "SMS"
                            }
                            Text(
                                if (source.needsReview) label + " • Recheck after editing this transaction"
                                else if (source.reason == "REFERENCE") label + " • Matched to an existing transaction" 
                                else label + " • Source recorded for this transaction",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        if (current.reviewState == ReviewState.USER_CLASSIFIED.name &&
            current.direction == MoneyDirection.EXPENSE.name &&
            current.ruleId == null && current.relationType == null &&
            !confirmedTransfer && canLearnIdentity(current) &&
            PersonalLearningPolicy.isLearnableCategory(
                categoryStore.canonicalId(current.category), MoneyDirection.EXPENSE
            ) && personalLearningStore.preferenceFor(current) == null) {
            item {
                PesaCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Remember in Activity + Statements",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text("This Activity transaction is already categorized. " +
                            "You can teach the category to eligible historical and future " +
                            "Statements without combining their financial records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(enabled = !learningBusy, onClick = {
                            val original = current
                            learningBusy = true
                            scope.launch {
                                try {
                                    val preview = withContext(Dispatchers.IO) {
                                        learningRepository.matchingPreview(
                                            original, categoryStore.canonicalId(original.category)
                                        )
                                    }
                                    pendingMatchCount = preview.activity
                                    pendingStatementMatchCount = preview.statements
                                    pendingCategoryId = categoryStore.canonicalId(original.category)
                                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                    throw cancelled
                                } catch (failure: Exception) {
                                    learningError = failure.message ?: "Unable to check remembered matches."
                                } finally { learningBusy = false }
                            }
                        }) { Text("Remember across Activity and Statements") }
                    }
                }
            }
        }
        item {
            TransactionDetailFacts(
                item = current,
                accountLabel = current.accountId?.let { id -> accountStore.listAccounts(enabledOnly = false).firstOrNull { it.id == id }?.name } ?: "Not identified",
                categoryLabel = receivableLinks[current.id]?.description ?: categoryStore.labelFor(categoryStore.canonicalId(current.category)),
                onAccount = current.accountId?.let { id -> { onAccountClick(id) } },
                onCategory = if (!confirmedTransfer && current.relationType != TransactionStore.RELATION_REFUND) ({ showCategoryPicker = true }) else null
            )
        }
        if (!confirmedTransfer) item(key = "transaction-correction-actions") {
            SectionHeader("Correct transaction")
            PesaCard(Modifier.testTag("transaction-corrections")) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Check the original message evidence before correcting money movements. " +
                            "Changes here can affect your financial summaries.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    correctionResult?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    if (current.counterparty.isNullOrBlank() &&
                        !current.evidenceLabel.isNullOrBlank()
                    ) {
                        ActionRow(
                            icon = Icons.Rounded.CheckCircle,
                            label = "Use suggested identity",
                            onClick = {
                                correctionError = null
                                pendingCorrection = TransactionCorrectionAction.ACCEPT_IDENTITY
                            }
                        )
                    }
                    if (current.relationType == TransactionStore.RELATION_REFUND) {
                        ActionRow(
                            icon = Icons.Rounded.SwapHoriz,
                            label = "This was not a refund",
                            onClick = {
                                correctionError = null
                                pendingCorrection = TransactionCorrectionAction.REMOVE_REFUND_LINK
                            }
                        )
                    } else if (current.relationType == null &&
                        current.direction != MoneyDirection.TRANSFER.name &&
                        current.reviewState != ReviewState.EXCLUDED.name
                    ) {
                        ActionRow(
                            icon = Icons.Rounded.SwapHoriz,
                            label = "Mark as own-account transfer",
                            onClick = {
                                correctionError = null
                                pendingCorrection = TransactionCorrectionAction.MARK_OWN_TRANSFER
                            }
                        )
                    }
                    if (current.reviewState != ReviewState.EXCLUDED.name) {
                        ActionRow(
                            icon = Icons.Rounded.Block,
                            label = "Exclude from tracked activity",
                            tintError = true,
                            onClick = {
                                correctionError = null
                                pendingCorrection = TransactionCorrectionAction.EXCLUDE_ENTRY
                            }
                        )
                    }
                }
            }
        }

        if (
            current.fulizaFinancedMinor > 0L
        ) {
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
                    original =
                        linkedRefundOriginal
                )
            }
        }

        item {
            val canonicalCategory =
                categoryStore.canonicalId(
                    current.category
                )
            val categoryDirection =
                categoryStore.directionFor(
                    canonicalCategory,
                    runCatching {
                        MoneyDirection
                            .valueOf(
                                current.direction
                            )
                    }.getOrDefault(
                        MoneyDirection.NEUTRAL
                    )
                )
            val canRemember =
                canLearnIdentity(current) &&
                    PersonalLearningPolicy
                        .isLearnableCategory(
                            canonicalCategory,
                            categoryDirection
                        )

            TransactionDetailMessage(
                item = current,
                evidence = messageEvidence,
                expanded = messageExpanded,
                onExpanded = { messageExpanded = !messageExpanded },
                hasAnnotation = annotation.note.isNotBlank() || annotation.receiptUri != null,
                onAnnotation = { showAnnotationDialog = true }
            ) {
            if (!confirmedTransfer) ClassificationCard(
                item = current,
                appliedRule =
                    appliedRule,
                learningPreferenceActive =
                    learningPreference != null,
                canRemember =
                    canRemember,
                confidenceLabel =
                    classificationConfidenceLabel(
                        current,
                        learningExplanation,
                        appliedRule
                    ),
                onRememberChanged = {
                        rememberChoice ->
                    if (
                        !learningBusy &&
                        canRemember
                    ) {
                        learningBusy = true
                        learningError = null
                        scope.launch {
                        try {
                            if (rememberChoice) {
                                // Same approval as the category-picker and
                                // previously-manual classification paths.
                                // The switch must not silently save an Activity-
                                // only preference without a Statement preview.
                                val preview = withContext(Dispatchers.IO) {
                                    learningRepository.matchingPreview(
                                        current, canonicalCategory
                                    )
                                }
                                pendingMatchCount = preview.activity
                                pendingStatementMatchCount = preview.statements
                                pendingCategoryId = canonicalCategory
                            } else {
                                withContext(Dispatchers.IO) {
                                    personalLearningStore.forgetPreferenceFor(current)
                                }
                                learnedMessage =
                                    "The remembered category was turned off for future " +
                                    "Activity and Statement matches."
                                refresh()
                            }
                        } catch (
                            cancelled:
                                kotlinx.coroutines
                                    .CancellationException
                        ) {
                            throw cancelled
                        } catch (
                            failure: Exception
                        ) {
                            learningError =
                                failure.message
                                    ?: "Could not update learning."
                        } finally {
                            learningBusy =
                                false
                        }
                    }
                    }
                },
                onDisableRule = {
                        rule ->
                    if (
                        ruleStore.setEnabled(
                            id = rule.id,
                            enabled = false
                        )
                    ) {
                        learnedMessage =
                            "Smart rule disabled for future transactions."
                        refresh()
                    }
                }
            )
            }
        }

        learnedMessage?.let {
                message ->
            item {
                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),
                    color =
                        pesaToneContainer(
                            "green"
                        )
                ) {
                    Row(
                        modifier =
                            Modifier.padding(
                                12.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded
                                .CheckCircle,
                            contentDescription =
                                null,
                            tint =
                                pesaToneContent(
                                    "green"
                                )
                        )
                        Text(
                            message,
                            modifier =
                                Modifier.padding(
                                    start = 10.dp
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }
                }
            }
        }

        if (
            pendingCategoryId == null
        ) {
            learningError?.let {
                    message ->
                item {
                    Text(
                        message,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            }
        }

        item {
            TransactionQuickActions(
                merchant =
                    current.counterparty
                        ?: pretty(
                            current.type
                        ),
                annotation =
                    annotation,
                accountLabel =
                    current.accountId
                        ?.let {
                                accountId ->
                            accountStore
                                .listAccounts(
                                    enabledOnly =
                                        false
                                )
                                .firstOrNull {
                                    it.id ==
                                        accountId
                                }
                                ?.name
                        }
                        ?: "Account",
                hasAccount =
                    !current.accountId
                        .isNullOrBlank(),
                onMerchantHistory = {
                    showMerchantHistory =
                        true
                },
                onAnnotation = {
                    showAnnotationDialog =
                        true
                },
                onAccount = {
                    current.accountId
                        ?.let(
                            onAccountClick
                        )
                }
            )
        }

        learningSuggestion
            ?.takeIf {
                it.action ==
                    PersonalLearningAction
                        .SUGGEST
            }
            ?.let {
                    suggestion ->
                item {
                    PesaCard {
                        Column(
                            Modifier.padding(
                                16.dp
                            )
                        ) {
                            Row(
                                verticalAlignment =
                                    Alignment
                                        .CenterVertically
                            ) {
                                Text(
                                    "PesaTrack suggestion",
                                    modifier =
                                        Modifier.weight(
                                            1f
                                        ),
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
                                    categoryStore
                                        .labelFor(
                                            suggestion.categoryId
                                        ) +
                                    ". Confirm before PesaTrack changes this transaction.",
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
                                        suggestion
                                            .categoryId
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
                                        categoryStore
                                            .labelFor(
                                                suggestion
                                                    .categoryId
                                            )
                                )
                            }
                        }
                    }
                }
            }




    }
}

@Composable
private fun TransactionHero(
    item: StoredTransaction
) {
    val context =
        LocalContext.current
    val locale =
        androidx.compose.ui.platform
            .LocalConfiguration
            .current
            .locales[0]
    val accountLabel =
        item.accountId
            ?.let {
                    accountId ->
                AccountStore.get(context)
                    .listAccounts(
                        enabledOnly = false
                    )
                    .firstOrNull {
                        it.id == accountId
                    }
                    ?.name
            }
            ?: if (
                item.accountId ==
                AccountStore.DEFAULT_MPESA_ID
            ) {
                "M-PESA"
            } else {
                pretty(
                    item.sourceKind
                )
            }

    val status =
        when (
            item.reviewState
        ) {
            ReviewState.EXCLUDED.name ->
                "Excluded"
            ReviewState.NEEDS_REVIEW.name ->
                "Review"
            else ->
                "Completed"
        }

    LandscapeBackdrop(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp),
        showSun = false
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                TransactionCounterpartyBadge(
                    name =
                        item.counterparty,
                    size = 54.dp
                )
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(
                                start = 10.dp
                            )
                ) {
                    Text(
                        item.counterparty
                            ?: pretty(
                                item.type
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall,
                        fontWeight =
                            FontWeight.Bold
                    )
                    Text(
                        pretty(
                            item.type
                        ),
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                    Surface(
                        modifier =
                            Modifier.padding(
                                top = 5.dp
                            ),
                        shape =
                            RoundedCornerShape(
                                11.dp
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .surface
                                .copy(
                                    alpha = .38f
                                )
                    ) {
                        Row(
                            modifier =
                                Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.SwapHoriz,
                                contentDescription =
                                    null,
                                modifier =
                                    Modifier.size(
                                        15.dp
                                    ),
                                tint =
                                    Color(
                                        0xFF77E8B6
                                    )
                            )
                            Text(
                                accountLabel,
                                modifier =
                                    Modifier.padding(
                                        start = 6.dp
                                    ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelMedium
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment =
                        Alignment.End
                ) {
                    Surface(
                        shape =
                            CircleShape,
                        color =
                            MaterialTheme
                                .colorScheme
                                .surface
                                .copy(
                                    alpha = .32f
                                )
                    ) {
                        Icon(
                            Icons.Rounded.Send,
                            contentDescription =
                                null,
                            modifier =
                                Modifier.padding(
                                    9.dp
                                ),
                            tint =
                                Color(
                                    0xFF74F1B9
                                )
                        )
                    }
                    Text(
                        "Amount",
                        modifier =
                            Modifier.padding(
                                top = 4.dp
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
                    Text(
                        formatMoney(
                            item.amountMinor
                        ),
                        style =
                            MaterialTheme
                                .typography
                                .headlineMedium,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Spacer(
                Modifier.weight(1f)
            )

            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(
                        18.dp
                    ),
                color =
                    MaterialTheme
                        .colorScheme
                        .surface
                        .copy(
                            alpha = .92f
                        )
            ) {
                Column(
                    modifier =
                        Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 8.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            5.dp
                        )
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {
                        CompactHeroMetric(
                            "Transaction code",
                            item.transactionCode
                                ?: "—",
                            Icons.AutoMirrored
                                .Rounded
                                .ReceiptLong,
                            Modifier.weight(1f)
                        )
                        CompactHeroMetric(
                            "Status",
                            status,
                            Icons.Rounded.CheckCircle,
                            Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {
                        CompactHeroMetric(
                            "Transaction date",
                            SimpleDateFormat(
                                "d MMM",
                                locale
                            ).format(
                                Date(
                                    item.occurredAtEpochMs
                                )
                            ),
                            Icons.Rounded.CalendarMonth,
                            Modifier.weight(1f)
                        )
                        CompactHeroMetric(
                            "Fees",
                            formatMoney(
                                item.feeMinor +
                                    item
                                        .fulizaAccessFeeMinor
                            ),
                            Icons.Rounded.Paid,
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionCounterpartyBadge(
    name: String?,
    size: androidx.compose.ui.unit.Dp
) {
    val initials =
        bankInitials(name)

    Surface(
        modifier =
            Modifier.size(size),
        shape = CircleShape,
        color =
            Color(0xFF4F46D8)
    ) {
        Box(
            contentAlignment =
                Alignment.Center
        ) {
            if (initials != null) {
                Text(
                    initials,
                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,
                    fontWeight =
                        FontWeight.ExtraBold,
                    color =
                        Color(0xFF55E6D1)
                )
            } else {
                Icon(
                    Icons.Rounded.AccountCircle,
                    contentDescription =
                        null,
                    modifier =
                        Modifier.size(
                            size * .62f
                        ),
                    tint =
                        Color(0xFF55E6D1)
                )
            }
        }
    }
}

internal fun bankInitials(
    rawName: String?
): String? {
    val name =
        rawName?.trim()
            ?.takeIf(String::isNotBlank)
            ?: return null
    val words =
        name.replace(
            Regex("""[^A-Za-z0-9&]+"""),
            " "
        )
            .trim()
            .split(Regex("""\s+"""))
            .filter(String::isNotBlank)
    val bankIndex =
        words.indexOfFirst {
            it.equals(
                "bank",
                ignoreCase = true
            )
        }
    if (bankIndex < 0) {
        return null
    }

    val beforeBank =
        words.take(bankIndex)
            .filterNot {
                it.equals(
                    "the",
                    ignoreCase = true
                ) ||
                    it.equals(
                        "of",
                        ignoreCase = true
                    )
            }

    val first =
        beforeBank.firstOrNull()
            ?: return "BK"
    val isAcronym =
        first.length in 2..4 &&
            first.all {
                !it.isLetter() ||
                    it.isUpperCase()
            }
    if (isAcronym) {
        return first.take(4)
    }

    return if (beforeBank.size >= 2) {
        buildString {
            append(
                beforeBank[0]
                    .first()
                    .uppercaseChar()
            )
            append(
                beforeBank[1]
                    .first()
                    .uppercaseChar()
            )
        }
    } else {
        buildString {
            append(
                first.first()
                    .uppercaseChar()
            )
            append('B')
        }
    }
}

@Composable
private fun CompactHeroMetric(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier
) {
    Row(
        modifier =
            modifier.padding(
                horizontal = 1.dp,
                vertical = 1.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color =
                MaterialTheme
                    .colorScheme
                    .surfaceVariant
                    .copy(alpha = .72f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier =
                    Modifier.padding(
                        6.dp
                    )
                        .size(16.dp),
                tint =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 7.dp)
        ) {
            Text(
                label,
                style =
                    MaterialTheme
                        .typography
                        .labelSmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
            Text(
                value,
                modifier =
                    Modifier.padding(top = 1.dp),
                style =
                    MaterialTheme
                        .typography
                        .titleSmall,
                fontWeight =
                    FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
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
    Column(
        modifier =
            modifier.padding(
                horizontal = 2.dp
            )
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            modifier =
                Modifier.size(
                    19.dp
                )
        )
        Text(
            label,
            modifier =
                Modifier.padding(
                    top = 4.dp
                ),
            style =
                MaterialTheme
                    .typography
                    .labelSmall,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            maxLines = 2
        )
        Text(
            value,
            style =
                MaterialTheme
                    .typography
                    .titleSmall,
            fontWeight =
                FontWeight.SemiBold,
            maxLines = 2
        )
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
    learningPreferenceActive: Boolean,
    canRemember: Boolean,
    confidenceLabel: String,
    onRememberChanged: (Boolean) -> Unit,
    onDisableRule: (SmartRule) -> Unit
) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Remember in Activity + Statements", style = MaterialTheme.typography.bodyMedium)
                    Text("One saved preference for eligible SMS and statement history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(confidenceLabel, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = learningPreferenceActive, onCheckedChange = onRememberChanged, enabled = canRemember)
            }
            if (!canRemember) Text("Category memory is unavailable for this transaction.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            appliedRule?.let { rule ->
                TextButton(onClick = { onDisableRule(rule) }) { Text("Disable applied smart rule") }
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

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val narrowScreen =
            maxWidth < 430.dp
        val compactCategories =
            categories.filter {
                it.label.length <= 10
            }
        val wideCategories =
            categories.filter {
                it.label.length > 10
            }

        Column(
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            if (narrowScreen) {
                CategoryGridRows(
                    categories = categories,
                    columns = 2,
                    currentCategoryId =
                        currentCategoryId,
                    onSelected = onSelected
                )
            } else {
                CategoryGridRows(
                    categories =
                        compactCategories,
                    columns = 3,
                    currentCategoryId =
                        currentCategoryId,
                    onSelected = onSelected
                )

                if (
                    wideCategories
                        .isNotEmpty()
                ) {
                    CategoryGridRows(
                        categories =
                            wideCategories,
                        columns = 2,
                        currentCategoryId =
                            currentCategoryId,
                        onSelected =
                            onSelected
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    showAddCategory =
                        true
                },
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(
                        16.dp
                    )
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription =
                        null
                )
                Text(
                    "Add category",
                    modifier =
                        Modifier.padding(
                            start = 6.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun CategoryGridRows(
    categories:
        List<
            com.kedevjimmy
                .pesatrack
                .data
                .local
                .PesaCategory
            >,
    columns: Int,
    currentCategoryId: String,
    onSelected: (String) -> Unit
) {
    categories.chunked(columns)
        .forEach {
                rowItems ->
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {
                rowItems.forEach {
                        category ->
                    val selected =
                        currentCategoryId ==
                            category.id
                    val visual =
                        categoryVisual(
                            category.id
                        )

                    Surface(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clickable {
                                    onSelected(
                                        category.id
                                    )
                                },
                        shape =
                            RoundedCornerShape(
                                16.dp
                            ),
                        color =
                            if (selected) {
                                MaterialTheme
                                    .colorScheme
                                    .primaryContainer
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .surfaceVariant
                                    .copy(
                                        alpha = .62f
                                    )
                            },
                        border =
                            BorderStroke(
                                if (selected) {
                                    2.dp
                                } else {
                                    1.dp
                                },
                                if (selected) {
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                                } else {
                                    MaterialTheme
                                        .colorScheme
                                        .outlineVariant
                                        .copy(
                                            alpha = .34f
                                        )
                                }
                            )
                    ) {
                        Row(
                            modifier =
                                Modifier.padding(
                                    horizontal =
                                        if (
                                            columns ==
                                            3
                                        ) {
                                            9.dp
                                        } else {
                                            12.dp
                                        },
                                    vertical = 7.dp
                                ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Surface(
                                shape =
                                    RoundedCornerShape(
                                        12.dp
                                    ),
                                color =
                                    visual.color
                                        .copy(
                                            alpha = .17f
                                        )
                            ) {
                                Icon(
                                    visual.icon,
                                    contentDescription =
                                        null,
                                    modifier =
                                        Modifier
                                            .padding(
                                                6.dp
                                            )
                                            .size(
                                                20.dp
                                            ),
                                    tint =
                                        visual.color
                                )
                            }
                            Text(
                                category.label,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(
                                            start = 8.dp
                                        ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelLarge,
                                fontWeight =
                                    FontWeight.SemiBold,
                                maxLines = 1,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurface
                            )
                            if (selected) {
                                Icon(
                                    Icons.Rounded
                                        .CheckCircle,
                                    contentDescription =
                                        "Selected",
                                    modifier =
                                        Modifier.size(
                                            18.dp
                                        ),
                                    tint =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                )
                            }
                        }
                    }
                }

                repeat(
                    columns -
                        rowItems.size
                ) {
                    Spacer(
                        Modifier.weight(1f)
                    )
                }
            }

        }
}

private data class CategoryVisual(
    val icon: ImageVector,
    val color: Color
)

@Composable
private fun categoryVisual(
    categoryId: String
): CategoryVisual =
    when (categoryId) {
        TransactionCategory
            .GROCERIES.name ->
            CategoryVisual(
                Icons.Rounded
                    .ShoppingCart,
                com.kedevjimmy.pesatrack.ui.components.pesaToneContent("violet")
            )

        TransactionCategory
            .EATING_OUT.name ->
            CategoryVisual(
                Icons.Rounded
                    .Restaurant,
                com.kedevjimmy.pesatrack.ui.components.pesaToneContent("amber")
            )

        TransactionCategory
            .TRANSPORT.name ->
            CategoryVisual(
                Icons.Rounded
                    .DirectionsBus,
                com.kedevjimmy.pesatrack.ui.components.pesaToneContent("blue")
            )

        TransactionCategory
            .RENT.name ->
            CategoryVisual(
                Icons.Rounded.Home,
                com.kedevjimmy.pesatrack.ui.components.pesaToneContent("red")
            )

        TransactionCategory
            .ELECTRICITY.name ->
            CategoryVisual(
                Icons.Rounded.Bolt,
                com.kedevjimmy.pesatrack.ui.components.pesaToneContent("amber")
            )

        TransactionCategory
            .WATER.name ->
            CategoryVisual(
                Icons.Rounded.WaterDrop,
                com.kedevjimmy.pesatrack.ui.components.pesaToneContent("blue")
            )

        TransactionCategory
            .INTERNET.name,
        TransactionCategory
            .PHONE_AND_DATA.name ->
            CategoryVisual(
                Icons.Rounded.Wifi,
                com.kedevjimmy.pesatrack.ui.components.pesaToneContent("violet")
            )

        TransactionCategory
            .SHOPPING.name ->
            CategoryVisual(
                Icons.Rounded.ShoppingBag,
                com.kedevjimmy.pesatrack.ui.components.pesaToneContent("red")
            )

        TransactionCategory
            .HEALTH.name,
        TransactionCategory
            .PERSONAL_CARE.name,
        TransactionCategory
            .FAMILY.name ->
            CategoryVisual(
                Icons.Rounded.Favorite,
                com.kedevjimmy.pesatrack.ui.components.pesaToneContent("red")
            )

        TransactionCategory
            .EDUCATION.name ->
            CategoryVisual(
                Icons.Rounded.School,
                com.kedevjimmy.pesatrack.ui.components.pesaToneContent("violet")
            )

        TransactionCategory
            .FEES.name,
        TransactionCategory
            .DEBT_LOAN.name ->
            CategoryVisual(
                Icons.AutoMirrored
                    .Rounded
                    .ReceiptLong,
                com.kedevjimmy.pesatrack.ui.components.pesaToneContent("amber")
            )

        TransactionCategory
            .SAVINGS.name ->
            CategoryVisual(
                Icons.Rounded.Savings,
                Color(0xFFFFD78A)
            )

        TransactionCategory
            .INVESTMENT.name ->
            CategoryVisual(
                Icons.Rounded.TrendingUp,
                com.kedevjimmy.pesatrack.ui.components.pesaToneContent("green")
            )

        TransactionCategory
            .INCOME.name ->
            CategoryVisual(
                Icons.Rounded.Paid,
                com.kedevjimmy.pesatrack.ui.components.pesaToneContent("violet")
            )

        TransactionCategory
            .HOME_HOUSEHOLD.name ->
            CategoryVisual(
                Icons.Rounded.Home,
                com.kedevjimmy.pesatrack.ui.components.pesaToneContent("green")
            )

        else ->
            CategoryVisual(
                Icons.Rounded.MoreHoriz,
                Color(0xFFB3C7C2)
            )
    }

@Composable
private fun TransactionQuickActions(
    merchant: String,
    annotation: TransactionAnnotation,
    accountLabel: String,
    hasAccount: Boolean,
    onMerchantHistory: () -> Unit,
    onAnnotation: () -> Unit,
    onAccount: () -> Unit
) {
    PesaCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            ActionRow(Icons.Rounded.AccountCircle, "Merchant history", onClick = onMerchantHistory)
            if (hasAccount) ActionRow(Icons.Rounded.Paid, "View $accountLabel", onClick = onAccount)
        }
    }
}

@Composable
private fun TransactionActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier =
            modifier.clickable(
                enabled = enabled,
                onClick = onClick
            ),
        shape =
            RoundedCornerShape(
                18.dp
            ),
        color =
            MaterialTheme
                .colorScheme
                .surfaceVariant
                .copy(alpha = .64f),
        border =
            BorderStroke(
                1.dp,
                MaterialTheme
                    .colorScheme
                    .outlineVariant
                    .copy(alpha = .35f)
            )
    ) {
        Row(
            modifier =
                Modifier.padding(
                    14.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Surface(
                shape =
                    CircleShape,
                color =
                    com.kedevjimmy.pesatrack.ui.components.pesaToneContent("green")
                        .copy(
                            alpha = .2f
                        )
            ) {
                Icon(
                    icon,
                    contentDescription =
                        null,
                    modifier =
                        Modifier.padding(
                            9.dp
                        ),
                    tint =
                        Color(
                            0xFF8ADFC0
                        )
                )
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(
                            start = 10.dp
                        )
            ) {
                Text(
                    title,
                    style =
                        MaterialTheme
                            .typography
                            .titleSmall,
                    fontWeight =
                        FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    maxLines = 2
                )
            }
            Icon(
                Icons.Rounded
                    .ChevronRight,
                contentDescription =
                    null,
                tint =
                    if (enabled) {
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    } else {
                        MaterialTheme
                            .colorScheme
                            .outline
                    }
            )
        }
    }
}

@Composable
private fun TransactionAnnotationDialog(
    transaction: StoredTransaction,
    annotation: TransactionAnnotation,
    onDismiss: () -> Unit,
    onSaveNote: (String) -> Unit,
    onAttachReceipt: () -> Unit,
    onRemoveReceipt: () -> Unit
) {
    val context =
        LocalContext.current
    var note by remember(
        transaction.id,
        annotation.note
    ) {
        mutableStateOf(
            annotation.note
        )
    }

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                "Note or receipt"
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {
                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            "Personal note"
                        )
                    },
                    placeholder = {
                        Text(
                            "What was this payment for?"
                        )
                    },
                    minLines = 3,
                    maxLines = 5
                )

                if (
                    annotation.receiptUri !=
                    null
                ) {
                    Surface(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(
                                14.dp
                            ),
                        color =
                            pesaToneContainer(
                                "green"
                            )
                    ) {
                        Row(
                            modifier =
                                Modifier.padding(
                                    12.dp
                                ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded
                                    .AttachFile,
                                contentDescription =
                                    null
                            )
                            Text(
                                "Receipt attached",
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(
                                            start = 8.dp
                                        )
                            )
                            TextButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                android.net.Uri
                                                    .parse(
                                                        annotation
                                                            .receiptUri
                                                    )
                                            ).apply {
                                                addFlags(
                                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                )
                                            }
                                        )
                                    }
                                }
                            ) {
                                Text("Open")
                            }
                            TextButton(
                                onClick =
                                    onRemoveReceipt
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick =
                        onAttachReceipt,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Rounded
                            .AttachFile,
                        contentDescription =
                            null
                    )
                    Text(
                        if (
                            annotation.receiptUri !=
                            null
                        ) {
                            "Replace receipt"
                        } else {
                            "Attach receipt"
                        },
                        modifier =
                            Modifier.padding(
                                start = 8.dp
                            )
                    )
                }

                Text(
                    "Notes and receipt links stay on this device with your PesaTrack data.",
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
        },
        dismissButton = {
            TextButton(
                onClick =
                    onDismiss
            ) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveNote(
                        note
                    )
                }
            ) {
                Text("Save")
            }
        }
    )
}

@Composable
private fun MerchantHistoryDialog(
    merchant: String,
    transactions:
        List<StoredTransaction>,
    categoryStore: CategoryStore,
    onDismiss: () -> Unit,
    onTransactionClick: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                "Merchant history"
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {
                Text(
                    merchant,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )

                if (
                    transactions.isEmpty()
                ) {
                    Text(
                        "No other recent transactions with this merchant.",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                } else {
                    transactions.take(8)
                        .forEach {
                                item ->
                            Surface(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onTransactionClick(
                                                item.id
                                            )
                                        },
                                shape =
                                    RoundedCornerShape(
                                        14.dp
                                    ),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                                        .copy(
                                            alpha = .6f
                                        )
                            ) {
                                Row(
                                    modifier =
                                        Modifier.padding(
                                            11.dp
                                        ),
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier =
                                            Modifier
                                                .weight(
                                                    1f
                                                )
                                    ) {
                                        Text(
                                            formatMoney(
                                                item.amountMinor
                                            ),
                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .titleSmall
                                        )
                                        Text(
                                            formatDateTime(
                                                item.occurredAtEpochMs
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
                                    Text(
                                        categoryStore.labelFor(
                                            categoryStore.canonicalId(
                                                item.category
                                            )
                                        ),
                                        style =
                                            MaterialTheme
                                                .typography
                                                .labelMedium
                                    )
                                    Icon(
                                        Icons.Rounded
                                            .ChevronRight,
                                        contentDescription =
                                            null
                                    )
                                }
                            }
                        }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick =
                    onDismiss
            ) {
                Text("Done")
            }
        }
    )
}

private fun sameMerchant(
    first: StoredTransaction,
    second: StoredTransaction
): Boolean {
    val firstPhone =
        first.counterpartyPhone
            ?.filter(Char::isDigit)
            .orEmpty()
    val secondPhone =
        second.counterpartyPhone
            ?.filter(Char::isDigit)
            .orEmpty()
    if (
        firstPhone.isNotBlank() &&
        secondPhone.isNotBlank()
    ) {
        return firstPhone ==
            secondPhone
    }

    val firstName =
        first.counterparty
            ?.trim()
            ?.lowercase()
            .orEmpty()
    val secondName =
        second.counterparty
            ?.trim()
            ?.lowercase()
            .orEmpty()

    return firstName.isNotBlank() &&
        firstName == secondName
}

private fun classificationConfidenceLabel(
    item: StoredTransaction,
    learningExplanation:
        com.kedevjimmy.pesatrack
            .domain.learning
            .PersonalLearningExplanation?,
    appliedRule: SmartRule?
): String {
    if (
        item.reviewState ==
        ReviewState.USER_CLASSIFIED.name
    ) {
        return "100% match"
    }

    learningExplanation
        ?.application
        ?.confidence
        ?.let {
            return when (it) {
                com.kedevjimmy.pesatrack
                    .domain.learning
                    .PersonalLearningConfidence
                    .HIGH ->
                    "95% match"
                com.kedevjimmy.pesatrack
                    .domain.learning
                    .PersonalLearningConfidence
                    .MEDIUM ->
                    "75% match"
                com.kedevjimmy.pesatrack
                    .domain.learning
                    .PersonalLearningConfidence
                    .LOW ->
                    "55% match"
            }
        }

    item.evidenceConfidence
        ?.uppercase()
        ?.let {
            return when (it) {
                "HIGH" ->
                    "90% match"
                "MEDIUM" ->
                    "70% match"
                "LOW" ->
                    "50% match"
                else ->
                    "Auto match"
            }
        }

    if (appliedRule != null) {
        return "Rule match"
    }

    return if (
        item.reviewState ==
        ReviewState.NEEDS_REVIEW.name
    ) {
        "Needs review"
    } else {
        "Auto match"
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

