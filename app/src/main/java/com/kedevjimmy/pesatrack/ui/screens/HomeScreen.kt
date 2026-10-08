package com.kedevjimmy.pesatrack.ui.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.TrackChanges
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
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot
import com.kedevjimmy.pesatrack.data.local.ActivitySummary
import com.kedevjimmy.pesatrack.data.local.BudgetStore
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.CycleProtectionStore
import com.kedevjimmy.pesatrack.data.local.DebtAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.DebtStore
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

data class HomeLoadedState(
    val cycleSnapshot: PayCycleSnapshot,
    val ledgerSnapshot: LedgerSnapshot,
    val activitySnapshot: ActivityLedgerSnapshot,
    val debtAccounts: List<DebtAccountSnapshot>,
    val trackedAccounts: List<MoneyAccount>,
    val monthlyBudgets: List<MonthlyBudget>,
    val confirmedAccountIds: Set<String>,
    val salaryRule: SmartRule?,
    val upcomingBills: List<ScheduledBill>,
    val needsAttentionCount: Int,
    val accountBalanceSnapshots:
        List<PaydayAccountSnapshot>,
    val moneyPictureHealth:
        MoneyPictureHealth
)

class HomeScreenCache {
    var data: HomeLoadedState? = null
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

    suspend fun loadHomeState(
        rebuildCycles: Boolean
    ): HomeLoadedState =
        withContext(Dispatchers.IO) {
            if (rebuildCycles) {
                cycleStore.rebuild()
            }
            val referenceEpochMs =
                System.currentTimeMillis()
            val accounts =
                accountStore.listAccounts(
                    enabledOnly = true
                )
            val balanceSnapshots =
                PaydaySnapshotStore
                    .get(context)
                    .currentAccountSnapshots(
                        accounts = accounts,
                        nowEpochMs =
                            referenceEpochMs
                    )
            HomeLoadedState(
                cycleSnapshot =
                    cycleStore.snapshot(),
                ledgerSnapshot =
                    transactionStore.snapshot(),
                activitySnapshot =
                    transactionStore
                        .activitySnapshot(
                            nowEpochMs =
                                referenceEpochMs
                        ),
                debtAccounts =
                    debtStore.accounts(),
                trackedAccounts =
                    accounts,
                monthlyBudgets =
                    budgetStore.list(),
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
                accountBalanceSnapshots =
                    balanceSnapshots,
                moneyPictureHealth =
                    MoneyPictureHealthEvaluator(
                        context
                    ).evaluate(
                        nowEpochMs =
                            referenceEpochMs
                    )
            )
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
                    val loaded =
                        loadHomeState(
                            rebuildCycles =
                                shouldRebuild
                        )
                    cycleSnapshot =
                        loaded.cycleSnapshot
                    ledgerSnapshot =
                        loaded.ledgerSnapshot
                    activitySnapshot =
                        loaded.activitySnapshot
                    debtAccounts =
                        loaded.debtAccounts
                    trackedAccounts =
                        loaded.trackedAccounts
                    monthlyBudgets =
                        loaded.monthlyBudgets
                    confirmedAccountIds =
                        loaded.confirmedAccountIds
                    salaryRule =
                        loaded.salaryRule
                    upcomingBills =
                        loaded.upcomingBills
                    needsAttentionCount =
                        loaded.needsAttentionCount
                    accountBalanceSnapshots =
                        loaded
                            .accountBalanceSnapshots
                    moneyPictureHealth =
                        loaded.moneyPictureHealth
                    stateCache?.data = loaded
                    homeLoaded = true
                } while (refreshPending)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                android.util.Log.e("PesaTrackHome", "Home data could not be loaded", failure)
                homeError = true
            } finally {
                homeLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    LaunchedEffect(
        ledgerUpdateState.revision,
        homeLoaded
    ) {
        if (
            homeLoaded &&
            ledgerUpdateState.revision > 0L
        ) {
            refresh()
        }
    }

    val visibleAccountGroups =
        AccountDisplayPolicy.groups(
            trackedAccounts
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
    val activeDebtAccounts =
        debtAccounts
            .filter {
                it.outstandingMinor > 0L
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
                PesaTrackTopBar(
                    onProfileClick =
                        onProfileClick,
                    showProfile = false
                )
            }
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

    val planningAvailability =
        PlanningAvailabilityPolicy
            .from(
                cycleSnapshot
                    .availableToLiveMinor
            )

    val spendSlices = remember(activitySnapshot) {
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
                    items.sumOf { item ->
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
                                -item.amountMinor
                            } else {
                                0L
                            }
                        } else {
                            item.amountMinor + item.feeMinor
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
    }

    val spentTodayMinor = remember(
        activitySnapshot,
        ledgerUpdateState.revision
    ) {
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
                .sumOf { item ->
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
                            -item.amountMinor
                        } else {
                            0L
                        }
                    } else {
                        item.amountMinor +
                            item.feeMinor
                    }
                }
        )
    }

    val homeSpendingItems =
        spendSlices.take(3)
            .map {
                HomeSpendingItem(
                    label = it.label,
                    amountMinor =
                        it.amountMinor,
                    color = it.color
                )
            }
    val totalTrustedSpendingMinor =
        maxOf(
            0L,
            activitySnapshot
                .summary
                .spentMinor +
                activitySnapshot
                    .summary
                    .feesMinor -
                activitySnapshot
                    .summary
                    .refundMinor
        )
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
    val monthlyBudgetTotalMinor =
        monthlyBudgets.sumOf { it.limitMinor }
    val monthlyBudgetCategoryIds =
        monthlyBudgets.map { it.categoryId }.toSet()
    val monthlyBudgetSpentMinor =
        activitySnapshot.trustedTransactions
            .filter {
                it.direction == MoneyDirection.EXPENSE.name &&
                    it.reviewState != ReviewState.EXCLUDED.name &&
                    it.reviewState != ReviewState.NEEDS_REVIEW.name &&
                    categoryStore.canonicalId(it.category) in monthlyBudgetCategoryIds
            }
            .sumOf { it.amountMinor + it.feeMinor }
            .coerceAtLeast(0L)

    val activeDebtOutstandingMinor =
        activeDebtAccounts.sumOf {
            it.outstandingMinor
        }
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

    if (
        showProtectionDialog &&
        cycleSnapshot.cycle != null
    ) {
        ProtectMoneyDialog(
            currentMinor =
                cycleSnapshot
                    .userProtectedMinor,
            maximumSuggestedMinor =
                maxOf(
                    0L,
                    cycleSnapshot
                        .startingFundsMinor +
                        cycleSnapshot
                            .additionalIncomeMinor +
                        cycleSnapshot
                            .refundsMinor -
                        cycleSnapshot
                            .spentMinor -
                        cycleSnapshot
                            .feesMinor -
                        cycleSnapshot
                            .savingsMinor -
                        cycleSnapshot
                            .remainingCommitmentsMinor
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

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PesaTrackTopBar(
                onProfileClick = onProfileClick,
                showProfile = false
            )
        }

        item {
            GreetingBlock(onProfileClick = onProfileClick)
        }

        item {
            ConceptEmeraldHero(
                title = "Tracked funds",
                amount =
                    if (cycleSnapshot.cycle == null) {
                        "Waiting for salary"
                    } else {
                        formatMoney(planningAvailability.availableMinor)
                    },
                subtitle =
                    if (planningAvailability.shortfallMinor > 0L) {
                        "Planning shortfall " + formatMoney(planningAvailability.shortfallMinor)
                    } else if (cycleSnapshot.cycle != null) {
                        "Available after commitments • updated from your real activity"
                    } else {
                        "PesaTrack will calculate this after your first salary is recorded."
                    },
                eyebrow = visibleAccountGroups.size.toString() + " tracked account" +
                    if (visibleAccountGroups.size == 1) "" else "s"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ConceptMetricTile(
                    icon = Icons.Rounded.SouthWest,
                    label = "Money in",
                    value = formatMoney(activitySnapshot.summary.incomeMinor),
                    modifier = Modifier.weight(1f),
                    positive = true
                )
                ConceptMetricTile(
                    icon = Icons.Rounded.NorthEast,
                    label = "Spent",
                    value = formatMoney(
                        maxOf(
                            0L,
                            activitySnapshot.summary.spentMinor +
                                activitySnapshot.summary.feesMinor -
                                activitySnapshot.summary.refundMinor
                        )
                    ),
                    modifier = Modifier.weight(1f),
                    positive = false
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HomeConceptAction(
                    icon = Icons.Rounded.AddCircle,
                    label = "Add salary",
                    modifier = Modifier.weight(1f),
                    onClick = onSalaryClick
                )
                HomeConceptAction(
                    icon = Icons.Rounded.ReceiptLong,
                    label = "Bills",
                    modifier = Modifier.weight(1f),
                    onClick = onBillsClick
                )
                HomeConceptAction(
                    icon = Icons.Rounded.TrackChanges,
                    label = "Goals",
                    modifier = Modifier.weight(1f),
                    onClick = onGoalsClick
                )
                HomeConceptAction(
                    icon = Icons.Rounded.Insights,
                    label = "Reports",
                    modifier = Modifier.weight(1f),
                    onClick = onReportsClick
                )
            }
        }

        if (monthlyBudgetTotalMinor > 0L) {
            item {
                ConceptProgressCard(
                    title = "Monthly plan",
                    amount = formatMoney(
                        (monthlyBudgetTotalMinor - monthlyBudgetSpentMinor)
                            .coerceAtLeast(0L)
                    ) + " left",
                    helper =
                        formatMoney(monthlyBudgetSpentMinor) + " of " +
                            formatMoney(monthlyBudgetTotalMinor) + " used",
                    progress =
                        (monthlyBudgetSpentMinor.toFloat() /
                            monthlyBudgetTotalMinor.toFloat())
                            .coerceIn(0f, 1f),
                    modifier = Modifier.clickable(onClick = onPlanClick),
                    icon = Icons.Rounded.TrackChanges
                )
            }
        }

        cycleSnapshot.pendingSalary?.let {
                candidate ->
            item {
                PendingSalaryCard(
                    candidate = candidate,
                    onStartCycle = {
                        cycleStore
                            .confirmNewCycle(
                                candidate
                                    .transactionId
                            )
                        refresh()
                    },
                    onKeepAsIncome = {
                        cycleStore
                            .keepAsAdditionalIncome(
                                candidate
                                    .transactionId
                            )
                        refresh()
                    }
                )
            }
        }

        if (
            cycleSnapshot.pendingSalary ==
            null &&
            expectedSalaryPrompt != null
        ) {
            item {
                ExpectedSalaryCard(
                    prompt =
                        expectedSalaryPrompt,
                    onAddSalary = onSalaryClick,
                    onNotPaidYet = {
                        com.kedevjimmy.pesatrack.salary.SalaryReminders.defer(context)
                        salaryPromptDeferredUntil = preferences.getLong("salary_prompt_deferred_until", 0L)
                    }
                )
            }
        }

        if (needsAttentionCount > 0) {
            item {
                PesaCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClick =
                                    onAttentionClick
                            )
                ) {
                    Row(
                        modifier =
                            Modifier.padding(
                                15.dp
                            ),
                        verticalAlignment =
                            Alignment
                                .CenterVertically
                    ) {
                        IconBubble(
                            Icons.AutoMirrored
                                .Rounded
                                .FactCheck,
                            size = 44.dp,
                            background =
                                MaterialTheme
                                    .colorScheme
                                    .tertiaryContainer,
                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .onTertiaryContainer
                        )
                        Column(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(
                                        start = 12.dp
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
                                needsAttentionCount
                                    .toString() +
                                    " item" +
                                    if (
                                        needsAttentionCount ==
                                        1
                                    ) {
                                        " needs"
                                    } else {
                                        "s need"
                                    } +
                                    " your help",
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
                            Icons.Rounded
                                .ChevronRight,
                            contentDescription =
                                null,
                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (
            cycleSnapshot
                .unexplainedFundingGapMinor >
            0L
        ) {
            item {
                FundingGapCard(
                    cycleSnapshot
                        .unexplainedFundingGapMinor
                )
            }
        }

        item {
            HomeTodayCard(
                spentTodayMinor =
                    spentTodayMinor,
                safeTodayMinor =
                    cycleSnapshot
                        .safeTodayMinor,
                activeCycle =
                    cycleSnapshot.cycle !=
                        null,
                onProtectMoney = {
                    showProtectionDialog =
                        true
                }
            )
        }

        item {
            HomeUpcomingBillsCard(
                bills =
                    upcomingBills,
                onBillsClick =
                    onBillsClick
            )
        }

        item {
            val mpesaAccount =
                trackedAccounts
                    .firstOrNull {
                        it.id ==
                            AccountStore
                                .DEFAULT_MPESA_ID
                    }
            HomeMoneyPictureCard(
                walletLabel =
                    mpesaAccount
                        ?.let {
                            AccountIdentityPresentation
                                .title(it)
                        }
                        ?: "M-Pesa",
                knownBalanceMinor =
                    accountBalanceSnapshots
                        .firstOrNull {
                            it.accountId ==
                                AccountStore
                                    .DEFAULT_MPESA_ID
                        }
                        ?.balanceMinor,
                balanceObservedAtEpochMs =
                    accountBalanceSnapshots
                        .firstOrNull {
                            it.accountId ==
                                AccountStore
                                    .DEFAULT_MPESA_ID
                        }
                        ?.referenceEpochMs,
                accountCount =
                    visibleAccountGroups.size,
                confidenceScore =
                    moneyPictureHealth.score,
                confidenceLabel =
                    moneyPictureHealth.label,
                debtOutstandingMinor =
                    activeDebtOutstandingMinor,
                debtAccountCount =
                    activeDebtAccounts.size,
                onAccountsClick =
                    onAccountsClick,
                onDebtsClick =
                    onDebtsClick
            )
        }

        item {
            HomeWhereMoneyWentCard(
                items =
                    homeSpendingItems,
                totalMinor =
                    totalTrustedSpendingMinor,
                onPlanClick =
                    onPlanClick,
                onReviewClick =
                    onActivityClick
            )
        }

        item {
            HomeRecentActivityCard(
                transactions =
                    recentCanonicalTransactions,
                categoryLabel = {
                    categoryStore
                        .labelFor(it)
                },
                onTransactionClick =
                    onTransactionClick,
                onSeeAll =
                    onActivityClick
            )
        }

        item {
            SmartInsightCard(
                title =
                    "One useful insight",
                body =
                    homeInsight(
                        cycleSnapshot =
                            cycleSnapshot,
                        needsReviewCount =
                            needsAttentionCount
                    )
            )
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
    needsReviewCount: Int
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
    cycleSnapshot.cycle == null ->
        "Once a salary cycle is recognized, this space will explain your real spending and commitments."
    cycleSnapshot.remainingCommitmentsMinor > 0L ->
        formatMoney(cycleSnapshot.remainingCommitmentsMinor) +
            " is still protected for commitments this cycle."
    else ->
        "Your tracked commitments for this cycle are covered. Keep reviewing new transactions as they arrive."
    }
}

private fun formatMoney(minor: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    return if (minor < 0) {
        "-KES " + formatter.format(-minor / 100.0)
    } else {
        "KES " + formatter.format(minor / 100.0)
    }
}

private fun formatShortDate(epochMs: Long): String =
    SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(epochMs))




