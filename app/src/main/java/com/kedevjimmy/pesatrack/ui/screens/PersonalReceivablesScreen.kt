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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.kedevjimmy.pesatrack.domain.receivables.SharedBillPolicy
import com.kedevjimmy.pesatrack.domain.receivables.ReceivableHistoryPolicy
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

/** Last complete overview, kept in memory only and invalidated by ledger revision. */
class PersonalReceivablesScreenCache {
    internal var people: List<ReceivablePerson> = emptyList()
    internal var revision: Long = -1L
}

/** Local-only personal receivables. The existing institutional debt cards remain independent. */
@Composable
fun PersonalReceivablesScreen(
    scaffoldPadding: PaddingValues,
    onLiabilities: () -> Unit,
    header: (@Composable () -> Unit)? = null,
    stateCache: PersonalReceivablesScreenCache? = null
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        com.kedevjimmy.pesatrack.planning.SmartNoticePreferences(context).completeDiscovery()
        withContext(Dispatchers.IO) {
            runCatching { com.kedevjimmy.pesatrack.planning.SmartNoticeCoordinator.check(context,allowPost = false) }
        }
    }
    val locale = LocalConfiguration.current.locales[0]
    val store = remember { PersonalReceivableStore(context) }
    val revision by LedgerUpdateBus.state.collectAsState()
    val cached = remember(stateCache) {
        stateCache?.takeIf { it.revision >= 0L }
    }
    var people by remember { mutableStateOf(cached?.people ?: emptyList()) }
    var peopleRevision by remember { mutableStateOf(cached?.revision ?: -1L) }
    var peopleLoaded by remember { mutableStateOf(cached != null) }
    var loading by remember { mutableStateOf(cached == null) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedPersonId by rememberSaveable { mutableStateOf<String?>(null) }
    var loadedPersonId by remember { mutableStateOf<String?>(null) }
    var createKind by remember { mutableStateOf<String?>(null) }
    var deleteEvent by remember { mutableStateOf<ReceivableEvent?>(null) }
    var history by remember { mutableStateOf<List<ReceivableEvent>>(emptyList()) }
    var historyLinks by remember { mutableStateOf<Map<Long,ReceivableActivityLink>>(emptyMap()) }
    var retry by remember { mutableIntStateOf(0) }
    var reviewHistory by remember { mutableStateOf(false) }

    LaunchedEffect(revision.revision, selectedPersonId, retry) {
        val revisionToLoad = revision.revision
        // Re-entering the overview at the same revision needs no database work.
        if (peopleLoaded && peopleRevision == revisionToLoad &&
            selectedPersonId == null && retry == 0
        ) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            val data = withContext(Dispatchers.IO) {
                val currentPeople = if (
                    peopleLoaded && peopleRevision == revisionToLoad && retry == 0
                ) people else traceFinancialRead("receivables.people") { store.people() }
                val currentHistory = selectedPersonId?.let { id ->
                    traceFinancialRead("receivables.history") { store.events(id) }
                }.orEmpty()
                val links = if (selectedPersonId == null) emptyMap()
                else traceFinancialRead("receivables.activityLinks") {
                    loadReceivableActivityLinks(context)
                }
                Triple(currentPeople, currentHistory, links)
            }
            people = data.first
            history = data.second
            historyLinks = data.third
            loadedPersonId = selectedPersonId
            peopleRevision = revisionToLoad
            peopleLoaded = true
            stateCache?.apply {
                this.people = data.first
                this.revision = revisionToLoad
            }
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
                        Text("Loans and shared bills", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        // A localized first-load indicator only. No screen-wide progress bar
        // or clearing people cards for an ordinary background refresh.
        if (loading && !peopleLoaded) item {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Preparing your personal debts", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (loading && peopleLoaded && selectedPersonId != null &&
            loadedPersonId != selectedPersonId
        ) item {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Opening this person's history", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (error != null) item {
            PesaCard {
                Column(Modifier.padding(16.dp)) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { retry++ }) { Text("Retry") }
                }
            }
        }
        if (selected == null) {
            if (peopleLoaded) {
                val active = people.filter { (it.outstandingMinor ?: 0L) > 0L }
                val total = if (people.any { it.outstandingMinor == null }) null
                    else runCatching { active.fold(0L) { s, p -> Math.addExact(s, p.outstandingMinor!!) } }.getOrNull()
                item {
                    PesaCard {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                if (peopleRevision != revision.revision) "Last verified total owed to you"
                                else "Total owed to you",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                item {
                    OutlinedButton(onClick = { reviewHistory = true },modifier = Modifier.fillMaxWidth()) {
                        Text("Find in transactions")
                    }
                }
                if (people.isEmpty()) item {
                    PesaCard {
                        Text(
                             if (peopleRevision != revision.revision) "No personal loans were recorded at the last successful refresh."
                             else "No personal loans recorded yet. Record a loan or open a payment in Activity to link it.",
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
                                Text(if (person.outstandingMinor == null) "Review loan and repayment history"
                                     else if (person.outstandingMinor == 0L) "Settled" else "Outstanding",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(person.outstandingMinor?.let(::receivableMoney) ?: "Balance unavailable", fontWeight = FontWeight.Bold)
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        } else if (peopleLoaded && loadedPersonId == selectedPersonId) {
            item {
                PesaCard {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Outstanding balance", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(selected.outstandingMinor?.let(::receivableMoney) ?: "Balance unavailable",
                            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(if (selected.outstandingMinor == null)
                            "Source evidence changed. Review the history and remove incorrect repayment records. No balance is assumed."
                            else "Only confirmed loan and repayment records are included.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { createKind = PersonalReceivableStore.LENT },
                        modifier = Modifier.weight(1f)) { Text("Add loan") }
                    OutlinedButton(onClick = { createKind = PersonalReceivableStore.REPAID },
                        enabled = (selected.outstandingMinor ?: 0L) > 0L,
                        modifier = Modifier.weight(1f)) { Text("Record repayment") }
                }
            }
            item {
                OutlinedButton(onClick = { reviewHistory = true },modifier = Modifier.fillMaxWidth()) {
                    Text("Find loans and repayments in transactions")
                }
                val lent = runCatching { history.filter { it.active && it.kind == PersonalReceivableStore.LENT }
                    .fold(0L) { sum, event -> Math.addExact(sum,event.amountMinor) } }.getOrNull()
                val repaid = runCatching { history.filter { it.active && it.kind == PersonalReceivableStore.REPAID }
                    .fold(0L) { sum, event -> Math.addExact(sum,event.amountMinor) } }.getOrNull()
                Text("Money lent: " + (lent?.let(::receivableMoney) ?: "Unavailable"))
                Text("Repaid: " + (repaid?.let(::receivableMoney) ?: "Unavailable"))
            }
            item { Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(history, key = { it.id }) { event ->
                PesaCard {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (event.kind == PersonalReceivableStore.LENT) Icons.Rounded.AccountBalanceWallet
                             else Icons.Rounded.ReceiptLong, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            val link = event.transactionId?.let { historyLinks[it] }
                            Text((link?.title ?: if (event.kind == PersonalReceivableStore.LENT) "Money lent" else "Repayment") +
                                (link?.sourceCategory?.let { " · " + it } ?: ""),
                                fontWeight = FontWeight.Medium)
                            Text(SimpleDateFormat("d MMM yyyy", locale).format(event.occurredAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (!event.active) "Source changed — excluded from balance"
                                 else if (event.transactionId == null) "Manual record • no wallet adjustment"
                                 else "Linked to Activity transaction",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (!event.active) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                            link?.billSummary?.let { Text(it,style = MaterialTheme.typography.labelSmall) }
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
    if (reviewHistory) {
        ReceivableHistoryReview(selectedPersonId,selected?.name.orEmpty(),selected?.phone,
            onDismiss = { reviewHistory = false },
            onSaved = { id -> reviewHistory = false; selectedPersonId = id; retry++ })
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
    var sharedBill by rememberSaveable(row.id) { mutableStateOf(false) }
    var presentation by remember(row.id) { mutableStateOf<ReceivableActivityLink?>(null) }
    var hasCategorySplit by remember(row.id) { mutableStateOf(false) }
    var actionLoading by remember(row.id) { mutableStateOf(true) }
    var actionError by remember(row.id) { mutableStateOf(false) }
    var reviewHistory by remember(row.id) { mutableStateOf(false) }
    var reviewPersonId by remember(row.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(row.id, revision.revision) {
        actionLoading = true; actionError = false
        try {
            val result = withContext(Dispatchers.IO) {
                Triple(store.people(),store.eventForTransaction(row.id),
                    loadReceivableActivityLinks(context)[row.id] to EverydayMoneyStore(context).allocations(row.id).isNotEmpty())
            }
            people = result.first; current = result.second
            presentation = result.third.first; hasCategorySplit = result.third.second
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { actionError = true }
        finally { actionLoading = false }
    }
    if (actionLoading) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    } else if (actionError) {
        Text("Could not read the debt link. Close and reopen this payment to try again.",color = MaterialTheme.colorScheme.error)
    } else if (current == null) {
        if (hasCategorySplit) {
            Text("This payment has a saved category split. Remove it using Edit saved category split before recording a loan or shared bill.",
                style = MaterialTheme.typography.bodySmall)
        } else {
            OutlinedButton(onClick = { sharedBill = false; open = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("record-money-lent")) {
                Text(if (row.direction == MoneyDirection.EXPENSE.name) "Money lent" else "Received debt repayment")
            }
            if (row.direction == MoneyDirection.EXPENSE.name) {
                OutlinedButton(onClick = { sharedBill = true; open = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("record-shared-bill")) {
                    Text("Shared bill")
                }
            }
        }
    } else {
        PesaCard {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(presentation?.description ?: "Personal debt record",fontWeight = FontWeight.SemiBold)
                presentation?.billSummary?.let { Text(it,style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = { reviewPersonId = current!!.personId; reviewHistory = true }) {
                    Text("Review this person's history")
                }
                Text("Edit or remove this link from Accounts → Debts → Owed to me.",
                    style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (reviewHistory) {
        LaunchedEffect(row.id) {
            try { reviewPersonId = withContext(Dispatchers.IO) { store.eventForTransaction(row.id)?.personId } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { reviewHistory = false }
        }
        if (reviewPersonId != null) ReceivableHistoryReview(reviewPersonId,row.counterparty.orEmpty(),row.counterpartyPhone,
            onDismiss = { reviewHistory = false },
            onSaved = { reviewHistory = false; onChanged() })
    }
    if (open) ReceivableEntryDialog(
        people = people, initialPersonId = null,
        kind = if (row.direction == MoneyDirection.EXPENSE.name) PersonalReceivableStore.LENT else PersonalReceivableStore.REPAID,
        linkedTransaction = row, sharedBill = sharedBill,
        onDismiss = { open = false },
        onSaved = {
            open = false; onChanged()
            reviewPersonId = null; reviewHistory = true
        }
    )
}

@Composable
internal fun ReceivableEntryDialog(
    people: List<ReceivablePerson>,
    initialPersonId: String?,
    kind: String,
    linkedTransaction: StoredTransaction?,
    sharedBill: Boolean = false,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val store = remember { PersonalReceivableStore(context) }
    val scope = rememberCoroutineScope()
    var selectedId by remember(initialPersonId, linkedTransaction?.id) {
        val sourcePhone = if (sharedBill) null else ReceivableHistoryPolicy.normalizedPhone(linkedTransaction?.counterpartyPhone)
        val matches = if (sourcePhone == null) emptyList() else people.filter {
            ReceivableHistoryPolicy.normalizedPhone(it.phone) == sourcePhone
        }
        mutableStateOf(initialPersonId ?: matches.singleOrNull()?.id)
    }
    var name by remember(linkedTransaction?.id) {
        mutableStateOf(if (sharedBill) "" else linkedTransaction?.counterparty.orEmpty())
    }
    var phone by remember(linkedTransaction?.id) {
        mutableStateOf(if (sharedBill) "" else linkedTransaction?.counterpartyPhone.orEmpty())
    }
    var amount by remember(linkedTransaction?.id) {
        mutableStateOf(if (sharedBill) "" else linkedTransaction?.amountMinor?.let { java.math.BigDecimal.valueOf(it, 2).toPlainString() }.orEmpty())
    }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(linkedTransaction?.occurredAtEpochMs ?: System.currentTimeMillis()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val existing = people.firstOrNull { it.id == selectedId }
    val money = EverydayMoneyPolicy.money(amount)
    val valid = money != null && money > 0L &&
        (linkedTransaction == null || money <= linkedTransaction.amountMinor) &&
        (!sharedBill || (linkedTransaction != null && SharedBillPolicy.validAmount(linkedTransaction.amountMinor,money))) &&
        (existing != null || name.trim().length >= 2)
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (sharedBill) "Record shared bill" else if (kind == PersonalReceivableStore.LENT) "Record money lent" else "Record repayment") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (sharedBill)
                    "Choose who shared this bill and enter their share. The payment recipient is the merchant, so no person is selected automatically."
                else if (linkedTransaction == null)
                    "This is a manual debt record, not a new bank or M-Pesa transaction."
                else if (kind == PersonalReceivableStore.LENT)
                    "Record the full payment as money lent. For part of a bill, choose Shared bill instead."
                else "Link some or all of this receipt to a repayment. The original wallet movement remains unchanged.",
                    style = MaterialTheme.typography.bodySmall)
                if (sharedBill) {
                    SharedBillPersonFields(people,selectedId,existing?.name ?: name,existing?.phone ?: phone,busy,
                        onExisting = { person -> selectedId = person?.id; name = person?.name.orEmpty(); phone = person?.phone.orEmpty() },
                        onName = { name = it; phone = "" },onPhone = { phone = it },
                        onIdentity = { fullName,number ->
                            val normalized = ReceivableHistoryPolicy.normalizedPhone(number)
                            val matches = people.filter { normalized != null &&
                                ReceivableHistoryPolicy.normalizedPhone(it.phone) == normalized &&
                                ReceivableHistoryPolicy.normalizedName(it.name) == ReceivableHistoryPolicy.normalizedName(fullName) }
                            selectedId = matches.singleOrNull()?.id
                            name = fullName; phone = number.orEmpty()
                        })
                }
                if (!sharedBill && people.isNotEmpty()) {
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
                if (!sharedBill && existing == null) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true,
                        label = { Text("Person's name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, singleLine = true,
                        label = { Text("M-Pesa phone (optional)") }, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it }, singleLine = true,
                    readOnly = linkedTransaction != null && kind == PersonalReceivableStore.LENT && !sharedBill,
                    enabled = !busy,label = { Text(if (sharedBill) "Their share in KES" else "Amount in KES") },
                    modifier = Modifier.fillMaxWidth().testTag("receivable-entry-amount"))
                if (sharedBill && linkedTransaction != null) {
                    Text("Bill: " + receivableMoney(linkedTransaction.amountMinor) + " · " +
                        CategoryStore.get(context).labelFor(linkedTransaction.category),style = MaterialTheme.typography.bodySmall)
                    if (money != null && SharedBillPolicy.validAmount(linkedTransaction.amountMinor,money)) {
                        Text("Owed to me: " + receivableMoney(money) + " • Your share: " +
                            receivableMoney(SharedBillPolicy.ownShare(linkedTransaction.amountMinor,money)) + " • Fees stay separate",
                            style = MaterialTheme.typography.bodySmall)
                    } else if (amount.isNotBlank()) Text("Enter a share greater than zero and less than the bill.",
                        color = MaterialTheme.colorScheme.error,style = MaterialTheme.typography.bodySmall)
                }
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
                        Text("Date: " + SimpleDateFormat("d MMM yyyy", locale).format(date))
                    }
                }
                OutlinedTextField(value = note, onValueChange = { if (it.length <= 250) note = it },
                    label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
                if (kind == PersonalReceivableStore.REPAID && existing != null)
                    Text("Outstanding: " + (existing.outstandingMinor?.let(::receivableMoney) ?: "Balance unavailable"),
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
                                memo = note,sharedBill = sharedBill,expectedTransaction = linkedTransaction
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

internal fun receivableMoney(minor: Long): String =
    "KES " + java.text.NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }.format(java.math.BigDecimal.valueOf(minor, 2))
