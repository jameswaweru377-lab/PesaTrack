package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.rules.SmartNoticeKind
import com.kedevjimmy.pesatrack.planning.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

/** Taps read current local evidence. Financial writes remain in the existing confirmation dialog. */
@Composable
fun SmartNoticeReviewScreen(padding: PaddingValues, key: String, action: String,
    onBack: () -> Unit, onTransaction: (Long) -> Unit, expectedRevision: String = "") {
    val context = LocalContext.current
    val revision by LedgerUpdateBus.state.collectAsState()
    var notice by remember(key) { mutableStateOf<SmartNotice?>(null) }
    var row by remember(key) { mutableStateOf<StoredTransaction?>(null) }
    var people by remember(key) { mutableStateOf<List<ReceivablePerson>>(emptyList()) }
    var loading by remember(key) { mutableStateOf(true) }
    var failed by remember(key) { mutableStateOf(false) }
    var entry by remember(key,action) { mutableStateOf(false) }
    var handled by remember(key) { mutableStateOf(false) }
    var decided by remember(key) { mutableStateOf(false) }
    var undoAvailable by remember(key) { mutableStateOf(false) }
    var financialSaved by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key,revision) {
        loading = true; failed = false
        try {
            val result = withContext(Dispatchers.IO) {
                synchronized(CanonicalStoreLock.monitor) {
                    val current = SmartNoticeCoordinator.resolve(context,key,expectedRevision)
                    Triple(current,current?.transactionId?.let { TransactionStore.get(context).getById(it) },
                        if (current?.candidate?.kind == SmartNoticeKind.PAYMENT) PersonalReceivableStore(context).people() else emptyList())
                }
            }
            notice = result.first; row = result.second; people = result.third
            if (!handled && action == "entry" && notice?.candidate?.kind == SmartNoticeKind.PAYMENT) {
                entry = !notice!!.incoming || notice!!.repaymentAllowed; handled = true
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true; notice = null; row = null }
        finally { loading = false }
    }
    val current = notice
    if (loading) { Column(Modifier.padding(padding).padding(20.dp)) { LinearProgressIndicator() }; return }
    if (current == null || decided) {
        Column(Modifier.padding(padding).padding(20.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (failed) "This review could not be loaded. Try opening it again."
                else if (financialSaved) "Saved. This payment no longer needs this suggestion."
                else if (decided) "Suggestion dismissed. Your money records have not changed."
                else "This item no longer needs this review.",Modifier.testTag("smart-notice-resolved"))
            if (undoAvailable) TextButton(onClick = {
                SmartNoticePreferences(context).undoDismissal(key); decided = false; undoAvailable = false
            }) { Text("Undo choice") }
            TextButton(onClick = onBack) { Text("Back") }
        }
        return
    }
    when (current.candidate.kind) {
        SmartNoticeKind.ACCOUNT -> ActivityScreen(padding,onTransactionClick = onTransaction,
            openNeedsAttention = true,initialAccountReviewKey = current.entityKey)
        SmartNoticeKind.ACTIVITY -> ActivityScreen(padding,onTransactionClick = onTransaction,openNeedsAttention = true)
        SmartNoticeKind.DISCOVERY -> PersonalReceivablesScreen(padding,onLiabilities = onBack)
        SmartNoticeKind.PAYMENT -> {
            val payment = row ?: return
            val person = people.firstOrNull { it.id == current.personId } ?: return
            Column(Modifier.padding(padding).padding(20.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Review They Owe Me payment",style = MaterialTheme.typography.titleLarge)
                Text(person.name)
                Text((if (current.incoming) "Received: " else "Sent: ") + receivableMoney(payment.amountMinor))
                Text("Matching phone number and recorded They Owe Me history. Confirm the purpose; a match does not prove a loan or repayment.")
                TextButton(onClick = { onTransaction(payment.id) }) { Text("View original payment") }
                if (!current.incoming || current.repaymentAllowed) Button(onClick = { entry = true }) {
                    Text(if (current.incoming) "Review repayment" else "Add as loan")
                }
                if (current.incoming && !current.repaymentAllowed) Text("No repayment is preselected. Review the person's outstanding balance and the original payment.")
                fun dismiss() {
                    SmartNoticePreferences(context).dismissed(key,current.candidate.revision)
                    SmartNoticeCoordinator.cancelIfPosted(context,key); decided = true; undoAvailable = true
                }
                OutlinedButton(onClick = { dismiss() }) { Text(if (action == "gift") "Confirm gift / unrelated to debt" else "Gift") }
                OutlinedButton(onClick = { dismiss() }) { Text(if (current.incoming) "Other / Not repayment" else "Not a loan") }
                TextButton(onClick = onBack) { Text("Leave undecided") }
            }
            if (entry) ReceivableEntryDialog(people,person.id,
                if (current.incoming) PersonalReceivableStore.REPAID else PersonalReceivableStore.LENT,payment,
                onDismiss = { entry = false },onSaved = {
                    entry = false; decided = true; undoAvailable = false
                    financialSaved = true
                    SmartNoticeCoordinator.cancelIfPosted(context,key)
                })
        }
    }
}

@Composable
internal fun SmartDiscoveryCard(onOpen: () -> Unit) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val prefs = SmartNoticePreferences(context)
        if (prefs.discoveryPending()) {
            try {
                val used = withContext(Dispatchers.IO) { PersonalReceivableStore(context).people().isNotEmpty() }
                visible = !used || prefs.discoveryRequestedAgain()
                prefs.completeDiscovery()
                withContext(Dispatchers.IO) { runCatching { SmartNoticeCoordinator.check(context,allowPost = false) } }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { visible = false }
        }
    }
    if (!visible) return
    Card(Modifier.fillMaxWidth().testTag("they-owe-me-discovery")) {
        Column(Modifier.padding(16.dp),verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Track money owed to you",style = MaterialTheme.typography.titleMedium)
            Text("Lent money to someone? They Owe Me helps you review payments and record loans, including when you start fresh.")
            TextButton(onClick = onOpen) { Text("Explore They Owe Me") }
            TextButton(onClick = { visible = false }) { Text("Don't show again") }
        }
    }
}

/** A once-per-evidence inline fallback on the original payment, including permission denial. */
@Composable
internal fun SmartPaymentReviewHint(row: StoredTransaction) {
    val context = LocalContext.current
    val revision by LedgerUpdateBus.state.collectAsState()
    var notice by remember(row.id) { mutableStateOf<SmartNotice?>(null) }
    var open by remember(row.id) { mutableStateOf(false) }
    LaunchedEffect(row.id,revision) {
        try {
            val priorRevision = notice?.candidate?.revision
            val current = withContext(Dispatchers.IO) {
                synchronized(CanonicalStoreLock.monitor) {
                    val prefs = SmartNoticePreferences(context)
                    val key = "payment:" + SmartNoticePreferences.hash(row.fingerprint)
                    SmartNoticeCoordinator.resolve(context,key)?.takeIf {
                        it.transactionId == row.id && it.candidate.enabled &&
                            (priorRevision == it.candidate.revision || prefs.lastHintRevision(key) != it.candidate.revision)
                    }
                }
            }
            notice = current
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { notice = null }
    }
    val current = notice ?: return
    LaunchedEffect(current.candidate.revision) {
        SmartNoticePreferences(context).hintSeen(current.candidate.key,current.candidate.revision)
    }
    Card(Modifier.fillMaxWidth().testTag("they-owe-me-payment-hint")) {
        Column(Modifier.padding(14.dp),verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Review this They Owe Me payment",style = MaterialTheme.typography.titleMedium)
            Text("Matching phone number and recorded lending history. A match does not prove a loan or repayment.")
            TextButton(onClick = { open = true }) { Text("Review options") }
            TextButton(onClick = {
                SmartNoticePreferences(context).dismissed(current.candidate.key,current.candidate.revision)
                SmartNoticeCoordinator.cancelIfPosted(context,current.candidate.key); notice = null
            }) { Text("Don't ask about this payment again") }
        }
    }
    if (open) Dialog(onDismissRequest = { open = false },properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            SmartNoticeReviewScreen(PaddingValues(),current.candidate.key,"review",onBack = { open = false; notice = null },
                onTransaction = { open = false; notice = null },expectedRevision = current.candidate.revision)
        }
    }
}
