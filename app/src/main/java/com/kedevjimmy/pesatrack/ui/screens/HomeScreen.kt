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
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Tune
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
import com.kedevjimmy.pesatrack.data.local.LedgerSnapshot
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.MonthlyBudget
import com.kedevjimmy.pesatrack.data.local.PayCycleSnapshot
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
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
import com.kedevjimmy.pesatrack.ui.components.AvailableToLiveHero
import com.kedevjimmy.pesatrack.ui.components.BudgetProgressCard
import com.kedevjimmy.pesatrack.ui.components.BudgetProgressItem
import com.kedevjimmy.pesatrack.ui.components.FundingGapCard
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.MoneySummaryTile
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.PesaTrackTopBar
import com.kedevjimmy.pesatrack.ui.components.QuickAction
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.SmartInsightCard
import com.kedevjimmy.pesatrack.ui.components.SpendingDonutCard
import com.kedevjimmy.pesatrack.ui.components.SpendingSlice
import com.kedevjimmy.pesatrack.ui.components.premiumCategoryColor
import java.text.NumberFormat
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class HomeLoadedState(
    val cycleSnapshot: PayCycleSnapshot,
    val ledgerSnapshot: LedgerSnapshot,
    val activitySnapshot: ActivityLedgerSnapshot,
    val monthlyBudgets: List<MonthlyBudget>,
    val debtAccounts: List<DebtAccountSnapshot>,
    val trackedAccounts: List<MoneyAccount>,
    val confirmedAccountIds: Set<String>,
    val salaryRule: SmartRule?
)

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
    onReviewClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onRulesClick: () -> Unit = {},
    onGoalsClick: () -> Unit = {},
    onBudgetClick: () -> Unit = {},
    onAccountsClick: () -> Unit = {},
    onDebtsClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cycleStore = remember { PayCycleStore.get(context) }
    val transactionStore = remember { TransactionStore.get(context) }
    val accountStore = remember { AccountStore.get(context) }
    val budgetStore = remember { BudgetStore.get(context) }
    val categoryStore = remember { CategoryStore.get(context) }
    val debtStore = remember { DebtStore.get(context) }
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
    var showManualSalaryDialog by remember {
        mutableStateOf(false)
    }
    var showProtectionDialog by remember {
        mutableStateOf(false)
    }
    var homeLoading by remember {
        mutableStateOf(true)
    }
    var homeLoaded by remember {
        mutableStateOf(false)
    }

    var cycleSnapshot by remember {
        mutableStateOf(
            emptyPayCycleSnapshot()
        )
    }
    var ledgerSnapshot by remember {
        mutableStateOf(
            LedgerSnapshot(
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
            emptyActivitySnapshot()
        )
    }
    var monthlyBudgets by remember {
        mutableStateOf<
            List<MonthlyBudget>
        >(emptyList())
    }
    var debtAccounts by remember {
        mutableStateOf<
            List<DebtAccountSnapshot>
        >(emptyList())
    }
    var trackedAccounts by remember {
        mutableStateOf<
            List<MoneyAccount>
        >(emptyList())
    }
    var confirmedAccountIds by remember {
        mutableStateOf<Set<String>>(
            emptySet()
        )
    }
    var salaryRule by remember {
        mutableStateOf<SmartRule?>(
            null
        )
    }

    suspend fun loadHomeState(
        rebuildCycles: Boolean
    ): HomeLoadedState =
        withContext(Dispatchers.IO) {
            if (rebuildCycles) {
                cycleStore.rebuild()
            }
            HomeLoadedState(
                cycleSnapshot =
                    cycleStore.snapshot(),
                ledgerSnapshot =
                    transactionStore.snapshot(),
                activitySnapshot =
                    transactionStore
                        .activitySnapshot(),
                monthlyBudgets =
                    budgetStore.list(),
                debtAccounts =
                    debtStore.accounts(),
                trackedAccounts =
                    accountStore.listAccounts(
                        enabledOnly = true
                    ),
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
                        }
            )
        }

    fun refresh(
        rebuildCycles: Boolean = false
    ) {
        homeScope.launch {
            try {
                val loaded =
                    loadHomeState(
                        rebuildCycles =
                            rebuildCycles
                    )
                cycleSnapshot =
                    loaded.cycleSnapshot
                ledgerSnapshot =
                    loaded.ledgerSnapshot
                activitySnapshot =
                    loaded.activitySnapshot
                monthlyBudgets =
                    loaded.monthlyBudgets
                debtAccounts =
                    loaded.debtAccounts
                trackedAccounts =
                    loaded.trackedAccounts
                confirmedAccountIds =
                    loaded.confirmedAccountIds
                salaryRule =
                    loaded.salaryRule
                homeLoaded = true
            } finally {
                homeLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refresh(
            rebuildCycles = true
        )
    }

    LaunchedEffect(
        ledgerUpdateState.revision
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

    if (homeLoading) {
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
                        CircularProgressIndicator(
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
                                "Preparing your money picture",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                fontWeight =
                                    FontWeight
                                        .SemiBold
                            )
                            Text(
                                "Loading your accounts, pay cycle and balances without blocking the app.",
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
        }
        return
    }

    val spendSlices = remember(activitySnapshot) {
        activitySnapshot.transactions
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

    val budgetItems = remember(monthlyBudgets, activitySnapshot) {
        val trustedByCategory = activitySnapshot.transactions
            .filter {
                it.reviewState != ReviewState.EXCLUDED.name &&
                    it.reviewState != ReviewState.NEEDS_REVIEW.name
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

        monthlyBudgets
            .mapIndexed { index, budget ->
                val trackedMinor =
                    trustedByCategory[budget.categoryId] ?: 0L
                val difference = budget.limitMinor - trackedMinor
                BudgetProgressItem(
                    label = categoryStore.labelFor(budget.categoryId),
                    spentLabel = formatMoney(trackedMinor),
                    limitLabel = formatMoney(budget.limitMinor),
                    remainingLabel = if (difference >= 0L) {
                        formatMoney(difference) + " remaining"
                    } else {
                        formatMoney(-difference) + " over budget"
                    },
                    overBudget = difference < 0L,
                    progress = if (budget.limitMinor <= 0L) {
                        0f
                    } else {
                        trackedMinor.toFloat() / budget.limitMinor.toFloat()
                    },
                    color = premiumCategoryColor(index)
                )
            }
            .take(4)
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
            activitySnapshot.transactions
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

        cycleSnapshot.pendingSalary?.let { candidate ->
            item {
                PendingSalaryCard(
                    candidate = candidate,
                    onStartCycle = {
                        cycleStore.confirmNewCycle(candidate.transactionId)
                        refresh()
                    },
                    onKeepAsIncome = {
                        cycleStore.keepAsAdditionalIncome(candidate.transactionId)
                        refresh()
                    }
                )
            }
        }

        if (
            cycleSnapshot.pendingSalary == null &&
            expectedSalaryPrompt != null
        ) {
            item {
                ExpectedSalaryCard(
                    prompt = expectedSalaryPrompt,
                    onAddSalary = {
                        showManualSalaryDialog = true
                    },
                    onNotPaidYet = {
                        val deferUntil =
                            System.currentTimeMillis() +
                                24L * 60L * 60L * 1000L
                        preferences.edit()
                            .putLong(
                                "salary_prompt_deferred_until",
                                deferUntil
                            )
                            .apply()
                        salaryPromptDeferredUntil = deferUntil
                    }
                )
            }
        }

        item {
            AvailableToLiveHero(
                amount =
                    if (cycleSnapshot.cycle == null) {
                        "Waiting for salary"
                    } else {
                        formatMoney(
                            cycleSnapshot
                                .availableToLiveMinor
                        )
                    },
                subtitle =
                    if (cycleSnapshot.cycle == null) {
                        "PesaTrack will calculate this from real salary-cycle data."
                    } else {
                        "After bills, debt repayments, goals and protected commitments."
                    },
                footer = null,
                title = "Available after commitments",
                spentToday =
                    formatMoney(spentTodayMinor),
                safeToday =
                    cycleSnapshot.safeTodayMinor
                        ?.let(::formatMoney)
            )
        }

        if (cycleSnapshot.cycle != null) {
            item {
                PaydayContextCard(
                    snapshot =
                        cycleSnapshot,
                    onProtectMoney = {
                        showProtectionDialog =
                            true
                    }
                )
            }
        }

        item {
            val cycle = cycleSnapshot.cycle
            val helper = cycle?.let {
                "Since " + formatShortDate(it.startEpochMs)
            } ?: "This month"
            val cycleIncome =
                if (cycle != null) {
                    cycle.openingIncomeMinor +
                        cycleSnapshot.additionalIncomeMinor
                } else {
                    activitySnapshot.summary.incomeMinor
                }
            val cycleSpent =
                if (cycle != null) {
                    maxOf(
                        0L,
                        cycleSnapshot.spentMinor +
                            cycleSnapshot.feesMinor -
                            cycleSnapshot.refundsMinor
                    )
                } else {
                    activitySnapshot.summary.spentMinor +
                        activitySnapshot.summary.feesMinor
                }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneySummaryTile(
                    label = if (cycle != null) {
                        "Cycle income"
                    } else {
                        "Money in"
                    },
                    amount = formatMoney(cycleIncome),
                    incoming = true,
                    helper = helper,
                    modifier = Modifier.weight(1f)
                )
                MoneySummaryTile(
                    label = if (cycle != null) {
                        "Cycle spent"
                    } else {
                        "Money spent"
                    },
                    amount = formatMoney(cycleSpent),
                    incoming = false,
                    helper = helper,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                QuickAction(
                    label = "Review",
                    icon = Icons.AutoMirrored.Rounded.FactCheck,
                    tone = "amber",
                    onClick = onReviewClick
                )
                QuickAction(
                    label = "Import",
                    icon = Icons.Rounded.FileUpload,
                    tone = "blue",
                    onClick = onImportClick
                )
                QuickAction(
                    label = "Rules",
                    icon = Icons.Rounded.Tune,
                    tone = "violet",
                    onClick = onRulesClick
                )
                QuickAction(
                    label = "Goals",
                    icon = Icons.Rounded.TrackChanges,
                    tone = "green",
                    onClick = onGoalsClick
                )
            }
        }

        if (activeDebtAccounts.isNotEmpty()) {
            item {
                DebtOverviewCard(
                    accounts =
                        activeDebtAccounts,
                    onViewAll =
                        onDebtsClick
                )
            }
        }

        if (cycleSnapshot.unexplainedFundingGapMinor > 0L) {
            item {
                FundingGapCard(cycleSnapshot.unexplainedFundingGapMinor)
            }
        }

        item {
            SpendingDonutCard(
                slices = spendSlices,
                totalLabel = formatMoney(
                    activitySnapshot.summary.spentMinor +
                        activitySnapshot.summary.feesMinor
                )
            )
        }

        item {
            BudgetProgressCard(
                items = budgetItems,
                onManageClick = onBudgetClick
            )
        }

        item {
            SmartInsightCard(
                title = "Smart insight",
                body = homeInsight(
                    cycleSnapshot = cycleSnapshot,
                    needsReviewCount = activitySnapshot.summary.needsReviewCount
                )
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
            WalletsAndBalancesCard(
                mpesaLabel =
                    mpesaAccount
                        ?.let {
                            AccountIdentityPresentation
                                .title(it)
                        }
                        ?: "M-Pesa",
                mpesaBalanceMinor =
                    ledgerSnapshot.latestBalanceMinor,
                mpesaObservedAtEpochMs =
                    ledgerSnapshot
                        .latestBalanceObservedAtEpochMs,
                trackedAccountCount =
                    visibleAccountGroups.size,
                onSeeAll =
                    onAccountsClick,
                onTrackedAccountsClick =
                    onAccountsClick
            )
        }
    }
}

@Composable
private fun DebtOverviewCard(
    accounts: List<
        com.kedevjimmy.pesatrack.data.local
            .DebtAccountSnapshot
        >,
    onViewAll: () -> Unit
) {
    val sortedAccounts =
        accounts.sortedWith(
            compareByDescending<
                com.kedevjimmy.pesatrack.data.local
                    .DebtAccountSnapshot
                > {
                it.outstandingMinor
            }.thenBy {
                it.name.lowercase()
            }
        )

    PesaCard {
        Column(Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                IconBubble(
                    icon = Icons.Rounded.CreditCard,
                    size = 42.dp,
                    background =
                        MaterialTheme.colorScheme
                            .primaryContainer,
                    tint =
                        MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 11.dp)
                ) {
                    Text(
                        "Debt overview",
                        style =
                            MaterialTheme.typography
                                .titleLarge
                    )
                    Text(
                        "Borrowed money is tracked separately from spending.",
                        modifier =
                            Modifier.padding(top = 2.dp),
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
                Surface(
                    modifier =
                        Modifier.clickable(
                            onClick = onViewAll
                        ),
                    shape =
                        androidx.compose.foundation.shape
                            .RoundedCornerShape(999.dp),
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme
                            .outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Text(
                            "View all debt",
                            style =
                                MaterialTheme.typography
                                    .labelMedium,
                            color =
                                MaterialTheme.colorScheme
                                    .primary
                        )
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint =
                                MaterialTheme.colorScheme
                                    .primary,
                            modifier = Modifier
                                .padding(start = 2.dp)
                                .size(17.dp)
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(16.dp),
                color =
                    MaterialTheme.colorScheme
                        .surfaceVariant
                        .copy(alpha = .42f)
            ) {
                Row(
                    modifier =
                        Modifier.padding(
                            horizontal = 13.dp,
                            vertical = 11.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        "Total outstanding",
                        modifier =
                            Modifier.weight(1f),
                        style =
                            MaterialTheme.typography
                                .bodyMedium,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                    Text(
                        formatMoney(
                            sortedAccounts
                                .sumOf {
                                    it.outstandingMinor
                                }
                        ),
                        style =
                            MaterialTheme.typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                val visibleCount =
                    if (maxWidth >= 600.dp) {
                        3
                    } else {
                        2
                    }
                val displayAccounts =
                    sortedAccounts.take(visibleCount)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    displayAccounts.forEachIndexed {
                            index,
                            account ->
                        val icon =
                            when (index % 3) {
                                0 ->
                                    Icons.Rounded.CreditCard
                                1 ->
                                    Icons.Rounded.Savings
                                else ->
                                    Icons.Rounded.AccountBalance
                            }
                        val tone =
                            premiumCategoryColor(index)

                        Surface(
                            modifier =
                                Modifier.weight(1f),
                            shape =
                                androidx.compose.foundation
                                    .shape.RoundedCornerShape(
                                        18.dp
                                    ),
                            color =
                                MaterialTheme.colorScheme
                                    .surfaceVariant
                                    .copy(alpha = .55f)
                        ) {
                            Row(
                                modifier =
                                    Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 10.dp
                                    ),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                IconBubble(
                                    icon = icon,
                                    size = 36.dp,
                                    background =
                                        tone.copy(alpha = .16f),
                                    tint = tone
                                )
                                Column(
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .padding(
                                                start = 8.dp
                                            )
                                ) {
                                    Text(
                                        account.name,
                                        style =
                                            MaterialTheme.typography
                                                .labelMedium,
                                        maxLines = 1,
                                        overflow =
                                            androidx.compose.ui.text
                                                .style.TextOverflow
                                                .Ellipsis
                                    )
                                    Text(
                                        formatMoney(
                                            account
                                                .outstandingMinor
                                        ),
                                        modifier =
                                            Modifier.padding(
                                                top = 2.dp
                                            ),
                                        style =
                                            MaterialTheme.typography
                                                .titleSmall,
                                        fontWeight =
                                            FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint =
                                        MaterialTheme.colorScheme
                                            .onSurfaceVariant,
                                    modifier =
                                        Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletsAndBalancesCard(
    mpesaLabel: String,
    mpesaBalanceMinor: Long?,
    mpesaObservedAtEpochMs: Long?,
    trackedAccountCount: Int,
    onSeeAll: () -> Unit,
    onTrackedAccountsClick: () -> Unit
) {
    PesaCard {
        Column(Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    "Wallets & balances",
                    modifier = Modifier.weight(1f),
                    style =
                        MaterialTheme.typography
                            .titleLarge
                )
                Text(
                    "See all",
                    modifier =
                        Modifier.clickable(
                            onClick = onSeeAll
                        ),
                    style =
                        MaterialTheme.typography
                            .labelMedium,
                    color =
                        MaterialTheme.colorScheme.primary
                )
            }

            WalletBalanceRow(
                icon = Icons.Rounded.AccountBalanceWallet,
                label = mpesaLabel,
                supportingText =
                    if (
                        mpesaObservedAtEpochMs != null
                    ) {
                        "Last observed " +
                            formatDateTime(
                                mpesaObservedAtEpochMs
                            )
                    } else {
                        "Waiting for a balance observation"
                    },
                value =
                    mpesaBalanceMinor
                        ?.let(::formatMoney)
                        ?: "—",
                tone = 0,
                modifier = Modifier.padding(top = 11.dp)
            )

            WalletBalanceRow(
                icon = Icons.Rounded.AccountBalance,
                label = "Tracked accounts",
                supportingText =
                    "Open your confirmed accounts and balances",
                value =
                    trackedAccountCount.toString() +
                        if (trackedAccountCount == 1) {
                            " account"
                        } else {
                            " accounts"
                        },
                tone = 1,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable(
                        onClick =
                            onTrackedAccountsClick
                    )
            )
        }
    }
}

@Composable
private fun WalletBalanceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    supportingText: String?,
    value: String,
    tone: Int,
    modifier: Modifier = Modifier
) {
    val accent = premiumCategoryColor(tone)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(18.dp),
        color =
            MaterialTheme.colorScheme
                .surfaceVariant
                .copy(alpha = .55f)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 13.dp,
                vertical = 11.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            IconBubble(
                icon = icon,
                size = 38.dp,
                background =
                    accent.copy(alpha = .16f),
                tint = accent
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 11.dp)
            ) {
                Text(
                    label,
                    style =
                        MaterialTheme.typography
                            .titleMedium
                )
                if (!supportingText.isNullOrBlank()) {
                    Text(
                        supportingText,
                        modifier =
                            Modifier.padding(top = 2.dp),
                        style =
                            MaterialTheme.typography
                                .labelSmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        maxLines = 1,
                        overflow =
                            androidx.compose.ui.text
                                .style.TextOverflow
                                .Ellipsis
                    )
                }
            }

            Text(
                value,
                style =
                    MaterialTheme.typography
                        .titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 5.dp)
                    .size(18.dp)
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
private fun GreetingBlock(
    onProfileClick: () -> Unit
) {
    val hour = remember {
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }
    val greeting = when (hour) {
        in 5..11 -> "Good morning,"
        in 12..16 -> "Good afternoon,"
        else -> "Good evening,"
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "James",
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
private fun PaydayContextCard(
    snapshot: PayCycleSnapshot,
    onProtectMoney: () -> Unit
) {
    val cycle =
        snapshot.cycle
            ?: return
    val payday =
        snapshot.paydaySnapshot
    val startingResources =
        cycle.openingIncomeMinor +
            cycle.carryoverMinor

    PesaCard {
        Column(
            modifier =
                Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        "Payday picture",
                        style =
                            MaterialTheme.typography
                                .titleLarge,
                        fontWeight =
                            FontWeight.Bold
                    )
                    Text(
                        "Cycle started " +
                            formatShortDate(
                                cycle.startEpochMs
                            ),
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
                Text(
                    formatMoney(
                        startingResources
                    ),
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                PaydayValue(
                    label = "Carried in",
                    value =
                        formatMoney(
                            cycle.carryoverMinor
                        ),
                    modifier =
                        Modifier.weight(1f)
                )
                PaydayValue(
                    label = "Salary",
                    value =
                        formatMoney(
                            cycle.openingIncomeMinor
                        ),
                    modifier =
                        Modifier.weight(1f)
                )
            }

            payday?.let { context ->
                if (
                    context.protectedSavingsMinor >
                    0L ||
                    context.debtOutstandingMinor >
                    0L
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            )
                    ) {
                        PaydayValue(
                            label =
                                "Protected savings",
                            value =
                                formatMoney(
                                    context
                                        .protectedSavingsMinor
                                ),
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )
                        PaydayValue(
                            label =
                                "Debt at payday",
                            value =
                                formatMoney(
                                    context
                                        .debtOutstandingMinor
                                ),
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )
                    }
                }

                if (
                    context
                        .unknownLiquidAccountCount >
                    0
                ) {
                    Text(
                        context
                            .unknownLiquidAccountCount
                            .toString() +
                            " spendable account balance" +
                            if (
                                context
                                    .unknownLiquidAccountCount ==
                                1
                            ) {
                                " was"
                            } else {
                                "s were"
                            } +
                            " unknown at payday, so carried-in cash is a known minimum rather than a guess.",
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .secondary
                    )
                }
            }

            if (
                snapshot.userProtectedMinor >
                0L
            ) {
                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        androidx.compose.foundation
                            .shape
                            .RoundedCornerShape(
                                16.dp
                            ),
                    color =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                            .copy(alpha = .55f)
                ) {
                    Row(
                        modifier =
                            Modifier.padding(
                                12.dp
                            ),
                        verticalAlignment =
                            Alignment
                                .CenterVertically
                    ) {
                        Column(
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        ) {
                            Text(
                                "Protected by you",
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelMedium
                            )
                            Text(
                                formatMoney(
                                    snapshot
                                        .userProtectedMinor
                                ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                fontWeight =
                                    FontWeight
                                        .SemiBold
                            )
                        }
                        TextButton(
                            onClick =
                                onProtectMoney
                        ) {
                            Text("Edit")
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onProtectMoney,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {
                    Text(
                        "Protect some money from spending"
                    )
                }
            }

            Text(
                "PesaTrack treats existing cash and new salary as one spendable pool. It does not pretend to know which individual shilling funded a purchase.",
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
private fun PaydayValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(16.dp),
        color =
            MaterialTheme.colorScheme
                .surfaceVariant
                .copy(alpha = .48f)
    ) {
        Column(
            modifier =
                Modifier.padding(12.dp)
        ) {
            Text(
                label,
                style =
                    MaterialTheme.typography
                        .labelMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
            Text(
                value,
                modifier =
                    Modifier.padding(top = 3.dp),
                style =
                    MaterialTheme.typography
                        .titleMedium,
                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }
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
                    Text("Not paid yet")
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
    var amount by remember(prompt.windowEndEpochMs) {
        mutableStateOf("")
    }
    var date by remember(prompt.windowEndEpochMs) {
        mutableStateOf(
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
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
): String = when {
    needsReviewCount > 0 ->
        "$needsReviewCount transaction" +
            if (needsReviewCount == 1) {
                " is waiting for review before it enters trusted totals."
            } else {
                "s are waiting for review before they enter trusted totals."
            }
    cycleSnapshot.paydaySnapshot
        ?.unknownLiquidAccountCount
        ?.let { it > 0 } == true ->
        cycleSnapshot.paydaySnapshot
            .unknownLiquidAccountCount
            .toString() +
            " spendable account balance" +
            if (
                cycleSnapshot.paydaySnapshot
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

private fun formatDateTime(epochMs: Long): String =
    SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(epochMs))
