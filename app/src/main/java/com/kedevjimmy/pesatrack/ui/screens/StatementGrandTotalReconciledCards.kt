package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.statements.StatementGrandTotalReconciledPolicy

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
