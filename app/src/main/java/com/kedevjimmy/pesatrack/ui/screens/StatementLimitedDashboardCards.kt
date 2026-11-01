package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.StatementBankArchiveStore
import com.kedevjimmy.pesatrack.domain.statements.StatementGrandTotalReconciledPolicy

/** Display-only insights from one source whose saved rows have reconciled again. */
@Composable
internal fun StatementLimitedDashboardCards(
    report: StatementGrandTotalReconciledPolicy.Report,
    largestOutflows: List<StatementBankArchiveStore.Leg>
) {
    val p = HomeReferencePalette
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = p.purple, shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Net movement · this statement", color = MaterialTheme.colorScheme.onPrimary)
                Text(statementMoney(report.netMinor), color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Money in minus money out; not an account balance.",
                    color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MovementTile("Money in", report.moneyInMinor, Modifier.weight(1f))
            MovementTile("Money out", report.moneyOutMinor, Modifier.weight(1f))
        }
        MonthlyMovementCard(report.months)
        StatementInsightCard {
            Text("Spending categories", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = p.ink)
            Text("Spending categories are not yet available for this statement.",
                color = p.muted, style = MaterialTheme.typography.bodyMedium)
            Text("Money out can include transfers, cash withdrawals and other movements.",
                color = p.muted, style = MaterialTheme.typography.bodySmall)
        }
        StatementInsightCard {
            Text("Largest money-out movements", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = p.ink)
            Text("These are not automatically classified as spending.",
                color = p.muted, style = MaterialTheme.typography.bodySmall)
            if (largestOutflows.isEmpty()) {
                Text("No outgoing movements to show.", color = p.muted)
            } else largestOutflows.forEach { leg ->
                Text(leg.description, fontWeight = FontWeight.SemiBold, color = p.ink)
                Text("${statementDate(leg.occurredAt)} · ${statementMoney(-leg.movementMinor)}",
                    color = p.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MovementTile(label: String, amount: Long, modifier: Modifier) {
    val p = HomeReferencePalette
    Surface(modifier = modifier, shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = p.muted, style = MaterialTheme.typography.labelLarge)
            Text(statementMoney(amount), color = p.ink, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun MonthlyMovementCard(months: List<StatementGrandTotalReconciledPolicy.Month>) {
    val p = HomeReferencePalette
    val shown = months.take(12).asReversed()
    val maximum = shown.maxOfOrNull { maxOf(it.moneyInMinor, it.moneyOutMinor) } ?: 0L
    StatementInsightCard {
        Text("Monthly money movement", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = p.ink)
        Text(if (months.size > shown.size) "Most recent 12 months in this PDF"
            else "Months present in this PDF · gross movements", color = p.muted,
            style = MaterialTheme.typography.bodySmall)
        Text("Scale: ${statementMoney(maximum)}", color = p.muted,
            style = MaterialTheme.typography.labelSmall)
        shown.forEach { month ->
            Text(month.key, fontWeight = FontWeight.SemiBold, color = p.ink)
            Text("In ${statementMoney(month.moneyInMinor)}", color = p.muted,
                style = MaterialTheme.typography.labelSmall)
            LinearProgressIndicator(progress = { chartRatio(month.moneyInMinor, maximum) },
                modifier = Modifier.fillMaxWidth(), color = p.green, trackColor = p.line)
            Text("Out ${statementMoney(month.moneyOutMinor)}", color = p.muted,
                style = MaterialTheme.typography.labelSmall)
            LinearProgressIndicator(progress = { chartRatio(month.moneyOutMinor, maximum) },
                modifier = Modifier.fillMaxWidth(), color = p.purple, trackColor = p.line)
        }
    }
}

/** Floating point only controls the bar length; amounts and totals stay exact integer minor units. */
private fun chartRatio(amount: Long, maximum: Long): Float =
    if (maximum <= 0L) 0f else (amount.toDouble() / maximum.toDouble()).toFloat().coerceIn(0f, 1f)
