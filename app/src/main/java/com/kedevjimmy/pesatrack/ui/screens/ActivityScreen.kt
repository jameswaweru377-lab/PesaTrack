package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalAtm
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot
import com.kedevjimmy.pesatrack.data.local.ActivityMonthBucket
import com.kedevjimmy.pesatrack.data.local.ActivityReviewOverview
import com.kedevjimmy.pesatrack.data.local.ActivitySummary
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.DiscoveredFinancialEntity
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.IdentityGraphStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.PossibleDuplicateGroup
import com.kedevjimmy.pesatrack.data.local.ReviewGroup
import com.kedevjimmy.pesatrack.data.local.RelationshipReviewCandidate
import com.kedevjimmy.pesatrack.data.local.RelationshipReviewItem
import com.kedevjimmy.pesatrack.data.local.RelationshipReviewKind
import com.kedevjimmy.pesatrack.data.local.RelationshipReviewStore
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.KenyanBankDirectory
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import com.kedevjimmy.pesatrack.domain.identity.OwnershipReviewEvidence
import com.kedevjimmy.pesatrack.domain.rules.AccountIdentityPresentation
import com.kedevjimmy.pesatrack.domain.rules.HistoryImportWindow
import com.kedevjimmy.pesatrack.domain.rules.TrackedAccountGroup
import com.kedevjimmy.pesatrack.ingestion.FinancialMessageIngestionCoordinator
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnostic
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnosticCode
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnosticsStore
import com.kedevjimmy.pesatrack.ingestion.HistoricalSmsImporter
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialProviderRegistry
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.PesaTrackTopBar
import com.kedevjimmy.pesatrack.ui.components.SmallStatusPill
import com.kedevjimmy.pesatrack.ui.components.isPesaDarkTheme
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney
import com.kedevjimmy.pesatrack.domain.model.checkedMoneySumOf

data class ActivityCoreData(
    val monthBuckets: List<ActivityMonthBucket>,
    val snapshot: ActivityLedgerSnapshot,
    val previousSnapshot: ActivityLedgerSnapshot,
    val accountsById: Map<String, MoneyAccount>,
    val confirmedAccountIds: Set<String>
)

data class ActivitySupplementalData(
    val globalOverview: ActivityReviewOverview,
    val relationshipReviews: List<RelationshipReviewItem>,
    val pendingFinancialEntities: List<DiscoveredFinancialEntity>,
    val financialDiagnostics: List<FinancialIngestionDiagnostic>,
    val identityReviewCount: Int,
    val ownershipEvidence: Map<String, OwnershipReviewEvidence> = emptyMap()
)

data class ActivityScreenData(
    val monthBuckets: List<ActivityMonthBucket>,
    val snapshot: ActivityLedgerSnapshot,
    val previousSnapshot: ActivityLedgerSnapshot,
    val globalOverview: ActivityReviewOverview,
    val relationshipReviews: List<RelationshipReviewItem>,
    val pendingFinancialEntities: List<DiscoveredFinancialEntity>,
    val financialDiagnostics: List<FinancialIngestionDiagnostic>,
    val accountsById: Map<String, MoneyAccount>,
    val confirmedAccountIds: Set<String>,
    val identityReviewCount: Int,
    val ownershipEvidence: Map<String, OwnershipReviewEvidence> = emptyMap()
)

class ActivityScreenCache {
    var selectedMonthEpochMs: Long? = null
    var data: ActivityScreenData? = null
}

private fun emptyActivityLedgerSnapshot(): ActivityLedgerSnapshot =
    ActivityLedgerSnapshot(
        transactions = emptyList(),
        summary =
            ActivitySummary(
                incomeMinor = 0L,
                spentMinor = 0L,
                feesMinor = 0L,
                transferMinor = 0L,
                netMinor = 0L,
                needsReviewCount = 0
            )
    )

@Composable
fun ActivityScreen(
    scaffoldPadding: PaddingValues,
    onTransactionClick: (Long) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onIdentityReviewClick: () -> Unit = {},
    onSalaryClick: () -> Unit = {},
    openNeedsAttention: Boolean = false,
    stateCache: ActivityScreenCache? = null
) {
    var payCycleMode by rememberSaveable { mutableStateOf(false) }
    if (payCycleMode) {
        PayCycleActivityScreen(scaffoldPadding, onCalendar = { payCycleMode = false },
            onSalaryClick = onSalaryClick, onTransactionClick = onTransactionClick)
        return
    }
    val context = LocalContext.current
    val activityScope =
        rememberCoroutineScope()
    val store = remember { TransactionStore.get(context) }
    val categoryStore = remember { CategoryStore.get(context) }
    val financialStore = remember {
        FinancialUnderstandingStore.get(context)
    }
    val financialCoordinator = remember {
        FinancialMessageIngestionCoordinator.get(context)
    }
    val diagnosticsStore = remember {
        FinancialIngestionDiagnosticsStore.get(context)
    }
    val accountStore = remember {
        AccountStore.get(context)
    }
    val relationshipReviewStore = remember {
        RelationshipReviewStore.get(context)
    }
    val identityGraphStore = remember {
        IdentityGraphStore.get(context)
    }
    val ledgerUpdateState by LedgerUpdateBus.state.collectAsState()
    val currentMonthEpochMs = remember {
        monthStart(System.currentTimeMillis())
    }
    val warmData =
        remember(stateCache) {
            stateCache?.data
        }
    val warmMonth =
        remember(stateCache) {
            stateCache?.selectedMonthEpochMs
                ?: currentMonthEpochMs
        }

    var selectedMonthEpochMs by rememberSaveable {
        mutableStateOf(warmMonth)
    }
    var expandedMonthEpochMs by rememberSaveable {
        mutableStateOf<Long?>(warmMonth)
    }
    var monthBuckets by remember {
        mutableStateOf(
            warmData?.monthBuckets
                ?: listOf(
                    ActivityMonthBucket(
                        monthStartEpochMs =
                            currentMonthEpochMs,
                        transactionCount = 0
                    )
                )
        )
    }
    var snapshot by remember {
        mutableStateOf(
            warmData?.snapshot
                ?: emptyActivityLedgerSnapshot()
        )
    }
    var previousSnapshot by remember {
        mutableStateOf(
            warmData?.previousSnapshot
                ?: emptyActivityLedgerSnapshot()
        )
    }
    var activityLoading by remember { mutableStateOf(warmData == null) }
    var activityError by remember { mutableStateOf(false) }
    var loadedMonthEpochMs by remember { mutableStateOf<Long?>(if (warmData != null) warmMonth else null) }
    var globalOverview by remember {
        mutableStateOf(
            warmData?.globalOverview
                ?: ActivityReviewOverview(
                    reviewGroups = emptyList(),
                    possibleDuplicates = emptyList()
                )
        )
    }
    var showEvidenceLinks by remember { mutableStateOf(false) }
    var optionalEvidenceLinks by remember { mutableStateOf<List<RelationshipReviewItem>>(emptyList()) }
    LaunchedEffect(showEvidenceLinks, ledgerUpdateState.revision) {
        optionalEvidenceLinks = if (showEvidenceLinks) withContext(Dispatchers.IO) {
            relationshipReviewStore.pendingEvidenceLinks()
        } else emptyList()
    }
    var relationshipReviews by remember {
        mutableStateOf<List<RelationshipReviewItem>>(
            warmData?.relationshipReviews
                ?: emptyList()
        )
    }
    var pendingFinancialEntities by remember {
        mutableStateOf<List<DiscoveredFinancialEntity>>(
            warmData?.pendingFinancialEntities
                ?: emptyList()
        )
    }
    var selectedFilter by remember {
        mutableStateOf("All")
    }
    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }
    // The ledger is the primary Activity task. Keep the full monthly breakdown
    // available on demand instead of forcing it above every transaction.
    var showDetailedSummary by rememberSaveable { mutableStateOf(false) }
    var selectedFinancialSource by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    var financialDiagnostics by remember {
        mutableStateOf<List<FinancialIngestionDiagnostic>>(
            warmData?.financialDiagnostics
                ?: emptyList()
        )
    }
    var ownershipEvidence by remember {
        mutableStateOf(warmData?.ownershipEvidence ?: emptyMap())
    }
    var accountsById by remember {
        mutableStateOf<Map<String, MoneyAccount>>(
            warmData?.accountsById
                ?: emptyMap()
        )
    }
    var confirmedAccountIds by remember {
        mutableStateOf<Set<String>>(
            warmData?.confirmedAccountIds
                ?: emptySet()
        )
    }
    var identityReviewCount by remember {
        mutableStateOf(
            warmData?.identityReviewCount
                ?: 0
        )
    }
    var selectedGlobalQueue by
        rememberSaveable(
            openNeedsAttention
        ) {
            mutableStateOf<String?>(
                if (openNeedsAttention) {
                    "Attention"
                } else {
                    null
                }
            )
        }
    var showMonthPicker by remember {
        mutableStateOf(false)
    }
    var accountReviewEntity by remember {
        mutableStateOf<DiscoveredFinancialEntity?>(
            null
        )
    }
    var accountReviewBusyKey by remember {
        mutableStateOf<String?>(null)
    }
    var accountReviewError by remember {
        mutableStateOf<String?>(null)
    }

    suspend fun loadActivityCoreData(
        monthEpochMs: Long
    ): ActivityCoreData =
        withContext(Dispatchers.IO) {
            val existing =
                store.activityMonths()
                    .toMutableList()
            if (
                existing.none {
                    it.monthStartEpochMs ==
                        currentMonthEpochMs
                }
            ) {
                existing.add(
                    0,
                    ActivityMonthBucket(
                        monthStartEpochMs =
                            currentMonthEpochMs,
                        transactionCount = 0
                    )
                )
            }

            ActivityCoreData(
                monthBuckets =
                    existing.sortedByDescending {
                        it.monthStartEpochMs
                    },
                snapshot =
                    store.activitySnapshot(
                        nowEpochMs =
                            monthEpochMs
                    ),
                previousSnapshot =
                    store.activitySnapshot(
                        nowEpochMs =
                            shiftMonth(
                                monthEpochMs,
                                -1
                            ),
                        performMaintenance =
                            false
                    ),
                accountsById =
                    accountStore
                        .listAccounts(
                            enabledOnly = false
                        )
                        .associateBy {
                            it.id
                        },
                confirmedAccountIds =
                    financialStore
                        .confirmedAccountIds()
            )
        }

    suspend fun loadActivitySupplementalData():
        ActivitySupplementalData =
        withContext(Dispatchers.IO) {
            val evidence = financialStore.ownershipReviewEvidence()
            ActivitySupplementalData(
                ownershipEvidence = evidence,
                globalOverview =
                    store.activityReviewOverview(
                        performMaintenance =
                            false
                    ),
                relationshipReviews =
                    relationshipReviewStore
                        .pendingReviews(),
                pendingFinancialEntities =
                    financialStore
                        .pendingEntities().filter { evidence[it.entityKey]?.canAsk == true }.distinctBy { it.entityKey },
                financialDiagnostics =
                    diagnosticsStore
                        .recent(500)
                        .filter {
                            it.code !=
                                FinancialIngestionDiagnosticCode
                                    .IGNORED_NON_FINANCIAL
                        },
                identityReviewCount =
                    identityGraphStore
                        .pendingReviewPrompts(
                            limit = 100,
                            includeOptional = false
                        )
                        .size
            )
        }

    fun applyActivityCoreData(
        core: ActivityCoreData
    ) {
        monthBuckets = core.monthBuckets
        snapshot = core.snapshot
        previousSnapshot =
            core.previousSnapshot
        accountsById =
            core.accountsById
        confirmedAccountIds =
            core.confirmedAccountIds
    }

    fun applyActivitySupplementalData(
        supplemental: ActivitySupplementalData
    ) {
        ownershipEvidence = supplemental.ownershipEvidence
        globalOverview =
            supplemental.globalOverview
        relationshipReviews =
            supplemental.relationshipReviews
        pendingFinancialEntities =
            supplemental.pendingFinancialEntities
        financialDiagnostics =
            supplemental.financialDiagnostics
        identityReviewCount =
            supplemental.identityReviewCount
    }

    fun cacheActivityData(
        monthEpochMs: Long,
        core: ActivityCoreData,
        supplemental: ActivitySupplementalData
    ) {
        stateCache?.let {
            it.selectedMonthEpochMs =
                monthEpochMs
            it.data =
                ActivityScreenData(
                    ownershipEvidence = supplemental.ownershipEvidence,
                    monthBuckets =
                        core.monthBuckets,
                    snapshot =
                        core.snapshot,
                    previousSnapshot =
                        core.previousSnapshot,
                    globalOverview =
                        supplemental
                            .globalOverview,
                    relationshipReviews =
                        supplemental
                            .relationshipReviews,
                    pendingFinancialEntities =
                        supplemental
                            .pendingFinancialEntities,
                    financialDiagnostics =
                        supplemental
                            .financialDiagnostics,
                    accountsById =
                        core.accountsById,
                    confirmedAccountIds =
                        core.confirmedAccountIds,
                    identityReviewCount =
                        supplemental
                            .identityReviewCount
                )
        }
    }

    suspend fun reloadActivityData(
        requestedMonth: Long
    ) {
        activityLoading = true
        activityError = false
        try {
        val core =
            loadActivityCoreData(
                requestedMonth
            )
        if (
            selectedMonthEpochMs !=
            requestedMonth
        ) {
            return
        }

        // Show the selected month as soon as the ledger query finishes.
        // Diagnostics/review metadata continue loading afterward instead of
        // holding the transaction list behind slower secondary stores.
        applyActivityCoreData(core)
        loadedMonthEpochMs = requestedMonth

        val supplemental =
            loadActivitySupplementalData()
        if (
            selectedMonthEpochMs !=
            requestedMonth
        ) {
            return
        }

        applyActivitySupplementalData(
            supplemental
        )
        cacheActivityData(
            monthEpochMs =
                requestedMonth,
            core = core,
            supplemental =
                supplemental
        )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            if (selectedMonthEpochMs == requestedMonth) {
                android.util.Log.e("PesaTrackActivity", "Activity could not load", failure)
                activityError = true
            }
        } finally {
            if (selectedMonthEpochMs == requestedMonth) activityLoading = false
        }
    }

    val reviewableFinancialEntities =
        remember(
            pendingFinancialEntities,
            financialDiagnostics
        ) {
            pendingFinancialEntities
                .filter {
                    shouldOfferOwnershipReview(
                        entity = it,
                        diagnostics =
                            financialDiagnostics
                    )
                }
        }

    fun selectMonth(epochMs: Long) {
        val normalizedMonth =
            monthStart(epochMs)
        selectedMonthEpochMs =
            normalizedMonth
        expandedMonthEpochMs =
            normalizedMonth
        selectedFilter = "All"
        selectedFinancialSource = null
        selectedGlobalQueue = null
    }

    fun refresh() {
        val requestedMonth =
            selectedMonthEpochMs
        activityScope.launch {
            reloadActivityData(
                requestedMonth
            )
        }
    }

    LaunchedEffect(
        ledgerUpdateState.revision,
        selectedMonthEpochMs
    ) {
        reloadActivityData(
            selectedMonthEpochMs
        )
    }

    if (loadedMonthEpochMs != selectedMonthEpochMs) {
        LazyColumn(modifier = Modifier.padding(scaffoldPadding), contentPadding = PaddingValues(16.dp)) {
            item {
                Text(if (activityError) "Activity could not load" else "Loading activity…")
                if (activityLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (activityError) {
                    Text("Your saved data is unchanged. Try loading this month again.")
                    TextButton(onClick = { refresh() }) { Text("Try again") }
                }
            }
        }
        return
    }

    val selectedMonthLabel =
        monthLabel(selectedMonthEpochMs)
    val nextMonthEpochMs =
        shiftMonth(selectedMonthEpochMs, 1)
    val monthDiagnostics =
        financialDiagnostics.filter { diagnostic ->
            diagnostic.receivedAtEpochMs >=
                selectedMonthEpochMs &&
                diagnostic.receivedAtEpochMs <
                    nextMonthEpochMs
        }
    val ownedAccountGroups =
        AccountDisplayPolicy.groups(
            accountsById.values.toList()
        ).filter { group ->
            group.accounts.any {
                AccountDisplayPolicy
                    .isConfirmedOwned(
                        account = it,
                        confirmedAccountIds =
                            confirmedAccountIds
                    )
            }
        }
    val ownedAccountDiagnostics =
        monthDiagnostics.mapNotNull {
                diagnostic ->
            val sourceName =
                financialSourceDisplayName(
                    diagnostic
                )
            val group =
                ownedAccountGroups
                    .firstOrNull {
                        AccountDisplayPolicy
                            .matchesSource(
                                it,
                                sourceName
                            )
                    }
                ?: return@mapNotNull null
            group.key to diagnostic
        }
    val sourceSummaries =
        ownedAccountDiagnostics
            .groupBy { it.first }
            .mapNotNull { (key, entries) ->
                val group =
                    ownedAccountGroups
                        .firstOrNull {
                            it.key == key
                        }
                        ?: return@mapNotNull null
                FinancialSourceSummary(
                    key = key,
                    displayName =
                        group.displayName,
                    sourceType =
                        accountGroupTypeLabel(
                            group
                        ),
                    messageCount =
                        entries.size
                )
            }
            .sortedWith(
                compareByDescending<
                    FinancialSourceSummary
                > { it.messageCount }
                    .thenBy {
                        it.displayName
                    }
            )
    val selectedSourceDiagnostics =
        selectedFinancialSource
            ?.let { selectedKey ->
                ownedAccountDiagnostics
                    .filter {
                        it.first ==
                            selectedKey
                    }
                    .map { it.second }
            }
            .orEmpty()
            .sortedByDescending {
                it.receivedAtEpochMs
            }
    val possibleDuplicateIds = snapshot.possibleDuplicates
        .flatMap { listOf(it.first.id, it.second.id) }
        .toSet()
    val filtered = snapshot.transactions.filter { item ->
        if (item.reviewState == ReviewState.EXCLUDED.name) {
            return@filter false
        }
        if (item.id in possibleDuplicateIds) {
            return@filter false
        }

        val matchesFilter =
            when (selectedFilter) {
                "Needs review" ->
                    item.reviewState ==
                        ReviewState.NEEDS_REVIEW.name
                "Spending" ->
                    item.direction == MoneyDirection.EXPENSE.name
                "Income" ->
                    item.direction == MoneyDirection.INCOME.name
                "Transfers" ->
                    item.direction == MoneyDirection.TRANSFER.name &&
                        item.relationType !=
                            TransactionStore.RELATION_REFUND
                else -> true
            }

        val query = searchQuery.trim()
        val matchesSearch =
            query.isBlank() ||
                item.counterparty?.contains(query, ignoreCase = true) == true ||
                item.evidenceLabel?.contains(query, ignoreCase = true) == true ||
                categoryStore.labelFor(item.category)
                    .contains(query, ignoreCase = true)

        matchesFilter && matchesSearch
    }

    val groupedFiltered = filtered
        .sortedByDescending { it.occurredAtEpochMs }
        .groupBy { dayStart(it.occurredAtEpochMs) }
        .toList()
        .sortedByDescending { it.first }

    Column(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 1.dp
        ) {
            PesaTrackTopBar(
                onProfileClick = onProfileClick,
                modifier = Modifier.padding(
                    horizontal = 18.dp,
                    vertical = 12.dp
                )
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = 18.dp,
                vertical = 10.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (ledgerUpdateState.isProcessing) {
                item {
                    LiveActivityUpdateStatus()
                }
            }

            item {
                Text(
                    "Activity",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item { ActivityMoneyActions(onChanged = { refresh() }) }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search transactions") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
            }

            if (activityError || activityLoading) item {
                if (activityError) {
                    Text("Activity could not load")
                    Text("Showing your last successfully loaded activity.")
                    TextButton(onClick = { refresh() }) { Text("Try again") }
                }
                if (activityLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            item { SpendingPeriodSelector(false, {}, { payCycleMode = true }) }

            item {
                ActivityMonthNavigator(
                    selectedMonthEpochMs =
                        selectedMonthEpochMs,
                    currentMonthEpochMs =
                        currentMonthEpochMs,
                    onPrevious = {
                        selectMonth(
                            shiftMonth(
                                selectedMonthEpochMs,
                                -1
                            )
                        )
                    },
                    onNext = {
                        if (
                            selectedMonthEpochMs <
                            currentMonthEpochMs
                        ) {
                            selectMonth(
                                shiftMonth(
                                    selectedMonthEpochMs,
                                    1
                                )
                            )
                        }
                    },
                    onPickMonth = {
                        showMonthPicker = true
                    }
                )
            }

            item(key = "monthly-summary-toggle") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activity-summary-toggle")
                        .semantics {
                            stateDescription = if (showDetailedSummary) "Expanded" else "Collapsed"
                        }
                        .clickable { showDetailedSummary = !showDetailedSummary },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "$selectedMonthLabel totals",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "In " + formatActivityMoney(snapshot.summary.incomeMinor) +
                                    "  •  Spent " + formatActivityMoney(snapshot.summary.spentMinor),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                        Icon(
                            if (showDetailedSummary) Icons.Rounded.ExpandLess
                            else Icons.Rounded.ExpandMore,
                            contentDescription = if (showDetailedSummary) "Hide monthly details"
                            else "Show monthly details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (showDetailedSummary) {
                item(key = "monthly-summary-details") {
                    ActivitySummaryCard(
                        snapshot = snapshot,
                        previousSnapshot = previousSnapshot,
                        monthLabel = selectedMonthLabel
                    )
                }
            }

            val needsAttentionCount =
                globalOverview.reviewGroups
                    .sumOf {
                        it.transactionCount
                    } +
                    globalOverview
                        .possibleDuplicates
                        .size +
                    relationshipReviews.size +
                    reviewableFinancialEntities
                        .size +
                    identityReviewCount

            if (needsAttentionCount > 0 || selectedGlobalQueue == "Attention") item {
                NeedsAttentionCard(
                    count = needsAttentionCount,
                    expanded =
                        selectedGlobalQueue ==
                            "Attention",
                    onClick = {
                        selectedGlobalQueue =
                            if (
                                selectedGlobalQueue ==
                                "Attention"
                            ) {
                                null
                            } else {
                                "Attention"
                            }
                    }
                )
            }

            if (
                selectedGlobalQueue ==
                "Attention"
            ) {
                if (needsAttentionCount == 0) {
                    item {
                        EmptyGlobalReviewState()
                    }
                }

                if (
                    reviewableFinancialEntities
                        .isNotEmpty()
                ) {
                    item {
                        FinancialAccountDiscoveryCard(
                            entity =
                                reviewableFinancialEntities
                                    .first(),
                            diagnostics =
                                financialDiagnostics,
                            accountsById =
                                accountsById,
                            evidence = ownershipEvidence[reviewableFinancialEntities.first().entityKey] ?: OwnershipReviewEvidence(),
                            remainingCount =
                                reviewableFinancialEntities
                                    .size - 1,
                            onReview = {
                                accountReviewEntity =
                                    reviewableFinancialEntities
                                        .first()
                            }
                        )
                    }
                }

                if (
                    identityReviewCount > 0
                ) {
                    item {
                        AttentionQuestionHeader(
                            title =
                                "Your names and numbers",
                            subtitle =
                                "Check possible own-account transfers."
                        )
                    }
                    item {
                        AttentionNavigationCard(
                            icon =
                                Icons.Rounded
                                    .AccountBalanceWallet,
                            title =
                                "$identityReviewCount identity question" +
                                    if (
                                        identityReviewCount ==
                                        1
                                    ) {
                                        ""
                                    } else {
                                        "s"
                                    },
                            subtitle =
                                "Review names and phone numbers linked to possible own-account transfers.",
                            tone = "blue",
                            onClick =
                                onIdentityReviewClick
                        )
                    }
                }


                if (
                    globalOverview
                        .reviewGroups
                        .isNotEmpty()
                ) {
                    item {
                        AttentionQuestionHeader(
                            title =
                                "What were these transactions?",
                            subtitle =
                                "Confirm a category once and PesaTrack can learn the stable identity when the evidence is strong."
                        )
                    }
                    items(
                        count =
                            globalOverview
                                .reviewGroups
                                .size,
                        key = { index ->
                            "attention-review-" +
                                globalOverview
                                    .reviewGroups[
                                        index
                                    ]
                                    .key
                        }
                    ) { index ->
                        val group =
                            globalOverview
                                .reviewGroups[
                                    index
                                ]
                        ReviewGroupRow(
                            group = group,
                            onClick = {
                                onTransactionClick(
                                    group
                                        .representativeTransactionId
                                )
                            }
                        )
                    }
                }

                if (
                    globalOverview
                        .possibleDuplicates
                        .isNotEmpty()
                ) {
                    item {
                        AttentionQuestionHeader(
                            title =
                                "Are these the same movement?",
                            subtitle =
                                "PesaTrack keeps both source observations until you confirm whether they represent one real transaction."
                        )
                    }
                    items(
                        count =
                            globalOverview
                                .possibleDuplicates
                                .size,
                        key = { index ->
                            "attention-duplicate-" +
                                globalOverview
                                    .possibleDuplicates[
                                        index
                                    ]
                                    .key
                        }
                    ) { index ->
                        val group =
                            globalOverview
                                .possibleDuplicates[
                                    index
                                ]
                        PossibleDuplicateRow(
                            group = group,
                            onSameTransaction = {
                                activityScope.launch {
                                    withContext(
                                        Dispatchers.IO
                                    ) {
                                        store.resolvePossibleDuplicate(
                                            pairKey =
                                                group.key,
                                            sameTransaction =
                                                true
                                        )
                                    }
                                    refresh()
                                }
                            },
                            onKeepBoth = {
                                activityScope.launch {
                                    withContext(
                                        Dispatchers.IO
                                    ) {
                                        store.resolvePossibleDuplicate(
                                            pairKey =
                                                group.key,
                                            sameTransaction =
                                                false
                                        )
                                    }
                                    refresh()
                                }
                            }
                        )
                    }
                }

                item {
                    TextButton(onClick = { showEvidenceLinks = !showEvidenceLinks }) {
                        Text(if (showEvidenceLinks) "Hide optional repayment links" else "Show optional repayment links")
                    }
                    if (showEvidenceLinks) Text("These repayments are already recorded. Linking their supporting messages is optional.")
                }
                val displayedRelationshipReviews = relationshipReviews + optionalEvidenceLinks
                if (
                    displayedRelationshipReviews
                        .isNotEmpty()
                ) {
                    item {
                        AttentionQuestionHeader(
                            title =
                                "Which events belong together?",
                            subtitle =
                                "Link repayments, balances and settlements only when the evidence is clear."
                        )
                    }
                    items(
                        count =
                            displayedRelationshipReviews.size,
                        key = { index ->
                            "attention-link-" +
                                displayedRelationshipReviews[
                                    index
                                ].key
                        }
                    ) { index ->
                        val review =
                            displayedRelationshipReviews[
                                index
                            ]
                        RelationshipReviewRow(
                            item = review,
                            onLink = { candidate ->
                                activityScope.launch {
                                    withContext(
                                        Dispatchers.IO
                                    ) {
                                        when (
                                            review.kind
                                        ) {
                                            RelationshipReviewKind
                                                .REPAYMENT_FUNDING -> {
                                                val eventId =
                                                    review
                                                        .debtEventId
                                                val targetId =
                                                    candidate
                                                        .targetId
                                                if (
                                                    eventId !=
                                                    null &&
                                                    targetId !=
                                                    null
                                                ) {
                                                    relationshipReviewStore
                                                        .resolveFunding(
                                                            eventId,
                                                            targetId
                                                        )
                                                }
                                            }

                                            RelationshipReviewKind
                                                .REPAYMENT_BALANCE -> {
                                                val eventId =
                                                    review
                                                        .debtEventId
                                                val targetId =
                                                    candidate
                                                        .targetId
                                                if (
                                                    eventId !=
                                                    null &&
                                                    targetId !=
                                                    null
                                                ) {
                                                    relationshipReviewStore
                                                        .resolveBalance(
                                                            eventId,
                                                            targetId
                                                        )
                                                }
                                            }

                                            RelationshipReviewKind
                                                .LIABILITY_PAYMENT -> {
                                                val transactionId =
                                                    review
                                                        .transactionId
                                                val debtAccountId =
                                                    candidate
                                                        .targetKey
                                                if (
                                                    transactionId !=
                                                    null &&
                                                    debtAccountId !=
                                                    null
                                                ) {
                                                    relationshipReviewStore
                                                        .resolveLiabilityPayment(
                                                            transactionId,
                                                            debtAccountId
                                                        )
                                                }
                                            }

                                            RelationshipReviewKind
                                                .INVESTMENT_SETTLEMENT -> {
                                                val eventId =
                                                    review
                                                        .investmentEventId
                                                val targetId =
                                                    candidate
                                                        .targetId
                                                if (
                                                    eventId !=
                                                    null &&
                                                    targetId !=
                                                    null
                                                ) {
                                                    relationshipReviewStore
                                                        .resolveInvestmentSettlement(
                                                            eventId,
                                                            targetId
                                                        )
                                                }
                                            }
                                        }
                                    }
                                    refresh()
                                }
                            },
                            onKeepUnlinked = {
                                activityScope.launch {
                                    withContext(
                                        Dispatchers.IO
                                    ) {
                                        when (
                                            review.kind
                                        ) {
                                            RelationshipReviewKind
                                                .REPAYMENT_FUNDING ->
                                                review.debtEventId
                                                    ?.let {
                                                        relationshipReviewStore
                                                            .resolveFunding(
                                                                it,
                                                                null
                                                            )
                                                    }

                                            RelationshipReviewKind
                                                .REPAYMENT_BALANCE ->
                                                review.debtEventId
                                                    ?.let {
                                                        relationshipReviewStore
                                                            .resolveBalance(
                                                                it,
                                                                null
                                                            )
                                                    }

                                            RelationshipReviewKind
                                                .LIABILITY_PAYMENT ->
                                                review.transactionId
                                                    ?.let {
                                                        relationshipReviewStore
                                                            .resolveLiabilityPayment(
                                                                it,
                                                                null
                                                            )
                                                    }

                                            RelationshipReviewKind
                                                .INVESTMENT_SETTLEMENT ->
                                                review.investmentEventId
                                                    ?.let {
                                                        relationshipReviewStore
                                                            .resolveInvestmentSettlement(
                                                                it,
                                                                null
                                                            )
                                                    }
                                        }
                                    }
                                    refresh()
                                }
                            }
                        )
                    }
                }
            }

            monthBuckets.forEach { bucket ->
                val isExpanded =
                    expandedMonthEpochMs ==
                        bucket.monthStartEpochMs

                item(
                    key =
                        "month-" +
                            bucket.monthStartEpochMs
                ) {
                    ActivityMonthSectionHeader(
                        monthLabel = monthLabel(
                            bucket.monthStartEpochMs
                        ),
                        transactionCount =
                            bucket.transactionCount,
                        expanded = isExpanded,
                        onClick = {
                            if (isExpanded) {
                                expandedMonthEpochMs =
                                    null
                            } else {
                                selectMonth(
                                    bucket
                                        .monthStartEpochMs
                                )
                            }
                        }
                    )
                }

                if (isExpanded) {
                    item(
                        key =
                            "filters-" +
                                bucket
                                    .monthStartEpochMs
                    ) {
                        LazyRow(
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {
                            listOf(
                                "All",
                                "Needs review",
                                "Spending",
                                "Income",
                                "Transfers"
                            ).forEach { label ->
                                item {
                                    FilterChip(
                                        selected =
                                            selectedFilter ==
                                                label,
                                        onClick = {
                                            selectedFilter =
                                                label
                                        },
                                        label = {
                                            Text(label)
                                        },
                                        shape =
                                            RoundedCornerShape(
                                                12.dp
                                            ),
                                        colors =
                                            FilterChipDefaults
                                                .filterChipColors(
                                                    selectedContainerColor =
                                                        MaterialTheme
                                                            .colorScheme
                                                            .primary,
                                                    selectedLabelColor =
                                                        MaterialTheme
                                                            .colorScheme
                                                            .onPrimary
                                                )
                                    )
                                }
                            }
                        }
                    }

                    if (selectedFinancialSource != null) {
                        item(
                            key =
                                "source-feed-header-" +
                                    bucket.monthStartEpochMs
                        ) {
                            FinancialSourceFeedHeader(
                                source =
                                    sourceSummaries
                                        .firstOrNull {
                                            it.key ==
                                                selectedFinancialSource
                                        },
                                messageCount =
                                    selectedSourceDiagnostics
                                        .size
                            )
                        }

                        if (
                            selectedSourceDiagnostics
                                .isEmpty()
                        ) {
                            item(
                                key =
                                    "source-feed-empty-" +
                                        bucket
                                            .monthStartEpochMs
                            ) {
                                EmptyFinancialSourceFeed()
                            }
                        } else {
                            selectedSourceDiagnostics
                                .groupBy {
                                    dayStart(
                                        it.receivedAtEpochMs
                                    )
                                }
                                .toList()
                                .sortedByDescending {
                                    it.first
                                }
                                .forEach {
                                        (
                                            dayEpochMs,
                                            messages
                                        ) ->
                                    item(
                                        key =
                                            "source-day-" +
                                                dayEpochMs
                                    ) {
                                        FinancialMessageDateHeader(
                                            dayEpochMs =
                                                dayEpochMs,
                                            count =
                                                messages.size
                                        )
                                    }
                                    items(
                                        count =
                                            messages.size,
                                        key = { index ->
                                            "source-message-" +
                                                messages[
                                                    index
                                                ]
                                                    .receivedAtEpochMs +
                                                "-" +
                                                index
                                        }
                                    ) { index ->
                                        CapturedFinancialMessageRow(
                                            diagnostic =
                                                messages[
                                                    index
                                                ]
                                        )
                                    }
                                }
                        }
                    } else if (filtered.isEmpty()) {
                        item(
                            key =
                                "empty-" +
                                    bucket
                                        .monthStartEpochMs
                        ) {
                            EmptyActivityState(
                                selectedFilter =
                                    selectedFilter,
                                capturedCount =
                                    snapshot
                                        .transactions
                                        .size,
                                monthLabel =
                                    selectedMonthLabel
                            )
                        }
                    } else {
                        groupedFiltered.forEach {
                                (dayEpochMs, transactions) ->
                            item(
                                key = "day-" +
                                    bucket
                                        .monthStartEpochMs +
                                    "-" +
                                    dayEpochMs
                            ) {
                                ActivityDateHeader(
                                    dayEpochMs =
                                        dayEpochMs,
                                    transactions =
                                        transactions
                                )
                            }
                            items(
                                items = transactions,
                                key = { transaction ->
                                    buildString {
                                        append(transaction.id)
                                        append(':')
                                        append(transaction.category)
                                        append(':')
                                        append(transaction.direction)
                                        append(':')
                                        append(transaction.reviewState)
                                    }
                                }
                            ) { transaction ->
                                ActivityRow(
                                    item = transaction,
                                    accountName =
                                        transaction
                                            .accountId
                                            ?.takeIf {
                                                it in
                                                    confirmedAccountIds ||
                                                    it ==
                                                        AccountStore
                                                            .DEFAULT_MPESA_ID ||
                                                    it ==
                                                        AccountStore
                                                            .DEFAULT_CASH_ID
                                            }
                                            ?.let {
                                                accountsById[
                                                    it
                                                ]?.name
                                            },
                                    linkedAccountName =
                                        transaction
                                            .linkedAccountId
                                            ?.takeIf {
                                                it in
                                                    confirmedAccountIds ||
                                                    it ==
                                                        AccountStore
                                                            .DEFAULT_MPESA_ID ||
                                                    it ==
                                                        AccountStore
                                                            .DEFAULT_CASH_ID
                                            }
                                            ?.let {
                                                accountsById[
                                                    it
                                                ]?.name
                                            },
                                    onClick = {
                                        onTransactionClick(
                                            transaction.id
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMonthPicker) {
        ActivityMonthPickerDialog(
            selectedMonthEpochMs =
                selectedMonthEpochMs,
            currentMonthEpochMs =
                currentMonthEpochMs,
            onDismiss = {
                showMonthPicker = false
            },
            onMonthSelected = { epochMs ->
                selectMonth(epochMs)
                showMonthPicker = false
            }
        )
    }

    accountReviewEntity?.let { entity ->
        FinancialAccountReviewDialog(
            evidence = ownershipEvidence[entity.entityKey] ?: OwnershipReviewEvidence(),
            entity = entity,
            diagnostics =
                financialDiagnostics,
            accountsById =
                accountsById,
            isWorking =
                accountReviewBusyKey ==
                    entity.entityKey,
            errorText =
                accountReviewError,
            onDismiss = {
                if (
                    accountReviewBusyKey ==
                    null
                ) {
                    accountReviewError =
                        null
                    accountReviewEntity =
                        null
                }
            },
            onConfirmOwned = {
                if (
                    accountReviewBusyKey ==
                    null
                ) {
                    accountReviewBusyKey =
                        entity.entityKey
                    accountReviewError =
                        null
                    activityScope.launch {
                        val result =
                            withContext(
                                Dispatchers.IO
                            ) {
                                runCatching {
                                    financialStore
                                        .confirmEntityOwned(
                                            entity.entityKey
                                        )
                                        ?: error(
                                            "Account confirmation did not persist."
                                        )
                                    financialCoordinator
                                        .reconcilePendingTransfers(
                                            entity.entityKey
                                        )
                                }
                            }

                        result.onSuccess {
                            accountReviewBusyKey =
                                null
                            accountReviewEntity =
                                null
                            refresh()

                            activityScope.launch(
                                Dispatchers.IO
                            ) {
                                runCatching {
                                    HistoryImportWindow
                                        .importStartEpochMs(
                                            3
                                        )
                                        ?.let {
                                            HistoricalSmsImporter(
                                                context
                                                    .applicationContext
                                            ).importSince(
                                                it
                                            )
                                        }
                                }
                            }
                        }.onFailure {
                            accountReviewBusyKey =
                                null
                            accountReviewError =
                                "PesaTrack could not confirm this account yet. Please try again."
                        }
                    }
                }
            },
            onNotMine = {
                if (
                    accountReviewBusyKey ==
                    null
                ) {
                    accountReviewBusyKey =
                        entity.entityKey
                    accountReviewError =
                        null
                    activityScope.launch {
                        val result =
                            withContext(
                                Dispatchers.IO
                            ) {
                                runCatching {
                                    financialStore
                                        .markEntityExternal(
                                            entity.entityKey
                                        )
                                    financialCoordinator
                                        .reconcileExternalEntity(
                                            entity.entityKey
                                        )
                                }
                            }

                        result.onSuccess {
                            accountReviewBusyKey =
                                null
                            accountReviewEntity =
                                null
                            refresh()
                        }.onFailure {
                            accountReviewBusyKey =
                                null
                            accountReviewError =
                                "PesaTrack could not save this decision yet. Please try again."
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun FinancialAccountDiscoveryCard(
    entity: DiscoveredFinancialEntity,
    diagnostics:
        List<FinancialIngestionDiagnostic>,
    accountsById:
        Map<String, com.kedevjimmy.pesatrack.domain.model.MoneyAccount>,
    evidence: OwnershipReviewEvidence,
    remainingCount: Int,
    onReview: () -> Unit
) {
    val displayName =
        financialEntityReviewDisplayName(
            entity = entity,
            diagnostics = diagnostics,
            accountsById = accountsById
        )

    PesaCard {
        Column(
            modifier =
                Modifier.padding(15.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                IconBubble(
                    icon =
                        Icons.Rounded
                            .AccountBalance,
                    size = 44.dp,
                    background =
                        pesaToneContainer("blue"),
                    tint =
                        pesaToneContent("blue")
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        "Is this your account?",
                        style =
                            MaterialTheme.typography
                                .titleMedium
                    )
                    Text(
                        displayName,
                        style =
                            MaterialTheme.typography
                                .titleLarge
                    )
                }
                SmallStatusPill("Review")
            }

            Text(
                ownershipEvidenceSummary(evidence),
                style =
                    MaterialTheme.typography
                        .bodyMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
            Text(
                ownershipEvidenceLabel(
                    entity
                ),
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            if (remainingCount > 0) {
                Text(
                    "+" + remainingCount +
                        " more account" +
                        if (
                            remainingCount == 1
                        ) {
                            ""
                        } else {
                            "s"
                        } +
                        " waiting for review",
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            Button(
                onClick = onReview,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("Review account")
            }
        }
    }
}

@Composable
private fun FinancialAccountReviewDialog(
    entity: DiscoveredFinancialEntity,
    diagnostics:
        List<FinancialIngestionDiagnostic>,
    accountsById:
        Map<String, com.kedevjimmy.pesatrack.domain.model.MoneyAccount>,
    evidence: OwnershipReviewEvidence,
    isWorking: Boolean,
    errorText: String?,
    onDismiss: () -> Unit,
    onConfirmOwned: () -> Unit,
    onNotMine: () -> Unit
) {
    val displayName =
        financialEntityReviewDisplayName(
            entity = entity,
            diagnostics = diagnostics,
            accountsById = accountsById
        )

    AlertDialog(
        onDismissRequest = {
            if (!isWorking) {
                onDismiss()
            }
        },
        title = {
            Text(
                "Is this your account?"
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    displayName,
                    style =
                        MaterialTheme.typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.SemiBold
                )
                Text(
                    ownershipEvidenceLabel(
                        entity
                    ),
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
                Text(
                    ownershipEvidenceSummary(evidence)
                )
                Text(
                    "Yes lets PesaTrack treat transfers between your accounts as your own money moving.",
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
                Text(
                    if (isWorking) {
                        "Saving your decision…"
                    } else {
                        "Choose Yes only if you own this account."
                    },
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .primary
                )
                errorText?.let {
                    Text(
                        it,
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick =
                    onConfirmOwned,
                enabled =
                    !isWorking
            ) {
                Text(
                    if (isWorking) {
                        "Saving…"
                    } else {
                        "Yes"
                    }
                )
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                TextButton(
                    onClick =
                        onDismiss,
                    enabled =
                        !isWorking
                ) {
                    Text("Not now")
                }
                OutlinedButton(
                    onClick =
                        onNotMine,
                    enabled =
                        !isWorking
                ) {
                    Text("No")
                }
            }
        }
    )
}

@Composable
private fun ActivityMonthNavigator(
    selectedMonthEpochMs: Long,
    currentMonthEpochMs: Long,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPickMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onPickMonth),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 13.dp,
                    vertical = 11.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    monthLabel(selectedMonthEpochMs),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 9.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Choose month",
                    tint =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        MonthArrowButton(
            icon = Icons.Rounded.ChevronLeft,
            contentDescription = "Previous month",
            enabled = true,
            onClick = onPrevious
        )
        MonthArrowButton(
            icon = Icons.Rounded.ChevronRight,
            contentDescription = "Next month",
            enabled =
                selectedMonthEpochMs <
                    currentMonthEpochMs,
            onClick = onNext
        )
    }
}

@Composable
private fun MonthArrowButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(42.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant
                .copy(alpha = .45f)
        }
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                        .copy(alpha = .45f)
                }
            )
        }
    }
}

@Composable
private fun ActivityMonthPickerDialog(
    selectedMonthEpochMs: Long,
    currentMonthEpochMs: Long,
    onDismiss: () -> Unit,
    onMonthSelected: (Long) -> Unit
) {
    val selectedCalendar =
        remember(selectedMonthEpochMs) {
            Calendar.getInstance().apply {
                timeInMillis =
                    selectedMonthEpochMs
            }
        }
    val currentLocale =
        androidx.compose.ui.platform
            .LocalConfiguration
            .current
            .locales[0]
    val currentCalendar =
        remember(currentMonthEpochMs) {
            Calendar.getInstance().apply {
                timeInMillis =
                    currentMonthEpochMs
            }
        }
    var displayedYear by remember(
        selectedMonthEpochMs
    ) {
        mutableStateOf(
            selectedCalendar.get(
                Calendar.YEAR
            )
        )
    }
    val selectedYear =
        selectedCalendar.get(Calendar.YEAR)
    val selectedMonth =
        selectedCalendar.get(Calendar.MONTH)
    val currentYear =
        currentCalendar.get(Calendar.YEAR)
    val monthNames = remember(currentLocale) {
        (0..11).map { month ->
            Calendar.getInstance().apply {
                clear()
                set(2000, month, 1)
            }.let { calendar ->
                SimpleDateFormat(
                    "MMM",
                    currentLocale
                ).format(calendar.time)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Choose activity month")
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            displayedYear -= 1
                        }
                    ) {
                        Icon(
                            Icons.Rounded
                                .ChevronLeft,
                            contentDescription =
                                "Previous year"
                        )
                    }
                    Text(
                        displayedYear.toString(),
                        modifier =
                            Modifier.weight(1f),
                        style = MaterialTheme
                            .typography
                            .titleLarge,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = {
                            displayedYear += 1
                        },
                        enabled =
                            displayedYear <
                                currentYear
                    ) {
                        Icon(
                            Icons.Rounded
                                .ChevronRight,
                            contentDescription =
                                "Next year"
                        )
                    }
                }

                (0 until 12 step 3).forEach {
                    startMonth ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {
                        (
                            startMonth until
                                startMonth + 3
                            ).forEach { month ->
                            val candidate =
                                monthStart(
                                    displayedYear,
                                    month
                                )
                            FilterChip(
                                modifier =
                                    Modifier.weight(
                                        1f
                                    ),
                                selected =
                                    displayedYear ==
                                        selectedYear &&
                                        month ==
                                        selectedMonth,
                                onClick = {
                                    onMonthSelected(
                                        candidate
                                    )
                                },
                                enabled =
                                    candidate <=
                                        currentMonthEpochMs,
                                label = {
                                    Text(
                                        monthNames[
                                            month
                                        ],
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun NeedsAttentionCard(
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit
) {
    PesaCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                )
    ) {
        Row(
            modifier =
                Modifier.padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            IconBubble(
                Icons.AutoMirrored.Rounded
                    .FactCheck,
                size = 44.dp,
                background =
                    if (count > 0) {
                        pesaToneContainer(
                            "amber"
                        )
                    } else {
                        pesaToneContainer(
                            "green"
                        )
                    },
                tint =
                    if (count > 0) {
                        pesaToneContent(
                            "amber"
                        )
                    } else {
                        pesaToneContent(
                            "green"
                        )
                    }
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(
                            horizontal = 12.dp
                        )
            ) {
                Text(
                    "Needs Attention",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )
                Text(
                    if (count > 0) {
                        "$count item" +
                            if (count == 1) {
                                " needs"
                            } else {
                                "s need"
                            } +
                            " your help"
                    } else {
                        "Your captured money picture has no open questions here."
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
            }
            SmallStatusPill(
                if (count > 0) {
                    count.toString()
                } else {
                    "Clear"
                },
                if (count > 0) {
                    "amber"
                } else {
                    "green"
                }
            )
            Icon(
                if (expanded) {
                    Icons.Rounded
                        .ExpandLess
                } else {
                    Icons.Rounded
                        .ExpandMore
                },
                contentDescription =
                    if (expanded) {
                        "Hide questions"
                    } else {
                        "Show questions"
                    },
                modifier =
                    Modifier.padding(
                        start = 8.dp
                    ),
                tint =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AttentionNavigationCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tone: String,
    onClick: () -> Unit
) {
    PesaCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                )
    ) {
        Row(
            modifier =
                Modifier.padding(15.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            IconBubble(
                icon = icon,
                size = 42.dp,
                background =
                    pesaToneContainer(
                        tone
                    ),
                tint =
                    pesaToneContent(
                        tone
                    )
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(
                            horizontal = 12.dp
                        )
            ) {
                Text(
                    title,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
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
                            .onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AttentionQuestionHeader(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            title,
            style =
                MaterialTheme
                    .typography
                    .titleLarge
        )
        Text(
            subtitle,
            modifier =
                Modifier.padding(
                    top = 3.dp
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

@Composable
private fun ActivityQueueButton(
    label: String,
    count: Int,
    icon: ImageVector,
    selected: Boolean,
    tone: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = if (selected) {
        if (tone == "green") {
            MaterialTheme.colorScheme.primary
        } else {
            pesaToneContainer(tone)
        }
    } else {
        MaterialTheme.colorScheme.surface
    }
    val foreground = if (
        selected &&
        tone == "green"
    ) {
        MaterialTheme.colorScheme.onPrimary
    } else if (selected) {
        pesaToneContent(tone)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = background,
        border = if (selected) {
            null
        } else {
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline
                    .copy(alpha = .65f)
            )
        }
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 12.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = foreground
            )
            Text(
                label,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = foreground
            )
            if (count > 0) {
                Text(
                    count.toString(),
                    modifier = Modifier.padding(start = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = foreground.copy(alpha = .76f)
                )
            }
        }
    }
}

@Composable
private fun GlobalReviewHeader(
    overview: ActivityReviewOverview
) {
    val pendingTransactions =
        overview.reviewGroups.sumOf {
            it.transactionCount
        }

    Column {
        Text(
            "Review all months",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            pendingTransactions.toString() +
                " pending transaction" +
                (
                    if (pendingTransactions == 1) {
                        ""
                    } else {
                        "s"
                    }
                ) +
                " across all captured months. Teach an identity once and matching history can follow.",
            modifier = Modifier.padding(top = 3.dp),
            style = MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GlobalDuplicateHeader(
    overview: ActivityReviewOverview
) {
    Text(
        overview.possibleDuplicates.size.toString() +
            " possible duplicate pair" +
            (
                if (
                    overview.possibleDuplicates.size == 1
                ) {
                    ""
                } else {
                    "s"
                }
            ) +
            " across all captured months",
        style = MaterialTheme.typography.titleLarge
    )
}

@Composable
private fun EmptyGlobalReviewState() {
    PesaCard {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            IconBubble(
                Icons.AutoMirrored.Rounded.HelpOutline,
                size = 52.dp,
                background =
                    pesaToneContainer("green"),
                tint =
                    pesaToneContent("green")
            )
            Text(
                "Nothing needs review",
                modifier =
                    Modifier.padding(top = 12.dp),
                style =
                    MaterialTheme.typography
                        .titleMedium
            )
            Text(
                "There are no pending review groups across the captured months.",
                modifier =
                    Modifier.padding(top = 5.dp),
                style =
                    MaterialTheme.typography
                        .bodyMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyGlobalDuplicateState() {
    PesaCard {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            IconBubble(
                Icons.Rounded.WarningAmber,
                size = 52.dp,
                background =
                    pesaToneContainer("green"),
                tint =
                    pesaToneContent("green")
            )
            Text(
                "No possible duplicates",
                modifier =
                    Modifier.padding(top = 12.dp),
                style =
                    MaterialTheme.typography
                        .titleMedium
            )
            Text(
                "No unresolved duplicate pairs remain across the captured months.",
                modifier =
                    Modifier.padding(top = 5.dp),
                style =
                    MaterialTheme.typography
                        .bodyMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivityMonthSectionHeader(
    monthLabel: String,
    transactionCount: Int,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (expanded) 2.dp else 0.dp
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    monthLabel + " activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    transactionCount.toString() +
                        " transaction" +
                        if (transactionCount == 1) "" else "s",
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (expanded) {
                    Icons.Rounded.ExpandLess
                } else {
                    Icons.Rounded.ExpandMore
                },
                contentDescription = if (expanded) {
                    "Collapse month"
                } else {
                    "Expand month"
                }
            )
        }
    }
}

@Composable
private fun LiveActivityUpdateStatus() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(
                "Updating activity…",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "A financial notification is being checked and reconciled.",
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ActivitySummaryCard(
    snapshot: ActivityLedgerSnapshot,
    previousSnapshot: ActivityLedgerSnapshot,
    monthLabel: String
) {
    val summary = snapshot.summary
    val previous = previousSnapshot.summary
    val savingsMinor = monthSavings(snapshot)
    val previousSavingsMinor =
        monthSavings(previousSnapshot)
    val dark = isPesaDarkTheme()
    val gradient = if (dark) {
        listOf(
            Color(0xFF24213F),
            Color(0xFF3933A8)
        )
    } else {
        listOf(
            Color(0xFF4F46D8),
            Color(0xFF6F63F2)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("activity-summary-details")
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(gradient)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(11.dp),
                    color = Color.White.copy(alpha = .14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                            tint = Color(0xFFF4C768)
                        )
                    }
                }
                Text(
                    monthLabel + " Summary",
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(18.dp)
            ) {
                SummaryAmountWithChange(
                    label = "Money in",
                    value = formatActivityMoney(
                        summary.incomeMinor
                    ),
                    current = summary.incomeMinor,
                    previous = previous.incomeMinor,
                    positiveWhenUp = true,
                    zeroComparisonLabel =
                        "No income recorded this month",
                    modifier = Modifier.weight(1f)
                )
                SummaryAmountWithChange(
                    label = "Money spent",
                    value = formatActivityMoney(
                        summary.spentMinor
                    ),
                    current = summary.spentMinor,
                    previous = previous.spentMinor,
                    positiveWhenUp = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp),
                shape = RoundedCornerShape(15.dp),
                color = if (dark) {
                    Color(0xFF3A2E1C)
                } else {
                    Color(0xFFFFE8AC)
                }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = RoundedCornerShape(11.dp),
                        color = MaterialTheme
                            .colorScheme
                            .primary
                            .copy(alpha = .13f)
                    ) {
                        Box(
                            contentAlignment =
                                Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded
                                    .AccountBalanceWallet,
                                contentDescription = null,
                                modifier =
                                    Modifier.size(21.dp),
                                tint =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp)
                    ) {
                        Text(
                            "Saved & invested",
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
                            savingsMinor?.let(::formatActivityMoney) ?: "Amount unavailable",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    SavingsChangeBadge(
                        current = savingsMinor,
                        previous =
                            previousSavingsMinor
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryAmountWithChange(
    label: String,
    value: String,
    current: Long,
    previous: Long,
    positiveWhenUp: Boolean,
    zeroComparisonLabel: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = .72f)
        )
        Text(
            value,
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
        SummaryComparison(
            current = current,
            previous = previous,
            positiveWhenUp = positiveWhenUp,
            zeroComparisonLabel =
                zeroComparisonLabel
        )
    }
}

@Composable
private fun SummaryComparison(
    current: Long,
    previous: Long,
    positiveWhenUp: Boolean,
    zeroComparisonLabel: String? = null
) {
    if (
        current == 0L &&
        zeroComparisonLabel != null
    ) {
        Text(
            zeroComparisonLabel,
            modifier =
                Modifier.padding(
                    top = 4.dp
                ),
            style =
                MaterialTheme
                    .typography
                    .labelSmall,
            color =
                Color.White.copy(
                    alpha = .68f
                )
        )
        return
    }

    val percent = percentageChange(
        current = current,
        previous = previous
    )

    if (percent == null) {
        Text(
            if (current == 0L) {
                "No change from prior month"
            } else {
                "New vs prior month"
            },
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = .68f)
        )
        return
    }

    val up = percent > 0
    val favorable =
        if (positiveWhenUp) up else !up
    val tint = if (percent == 0) {
        Color.White.copy(alpha = .66f)
    } else if (favorable) {
        Color(0xFF55E6D1)
    } else {
        Color(0xFFFF92A3)
    }

    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (percent != 0) {
            Icon(
                if (up) {
                    Icons.Rounded.ArrowUpward
                } else {
                    Icons.Rounded.ArrowDownward
                },
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            if (percent == 0) {
                "0% from prior month"
            } else {
                kotlin.math.abs(percent)
                    .toString() +
                    "% from prior month"
            },
            modifier = Modifier.padding(
                start = if (percent == 0) {
                    0.dp
                } else {
                    2.dp
                }
            ),
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

@Composable
private fun SavingsChangeBadge(
    current: Long?,
    previous: Long?
) {
    if (current == null || previous == null) {
        Text("Comparison unavailable")
        return
    }
    val percent = percentageChange(
        current = current,
        previous = previous
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme
            .colorScheme
            .primary
            .copy(alpha = .12f)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 9.dp,
                vertical = 5.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            if (
                percent != null &&
                percent != 0
            ) {
                Icon(
                    if (percent > 0) {
                        Icons.Rounded.ArrowUpward
                    } else {
                        Icons.Rounded.ArrowDownward
                    },
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (percent > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            Text(
                when {
                    percent == null &&
                        current > 0L ->
                        "New"
                    percent == null ->
                        "No change"
                    else ->
                        kotlin.math.abs(percent)
                            .toString() + "%"
                },
                modifier = Modifier.padding(
                    start = if (
                        percent != null &&
                        percent != 0
                    ) {
                        2.dp
                    } else {
                        0.dp
                    }
                ),
                style =
                    MaterialTheme.typography
                        .labelMedium,
                color = when {
                    percent == null ||
                        percent >= 0 ->
                        MaterialTheme
                            .colorScheme
                            .primary
                    else ->
                        MaterialTheme
                            .colorScheme
                            .error
                }
            )
        }
    }
}

private fun percentageChange(
    current: Long,
    previous: Long
): Int? {
    if (previous == 0L) return null
    return (
        ((current.toDouble() - previous.toDouble()) * 100.0) /
            kotlin.math.abs(previous.toDouble())
        ).toInt()
}

private fun monthSavings(
    snapshot: ActivityLedgerSnapshot
): Long? = CheckedMoney.orNull {
    snapshot.transactions
        .filter {
            it.category in setOf(
                TransactionCategory.SAVINGS.name,
                TransactionCategory.INVESTMENT.name
            ) &&
                it.reviewState !=
                    ReviewState.EXCLUDED.name &&
                it.reviewState !=
                    ReviewState.NEEDS_REVIEW.name
        }
        .checkedMoneySumOf { it.amountMinor }
}

@Composable
private fun ResponsiveActivityMetrics(
    snapshot: ActivityLedgerSnapshot,
    monthLabel: String
) {
    val summary = snapshot.summary
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 600.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActivityMetric(
                        "Spent",
                        formatActivityMoney(summary.spentMinor),
                        "$monthLabel",
                        Icons.Rounded.Paid,
                        "red",
                        Modifier.weight(1f)
                    )
                    ActivityMetric(
                        "Income",
                        formatActivityMoney(summary.incomeMinor),
                        "$monthLabel",
                        Icons.AutoMirrored.Rounded.CallReceived,
                        "green",
                        Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActivityMetric(
                        "Fees",
                        formatActivityMoney(summary.feesMinor),
                        "M-Pesa + Fuliza",
                        Icons.AutoMirrored.Rounded.ReceiptLong,
                        "amber",
                        Modifier.weight(1f)
                    )
                    ActivityMetric(
                        "Net",
                        formatSignedMoney(summary.netMinor),
                        "Cash flow",
                        Icons.Rounded.AccountBalance,
                        "blue",
                        Modifier.weight(1f)
                    )
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActivityMetric("Spent", formatActivityMoney(summary.spentMinor), "$monthLabel", Icons.Rounded.Paid, "red", Modifier.weight(1f))
                ActivityMetric("Income", formatActivityMoney(summary.incomeMinor), "$monthLabel", Icons.AutoMirrored.Rounded.CallReceived, "green", Modifier.weight(1f))
                ActivityMetric("Fees", formatActivityMoney(summary.feesMinor), "M-Pesa fees", Icons.AutoMirrored.Rounded.ReceiptLong, "amber", Modifier.weight(1f))
                ActivityMetric("Net", formatSignedMoney(summary.netMinor), "Cash flow", Icons.Rounded.AccountBalance, "blue", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActivityMetric(
    label: String,
    value: String,
    helper: String,
    icon: ImageVector,
    tone: String,
    modifier: Modifier
) {
    Column(modifier) {
        IconBubble(
            icon,
            size = 34.dp,
            background = pesaToneContainer(tone),
            tint = pesaToneContent(tone)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Text(
            helper,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyActivityState(
    selectedFilter: String,
    capturedCount: Int,
    monthLabel: String
) {
    PesaCard {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBubble(
                if (selectedFilter == "Review") Icons.AutoMirrored.Rounded.HelpOutline else Icons.AutoMirrored.Rounded.ReceiptLong,
                size = 52.dp,
                background = pesaToneContainer(if (selectedFilter == "Review") "amber" else "green"),
                tint = pesaToneContent(if (selectedFilter == "Review") "amber" else "green")
            )
            Text(
                if (selectedFilter == "Review") {
                    "Nothing to review in $monthLabel"
                } else if (capturedCount == 0) {
                    "No activity in $monthLabel"
                } else {
                    "Nothing in $selectedFilter for $monthLabel"
                },
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                if (selectedFilter == "Review") {
                    "Good — this month's captured transactions currently have no pending review groups."
                } else if (capturedCount == 0) {
                    "Choose another month or refresh after importing older M-Pesa and bank activity."
                } else {
                    "Try another filter to see more activity from $monthLabel."
                },
                modifier = Modifier.padding(top = 5.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RelationshipReviewHeader(
    count: Int
) {
    Column {
        Text(
            "$count financial relationship" +
                if (count == 1) {
                    " needs review"
                } else {
                    "s need review"
                },
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "PesaTrack found plausible links but deliberately refused to choose automatically.",
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyRelationshipReviewState() {
    PesaCard {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            IconBubble(
                Icons.Rounded.SyncAlt,
                size = 52.dp,
                background =
                    pesaToneContainer("green"),
                tint =
                    pesaToneContent("green")
            )
            Text(
                "No ambiguous links",
                modifier =
                    Modifier.padding(top = 12.dp),
                style =
                    MaterialTheme.typography
                        .titleMedium
            )
            Text(
                "PesaTrack currently has no debt, liability-payment, or investment-settlement relationships that need a manual decision.",
                modifier =
                    Modifier.padding(top = 5.dp),
                style =
                    MaterialTheme.typography
                        .bodyMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RelationshipReviewRow(
    item: RelationshipReviewItem,
    onLink: (RelationshipReviewCandidate) -> Unit,
    onKeepUnlinked: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                IconBubble(
                    Icons.Rounded.SyncAlt,
                    size = 42.dp,
                    background =
                        pesaToneContainer("amber"),
                    tint =
                        pesaToneContent("amber")
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 11.dp)
                ) {
                    Text(
                        when (item.kind) {
                            RelationshipReviewKind
                                .REPAYMENT_FUNDING ->
                                "Which incoming funds paid this loan?"
                            RelationshipReviewKind
                                .REPAYMENT_BALANCE ->
                                "Which balance came after this repayment?"
                            RelationshipReviewKind
                                .LIABILITY_PAYMENT ->
                                "Is this payment a debt repayment?"
                            RelationshipReviewKind
                                .INVESTMENT_SETTLEMENT ->
                                "Which cash movement settled this investment?"
                        },
                        style =
                            MaterialTheme.typography
                                .titleMedium
                    )
                    Text(
                        item.debtName +
                            " • " +
                            formatActivityMoney(
                                item.repaymentAmountMinor
                            ),
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            Text(
                (
                    when (item.kind) {
                        RelationshipReviewKind
                            .LIABILITY_PAYMENT ->
                            "Paid from: "
                        RelationshipReviewKind
                            .INVESTMENT_SETTLEMENT ->
                            "Investment event: "
                        else ->
                            "Funding account: "
                    }
                ) +
                    item.fundingAccountName +
                    " • " +
                    formatPreciseTime(
                        item.occurredAtEpochMs
                    ),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            item.candidates.forEachIndexed {
                    index,
                    candidate ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape =
                        RoundedCornerShape(14.dp),
                    color =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                            .copy(alpha = .45f)
                ) {
                    Column(
                        Modifier.padding(12.dp)
                    ) {
                        Text(
                            candidate.label,
                            style =
                                MaterialTheme
                                    .typography
                                    .labelLarge
                        )
                        Text(
                            formatActivityMoney(
                                candidate
                                    .amountMinor
                            ) +
                                " • " +
                                formatPreciseTime(
                                    candidate
                                        .occurredAtEpochMs
                                ),
                            modifier =
                                Modifier.padding(
                                    top = 2.dp
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
                            candidate.reason,
                            modifier =
                                Modifier.padding(
                                    top = 4.dp
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                pesaToneContent(
                                    "amber"
                                )
                        )
                        Button(
                            onClick = {
                                onLink(candidate)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                when (item.kind) {
                                    RelationshipReviewKind
                                        .REPAYMENT_FUNDING ->
                                        "Use as funding transaction"
                                    RelationshipReviewKind
                                        .REPAYMENT_BALANCE ->
                                        "Use this balance"
                                    RelationshipReviewKind
                                        .LIABILITY_PAYMENT ->
                                        "Mark as repayment to this debt"
                                    RelationshipReviewKind
                                        .INVESTMENT_SETTLEMENT ->
                                        "Use as investment settlement"
                                }
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onKeepUnlinked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text(
                    when (item.kind) {
                        RelationshipReviewKind
                            .REPAYMENT_FUNDING ->
                            "None of these funded it"
                        RelationshipReviewKind
                            .REPAYMENT_BALANCE ->
                            "None of these balances"
                        RelationshipReviewKind
                            .LIABILITY_PAYMENT ->
                            "This was not a debt repayment"
                        RelationshipReviewKind
                            .INVESTMENT_SETTLEMENT ->
                            "None of these settled it"
                    }
                )
            }

            Text(
                "Your choice is remembered and automatic reconciliation will not overwrite it.",
                modifier =
                    Modifier.padding(top = 8.dp),
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PossibleDuplicateCallout(
    pairCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                Icons.Rounded.WarningAmber,
                size = 42.dp,
                background =
                    MaterialTheme.colorScheme.surface.copy(alpha = .7f),
                tint = MaterialTheme.colorScheme.error
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp)
            ) {
                Text(
                    pairCount.toString() +
                        " possible duplicate pair" +
                        if (pairCount == 1) "" else "s",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "These observations are close enough to deserve reconciliation before they are treated as separate transactions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Review possible duplicates"
            )
        }
    }
}

@Composable
private fun PossibleDuplicateRow(
    group: PossibleDuplicateGroup,
    onSameTransaction: () -> Unit,
    onKeepBoth: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(
                    Icons.Rounded.WarningAmber,
                    size = 40.dp,
                    background = MaterialTheme.colorScheme.errorContainer,
                    tint = MaterialTheme.colorScheme.error
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 11.dp)
                ) {
                    Text(
                        formatActivityMoney(group.amountMinor),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Two observations • " +
                            (group.timeDeltaMs / 1000L).toString() +
                            "s apart",
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DuplicateObservationLine(
                label = "Observation 1",
                item = group.first
            )
            DuplicateObservationLine(
                label = "Observation 2",
                item = group.second
            )

            Text(
                "PesaTrack will not remove either record unless you confirm they describe the same transaction.",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onKeepBoth,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Keep both")
                }
                Button(
                    onClick = onSameTransaction,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Same transaction")
                }
            }
        }
    }
}

@Composable
private fun DuplicateObservationLine(
    label: String,
    item: StoredTransaction
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            duplicateSourceLabel(item) +
                " • " +
                formatPreciseTime(item.receivedAtEpochMs),
            modifier = Modifier.padding(top = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            buildString {
                append(humanize(item.direction))
                if (!item.transactionCode.isNullOrBlank()) {
                    append(" • code ")
                    append(item.transactionCode)
                } else {
                    append(" • no transaction code")
                }
            },
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyDuplicateState(
    monthLabel: String
) {
    PesaCard {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBubble(
                Icons.Rounded.WarningAmber,
                size = 52.dp,
                background = pesaToneContainer("green"),
                tint = pesaToneContent("green")
            )
            Text(
                "No possible duplicates in " + monthLabel,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Any previously reviewed pairs marked Keep both are also kept out of this queue.",
                modifier = Modifier.padding(top = 5.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReviewQueueCallout(
    groupCount: Int,
    transactionCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = pesaToneContainer("amber")
    ) {
        Row(
            Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                Icons.AutoMirrored.Rounded.HelpOutline,
                size = 42.dp,
                background = MaterialTheme.colorScheme.surface.copy(alpha = .7f),
                tint = pesaToneContent("amber")
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp)
            ) {
                Text(
                    "$groupCount group" +
                        if (groupCount == 1) " to review" else "s to review",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "$transactionCount pending transaction" +
                        if (transactionCount == 1) "" else "s" +
                        " collapsed by full name or phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReviewGroupRow(
    group: ReviewGroup,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                Icons.AutoMirrored.Rounded.HelpOutline,
                size = 42.dp,
                background = pesaToneContainer("amber"),
                tint = pesaToneContent("amber")
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp)
            ) {
                Text(
                    group.label,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${group.transactionCount} transaction" +
                        if (group.transactionCount == 1) "" else "s" +
                        " • ${formatActivityMoney(group.totalMinor)} total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Tap once to classify and optionally remember this identity.",
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = pesaToneContent("amber")
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivityDateHeader(
    dayEpochMs: Long,
    transactions: List<StoredTransaction>
) {
    val expenseTotal = CheckedMoney.orNull { transactions
        .filter { it.direction == MoneyDirection.EXPENSE.name }
        .checkedMoneySumOf { CheckedMoney.add(it.amountMinor, it.feeMinor) } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            dayLabel(dayEpochMs),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (expenseTotal == null || expenseTotal > 0L) {
            Text(
                expenseTotal?.let(::formatActivityMoney) ?: "Total unavailable",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivityRow(
    item: StoredTransaction,
    accountName: String?,
    linkedAccountName: String?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val categoryStore = remember { CategoryStore.get(context) }
    val categoryId = categoryStore.canonicalId(item.category)
    val categoryLabel = categoryStore.labelFor(categoryId)
    val needsReview =
        item.reviewState == ReviewState.NEEDS_REVIEW.name
    val isRefund =
        item.relationType ==
            TransactionStore.RELATION_REFUND
    val direction = runCatching {
        MoneyDirection.valueOf(item.direction)
    }.getOrDefault(MoneyDirection.NEUTRAL)

    val tone = when {
        needsReview -> "amber"
        isRefund -> "green"
        direction == MoneyDirection.INCOME -> "green"
        direction == MoneyDirection.TRANSFER -> "blue"
        categoryId == TransactionCategory.SHOPPING.name -> "violet"
        categoryId == TransactionCategory.ELECTRICITY.name ||
            categoryId == TransactionCategory.RENT.name -> "amber"
        else -> "green"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MerchantBadge(
                item = item,
                categoryId = categoryId,
                tone = tone
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp)
            ) {
                val rawDisplayName =
                    item.evidenceLabel
                        ?.takeIf {
                            item.evidenceSource
                                ?.startsWith(
                                    "entity_intelligence:"
                                ) == true
                        }
                        ?: item.counterparty
                        ?: humanize(item.type)
                val displayName =
                    financialActivityDisplayName(
                        item = item,
                        rawDisplayName =
                            rawDisplayName,
                        accountName =
                            accountName
                    )

                Text(
                    displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatTime(item.occurredAtEpochMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "  •  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val activityLabel =
                        when {
                            needsReview ->
                                "Needs review"
                            isRefund ->
                                "Refund • " +
                                    categoryLabel
                            else ->
                                financialActivityEventLabel(
                                    item = item,
                                    categoryLabel =
                                        categoryLabel,
                                    rawDisplayName =
                                        rawDisplayName,
                                    accountName =
                                        accountName
                                )
                        }
                    val routeLabel =
                        financialActivityRouteLabel(
                            item = item,
                            accountName =
                                accountName,
                            linkedAccountName =
                                linkedAccountName
                        )
                    Text(
                        listOfNotNull(
                            routeLabel,
                            activityLabel
                        ).joinToString(
                            " • "
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = pesaToneContent(tone),
                        maxLines = 1,
                        overflow =
                            androidx.compose.ui.text
                                .style.TextOverflow
                                .Ellipsis
                    )
                }
                if (
                    item.counterparty.isNullOrBlank() &&
                    !item.evidenceLabel.isNullOrBlank()
                ) {
                    Text(
                        "Possible match: " + item.evidenceLabel,
                        modifier = Modifier.padding(top = 3.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.fulizaFinancedMinor > 0L) {
                    Text(
                        "Fuliza financed " +
                            formatActivityMoney(item.fulizaFinancedMinor) +
                            if (item.fulizaAccessFeeMinor > 0L) {
                                " • access fee " +
                                    formatActivityMoney(
                                        item.fulizaAccessFeeMinor
                                    )
                            } else {
                                ""
                            },
                        modifier = Modifier.padding(top = 3.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    amountLabel(item, direction),
                    style = MaterialTheme.typography.titleSmall,
                    color = when {
                        direction == MoneyDirection.INCOME ||
                            item.type ==
                            MpesaTransactionType.REVERSAL.name ->
                            MaterialTheme.colorScheme.primary
                        direction == MoneyDirection.EXPENSE ->
                            MaterialTheme.colorScheme.error
                        needsReview ||
                            direction == MoneyDirection.NEUTRAL ->
                            pesaToneContent("amber")
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(18.dp)
                )
            }
        }
    }
}



