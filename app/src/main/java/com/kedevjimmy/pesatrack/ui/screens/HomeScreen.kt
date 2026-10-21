package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.domain.model.AccountType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.CanonicalStoreLock
import com.kedevjimmy.pesatrack.data.local.PersonalReceivableStore
import com.kedevjimmy.pesatrack.data.local.ReceivableShare
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot
import com.kedevjimmy.pesatrack.data.local.ActivitySummary
import com.kedevjimmy.pesatrack.data.local.BudgetStore
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.CycleProtectionStore
import com.kedevjimmy.pesatrack.data.local.DebtAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.DebtStore
import com.kedevjimmy.pesatrack.data.local.FinancialHealthEvidenceStore
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.IdentityGraphStore
import com.kedevjimmy.pesatrack.data.local.LedgerSnapshot
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.MonthlyBudget
import com.kedevjimmy.pesatrack.data.local.PaydaySnapshotStore
import com.kedevjimmy.pesatrack.data.local.PaydayAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.MoneyPictureHealthEvaluator
import com.kedevjimmy.pesatrack.data.local.MoneyPictureHealth
import com.kedevjimmy.pesatrack.data.local.PayCycleSnapshot
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
import com.kedevjimmy.pesatrack.data.local.RelationshipReviewStore
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.SalaryCycleCandidate
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney
import com.kedevjimmy.pesatrack.domain.model.checkedMoneySumOf
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import com.kedevjimmy.pesatrack.domain.rules.AccountIdentityPresentation
import com.kedevjimmy.pesatrack.domain.rules.SalaryExpectationPolicy
import com.kedevjimmy.pesatrack.domain.rules.PlanningAvailabilityPolicy
import com.kedevjimmy.pesatrack.planning.BillSchedules
import com.kedevjimmy.pesatrack.planning.ScheduledBill
import com.kedevjimmy.pesatrack.ui.components.AvailableToLiveHero
import com.kedevjimmy.pesatrack.ui.components.ConceptEmeraldHero
import com.kedevjimmy.pesatrack.ui.components.ConceptMetricTile
import com.kedevjimmy.pesatrack.ui.components.ConceptProgressCard
import com.kedevjimmy.pesatrack.ui.components.FundingGapCard
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.PesaTrackTopBar
import com.kedevjimmy.pesatrack.ui.components.SmartInsightCard
import com.kedevjimmy.pesatrack.ui.components.SpendingSlice
import com.kedevjimmy.pesatrack.ui.components.premiumCategoryColor
import java.text.NumberFormat
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeCoreState(
    val cycleSnapshot: PayCycleSnapshot,
    val ledgerSnapshot: LedgerSnapshot,
    val activitySnapshot: ActivityLedgerSnapshot,
    val receivableShares: Map<Long, ReceivableShare> = emptyMap(),
    val debtAccounts: List<DebtAccountSnapshot>,
    val trackedAccounts: List<MoneyAccount>,
    val monthlyBudgets: List<MonthlyBudget>,
    val budgetSpendingByCategory: Map<String, Long>?,
    val confirmedAccountIds: Set<String>,
    val salaryRule: SmartRule?,
    val upcomingBills: List<ScheduledBill>,
    val accountBalanceSnapshots:
        List<PaydayAccountSnapshot>
)

data class HomeSupplementalState(
    val recordedSpendingInsight: String? = null,
    val needsAttentionCount: Int,
    val moneyPictureHealth:
        MoneyPictureHealth
)

data class HomeLoadedState(
    val cycleSnapshot: PayCycleSnapshot,
    val ledgerSnapshot: LedgerSnapshot,
    val activitySnapshot: ActivityLedgerSnapshot,
    val receivableShares: Map<Long, ReceivableShare> = emptyMap(),
    val debtAccounts: List<DebtAccountSnapshot>,
    val trackedAccounts: List<MoneyAccount>,
    val monthlyBudgets: List<MonthlyBudget>,
    val budgetSpendingByCategory: Map<String, Long>?,
    val confirmedAccountIds: Set<String>,
    val salaryRule: SmartRule?,
    val upcomingBills: List<ScheduledBill>,
    val needsAttentionCount: Int,
    val accountBalanceSnapshots:
        List<PaydayAccountSnapshot>,
    val moneyPictureHealth:
        MoneyPictureHealth
)

internal data class HomeHistorySnapshots(
    val selected: ActivityLedgerSnapshot?,
    val previousAligned: ActivityLedgerSnapshot
)

class HomeScreenCache {
    var data: HomeLoadedState? = null
    var revision: Long = -1L
    private var positionsRevision: Long = -1L
    private var positions: List<com.kedevjimmy.pesatrack.data.local.ObservedInvestmentPosition>? = null
    private var historyRevision: Long = -1L
    private var historyOffset: Int? = null
    private var historyDayKey: Int = -1
    private var history: HomeHistorySnapshots? = null

    @Synchronized
    fun investmentsFor(revision: Long):
        List<com.kedevjimmy.pesatrack.data.local.ObservedInvestmentPosition>? =
        positions?.takeIf { positionsRevision == revision }

    @Synchronized
    fun saveInvestments(revision: Long,
        value: List<com.kedevjimmy.pesatrack.data.local.ObservedInvestmentPosition>) {
        positionsRevision = revision
        positions = value
    }

    @Synchronized
    internal fun historyFor(revision: Long, monthOffset: Int, dayKey: Int):
        HomeHistorySnapshots? =
        history?.takeIf { historyRevision == revision &&
            historyOffset == monthOffset && historyDayKey == dayKey }

    @Synchronized
    internal fun saveHistory(revision: Long, monthOffset: Int, dayKey: Int,
        value: HomeHistorySnapshots) {
        historyRevision = revision
        historyOffset = monthOffset
        historyDayKey = dayKey
        history = value
    }
}

private fun emptyPayCycleSnapshot() =
    PayCycleSnapshot(
        cycle = null,
        startingFundsMinor = 0L,
        additionalIncomeMinor = 0L,
        spentMinor = 0L,
        feesMinor = 0L,
        savingsMinor = 0L,
        remainingCommitmentsMinor = 0L,
        availableToLiveMinor = 0L,
        safeTodayMinor = null,
        estimatedDaysRemaining = null,
        commitments = emptyList(),
        pendingSalary = null
    )

private fun emptyActivitySnapshot() =
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
fun HomeScreen(
    scaffoldPadding: PaddingValues,
    onProfileClick: () -> Unit = {},
    onAttentionClick: () -> Unit = {},
    onActivityClick: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onPlanClick: () -> Unit = {},
    onAccountsClick: () -> Unit = {},
    onDebtsClick: () -> Unit = {},
    onSalaryClick: () -> Unit = {},
    onBillsClick: () -> Unit = {},
    onGoalsClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    stateCache: HomeScreenCache? = null
) {
    var homeMoneyToolsExpanded by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val cycleStore = remember { PayCycleStore.get(context) }
    val transactionStore = remember { TransactionStore.get(context) }
    val accountStore = remember { AccountStore.get(context) }
    val categoryStore = remember { CategoryStore.get(context) }
    val debtStore = remember { DebtStore.get(context) }
    val budgetStore = remember { BudgetStore.get(context) }
    val billSchedules = remember { BillSchedules(context) }
    val cycleProtectionStore =
        remember {
            CycleProtectionStore.get(context)
        }
    val financialStore =
        remember {
            FinancialUnderstandingStore
                .get(context)
        }
    val ruleStore = remember { RuleStore.get(context) }
    val relationshipReviewStore =
        remember {
            RelationshipReviewStore.get(context)
        }
    val identityGraphStore =
        remember {
            IdentityGraphStore.get(context)
        }
    val preferences = remember {
        context.getSharedPreferences(
            "pesatrack_preferences",
            android.content.Context.MODE_PRIVATE
        )
    }
    val ledgerUpdateState by
        LedgerUpdateBus.state.collectAsState()
    val homeScope =
        rememberCoroutineScope()
    var salaryPromptDeferredUntil by remember {
        mutableStateOf(
            preferences.getLong(
                "salary_prompt_deferred_until",
                0L
            )
        )
    }
    LaunchedEffect(ledgerUpdateState.revision) {
        salaryPromptDeferredUntil = preferences.getLong("salary_prompt_deferred_until", 0L)
    }
    var showManualSalaryDialog by remember {
        mutableStateOf(false)
    }
    var showProtectionDialog by remember {
        mutableStateOf(false)
    }
    val warmHome =
        remember(stateCache) {
            stateCache?.data
        }
    var homeLoading by remember {
        mutableStateOf(warmHome == null)
    }
    var homeError by remember { mutableStateOf(false) }
    var refreshJob by remember { mutableStateOf<Job?>(null) }
    var refreshPending by remember { mutableStateOf(false) }
    var rebuildPending by remember { mutableStateOf(false) }
    var homeLoaded by remember {
        mutableStateOf(warmHome != null)
    }

    var cycleSnapshot by remember {
        mutableStateOf(
            warmHome?.cycleSnapshot
                ?: emptyPayCycleSnapshot()
        )
    }
    var ledgerSnapshot by remember {
        mutableStateOf(
            warmHome?.ledgerSnapshot
                ?: LedgerSnapshot(
                    transactionCount = 0,
                    latestTransaction = null,
                    latestBalanceMinor = null,
                    latestBalanceObservedAtEpochMs =
                        null
                )
        )
    }
    var activitySnapshot by remember {
        mutableStateOf(
            warmHome?.activitySnapshot
                ?: emptyActivitySnapshot()
        )
    }
    var receivableShares by remember { mutableStateOf(warmHome?.receivableShares ?: emptyMap()) }
    var debtAccounts by remember {
        mutableStateOf<
            List<DebtAccountSnapshot>
        >(
            warmHome?.debtAccounts
                ?: emptyList()
        )
    }
    var trackedAccounts by remember {
        mutableStateOf<
            List<MoneyAccount>
        >(
            warmHome?.trackedAccounts
                ?: emptyList()
        )
    }
    var monthlyBudgets by remember {
        mutableStateOf<List<MonthlyBudget>>(
            warmHome?.monthlyBudgets
                ?: emptyList()
        )
    }
    var budgetSpendingByCategory by remember {
        mutableStateOf<Map<String, Long>?>(warmHome?.budgetSpendingByCategory)
    }
    var confirmedAccountIds by remember {
        mutableStateOf<Set<String>>(
            warmHome?.confirmedAccountIds
                ?: emptySet()
        )
    }
    var salaryRule by remember {
        mutableStateOf<SmartRule?>(
            warmHome?.salaryRule
        )
    }
    var upcomingBills by remember {
        mutableStateOf<List<ScheduledBill>>(
            warmHome?.upcomingBills
                ?: emptyList()
        )
    }
    var needsAttentionCount by remember {
        mutableStateOf(
            warmHome?.needsAttentionCount
                ?: 0
        )
    }
    var accountBalanceSnapshots by remember {
        mutableStateOf<
            List<PaydayAccountSnapshot>
        >(
            warmHome?.accountBalanceSnapshots
                ?: emptyList()
        )
    }
    var recordedSpendingInsight by remember { mutableStateOf<String?>(null) }
    var moneyPictureHealth by remember {
        mutableStateOf(
            warmHome?.moneyPictureHealth
                ?: MoneyPictureHealth(
                    score = 0,
                    understoodTransactions = 0,
                    consideredTransactions = 0,
                    needsReviewCount = 0,
                    lowQualitySpendingShare = 0,
                    confirmedAccountCount = 0,
                    knownBalanceAccountCount = 0,
                    salaryCycleActive = false,
                    issues = emptyList()
                )
        )
    }

    suspend fun loadHomeCoreState(
        rebuildCycles: Boolean
    ): HomeCoreState =
        withContext(Dispatchers.IO) {
            if (rebuildCycles) {
                traceFinancialRead("home.rebuildPayCycles") { cycleStore.rebuild() }
            }
            val referenceEpochMs =
                System.currentTimeMillis()
            val accounts = traceFinancialRead("home.accountList") {
                accountStore.listAccounts(enabledOnly = true)
            }
            val balanceSnapshots = traceFinancialRead("home.accountBalances") {
                PaydaySnapshotStore.get(context).currentAccountSnapshots(
                    accounts = accounts, nowEpochMs = referenceEpochMs
                )
            }

            val activityData = traceFinancialRead("home.activityAndBudgetEvidence") {
                synchronized(CanonicalStoreLock.monitor) {
                    val activity = transactionStore.activitySnapshot(nowEpochMs = referenceEpochMs)
                    val shares = PersonalReceivableStore(context).linkedShares()
                    val allocations = com.kedevjimmy.pesatrack.data.local
                        .EverydayMoneyStore(context).allAllocations()
                    val categoryStore = CategoryStore.get(context)
                    val spendingByCategory = try {
                        com.kedevjimmy.pesatrack.data.local
                            .BudgetSpendingProjection.calculate(
                                trustedRows = activity.trustedTransactions,
                                trustedOriginal = transactionStore::trustedRefundOriginal,
                                canonicalCategoryId = categoryStore::canonicalId,
                                receivableLent = { shares[it]?.lentMinor ?: 0L },
                                allocations = { allocations[it].orEmpty() }
                            )
                    } catch (_: ArithmeticException) {
                        null
                    } catch (_: IllegalStateException) {
                        null
                    }
                    Triple(activity, shares, spendingByCategory)
                }
            }
            val budgets = budgetStore.list()
            HomeCoreState(
                cycleSnapshot = traceFinancialRead("home.payCycleSnapshot") {
                    cycleStore.snapshot()
                },
                ledgerSnapshot = traceFinancialRead("home.ledgerSummary") {
                    transactionStore.snapshot()
                },
                activitySnapshot = activityData.first,
                receivableShares = activityData.second,
                debtAccounts =
                    debtStore.accounts(),
                trackedAccounts =
                    accounts,
                monthlyBudgets = budgets,
                budgetSpendingByCategory = activityData.third,
                confirmedAccountIds =
                    financialStore
                        .confirmedAccountIds(),
                salaryRule =
                    ruleStore
                        .listRules(
                            enabledOnly = true
                        )
                        .firstOrNull {
                            it.startsPayCycle
                        },
                upcomingBills =
                    billSchedules
                        .upcomingThisMonth(),
                accountBalanceSnapshots =
                    balanceSnapshots
            )
        }

    suspend fun loadHomeSupplementalState():
        HomeSupplementalState =
        withContext(Dispatchers.IO) {
            val referenceEpochMs =
                System.currentTimeMillis()
            HomeSupplementalState(
                recordedSpendingInsight = com.kedevjimmy.pesatrack.data.local.OfflineMoneyInsightStore
                    .spendingComparison(transactionStore, referenceEpochMs),
                needsAttentionCount =
                    transactionStore
                        .activityReviewOverview(
                            performMaintenance =
                                false
                        )
                        .let { overview ->
                            overview.reviewGroups
                                .sumOf {
                                    it.transactionCount
                                } +
                                overview
                                    .possibleDuplicates
                                    .size
                        } +
                        relationshipReviewStore
                            .pendingReviews()
                            .size +
                        identityGraphStore
                            .pendingReviewPrompts(
                                limit = 100,
                                includeOptional = false
                            )
                            .size,
                moneyPictureHealth =
                    MoneyPictureHealthEvaluator(
                        context
                    ).evaluate(
                        nowEpochMs =
                            referenceEpochMs
                    )
            )
        }

    fun applyHomeCoreState(
        core: HomeCoreState
    ) {
        cycleSnapshot =
            core.cycleSnapshot
        ledgerSnapshot =
            core.ledgerSnapshot
        receivableShares = core.receivableShares
        activitySnapshot =
            core.activitySnapshot
        debtAccounts =
            core.debtAccounts
        trackedAccounts =
            core.trackedAccounts
        monthlyBudgets =
            core.monthlyBudgets
        budgetSpendingByCategory =
            core.budgetSpendingByCategory
        confirmedAccountIds =
            core.confirmedAccountIds
        salaryRule =
            core.salaryRule
        upcomingBills =
            core.upcomingBills
        accountBalanceSnapshots =
            core.accountBalanceSnapshots
    }

    fun applyHomeSupplementalState(
        supplemental: HomeSupplementalState
    ) {
        recordedSpendingInsight = supplemental.recordedSpendingInsight
        needsAttentionCount =
            supplemental.needsAttentionCount
        moneyPictureHealth =
            supplemental.moneyPictureHealth
    }

    fun cacheHomeState(
        core: HomeCoreState,
        supplemental: HomeSupplementalState,
        loadedRevision: Long
    ) {
        stateCache?.data =
            HomeLoadedState(
                cycleSnapshot =
                    core.cycleSnapshot,
                ledgerSnapshot =
                    core.ledgerSnapshot,
                activitySnapshot =
                    core.activitySnapshot,
                receivableShares = core.receivableShares,
                debtAccounts =
                    core.debtAccounts,
                trackedAccounts =
                    core.trackedAccounts,
                monthlyBudgets =
                    core.monthlyBudgets,
                budgetSpendingByCategory =
                    core.budgetSpendingByCategory,
                confirmedAccountIds =
                    core.confirmedAccountIds,
                salaryRule =
                    core.salaryRule,
                upcomingBills =
                    core.upcomingBills,
                needsAttentionCount =
                    supplemental
                        .needsAttentionCount,
                accountBalanceSnapshots =
                    core.accountBalanceSnapshots,
                moneyPictureHealth =
                    supplemental
                        .moneyPictureHealth
            )
        stateCache?.revision = loadedRevision
    }

    fun refresh(
        rebuildCycles: Boolean = false
    ) {
        refreshPending = true
        rebuildPending = rebuildPending || rebuildCycles
        if (refreshJob?.isActive == true) return
        refreshJob = homeScope.launch {
            homeError = false
            homeLoading = !homeLoaded
            try {
                do {
                    refreshPending = false
                    val shouldRebuild = rebuildPending
                    rebuildPending = false
                    val loadedRevision = LedgerUpdateBus.state.value.revision
                    val refreshStarted = android.os.SystemClock.elapsedRealtime()
                    val core =
                        loadHomeCoreState(
                            rebuildCycles =
                                shouldRebuild
                        )
                    applyHomeCoreState(core)

                    // The primary money picture should appear as soon as the
                    // ledger/pay-cycle reads finish. Attention diagnostics and
                    // health analysis can complete just after the main cards
                    // are already usable.
                    homeLoaded = true
                    homeLoading = false

                    stateCache?.data
                        ?.let {
                            existing ->
                            stateCache.data =
                                existing.copy(
                                    cycleSnapshot =
                                        core.cycleSnapshot,
                                    ledgerSnapshot =
                                        core.ledgerSnapshot,
                                    activitySnapshot =
                                        core.activitySnapshot,
                                    receivableShares = core.receivableShares,
                                    debtAccounts =
                                        core.debtAccounts,
                                    trackedAccounts =
                                        core.trackedAccounts,
                                    monthlyBudgets =
                                        core.monthlyBudgets,
                                    budgetSpendingByCategory =
                                        core.budgetSpendingByCategory,
                                    confirmedAccountIds =
                                        core.confirmedAccountIds,
                                    salaryRule =
                                        core.salaryRule,
                                    upcomingBills =
                                        core.upcomingBills,
                                    accountBalanceSnapshots =
                                        core.accountBalanceSnapshots
                                )
                        }

                    val supplemental =
                        loadHomeSupplementalState()
                    applyHomeSupplementalState(
                        supplemental
                    )
                    cacheHomeState(
                        core = core,
                        supplemental = supplemental,
                        loadedRevision = loadedRevision
                    )
                    val elapsed = android.os.SystemClock.elapsedRealtime() - refreshStarted
                    if (elapsed > 400L) android.util.Log.i("PesaTrackPerf", "Home refresh took ${elapsed}ms")
                } while (refreshPending)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                android.util.Log.e("PesaTrackHome", "Home data failed to load; no transaction details logged.")
                homeError = true
            } finally {
                homeLoading = false
            }
        }
    }

    // A single revision-driven effect replaces two independent launch
    // effects, which previously loaded Home twice after onboarding or entry.
    // Navigate back to an unchanged snapshot without hitting the database.
    LaunchedEffect(ledgerUpdateState.revision) {
        if (!homeLoaded || stateCache?.revision != ledgerUpdateState.revision) {
            refresh()
        }
    }

    val visibleAccountGroups =
        AccountDisplayPolicy.groups(
            trackedAccounts, includeCash = true
        ).filter { group ->
            group.accounts.any {
                AccountDisplayPolicy
                    .isConfirmedOwned(
                        account = it,
                        confirmedAccountIds =
                            confirmedAccountIds, includeCash = true
                    )
            }
        }
    val activeDebtAccounts =
        debtAccounts
            .filter {
                it.isActive
            }
            .sortedWith(
                compareByDescending<
                    DebtAccountSnapshot
                > {
                    it.outstandingMinor
                }.thenBy {
                    it.name.lowercase()
                }
            )
    // Design B uses the confirmed local ledger; no remote score provider.
    val investmentStore = remember { com.kedevjimmy.pesatrack.data.local.InvestmentStore.get(context) }
    var observedPositions by remember {
        mutableStateOf(stateCache?.investmentsFor(ledgerUpdateState.revision))
    }
    var showInvestments by remember { mutableStateOf(false) }
    var showHomeHealthInfo by remember { mutableStateOf(false) }
    var showHealthEvidenceEditor by remember { mutableStateOf(false) }
    var healthEvidenceRevision by remember { mutableStateOf(0) }
    val healthEvidenceStore = remember(context) { FinancialHealthEvidenceStore(context) }
    val healthEvidence = remember(healthEvidenceRevision,ledgerUpdateState.revision) {
        healthEvidenceStore.snapshot()
    }
    var showNetFlowExplanation by remember { mutableStateOf(false) }
    var showHomeMonthPicker by remember { mutableStateOf(false) }
    var selectedHomeMonthOffset by remember { mutableStateOf(0) }
    val historyDayKey = remember {
        java.util.Calendar.getInstance().run {
            get(java.util.Calendar.YEAR) * 1000 + get(java.util.Calendar.DAY_OF_YEAR)
        }
    }
    val cachedHistory = remember(stateCache, ledgerUpdateState.revision, historyDayKey) {
        stateCache?.historyFor(ledgerUpdateState.revision, 0, historyDayKey)
    }
    var pastMonthSnapshot by remember {
        mutableStateOf<ActivityLedgerSnapshot?>(cachedHistory?.selected)
    }
    var priorAlignedSnapshot by remember {
        mutableStateOf<ActivityLedgerSnapshot?>(cachedHistory?.previousAligned)
    }
    var finishedMonthOffset by remember {
        mutableStateOf<Int?>(if (cachedHistory != null) 0 else null)
    }
    var historicalLoadFailed by remember { mutableStateOf(false) }
    LaunchedEffect(ledgerUpdateState.revision) {
        val revision = ledgerUpdateState.revision
        val existing = stateCache?.investmentsFor(revision)
        if (existing != null) {
            observedPositions = existing
            return@LaunchedEffect
        }
        try {
            val loaded = withContext(Dispatchers.IO) { investmentStore.observedPositions() }
            if (LedgerUpdateBus.state.value.revision == revision) {
                observedPositions = loaded
                stateCache?.saveInvestments(revision, loaded)
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { observedPositions = null }
    }
    LaunchedEffect(selectedHomeMonthOffset, ledgerUpdateState.revision, historyDayKey) {
        val requestedRevision = ledgerUpdateState.revision
        val requestedOffset = selectedHomeMonthOffset
        val cached = stateCache?.historyFor(requestedRevision, requestedOffset, historyDayKey)
        if (cached != null) {
            pastMonthSnapshot = cached.selected
            priorAlignedSnapshot = cached.previousAligned
            finishedMonthOffset = requestedOffset
            historicalLoadFailed = false
            return@LaunchedEffect
        }
        finishedMonthOffset = null
        historicalLoadFailed = false
        try {
            val historical = withContext(Dispatchers.IO) {
                val today = java.util.Calendar.getInstance()
                val monthStart = (today.clone() as java.util.Calendar).apply {
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                    add(java.util.Calendar.MONTH, -selectedHomeMonthOffset)
                }
                val next = (monthStart.clone() as java.util.Calendar).apply {
                    add(java.util.Calendar.MONTH, 1)
                }
                val end = if (selectedHomeMonthOffset == 0)
                    today.timeInMillis + 1L else next.timeInMillis
                val priorStart = (monthStart.clone() as java.util.Calendar).apply {
                    add(java.util.Calendar.MONTH, -1)
                }
                val priorEnd = if (selectedHomeMonthOffset == 0) {
                    (priorStart.clone() as java.util.Calendar).apply {
                        set(java.util.Calendar.DAY_OF_MONTH,
                            minOf(today.get(java.util.Calendar.DAY_OF_MONTH),
                                getActualMaximum(java.util.Calendar.DAY_OF_MONTH)))
                        set(java.util.Calendar.HOUR_OF_DAY,
                            today.get(java.util.Calendar.HOUR_OF_DAY))
                        set(java.util.Calendar.MINUTE, today.get(java.util.Calendar.MINUTE))
                        set(java.util.Calendar.SECOND, today.get(java.util.Calendar.SECOND))
                    }.timeInMillis + 1L
                } else monthStart.timeInMillis
                val previous = transactionStore.activitySnapshotBetween(
                    priorStart.timeInMillis, priorEnd, performMaintenance = false
                )
                val selected = if (selectedHomeMonthOffset == 0) null
                else transactionStore.activitySnapshotBetween(
                    monthStart.timeInMillis, end, performMaintenance = false
                )
                selected to previous
            }
            if (LedgerUpdateBus.state.value.revision != requestedRevision ||
                selectedHomeMonthOffset != requestedOffset) return@LaunchedEffect
            pastMonthSnapshot = historical.first
            priorAlignedSnapshot = historical.second
            finishedMonthOffset = requestedOffset
            stateCache?.saveHistory(requestedRevision, requestedOffset, historyDayKey,
                HomeHistorySnapshots(historical.first, historical.second))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            pastMonthSnapshot = null
            priorAlignedSnapshot = null
            historicalLoadFailed = true
        }
    }

    val expectedSalaryPrompt = remember(
        cycleSnapshot,
        salaryRule,
        salaryPromptDeferredUntil
    ) {
        expectedSalaryPrompt(
            cycleSnapshot = cycleSnapshot,
            salaryRule = salaryRule,
            nowEpochMs = System.currentTimeMillis(),
            deferredUntilEpochMs = salaryPromptDeferredUntil
        )
    }

    if (!homeLoaded) {
        LazyColumn(
            modifier = Modifier
                .padding(scaffoldPadding)
                .background(
                    MaterialTheme.colorScheme
                        .background
                ),
            contentPadding =
                PaddingValues(
                    horizontal = 16.dp,
                    vertical = 14.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
            item {
                GreetingBlock(
                    onProfileClick =
                        onProfileClick
                )
            }
            item {
                PesaCard {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                        verticalAlignment =
                            Alignment
                                .CenterVertically
                    ) {
                        if (homeLoading) CircularProgressIndicator(
                            modifier =
                                Modifier.size(
                                    26.dp
                                ),
                            strokeWidth = 2.dp
                        )
                        Column(
                            modifier =
                                Modifier.padding(
                                    start = 14.dp
                                )
                        ) {
                            Text(
                                if (homeError) "Your money picture could not load" else "Preparing your money picture",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                fontWeight =
                                    FontWeight
                                        .SemiBold
                            )
                            Text(
                                if (homeError) "Your saved data is still on this device. Try loading it again." else "Loading your accounts, pay cycle and balances.",
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                            if (homeError) {
                                TextButton(onClick = { refresh() }) { Text("Try again") }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    val planningAvailability = if (cycleSnapshot.availableToLiveMinor == Long.MIN_VALUE) null else
        PlanningAvailabilityPolicy
            .from(
                cycleSnapshot
                    .availableToLiveMinor
            )

    val spendSlices = remember(activitySnapshot, receivableShares) {
        try {
        activitySnapshot.trustedTransactions
            .filter {
                it.reviewState != ReviewState.EXCLUDED.name &&
                    it.reviewState != ReviewState.NEEDS_REVIEW.name &&
                    (
                        it.direction ==
                            MoneyDirection.EXPENSE.name ||
                            it.relationType ==
                                TransactionStore.RELATION_REFUND
                        )
            }
            .groupBy {
                categoryStore.canonicalId(it.category)
            }
            .mapValues { (_, items) ->
                maxOf(
                    0L,
                    items.checkedMoneySumOf { item ->
                        if (
                            item.relationType ==
                            TransactionStore.RELATION_REFUND
                        ) {
                            if (
                                refundCountsInTrustedTotals(
                                    item = item,
                                    store = transactionStore
                                )
                            ) {
                                CheckedMoney.negate(item.amountMinor)
                            } else {
                                0L
                            }
                        } else {
                            CheckedMoney.add(CheckedMoney.subtract(item.amountMinor, receivableShares[item.id]?.lentMinor ?: 0L), item.feeMinor)
                        }
                    }
                )
            }
            .filterValues { it > 0L }
            .entries
            .sortedByDescending { it.value }
            .mapIndexed { index, entry ->
                SpendingSlice(
                    label = categoryStore.labelFor(entry.key),
                    amountMinor = entry.value,
                    color = premiumCategoryColor(index)
                )
            }
        } catch (_: ArithmeticException) { null }
    }

    val spentTodayMinor = remember(
        activitySnapshot,
        receivableShares,
        ledgerUpdateState.revision
    ) { CheckedMoney.orNull {
        val startCalendar =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val startOfToday =
            startCalendar.timeInMillis
        val startOfTomorrow =
            (startCalendar.clone() as Calendar)
                .apply {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
                .timeInMillis

        maxOf(
            0L,
            activitySnapshot.trustedTransactions
                .filter {
                    it.occurredAtEpochMs >=
                        startOfToday &&
                        it.occurredAtEpochMs <
                            startOfTomorrow &&
                        it.reviewState !=
                            ReviewState.EXCLUDED.name &&
                        it.reviewState !=
                            ReviewState.NEEDS_REVIEW.name &&
                        (
                            it.direction ==
                                MoneyDirection.EXPENSE.name ||
                                it.relationType ==
                                    TransactionStore
                                        .RELATION_REFUND
                            )
                }
                .checkedMoneySumOf { item ->
                    if (
                        item.relationType ==
                        TransactionStore.RELATION_REFUND
                    ) {
                        if (
                            refundCountsInTrustedTotals(
                                item = item,
                                store = transactionStore
                            )
                        ) {
                            CheckedMoney.negate(item.amountMinor)
                        } else {
                            0L
                        }
                    } else {
                        CheckedMoney.add(CheckedMoney.subtract(item.amountMinor, receivableShares[item.id]?.lentMinor ?: 0L), item.feeMinor)
                    }
                }
        )
    } }

    val homeSpendingItems =
        spendSlices?.take(3)
            ?.map {
                HomeSpendingItem(
                    label = it.label,
                    amountMinor =
                        it.amountMinor,
                    color = it.color
                )
            }
    val totalTrustedSpendingMinor = CheckedMoney.orNull {
        maxOf(
            0L,
            CheckedMoney.total(activitySnapshot.summary.spentMinor, activitySnapshot.summary.feesMinor,
                CheckedMoney.negate(activitySnapshot.summary.refundMinor))
        )
    }
    val unresolvedDuplicateSecondIds =
        activitySnapshot
            .possibleDuplicates
            .map {
                it.second.id
            }
            .toSet()
    val recentCanonicalTransactions =
        activitySnapshot.trustedTransactions
            .asSequence()
            .filter {
                it.reviewState !=
                    ReviewState.EXCLUDED.name
            }
            .filter {
                it.id !in
                    unresolvedDuplicateSecondIds
            }
            .sortedByDescending {
                it.occurredAtEpochMs
            }
            .take(5)
            .toList()
    val healthBudgetTotals = remember(monthlyBudgets, budgetSpendingByCategory) {
        financialHealthBudgetTotals(
            budgets = monthlyBudgets,
            spendingByCategory = budgetSpendingByCategory,
            canonicalCategoryId = categoryStore::canonicalId
        )
    }
    val monthlyBudgetTotalMinor = healthBudgetTotals.limitMinor
    val monthlyBudgetSpentMinor = healthBudgetTotals.spentMinor
    val activeDebtOutstandingMinor =
        if (activeDebtAccounts.any { it.balanceQuality == com.kedevjimmy.pesatrack.data.local.DebtBalanceQuality.UNKNOWN }) null
        else CheckedMoney.orNull { activeDebtAccounts.checkedMoneySumOf { it.outstandingMinor } }
    val activeSalaryRule =
        salaryRule
    if (
        showManualSalaryDialog &&
        activeSalaryRule != null &&
        expectedSalaryPrompt != null
    ) {
        ManualSalaryEntryDialog(
            prompt = expectedSalaryPrompt,
            onDismiss = {
                showManualSalaryDialog = false
            },
            onSave = { amountMinor, occurredAtEpochMs ->
                val inserted = transactionStore.insertManualIncome(
                    amountMinor = amountMinor,
                    occurredAtEpochMs = occurredAtEpochMs,
                    label =
                        activeSalaryRule
                            .displayLabel
                            ?: activeSalaryRule
                                .name
                                .ifBlank {
                                    "Salary"
                                },
                    matchedRule =
                        activeSalaryRule
                )
                if (inserted) {
                    preferences.edit()
                        .remove("salary_prompt_deferred_until")
                        .apply()
                    salaryPromptDeferredUntil = 0L
                    showManualSalaryDialog = false
                    refresh()
                }
                inserted
            }
        )
    }

    val maximumProtectionMinor = CheckedMoney.orNull {
        CheckedMoney.add(cycleSnapshot.availableToLiveMinor, cycleSnapshot.userProtectedMinor)
    }
    if (showProtectionDialog && maximumProtectionMinor == null) {
        AlertDialog(onDismissRequest = { showProtectionDialog = false },
            title = { Text("Amount unavailable") },
            text = { Text("This planning amount cannot be represented safely.") },
            confirmButton = { TextButton(onClick = { showProtectionDialog = false }) { Text("Close") } })
    }
    if (
        showProtectionDialog &&
        cycleSnapshot.cycle != null && maximumProtectionMinor != null
    ) {
        ProtectMoneyDialog(
            currentMinor =
                cycleSnapshot
                    .userProtectedMinor,
            maximumSuggestedMinor =
                maxOf(
                    0L,
                    maximumProtectionMinor
                ),
            onDismiss = {
                showProtectionDialog =
                    false
            },
            onSave = { amountMinor ->
                val activeCycle =
                    cycleSnapshot.cycle
                if (activeCycle == null) {
                    false
                } else {
                    cycleProtectionStore.set(
                        cycleStartTransactionId =
                            activeCycle.startTransactionId,
                        amountMinor = amountMinor
                    )
                    showProtectionDialog = false
                    refresh()
                    true
                }
            }
        )
    }

    val homeConfigurationLocale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val selectedCalendar = remember(selectedHomeMonthOffset) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -selectedHomeMonthOffset)
        }
    }
    val selectedMonthText = monthLabel(selectedCalendar.timeInMillis)
    val chosenActivity = if(selectedHomeMonthOffset == 0) activitySnapshot
        else if(finishedMonthOffset == selectedHomeMonthOffset) pastMonthSnapshot
        else null
    val priorActivity = if(finishedMonthOffset == selectedHomeMonthOffset)
        priorAlignedSnapshot else null
    val selectedMoneyIn = chosenActivity?.summary?.incomeMinor
    val selectedSpending = chosenActivity?.summary?.let { summary ->
        CheckedMoney.orNull {
            maxOf(0L, CheckedMoney.total(summary.spentMinor,summary.feesMinor,
                CheckedMoney.negate(summary.refundMinor)))
        }
    }
    val selectedNetFlow = if(selectedMoneyIn != null && selectedSpending != null)
        CheckedMoney.orNull { CheckedMoney.subtract(selectedMoneyIn,selectedSpending) }
        else null
    val comparablePriorSpending = priorActivity?.summary?.let { summary ->
        CheckedMoney.orNull {
            maxOf(0L, CheckedMoney.total(summary.spentMinor,summary.feesMinor,
                CheckedMoney.negate(summary.refundMinor)))
        }
    }
    val daysRemaining = if(selectedHomeMonthOffset == 0)
        monthDaysRemaining(selectedCalendar.timeInMillis, System.currentTimeMillis())
        else null
    val selectedTrusted = chosenActivity?.trustedTransactions.orEmpty()
        .filter { it.reviewState != ReviewState.EXCLUDED.name }
    val uncategorizedFraction = chosenActivity?.let {
        val all = CheckedMoney.orNull {
            selectedTrusted.filter { row ->
                row.direction == MoneyDirection.EXPENSE.name &&
                    row.reviewState != ReviewState.NEEDS_REVIEW.name
            }.checkedMoneySumOf { row -> row.amountMinor }
        }
        val unknown = CheckedMoney.orNull {
            selectedTrusted.filter { row ->
                row.direction == MoneyDirection.EXPENSE.name &&
                    row.reviewState != ReviewState.NEEDS_REVIEW.name &&
                    categoryStore.canonicalId(row.category) in setOf("OTHER","UNCATEGORIZED")
            }.checkedMoneySumOf { row -> row.amountMinor }
        }
        if(all != null && all > 0L && unknown != null) unknown.toDouble()/all.toDouble()
        else null
    }
    val trustedRows = chosenActivity?.trustedTransactions?.count {
        it.reviewState != ReviewState.EXCLUDED.name &&
            it.reviewState != ReviewState.NEEDS_REVIEW.name
    } ?: 0
    val financialHealth = evaluateHomeFinancialSignals(
        HomeFinancialSignalsInput(
            budgetLimitMinor = if(selectedHomeMonthOffset==0) monthlyBudgetTotalMinor else null,
            budgetSpentMinor = if(selectedHomeMonthOffset==0) monthlyBudgetSpentMinor else null,
            uncategorizedShare = uncategorizedFraction,
            currentSpentMinor = selectedSpending,
            previousAlignedSpentMinor = comparablePriorSpending,
            incomeMinor = selectedMoneyIn,
            debtOutstandingMinor = if(selectedHomeMonthOffset==0)
                activeDebtOutstandingMinor else null,
            unknownDebtBalances = activeDebtAccounts.any {
                it.balanceQuality == com.kedevjimmy.pesatrack.data.local.DebtBalanceQuality.UNKNOWN
            },
            feesMinor = chosenActivity?.summary?.feesMinor,
            currentTrustedRows = trustedRows,
            previousTrustedRows = priorActivity?.trustedTransactions?.size ?: 0,
            // Only actual financial transaction reviews restrict score trust.
            // Optional identity or relationship prompts remain actionable, but
            // they are not proof that every trusted cash flow is inaccurate.
            pendingReviews = chosenActivity?.summary?.needsReviewCount ?: 0,
            confirmedAccountCount = visibleAccountGroups.size,
            hasConfirmedSalaryCycle = selectedHomeMonthOffset==0 &&
                cycleSnapshot.cycle != null,
            debtAccountCount = activeDebtAccounts.size,
            remainingCommitmentsMinor = if(selectedHomeMonthOffset==0 &&
                cycleSnapshot.cycle!=null) cycleSnapshot.remainingCommitmentsMinor else null,
            // Emergency reserves and required debt repayment schedules are not
            // proven by account balances, transfer labels or last repayments.
            confirmedEmergencyReserveMinor = healthEvidence.emergencyReserveMinor,
            confirmedEssentialCostsMinor = healthEvidence.essentialMonthlyMinor,
            confirmedMonthlyDebtPaymentsMinor = healthEvidence.requiredDebtPaymentsMinor,
            elapsedMonthFraction = if(selectedHomeMonthOffset==0) {
                val today=Calendar.getInstance()
                today.get(Calendar.DAY_OF_MONTH).toDouble() /
                    today.getActualMaximum(Calendar.DAY_OF_MONTH).toDouble()
            } else null,
            isCurrentMonth = selectedHomeMonthOffset==0
        )
    )
    val monthTransactions = if (selectedHomeMonthOffset == 0)
        recentCanonicalTransactions.take(3)
    else selectedTrusted.sortedByDescending { it.occurredAtEpochMs }.take(3)
    if(showHomeMonthPicker) AlertDialog(
        onDismissRequest={showHomeMonthPicker=false},
        title={Text("Choose month")},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                (0..8).forEach { offset ->
                    val c=Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH,1)
                        add(Calendar.MONTH,-offset)
                    }
                    TextButton(onClick={
                        selectedHomeMonthOffset=offset
                        showHomeMonthPicker=false
                    },modifier=Modifier.fillMaxWidth()){
                        Text(monthLabel(c.timeInMillis))
                    }
                }
            }
        },
        confirmButton={TextButton(onClick={showHomeMonthPicker=false}){Text("Close")}}
    )
    if(showHomeHealthInfo) AlertDialog(
        onDismissRequest={showHomeHealthInfo=false},
        title={Text("Your financial guidance")},
        text={
            LazyColumn(modifier=Modifier.heightIn(max=510.dp),
                verticalArrangement=Arrangement.spacedBy(10.dp)) {
                item {
                    Text("Based only on saved, reviewed local financial records. This is not a credit score, a licensed financial assessment, or a live balance.",
                        style=MaterialTheme.typography.bodySmall)
                    Text(financialHealth.explanation,
                        style=MaterialTheme.typography.bodyMedium)
                }
                items(financialHealth.indicators.size) { index ->
                    val indicator=financialHealth.indicators[index]
                    Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        Text(indicator.title+" • "+(indicator.score?.let { it.toString()+"/100" }
                            ?:"Evidence needed"),fontWeight=FontWeight.SemiBold)
                        Text(indicator.reason,style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    Text("Data confidence • "+(financialHealth.confidence.score?.let {
                        it.toString()+"/100" }?:"Unknown"),
                        fontWeight=FontWeight.SemiBold)
                    Text(financialHealth.confidence.reason,
                        style=MaterialTheme.typography.bodySmall)
                }
                if(financialHealth.actions.isNotEmpty()) {
                    item {
                        Text("Useful next steps",
                            style=MaterialTheme.typography.titleSmall,
                            fontWeight=FontWeight.Bold)
                    }
                    items(financialHealth.actions.size) { index ->
                        val action=financialHealth.actions[index]
                        Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
                            Text(action.title,fontWeight=FontWeight.SemiBold)
                            Text(action.detail,style=MaterialTheme.typography.bodySmall,
                                color=MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick={
                                showHomeHealthInfo=false
                                when(action.destination) {
                                    "activity" -> onActivityClick()
                                    "debts" -> onDebtsClick()
                                    "accounts" -> onAccountsClick()
                                    "salary" -> onSalaryClick()
                                    else -> onPlanClick()
                                }
                            }) {
                                Text("Open "+when(action.destination) {
                                    "activity" -> "Activity"
                                    "debts" -> "Debts"
                                    "accounts" -> "Accounts"
                                    "salary" -> "Salary"
                                    else -> "Plan"
                                })
                            }
                        }
                    }
                }
                item {
                    Text("Why no overall rating? PesaTrack does not assume missing emergency savings or loan repayments are zero. Confirmed balances and repayment schedules are required for a complete assessment.",
                        style=MaterialTheme.typography.labelSmall)
                }
            }
        },
        dismissButton={TextButton(onClick={showHomeHealthInfo=false}){Text("Close")}},
        confirmButton={TextButton(onClick={
            showHomeHealthInfo=false
            showHealthEvidenceEditor=true
        }){Text("Update amounts")}}
    )
    if(showHealthEvidenceEditor) FinancialHealthEvidenceDialog(
        store=healthEvidenceStore,
        onDismiss={showHealthEvidenceEditor=false},
        onSaved={
            showHealthEvidenceEditor=false
            healthEvidenceRevision++
            showHomeHealthInfo=true
        }
    )
    if(showNetFlowExplanation) AlertDialog(
        onDismissRequest={showNetFlowExplanation=false},
        title={Text("About net flow")},
        text={Text("Net flow is recorded income minus trusted spending and recorded fees, adjusted for refunds. It is NOT your live bank balance or spendable money after bills. Transfers and loan principal should remain separate.")},
        confirmButton={TextButton(onClick={showNetFlowExplanation=false}){Text("Close")}}
    )
    if(showInvestments) HomeBInvestmentsDialog(
        positions=observedPositions.orEmpty(),
        onDismiss={showInvestments=false}
    )

    LazyColumn(
        modifier = Modifier.padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal=13.dp,vertical=11.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item(key="b-greeting") {
            HomeBGreeting(
                preferences.getString("profile_name","").orEmpty()
                    .trim().split(Regex("\\s+")).firstOrNull().orEmpty()
            )
        }
        if(homeError) item(key="b-error") {
            Text("Showing the most recently recorded money data.",
                color=MaterialTheme.colorScheme.error,
                style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={refresh()}){Text("Retry")}
        }
        item(key="b-health") {
            HomeBHealth(financialHealth) { showHomeHealthInfo=true }
        }
        item(key="b-money") {
            HomeBMonthly(
                isCurrent=selectedHomeMonthOffset==0,
                month=SimpleDateFormat("MMMM yyyy",homeConfigurationLocale)
                    .format(selectedCalendar.time),
                daysLeft=daysRemaining,
                moneyIn=selectedMoneyIn?.let(::formatMoney)?:"Updating…",
                moneyOut=selectedSpending?.let(::formatMoney)?:"Updating…",
                netFlow=selectedNetFlow?.let(::formatMoney)?:"Unavailable",
                negative=(selectedNetFlow?:0L)<0L,
                inComparison=if(priorActivity==null || selectedMoneyIn==null)
                    null else periodPercentChange(selectedMoneyIn,
                        priorActivity.summary.incomeMinor),
                outComparison=if(comparablePriorSpending==null || selectedSpending==null)
                    null else periodPercentChange(selectedSpending,
                        comparablePriorSpending),
                progress=if(selectedMoneyIn==null || selectedSpending==null)null
                    else netFlowRingProgress(selectedMoneyIn,selectedSpending),
                onExplain={showNetFlowExplanation=true},
                onChooseMonth={showHomeMonthPicker=true}
            )
        }
        if(historicalLoadFailed && selectedHomeMonthOffset != 0) item {
            Text("This month could not be loaded. No figures were estimated.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.error)
        }
        if (selectedHomeMonthOffset == 0 && needsAttentionCount > 0) {
            item(key = "b-attention-insight") {
                HomeBInsight(
                    needsAttentionCount.toString() +
                        " transactions or relationships need review.",
                    onClick = onAttentionClick,
                    actionNeeded = true
                )
            }
        } else if (selectedHomeMonthOffset == 0 &&
            !recordedSpendingInsight.isNullOrBlank()
        ) {
            item(key = "b-insight") {
                HomeBInsight(
                    recordedSpendingInsight.orEmpty(),
                    onClick = onActivityClick,
                    actionNeeded = false
                )
            }
        }
        if(cycleSnapshot.pendingSalary != null) item(key="b-confirm-salary") {
            PendingSalaryCard(
                candidate=requireNotNull(cycleSnapshot.pendingSalary),
                onStartCycle={
                    cycleStore.confirmNewCycle(requireNotNull(cycleSnapshot.pendingSalary).transactionId)
                    refresh()
                },
                onKeepAsIncome={
                    cycleStore.keepAsAdditionalIncome(requireNotNull(cycleSnapshot.pendingSalary).transactionId)
                    refresh()
                }
            )
        }
        if(cycleSnapshot.unexplainedFundingGapMinor > 0L) item(key="b-gap") {
            FundingGapCard(cycleSnapshot.unexplainedFundingGapMinor)
        }
        item(key="b-upcoming-bills") {
            HomeUpcomingBillsCard(
                bills=upcomingBills,
                onBillsClick=onBillsClick
            )
        }
        item(key="b-recent") {
            HomeBRecent(
                rows=monthTransactions,
                category={categoryStore.labelFor(it)},
                money={formatMoney(it)},
                onOpen=onTransactionClick,
                onAll=onActivityClick
            )
        }
        item(key="b-tools-disclosure") {
            TextButton(
                onClick={homeMoneyToolsExpanded = !homeMoneyToolsExpanded},
                modifier=Modifier.fillMaxWidth()
            ) {
                Text(if(homeMoneyToolsExpanded) "Hide shortcuts" else "More money tools")
            }
        }
        if(homeMoneyToolsExpanded) {
            item(key="b-debt-tool") {
                HomeBTile(
                    "Debts & loans",
                    activeDebtOutstandingMinor?.let(::formatMoney)?:"Amount unavailable",
                    activeDebtAccounts.size.toString()+
                        if(activeDebtAccounts.size==1)" active debt" else " active debts",
                    androidx.compose.material.icons.Icons.Rounded.AccountBalance,
                    androidx.compose.ui.graphics.Color(0xFFB7A4FF),
                    onClick=onDebtsClick,
                    modifier=Modifier.fillMaxWidth()
                )
            }
            item(key="b-tools-pair") {
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    HomeBTile(
                        "Investments",
                        "Value unavailable",
                        observedPositions?.size?.let { n ->
                            n.toString()+if(n==1)" observed position" else " observed positions"
                        }?:"Checking records",
                        androidx.compose.material.icons.Icons.Rounded.TrendingUp,
                        androidx.compose.ui.graphics.Color(0xFF42E5D2),
                        onClick={showInvestments=true},modifier=Modifier.weight(1f)
                    )
                    HomeBTile(
                        "Enter salary",
                        if(cycleSnapshot.cycle==null)"Set up pay"
                        else "Pay cycle active",
                        "Review salary details",
                        androidx.compose.material.icons.Icons.Rounded.Paid,
                        androidx.compose.ui.graphics.Color(0xFF39BDF5),
                        onClick=onSalaryClick,modifier=Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun refundCountsInTrustedTotals(
    item: com.kedevjimmy.pesatrack.data.local.StoredTransaction,
    store: TransactionStore
): Boolean {
    val originalId = item.relatedTransactionId ?: return false
    val original = store.getById(originalId) ?: return false
    return original.direction == MoneyDirection.EXPENSE.name &&
        original.reviewState != ReviewState.EXCLUDED.name &&
        original.reviewState != ReviewState.NEEDS_REVIEW.name &&
        original.type !=
            com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
                .REVERSAL.name
}

@Composable
private fun HomeConceptAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBubble(
                icon = icon,
                size = 38.dp,
                background = MaterialTheme.colorScheme.primaryContainer,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                label,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GreetingBlock(
    onProfileClick: () -> Unit
) {
    val hour = remember {
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(50.dp)
                .clickable(onClick = onProfileClick),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = "Open profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(27.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                greeting,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Small steps. Bigger tomorrows.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PendingSalaryCard(
    candidate: SalaryCycleCandidate,
    onStartCycle: () -> Unit,
    onKeepAsIncome: () -> Unit
) {
    PesaCard {
        Column(Modifier.padding(15.dp)) {
            Text(
                "Salary check",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                candidate.label + " • " + formatMoney(candidate.amountMinor),
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                candidate.reason,
                modifier = Modifier.padding(top = 3.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onKeepAsIncome,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Extra income")
                }
                androidx.compose.material3.Button(
                    onClick = onStartCycle,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start cycle")
                }
            }
        }
    }
}

private data class ExpectedSalaryPrompt(
    val windowStartEpochMs: Long,
    val windowEndEpochMs: Long,
    val currentCycleStartEpochMs: Long
)

private fun expectedSalaryPrompt(
    cycleSnapshot: PayCycleSnapshot,
    salaryRule: SmartRule?,
    nowEpochMs: Long,
    deferredUntilEpochMs: Long
): ExpectedSalaryPrompt? {
    val cycle = cycleSnapshot.cycle ?: return null
    if (cycleSnapshot.pendingSalary != null) return null
    if (nowEpochMs < deferredUntilEpochMs) return null

    val window = SalaryExpectationPolicy.overdueWindow(
        lastSalaryEpochMs = cycle.startEpochMs,
        payWindowStartDay = salaryRule?.payWindowStartDay,
        payWindowEndDay = salaryRule?.payWindowEndDay,
        nowEpochMs = nowEpochMs
    ) ?: return null

    return ExpectedSalaryPrompt(
        windowStartEpochMs = window.startEpochMs,
        windowEndEpochMs = window.endEpochMs,
        currentCycleStartEpochMs = cycle.startEpochMs
    )
}

@Composable
private fun ExpectedSalaryCard(
    prompt: ExpectedSalaryPrompt,
    onAddSalary: () -> Unit,
    onNotPaidYet: () -> Unit
) {
    PesaCard {
        Column(Modifier.padding(15.dp)) {
            Text(
                "Salary not detected",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                "Your usual payday window ended " +
                    formatShortDate(prompt.windowEndEpochMs) +
                    ".",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "PesaTrack is still using the pay cycle that started " +
                    formatShortDate(prompt.currentCycleStartEpochMs) +
                    ". No future salary has been added.",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNotPaidYet,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Not yet received")
                }
                Button(
                    onClick = onAddSalary,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add salary")
                }
            }
        }
    }
}

@Composable
private fun ProtectMoneyDialog(
    currentMinor: Long,
    maximumSuggestedMinor: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Boolean
) {
    var amount by remember(
        currentMinor
    ) {
        mutableStateOf(
            if (currentMinor > 0L) {
                java.math.BigDecimal(
                    currentMinor
                )
                    .movePointLeft(2)
                    .stripTrailingZeros()
                    .toPlainString()
            } else {
                ""
            }
        )
    }
    var error by remember(
        currentMinor
    ) {
        mutableStateOf<String?>(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Protect money"
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {
                Text(
                    "Use this when money stays in a normal bank or wallet but you do not want PesaTrack to treat it as available for everyday spending. This does not move the money.",
                    style =
                        MaterialTheme.typography
                            .bodyMedium,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        error = null
                    },
                    label = {
                        Text(
                            "Amount to protect"
                        )
                    },
                    prefix = {
                        Text("KES ")
                    },
                    supportingText = {
                        Text(
                            if (
                                maximumSuggestedMinor >
                                0L
                            ) {
                                "Current known unprotected resources: " +
                                    formatMoney(
                                        maximumSuggestedMinor
                                    )
                            } else {
                                "Enter 0 to remove the protection."
                            }
                        )
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Decimal
                        ),
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine = true
                )
                error?.let {
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
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed =
                        if (
                            amount.trim()
                                .isBlank()
                        ) {
                            0L
                        } else {
                            parseHomeMoneyMinor(
                                amount
                            )
                        }
                    error =
                        when {
                            parsed == null ->
                                "Enter a valid amount."
                            parsed < 0L ->
                                "Protected money cannot be negative."
                            parsed >
                                maximumSuggestedMinor ->
                                "That is more than the cash PesaTrack currently knows is available."
                            !onSave(
                                parsed
                            ) ->
                                "Could not save this protection."
                            else -> null
                        }
                }
            ) {
                Text(
                    if (
                        currentMinor > 0L
                    ) {
                        "Save"
                    } else {
                        "Protect"
                    }
                )
            }
        }
    )
}

@Composable
private fun ManualSalaryEntryDialog(
    prompt: ExpectedSalaryPrompt,
    onDismiss: () -> Unit,
    onSave: (Long, Long) -> Boolean
) {
    val currentLocale =
        androidx.compose.ui.platform
            .LocalConfiguration
            .current
            .locales[0]
    var amount by remember(prompt.windowEndEpochMs) {
        mutableStateOf("")
    }
    var date by remember(
        prompt.windowEndEpochMs,
        currentLocale
    ) {
        mutableStateOf(
            SimpleDateFormat(
                "dd/MM/yyyy",
                currentLocale
            ).format(Date(prompt.windowEndEpochMs))
        )
    }
    var error by remember(prompt.windowEndEpochMs) {
        mutableStateOf<String?>(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add salary manually") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Only add this if the salary actually arrived and is not already visible in Activity. If the payment is there, classify that transaction instead. The date you enter becomes the start of the new pay cycle.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        error = null
                    },
                    label = { Text("Salary amount") },
                    prefix = { Text("KES ") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = {
                        date = it
                        error = null
                    },
                    label = { Text("Date salary arrived") },
                    placeholder = { Text("25/09/2026") },
                    supportingText = {
                        Text("Day / month / year")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                error?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountMinor =
                        parseHomeMoneyMinor(amount)
                    val occurredAt =
                        parseHomeDate(date)

                    error = when {
                        amountMinor == null ||
                            amountMinor <= 0L ->
                            "Enter a salary amount above zero."
                        occurredAt == null ->
                            "Enter a valid date, for example 25/09/2026."
                        occurredAt >
                            System.currentTimeMillis() ->
                            "Salary cannot be dated in the future."
                        occurredAt <=
                            prompt.currentCycleStartEpochMs ->
                            "The new salary must be after the current cycle started."
                        !onSave(
                            amountMinor,
                            occurredAt
                        ) ->
                            "That salary entry is already recorded."
                        else -> null
                    }
                }
            ) {
                Text("Add salary")
            }
        }
    )
}

private fun parseHomeMoneyMinor(raw: String): Long? =
    runCatching {
        raw.replace(",", "")
            .trim()
            .toBigDecimal()
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }.getOrNull()

private fun parseHomeDate(raw: String): Long? =
    runCatching {
        SimpleDateFormat(
            "d/M/yyyy",
            Locale.getDefault()
        ).apply {
            isLenient = false
        }.parse(raw.trim())?.time
    }.getOrNull()

private fun homeInsight(
    cycleSnapshot: PayCycleSnapshot,
    needsReviewCount: Int,
    recordedSpendingInsight: String? = null
): String {
    val paydaySnapshot =
        cycleSnapshot.paydaySnapshot

    return when {
    needsReviewCount > 0 ->
        "$needsReviewCount transaction" +
            if (needsReviewCount == 1) {
                " is waiting for review before it enters trusted totals."
            } else {
                "s are waiting for review before they enter trusted totals."
            }
    paydaySnapshot != null &&
        paydaySnapshot.unknownLiquidAccountCount > 0 ->
        paydaySnapshot
            .unknownLiquidAccountCount
            .toString() +
            " spendable account balance" +
            if (
                paydaySnapshot
                    .unknownLiquidAccountCount ==
                1
            ) {
                " was"
            } else {
                "s were"
            } +
            " unknown at payday. Your carried-in amount is conservative."

    cycleSnapshot.unexplainedFundingGapMinor > 0L ->
        "Some spending still needs a funding source. Reconcile it before relying on the full picture."
    recordedSpendingInsight != null -> recordedSpendingInsight
    cycleSnapshot.cycle == null ->
        "Once a salary cycle is recognized, this space will explain your real spending and commitments."
    cycleSnapshot.remainingCommitmentsMinor > 0L ->
        formatMoney(cycleSnapshot.remainingCommitmentsMinor) +
            " is still protected for commitments this cycle."
    else ->
        "Your tracked commitments for this cycle are covered. Keep reviewing new transactions as they arrive."
    }
}

private fun formatMoney(minor: Long?): String =
    minor?.let(::formatKesAmount) ?: "Amount unavailable"

private fun formatShortDate(epochMs: Long): String =
    SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(epochMs))
