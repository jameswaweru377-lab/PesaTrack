package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.everyday.EverydayMoneyPolicy
import com.kedevjimmy.pesatrack.domain.receivables.BatchTotals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat

/** One local review session. Paging never selects unseen rows. */
@Composable
internal fun ReceivableHistoryReview(
    initialPersonId: String?,
    initialName: String = "",
    initialPhone: String? = null,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val repository = remember { ReceivableHistoryRepository(context) }
    val journal = remember { PersonalReceivableStore(context) }
    val scope = rememberCoroutineScope()
    var personId by remember { mutableStateOf(initialPersonId) }
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone.orEmpty()) }
    var query by remember { mutableStateOf(initialName.ifBlank { initialPhone.orEmpty() }) }
    var people by remember { mutableStateOf<List<ReceivablePerson>>(emptyList()) }
    var personMenu by remember { mutableStateOf(false) }
    var rows by remember { mutableStateOf<List<ReceivableHistoryCandidate>>(emptyList()) }
    var selected by remember { mutableStateOf<Map<Long,ReceivableHistorySelection>>(emptyMap()) }
    var gifts by remember { mutableStateOf<Map<Long,StoredTransaction>>(emptyMap()) }
    var amountInputs by remember { mutableStateOf<Map<Long,String>>(emptyMap()) }
    var offset by remember { mutableIntStateOf(0) }
    var searched by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<BatchTotals?>(null) }
    var confirming by remember { mutableStateOf(false) }
    var showRepayments by remember { mutableStateOf(false) }
    var sessionName by remember { mutableStateOf("") }
    var sessionPhone by remember { mutableStateOf("") }
    var sessionQuery by remember { mutableStateOf("") }
    val revision by LedgerUpdateBus.state.collectAsState()

    LaunchedEffect(Unit) {
        try {
            people = withContext(Dispatchers.IO) { journal.people() }
            people.firstOrNull { it.id == initialPersonId }?.let {
                name = it.name; phone = it.phone.orEmpty(); query = it.name
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = "Could not load people. Close and retry." }
    }
    // A changed ledger invalidates the preview; confirmation also rechecks the actual rows.
    LaunchedEffect(revision.revision) { preview = null; confirming = false }

    fun resetChoices() {
        rows = emptyList(); selected = emptyMap(); gifts = emptyMap()
        amountInputs = emptyMap(); searched = false; preview = null; confirming = false
    }
    fun load(page: Int, fresh: Boolean) {
        if (busy) return
        if (fresh) {
            resetChoices(); sessionName = name; sessionPhone = phone; sessionQuery = query
        }
        busy = true; error = null
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.search(sessionQuery,personId,sessionName,sessionPhone,50,page)
                }
                rows = result; offset = page; searched = true
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: "Could not search imported transactions." }
            finally { busy = false }
        }
    }
    fun choose(row: ReceivableHistoryCandidate, kind: String?) {
        preview = null
        when (kind) {
            "GIFT" -> { gifts = gifts + (row.row.id to row.row); selected = selected - row.row.id }
            "LENT","REPAID" -> {
                val input = amountInputs[row.row.id] ?: java.math.BigDecimal.valueOf(row.row.amountMinor,2).toPlainString()
                val amount = EverydayMoneyPolicy.money(input)
                if (amount == null || amount <= 0 || amount > row.row.amountMinor) {
                    error = "Enter a positive amount within the original payment."; return
                }
                gifts = gifts - row.row.id
                selected = selected + (row.row.id to ReceivableHistorySelection(row.row,kind,amount))
            }
            else -> { gifts = gifts - row.row.id; selected = selected - row.row.id }
        }
    }

    Dialog(onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(),color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Find loans and repayments",style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),fontWeight = FontWeight.Bold)
                    TextButton(enabled = !busy,onClick = onDismiss) { Text("Close") }
                }
                Text("Review imported transactions on this phone. Nothing changes until you confirm.",
                    style = MaterialTheme.typography.bodySmall)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (error != null) Text(error.orEmpty(),color = MaterialTheme.colorScheme.error)
                LazyColumn(Modifier.weight(1f).testTag("receivable-history-list"),verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!confirming) {
                        item {
                            Box {
                                TextButton(enabled = !busy,onClick = { personMenu = true }) {
                                    Text(if (personId == null) "Choose an existing person (optional)" else "Person: " + name)
                                }
                                DropdownMenu(expanded = personMenu,onDismissRequest = { personMenu = false }) {
                                    DropdownMenuItem(text = { Text("New person") },onClick = {
                                        personId = null; name = ""; phone = ""; query = ""; personMenu = false; resetChoices()
                                    })
                                    people.forEach { person ->
                                        DropdownMenuItem(text = { Text(person.name + (person.phone?.let { " • " + it } ?: "")) },
                                            onClick = {
                                                personId = person.id; name = person.name; phone = person.phone.orEmpty()
                                                query = person.name; personMenu = false; resetChoices()
                                            })
                                    }
                                }
                            }
                            if (personId == null) {
                                OutlinedTextField(name,{ name = it; resetChoices() },enabled = !busy,
                                    label = { Text("Person's full name") },singleLine = true,modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(phone,{ phone = it; resetChoices() },enabled = !busy,
                                    label = { Text("Phone number (optional)") },singleLine = true,modifier = Modifier.fillMaxWidth())
                            }
                            OutlinedTextField(query,{ query = it },enabled = !busy,
                                label = { Text("Search name or phone") },singleLine = true,modifier = Modifier.fillMaxWidth())
                            Button(enabled = !busy && name.trim().length in 2..100 && query.trim().length in 2..100,
                                onClick = { load(0,true) }) { Text("Search transactions") }
                        }
                        if (searched) {
                            item {
                                Text("Results from all imported months. Names can belong to different people.",
                                    style = MaterialTheme.typography.bodySmall)
                                Row {
                                    FilterChip(selected = !showRepayments,onClick = { showRepayments = false },
                                        label = { Text("Money sent") })
                                    Spacer(Modifier.width(8.dp))
                                    FilterChip(selected = showRepayments,onClick = { showRepayments = true },
                                        label = { Text("Possible repayments") },modifier = Modifier.testTag("history-repayments"))
                                }
                                if (!showRepayments) TextButton(enabled = !busy,modifier = Modifier.testTag("select-page-loans"),onClick = {
                                    rows.filter { it.row.direction == "EXPENSE" && it.problem == null &&
                                        it.match.bulkEligible && !it.giftExcluded && it.row.id !in gifts }
                                        .forEach { choose(it,"LENT") }
                                }) { Text("Select all eligible loans on this page") }
                                Text("Bulk selection requires a matching phone. Name-only matches need individual review.",
                                    style = MaterialTheme.typography.bodySmall)
                                Text("Selected: " + selected.size + " debt records • " + gifts.size + " gifts/not loans",
                                    style = MaterialTheme.typography.labelLarge)
                            }
                            val visible = rows.filter { if (showRepayments) it.row.direction == "INCOME" else it.row.direction != "INCOME" }
                            if (visible.isEmpty()) item {
                                Text(if (rows.isEmpty()) "No matching imported transactions. If history is missing, use Activity's existing SMS import, then search again."
                                    else "No records in this section on this page.")
                            }
                            items(visible,key = { it.row.id }) { candidate ->
                                val row = candidate.row
                                val choice = selected[row.id]
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp),verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(row.counterparty ?: "Unknown person",fontWeight = FontWeight.SemiBold)
                                        Text((row.counterpartyPhone?.let { it + " • " } ?: "") + candidate.accountName +
                                            " • " + SimpleDateFormat("d MMM yyyy",locale).format(row.occurredAtEpochMs),
                                            style = MaterialTheme.typography.bodySmall)
                                        Text(receivableMoney(row.amountMinor) + if (row.direction == "EXPENSE") " sent" else " received")
                                        Text(candidate.match.reason,style = MaterialTheme.typography.labelSmall)
                                        if (candidate.giftExcluded) Text("Previously marked gift/not a loan",
                                            style = MaterialTheme.typography.labelSmall)
                                        if (candidate.problem != null) Text(candidate.problem,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        else {
                                            val kind = if (row.direction == "EXPENSE") "LENT" else "REPAID"
                                            OutlinedTextField(
                                                amountInputs[row.id] ?: java.math.BigDecimal.valueOf(row.amountMinor,2).toPlainString(),
                                                { input ->
                                                    amountInputs = amountInputs + (row.id to input)
                                                    selected = selected - row.id; preview = null
                                                },enabled = !busy,label = { Text(if (kind == "LENT") "Loan share in KES" else "Repayment in KES") },
                                                singleLine = true,modifier = Modifier.fillMaxWidth().testTag("history-amount-" + row.id))
                                            Row {
                                                Checkbox(checked = choice != null,enabled = !busy,modifier = Modifier.testTag("loan-choice-" + row.id),
                                                    onCheckedChange = { choose(candidate,if (it) kind else null) })
                                                Text(if (kind == "LENT") "Money lent" else "Confirm repayment",
                                                    modifier = Modifier.padding(top = 12.dp))
                                            }
                                            if (kind == "LENT") Row {
                                                Checkbox(checked = row.id in gifts,enabled = !busy,modifier = Modifier.testTag("gift-choice-" + row.id),
                                                    onCheckedChange = { choose(candidate,if (it) "GIFT" else null) })
                                                Text("Gift / not a loan",modifier = Modifier.padding(top = 12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                Row {
                                    TextButton(enabled = !busy && offset > 0,onClick = { load((offset - 50).coerceAtLeast(0),false) }) { Text("Previous page") }
                                    TextButton(enabled = !busy && rows.size == 50,onClick = { load(offset + 50,false) }) { Text("Next page") }
                                }
                                Text("Page " + (offset / 50 + 1) + " • selections from reviewed pages stay in the preview.",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        item {
                            Text("Confirm records for " + name,style = MaterialTheme.typography.titleMedium)
                            preview?.let { total ->
                                Text("Total lent: " + receivableMoney(total.lentMinor))
                                Text("Total repaid: " + receivableMoney(total.repaidMinor))
                                Text("Still owed: " + receivableMoney(total.outstandingMinor),fontWeight = FontWeight.Bold)
                            }
                            Text("This includes existing confirmed history. Gifts are excluded. Original bank and M-Pesa movements remain unchanged.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        items(selected.values.toList().sortedBy { it.row.occurredAtEpochMs },key = { it.row.id }) { choice ->
                            Text((if (choice.kind == "LENT") "Loan: " else "Repayment: ") +
                                (choice.row.counterparty ?: "Person") + " • " + receivableMoney(choice.amountMinor) +
                                " • " + SimpleDateFormat("d MMM yyyy",locale).format(choice.row.occurredAtEpochMs))
                        }
                        items(gifts.values.toList(),key = { -it.id }) { row ->
                            Text("Gift / not a loan: " + (row.counterparty ?: "Person") + " • " + receivableMoney(row.amountMinor))
                        }
                    }
                }
                if (!confirming) Button(
                    enabled = !busy && (selected.isNotEmpty() || gifts.isNotEmpty()) && selected.size + gifts.size <= 500,
                    modifier = Modifier.fillMaxWidth().testTag("history-preview"),onClick = {
                        busy = true; error = null
                        scope.launch {
                            try {
                                preview = withContext(Dispatchers.IO) { repository.preview(personId,selected.values.toList()) }
                                confirming = true
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (failure: Exception) { error = failure.message ?: "Review the selected amounts." }
                            finally { busy = false }
                        }
                    }) { Text("Review " + (selected.size + gifts.size) + " selected records") }
                else Row {
                    OutlinedButton(enabled = !busy,onClick = { confirming = false },modifier = Modifier.weight(1f)) { Text("Back") }
                    Spacer(Modifier.width(8.dp))
                    Button(enabled = !busy && preview != null,modifier = Modifier.weight(1f),onClick = {
                        busy = true; error = null
                        scope.launch {
                            try {
                                val id = withContext(Dispatchers.IO) {
                                    journal.confirmHistory(personId,name,phone,selected.values.toList(),gifts.values.toList())
                                }
                                onSaved(id)
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (failure: Exception) { error = failure.message ?: "Could not save. No records were changed."; confirming = false }
                            finally { busy = false }
                        }
                    }) { Text(if (busy) "Saving…" else "Confirm records") }
                }
            }
        }
    }
}
