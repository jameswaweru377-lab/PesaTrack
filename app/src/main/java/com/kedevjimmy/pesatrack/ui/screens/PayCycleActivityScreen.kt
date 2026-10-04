package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun SpendingPeriodSelector(payCycle: Boolean, onCalendar: () -> Unit, onPayCycle: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(selected = !payCycle, onClick = onCalendar, label = { Text("Calendar month") })
        FilterChip(selected = payCycle, onClick = onPayCycle, label = { Text("Pay cycle") })
    }
}

@Composable
internal fun PayCycleActivityScreen(scaffoldPadding: PaddingValues, onCalendar: () -> Unit,
    onSalaryClick: () -> Unit, onTransactionClick: (Long) -> Unit) {
    val context = LocalContext.current
    val revision by LedgerUpdateBus.state.collectAsState()
    var cycles by remember { mutableStateOf(emptyList<PayCycle>()) }
    var selectedStartId by rememberSaveable { mutableStateOf<Long?>(null) }
    var snapshot by remember { mutableStateOf<ActivityLedgerSnapshot?>(null) }
    var previous by remember { mutableStateOf<ActivityLedgerSnapshot?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var filter by rememberSaveable { mutableStateOf("All") }
    LaunchedEffect(revision.revision, selectedStartId, retry) {
        loading = true; error = null
        try {
            val data = withContext(Dispatchers.IO) {
                val cycleStore = PayCycleStore.get(context)
                val store = TransactionStore.get(context)
                val all = cycleStore.listCycles()
                val selected = all.firstOrNull { it.startTransactionId == selectedStartId } ?: all.firstOrNull()
                val index = all.indexOf(selected)
                val prior = all.getOrNull(index + 1)
                val now = System.currentTimeMillis()
                Triple(all, selected?.let { store.activitySnapshotBetween(it.startEpochMs, it.endEpochMs ?: (now + 1)) },
                    prior?.let { store.activitySnapshotBetween(it.startEpochMs, it.endEpochMs ?: (now + 1)) })
            }
            cycles = data.first; snapshot = data.second; previous = data.third
        } catch (_: Exception) { error = "Could not load pay-cycle activity. Please retry." }
        finally { loading = false }
    }
    val selected = cycles.firstOrNull { it.startTransactionId == selectedStartId } ?: cycles.firstOrNull()
    val index = cycles.indexOf(selected)
    LazyColumn(Modifier.fillMaxSize().padding(scaffoldPadding), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SpendingPeriodSelector(true, onCalendar, {}) }
        item {
            Text("Pay-cycle spending", style = MaterialTheme.typography.headlineSmall)
            Text("All spending since salary arrived, across your accounts, including savings you used. Transfers between your own accounts remain separate from spending.")
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { item { Text(it); Button(onClick = { retry++ }) { Text("Try again") } } }
        if (!loading && error == null && selected == null) item {
            Text("Record or confirm a received salary to start your first pay cycle. An expected payday alone does not create one.")
            Button(onClick = onSalaryClick) { Text("Set up / add salary") }
        }
        if (selected != null && !loading && error == null) {
            item {
                Text(salaryDate(selected.startEpochMs) + " → " + (selected.endEpochMs?.let(::salaryDate) ?: "next received salary"),
                    style = MaterialTheme.typography.titleMedium)
                Text("Includes the opening salary. The next salary starts a new cycle. Recorded receipt times determine the boundary.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(enabled = index + 1 < cycles.size && !loading, onClick = {
                        selectedStartId = cycles[index + 1].startTransactionId; filter = "All"
                    }) { Text("Previous cycle") }
                    TextButton(enabled = index > 0 && !loading, onClick = {
                        selectedStartId = cycles[index - 1].startTransactionId; filter = "All"
                    }) { Text("Next cycle") }
                }
            }
            snapshot?.let { ledger ->
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Money spent", style = MaterialTheme.typography.titleMedium)
                            Text(salaryMoney(ledger.summary.spentMinor), style = MaterialTheme.typography.headlineMedium)
                            Text("Fees: " + salaryMoney(ledger.summary.feesMinor))
                            Text("Money in: " + salaryMoney(ledger.summary.incomeMinor))
                            Text("Transfers: " + salaryMoney(ledger.summary.transferMinor))
                            if (previous != null) {
                                Text("Previous cycle spent: " + salaryMoney(previous!!.summary.spentMinor))
                                if (selected.endEpochMs == null) Text("Current cycle is still in progress.", style = MaterialTheme.typography.bodySmall)
                            }
                            if (ledger.summary.needsReviewCount > 0) Text("${ledger.summary.needsReviewCount} receipts need review and are excluded from trusted totals.")
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Spending", "Income").forEach { choice ->
                            FilterChip(selected = filter == choice, onClick = { filter = choice }, label = { Text(choice) })
                        }
                    }
                }
                val rows = ledger.transactions.filter {
                    filter == "All" || filter == "Spending" && it.direction == "EXPENSE" || filter == "Income" && it.direction == "INCOME"
                }
                if (rows.isEmpty() && !loading) item { Text("No captured transactions in this period.") }
                items(rows, key = { it.id }) { transaction ->
                    ElevatedCard(Modifier.fillMaxWidth().clickable { onTransactionClick(transaction.id) }) {
                        Column(Modifier.padding(14.dp)) {
                            Text(transaction.counterparty ?: transaction.category)
                            Text(salaryMoney(transaction.amountMinor) + " · " + transaction.direction.lowercase())
                            Text(salaryDate(transaction.occurredAtEpochMs), style = MaterialTheme.typography.bodySmall)
                            if (transaction.reviewState == "NEEDS_REVIEW" || transaction.cycleCandidateReason != null) Text("Needs review")
                            if (transaction.reviewState == "EXCLUDED") Text("Excluded")
                        }
                    }
                }
            }
            item { TextButton(onClick = onSalaryClick) { Text("Salary entries & reminders") } }
        }
    }
}
