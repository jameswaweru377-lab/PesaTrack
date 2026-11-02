package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.FingerprintConflictReviewItem
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun FingerprintConflictReviewSection(
    conflicts: List<FingerprintConflictReviewItem>,
    onOpenActivity: (Long) -> Unit
) {
    if (conflicts.isEmpty()) return
    Column {
        Text("Conflicting transaction evidence", style = MaterialTheme.typography.titleMedium)
        Text("The same code was observed with different financial details. Neither observation has been merged or counted again.",
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        conflicts.forEach { conflict -> FingerprintConflictReviewCard(conflict, onOpenActivity) }
    }
}

@Composable
internal fun FingerprintConflictReviewCard(
    conflict: FingerprintConflictReviewItem,
    onOpenActivity: (Long) -> Unit
) {
    StatementInsightCard {
                Text("Reference ${conflict.transactionCode}", style = MaterialTheme.typography.bodyMedium)
                Text("Existing Activity: ${conflictMoney(conflict.originalAmountMinor)}")
                Text("Incoming ${conflict.sourceKind.take(32)}: ${conflictMoney(conflict.incomingAmountMinor)}")
                Text("Needs review — these may be separate records. No balances or totals were changed.",
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { onOpenActivity(conflict.existingTransactionId) }) {
                    Text("Open Activity record")
                }
    }
}

private fun conflictMoney(minor: Long): String = "KES " + NumberFormat.getNumberInstance(Locale.US).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}.format(BigDecimal.valueOf(minor, 2))
