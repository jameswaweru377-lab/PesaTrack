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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Source-only evidence. No report totals, category bridge or Activity action. */
@Composable
internal fun StatementLimitedSourcePage(
    source: StatementBankArchiveStore.Source,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var rows by remember(source.id) {
        mutableStateOf<List<StatementBankArchiveStore.Leg>>(emptyList())
    }
    var failed by remember(source.id) { mutableStateOf(false) }
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
                    Text("Limited assurance", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text("${source.rows} locally reconciled rows",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            StatementInsightCard {
                Text("Not independently fully verified",
                    fontWeight = FontWeight.SemiBold)
                Text("The printed grand totals and adjacent running balances match. The statement has no printed opening balance or per-page totals. These rows are not included in verified bank reports, Activity or automatic matching.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        if (failed) item {
            Text("Could not load saved rows. No financial information was changed.")
        }
        items(rows, key = { "${it.sourceId}:${it.number}" }) { leg ->
            StatementInsightCard {
                Text(leg.description, fontWeight = FontWeight.SemiBold)
                Text("${statementDate(leg.occurredAt)} · Page ${leg.page}, row ${leg.row}",
                    style = MaterialTheme.typography.bodySmall)
                Text(if (leg.movementMinor > 0) "Money in ${statementMoney(leg.movementMinor)}"
                    else "Money out ${statementMoney(-leg.movementMinor)}")
                Text("Printed balance ${statementMoney(leg.runningBalanceMinor)}",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
