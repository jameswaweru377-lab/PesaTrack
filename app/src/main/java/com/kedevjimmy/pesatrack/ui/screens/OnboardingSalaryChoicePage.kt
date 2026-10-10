package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingSalarySuggestion
import com.kedevjimmy.pesatrack.ui.components.PesaCard

/** Step 6 of the approved design. Nothing is accepted without tapping Continue. */
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
    var decision by rememberSaveable(suggestion?.label, suggestion?.accountId) {
        mutableStateOf("later")
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(78.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Payments, null, modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Text(
            if (suggestion != null) "We found a possible salary" else "Set up your salary (optional)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (suggestion != null)
                "It looks like you receive a salary from ${suggestion.label}."
            else
                "No salary was identified. You can set one up later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (suggestion != null) {
            PesaCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Detected from messages",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatSetupMoney(suggestion.representativeAmountMinor) + " (estimated)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Text("Would you like to set this up?",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (suggestion != null) {
            SalaryChoiceRadio(
                selected = decision == "yes",
                title = "Yes, set up my salary",
                description = "Track expected vs received salary and analyse deductions.",
                onClick = { decision = "yes" }
            )
        }
        SalaryChoiceRadio(
            selected = decision == "later",
            title = "Not now",
            description = "I'll set this up later.",
            onClick = { decision = "later" }
        )
        if (suggestion != null) {
            SalaryChoiceRadio(
                selected = decision == "not_my_salary",
                title = "This is not my salary",
                description = "Ignore this suggestion.",
                onClick = { decision = "not_my_salary" }
            )
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                when (decision) {
                    "yes" -> onConfirmDetected()
                    "not_my_salary" -> onNotMySalary()
                    else -> onSkip()
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text(if (saving) "Saving…" else "Continue") }
        TextButton(onClick = onEnterManually, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
            Text("Record salary received manually")
        }
    }
}

@Composable
private fun SalaryChoiceRadio(
    selected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 57.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 9.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
