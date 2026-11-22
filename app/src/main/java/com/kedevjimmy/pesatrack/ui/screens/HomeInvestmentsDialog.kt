package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.ObservedInvestmentPosition
import java.text.NumberFormat
import java.util.Locale

/** Existing evidence-only investment details, retained without the retired Home B pages. */
@Composable
internal fun HomeInvestmentsDialog(
    positions: List<ObservedInvestmentPosition>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Observed investments") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "The app has execution observations, not a verified market value or an authoritative brokerage balance.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (positions.isEmpty()) Text("No executed investments have been detected.")
                positions.take(6).forEach { position ->
                    Text(position.label, fontWeight = FontWeight.Bold)
                    Text(
                        "Observed net units: " +
                            NumberFormat.getNumberInstance(Locale.US)
                                .format(position.quantityMicrounits / 1_000_000.0) +
                            " · Partial history",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (positions.size > 6) Text("More investment records are available.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
