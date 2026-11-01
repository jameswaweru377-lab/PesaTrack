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
import com.kedevjimmy.pesatrack.data.local.StatementEvidenceReadModel
import com.kedevjimmy.pesatrack.domain.statements.StatementEvidenceCorrelationPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementMatchReviewPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
internal fun StatementMatchReviewPage(year: Int, labels: Map<String, String>, onBack: () -> Unit) {
    val context = LocalContext.current
    var review by remember(year) { mutableStateOf<StatementMatchReviewPolicy.Review?>(null) }
    var failed by remember(year) { mutableStateOf(false) }
    var attempt by remember(year) { mutableIntStateOf(0) }
    val ledgerRevision by LedgerUpdateBus.state.collectAsState()
    LaunchedEffect(year, attempt, ledgerRevision.revision) {
        failed = false
        review = null
        try {
            review = withContext(Dispatchers.IO) {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("Africa/Nairobi")).apply {
                    clear(); set(year, Calendar.JANUARY, 1, 0, 0, 0)
                }
                val start = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1)
                val end = calendar.timeInMillis
                val edge = 48L * 60L * 60L * 1000L
                val snapshot = StatementEvidenceReadModel(context).readWithActivityEvidence(maxOf(1L, start - edge), end + edge)
                StatementMatchReviewPolicy.project(
                    snapshot.statements.observations + snapshot.originalActivityMessages, snapshot.eventProof, start, end)
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
    }
    StatementMatchReviewContent(year, labels, review, failed, onBack) { attempt++ }
}

@Composable
internal fun StatementMatchReviewContent(
    year: Int, labels: Map<String, String>, review: StatementMatchReviewPolicy.Review?,
    failed: Boolean, onBack: () -> Unit, onRetry: () -> Unit
) {
    val p = HomeReferencePalette
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
                Text("These are suggestions based on matching evidence, not confirmed transfers. Nothing is merged, deleted or added to Activity.",
                    color = p.muted, style = MaterialTheme.typography.bodyMedium)
                Text("Only confirmed accounts are compared. Statements used in suggestions must be verified; limited statements are not included.",
                    color = p.muted, style = MaterialTheme.typography.bodySmall)
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
                }
            }
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
private fun MatchEvidenceLines(rows: List<StatementEvidenceCorrelationPolicy.Observation>, labels: Map<String, String>) {
    val p = HomeReferencePalette
    rows.forEach { row ->
        val source = when (row.source) {
            StatementEvidenceCorrelationPolicy.Source.ACTIVITY -> "Activity message"
            StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT -> "M-Pesa statement"
            StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT -> "Bank statement"
        }
        val direction = if (row.movementMinor < 0) "Money out" else "Money in"
        val reference = row.reference?.takeIf { it.isNotBlank() }?.let { " · Ref $it" }.orEmpty()
        Text("$source · ${labels[row.accountId] ?: "Account"} · $direction · ${statementDate(row.occurredAt)}$reference", color = p.muted,
            style = MaterialTheme.typography.bodySmall)
    }
}
