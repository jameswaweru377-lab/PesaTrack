package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.BudgetStore
import com.kedevjimmy.pesatrack.data.local.FinancialProgress
import com.kedevjimmy.pesatrack.data.local.FinancialProgressRepository
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.MonthlyBudget
import com.kedevjimmy.pesatrack.data.local.PayCycleSnapshot
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
import com.kedevjimmy.pesatrack.data.local.RecurringPatternStore
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.recurring.RecurringPatternCandidate
import com.kedevjimmy.pesatrack.domain.recurring.RecurringPatternConfidence
import com.kedevjimmy.pesatrack.domain.rules.PlanningAvailabilityPolicy
import com.kedevjimmy.pesatrack.domain.rules.PlanSpendableAccount
import com.kedevjimmy.pesatrack.domain.rules.PlanSpendableBalancePolicy
import com.kedevjimmy.pesatrack.planning.BillSchedules
import com.kedevjimmy.pesatrack.planning.ScheduledBill
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.FundingGapCard
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.SmallStatusPill
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class PlanBillProgress(
    val paidMinor: Long,
    val expectedMinor: Long
)

internal data class PlanReferenceData(
    val snapshot: PayCycleSnapshot,
    val recurring: List<RecurringPatternCandidate>,
    val budgets: List<MonthlyBudget>,
    val goals: List<SmartRule>,
    val bills: List<ScheduledBill>,
    val financialProgress: FinancialProgress? = null,
    val billProgress: PlanBillProgress? = null,
    val accountBalances: List<PlanSpendableAccount> = emptyList()
)

class PlanScreenCache {
    internal var data: PlanReferenceData? = null
    internal var revision: Long = -1L
}

@Composable
fun PlanScreen(
    scaffoldPadding: PaddingValues,
    onBudgetClick: (() -> Unit)? = null,
    onGoalsClick: (() -> Unit)? = null,
    onShoppingClick: (() -> Unit)? = null,
    onSalaryClick: (() -> Unit)? = null,
    onBillsClick: (() -> Unit)? = null,
    onPayCycleClick: (() -> Unit)? = null,
    onInsightsClick: (() -> Unit)? = null,
    stateCache: PlanScreenCache? = null
) {
    val context = LocalContext.current
    val store = remember {
        PayCycleStore.get(context)
    }
    val recurringStore = remember {
        RecurringPatternStore.get(context)
    }
    val budgetStore = remember { BudgetStore.get(context) }
    val ruleStore = remember { RuleStore.get(context) }
    val billSchedules = remember { BillSchedules(context) }
    val planScope =
        rememberCoroutineScope()
    val ledgerUpdateState by
        LedgerUpdateBus.state.collectAsState()
    val initialData = remember {
        stateCache?.takeIf { it.revision == ledgerUpdateState.revision }?.data
    }
    var snapshot by remember {
        mutableStateOf(
            initialData?.snapshot ?: emptyPlanSnapshot()
        )
    }
    var accountBalances by remember {
        mutableStateOf(initialData?.accountBalances.orEmpty())
    }
    var recurringSuggestions by remember {
        mutableStateOf<
            List<RecurringPatternCandidate>
        >(initialData?.recurring ?: emptyList())
    }
    var localRefresh by remember {
        mutableStateOf(0)
    }
    // Plan and Bills must use the same confirmed schedule creation flow.
    // Accepting only a recurring Smart Rule does not put a bill on the calendar.
    var billSetupCandidate by remember {
        mutableStateOf<RecurringPatternCandidate?>(null)
    }
    var monthlyBudgets by remember {
        mutableStateOf<List<MonthlyBudget>>(initialData?.budgets ?: emptyList())
    }
    var savingsGoals by remember {
        mutableStateOf<List<SmartRule>>(initialData?.goals ?: emptyList())
    }
    var upcomingBills by remember {
        mutableStateOf<List<ScheduledBill>>(initialData?.bills ?: emptyList())
    }
    var financialProgress by remember {
        mutableStateOf(initialData?.financialProgress)
    }
    var billProgress by remember {
        mutableStateOf(initialData?.billProgress)
    }
    var showPlanDetails by remember {
        mutableStateOf(false)
    }
    var showFundingEvidence by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(initialData == null) }
    var hasLoaded by remember { mutableStateOf(initialData != null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(
        ledgerUpdateState.revision,
        localRefresh
    ) {
        // Reopening Plan on the same financial revision needs no SQL work.
        // An explicit action or new revision still forces a fresh snapshot.
        if (hasLoaded && !ScreenRefreshPolicy.needsRead(
                stateCache?.revision, ledgerUpdateState.revision, localRefresh
            )) {
            loading = false
            return@LaunchedEffect
        }
        if (hasLoaded) delay(180) // batch closely spaced import notifications
        loading = true
        loadError = null
        val refreshStarted = android.os.SystemClock.elapsedRealtime()
        try {
        val loaded =
            withContext(Dispatchers.IO) {
                // Pay cycles are rebuilt when ledger data changes. Reading Plan
                // should not rebuild the whole cycle graph every time the tab opens.
                val cycle = traceFinancialRead("plan.payCycle") { store.snapshot() }
                val recurring = traceFinancialRead("plan.recurringPatterns") {
                    recurringStore.suggestions(limit = 3)
                }
                val budgets = traceFinancialRead("plan.budgetList") { budgetStore.list() }
                val goals =
                    ruleStore.listRules(enabledOnly = true)
                        .filter {
                            it.category == TransactionCategory.SAVINGS &&
                                (it.plannedAmountMinor ?: 0L) > 0L
                        }
                val progress = try {
                    traceFinancialRead("plan.progress") {
                        FinancialProgressRepository(context).read()
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                val scheduledBills = traceFinancialRead("plan.scheduledBills") {
                    billSchedules.list()
                }
                val accountBalances = try {
                    traceFinancialRead("plan.accountBalances") {
                        readPlanSpendableAccounts(context)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    android.util.Log.w("PesaTrackPlan", "Account balance read unavailable")
                    emptyList()
                }
                // One canonical schedule read provides both upcoming items and real
                // payment progress. An unknown due date is never assumed paid.
                val monthBills = scheduledBills.filter {
                    it.dueThisMonth && it.expectedMinor > 0L
                }
                PlanReferenceData(
                    snapshot = cycle,
                    recurring = recurring,
                    budgets = budgets,
                    goals = goals,
                    bills = scheduledBills.filter { it.dueThisMonth && it.remainingMinor > 0L },
                    financialProgress = progress,
                    accountBalances = accountBalances,
                    billProgress = monthBills.takeIf { it.isNotEmpty() }?.let { items ->
                        PlanBillProgress(
                            paidMinor = items.sumOf {
                                it.paidMinor.coerceIn(0L, it.expectedMinor)
                            },
                            expectedMinor = items.sumOf { it.expectedMinor }
                        )
                    }
                )
            }
        snapshot = loaded.snapshot
        accountBalances = loaded.accountBalances
        recurringSuggestions = loaded.recurring
        monthlyBudgets = loaded.budgets
        savingsGoals = loaded.goals
        upcomingBills = loaded.bills
        financialProgress = loaded.financialProgress
        billProgress = loaded.billProgress
        hasLoaded = true
        stateCache?.data = loaded
        stateCache?.revision = ledgerUpdateState.revision
        val elapsed = android.os.SystemClock.elapsedRealtime() - refreshStarted
        if (elapsed > 400L) android.util.Log.i("PesaTrackPerf", "Plan refresh took ${elapsed}ms")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            loadError = "Could not load your plan. Try again."
        } finally { loading = false }
    }
    if (!hasLoaded && (loading || loadError != null)) {
        Column(Modifier.padding(scaffoldPadding).padding(18.dp)) {
            Text(if (loading) "Loading your plan…" else loadError.orEmpty())
            if (!loading) TextButton(onClick = { localRefresh += 1 }) { Text("Retry") }
        }
        return
    }

    val planningAvailability =
        PlanningAvailabilityPolicy
            .from(
                snapshot
                    .availableToLiveMinor
            )
    BackHandler(enabled = showPlanDetails) { showPlanDetails = false }
    val journeyBillRows = upcomingBills.sortedWith(compareBy<ScheduledBill> {
        it.nextDueEpochMs ?: Long.MAX_VALUE
    }.thenBy { it.rule.displayLabel ?: it.rule.name }).map { bill ->
        PlanJourneyBill(
            label = planBillFriendlyName(bill.rule.displayLabel ?: bill.rule.name),
            dueLabel = planBillDueLabel(bill.nextDueEpochMs),
            amount = formatPlanMoney(bill.remainingMinor),
            dueEpochMs = bill.nextDueEpochMs
        )
    }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val now = System.currentTimeMillis()
    val week = planCalendarJourneyWeek(
        todayEpochMs = now,
        dueEpochMs = journeyBillRows.mapNotNull(PlanJourneyBill::dueEpochMs),
        locale = locale,
        timeZone = TimeZone.getDefault()
    )
    val budgetRows = financialProgress?.budgets.orEmpty()
    val goalRows = financialProgress?.goals.orEmpty()
    val monthBudgetSpent = budgetRows.sumOf { it.saved }
    val monthBudgetLimit = budgetRows.sumOf { it.target }
    val monthGoalSaved = goalRows.sumOf { it.saved }
    val monthGoalTarget = goalRows.sumOf { it.target }
    val budgetSpent = monthBudgetSpent
    val budgetLimit = monthBudgetLimit
    val goalSaved = monthGoalSaved
    val goalLimit = monthGoalTarget
    val budgetAvailable = financialProgress != null && budgetRows.isNotEmpty()
    val goalAvailable = financialProgress != null && goalRows.isNotEmpty()
    val spendable = PlanSpendableBalancePolicy.calculate(
        accountBalances,
        planUpcomingBillsTotalMinor(upcomingBills.map { it.remainingMinor })
    )
    val journey = PlanJourneyView(
        month = SimpleDateFormat("MMMM yyyy", locale).format(Date(now)),
        period = formatPlanPeriod(snapshot),
        hasCycle = snapshot.cycle != null,
        available = formatPlanMoney(planningAvailability.availableMinor),
        spendable = spendable.spendableMinor?.let(::formatPlanMoney),
        liquid = spendable.liquidMinor?.let(::formatPlanMoney),
        protectedSavings = spendable.protectedMinor?.let(::formatPlanMoney),
        spendableNeedsReview = spendable.needsReview,
        starting = formatPlanMoney(snapshot.startingFundsMinor),
        committed = formatPlanMoney(snapshot.remainingCommitmentsMinor),
        billsTotal = planUpcomingBillsTotalMinor(upcomingBills.map { it.remainingMinor })
            ?.let(::formatPlanMoney),
        saved = formatPlanMoney(snapshot.savingsMinor),
        received = formatPlanMoney(snapshot.additionalIncomeMinor),
        spent = formatPlanMoney(snapshot.spentMinor),
        fees = formatPlanMoney(snapshot.feesMinor),
        safeToday = snapshot.safeTodayMinor?.let(::formatPlanMoney),
        billsPaidPercent = billProgress?.let {
            planProgressPercent(it.paidMinor, it.expectedMinor)
        },
        budgetUsed = if (budgetAvailable) formatPlanMoney(budgetSpent) else null,
        budgetLimit = if (budgetAvailable && budgetLimit != null)
            formatPlanMoney(budgetLimit) else null,
        budgetPercent = if (budgetAvailable && budgetLimit != null)
            planProgressPercent(budgetSpent, budgetLimit) else null,
        goalSaved = if (goalAvailable) formatPlanMoney(goalSaved) else null,
        goalTarget = if (goalAvailable && goalLimit != null)
            formatPlanMoney(goalLimit) else null,
        goalPercent = if (goalAvailable && goalLimit != null)
            planProgressPercent(goalSaved, goalLimit) else null,
        displayedShares = if (snapshot.cycle != null) planJourneyDisplayedShares(
            snapshot.remainingCommitmentsMinor, snapshot.savingsMinor,
            planningAvailability.availableMinor
        ) else null,
        bills = journeyBillRows,
        week = week
    )

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(BrownHomePalette.canvas),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (loadError != null) item {
            Column {
                Text("Showing your last loaded plan. ${loadError.orEmpty()}")
                TextButton(onClick = { localRefresh += 1 }) { Text("Retry") }
            }
        }
        if (showPlanDetails) {
            item(key = "calendar-journey-details") {
                PlanJourneyDetails(
                    data = journey,
                    onBack = { showPlanDetails = false },
                    onOtherMonths = { onInsightsClick?.invoke() },
                    onBills = { onBillsClick?.invoke() },
                    onPayCycle = { onPayCycleClick?.invoke() }
                )
            }
        } else {
            item(key = "calendar-journey-overview") {
                PlanJourneyOverview(
                    data = journey,
                    selectedPeriod = "Month",
                    onPeriod = {},
                    onOtherMonths = { onInsightsClick?.invoke() },
                    onBudget = { onBudgetClick?.invoke() },
                    onGoals = { onGoalsClick?.invoke() },
                    onBills = { onBillsClick?.invoke() },
                    onShopping = onShoppingClick,
                    onDetails = { showPlanDetails = true },
                    onSalary = { onSalaryClick?.invoke() }
                )
            }
        }
        if (!showPlanDetails) {
        // Preserve the existing forecasting, recurring-bill confirmation and
        // funding-gap review flows below the new Plan overview.
        item {
            // This card also provides the forecast/payday confirmation flow
            // for people who have not yet recorded a confirmed salary cycle.
            MoneyUntilSalaryCard(snapshot)
        }
        item(key = "coming-up-after-status") {
            ApprovedPlanComingUp(journey.bills.take(2)) { onBillsClick?.invoke() }
        }
        if (
            recurringSuggestions
                .isNotEmpty()
        ) {
            item {
                SectionHeader(
                    "Possible recurring payments"
                )
                Text(
                    "PesaTrack noticed a monthly pattern. Nothing is added to your plan until you confirm it.",
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
            }

            items(
                recurringSuggestions.size
            ) { index ->
                val candidate =
                    recurringSuggestions[
                        index
                    ]
                RecurringSuggestionCard(
                    candidate =
                        candidate,
                    onAdd = {
                        // Defer all writes until the user confirms the amount,
                        // cadence and due date in the existing bill setup sheet.
                        billSetupCandidate = candidate
                    },
                    onDismiss = {
                        planScope.launch {
                            withContext(
                                Dispatchers.IO
                            ) {
                                recurringStore
                                    .dismiss(
                                        candidate
                                    )
                            }
                            localRefresh += 1
                        }
                    }
                )
            }
        }

        val unexplained = snapshot.unexplainedFundingGapMinor
        val shortfall = if (snapshot.cycle != null)
            planningAvailability.shortfallMinor else 0L
        if (unexplained > 0L || shortfall > 0L) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VibrantPlanSourceWarning(
                        title = if (unexplained > 0L)
                            "Some spending still needs a source"
                        else "Planned commitments need attention",
                        message = when {
                            unexplained > 0L && shortfall > 0L ->
                                "Review unexplained spending and the calculated planning gap."
                            unexplained > 0L ->
                                "Review unexplained spending to improve your plan."
                            else -> "Your planned commitments exceed recorded available funds."
                        },
                        onClick = { showFundingEvidence = !showFundingEvidence },
                        expanded = showFundingEvidence
                    )
                    if (showFundingEvidence) {
                        if (shortfall > 0L) {
                            VibrantPlanSurface {
                                Text(
                                    "Commitment shortfall: " + formatPlanMoney(shortfall),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "This is a planning gap, not an account balance.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (unexplained > 0L) FundingGapCard(unexplained)
                    }
                }
            }
        }

        } else if (snapshot.cycle != null) {
            item {
                // Keep the complete ledger-derived breakdown under Plan Details.
                VibrantPlanMoneyBreakdown(
                    expanded = true,
                    onToggle = {},
                    startingFunds = formatSignedPlanMoney(snapshot.startingFundsMinor),
                    received = formatSignedPlanMoney(snapshot.additionalIncomeMinor),
                    cashPaid = formatSignedPlanMoney(-snapshot.spentMinor),
                    fees = formatSignedPlanMoney(-snapshot.feesMinor),
                    saved = formatSignedPlanMoney(-snapshot.savingsMinor),
                    committed = formatSignedPlanMoney(-snapshot.remainingCommitmentsMinor),
                    available = formatPlanMoney(planningAvailability.availableMinor)
                )
            }
        }
    }

    billSetupCandidate?.let { candidate ->
        SuggestedBillSheet(candidate, onDismiss = {
            billSetupCandidate = null
            // Re-read the explicit bill schedule, even when save updates only
            // local prefs. Closing an unmodified sheet is harmless.
            localRefresh += 1
        })
    }
}

@Composable
private fun RecurringSuggestionCard(
    candidate:
        RecurringPatternCandidate,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    PesaCard {
        Column(
            modifier =
                Modifier.padding(15.dp),
            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                IconBubble(
                    Icons.Rounded.Repeat,
                    size = 42.dp,
                    background =
                        pesaToneContainer(
                            "blue"
                        ),
                    tint =
                        pesaToneContent(
                            "blue"
                        )
                )
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(
                                start = 11.dp
                            )
                ) {
                    Text(
                        candidate.label,
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )
                    Text(
                        candidate
                            .occurrenceCount
                            .toString() +
                            " payments • about every " +
                            candidate
                                .medianIntervalDays +
                            " days",
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
                    if (
                        candidate.confidence ==
                        RecurringPatternConfidence
                            .HIGH
                    ) {
                        "Strong pattern"
                    } else {
                        "Possible"
                    },
                    if (
                        candidate.confidence ==
                        RecurringPatternConfidence
                            .HIGH
                    ) {
                        "green"
                    } else {
                        "amber"
                    }
                )
            }

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
                        "Typical amount",
                        style =
                            MaterialTheme
                                .typography
                                .labelSmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                    Text(
                        formatPlanMoney(
                            candidate
                                .typicalAmountMinor
                        ),
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
                Column(
                    horizontalAlignment =
                        Alignment.End
                ) {
                    Text(
                        "Next around",
                        style =
                            MaterialTheme
                                .typography
                                .labelSmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                    Text(
                        formatPlanDate(
                            candidate
                                .nextExpectedAtEpochMs
                        ),
                        style =
                            MaterialTheme
                                .typography
                                .labelLarge
                    )
                }
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.End,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Not recurring")
                }
                Button(
                    onClick = onAdd
                ) {
                    Text("Set up bill")
                }
            }
        }
    }
}

private fun planProgressPercent(saved: Long, target: Long): Int? =
    if (target <= 0L) null else
        (saved.coerceAtLeast(0L).toDouble() * 100.0 / target.toDouble())
            .toInt().coerceAtLeast(0)

private fun formatPlanPeriod(snapshot: PayCycleSnapshot): String {
    val cycle = snapshot.cycle ?: return "Current pay cycle"
    val endMs = cycle.endEpochMs
    val estimated = if (endMs == null && snapshot.estimatedDaysRemaining != null) {
        java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR,
                snapshot.estimatedDaysRemaining!!.coerceIn(0, 120))
        }.timeInMillis
    } else null
    val end = endMs ?: estimated
    return if (end != null) {
        val start = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(cycle.startEpochMs))
        val finish = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(end))
        "$start – $finish" + if (estimated != null) " est." else ""
    } else {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(cycle.startEpochMs)) +
            " – next payday"
    }
}


/** Compact payee label; the Bills destination retains the full source identity. */
private fun planBillFriendlyName(label: String): String =
    label.replace(Regex("\\s+for\\s+account\\b.*$", RegexOption.IGNORE_CASE), "")
        .trim().ifBlank { label }

private fun planBillDueLabel(
    dueEpochMs: Long?
): String {
    if (dueEpochMs == null) {
        return "Due this month"
    }

    val today =
        java.util.Calendar
            .getInstance()
            .apply {
                set(
                    java.util.Calendar.HOUR_OF_DAY,
                    0
                )
                set(
                    java.util.Calendar.MINUTE,
                    0
                )
                set(
                    java.util.Calendar.SECOND,
                    0
                )
                set(
                    java.util.Calendar.MILLISECOND,
                    0
                )
            }
    val due =
        java.util.Calendar
            .getInstance()
            .apply {
                timeInMillis =
                    dueEpochMs
                set(
                    java.util.Calendar.HOUR_OF_DAY,
                    0
                )
                set(
                    java.util.Calendar.MINUTE,
                    0
                )
                set(
                    java.util.Calendar.SECOND,
                    0
                )
                set(
                    java.util.Calendar.MILLISECOND,
                    0
                )
            }

    val label = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(dueEpochMs))
    val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(
        due.timeInMillis - today.timeInMillis
    )
    return when {
        days < 0L -> "Overdue • $label"
        days == 0L -> "Due today • $label"
        days == 1L -> "Due in 1 day • $label"
        else -> "Due in $days days • $label"
    }
}

private fun formatPlanMoney(minor: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    return if (minor < 0) {
        "-KES " + formatter.format(-minor / 100.0)
    } else {
        "KES " + formatter.format(minor / 100.0)
    }
}

private fun formatSignedPlanMoney(minor: Long): String =
    if (minor < 0) {
        "- " + formatPlanMoney(-minor)
    } else {
        "+ " + formatPlanMoney(minor)
    }

private fun formatPlanDate(epochMs: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMs))

private fun emptyPlanSnapshot(): PayCycleSnapshot =
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
