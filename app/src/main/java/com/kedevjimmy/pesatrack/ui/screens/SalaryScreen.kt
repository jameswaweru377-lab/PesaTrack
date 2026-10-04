package com.kedevjimmy.pesatrack.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.kedevjimmy.pesatrack.domain.model.*
import com.kedevjimmy.pesatrack.salary.SalaryReminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal fun salaryDate(epoch: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(epoch)
internal fun salaryMoney(minor: Long): String = "KES " + java.text.NumberFormat.getNumberInstance().format(minor / 100.0)
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
    var rule by remember { mutableStateOf<SmartRule?>(null) }
    var accounts by remember { mutableStateOf(emptyList<MoneyAccount>()) }
    var candidates by remember { mutableStateOf(emptyList<SalaryHistoryCandidate>()) }
    var payer by rememberSaveable { mutableStateOf("") }
    var accountId by rememberSaveable { mutableStateOf("") }
    var startDay by rememberSaveable { mutableStateOf("24") }
    var endDay by rememberSaveable { mutableStateOf("28") }
    var amount by rememberSaveable { mutableStateOf("") }
    var receiptDate by rememberSaveable { mutableStateOf(salaryDate(System.currentTimeMillis())) }
    var ready by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var reminders by remember { mutableStateOf(SalaryReminders.preferences(context).getBoolean("salary_reminders_enabled", false)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        reminders = granted
        SalaryReminders.preferences(context).edit().putBoolean("salary_reminders_enabled", granted).apply()
        if (granted) SalaryReminders.checkSoon(context)
        else message = "Phone notifications are disabled. You can still record salary here."
    }
    suspend fun loadCandidates(active: SmartRule?) {
        candidates = withContext(Dispatchers.IO) {
            if (active == null) emptyList() else TransactionStore.get(context).salaryHistoryCandidates(
                payerQuery = active.counterpartyExact.ifBlank { active.counterpartyContains.ifBlank { active.displayLabel ?: active.name } },
                sinceEpochMs = Calendar.getInstance().apply { add(Calendar.MONTH, -3) }.timeInMillis,
                accountId = active.accountId, limit = 100)
        }
    }
    LaunchedEffect(Unit) {
        runCatching {
            val data = withContext(Dispatchers.IO) {
                RuleStore.get(context).listRules(true).firstOrNull { it.startsPayCycle } to AccountStore.get(context).listAccounts()
            }
            rule = data.first; accounts = data.second
            rule?.let {
                payer = it.counterpartyExact.ifBlank { it.counterpartyContains.ifBlank { it.displayLabel ?: it.name } }
                accountId = it.accountId.orEmpty()
                startDay = (it.payWindowStartDay ?: 24).toString(); endDay = (it.payWindowEndDay ?: 28).toString()
            }
            loadCandidates(rule)
        }.onFailure { message = "Could not load salary settings. Reopen this screen to retry." }
        ready = true
    }
    fun perform(action: suspend () -> Unit) {
        busy = true; message = null
        scope.launch {
            try { action() } catch (e: Exception) { message = e.message ?: "Could not save. Please try again." }
            finally { busy = false }
        }
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
        contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Salary & payday", style = MaterialTheme.typography.headlineSmall)
            Text("Confirm the source once. Matching receipts can start future pay cycles automatically. When a message is missing, record only money you actually received.")
        }
        if (!ready) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item {
            OutlinedTextField(payer, { payer = it }, label = { Text("Employer / salary source") }, modifier = Modifier.fillMaxWidth(), enabled = !busy && ready)
            Text("Use the source name shown on the receipt, for example XYZ School.", style = MaterialTheme.typography.bodySmall)
        }
        item {
            Text("Receiving account")
            accounts.forEach { account ->
                Row { RadioButton(selected = accountId == account.id, onClick = { if (!busy) accountId = account.id })
                    TextButton(onClick = { if (!busy) accountId = account.id }) { Text(account.name) } }
            }
            if (accounts.isEmpty() && ready) Text("Add your receiving account in Accounts first.")
        }
        item {
            Text("Expected payday window (days of the month)")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(startDay, { startDay = it }, label = { Text("From day") }, modifier = Modifier.weight(1f), enabled = !busy)
                OutlinedTextField(endDay, { endDay = it }, label = { Text("Through day") }, modifier = Modifier.weight(1f), enabled = !busy)
            }
            Text("For payday on the 28th, enter 28 in both fields. A window such as 28–2 crosses into the next month. Short months use their last day.", style = MaterialTheme.typography.bodySmall)
            Button(enabled = !busy && ready, onClick = {
                val from = startDay.toIntOrNull(); val through = endDay.toIntOrNull()
                if (payer.isBlank() || accountId.isBlank() || from !in 1..31 || through !in 1..31) {
                    message = "Enter the salary source, receiving account and days from 1 to 31."
                } else perform {
                    val saved = withContext(Dispatchers.IO) {
                        val old = rule
                        val unchangedSource = old != null && payer.trim() == old.counterpartyExact.ifBlank {
                            old.counterpartyContains.ifBlank { old.displayLabel ?: old.name }
                        }
                        val savedRule = RuleStore.get(context).save(name = old?.name ?: "Salary",
                            counterpartyContains = if (unchangedSource) old!!.counterpartyContains else "",
                            counterpartyExact = if (unchangedSource) old!!.counterpartyExact else payer.trim(),
                            counterpartyPhoneEquals = if (unchangedSource) old!!.counterpartyPhoneEquals else "",
                            notificationTitleContains = if (unchangedSource) old!!.notificationTitleContains else "",
                            messageContains = if (unchangedSource) old!!.messageContains else "",
                            maximumAmountMinor = old?.maximumAmountMinor, plannedAmountMinor = old?.plannedAmountMinor,
                            planningMonthOffset = old?.planningMonthOffset ?: 0,
                            category = TransactionCategory.INCOME,
                            direction = MoneyDirection.INCOME, preset = RulePreset.SALARY, startsPayCycle = true,
                            displayLabel = payer.trim(), accountId = accountId, payWindowStartDay = from,
                            payWindowEndDay = through, id = old?.id ?: java.util.UUID.randomUUID().toString())
                        PayCycleStore.get(context).rebuild()
                        LedgerUpdateBus.notifyChanged()
                        savedRule
                    }
                    rule = saved; loadCandidates(saved)
                    SalaryReminders.checkSoon(context)
                    message = "Salary source saved. Confirm an existing receipt below or add a missing receipt."
                }
            }) { Text("Save salary source") }
        }
        item {
            Row {
                Switch(checked = reminders, enabled = ready && !busy && rule != null, onCheckedChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else {
                        reminders = enabled
                        SalaryReminders.preferences(context).edit().putBoolean("salary_reminders_enabled", enabled).apply()
                        if (enabled) SalaryReminders.checkSoon(context)
                    }
                })
                Text("Ask me if salary is missing after payday", modifier = Modifier.padding(12.dp))
            }
            Text("Checks run in the background after the window ends. ‘Not yet received’ postpones the next check for three days. You can turn these reminders off here.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { SalaryReminders.defer(context); message = "Okay. Salary is still pending; the next check is in three days." }) { Text("Not yet received") }
        }
        item {
            HorizontalDivider()
            Text("Add a missing salary receipt", style = MaterialTheme.typography.titleLarge)
            Text("Use the actual amount and receipt date. You can enter earlier months separately. Manual entries start at the beginning of the selected date. If the receipt is listed below, confirm it instead.")
            OutlinedTextField(amount, { amount = it }, label = { Text("Amount (KES)") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
            OutlinedTextField(receiptDate, { receiptDate = it }, label = { Text("Received on (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
            Button(enabled = !busy && rule != null, onClick = {
                val minor = parseSalaryAmount(amount); val date = parseSalaryDate(receiptDate); val active = rule
                if (minor == null || date == null || date > System.currentTimeMillis() || active == null) {
                    message = "Enter a positive amount and a valid receipt date that is not in the future."
                } else perform {
                    val inserted = withContext(Dispatchers.IO) {
                        val store = TransactionStore.get(context)
                        val end = Calendar.getInstance().apply { timeInMillis = date; add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
                        val existing = store.salaryHistoryCandidates(active.counterpartyExact.ifBlank { active.counterpartyContains.ifBlank { active.displayLabel ?: active.name } }, date, active.accountId, 100)
                            .any { it.occurredAtEpochMs < end && it.amountMinor == minor }
                        require(!existing) { "A matching receipt already exists. Confirm it below instead." }
                        store.insertManualIncome(minor, date, active.displayLabel ?: active.name, active)
                    }
                    require(inserted) { "This receipt is already recorded." }
                    SalaryReminders.receiptRecorded(context); loadCandidates(active); amount = ""
                    message = "Salary recorded. Your pay-cycle spending now starts on its receipt date."
                }
            }) { Text("Record received salary") }
        }
        message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item { Text("Recent receipts from this source", style = MaterialTheme.typography.titleLarge)
            Text("Past three months. Confirming uses the existing transaction; it does not add income again.") }
        items(candidates, key = { it.transactionId }) { candidate ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(salaryDate(candidate.occurredAtEpochMs) + " · " + salaryMoney(candidate.amountMinor))
                    Text(candidate.counterparty ?: "Salary")
                    Button(enabled = !busy && rule != null, onClick = { perform {
                        withContext(Dispatchers.IO) {
                            val store = TransactionStore.get(context)
                            require(store.applyRuleToTransaction(candidate.transactionId, rule!!)) { "Receipt could not be updated." }
                            PayCycleStore.get(context).confirmNewCycle(candidate.transactionId)
                        }
                        SalaryReminders.receiptRecorded(context); loadCandidates(rule)
                        message = "Receipt confirmed as salary. Pay cycles updated."
                    } }) { Text("Confirm as salary") }
                }
            }
        }
    }
}
