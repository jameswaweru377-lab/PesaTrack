package com.kedevjimmy.pesatrack.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.everyday.EverydayMoneyPolicy
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Local-only personal receivables. The existing institutional debt cards remain independent. */
@Composable
fun PersonalReceivablesScreen(
    scaffoldPadding: PaddingValues,
    onLiabilities: () -> Unit,
    header: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val store = remember { PersonalReceivableStore(context) }
    val revision by LedgerUpdateBus.state.collectAsState()
    var people by remember { mutableStateOf<List<ReceivablePerson>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedPersonId by rememberSaveable { mutableStateOf<String?>(null) }
    var createKind by remember { mutableStateOf<String?>(null) }
    var deleteEvent by remember { mutableStateOf<ReceivableEvent?>(null) }
    var history by remember { mutableStateOf<List<ReceivableEvent>>(emptyList()) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(revision.revision, selectedPersonId, retry) {
        loading = true
        error = null
        try {
            val data = withContext(Dispatchers.IO) {
                store.people() to selectedPersonId?.let(store::events).orEmpty()
            }
            people = data.first
            history = data.second
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = "Could not read personal debts. Your records have not been changed."
        } finally {
            loading = false
        }
    }
    val selected = people.firstOrNull { it.id == selectedPersonId }
    LazyColumn(
        modifier = Modifier.padding(scaffoldPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            if (selected == null) {
                header?.invoke() ?: DetailPageHeader(
                    title = "Debts", subtitle = "Track borrowing in both directions.", onBack = onLiabilities)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onLiabilities, modifier = Modifier.weight(1f)) { Text("I owe") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {}, modifier = Modifier.weight(1f), enabled = false) { Text("Owed to me") }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedPersonId = null }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to people")
                    }
                    Column {
                        Text(selected.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Personal loan history", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (loading) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        if (error != null) item {
            PesaCard {
                Column(Modifier.padding(16.dp)) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { retry++ }) { Text("Retry") }
                }
            }
        }
        if (selected == null) {
            if (!loading && error == null) {
                val active = people.filter { it.outstandingMinor > 0L }
                val total = runCatching { active.fold(0L) { s, p -> Math.addExact(s, p.outstandingMinor) } }.getOrNull()
                item {
                    PesaCard {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Total owed to you", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(total?.let(::receivableMoney) ?: "Balance unavailable",
                                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(active.size.toString() + " people with outstanding balances",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    Button(onClick = { createKind = PersonalReceivableStore.LENT },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Record money lent")
                    }
                }
                if (people.isEmpty()) item {
                    PesaCard {
                        Text("No personal loans recorded yet. Record a loan or open a payment in Activity to link it.",
                            modifier = Modifier.padding(18.dp))
                    }
                }
                items(people, key = { it.id }) { person ->
                    PesaCard(Modifier.fillMaxWidth().clickable { selectedPersonId = person.id }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Person, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(person.name, fontWeight = FontWeight.SemiBold)
                                Text(if (person.outstandingMinor == 0L) "Settled" else "Outstanding",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(receivableMoney(person.outstandingMinor), fontWeight = FontWeight.Bold)
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        } else if (!loading && error == null) {
            item {
                PesaCard {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Outstanding balance", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(receivableMoney(selected.outstandingMinor),
                            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Only confirmed loan and repayment records are included.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { createKind = PersonalReceivableStore.LENT },
                        modifier = Modifier.weight(1f)) { Text("Add loan") }
                    OutlinedButton(onClick = { createKind = PersonalReceivableStore.REPAID },
                        enabled = selected.outstandingMinor > 0L,
                        modifier = Modifier.weight(1f)) { Text("Record repayment") }
                }
            }
            item { Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(history, key = { it.id }) { event ->
                PesaCard {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (event.kind == PersonalReceivableStore.LENT) Icons.Rounded.AccountBalanceWallet
                             else Icons.Rounded.ReceiptLong, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (event.kind == PersonalReceivableStore.LENT) "Money lent" else "Repayment",
                                fontWeight = FontWeight.Medium)
                            Text(SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(event.occurredAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (!event.active) "Source changed — excluded from balance"
                                 else if (event.transactionId == null) "Manual record • no wallet adjustment"
                                 else "Linked to Activity transaction",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (!event.active) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!event.memo.isNullOrBlank()) Text(event.memo, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(receivableMoney(event.amountMinor), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { deleteEvent = event }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove personal debt record")
                        }
                    }
                }
            }
        }
    }
    if (createKind != null) {
        ReceivableEntryDialog(
            people = people, initialPersonId = selectedPersonId, kind = createKind!!,
            linkedTransaction = null,
            onDismiss = { createKind = null },
            onSaved = { createKind = null; retry++ }
        )
    }
    val removal = deleteEvent
    if (removal != null) {
        val scope = rememberCoroutineScope()
        var busy by remember(removal.id) { mutableStateOf(false) }
        var removeError by remember(removal.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!busy) deleteEvent = null },
            title = { Text("Remove this debt record?") },
            text = { Column {
                Text("Only the personal loan annotation is removed. Original M-Pesa or bank transactions remain unchanged.")
                if (removeError != null) Text(removeError.orEmpty(), color = MaterialTheme.colorScheme.error)
            } },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { store.removeEvent(removal.id) }
                            deleteEvent = null; retry++
                        } catch (cancelled: CancellationException) { throw cancelled
                        } catch (failure: Exception) { removeError = "Could not remove the record." }
                        finally { busy = false }
                    }
                }) { Text("Remove record") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleteEvent = null }) { Text("Cancel") } }
        )
    }
}

/** Transaction Details: user explicitly chooses if a payment was lent or a receipt repaid a loan. */
@Composable
internal fun PersonalReceivableTransactionAction(row: StoredTransaction, onChanged: () -> Unit) {
    if (row.direction !in setOf(MoneyDirection.EXPENSE.name, MoneyDirection.INCOME.name) ||
        row.relationType != null || row.reversalOfCode != null ||
        row.reviewState in setOf(ReviewState.EXCLUDED.name, ReviewState.NEEDS_REVIEW.name)) return
    val context = LocalContext.current
    val store = remember { PersonalReceivableStore(context) }
    val revision by LedgerUpdateBus.state.collectAsState()
    var people by remember { mutableStateOf<List<ReceivablePerson>>(emptyList()) }
    var current by remember(row.id) { mutableStateOf<ReceivableEvent?>(null) }
    var open by rememberSaveable(row.id) { mutableStateOf(false) }
    LaunchedEffect(row.id, revision.revision) {
        try {
            val result = withContext(Dispatchers.IO) { store.people() to store.eventForTransaction(row.id) }
            people = result.first; current = result.second
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { android.util.Log.e("PesaTrackReceivables", "Could not load personal loan link", failure) }
    }
    if (current == null) {
        OutlinedButton(
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Icon(Icons.Rounded.Person, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (row.direction == MoneyDirection.EXPENSE.name) "Money lent / shared bill" else "Received debt repayment")
        }
    } else {
        PesaCard {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Personal debt linked", fontWeight = FontWeight.SemiBold)
                Text((store.nameFor(current!!.personId) ?: "Person") + " • " +
                    receivableMoney(current!!.amountMinor) +
                    if (!current!!.active) " • Source requires review" else "")
                Text("Edit or remove this link from Accounts → Debts → Owed to me.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (open) ReceivableEntryDialog(
        people = people, initialPersonId = null,
        kind = if (row.direction == MoneyDirection.EXPENSE.name) PersonalReceivableStore.LENT else PersonalReceivableStore.REPAID,
        linkedTransaction = row,
        onDismiss = { open = false },
        onSaved = { open = false; onChanged() }
    )
}

@Composable
private fun ReceivableEntryDialog(
    people: List<ReceivablePerson>,
    initialPersonId: String?,
    kind: String,
    linkedTransaction: StoredTransaction?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { PersonalReceivableStore(context) }
    val scope = rememberCoroutineScope()
    var selectedId by remember(initialPersonId, linkedTransaction?.id) { mutableStateOf(initialPersonId) }
    var name by remember(linkedTransaction?.id) {
        mutableStateOf(linkedTransaction?.counterparty.orEmpty())
    }
    var phone by remember(linkedTransaction?.id) {
        mutableStateOf(linkedTransaction?.counterpartyPhone.orEmpty())
    }
    var amount by remember(linkedTransaction?.id) {
        mutableStateOf(linkedTransaction?.amountMinor?.let { java.math.BigDecimal.valueOf(it, 2).toPlainString() }.orEmpty())
    }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(linkedTransaction?.occurredAtEpochMs ?: System.currentTimeMillis()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val existing = people.firstOrNull { it.id == selectedId }
    val money = EverydayMoneyPolicy.money(amount)
    val valid = money != null && money > 0L &&
        (linkedTransaction == null || money <= linkedTransaction.amountMinor) &&
        (existing != null || name.trim().length >= 2)
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (kind == PersonalReceivableStore.LENT) "Record money lent" else "Record repayment") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (linkedTransaction == null)
                    "This is a manual debt record, not a new bank or M-Pesa transaction."
                else "Link some or all of this transaction to a person. The original wallet movement remains unchanged.",
                    style = MaterialTheme.typography.bodySmall)
                if (people.isNotEmpty()) {
                    Text("Choose an existing person", style = MaterialTheme.typography.labelLarge)
                    people.forEach { person ->
                        Row(Modifier.fillMaxWidth().clickable { selectedId = person.id },
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedId == person.id, onClick = { selectedId = person.id })
                            Text(person.name + (person.phone?.let { " • " + it } ?: ""))
                        }
                    }
                    TextButton(onClick = { selectedId = null }) { Text("Different person / new person") }
                }
                if (existing == null) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true,
                        label = { Text("Person's name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, singleLine = true,
                        label = { Text("M-Pesa phone (optional)") }, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it }, singleLine = true,
                    label = { Text("Amount in KES") }, modifier = Modifier.fillMaxWidth())
                if (linkedTransaction == null) {
                    TextButton(onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = date }
                        DatePickerDialog(context,
                            { _, year, month, day ->
                                val chosen = Calendar.getInstance().apply {
                                    set(year, month, day, 12, 0, 0); set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                date = chosen
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                        ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                    }) {
                        Text("Date: " + SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date))
                    }
                }
                OutlinedTextField(value = note, onValueChange = { if (it.length <= 250) note = it },
                    label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
                if (kind == PersonalReceivableStore.REPAID && existing != null)
                    Text("Outstanding: " + receivableMoney(existing.outstandingMinor),
                        style = MaterialTheme.typography.bodySmall)
                if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(enabled = valid && !busy, onClick = {
                busy = true; error = null
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            store.record(
                                personId = selectedId,
                                name = existing?.name ?: name,
                                phone = existing?.phone ?: phone,
                                kind = kind,
                                amountMinor = money ?: throw IllegalArgumentException("Invalid amount."),
                                occurredAt = date,
                                transactionId = linkedTransaction?.id,
                                memo = note
                            )
                        }
                        onSaved()
                    } catch (cancelled: CancellationException) { throw cancelled
                    } catch (failure: Exception) { error = failure.message ?: "Could not save this record." }
                    finally { busy = false }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun receivableMoney(minor: Long): String =
    "KES " + java.text.NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }.format(java.math.BigDecimal.valueOf(minor, 2))
