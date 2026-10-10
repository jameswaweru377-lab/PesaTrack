package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.rules.AccountIdentityPresentation
import com.kedevjimmy.pesatrack.ui.components.PesaCard

/**
 * Approved account details layout. Four-digit hints are display-only, never
 * used alone to prove ownership, sender identity, or a self-transfer.
 */
@Composable
internal fun PesaTrackAccountDetailsPage(
    accounts: List<MoneyAccount>,
    profileName: String,
    editedReferences: Map<String, String>,
    lastFourHints: Map<String, String>,
    saving: Boolean,
    error: String?,
    onNameChanged: (String) -> Unit,
    onReferenceChanged: (String, String) -> Unit,
    onLastFourChanged: (String, String) -> Unit,
    onSaveAndContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val banks = accounts.filter { it.type == AccountType.BANK }
    var multipleAccounts by rememberSaveable { mutableStateOf(banks.size > 1) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text(
            if (banks.size == 1) "${banks.first().name} account"
            else "Your bank accounts",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text("Help us identify your bank transactions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (banks.isEmpty()) {
            PesaCard {
                Text("No banks confirmed yet. You can add a bank later.",
                    Modifier.padding(15.dp))
            }
        } else {
            banks.forEach { account ->
                PesaCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text("Detected from messages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = profileName,
                            onValueChange = { onNameChanged(it.take(60)) },
                            label = { Text("Your name (as used in bank)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text("Use the same name you use with the bank.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = lastFourHints[account.id]
                                ?: account.identifier?.filter(Char::isDigit)?.takeLast(4).orEmpty(),
                            onValueChange = { onLastFourChanged(account.id, it.filter(Char::isDigit).take(4)) },
                            label = { Text("Account number (last 4 digits)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text("Last four digits are a private display hint, not proof of ownership.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (banks.size == 1) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Do you have more than one ${account.name} account?",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f))
                                Switch(checked = multipleAccounts, onCheckedChange = { multipleAccounts = it })
                            }
                            if (multipleAccounts) {
                                Text("You can add another account in Accounts after setup.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = onSaveAndContinue, enabled = !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(12.dp)) {
            Text(if (saving) "Saving…" else "Continue")
        }
        TextButton(onClick = onSkip, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
            Text("Back to this later")
        }
    }
}
