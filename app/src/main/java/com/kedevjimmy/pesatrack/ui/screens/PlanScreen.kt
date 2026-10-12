package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.BudgetReadRepository
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.BudgetStore
import com.kedevjimmy.pesatrack.data.local.CommitmentProgress
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
import com.kedevjimmy.pesatrack.planning.BillSchedules
import com.kedevjimmy.pesatrack.planning.ScheduledBill
import com.kedevjimmy.pesatrack.ui.components.ConceptEmeraldHero
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.FundingGapCard
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
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class PlanReferenceData(
    val snapshot: PayCycleSnapshot,
    val recurring: List<RecurringPatternCandidate>,
    val budgets: List<MonthlyBudget>,
    val spentByCategory: Map<String, Long>,
    val goals: List<SmartRule>,
    val bills: List<ScheduledBill>
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
    onSalaryClick: (() -> Unit)? = null,
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
    val categoryStore = remember { CategoryStore.get(context) }
    val budgetReads = remember { BudgetReadRepository(context) }
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
    var spentByBudgetCategory by remember {
        mutableStateOf<Map<String, Long>>(initialData?.spentByCategory ?: emptyMap())
    }
    var savingsGoals by remember {
        mutableStateOf<List<SmartRule>>(initialData?.goals ?: emptyList())
    }
    var upcomingBills by remember {
        mutableStateOf<List<ScheduledBill>>(initialData?.bills ?: emptyList())
    }
    var showPlanDetails by remember {
        mutableStateOf(false)
    }

    var loading by remember { mutableStateOf(true) }
    var hasLoaded by remember { mutableStateOf(initialData != null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(
        ledgerUpdateState.revision,
        localRefresh
    ) {
        loading = true
        loadError = null
        try {
        val loaded =
            withContext(Dispatchers.IO) {
                // Pay cycles are rebuilt when ledger data changes. Reading Plan
                // should not rebuild the whole cycle graph every time the tab opens.
                val cycle = store.snapshot()
                val recurring =
                    recurringStore.suggestions(limit = 3)
                val budgets = budgetStore.list()
                val spent = budgetReads.spendingByCategory()
                val goals =
                    ruleStore.listRules(enabledOnly = true)
                        .filter {
                            it.category == TransactionCategory.SAVINGS &&
                                (it.plannedAmountMinor ?: 0L) > 0L
                        }
                PlanReferenceData(
                    snapshot = cycle,
                    recurring = recurring,
                    budgets = budgets,
                    spentByCategory = spent,
                    goals = goals,
                    bills = billSchedules.upcomingThisMonth()
                )
            }
        snapshot = loaded.snapshot
        recurringSuggestions = loaded.recurring
        monthlyBudgets = loaded.budgets
        spentByBudgetCategory = loaded.spentByCategory
        savingsGoals = loaded.goals
        upcomingBills = loaded.bills
        hasLoaded = true
        stateCache?.data = loaded
        stateCache?.revision = ledgerUpdateState.revision
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

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (loading || loadError != null) item {
            Column {
                Text(if (loading) "Refreshing your plan…" else "Showing your last loaded plan. ${loadError.orEmpty()}")
                if (!loading) TextButton(onClick = { localRefresh += 1 }) { Text("Retry") }
            }
        }
        item {
            Column {
                Text("Plan your month", style = MaterialTheme.typography.headlineLarge)
                Text(
                    if (snapshot.cycle == null) {
                        "Bills, budgets and goals in one place."
                    } else {
                        "Budget, commitments and goals for your current pay cycle."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            PlanHero(
                snapshot = snapshot,
                planningAvailability =
                    planningAvailability
            )
        }

        if (
            snapshot.cycle == null &&
            onSalaryClick != null
        ) {
            item {
                Button(
                    onClick = onSalaryClick,
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            18.dp
                        )
                ) {
                    Icon(
                        Icons.Rounded.Paid,
                        contentDescription =
                            null
                    )
                    Spacer(
                        Modifier.width(8.dp)
                    )
                    Text(
                        "Add salary to start your plan"
                    )
                }
            }
        }

        if (monthlyBudgets.isNotEmpty()) {
            item {
                val totalBudget = monthlyBudgets.sumOf { it.limitMinor }
                val totalSpent = monthlyBudgets.sumOf {
                    spentByBudgetCategory[it.categoryId] ?: 0L
                }
                val left = (totalBudget - totalSpent).coerceAtLeast(0L)
                PesaCard {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconBubble(
                                Icons.Rounded.TrackChanges,
                                size = 44.dp,
                                background = pesaToneContainer("green"),
                                tint = pesaToneContent("green")
                            )
                            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                                Text(
                                    "Current budgets",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    formatPlanMoney(totalBudget),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                formatPlanMoney(left) + " left",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        LinearProgressIndicator(
                            progress = {
                                if (totalBudget <= 0L) 0f
                                else (totalSpent.toFloat() / totalBudget.toFloat())
                                    .coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .height(9.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            formatPlanMoney(totalSpent) + " used",
                            modifier = Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                SectionHeader("Category budgets")
            }

            items(monthlyBudgets.take(5).size) { index ->
                val budget = monthlyBudgets[index]
                val spent = spentByBudgetCategory[budget.categoryId] ?: 0L
                val progress =
                    if (budget.limitMinor <= 0L) 0f
                    else (spent.toFloat() / budget.limitMinor.toFloat())
                        .coerceIn(0f, 1f)
                PesaCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onBudgetClick?.invoke()
                            }
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                categoryStore.labelFor(budget.categoryId),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                formatPlanMoney(spent) + " / " +
                                    formatPlanMoney(budget.limitMinor),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 9.dp)
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        item {
            SectionHeader("Upcoming bills")
        }
        if (upcomingBills.isEmpty()) {
            item {
                PesaCard {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(15.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        IconBubble(
                            Icons.Rounded.CalendarMonth,
                            size = 42.dp,
                            background =
                                pesaToneContainer(
                                    "green"
                                ),
                            tint =
                                pesaToneContent(
                                    "green"
                                )
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(
                                    start = 11.dp
                                )
                        ) {
                            Text(
                                "Nothing else due this month",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium
                            )
                            Text(
                                "Paid bills disappear from this list until their next scheduled month.",
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
        } else {
            items(
                upcomingBills
                    .take(3)
                    .size
            ) { index ->
                val bill =
                    upcomingBills[index]
                PesaCard {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        IconBubble(
                            Icons.Rounded.CalendarMonth,
                            size = 42.dp,
                            background =
                                pesaToneContainer(
                                    "amber"
                                ),
                            tint =
                                pesaToneContent(
                                    "amber"
                                )
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(
                                    start = 11.dp,
                                    end = 10.dp
                                )
                        ) {
                            Text(
                                bill.rule
                                    .displayLabel
                                    ?: bill.rule.name,
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                maxLines = 2
                            )
                            Text(
                                planBillDueLabel(
                                    bill.nextDueEpochMs
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
                        BillAmountPill(
                            formatPlanMoney(
                                bill.remainingMinor
                            )
                        )
                    }
                }
            }
        }

        if (savingsGoals.isNotEmpty()) {
            item {
                SectionHeader("Goals")
            }
            items(savingsGoals.take(3).size) { index ->
                val goal = savingsGoals[index]
                PesaCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onGoalsClick?.invoke()
                            }
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBubble(
                            Icons.Rounded.Savings,
                            size = 42.dp,
                            background = pesaToneContainer("green"),
                            tint = pesaToneContent("green")
                        )
                        Column(Modifier.weight(1f).padding(start = 11.dp)) {
                            Text(
                                goal.displayLabel ?: goal.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Target " +
                                    formatPlanMoney(goal.plannedAmountMinor ?: 0L),
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
        }

        if (onBudgetClick != null && monthlyBudgets.isEmpty()) {
            item {
                PesaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = onBudgetClick
                        )
                ) {
                    Row(
                        modifier =
                            Modifier.padding(
                                15.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        IconBubble(
                            Icons.Rounded.BarChart,
                            size = 44.dp,
                            background =
                                pesaToneContainer(
                                    "green"
                                ),
                            tint =
                                pesaToneContent(
                                    "green"
                                )
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
                                "Budget",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium
                            )
                            Text(
                                "Set category limits and see how spending fits this plan.",
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

        if (onGoalsClick != null && savingsGoals.isEmpty()) {
            item {
                PesaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = onGoalsClick
                        )
                ) {
                    Row(
                        modifier =
                            Modifier.padding(
                                15.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        IconBubble(
                            Icons.Rounded
                                .TrackChanges,
                            size = 44.dp,
                            background =
                                pesaToneContainer(
                                    "violet"
                                ),
                            tint =
                                pesaToneContent(
                                    "violet"
                                )
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
                                "Goals",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium
                            )
                            Text(
                                "Savings goals live with your budget and pay-cycle plan.",
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

        if (snapshot.cycle != null) {
            if (
                planningAvailability
                    .shortfallMinor > 0L
            ) {
                item {
                    PesaCard {
                        Text(
                            "Commitment shortfall: " +
                                formatPlanMoney(
                                    planningAvailability
                                        .shortfallMinor
                                ) +
                                ". This is a planning gap, not an account balance.",
                            modifier =
                                Modifier.padding(
                                    16.dp
                                ),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }

            if (snapshot.unexplainedFundingGapMinor > 0L) {
                item {
                    FundingGapCard(snapshot.unexplainedFundingGapMinor)
                }
            }

            item {
                PesaCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPlanDetails =
                                    !showPlanDetails
                            }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconBubble(
                                Icons.Rounded.BarChart,
                                size = 42.dp,
                                background = pesaToneContainer("blue"),
                                tint = pesaToneContent("blue")
                            )
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(start = 11.dp)
                            ) {
                                Text(
                                    "Plan details",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "Cycle totals and accounting",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                if (showPlanDetails)
                                    Icons.Rounded.ExpandLess
                                else
                                    Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (showPlanDetails) {
                            Spacer(Modifier.height(14.dp))
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                if (maxWidth < 600.dp) {
                                    Column(
                                        verticalArrangement =
                                            Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement =
                                                Arrangement.spacedBy(10.dp)
                                        ) {
                                            PlanMetric(
                                                "Starting funds",
                                                formatPlanMoney(snapshot.startingFundsMinor),
                                                "Salary + carryover",
                                                "green",
                                                Modifier.weight(1f)
                                            )
                                            PlanMetric(
                                                "Committed",
                                                formatPlanMoney(snapshot.remainingCommitmentsMinor),
                                                "Still protected",
                                                "amber",
                                                Modifier.weight(1f)
                                            )
                                        }
                                        Row(
                                            horizontalArrangement =
                                                Arrangement.spacedBy(10.dp)
                                        ) {
                                            PlanMetric(
                                                "Saved",
                                                formatPlanMoney(snapshot.savingsMinor),
                                                "Moved to savings",
                                                "blue",
                                                Modifier.weight(1f)
                                            )
                                            PlanMetric(
                                                "Available",
                                                formatPlanMoney(planningAvailability.availableMinor),
                                                "Free to live",
                                                "violet",
                                                Modifier.weight(1f)
                                            )
                                        }
                                    }
                                } else {
                                    Row(
                                        horizontalArrangement =
                                            Arrangement.spacedBy(10.dp)
                                    ) {
                                        PlanMetric(
                                            "Starting funds",
                                            formatPlanMoney(snapshot.startingFundsMinor),
                                            "Salary + carryover",
                                            "green",
                                            Modifier.weight(1f)
                                        )
                                        PlanMetric(
                                            "Committed",
                                            formatPlanMoney(snapshot.remainingCommitmentsMinor),
                                            "Still protected",
                                            "amber",
                                            Modifier.weight(1f)
                                        )
                                        PlanMetric(
                                            "Saved",
                                            formatPlanMoney(snapshot.savingsMinor),
                                            "Moved to savings",
                                            "blue",
                                            Modifier.weight(1f)
                                        )
                                        PlanMetric(
                                            "Available",
                                            formatPlanMoney(planningAvailability.availableMinor),
                                            "Free to live",
                                            "violet",
                                            Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                            AccountingRow(
                                "Salary + carryover",
                                snapshot.startingFundsMinor
                            )
                            AccountingRow(
                                "Other money received",
                                snapshot.additionalIncomeMinor
                            )
                            AccountingRow(
                                "Cash paid",
                                -snapshot.spentMinor
                            )
                            AccountingRow(
                                "Fees",
                                -snapshot.feesMinor
                            )
                            AccountingRow(
                                "Savings allocations",
                                -snapshot.savingsMinor
                            )
                            AccountingRow(
                                "Still committed",
                                -snapshot.remainingCommitmentsMinor
                            )
                        }
                    }
                }
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
private fun PlanHero(
    snapshot: PayCycleSnapshot,
    planningAvailability:
        com.kedevjimmy.pesatrack.domain.rules.PlanningAvailability
) {
    if (snapshot.cycle == null) {
        ConceptEmeraldHero(
            title = "Monthly plan",
            amount = "Waiting for salary",
            subtitle = "Your plan starts from a real salary receipt, not an expected payday.",
            artwork = false
        )
    } else {
        val usedMinor =
            snapshot.spentMinor +
                snapshot.feesMinor +
                snapshot.savingsMinor
        val starting = maxOf(
            1L,
            snapshot.startingFundsMinor +
                snapshot.additionalIncomeMinor
        )
        val progress =
            (usedMinor.toFloat() / starting.toFloat())
                .coerceIn(0f, 1f)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ConceptEmeraldHero(
                title = "Monthly plan",
                amount = formatPlanMoney(planningAvailability.availableMinor),
                subtitle = "Available after commitments",
                eyebrow = formatPlanDate(snapshot.cycle.startEpochMs) + " → next salary",
                footer =
                    (progress * 100).toInt().toString() +
                        "% of starting funds allocated, spent or saved",
                artwork = false
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Safe today",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            snapshot.safeTodayMinor?.let(::formatPlanMoney)
                                ?: "Learning",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanMetric(
    label: String,
    value: String,
    helper: String,
    tone: String,
    modifier: Modifier
) {
    PesaCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            IconBubble(
                when (label) {
                    "Saved" -> Icons.Rounded.Savings
                    "Committed" -> Icons.Rounded.Shield
                    "Starting funds" -> Icons.Rounded.AccountBalanceWallet
                    else -> Icons.Rounded.Paid
                },
                size = 36.dp,
                background = pesaToneContainer(tone),
                tint = pesaToneContent(tone)
            )
            Text(
                label,
                modifier = Modifier.padding(top = 9.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1
            )
            Text(
                helper,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

@Composable
private fun PlanCommitment(item: CommitmentProgress) {
    val progress = if (item.plannedMinor <= 0L) {
        0f
    } else {
        (item.paidMinor.toFloat() / item.plannedMinor.toFloat()).coerceIn(0f, 1f)
    }

    PesaCard {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(
                    if (item.remainingMinor == 0L) Icons.Rounded.CheckCircle else Icons.Rounded.Shield,
                    size = 42.dp,
                    background = pesaToneContainer(
                        if (item.remainingMinor == 0L) "green" else "amber"
                    ),
                    tint = pesaToneContent(
                        if (item.remainingMinor == 0L) "green" else "amber"
                    )
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 11.dp)
                ) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (item.remainingMinor == 0L) {
                            "Paid"
                        } else {
                            formatPlanMoney(item.remainingMinor) + " still reserved"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    formatPlanMoney(item.plannedMinor),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 11.dp)
                    .height(8.dp),
                strokeCap = StrokeCap.Round,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun AccountingRow(
    label: String,
    value: Long
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            formatSignedPlanMoney(value),
            style = MaterialTheme.typography.titleSmall
        )
    }
}

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

    return if (
        due.timeInMillis <
        today.timeInMillis
    ) {
        "Overdue • " +
            SimpleDateFormat(
                "d MMM",
                Locale.getDefault()
            ).format(
                Date(dueEpochMs)
            )
    } else {
        "Due " +
            SimpleDateFormat(
                "d MMM",
                Locale.getDefault()
            ).format(
                Date(dueEpochMs)
            )
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
