package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.ui.components.ConceptEmeraldHero
import com.kedevjimmy.pesatrack.ui.components.ConceptMetricTile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun SpendingPeriodSelector(payCycle: Boolean, onCalendar: () -> Unit, onPayCycle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = !payCycle, onClick = onCalendar, label = { Text("Calendar month") }, modifier = Modifier.weight(1f))
        FilterChip(selected = payCycle, onClick = onPayCycle, label = { Text("Pay cycle") }, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun PayCycleActivityScreen(scaffoldPadding: PaddingValues, onCalendar: () -> Unit,
    onSalaryClick: () -> Unit, onTransactionClick: (Long) -> Unit) {
    val context = LocalContext.current
    var accountTypes by remember { mutableStateOf<Map<String, com.kedevjimmy.pesatrack.domain.model.AccountType>>(emptyMap()) }
    LaunchedEffect(Unit) {
        accountTypes = withContext(Dispatchers.IO) { AccountStore.get(context).listAccounts(false).associate { it.id to it.type } }
    }
    val revision by LedgerUpdateBus.state.collectAsState()
    var cycles by remember { mutableStateOf(emptyList<PayCycle>()) }
    var selectedStartId by rememberSaveable { mutableStateOf<Long?>(null) }
    var snapshot by remember { mutableStateOf<ActivityLedgerSnapshot?>(null) }
    var previous by remember { mutableStateOf<ActivityLedgerSnapshot?>(null) }
    var categoryLabels by remember { mutableStateOf(emptyMap<String, String>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var filter by rememberSaveable { mutableStateOf("All") }
    var showHelp by remember { mutableStateOf(false) }
    LaunchedEffect(revision.revision, selectedStartId, retry) {
        loading = true; error = null
        try {
            val data = withContext(Dispatchers.IO) {
                val all = PayCycleStore.get(context).listCycles()
                val store = TransactionStore.get(context)
                val selected = all.firstOrNull { it.startTransactionId == selectedStartId } ?: all.firstOrNull()
                val prior = all.getOrNull(all.indexOf(selected) + 1)
                val now = System.currentTimeMillis()
                CyclePageData(
                    all,
                    selected?.let {
                        store.activitySnapshotBetween(
                            it.startEpochMs,
                            it.endEpochMs ?: (now + 1)
                        )
                    },
                    prior?.let {
                        store.activitySnapshotBetween(
                            it.startEpochMs,
                            it.endEpochMs ?: (now + 1),
                            performMaintenance = false
                        )
                    },
                    CategoryStore.get(context).all().associate {
                        it.id to it.label
                    }
                )
            }
            cycles = data.cycles; snapshot = data.snapshot; previous = data.previous; categoryLabels = data.labels
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (exception: Exception) {
            android.util.Log.e("PesaTrackActivity", "Could not load salary cycle", exception)
            error = "Couldn't load this pay cycle. Please retry."
        } finally { loading = false }
    }
    val selected = cycles.firstOrNull { it.startTransactionId == selectedStartId } ?: cycles.firstOrNull()
    val index = cycles.indexOf(selected)
    LazyColumn(Modifier.fillMaxSize().padding(scaffoldPadding), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SpendingPeriodSelector(true, onCalendar, {}) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("This pay cycle", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("From one salary receipt to the next", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showHelp = true }) { Icon(Icons.Rounded.Info, "How pay cycles work") }
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { item { Text(it); Button(onClick = { retry++ }) { Text("Try again") } } }
        if (!loading && error == null && selected == null) item {
            SalarySection("Start your first pay cycle") {
                Text("Add a salary you've received. Future matching messages can start the next cycle automatically.")
                Button(onClick = onSalaryClick) { Text("Add salary") }
            }
        }
        if (selected != null && !loading && error == null) {
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (cycles.size > 1) IconButton(enabled = index + 1 < cycles.size, onClick = {
                            selectedStartId = cycles[index + 1].startTransactionId; filter = "All"
                        }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Previous cycle") }
                        Column(Modifier.weight(1f).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(salaryDisplayDate(selected.startEpochMs) + " → " + (selected.endEpochMs?.let(::salaryDisplayDate) ?: "Now"), style = MaterialTheme.typography.labelLarge)
                            Text(if (selected.endEpochMs == null) "Current pay cycle" else "Completed pay cycle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (cycles.size > 1) IconButton(enabled = index > 0, onClick = {
                            selectedStartId = cycles[index - 1].startTransactionId; filter = "All"
                        }) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, "Next cycle") }
                    }
                }
            }
            snapshot?.let { ledger ->
                item {
                    val transfers = ledger.trustedTransactions.filter {
                        it.direction == "TRANSFER" && it.relationType != TransactionStore.RELATION_REFUND
                    }
                    val savedMinor = transfers.sumOf { ConceptLedgerPolicy.savingsDelta(it, accountTypes) }
                    val investedMinor = transfers.filter { it.category == "INVESTMENT" }.sumOf { it.amountMinor }
                    val debtMinor = transfers.filter { it.category == "DEBT_LOAN" }.sumOf { it.amountMinor }
                    val surplus = ledger.summary.netMinor - savedMinor - investedMinor - debtMinor

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ConceptEmeraldHero(
                            title = "Cycle surplus",
                            amount = salaryMoney(surplus),
                            subtitle = "After spending, fees, savings, investments and debt repayments",
                            artwork = false,
                            eyebrow = if (selected.endEpochMs == null) "Current pay cycle" else "Completed pay cycle",
                            footer = if (ledger.summary.needsReviewCount > 0) {
                                "${ledger.summary.needsReviewCount} pending reviews are excluded from totals"
                            } else {
                                "Transfers between your own accounts stay separate"
                            }
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ConceptMetricTile(
                                Icons.Rounded.SouthWest,
                                "Money in",
                                salaryMoney(ledger.summary.incomeMinor),
                                Modifier.weight(1f),
                                true
                            )
                            ConceptMetricTile(
                                Icons.Rounded.NorthEast,
                                "Spent",
                                salaryMoney(ledger.summary.spentMinor),
                                Modifier.weight(1f),
                                false
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ConceptMetricTile(
                                Icons.Rounded.Savings,
                                "Saved",
                                salaryMoney(savedMinor),
                                Modifier.weight(1f),
                                true
                            )
                            ConceptMetricTile(
                                Icons.Rounded.TrendingUp,
                                "Invested",
                                salaryMoney(investedMinor),
                                Modifier.weight(1f),
                                true
                            )
                        }
                        Text(
                            "Fees " + salaryMoney(ledger.summary.feesMinor) +
                                " • Transfers " + salaryMoney(ledger.summary.transferMinor),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Earlier carryover: " + salaryMoney(selected.carryoverMinor), style = MaterialTheme.typography.labelSmall)
                        Text("With recorded carryover: " + salaryMoney(selected.carryoverMinor + surplus), style = MaterialTheme.typography.labelSmall)
                        Text("A planning figure, not an account balance.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        previous?.let { prior ->
                            Text(
                                "Previous cycle spent " + salaryMoney(prior.summary.spentMinor),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Spending", "Income", "Transfers").forEach { choice ->
                            FilterChip(selected = filter == choice, onClick = { filter = choice }, label = { Text(choice) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer))
                        }
                    }
                }
                val rows = ledger.transactions.filter {
                    filter == "All" || filter == "Spending" && it.direction == "EXPENSE" ||
                        filter == "Income" && it.direction == "INCOME" || filter == "Transfers" && it.direction == "TRANSFER"
                }
                if (rows.isEmpty()) item { Text("No transactions in this view.", style = MaterialTheme.typography.bodyMedium) }
                rows.groupBy { salaryDisplayDate(it.occurredAtEpochMs) }.forEach { (date, transactions) ->
                    item(key = "date-$date") { Text(date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(transactions, key = { it.id }) { transaction ->
                        CycleTransactionRow(transaction, categoryLabels[transaction.category] ?: transaction.category.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }) {
                            onTransactionClick(transaction.id)
                        }
                    }
                }
            }
            item { TextButton(onClick = onSalaryClick) { Text("Salary entries & reminders") } }
        }
    }
    if (showHelp) AlertDialog(onDismissRequest = { showHelp = false }, title = { Text("How pay cycles work") },
        text = { Text("A cycle starts when salary is received and ends when the next salary arrives. Spending includes all your accounts and savings you used. Transfers between your own accounts are separate from spending. Fees are shown separately. Recorded receipt times determine each boundary; manual entries start at the beginning of the chosen day.") },
        confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Got it") } })
}

private data class CyclePageData(val cycles: List<PayCycle>, val snapshot: ActivityLedgerSnapshot?,
    val previous: ActivityLedgerSnapshot?, val labels: Map<String, String>)

@Composable
private fun CycleMetric(label: String, minor: Long, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(salaryMoney(minor), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CycleTransactionRow(transaction: StoredTransaction, category: String, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.US
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val income = transaction.direction == "INCOME"
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(if (income) Icons.Rounded.SouthWest else if (transaction.direction == "TRANSFER") Icons.Rounded.SwapHoriz else Icons.Rounded.NorthEast,
                    contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(transaction.counterparty?.takeIf { it.isNotBlank() && it != transaction.category } ?: category,
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(category + " · " + SimpleDateFormat("HH:mm", locale).format(transaction.occurredAtEpochMs),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (transaction.reviewState == "EXCLUDED") Text("Excluded", style = MaterialTheme.typography.labelSmall)
                else if (transaction.reviewState == "NEEDS_REVIEW" || transaction.cycleCandidateReason != null) Text("Needs review", style = MaterialTheme.typography.labelSmall)
            }
            Text((if (income) "+" else if (transaction.direction == "EXPENSE") "−" else "") + salaryMoney(transaction.amountMinor),
                style = MaterialTheme.typography.labelLarge, color = if (income) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
    }
}
