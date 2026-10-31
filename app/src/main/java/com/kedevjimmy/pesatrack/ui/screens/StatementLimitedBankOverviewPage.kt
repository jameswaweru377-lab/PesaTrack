package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.StatementBankArchiveStore

/** Saved grand-total reconciled evidence stays source-specific, outside verified reports. */
@Composable
internal fun StatementLimitedBankOverviewPage(
    accountLabel: String,
    sources: List<StatementBankArchiveStore.Source>,
    onBack: () -> Unit,
    onImport: () -> Unit
) {
    var selected by remember(accountLabel) { mutableStateOf<StatementBankArchiveStore.Source?>(null) }
    selected?.let { source ->
        StatementLimitedSourcePage(source) { selected = null }
        return
    }
    BackHandler(onBack = onBack)
    val p = HomeReferencePalette
    LazyColumn(Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
                Column(Modifier.weight(1f)) {
                    Text(accountLabel, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = p.ink)
                    Text("Saved bank statements", color = p.muted)
                }
            }
        }
        item {
            StatementInsightCard {
                Text("Grand-total reconciled · source only", fontWeight = FontWeight.SemiBold, color = p.ink)
                Text("The printed grand totals and adjacent balances matched at import. No independently printed opening balance or page totals were available. Each PDF is shown separately to avoid overlapping totals; these rows remain outside fully verified reports and Activity.",
                    color = p.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        items(sources, key = { it.id }) { source ->
            StatementGrandTotalReconciledSourceCard(source) { selected = source }
        }
        item { Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Text("Import another statement")
        } }
    }
}
