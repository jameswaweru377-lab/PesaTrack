package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.StatementBankArchiveStore
import com.kedevjimmy.pesatrack.domain.statements.StatementGrandTotalReconciledPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Never render financial metadata until this specific document's saved rows reconcile. */
@Composable
internal fun StatementGrandTotalReconciledSourceCard(
    source: StatementBankArchiveStore.Source,
    onSelect: () -> Unit
) {
    val context = LocalContext.current
    var checked by remember(source) { mutableStateOf(false) }
    var report by remember(source) {
        mutableStateOf<StatementGrandTotalReconciledPolicy.Report?>(null)
    }
    LaunchedEffect(source) {
        checked = false
        report = try {
            withContext(Dispatchers.IO) {
                val rows = StatementBankArchiveStore(context).limitedLegsFor(source.id).map { leg ->
                    StatementGrandTotalReconciledPolicy.Row(
                        leg.sourceId, leg.number, leg.occurredAt, leg.movementMinor
                    )
                }
                StatementGrandTotalReconciledPolicy.project(source.id, source.rows,
                    source.moneyInMinor, source.moneyOutMinor, rows)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        checked = true
    }
    StatementGrandTotalReconciledSourceCardContent(source, report, checked, onSelect)
}

@Composable
internal fun StatementGrandTotalReconciledSourceCardContent(
    source: StatementBankArchiveStore.Source,
    report: StatementGrandTotalReconciledPolicy.Report?,
    checked: Boolean,
    onSelect: () -> Unit
) {
    val p = HomeReferencePalette
    StatementInsightCard {
        Row(Modifier.fillMaxWidth().clickable(onClick = onSelect),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Grand-total reconciled · ${source.rows} rows",
                    color = p.ink, fontWeight = FontWeight.SemiBold)
                Text("${statementDate(source.firstAt)} – ${statementDate(source.lastAt)}",
                    color = p.muted, style = MaterialTheme.typography.bodySmall)
                Text(when {
                    checked && report != null -> "Money in ${statementMoney(report.moneyInMinor)} · Money out ${statementMoney(report.moneyOutMinor)}"
                    checked -> "Cash totals unavailable — saved rows need review"
                    else -> "Checking saved source rows…"
                }, color = if (checked && report == null) p.red else p.muted,
                    style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Rounded.ChevronRight, "View source rows", tint = p.muted)
        }
    }
}

/** The sums are for one saved document, never a combined account or Activity balance. */
@Composable
internal fun StatementGrandTotalReconciledSummary(
    report: StatementGrandTotalReconciledPolicy.Report
) {
    val p = HomeReferencePalette
    StatementInsightCard {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("This PDF · ${report.rows} reconciled rows", fontWeight = FontWeight.SemiBold,
                color = p.ink)
            Text("Money in · ${statementMoney(report.moneyInMinor)}", color = p.ink)
            Text("Money out · ${statementMoney(report.moneyOutMinor)}", color = p.ink)
            Text("Net movement · ${statementMoney(report.netMinor)}", color = p.ink,
                fontWeight = FontWeight.SemiBold)
            Text("Gross account movements, not categorized income or spending.",
                style = MaterialTheme.typography.bodySmall, color = p.muted)
        }
    }
    report.months.forEach { month ->
        StatementInsightCard {
            Column(Modifier.fillMaxWidth().padding(vertical = 1.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("${month.key} · ${month.rows} rows", fontWeight = FontWeight.SemiBold,
                    color = p.ink)
                Text("Money in ${statementMoney(month.moneyInMinor)} · Money out ${statementMoney(month.moneyOutMinor)}",
                    style = MaterialTheme.typography.bodySmall, color = p.muted)
            }
        }
    }
}
