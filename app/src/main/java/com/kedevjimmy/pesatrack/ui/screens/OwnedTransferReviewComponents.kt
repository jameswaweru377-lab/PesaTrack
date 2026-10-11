package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.evidence.*
import com.kedevjimmy.pesatrack.domain.identity.SelfNameEvidenceLevel
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.math.BigDecimal

@Composable
internal fun OwnedTransferReviewSection() {
    val context = LocalContext.current
    val store = remember { OwnedTransferReviewStore(context) }
    val scope = rememberCoroutineScope()
    val updates by LedgerUpdateBus.state.collectAsState()
    var candidates by remember { mutableStateOf<List<TransferReviewCandidate>>(emptyList()) }
    var names by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(updates.revision) {
        try {
            val loaded = withContext(Dispatchers.IO) {
                store.pendingCandidates() to AccountStore.get(context).listAccounts().associate { it.id to it.name }
            }
            candidates = loaded.first; names = loaded.second
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { candidates = emptyList(); error = "Could not load transfer questions." }
    }
    if (candidates.isEmpty() && error == null) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Possible bank to M-Pesa transfers", style = MaterialTheme.typography.titleMedium)
        Text("Check both observations. Names and numbers are hints; they do not prove a transfer.", style = MaterialTheme.typography.bodySmall)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        candidates.forEach { candidate ->
            OwnedTransferReviewCard(candidate, names[candidate.source.accountId].orEmpty(),
                names[candidate.destination.accountId].orEmpty(), busy) { decision ->
                if (!busy) {
                    busy = true; error = null
                    scope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) { store.decide(candidate, decision) }
                            if (result == TransferDecisionResult.STALE) error = "This transfer changed. Check the refreshed details before deciding."
                            candidates = withContext(Dispatchers.IO) { store.pendingCandidates() }
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = "Could not save the transfer decision. Please try again." }
                        finally { busy = false }
                    }
                }
            }
        }
    }
}

@Composable
internal fun OwnedTransferReviewCard(
    candidate: TransferReviewCandidate,
    sourceName: String,
    destinationName: String,
    busy: Boolean,
    onDecision: (TransferPairDecision) -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    PesaCard {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$sourceName → $destinationName", style = MaterialTheme.typography.titleMedium)
            Text("KES ${BigDecimal.valueOf(candidate.source.amountMinor, 2).toPlainString()}")
            Text("Bank fee: KES ${BigDecimal.valueOf(candidate.source.feeMinor, 2).toPlainString()}", style = MaterialTheme.typography.bodySmall)
            Text("Bank reference: ${candidate.source.primaryReference.orEmpty()}\nM-Pesa reference: ${candidate.destination.primaryReference.orEmpty()}", style = MaterialTheme.typography.bodySmall)
            val date = SimpleDateFormat("dd MMM yyyy HH:mm", locale)
            Text("Bank: ${date.format(Date(candidate.source.epochMs))}\nM-Pesa: ${date.format(Date(candidate.destination.epochMs))}", style = MaterialTheme.typography.bodySmall)
            Text(when (candidate.nameEvidence) {
                SelfNameEvidenceLevel.EXACT_THREE_NAMES -> "Three distinct names match your profile in order."
                SelfNameEvidenceLevel.TWO_OF_THREE_REVIEW -> "Two names match your profile. Your confirmation is required."
                SelfNameEvidenceLevel.NO_MATCH -> "The full phone number matches your confirmed M-Pesa account."
            }, style = MaterialTheme.typography.bodySmall)
            Text("Both accounts have your ownership confirmation. The amounts and transaction reference correlate. Confirm only if these are the two sides of your transfer.", style = MaterialTheme.typography.bodySmall)
            // Stacked controls remain legible with large fonts.
            Button(onClick = { onDecision(TransferPairDecision.CONFIRMED) }, enabled = !busy,
                modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Saving…" else "Confirm transfer") }
            OutlinedButton(onClick = { onDecision(TransferPairDecision.REJECTED) }, enabled = !busy,
                modifier = Modifier.fillMaxWidth()) { Text("Reject match") }
            TextButton(onClick = { onDecision(TransferPairDecision.DEFERRED) }, enabled = !busy) { Text("Not now") }
        }
    }
}

@Composable
internal fun TransferOwnershipConsentAction() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    var accounts by remember { mutableStateOf<List<MoneyAccount>>(emptyList()) }
    var confirmed by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(open) {
        if (open) {
            try {
                withContext(Dispatchers.IO) {
                    AccountStore.get(context).listAccounts().filter { it.type == AccountType.BANK || it.id == AccountStore.DEFAULT_MPESA_ID } to
                        AccountOwnershipConsentStore(context).confirmedRevisions()
                }.let { accounts = it.first; confirmed = it.second }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Could not load accounts." }
        }
    }
    TextButton(onClick = { open = true }) { Text("Confirm accounts for transfer matching") }
    if (open) AlertDialog(
        onDismissRequest = { if (!busy) open = false },
        title = { Text("Your accounts for transfer matching") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("Older account records need an explicit ownership choice for this new matching rule. This does not change their balances or history.") }
                error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
                items(accounts.size, key = { accounts[it].id }) { index ->
                    val account = accounts[index]
                    Column {
                        Text(account.name, style = MaterialTheme.typography.titleSmall)
                        Text(if (account.id in confirmed) "Ownership confirmed for matching" else "Not confirmed for matching", style = MaterialTheme.typography.bodySmall)
                        fun save(owned: Boolean) {
                            if (busy) return
                            busy = true; error = null
                            scope.launch {
                                try {
                                    confirmed = withContext(Dispatchers.IO) {
                                        val store = AccountOwnershipConsentStore(context)
                                        store.recordConsent(account.id, owned)
                                        LedgerUpdateBus.notifyChanged()
                                        store.confirmedRevisions()
                                    }
                                } catch (cancelled: CancellationException) { throw cancelled }
                                catch (_: Exception) { error = "Could not save account confirmation." }
                                finally { busy = false }
                            }
                        }
                        if (account.id !in confirmed) TextButton(onClick = { save(true) }, enabled = !busy) { Text("This is my account") }
                        else TextButton(onClick = { save(false) }, enabled = !busy) { Text("Don't use for transfer matching") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { open = false }, enabled = !busy) { Text("Done") } }
    )
}
