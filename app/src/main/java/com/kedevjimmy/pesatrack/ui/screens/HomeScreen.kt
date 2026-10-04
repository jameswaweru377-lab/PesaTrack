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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.CycleProtectionStore
import com.kedevjimmy.pesatrack.data.local.DebtAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.LedgerSnapshot
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.PayCycleSnapshot
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.SalaryCycleCandidate
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.RulePreset
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import com.kedevjimmy.pesatrack.domain.rules.AccountIdentityPresentation
import com.kedevjimmy.pesatrack.domain.rules.SalaryExpectationPolicy
import com.kedevjimmy.pesatrack.domain.rules.PlanningAvailabilityPolicy
import com.kedevjimmy.pesatrack.ui.components.AvailableToLiveHero
import com.kedevjimmy.pesatrack.ui.components.FundingGapCard
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.PesaTrackTopBar
import com.kedevjimmy.pesatrack.ui.components.SmartInsightCard
import com.kedevjimmy.pesatrack.ui.components.SpendingSlice
import com.kedevjimmy.pesatrack.ui.components.premiumCategoryColor
import java.text.NumberFormat
import java.math.RoundingMode
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeLoadedState(
    val cycleSnapshot: PayCycleSnapshot,
    val ledgerSnapshot: LedgerSnapshot,
    val activitySnapshot: ActivityLedgerSnapshot,
    val debtAccounts: List<DebtAccountSnapshot>,
    val trackedAccounts: List<MoneyAccount>,
    val confirmedAccountIds: Set<String>,
    val salaryRule: SmartRule?,
    val needsAttentionCount: Int,
    val categoryLabels: Map<String, String> = emptyMap()
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
    onAttentionClick: () -> Unit = {},
    onActivityClick: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onPlanClick: () -> Unit = {},
    onAccountsClick: () -> Unit = {},
    onDebtsClick: () -> Unit = {},
    homeStateLoader: (suspend () -> HomeLoadedState)? = null,
    loadingTimeoutMs: Long = 15_000L
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cycleStore = remember { PayCycleStore.get(context) }
    val transactionStore = remember { TransactionStore.get(context) }
    val categoryStore = remember { CategoryStore.get(context) }
    val cycleProtectionStore =
        remember {
            CycleProtectionStore.get(context)
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
    var homeLoaded by remember { mutableStateOf(false) }
    var homeLoadError by remember { mutableStateOf<String?>(null) }
    var homeLoadJob by remember { mutableStateOf<Job?>(null) }
    var refreshPending by remember { mutableStateOf(false) }

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
    var categoryLabels by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var homeActionError by remember { mutableStateOf<String?>(null) }
    var homeActionRunning by remember { mutableStateOf(false) }
    var needsAttentionCount by remember {
        mutableStateOf(0)
    }

    suspend fun loadHomeState(): HomeLoadedState =
        withContext(Dispatchers.IO) {
            homeStateLoader?.let { return@withContext it() }
            HomeStateReader.read(context)
        }

    fun refresh() {
        if (homeLoadJob?.isActive == true) {
            refreshPending = true
            return
        }
        homeLoadError = null
        if (!homeLoaded) homeLoading = true
        homeLoadJob = homeScope.launch {
            val watchdog = launch {
                delay(loadingTimeoutMs)
                homeLoading = false
                homeLoadError = "Your overview is taking longer than expected. You can still open your accounts or activity."
            }
            try {
                val loaded = loadHomeState()
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
                confirmedAccountIds =
                    loaded.confirmedAccountIds
                salaryRule =
                    loaded.salaryRule
                needsAttentionCount =
                    loaded.needsAttentionCount
                categoryLabels = loaded.categoryLabels
                homeLoaded = true
                homeLoadError = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("PesaTrackHome", "Could not read Home", error)
                homeLoadError = "We couldn't load your overview. Your saved records are still available."
            } finally {
                watchdog.cancel()
                homeLoading = false
                homeLoadJob = null
                if (refreshPending && homeScope.isActive) {
                    refreshPending = false
                    refresh()
                }
            }
        }
    }

    fun updateCycle(action: () -> Unit) {
        if (homeActionRunning) return
        homeActionRunning = true
        homeActionError = null
        homeScope.launch {
            try {
                withContext(Dispatchers.IO) { action() }
                refresh()
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("PesaTrackHome", "Could not update salary cycle", error)
                homeActionError = "We couldn't update this salary. Please try again."
            } finally { homeActionRunning = false }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    LaunchedEffect(
        ledgerUpdateState.revision
    ) {
        if (
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

    if (homeLoading || !homeLoaded) {
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
                                if (homeLoading) "Loading your overview" else "Your overview is unavailable",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                fontWeight =
                                    FontWeight
                                        .SemiBold
                            )
                            Text(
                                homeLoadError ?: "Reading your saved accounts and activity…",
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
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!homeLoading) OutlinedButton(onClick = { refresh() }, enabled = homeLoadJob?.isActive != true) { Text("Try again") }
                    OutlinedButton(onClick = onAccountsClick) { Text("Open accounts") }
                    OutlinedButton(onClick = onActivityClick) { Text("Open activity") }
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
        activitySnapshot.transactions
            .filter {
                it.id in activitySnapshot.trustedTransactionIds &&
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
                                item.id in activitySnapshot.trustedRefundIds
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
                    label = categoryLabels[entry.key] ?: entry.key.lowercase().replace('_', ' '),
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
            activitySnapshot.transactions
                .filter {
                    it.occurredAtEpochMs >=
                        startOfToday &&
                        it.occurredAtEpochMs <
                            startOfTomorrow &&
                        it.id in activitySnapshot.trustedTransactionIds &&
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
                            item.id in activitySnapshot.trustedRefundIds
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
                    .feesMinor
        )
    val unresolvedDuplicateSecondIds =
        activitySnapshot
            .possibleDuplicates
            .map {
                it.second.id
            }
            .toSet()
    val recentCanonicalTransactions =
        activitySnapshot.transactions
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
    val activeDebtOutstandingMinor =
        activeDebtAccounts.sumOf {
            it.outstandingMinor
        }
    val remainingCommitmentCount =
        cycleSnapshot.commitments
            .count {
                it.remainingMinor > 0L
            }

    val manualSalaryPrompt = remember(showManualSalaryDialog, expectedSalaryPrompt, cycleSnapshot.cycle?.startEpochMs) {
        expectedSalaryPrompt ?: ExpectedSalaryPrompt(
                windowStartEpochMs = System.currentTimeMillis(),
                windowEndEpochMs = System.currentTimeMillis(),
                currentCycleStartEpochMs = cycleSnapshot.cycle?.startEpochMs ?: 0L
            )
    }
    if (showManualSalaryDialog) {
        ManualSalaryEntryDialog(
            prompt = manualSalaryPrompt,
            accounts = trackedAccounts,
            initialAccountId = salaryRule?.accountId,
            onDismiss = { showManualSalaryDialog = false },
            onSave = { amountMinor, occurredAtEpochMs, accountId ->
                val inserted = withContext(Dispatchers.IO) {
                    val rule = salaryRule?.copy(accountId = accountId) ?: ruleStore.save(
                        name = "Salary", counterpartyContains = "",
                        messageContains = "pesatrack-manual-salary:$accountId",
                        category = TransactionCategory.INCOME, direction = MoneyDirection.INCOME,
                        displayLabel = "Salary", preset = RulePreset.SALARY,
                        startsPayCycle = true, accountId = accountId,
                        id = "manual-salary-$accountId"
                    )
                    transactionStore.insertManualIncome(
                        amountMinor = amountMinor, occurredAtEpochMs = occurredAtEpochMs,
                        label = rule.displayLabel ?: rule.name.ifBlank { "Salary" }, matchedRule = rule
                    )
                }
                if (inserted) {
                    preferences.edit().remove("salary_prompt_deferred_until").apply()
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
                    withContext(Dispatchers.IO) {
                        cycleProtectionStore.set(
                            cycleStartTransactionId = activeCycle.startTransactionId,
                            amountMinor = amountMinor
                        )
                    }
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

        homeActionError?.let { message -> item { PesaCard { Text(message, Modifier.padding(16.dp)) } } }
        homeLoadError?.let { message ->
            item { PesaCard { Column(Modifier.padding(16.dp)) {
                Text(message)
                TextButton(onClick = { refresh() }, enabled = homeLoadJob?.isActive != true) { Text("Try again") }
            } } }
        }

        item {
            AvailableToLiveHero(
                amount =
                    if (
                        cycleSnapshot.cycle ==
                        null
                    ) {
                        "Waiting for salary"
                    } else {
                        formatMoney(
                            planningAvailability
                                .availableMinor
                        )
                    },
                subtitle =
                    if (
                        planningAvailability
                            .shortfallMinor > 0L
                    ) {
                        "Tracked commitments exceed cycle funds by " +
                            formatMoney(
                                planningAvailability
                                    .shortfallMinor
                            ) +
                            ". This is a planning shortfall, not your account balance."
                    } else {
                        cycleSnapshot.cycle
                            ?.let { cycle ->
                                buildString {
                                append(
                                    "Since "
                                )
                                append(
                                    formatShortDate(
                                        cycle.startEpochMs
                                    )
                                )
                                cycleSnapshot
                                    .estimatedDaysRemaining
                                    ?.let { days ->
                                        append(" • ")
                                        append(days)
                                        append(
                                            if (
                                                days ==
                                                1
                                            ) {
                                                " day remaining"
                                            } else {
                                                " days remaining"
                                            }
                                        )
                                    }
                                }
                            }
                            ?: "PesaTrack will calculate this from real salary-cycle data."
                    },
                footer = null,
                title = "Available after commitments",
                spentToday = null,
                safeToday = null
            )
        }

        item {
            TextButton(onClick = { showManualSalaryDialog = true }) { Text("Add salary") }
        }

        cycleSnapshot.pendingSalary?.let {
                candidate ->
            item {
                PendingSalaryCard(
                    candidate = candidate,
                    onStartCycle = {
                        updateCycle { cycleStore.confirmNewCycle(candidate.transactionId) }
                    },
                    onKeepAsIncome = {
                        updateCycle { cycleStore.keepAsAdditionalIncome(candidate.transactionId) }
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
                    onAddSalary = {
                        showManualSalaryDialog =
                            true
                    },
                    onNotPaidYet = {
                        val deferUntil =
                            System
                                .currentTimeMillis() +
                                24L *
                                60L *
                                60L *
                                1000L
                        preferences.edit()
                            .putLong(
                                "salary_prompt_deferred_until",
                                deferUntil
                            )
                            .apply()
                        salaryPromptDeferredUntil =
                            deferUntil
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
                remainingCommitmentsMinor =
                    cycleSnapshot
                        .remainingCommitmentsMinor,
                remainingCommitmentCount =
                    remainingCommitmentCount,
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
            HomeWhereMoneyWentCard(
                items =
                    homeSpendingItems,
                totalMinor =
                    totalTrustedSpendingMinor,
                onPlanClick =
                    onPlanClick
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
                    ledgerSnapshot
                        .latestBalanceMinor,
                balanceObservedAtEpochMs =
                    ledgerSnapshot
                        .latestBalanceObservedAtEpochMs,
                accountCount =
                    visibleAccountGroups.size,
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
    onSave: suspend (Long) -> Boolean
) {
    val saveScope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
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
        onDismissRequest = { if (!saving) onDismiss() },
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
                onClick = onDismiss, enabled = !saving
            ) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
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
                            else -> null
                        }
                    if (error == null && parsed != null) {
                        saving = true
                        saveScope.launch {
                            try {
                                if (!onSave(parsed)) error = "Could not save this protection."
                            } catch (cancelled: CancellationException) { throw cancelled
                            } catch (failure: Exception) {
                                android.util.Log.e("PesaTrackHome", "Could not protect money", failure)
                                error = "Could not save this protection. Please try again."
                            } finally { saving = false }
                        }
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
    accounts: List<MoneyAccount>,
    initialAccountId: String?,
    onDismiss: () -> Unit,
    onSave: suspend (Long, Long, String) -> Boolean
) {
    val saveScope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var accountId by remember { mutableStateOf(initialAccountId?.takeIf { id -> accounts.any { it.id == id } } ?: accounts.firstOrNull()?.id) }
    var choosingAccount by remember { mutableStateOf(false) }
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
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Add salary") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Add money you actually received. If it already appears in Activity, mark that payment as salary there to avoid counting it twice.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Received in")
                Box {
                    OutlinedButton(onClick = { choosingAccount = true }, enabled = !saving) {
                        Text(accounts.firstOrNull { it.id == accountId }?.name ?: "Choose account")
                    }
                    DropdownMenu(expanded = choosingAccount, onDismissRequest = { choosingAccount = false }) {
                        accounts.forEach { account ->
                            DropdownMenuItem(text = { Text(account.name) }, onClick = {
                                accountId = account.id
                                choosingAccount = false
                            })
                        }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        error = null
                    },
                    enabled = !saving,
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
                    enabled = !saving,
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
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    val amountMinor =
                        parseHomeMoneyMinor(amount)
                    val occurredAt =
                        parseHomeDate(date)

                    error = when {
                        accountId == null -> "Choose the account that received your salary."
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
                        else -> null
                    }
                    val selectedAccountId = accountId
                    if (error == null && amountMinor != null && occurredAt != null && selectedAccountId != null) {
                        saving = true
                        saveScope.launch {
                            try {
                                if (!onSave(amountMinor, occurredAt, selectedAccountId)) error = "That salary entry is already recorded."
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                android.util.Log.e("PesaTrackHome", "Could not save salary", failure)
                                error = "Salary could not be saved. Please try again."
                            } finally { saving = false }
                        }
                    }
                }
            ) {
                Text(if (saving) "Saving…" else "Add salary")
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
        val text = raw.trim()
        val position = ParsePosition(0)
        val date = SimpleDateFormat("d/M/yyyy", Locale.getDefault()).apply {
            isLenient = false
        }.parse(text, position)
        date?.time?.takeIf { position.index == text.length }
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


