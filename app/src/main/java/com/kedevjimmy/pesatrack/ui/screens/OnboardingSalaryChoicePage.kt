package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingSalarySuggestion
import com.kedevjimmy.pesatrack.ui.components.PesaCard

/** Last optional step in the seven-screen first-run journey. No salary entry is invented. */
@Composable
internal fun OnboardingSalaryChoicePage(
    suggestion: OnboardingSalarySuggestion?,
    saving: Boolean,
    error: String?,
    onConfirmDetected: () -> Unit,
    onEnterManually: () -> Unit,
    onNotMySalary: () -> Unit,
    onSkip: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SmartSetupHero(
            icon = Icons.Rounded.Payments,
            title = if (suggestion != null) "We found a possible salary" else "Set up salary (optional)",
            body = if (suggestion != null)
                "This payment pattern could be your payday. Confirm it before PesaTrack creates a salary rule."
            else "No salary was confidently identified. You can record money you actually received or set this up later."
        )
        if (suggestion != null) {
            PesaCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(suggestion.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${suggestion.monthCount} month(s) observed · Typical credit ${formatSetupMoney(suggestion.representativeAmountMinor)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "This is a suggestion, not a new deposit. Confirming does not create duplicate income.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Button(onClick = onConfirmDetected, enabled = !saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(18.dp)) {
                Text(if (saving) "Saving…" else "Yes, this is my salary")
            }
            OutlinedButton(onClick = onNotMySalary, enabled = !saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("This isn't my salary")
            }
        }
        OutlinedButton(onClick = onEnterManually, enabled = !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Record salary I received")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TextButton(onClick = onSkip, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
            Text("Not now — continue without salary")
        }
        Text(
            "Salary is optional. PesaTrack keeps tracking accounts, spending and transfers without it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
