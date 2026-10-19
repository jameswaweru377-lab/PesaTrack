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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.flow.first
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
    var revision: Long = -1L
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
    onPaycheckClick: () -> Unit = {},
    openNeedsAttention: Boolean = false,
    initialAccountReviewKey: String? = null,
    stateCache: ActivityScreenCache? = null
) {
    var payCycleMode by rememberSaveable { mutableStateOf(false) }
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
    var receivableLinks by remember { mutableStateOf<Map<Long,ReceivableActivityLink>>(emptyMap()) }
    LaunchedEffect(ledgerUpdateState.revision) {
        try { receivableLinks = withContext(Dispatchers.IO) { loadReceivableActivityLinks(context) } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { receivableLinks = emptyMap() }
    }
    val currentMonthEpochMs = remember {
        monthStart(System.currentTimeMillis())
    }
    val warmData =
        remember(stateCache, ledgerUpdateState.revision) {
            // Only a snapshot from the current ledger revision can skip a
            // new read. After imports or account changes, never present an
            // outdated Activity cache as if it were up to date.
            stateCache?.takeIf {
                it.revision == ledgerUpdateState.revision
            }?.data
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
    var cycles by remember { mutableStateOf<List<com.kedevjimmy.pesatrack.data.local.PayCycle>>(emptyList()) }
    var cycleSelection by rememberSaveable { mutableStateOf<Long?>(null) }
    var cycleSnapshot by remember { mutableStateOf<ActivityLedgerSnapshot?>(null) }
    var cyclePrevious by remember { mutableStateOf<ActivityLedgerSnapshot?>(null) }
    var loadedCycleStart by remember { mutableStateOf<Long?>(null) }
    var cycleLoading by remember { mutableStateOf(false) }
    var cycleError by remember { mutableStateOf(false) }
    var showCyclePicker by remember { mutableStateOf(false) }
    var showExtraFilters by remember { mutableStateOf(false) }
    var explainNetFlow by remember { mutableStateOf(false) }
    LaunchedEffect(ledgerUpdateState.revision) {
        try {
            cycles = withContext(Dispatchers.IO) {
                com.kedevjimmy.pesatrack.data.local.PayCycleStore.get(context).listCycles()
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { cycles = emptyList() }
    }
    val chosenCycle = cycles.firstOrNull { it.startTransactionId == cycleSelection }
        ?: cycles.firstOrNull()
    LaunchedEffect(payCycleMode, chosenCycle?.startTransactionId, ledgerUpdateState.revision) {
        if(!payCycleMode || chosenCycle == null) return@LaunchedEffect
        cycleLoading = true
        cycleError = false
        try {
            val loaded = withContext(Dispatchers.IO) {
                val store = TransactionStore.get(context)
                val idx = cycles.indexOfFirst { it.startTransactionId == chosenCycle.startTransactionId }
                val prior = cycles.getOrNull(idx + 1)
                val now = System.currentTimeMillis()
                store.activitySnapshotBetween(chosenCycle.startEpochMs,
                    chosenCycle.endEpochMs ?: (now + 1L), performMaintenance = false) to
                    prior?.let { store.activitySnapshotBetween(it.startEpochMs,
                        it.endEpochMs ?: (now + 1L), performMaintenance = false) }
            }
            cycleSnapshot = loaded.first
            cyclePrevious = loaded.second
            loadedCycleStart = chosenCycle.startTransactionId
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            cycleError = true
            cycleSnapshot = null
            cyclePrevious = null
        } finally { cycleLoading = false }
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
            it.revision = ledgerUpdateState.revision
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
        activityError = false
        val started = android.os.SystemClock.elapsedRealtime()
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
        // The monthly ledger becomes available immediately; slow diagnostics
        // continue in the background without any top loading indicator.
        val coreMs = android.os.SystemClock.elapsedRealtime() - started

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
        val totalMs = android.os.SystemClock.elapsedRealtime() - started
        if (totalMs > 400L) android.util.Log.i(
            "PesaTrackPerf",
            "Activity refresh ${totalMs}ms (core ${coreMs}ms)"
        )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            if (selectedMonthEpochMs == requestedMonth) {
                android.util.Log.e("PesaTrackActivity", "Activity could not load", failure)
                activityError = true
            }
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

    var notificationAccountOpened by remember(initialAccountReviewKey) { mutableStateOf(false) }
    LaunchedEffect(initialAccountReviewKey, reviewableFinancialEntities) {
        if (!notificationAccountOpened && initialAccountReviewKey != null) {
            reviewableFinancialEntities.firstOrNull { it.entityKey == initialAccountReviewKey }?.let {
                accountReviewEntity = it
                notificationAccountOpened = true
            }
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
        if (
            loadedMonthEpochMs == selectedMonthEpochMs &&
            stateCache?.revision == ledgerUpdateState.revision &&
            stateCache?.selectedMonthEpochMs == selectedMonthEpochMs
        ) return@LaunchedEffect
        reloadActivityData(selectedMonthEpochMs)
    }

    // Avoid an intrusive spinner or transient zero-amount ledger while a
    // cold-start/month-switch snapshot is being read on Dispatchers.IO.
    // This stable shell displays no fabricated totals. Existing error retry
    // remains accessible when a real load failure occurs.
    if (loadedMonthEpochMs != selectedMonthEpochMs && !payCycleMode) {
        Column(
            modifier = Modifier.padding(scaffoldPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                "Activity",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Your money, in context.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (activityError) {
                Text("Activity could not load. Your saved data is unchanged.")
                TextButton(onClick = { refresh() }) { Text("Try again") }
            }
        }
        return
    }

    val selectedMonthLabel =
        monthLabel(selectedMonthEpochMs)
    val displaySnapshot = if (payCycleMode) cycleSnapshot else snapshot
    val displayPrevious = if (payCycleMode) cyclePrevious else previousSnapshot
    val periodReady = !payCycleMode ||
        (chosenCycle != null && loadedCycleStart == chosenCycle.startTransactionId &&
            !cycleLoading && !cycleError)
    val periodLabel = when {
        !payCycleMode -> selectedMonthLabel
        chosenCycle == null -> "Salary cycle"
        else -> approvedMoneyCardPeriodLabel(monthLabel(chosenCycle.startEpochMs), salaryMode = true)
    }
    val currentReviewCount = displaySnapshot?.summary?.needsReviewCount ?: 0
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
    val possibleDuplicateIds = (displaySnapshot ?: snapshot).possibleDuplicates
        .flatMap { listOf(it.first.id, it.second.id) }
        .toSet()
    val filtered = (if (payCycleMode && !periodReady) emptyList() else (displaySnapshot ?: snapshot).transactions).filter { item ->
        if (item.reviewState == ReviewState.EXCLUDED.name) {
            return@filter false
        }
        if (item.id in possibleDuplicateIds) {
            return@filter false
        }

        val matchesFilter =
            when (selectedFilter) {
                "Owed to me" -> receivableLinks[item.id]?.let { it.active && it.kind == "LENT" } == true
                "Repayments" -> receivableLinks[item.id]?.let { it.active && it.kind == "REPAID" } == true
                "Reviews" ->
                    item.reviewState ==
                        ReviewState.NEEDS_REVIEW.name
                "Pending" ->
                    item.reviewState == ReviewState.NEEDS_REVIEW.name &&
                        item.direction == MoneyDirection.TRANSFER.name
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
                    .contains(query, ignoreCase = true) ||
                receivableLinks[item.id]?.description?.contains(query,ignoreCase = true) == true

        matchesFilter && matchesSearch
    }

    // Progressive UI materialization. The complete monthly ledger still owns
    // money totals, classification, reviews and duplicate exclusion.
    var visibleActivityCount by remember(
        selectedMonthEpochMs, payCycleMode, chosenCycle?.startTransactionId,
        selectedFilter, searchQuery, selectedGlobalQueue, selectedFinancialSource,
        ledgerUpdateState.revision
    ) { mutableIntStateOf(ACTIVITY_FIRST_BATCH) }
    val activityListState = rememberLazyListState()
    val sortedFiltered = filtered.sortedWith(
        compareByDescending<StoredTransaction> { it.occurredAtEpochMs }
            .thenByDescending { it.id }
    )
    val visibleFiltered = activityVisibleRows(sortedFiltered, visibleActivityCount)
    val allTransactionsByDay = sortedFiltered.groupBy { dayStart(it.occurredAtEpochMs) }
    val groupedFiltered = visibleFiltered
        .groupBy { dayStart(it.occurredAtEpochMs) }
        .toList()
        .sortedByDescending { it.first }

    LaunchedEffect(
        activityListState, visibleActivityCount, sortedFiltered.size,
        selectedMonthEpochMs, selectedFinancialSource, payCycleMode,
        chosenCycle?.startTransactionId
    ) {
        if (selectedFinancialSource != null ||
            visibleActivityCount >= sortedFiltered.size) return@LaunchedEffect
        snapshotFlow {
            activityListState.layoutInfo.visibleItemsInfo.any {
                it.key == "activity-load-more"
            }
        }.first { it }
        val next = withContext(Dispatchers.Default) {
            activityVisibleCount(visibleActivityCount, sortedFiltered.size)
        }
        visibleActivityCount = next
    }
    LaunchedEffect(selectedMonthEpochMs, payCycleMode, chosenCycle?.startTransactionId) {
        activityListState.scrollToItem(0)
    }

    Column(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = activityListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = 12.dp,
                vertical = 6.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Column(Modifier.padding(bottom = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "Activity",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Your money, in context.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

            if (needsAttentionCount > 0) item {
                Box(Modifier.padding(bottom = 10.dp)) {
                    ActivityAttentionHero(needsAttentionCount) {
                        selectedGlobalQueue = if(selectedGlobalQueue == "Attention") null else "Attention"
                    }
                }
            }

            // Own-account review remains accessible but does not push the
            // transaction search further down on every visit.


            if (activityError) item {
                Text("Activity could not load")
                Text("Showing your last successfully loaded activity.")
                TextButton(onClick = { refresh() }) { Text("Try again") }
            }
            item(key = "premium-period-selectors") {
                Box(Modifier.padding(bottom = 7.dp)) { ActivityPeriodSelectors(
                    monthLabel = selectedMonthLabel,
                    cycleLabel = chosenCycle?.let { monthLabel(it.startEpochMs) + " cycle" }
                        ?: "Add salary",
                    salaryMode = payCycleMode,
                    onMonth = { payCycleMode = false; showMonthPicker = true },
                    onCycle = { showCyclePicker = true }
                ) }
            }
            item(key = "premium-period-money") {
                val badge = if (payCycleMode) {
                    if (chosenCycle == null) "No salary cycle"
                    else if(chosenCycle.endEpochMs == null) "Cycle in progress"
                    else "Completed cycle"
                } else monthDaysRemaining(selectedMonthEpochMs, System.currentTimeMillis())
                    ?.let { it.toString() + " days left" } ?: "Past month"
                Box(Modifier.padding(bottom = 9.dp)) { ActivityPremiumMoneyHero(
                    periodLabel = periodLabel,
                    snapshot = if(periodReady) displaySnapshot else null,
                    previous = displayPrevious,
                    badge = badge, waiting = !periodReady,
                    onExplain = { explainNetFlow = true }
                ) }
            }
            item(key = "premium-search") {
                Row(Modifier.fillMaxWidth().padding(bottom = 7.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f).heightIn(min=48.dp),
                        placeholder = { Text("Search transactions") },
                        leadingIcon = { Icon(Icons.Rounded.Search,null,Modifier.size(18.dp)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = RoundedCornerShape(16.dp)
                    )
                    IconButton(onClick = { showExtraFilters = !showExtraFilters },
                        modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Tune,"More filters")
                    }
                }
            }
            item(key = "premium-filters") {
                Box(Modifier.padding(bottom = 8.dp)) {
                    ActivityCompactFilters(selectedFilter,currentReviewCount) {
                        selectedFilter = it
                    }
                }
            }
            if(showExtraFilters) item(key = "additional-filters") {
                LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    listOf("Spending","Income","Owed to me","Repayments").forEach { name ->
                        item(key=name) {
                            FilterChip(selected=selectedFilter==name,
                                onClick={selectedFilter=name},
                                label={Text(name)})
                        }
                    }
                }
            }
            if(cycleError && payCycleMode) item {
                Text("Salary cycle could not load. No records were changed.")
                TextButton(onClick={payCycleMode=false}) { Text("Show calendar month") }
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

            (if(payCycleMode) listOf(ActivityMonthBucket(
                monthStartEpochMs=chosenCycle?.startEpochMs ?: selectedMonthEpochMs,
                transactionCount=displaySnapshot?.transactions?.size ?: 0))
              else monthBuckets).forEach { bucket ->
                val isExpanded = if(payCycleMode) true else expandedMonthEpochMs == bucket.monthStartEpochMs

                item(
                    key =
                        "month-" +
                            bucket.monthStartEpochMs
                ) {
                    ActivityMonthSectionHeader(
                        monthLabel = if(payCycleMode) periodLabel else monthLabel(bucket.monthStartEpochMs),
                        transactionCount = if(payCycleMode) filtered.size else bucket.transactionCount,
                        expanded = isExpanded,
                        onClick = {
                            if (payCycleMode) { /* salary period stays expanded */ }
                            else if (isExpanded) {
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
                                    dayEpochMs = dayEpochMs,
                                    transactions = allTransactionsByDay[dayEpochMs].orEmpty()
                                )
                            }
                            itemsIndexed(
                                items = transactions,
                                key = { _, transaction ->
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
                            ) { index, transaction ->
                                ActivityRow(
                                    item = transaction,
                                    position = activityGroupPosition(index, transactions.size),
                                    debtLink = receivableLinks[transaction.id],
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
                        if (visibleActivityCount < sortedFiltered.size) {
                            item(key = "activity-load-more") {
                                ActivityLoadingMoreRows()
                            }
                        }
                    }
                }
            }
            item(key = "own-account-review-more") {
                OwnedTransferReviewSection()
            }
        }
    }

    if (showCyclePicker) {
        AlertDialog(
            onDismissRequest={showCyclePicker=false},
            title={Text("Choose salary cycle")},
            text={
                Column(Modifier.heightIn(max=390.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text("One confirmed salary receipt to the next.",
                        style=MaterialTheme.typography.bodySmall)
                    if(cycles.isEmpty()) Text("No confirmed salary cycle. Add salary from Plan.")
                    cycles.forEach { cycle ->
                        TextButton(onClick={
                            cycleSelection=cycle.startTransactionId
                            payCycleMode=true
                            selectedFilter="All"
                            showCyclePicker=false
                        },modifier=Modifier.fillMaxWidth()) {
                            Text(monthLabel(cycle.startEpochMs)+" salary cycle")
                        }
                    }
                }
            },
            confirmButton={TextButton(onClick={showCyclePicker=false}){Text("Close")}}
        )
    }
    if (explainNetFlow) {
        AlertDialog(onDismissRequest={explainNetFlow=false},
            title={Text("About net flow")},
            text={Text("Money in minus spending for the selected period. This is not a wallet balance or money available after bills. Transfers, savings movements and loan principal are tracked separately. The ring is illustrative and does not represent a verified account balance.")},
            confirmButton={TextButton(onClick={explainNetFlow=false}){Text("Got it")}})
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
private fun ActivityMonthlyHero(
    monthLabel: String,
    moneyIn: Long,
    spent: Long,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth().testTag("activity-summary-toggle")
            .clickable(onClick = onToggle)
            .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .28f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .40f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(monthLabel, modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text("Money overview", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Hide monthly details" else "Show monthly details",
                    modifier = Modifier.padding(start = 6.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Money in", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatActivityMoney(moneyIn), style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text("Spent", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatActivityMoney(spent), style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                }
            }
            Text("Spending is separate from transfers, savings and loan movements. Tap for details.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
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
    monthLabel: String, transactionCount: Int, expanded: Boolean, onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick=onClick)
            .padding(horizontal=2.dp,vertical=5.dp),
        verticalAlignment=Alignment.CenterVertically
    ) {
        Text(monthLabel,Modifier.weight(1f),
            style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        Text(transactionCount.toString() +
            if(transactionCount==1)" transaction" else " transactions",
            style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(if(expanded)Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            if(expanded)"Collapse period" else "Expand period",
            Modifier.padding(start=6.dp).size(18.dp))
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
    val colors = MaterialTheme.colorScheme
    val gradient = listOf(colors.primary, colors.primary)

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
                    color = colors.onPrimary.copy(alpha = .14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                            tint = colors.tertiary
                        )
                    }
                }
                Text(
                    monthLabel + " Summary",
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimary
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
                    foreground = colors.onPrimary,
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
                    foreground = colors.onPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp),
                shape = RoundedCornerShape(15.dp),
                color = colors.tertiaryContainer
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
                                    .onTertiaryContainer
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
    foreground: Color,
    zeroComparisonLabel: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = foreground.copy(alpha = .72f)
        )
        Text(
            value,
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = foreground,
            maxLines = 1
        )
        SummaryComparison(
            current = current,
            previous = previous,
            positiveWhenUp = positiveWhenUp,
            zeroComparisonLabel =
                zeroComparisonLabel,
            foreground = foreground
        )
    }
}

@Composable
private fun SummaryComparison(
    current: Long,
    previous: Long,
    positiveWhenUp: Boolean,
    foreground: Color,
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
            color = foreground.copy(alpha = .68f)
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
            color = foreground.copy(alpha = .68f)
        )
        return
    }

    val up = percent > 0
    val favorable =
        if (positiveWhenUp) up else !up
    val tint = if (percent == 0) {
        foreground.copy(alpha = .66f)
    } else if (favorable) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.error
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

/**
 * Read-only daily spending, not a balance. Uses all filtered records for the
 * day, even when only the first page of rows has been materialized.
 */
@Composable
private fun ActivityDateHeader(
    dayEpochMs: Long,
    transactions: List<StoredTransaction>
) {
    val expenseTotal = CheckedMoney.orNull {
        transactions
            .filter {
                it.direction == MoneyDirection.EXPENSE.name &&
                    it.relationType != TransactionStore.RELATION_REFUND
            }
            .checkedMoneySumOf { CheckedMoney.add(it.amountMinor, it.feeMinor) }
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp,
            bottomStart = 3.dp, bottomEnd = 3.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .47f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                dayLabel(dayEpochMs), modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (expenseTotal == null || expenseTotal > 0L) {
                Text(
                    expenseTotal?.let(::formatActivityMoney) ?: "Total unavailable",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.semantics {
                        stateDescription = "Recorded spending for this day"
                    }
                )
            }
        }
    }
}

@Composable
private fun ActivityLoadingMoreRows() {
    val base = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .16f)
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp)
            .testTag("activity-loading-more"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(3) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(modifier = Modifier.size(34.dp),
                        shape = CircleShape, color = base) {}
                    Column(Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(Modifier.fillMaxWidth(.69f).height(9.dp),
                            color = base, shape = RoundedCornerShape(9.dp)) {}
                        Surface(Modifier.fillMaxWidth(.48f).height(7.dp),
                            color = base.copy(alpha = .72f),
                            shape = RoundedCornerShape(9.dp)) {}
                    }
                    Surface(Modifier.size(width = 70.dp, height = 9.dp),
                        color = base, shape = RoundedCornerShape(9.dp)) {}
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.size(width = 10.dp, height = 1.dp))
                Text(
                    "Loading more transactions…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

internal enum class ActivityGroupPosition { ONLY, FIRST, MIDDLE, LAST }

internal fun activityGroupPosition(index: Int, count: Int): ActivityGroupPosition {
    require(count > 0 && index in 0 until count)
    return when {
        count == 1 -> ActivityGroupPosition.ONLY
        index == 0 -> ActivityGroupPosition.FIRST
        index == count - 1 -> ActivityGroupPosition.LAST
        else -> ActivityGroupPosition.MIDDLE
    }
}

@Composable
private fun ActivityRow(
    item: StoredTransaction,
    position: ActivityGroupPosition = ActivityGroupPosition.ONLY,
    debtLink: ReceivableActivityLink? = null,
    accountName: String?,
    linkedAccountName: String?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val categoryStore = remember { CategoryStore.get(context) }
    val categoryId = categoryStore.canonicalId(item.category)
    val categoryName = categoryStore.labelFor(categoryId)
    val needsReview = item.reviewState == ReviewState.NEEDS_REVIEW.name
    val refund = item.relationType == TransactionStore.RELATION_REFUND
    val direction = runCatching { MoneyDirection.valueOf(item.direction) }
        .getOrDefault(MoneyDirection.NEUTRAL)
    val pill = when {
        needsReview -> "Needs review"
        debtLink?.active == true && debtLink.kind == "LENT" -> "Money lent"
        debtLink?.active == true && debtLink.kind == "REPAID" -> "Repayment"
        refund -> "Refund"
        direction == MoneyDirection.TRANSFER -> "Transfer"
        else -> categoryName
    }
    val pillColor = when {
        needsReview -> Color(0xFFE1A5F8)
        refund || direction == MoneyDirection.INCOME -> Color(0xFF51E0C6)
        debtLink?.active == true -> Color(0xFF8CC8F5)
        direction == MoneyDirection.TRANSFER -> Color(0xFF80B5FF)
        categoryId == TransactionCategory.GROCERIES.name -> Color(0xFF62DBC3)
        else -> Color(0xFFC4B0FB)
    }
    val amountColor = when {
        refund || direction == MoneyDirection.INCOME ||
            item.type == MpesaTransactionType.REVERSAL.name -> Color(0xFF57E6C5)
        direction == MoneyDirection.EXPENSE -> Color(0xFFFF6285)
        direction == MoneyDirection.TRANSFER -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val name = financialActivityDisplayName(
        item = item,
        rawDisplayName = item.evidenceLabel?.takeIf {
            item.evidenceSource?.startsWith("entity_intelligence:") == true
        } ?: item.counterparty ?: humanize(item.type),
        accountName = accountName
    )
    val source = financialActivityRouteLabel(
        item = item,
        accountName = accountName,
        linkedAccountName = linkedAccountName
    ) ?: "Source unknown"
    val dayShape = when (position) {
        ActivityGroupPosition.ONLY -> RoundedCornerShape(
            topStart = 2.dp, topEnd = 2.dp, bottomStart = 17.dp, bottomEnd = 17.dp
        )
        ActivityGroupPosition.FIRST, ActivityGroupPosition.MIDDLE ->
            RoundedCornerShape(2.dp)
        ActivityGroupPosition.LAST -> RoundedCornerShape(
            topStart = 2.dp, topEnd = 2.dp, bottomStart = 17.dp, bottomEnd = 17.dp
        )
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = dayShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 67.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    MerchantBadge(item = item, categoryId = categoryId, tone = "violet")
                    if (needsReview) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).size(8.dp),
                            shape = CircleShape,
                            color = pillColor,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface)
                        ) {}
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            SimpleDateFormat("HH:mm",
                                androidx.compose.ui.platform.LocalConfiguration.current.locales[0])
                                .format(Date(item.occurredAtEpochMs)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text("·", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            source,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = pillColor.copy(alpha = .13f)
                        ) {
                            Text(
                                pill,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    .widthIn(max = 96.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                color = pillColor,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Text(
                    amountLabel(item, direction),
                    color = amountColor,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    modifier = Modifier.widthIn(max = 118.dp)
                )
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp)
                )
            }
            if (position == ActivityGroupPosition.FIRST ||
                position == ActivityGroupPosition.MIDDLE) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 58.dp, end = 10.dp),
                    thickness = .5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .42f)
                )
            }
        }
    }
}

