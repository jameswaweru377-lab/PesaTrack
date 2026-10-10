package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.rules.AccountIdentityPresentation
import com.kedevjimmy.pesatrack.ui.components.PesaCard

@Composable
internal fun PesaTrackAccountDetailsPage(
    accounts: List<MoneyAccount>,
    profileName: String,
    editedReferences: Map<String, String>,
    saving: Boolean,
    error: String?,
    onNameChanged: (String) -> Unit,
    onReferenceChanged: (String, String) -> Unit,
    onSaveAndContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val banks = accounts.filter { it.type == AccountType.BANK }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SmartSetupHero(
            icon = Icons.Rounded.AccountBalance,
            title = "Your account details",
            body = "Only bank accounts you confirmed as yours appear here. Everything on this page is optional."
        )
        PesaCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { onNameChanged(it.take(60)) },
                    label = { Text("Your full name (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    "Saved to your local profile. A matching name alone never proves that money was transferred between your own accounts.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (banks.isEmpty()) {
            PesaCard {
                Text("No bank accounts confirmed yet. Add them later in Accounts.",
                    Modifier.padding(16.dp))
            }
        } else banks.forEach { account ->
            PesaCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(AccountIdentityPresentation.title(account),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text("Only the final digits of a saved account reference are displayed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = editedReferences[account.id].orEmpty(),
                        onValueChange = { onReferenceChanged(account.id, it.take(60)) },
                        label = { Text("Full account number (optional)") },
                        placeholder = { Text("Leave blank if unknown") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Last-four-only references are display hints, not proof of ownership. Enter a complete account number or skip.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onSaveAndContinue, enabled = !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(18.dp)) {
            if (saving) CircularProgressIndicator() else Text("Save and continue")
        }
        TextButton(onClick = onSkip, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
            Text("Skip for now")
        }
    }
}
