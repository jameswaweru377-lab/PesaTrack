package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.BudgetStore
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.PayCycleSnapshot
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
import com.kedevjimmy.pesatrack.data.local.SalaryCycleCandidate
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.ui.components.AvailableToLiveHero
import com.kedevjimmy.pesatrack.ui.components.BudgetProgressCard
import com.kedevjimmy.pesatrack.ui.components.BudgetProgressItem
import com.kedevjimmy.pesatrack.ui.components.FundingGapCard
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    scaffoldPadding: PaddingValues,
    onProfileClick: () -> Unit = {},
    onReviewClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onRulesClick: () -> Unit = {},
    onGoalsClick: () -> Unit = {},
    onBudgetClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cycleStore = remember { PayCycleStore.get(context) }
    val transactionStore = remember { TransactionStore.get(context) }
    val budgetStore = remember { BudgetStore.get(context) }
    val categoryStore = remember { CategoryStore.get(context) }
    val ledgerUpdateState by LedgerUpdateBus.state.collectAsState()

    var cycleSnapshot by remember {
        cycleStore.rebuild()
        mutableStateOf(cycleStore.snapshot())
    }
    var ledgerSnapshot by remember {
        mutableStateOf(transactionStore.snapshot())
    }
    var activitySnapshot by remember {
        mutableStateOf(transactionStore.activitySnapshot())
    }
    var monthlyBudgets by remember {
        mutableStateOf(budgetStore.list())
    }

    fun refresh() {
        cycleSnapshot = cycleStore.snapshot()
        ledgerSnapshot = transactionStore.snapshot()
        activitySnapshot = transactionStore.activitySnapshot()
        monthlyBudgets = budgetStore.list()
    }

    LaunchedEffect(ledgerUpdateState.revision) {
        if (ledgerUpdateState.revision > 0L) {
            refresh()
        }
    }

    val spendSlices = remember(activitySnapshot) {
        activitySnapshot.transactions
            .filter {
                it.direction == MoneyDirection.EXPENSE.name &&
                    it.reviewState != ReviewState.EXCLUDED.name &&
                    it.reviewState != ReviewState.NEEDS_REVIEW.name
            }
            .groupBy {
                categoryStore.canonicalId(it.category)
            }
            .mapValues { (_, items) ->
                items.sumOf { it.amountMinor + it.feeMinor }
            }
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
                items.sumOf { it.amountMinor + it.feeMinor }
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

        item {
            AvailableToLiveHero(
                amount = if (cycleSnapshot.cycle == null) {
                    "Waiting for salary"
                } else {
                    formatMoney(cycleSnapshot.availableToLiveMinor)
                },
                subtitle = if (cycleSnapshot.cycle == null) {
                    "PesaTrack will calculate this from real salary-cycle data."
                } else {
                    "After tracked spending, savings, investments and protected commitments."
                },
                footer = cycleSnapshot.safeTodayMinor?.let {
                    "Safe today " + formatMoney(it)
                }
            )
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
                    cycleSnapshot.spentMinor +
                        cycleSnapshot.feesMinor
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
                    icon = Icons.Rounded.FactCheck,
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

        ledgerSnapshot.latestBalanceMinor?.let { balance ->
            item {
                PesaCard {
                    Column(Modifier.padding(15.dp)) {
                        Text(
                            "Last known M-Pesa balance",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            formatMoney(balance),
                            modifier = Modifier.padding(top = 3.dp),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        ledgerSnapshot.latestBalanceObservedAtEpochMs?.let { observed ->
                            Text(
                                "Observed " + formatDateTime(observed) +
                                    " • not a live bank balance",
                                modifier = Modifier.padding(top = 3.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
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
