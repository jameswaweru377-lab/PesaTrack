package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.TransferReconciliationStore
import com.kedevjimmy.pesatrack.domain.statements.StatementEvidenceCorrelationPolicy
import com.kedevjimmy.pesatrack.domain.statements.TransferDecisionPolicy
import com.kedevjimmy.pesatrack.domain.statements.TransferDecisionPolicy.Decision
import com.kedevjimmy.pesatrack.domain.statements.TransferReconciliationPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Review workspace; existing duplicate/copy review remains accessible without changing navigation. */
@Composable
internal fun TransferReconciliationWorkspace(year: Int, labels: Map<String, String>,
                                             onOpenActivity: (Long) -> Unit = {}, onBack: () -> Unit) {
    var otherMatches by remember(year) { mutableStateOf(false) }
    BackHandler(otherMatches) { otherMatches = false }
    if (otherMatches) {
        StatementMatchReviewPage(year, labels, onOpenActivity) { otherMatches = false }
    } else {
        TransferReconciliationPage(year, labels, onOpenActivity, onBack) { otherMatches = true }
    }
}

@Composable
private fun TransferReconciliationPage(year: Int, labels: Map<String, String>, onOpenActivity: (Long) -> Unit,
                                       onBack: () -> Unit, onOtherMatches: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { TransferReconciliationStore(context) }
    val scope = rememberCoroutineScope()
    var snapshot by remember(year) { mutableStateOf<TransferReconciliationStore.Snapshot?>(null) }
    var failed by remember(year) { mutableStateOf(false) }
    var attempt by remember(year) { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var error by remember(year) { mutableStateOf<String?>(null) }
    var confirming by remember(year) { mutableStateOf<TransferReconciliationPolicy.Pair?>(null) }
    val revision by LedgerUpdateBus.state.collectAsState()
    LaunchedEffect(year, attempt, revision.revision) {
        snapshot = null; failed = false; confirming = null
        try { snapshot = withContext(Dispatchers.IO) { store.read(year) } }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
    }
    val decide: (TransferReconciliationPolicy.Pair, Decision?, Boolean) -> Unit = { pair, decision, attested ->
        val expected = snapshot?.decisions?.get(pair.fingerprint)
        if (!busy && snapshot != null) {
            busy = true; error = null
            scope.launch {
                try {
                    error = when (withContext(Dispatchers.IO) { store.decide(year, pair.fingerprint, expected, decision, attested) }) {
                        TransferDecisionPolicy.Result.SAVED -> null
                        TransferDecisionPolicy.Result.STALE -> "The evidence or review choice changed. Review the refreshed records."
                        TransferDecisionPolicy.Result.CONFLICT -> "A movement is confirmed in another match. Undo that choice in Other matches first."
                        TransferDecisionPolicy.Result.ACCOUNT_CHECK_REQUIRED -> "Check the original records and confirm both account identities."
                    }
                    attempt++
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { error = "Could not save your review. Please retry." }
                finally { busy = false }
            }
        }
    }
    confirming?.let { pair ->
        TransferConfirmation(pair, labels, onDismiss = { confirming = null }) {
            confirming = null; decide(pair, Decision.CONFIRMED, true)
        }
    }
    TransferReconciliationContent(year, labels, snapshot, failed, busy, error, onBack, onOtherMatches,
        onRetry = { attempt++ }, onOpenActivity = onOpenActivity,
        onConfirm = { confirming = it }, onReject = { decide(it, Decision.NOT_SAME, false) },
        onUndo = { decide(it, null, false) })
}

@Composable
private fun TransferConfirmation(pair: TransferReconciliationPolicy.Pair, labels: Map<String, String>,
                                 onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var checked by remember(pair.fingerprint) { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Confirm own-account transfer?") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${labels[pair.outgoing.first().accountId] ?: "Source account"} → ${labels[pair.incoming.first().accountId] ?: "Destination account"}")
            Text("Check the original account identities and that these are the two sides of the same transfer. A similar amount or time is not enough. Older messages may not preserve their original account number.")
            Text("This saves a review decision. Balances, income, expenses and categories remain unchanged. Fees are not inferred.")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked, { checked = it })
                Text("I checked both originals, own both accounts, and confirm this transfer.")
            }
        } }, confirmButton = { TextButton(onClick = onConfirm, enabled = checked) { Text("Confirm transfer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun TransferReconciliationContent(year: Int, labels: Map<String, String>, snapshot: TransferReconciliationStore.Snapshot?,
                                          failed: Boolean, busy: Boolean, error: String?, onBack: () -> Unit,
                                          onOtherMatches: () -> Unit, onRetry: () -> Unit, onOpenActivity: (Long) -> Unit,
                                          onConfirm: (TransferReconciliationPolicy.Pair) -> Unit,
                                          onReject: (TransferReconciliationPolicy.Pair) -> Unit,
                                          onUndo: (TransferReconciliationPolicy.Pair) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(HomeReferencePalette.canvas).testTag("transfer-reconciliation"),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                Column { Text("Transfer reconciliation", style = MaterialTheme.typography.headlineSmall); Text("$year · Review evidence") }
            }
            Text("Connect movements between your accounts. Original records and financial totals stay unchanged.")
            TextButton(onClick = onOtherMatches, enabled = !busy) { Text("Other matches and duplicate review") }
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        when {
            failed -> item {
                Text("Could not load reconciliation. Large evidence ranges may exceed the safe review limit. No result is being shown as complete.")
                TextButton(onClick = onRetry) { Text("Retry") }
            }
            snapshot == null -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            else -> {
                item { TransferCoverage(snapshot) }
                if (snapshot.reconciliation.pairs.isEmpty() && snapshot.reconciliation.unmatchedRoutes.isEmpty()) {
                    item { Text("No transfer candidates in the eligible evidence. This does not prove that no transfers occurred. Check account ownership, statement coverage and Other matches.") }
                }
                items(snapshot.reconciliation.pairs, key = { it.fingerprint }) { pair ->
                    TransferPairCard(pair, labels, snapshot.decisions[pair.fingerprint],
                        pair.rows.any { it.id in snapshot.legacyConfirmedIds }, busy, onOpenActivity,
                        onConfirm = { onConfirm(pair) }, onReject = { onReject(pair) }, onUndo = { onUndo(pair) })
                }
                items(snapshot.reconciliation.unmatchedRoutes, key = { "route:${it.row.id}" }) { route ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Account route · counterpart unresolved", style = MaterialTheme.typography.titleMedium)
                            Text("${labels[route.row.accountId] ?: "Account"} · ${money(route.row.currency, kotlin.math.abs(route.row.movementMinor))}")
                            Text("The narration points ${if (route.row.movementMinor < 0) "to" else "from"} ${labels[route.peerAccountId] ?: "another owned account"}.")
                            Text("No unique eligible counterpart is linked. It may be outside the available coverage or need review; this is not evidence of a failed transfer.")
                            EvidenceLine(route.row, onOpenActivity)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferCoverage(snapshot: TransferReconciliationStore.Snapshot) {
    val result = snapshot.reconciliation
    val confirmed = result.pairs.count { snapshot.decisions[it.fingerprint] == Decision.CONFIRMED }
    Text("${result.pairs.size} candidate pairs · $confirmed confirmed here · ${result.unmatchedRoutes.size} unresolved routes",
        style = MaterialTheme.typography.titleSmall)
    Text("Reference evidence and exact account routes support review. Time only limits the search. No fees or missing movements are invented.")
    Text("Coverage: verified statements and eligible original messages. Limited-assurance imports, unsupported routes and protected financial roles may be absent.",
        style = MaterialTheme.typography.bodySmall)
    if (result.ambiguousObservationCount > 0 || result.withheldObservationCount > 0 || snapshot.excludedBankSources > 0) {
        Text("${result.ambiguousObservationCount} ambiguous observations · ${result.withheldObservationCount} withheld observations · ${snapshot.excludedBankSources} bank sources with unverified account identity",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TransferPairCard(pair: TransferReconciliationPolicy.Pair, labels: Map<String, String>, decision: Decision?,
                             legacyConfirmed: Boolean, busy: Boolean, onOpenActivity: (Long) -> Unit,
                             onConfirm: () -> Unit, onReject: () -> Unit, onUndo: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${labels[pair.outgoing.first().accountId] ?: "Source account"} → ${labels[pair.incoming.first().accountId] ?: "Destination account"}",
                style = MaterialTheme.typography.titleMedium)
            Text(money(pair.currency, pair.principalMinor), style = MaterialTheme.typography.titleLarge)
            Text(when (decision) { Decision.CONFIRMED -> "Confirmed by you · review only"; Decision.NOT_SAME -> "Rejected by you"; null -> "Needs review" })
            Text(when (pair.basis) {
                TransferReconciliationPolicy.Basis.SHARED_REFERENCE -> "Shared reference, opposite movements and matching principal."
                TransferReconciliationPolicy.Basis.ACCOUNT_ROUTE -> "Exact directional account route and matching principal. No shared reference establishes the link."
                TransferReconciliationPolicy.Basis.REFERENCE_AND_ROUTE -> "Shared reference and exact directional account route support this pair."
            })
            Text("${pair.outgoing.size} source observations · ${pair.incoming.size} destination observations · fees not reconciled",
                style = MaterialTheme.typography.bodySmall)
            pair.rows.forEach { EvidenceLine(it, onOpenActivity) }
            if (legacyConfirmed) Text("A movement is already confirmed in Other matches. Review that decision before confirming here.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (decision == null) {
                    TextButton(onClick = onConfirm, enabled = !busy && !legacyConfirmed) { Text("Review transfer") }
                    TextButton(onClick = onReject, enabled = !busy) { Text("Not the same") }
                } else TextButton(onClick = onUndo, enabled = !busy) { Text("Undo review") }
            }
        }
    }
}

@Composable
private fun EvidenceLine(row: StatementEvidenceCorrelationPolicy.Observation, onOpenActivity: (Long) -> Unit) {
    val formatted = remember(row.occurredAt, row.source) {
        // Legacy statement evidence does not preserve timestamp precision.
        val pattern = if (row.source == StatementEvidenceCorrelationPolicy.Source.ACTIVITY)
            "dd MMM yyyy HH:mm" else "dd MMM yyyy"
        SimpleDateFormat(pattern, Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Africa/Nairobi")
        }.format(Date(row.occurredAt))
    }
    val source = when (row.source) {
        StatementEvidenceCorrelationPolicy.Source.ACTIVITY -> "Activity message"
        StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT -> "M-Pesa statement"
        StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT -> "Bank statement"
    }
    Text("$source · recorded $formatted EAT · ${if (row.movementMinor < 0) "debit" else "credit"}", style = MaterialTheme.typography.bodySmall)
    if (row.source == StatementEvidenceCorrelationPolicy.Source.ACTIVITY) {
        row.id.removePrefix("activity:").toLongOrNull()?.let { id ->
            TextButton(onClick = { onOpenActivity(id) }) { Text("Open Activity record") }
        }
    }
}

private fun money(currency: String, minor: Long): String = "$currency ${BigDecimal.valueOf(minor, 2).toPlainString()}"
