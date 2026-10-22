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
import androidx.compose.material.icons.rounded.WarningAmber
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class PlanReferenceData(
    val snapshot: PayCycleSnapshot,
    val recurring: List<RecurringPatternCandidate>,
    val budgets: List<MonthlyBudget>,
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
    onBillsClick: (() -> Unit)? = null,
    onPayCycleClick: (() -> Unit)? = null,
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
    var showPlanDetails by remember {
        mutableStateOf(false)
    }

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
                PlanReferenceData(
                    snapshot = cycle,
                    recurring = recurring,
                    budgets = budgets,
                    goals = goals,
                    bills = traceFinancialRead("plan.upcomingBills") {
                        billSchedules.upcomingThisMonth()
                    }
                )
            }
        snapshot = loaded.snapshot
        recurringSuggestions = loaded.recurring
        monthlyBudgets = loaded.budgets
        savingsGoals = loaded.goals
        upcomingBills = loaded.bills
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

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (loadError != null) item {
            Column {
                Text("Showing your last loaded plan. ${loadError.orEmpty()}")
                TextButton(onClick = { localRefresh += 1 }) { Text("Retry") }
            }
        }
        item {
            VibrantPlanPageTitle("Plan", "Make every shilling count.")
        }

        item {
            if (snapshot.cycle == null) {
                VibrantPlanSurface {
                    Text("Start your financial plan", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text("Confirm a salary you've received to calculate a reliable plan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (onSalaryClick != null) VibrantActionButton("Add confirmed salary", onSalaryClick)
                }
            } else {
                val period = snapshot.cycle?.let { formatPlanDate(it.startEpochMs) + " → next salary" } ?: "Current cycle"
                VibrantPlanHero(
                    period = period,
                    available = formatPlanMoney(planningAvailability.availableMinor),
                    safeToday = snapshot.safeTodayMinor?.let(::formatPlanMoney) ?: "Learning",
                    onCycleClick = onPayCycleClick
                )
            }
        }
        item {
            VibrantPlanSurface {
                VibrantPlanSectionTitle("Your plan")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    VibrantDestinationTile(Icons.Rounded.BarChart, "Budgets",
                        if (monthlyBudgets.isEmpty()) "Set your first limit"
                        else "${monthlyBudgets.size} active budgets", "pink",
                        onBudgetClick, Modifier.weight(1f))
                    VibrantDestinationTile(Icons.Rounded.TrackChanges, "Goals",
                        if (savingsGoals.isEmpty()) "Set your first goal"
                        else "${savingsGoals.size} savings goals", "mint",
                        onGoalsClick, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    VibrantDestinationTile(Icons.Rounded.CalendarMonth, "Pay cycle",
                        if (snapshot.cycle == null) "Awaiting salary"
                        else "Review cycle", "blue",
                        onPayCycleClick, Modifier.weight(1f))
                    VibrantDestinationTile(Icons.Rounded.Repeat, "Bills",
                        if (upcomingBills.isEmpty()) "No bills due"
                        else "${upcomingBills.size} upcoming", "orange",
                        onBillsClick, Modifier.weight(1f))
                }
            }
        }
        item {
            VibrantPlanSurface {
                VibrantPlanSectionTitle("Coming up next", "See all", onBillsClick)
                if (upcomingBills.isEmpty()) {
                    Text("No upcoming bills. You're all clear for now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    upcomingBills.take(2).forEachIndexed { index, bill ->
                        VibrantPlanListRow(Icons.Rounded.CalendarMonth,
                            if (index == 0) "orange" else "violet",
                            bill.rule.displayLabel ?: bill.rule.name,
                            planBillDueLabel(bill.nextDueEpochMs),
                            formatPlanMoney(bill.remainingMinor), onBillsClick)
                        if (index == 0 && upcomingBills.size > 1) {
                            androidx.compose.material3.HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
                        }
                    }
                }
            }
        }
        if (snapshot.cycle != null) item {
            val base = (snapshot.startingFundsMinor + snapshot.additionalIncomeMinor)
                .coerceAtLeast(1L).toFloat()
            val spent = (snapshot.spentMinor.toFloat() / base).coerceIn(0f,1f)
            val saved = (snapshot.savingsMinor.toFloat() / base).coerceIn(0f,1f)
            VibrantPlanSurface {
                VibrantPlanSectionTitle("Your progress", "View cycle", onPayCycleClick)
                VibrantPlanMeter("Cycle spent", "${(spent * 100).toInt()}%",
                    spent, Icons.Rounded.BarChart, "violet")
                VibrantPlanMeter("Cycle saved", "${(saved * 100).toInt()}%",
                    saved, Icons.Rounded.Savings, "mint")
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
private fun PlanDestinationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tone: String,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(
            icon,
            size = 40.dp,
            background = pesaToneContainer(tone),
            tint = pesaToneContent(tone)
        )
        Column(Modifier.weight(1f).padding(start = 11.dp, end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        if (onClick != null) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
