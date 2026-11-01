package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.StatementMatchDecisionStore
import com.kedevjimmy.pesatrack.domain.statements.StatementEvidenceCorrelationPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementMatchReviewPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun StatementMatchReviewPage(year: Int, labels: Map<String, String>, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { StatementMatchDecisionStore(context) }
    val scope = rememberCoroutineScope()
    var snapshot by remember(year) { mutableStateOf<StatementMatchDecisionStore.Snapshot?>(null) }
    var failed by remember(year) { mutableStateOf(false) }
    var attempt by remember(year) { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val ledgerRevision by LedgerUpdateBus.state.collectAsState()
    LaunchedEffect(year, attempt, ledgerRevision.revision) {
        failed = false
        snapshot = null
        try {
            snapshot = withContext(Dispatchers.IO) { store.read(year) }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
    }
    StatementMatchReviewContent(year, labels, snapshot?.review, failed, onBack, { attempt++ },
        snapshot = snapshot, busy = busy, actionError = actionError) { candidate, expected, decision, attested ->
        if (!busy) {
            busy = true; actionError = null
            scope.launch {
                try {
                    when (withContext(Dispatchers.IO) { store.decide(year, candidate.hash, candidate.kind, expected, decision, attested) }) {
                        StatementMatchDecisionStore.Result.SAVED -> Unit
                        StatementMatchDecisionStore.Result.STALE -> actionError = "These records changed. Review the refreshed suggestion."
                        StatementMatchDecisionStore.Result.CONFLICT -> actionError = "One record is already confirmed in another match. Undo that decision first."
                        StatementMatchDecisionStore.Result.ACCOUNT_CHECK_REQUIRED -> actionError = "Check that the original records belong to the accounts shown."
                    }
                    attempt++
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { actionError = "Could not save your review choice. Try again." }
                finally { busy = false }
            }
        }
    }
}

@Composable
internal fun StatementMatchReviewContent(
    year: Int, labels: Map<String, String>, review: StatementMatchReviewPolicy.Review?,
    failed: Boolean, onBack: () -> Unit, onRetry: () -> Unit,
    snapshot: StatementMatchDecisionStore.Snapshot? = null, busy: Boolean = false,
    actionError: String? = null,
    onDecision: (StatementMatchDecisionStore.Candidate, StatementMatchDecisionStore.Decision?, StatementMatchDecisionStore.Decision?, Boolean) -> Unit = { _, _, _, _ -> }
) {
    val p = HomeReferencePalette
    var confirming by remember { mutableStateOf<StatementMatchDecisionStore.Candidate?>(null) }
    confirming?.let { candidate ->
        var accountChecked by remember(candidate.hash) { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { confirming = null },
            title = { Text("Confirm this match?") },
            text = { Column {
                Text("This saves your review choice. It does not yet change Activity, balances, categories or report totals.")
                if (candidate.needsAccountAttestation) {
                    Spacer(Modifier.height(12.dp))
                    Text("Older messages and M-Pesa statements may not identify their original account number. Check the originals in Activity or Statements first.")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = accountChecked, onCheckedChange = { accountChecked = it })
                        Text("I checked that these records belong to the accounts shown.")
                    }
                }
            } },
            confirmButton = { TextButton(enabled = !candidate.needsAccountAttestation || accountChecked,
                onClick = { confirming = null; onDecision(candidate, null, StatementMatchDecisionStore.Decision.CONFIRMED, accountChecked) }) { Text("Confirm match") } },
            dismissButton = { TextButton(onClick = { confirming = null }) { Text("Cancel") } })
    }
    LazyColumn(Modifier.fillMaxSize().background(p.canvas).testTag("statement-match-review"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                Column {
                    Text("Review possible matches", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, color = p.ink)
                    Text("Statements and messages · $year", color = p.muted,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            StatementInsightCard {
                Text("Your records stay unchanged", color = p.ink, fontWeight = FontWeight.SemiBold)
                Text("Review these suggestions before confirming. Your choices do not yet change Activity, balances, categories or report totals.",
                    color = p.muted, style = MaterialTheme.typography.bodyMedium)
                Text("Only confirmed accounts are compared. Statements used in suggestions must be verified; limited statements are not included.",
                    color = p.muted, style = MaterialTheme.typography.bodySmall)
                Text("Older messages and M-Pesa statements may not identify your account number. Check the originals in Activity or Statements.",
                    color = p.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (actionError != null) item { Text(actionError, color = MaterialTheme.colorScheme.error) }
        if ((snapshot?.excludedBankSourceCount ?: 0) > 0) item {
            StatementInsightCard {
                Text("Some bank statements were not compared", color = p.ink, fontWeight = FontWeight.SemiBold)
                Text("The current account number could not be matched to the number checked at import. Your saved records are unchanged.",
                    color = p.muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (snapshot?.missingOwnershipConsent == true) item {
            StatementInsightCard {
                Text("Confirm which accounts are yours", fontWeight = FontWeight.SemiBold, color = p.ink)
                Text("Account ownership must be confirmed before saving a match decision.", color = p.muted)
                TransferOwnershipConsentAction()
            }
        }
        if (failed) item {
            StatementInsightCard {
                Text("Could not check matches", color = p.ink, fontWeight = FontWeight.SemiBold)
                Text("Try again or choose a different year in Statements. Your saved records are unchanged.", color = p.muted)
                TextButton(onClick = onRetry) { Text("Try again") }
            }
        } else if (review == null) item {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Checking saved evidence…", color = p.muted)
            }
        } else {
            if (review.transfers.isEmpty() && review.sameMovements.isEmpty()) item {
                StatementInsightCard {
                    Text("No strong matches found", color = p.ink, fontWeight = FontWeight.SemiBold)
                    Text("Some movements may have different references or missing evidence. This does not mean they are unrelated.",
                        color = p.muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (review.ambiguousCount > 0) item {
                StatementInsightCard {
                    Text("More evidence needed", color = p.ink, fontWeight = FontWeight.SemiBold)
                    Text("${review.ambiguousCount} records have conflicting or multiple possible matches. They have not been linked.",
                        color = p.muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (review.transfers.isNotEmpty()) item {
                Text("Possible transfers between your accounts", color = p.ink,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(review.transfers) { transfer ->
                val outgoing = transfer.outgoing.first()
                val incoming = transfer.incoming.first()
                StatementInsightCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CompareArrows, null, tint = p.purple)
                        Spacer(Modifier.width(10.dp))
                        Text(matchReviewMoney(-outgoing.movementMinor, outgoing.currency), color = p.ink,
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Text("${labels[outgoing.accountId] ?: "Account"} → ${labels[incoming.accountId] ?: "Account"}",
                        color = p.ink, fontWeight = FontWeight.SemiBold)
                    Text("Same reference and amount; money out on one account and in on the other.",
                        color = p.muted, style = MaterialTheme.typography.bodySmall)
                    MatchEvidenceLines(transfer.outgoing + transfer.incoming, labels)
                    MatchDecisionActions(snapshot, StatementMatchDecisionStore.Kind.TRANSFER,
                        transfer.outgoing + transfer.incoming, busy, onConfirm = { confirming = it }, onDecision = onDecision)
                }
            }
            if (review.sameMovements.isNotEmpty()) item {
                Text("Possible copies of one movement", color = p.ink,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(review.sameMovements) { rows ->
                val row = rows.first()
                StatementInsightCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Link, null, tint = p.purple)
                        Spacer(Modifier.width(10.dp))
                        Text(matchReviewMoney(kotlin.math.abs(row.movementMinor), row.currency), color = p.ink,
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Text(labels[row.accountId] ?: "Account", color = p.ink, fontWeight = FontWeight.SemiBold)
                    Text("Same reference, account, direction and amount across these records.",
                        color = p.muted, style = MaterialTheme.typography.bodySmall)
                    MatchEvidenceLines(rows, labels)
                    MatchDecisionActions(snapshot, StatementMatchDecisionStore.Kind.SAME_MOVEMENT,
                        rows, busy, onConfirm = { confirming = it }, onDecision = onDecision)
                }
            }
            val possibleTransfers = snapshot?.possibleTransfers.orEmpty()
            if (possibleTransfers.isNotEmpty()) item {
                Text("Other movements to check", color = p.ink,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("These are weaker suggestions and may be unrelated. Check the originals before deciding.",
                    color = p.muted, style = MaterialTheme.typography.bodyMedium)
            }
            items(possibleTransfers) { possible ->
                StatementInsightCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CompareArrows, null, tint = p.purple)
                        Spacer(Modifier.width(10.dp))
                        Text(matchReviewMoney(-possible.outgoing.movementMinor, possible.outgoing.currency),
                            color = p.ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Text("${labels[possible.outgoing.accountId] ?: "Account"} → ${labels[possible.incoming.accountId] ?: "Account"}",
                        color = p.ink, fontWeight = FontWeight.SemiBold)
                    Text("Same amount and nearby dates. This alone does not prove a transfer.",
                        color = p.muted, style = MaterialTheme.typography.bodySmall)
                    MatchEvidenceLines(listOf(possible.outgoing, possible.incoming), labels)
                }
            }
            val mentions = snapshot?.accountMentions.orEmpty()
            if (mentions.isNotEmpty()) item {
                Text("Account mentions to check", color = p.ink,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("A statement includes another saved account number. Check the original before drawing a conclusion.",
                    color = p.muted, style = MaterialTheme.typography.bodyMedium)
            }
            items(mentions) { hint ->
                StatementInsightCard {
                    Text(matchReviewMoney(kotlin.math.abs(hint.row.movementMinor), hint.row.currency),
                        color = p.ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Mentions ${labels[hint.mentionedAccountId] ?: "another account"}",
                        color = p.ink, fontWeight = FontWeight.SemiBold)
                    Text("A number alone does not identify the recipient or prove a transfer.",
                        color = p.muted, style = MaterialTheme.typography.bodySmall)
                    MatchEvidenceLines(listOf(hint.row), labels, includeReferences = false)
                }
            }
        }
    }
}

@Composable
private fun MatchDecisionActions(
    snapshot: StatementMatchDecisionStore.Snapshot?, kind: StatementMatchDecisionStore.Kind,
    rows: List<StatementEvidenceCorrelationPolicy.Observation>, busy: Boolean,
    onConfirm: (StatementMatchDecisionStore.Candidate) -> Unit,
    onDecision: (StatementMatchDecisionStore.Candidate, StatementMatchDecisionStore.Decision?, StatementMatchDecisionStore.Decision?, Boolean) -> Unit
) {
    val ids = rows.map { it.id }.toSet()
    val candidate = snapshot?.candidates?.singleOrNull { it.kind == kind && it.rows.map { row -> row.id }.toSet() == ids }
        ?: return
    if (candidate.needsAccountAttestation) Text("Check the original message or M-Pesa statement belongs to the account shown.",
        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    when (snapshot.decisions[candidate.hash]) {
        StatementMatchDecisionStore.Decision.CONFIRMED -> {
            Text("You confirmed this match", fontWeight = FontWeight.SemiBold)
            TextButton(enabled = !busy, onClick = { onDecision(candidate, StatementMatchDecisionStore.Decision.CONFIRMED, null, false) }) { Text("Undo") }
        }
        StatementMatchDecisionStore.Decision.NOT_SAME -> {
            Text("You marked these as different", fontWeight = FontWeight.SemiBold)
            TextButton(enabled = !busy, onClick = { onDecision(candidate, StatementMatchDecisionStore.Decision.NOT_SAME, null, false) }) { Text("Undo") }
        }
        null -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !busy, onClick = { onConfirm(candidate) }) { Text("Confirm") }
            OutlinedButton(enabled = !busy, onClick = { onDecision(candidate, null, StatementMatchDecisionStore.Decision.NOT_SAME, false) }) { Text("Not the same") }
        }
    }
}

private fun matchReviewMoney(minor: Long, currency: String): String {
    val amount = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }.format(BigDecimal.valueOf(minor, 2))
    return "${currency.uppercase(Locale.ROOT)} $amount"
}

@Composable
private fun MatchEvidenceLines(rows: List<StatementEvidenceCorrelationPolicy.Observation>, labels: Map<String, String>,
                               includeReferences: Boolean = true) {
    val p = HomeReferencePalette
    rows.forEach { row ->
        val source = when (row.source) {
            StatementEvidenceCorrelationPolicy.Source.ACTIVITY -> "Activity message"
            StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT -> "M-Pesa statement"
            StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT -> "Bank statement"
        }
        val direction = if (row.movementMinor < 0) "Money out" else "Money in"
        val reference = row.reference?.takeIf { includeReferences && it.isNotBlank() }?.let { " · Ref $it" }.orEmpty()
        Text("$source · ${labels[row.accountId] ?: "Account"} · $direction · ${statementDate(row.occurredAt)}$reference", color = p.muted,
            style = MaterialTheme.typography.bodySmall)
    }
}
