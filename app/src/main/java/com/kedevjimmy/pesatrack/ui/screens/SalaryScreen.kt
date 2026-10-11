package com.kedevjimmy.pesatrack.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.model.*
import com.kedevjimmy.pesatrack.salary.SalaryReminders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal fun salaryDate(epoch: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(epoch)
internal fun salaryMoney(minor: Long): String = "KES " + java.text.NumberFormat.getNumberInstance(Locale.US).apply {
    minimumFractionDigits = 2; maximumFractionDigits = 2
}.format(minor / 100.0)
internal fun parseSalaryDate(text: String): Long? = runCatching {
    require(text.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    format.parse(text)?.time?.takeIf { format.format(it) == text }
}.getOrNull()
internal fun parseSalaryAmount(text: String): Long? = runCatching {
    text.trim().replace(",", "").toBigDecimal().movePointRight(2).longValueExact().takeIf { it > 0 }
}.getOrNull()

@Composable
fun SalaryScreen(scaffoldPadding: PaddingValues, onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val repository = remember { SalaryReceiptRepository(context) }
    var rule by remember { mutableStateOf<SmartRule?>(null) }
    var accounts by remember { mutableStateOf(emptyList<MoneyAccount>()) }
    var receipts by remember { mutableStateOf(emptyList<SalaryReceipt>()) }
    var candidates by remember { mutableStateOf(emptyList<SalaryHistoryCandidate>()) }
    var payer by rememberSaveable { mutableStateOf("") }
    var accountId by rememberSaveable { mutableStateOf("") }
    var startDay by rememberSaveable { mutableStateOf("24") }
    var endDay by rememberSaveable { mutableStateOf("28") }
    var amount by rememberSaveable { mutableStateOf("") }
    var receiptDate by rememberSaveable { mutableStateOf(salaryDate(System.currentTimeMillis())) }
    var tab by rememberSaveable { mutableStateOf("Overview") }
    var editPayday by rememberSaveable { mutableStateOf(false) }
    var showCandidates by rememberSaveable { mutableStateOf(false) }
    var visibleReceipts by rememberSaveable { mutableIntStateOf(3) }
    var ready by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var failure by remember { mutableStateOf(false) }
    var removal by remember { mutableStateOf<SalaryReceipt?>(null) }
    var reminders by remember { mutableStateOf(SalaryReminders.preferences(context).getBoolean("salary_reminders_enabled", false)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        reminders = granted
        SalaryReminders.setEnabled(context, granted)
        if (!granted) { message = "Notifications are off. You can still record salary here."; failure = true }
    }
    suspend fun loadReceipts(active: SmartRule?) {
        val data = withContext(Dispatchers.IO) {
            if (active == null) emptyList<SalaryReceipt>() to emptyList<SalaryHistoryCandidate>() else {
                val saved = repository.receipts(active.id)
                val hidden = saved.map { it.transactionId }.toSet() + repository.removedReceiptIds()
                saved to TransactionStore.get(context).salaryHistoryCandidates(
                    payerQuery = active.counterpartyExact.ifBlank { active.counterpartyContains.ifBlank { active.displayLabel ?: active.name } },
                    sinceEpochMs = Calendar.getInstance().apply { add(Calendar.MONTH, -3) }.timeInMillis,
                    accountId = active.accountId, limit = 100).filterNot { it.transactionId in hidden }
            }
        }
        receipts = data.first; candidates = data.second
    }
    LaunchedEffect(Unit) {
        try {
            val data = withContext(Dispatchers.IO) {
                val active = RuleStore.get(context).listRules(true).firstOrNull { it.startsPayCycle }
                val owned = FinancialUnderstandingStore.get(context).confirmedAccountIds() + listOfNotNull(active?.accountId)
                active to AccountStore.get(context).listAccounts().filter {
                    com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy.isConfirmedOwned(it, owned, includeCash = true)
                }
            }
            rule = data.first; accounts = data.second
            rule?.let {
                payer = it.displayLabel ?: it.counterpartyExact.ifBlank { it.counterpartyContains.ifBlank { it.name } }
                accountId = it.accountId.orEmpty()
                startDay = (it.payWindowStartDay ?: 24).toString(); endDay = (it.payWindowEndDay ?: 28).toString()
            }
            if (accountId.isBlank()) accountId = accounts.firstOrNull()?.id.orEmpty()
            if (rule == null) tab = "Source"
            loadReceipts(rule)
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (error: Exception) {
            android.util.Log.e("PesaTrackSalary", "Could not load salary", error)
            message = "Couldn't load salary. Reopen this page to retry."; failure = true
        } finally { ready = true }
    }
    LaunchedEffect(tab) { listState.scrollToItem(0) }
    fun perform(action: suspend () -> Unit) {
        if (busy) return
        busy = true; message = null; failure = false
        scope.launch {
            try { action()
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: IllegalArgumentException) { message = error.message ?: "Please check your entry."; failure = true
            } catch (error: Exception) {
                android.util.Log.e("PesaTrackSalary", "Could not update salary", error)
                message = "Couldn't save this change. Please try again."; failure = true
            } finally { busy = false }
        }
    }
    fun confirm(candidate: SalaryHistoryCandidate) = perform {
        val active = rule ?: return@perform
        withContext(Dispatchers.IO) {
            require(TransactionStore.get(context).applyRuleToTransaction(candidate.transactionId, active)) { "This receipt could not be updated." }
            PayCycleStore.get(context).confirmNewCycle(candidate.transactionId)
        }
        SalaryReminders.receiptRecorded(context); loadReceipts(active)
        message = "Salary recorded from the existing payment."
    }
    LazyColumn(Modifier.fillMaxSize().padding(scaffoldPadding).testTag("salary-page"),
        state = listState, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (tab == "Overview") "Salary" else "Add salary",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when (tab) {
                            "Source" -> "1 of 2 • Salary source"
                            "Add salary" -> "2 of 2 • Salary receipt"
                            else -> "Your pay, kept up to date"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (tab == "Source" || tab == "Add salary") {
                LinearProgressIndicator(
                    progress = { if (tab == "Source") .5f else 1f },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
        item {
            TabRow(selectedTabIndex = listOf("Overview", "Add salary", "Source").indexOf(tab)) {
                listOf("Overview", "Add salary", "Source").forEach { label ->
                    Tab(selected = tab == label, onClick = { if (!busy) { tab = label; message = null } },
                        enabled = ready && !busy, text = { Text(label) }, modifier = Modifier.testTag("salary-tab-$label"))
                }
            }
        }
        if (!ready || busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        message?.let { status -> item {
            Surface(color = if (failure) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Text(status, Modifier.padding(12.dp), color = if (failure) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer)
            }
        } }
        if (ready && tab == "Overview") {
            val latest = receipts.firstOrNull()
            item {
                SalarySection(if (latest == null) "No salary recorded yet" else "Salary recorded", highlighted = true) {
                    if (latest != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }
                            Text(
                                salaryMoney(latest.amountMinor),
                                modifier = Modifier.padding(top = 10.dp),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            rule?.let { active ->
                                Text(
                                    (active.displayLabel ?: active.name) + " • " +
                                        (accounts.firstOrNull { it.id == active.accountId }?.name ?: "Receiving account"),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                salaryDisplayDate(latest.receivedAtEpochMs),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            "Add the salary you actually received. PesaTrack never creates income from an expected payday.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    rule?.let { active ->
                        Text(
                            "Pay cycle • expected days ${active.payWindowStartDay ?: 24}–${active.payWindowEndDay ?: 28}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { tab = if (rule == null) "Source" else "Add salary" }, modifier = Modifier.weight(1f), enabled = !busy) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                            Text(if (rule == null) "Set up salary" else "Add salary")
                        }
                        OutlinedButton(onClick = { tab = "Source" }, enabled = !busy) {
                            Icon(Icons.Rounded.Edit, "Edit salary source", Modifier.size(18.dp))
                        }
                    }
                }
            }
            if (receipts.isNotEmpty()) {
                item { Text("Salary history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                items(receipts.take(visibleReceipts), key = { it.transactionId }) { receipt ->
                    SalaryReceiptRow(receipt, accounts.firstOrNull { it.id == receipt.accountId }?.name,
                        enabled = !busy, onRemove = { removal = receipt })
                }
                if (receipts.size > visibleReceipts) item {
                    TextButton(onClick = { visibleReceipts += 5 }) { Text("Show more salary entries") }
                }
            }
            if (candidates.isNotEmpty()) {
                item { TextButton(onClick = { showCandidates = !showCandidates }) {
                    Text(if (showCandidates) "Hide matching payments" else "Matching payments (${candidates.size})")
                } }
                if (showCandidates) items(candidates.take(5), key = { "candidate-${it.transactionId}" }) { candidate ->
                    SalarySection(salaryMoney(candidate.amountMinor)) {
                        Text((candidate.counterparty ?: "Payment") + " · " + salaryDisplayDate(candidate.occurredAtEpochMs), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { confirm(candidate) }, enabled = !busy) { Text("Use as salary") }
                    }
                }
            }
        }
        if (ready && tab == "Source") {
            item {
                SalarySection("Where does your salary come from?") {
                    OutlinedTextField(payer, { payer = it }, label = { Text("Employer") },
                        supportingText = { Text("Use the name shown in your payment message.") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), enabled = !busy)
                    Text("Receiving account", style = MaterialTheme.typography.labelLarge)
                    SalaryAccountGrid(accounts, accountId, !busy) { accountId = it }
                    if (accounts.isEmpty()) Text("Add a receiving account in Accounts first.")
                }
            }
            item {
                SalarySection("Payday & reminders") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Expected payday", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Days $startDay–$endDay each month", style = MaterialTheme.typography.bodyLarge)
                        }
                        TextButton(onClick = { editPayday = !editPayday }, enabled = !busy) { Text(if (editPayday) "Done" else "Change") }
                    }
                    if (editPayday) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(startDay, { startDay = it.filter(Char::isDigit).take(2) }, label = { Text("From day") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), enabled = !busy)
                            OutlinedTextField(endDay, { endDay = it.filter(Char::isDigit).take(2) }, label = { Text("Through day") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), enabled = !busy)
                        }
                        Text("Use 28–28 for one payday, or 28–2 across two months.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Missing-salary reminder", style = MaterialTheme.typography.bodyMedium)
                            Text("After your payday window", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(reminders, onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else { reminders = enabled; SalaryReminders.setEnabled(context, enabled) }
                        }, enabled = !busy && rule != null)
                    }
                }
            }
            item {
                Button(enabled = !busy && accounts.isNotEmpty(), modifier = Modifier.fillMaxWidth(), onClick = {
                    val from = startDay.toIntOrNull(); val through = endDay.toIntOrNull()
                    if (payer.isBlank() || accountId.isBlank() || from !in 1..31 || through !in 1..31) {
                        message = "Enter your employer, receiving account and payday days from 1 to 31."; failure = true
                    } else perform {
                        val saved = withContext(Dispatchers.IO) {
                            val old = rule
                            val unchangedSource = old != null && payer.trim() == (old.displayLabel ?: old.counterpartyExact.ifBlank { old.counterpartyContains.ifBlank { old.name } })
                            val savedRule = RuleStore.get(context).save(name = old?.name ?: "Salary",
                                counterpartyContains = if (unchangedSource) old!!.counterpartyContains else "",
                                counterpartyExact = if (unchangedSource) old!!.counterpartyExact else payer.trim(),
                                counterpartyPhoneEquals = if (unchangedSource) old!!.counterpartyPhoneEquals else "",
                                notificationTitleContains = if (unchangedSource) old!!.notificationTitleContains else "",
                                messageContains = if (unchangedSource) old!!.messageContains else "",
                                maximumAmountMinor = old?.maximumAmountMinor, plannedAmountMinor = old?.plannedAmountMinor,
                                planningMonthOffset = old?.planningMonthOffset ?: 0,
                                category = TransactionCategory.INCOME, direction = MoneyDirection.INCOME,
                                preset = RulePreset.SALARY, startsPayCycle = true, displayLabel = payer.trim(), accountId = accountId,
                                payWindowStartDay = from, payWindowEndDay = through, id = old?.id ?: java.util.UUID.randomUUID().toString())
                            PayCycleStore.get(context).rebuild(); LedgerUpdateBus.notifyChanged()
                            savedRule
                        }
                        rule = saved; loadReceipts(saved); SalaryReminders.checkSoon(context)
                        tab = "Add salary"; editPayday = false; message = "Salary source saved. Record the salary you actually received."
                    }
                }) { Text("Save salary source") }
            }
        }
        if (ready && tab == "Add salary") {
            val active = rule
            item {
                SalarySection("What did you receive?") {
                    if (active == null) {
                        Text("Set your salary source once to get started.")
                        Button(onClick = { tab = "Source" }) { Text("Set up salary source") }
                    } else {
                        Text((active.displayLabel ?: active.name) + " · " + (accounts.firstOrNull { it.id == active.accountId }?.name ?: "Receiving account"), style = MaterialTheme.typography.labelLarge)
                        Text("Record only money that arrived in this account. You do not need an SMS, statement or payslip. An expected payday or payslip net pay alone is not received salary.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(amount, { amount = it }, label = { Text("Amount") }, prefix = { Text("KES ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("salary-amount"), enabled = !busy)
                        SalaryReceiptDateButton(parseSalaryDate(receiptDate) ?: System.currentTimeMillis(), !busy) { receiptDate = salaryDate(it) }
                        Button(enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("salary-record"), onClick = {
                            val minor = parseSalaryAmount(amount); val date = parseSalaryDate(receiptDate)
                            if (minor == null || date == null || date > System.currentTimeMillis()) {
                                message = "Enter a positive amount and select its receipt date."; failure = true
                            } else perform {
                                val inserted = withContext(Dispatchers.IO) {
                                    repository.recordManualSalary(minor, date, active)
                                }
                                require(inserted) { "This salary is already recorded." }
                                SalaryReminders.receiptRecorded(context); loadReceipts(active); amount = ""; tab = "Overview"
                                message = "Salary recorded. Your pay cycle is updated."
                            }
                        }) { Text(if (busy) "Saving…" else "Record received salary") }
                        TextButton(enabled = !busy, onClick = { SalaryReminders.defer(context); message = "We'll check again in three days." }) { Text("Not yet received") }
                    }
                }
            }
        }
    }
    removal?.let { receipt ->
        AlertDialog(onDismissRequest = { if (!busy) removal = null },
            title = { Text("Remove salary?") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(salaryMoney(receipt.amountMinor) + " · " + salaryDisplayDate(receipt.receivedAtEpochMs), fontWeight = FontWeight.SemiBold)
                Text(if (receipt.manualOnly) "This entry will be removed from your totals. Your pay cycle will be recalculated."
                    else "This payment will stay in Activity as ordinary income. It will no longer start a salary cycle.")
                Text("Your saved employer will still detect future matching messages.", style = MaterialTheme.typography.bodySmall)
                if (failure) message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            dismissButton = { TextButton(enabled = !busy, onClick = { removal = null }) { Text("Cancel") } },
            confirmButton = { TextButton(enabled = !busy, modifier = Modifier.testTag("salary-remove-confirm"), onClick = { perform {
                val active = rule ?: return@perform
                require(withContext(Dispatchers.IO) { repository.removeSalary(receipt.transactionId, active.id) }) { "This salary has already changed. Reopen the page to refresh it." }
                removal = null; loadReceipts(active); SalaryReminders.checkSoon(context)
                message = "Salary removed. Your pay cycle is updated."
            } }) { Text(if (busy) "Removing…" else "Remove", color = MaterialTheme.colorScheme.error) } }
        )
    }
}

@Composable
private fun SalaryReceiptRow(receipt: SalaryReceipt, accountName: String?, enabled: Boolean, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(salaryMoney(receipt.amountMinor), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(salaryDisplayDate(receipt.receivedAtEpochMs) + (accountName?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (receipt.manualOnly) "Added manually" else "From a payment message", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(enabled = enabled, onClick = onRemove, modifier = Modifier.testTag("salary-remove-${receipt.transactionId}")) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove salary entry")
            }
        }
    }
}
