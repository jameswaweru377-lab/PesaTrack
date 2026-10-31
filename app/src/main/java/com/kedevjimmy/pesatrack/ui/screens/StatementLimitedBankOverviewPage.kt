package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.StatementBankArchiveStore

/** Saved, lower-assurance bank evidence stays browseable without verified reports. */
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
                Text("Limited assurance only", fontWeight = FontWeight.SemiBold, color = p.ink)
                Text("These statements were saved locally, but have no independently verified opening balance or page totals. They are excluded from verified reports and Activity.",
                    color = p.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        items(sources, key = { it.id }) { source ->
            StatementInsightCard {
                Row(Modifier.fillMaxWidth().clickable { selected = source },
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${source.rows} saved source rows", color = p.ink,
                            fontWeight = FontWeight.SemiBold)
                        Text("${statementDate(source.firstAt)} – ${statementDate(source.lastAt)}",
                            color = p.muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Rounded.ChevronRight, "View source rows", tint = p.muted)
                }
            }
        }
        item { Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Text("Import another statement")
        } }
    }
}
