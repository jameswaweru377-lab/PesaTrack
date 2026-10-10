package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.model.FinancialInstitutionOption
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingOwnershipChoice
import com.kedevjimmy.pesatrack.ui.components.PesaCard

@Composable
internal fun LinkedSavingsReviewPage(
    options: List<FinancialInstitutionOption>,
    choices: Map<String, OnboardingOwnershipChoice>,
    saving: Boolean,
    error: String?,
    onChoice: (String, OnboardingOwnershipChoice) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SmartSetupHero(
            icon = Icons.Rounded.Savings,
            title = "Do you use M-Shwari savings?",
            body = "Your M-Pesa number doesn't prove that you use M-Shwari or Lock Savings. Confirm each account separately, or skip."
        )
        options.forEach { option ->
            val key = option.reference.providerKey ?: return@forEach
            PesaCard {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(option.reference.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(
                        if (key == "NCBA_MSHWARI_LOCK_SAVINGS")
                            "A separate locked savings product, not your available M-Pesa balance."
                        else
                            "A separate savings balance, not extra income when you move money from M-Pesa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            OnboardingOwnershipChoice.MINE to "Mine",
                            OnboardingOwnershipChoice.NOT_MINE to "Not mine",
                            OnboardingOwnershipChoice.UNSURE to "Unsure"
                        ).forEach { (choice, label) ->
                            FilterChip(
                                selected = (choices[key] ?: OnboardingOwnershipChoice.UNSURE) == choice,
                                onClick = { onChoice(key, choice) },
                                enabled = !saving,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                    .testTag("linked-savings-${key}-${choice.name}"),
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = onContinue, enabled = !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(18.dp)) {
            Text(if (saving) "Saving…" else "Continue")
        }
        TextButton(onClick = onSkip, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
            Text("Skip savings setup for now")
        }
        Text(
            "These choices are kept on your device. Moving money between your own accounts isn't income or spending.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
