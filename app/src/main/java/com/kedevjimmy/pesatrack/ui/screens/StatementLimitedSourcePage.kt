package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.StatementBankArchiveStore
import com.kedevjimmy.pesatrack.domain.statements.StatementGrandTotalReconciledPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementLimitedDashboardPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One PDF's source-only reconciliation; never feeds verified reports or Activity. */
@Composable
internal fun StatementLimitedSourcePage(
    source: StatementBankArchiveStore.Source,
    onBack: () -> Unit,
    accountLabel: String = "Bank statement",
    onImport: (() -> Unit)? = null
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var rows by remember(source.id) {
        mutableStateOf<List<StatementBankArchiveStore.Leg>?>(null)
    }
    var failed by remember(source.id) { mutableStateOf(false) }
    val dashboard = remember(source, rows) {
        rows?.let { saved ->
            StatementLimitedDashboardPolicy.project(
                sourceId = source.id, expectedRows = source.rows,
                expectedMoneyInMinor = source.moneyInMinor,
                expectedMoneyOutMinor = source.moneyOutMinor,
                rows = saved.map { leg ->
                    StatementGrandTotalReconciledPolicy.Row(
                        leg.sourceId, leg.number, leg.occurredAt, leg.movementMinor
                    )
                }
            )
        }
    }
    LaunchedEffect(source.id) {
        try {
            rows = withContext(Dispatchers.IO) {
                StatementBankArchiveStore(context).limitedLegsFor(source.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = true
        }
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, "Back")
                }
                Column {
                    Text(accountLabel, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text("Statement insights · ${source.rows} locally reconciled rows",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            StatementInsightCard {
                Text("Not a fully verified bank report",
                    fontWeight = FontWeight.SemiBold)
                Text("The printed grand totals and adjacent running balances match. The statement has no printed opening balance or per-page totals. These rows are not included in verified bank reports, Activity or automatic matching.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        if (failed) item {
            Text("Could not load saved rows. No financial information was changed.")
        }
        if (!failed && rows == null) item {
            Text("Checking saved source rows before showing totals…")
        }
        if (!failed && rows != null && dashboard == null) item {
            Text("Saved source rows did not reconcile with the original grand totals. Cash totals are withheld; no financial information was changed.")
        }
        if (dashboard != null) item {
            val savedRows = rows.orEmpty().associateBy { it.number }
            StatementLimitedDashboardCards(dashboard.report,
                dashboard.largestOutflowRowNumbers.mapNotNull { savedRows[it] })
        }
        if (onImport != null) item {
            OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Import another statement")
            }
        }
        if (rows != null) item {
            Text("Statement transactions", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
        }
        items(rows.orEmpty(), key = { "${it.sourceId}:${it.number}" }) { leg ->
            StatementInsightCard {
                Text(leg.description, fontWeight = FontWeight.SemiBold)
                Text("${statementDate(leg.occurredAt)} · Page ${leg.page}, row ${leg.row}",
                    style = MaterialTheme.typography.bodySmall)
                Text(if (leg.movementMinor > 0) "Money in ${statementMoney(leg.movementMinor)}"
                    else if (leg.movementMinor == Long.MIN_VALUE) "Invalid saved cash movement"
                    else "Money out ${statementMoney(-leg.movementMinor)}")
                Text("Printed balance ${statementMoney(leg.runningBalanceMinor)}",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
