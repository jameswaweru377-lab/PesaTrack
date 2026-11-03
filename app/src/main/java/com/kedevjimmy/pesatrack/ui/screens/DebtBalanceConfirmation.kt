package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.DebtAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.DebtStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.domain.everyday.EverydayMoneyPolicy
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountNature
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialProviderRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A user-entered lender balance is recorded only after explicit confirmation. */
@Composable
internal fun DebtBalanceConfirmationDialog(debts: List<DebtAccountSnapshot>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(debts.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Confirm debt balance") },
        text = {
            Column {
                Text("Enter the outstanding amount shown by your lender now. This becomes a new observed balance, not income or an expense.",
                    style = MaterialTheme.typography.bodySmall)
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text("Lender: ${selected?.name.orEmpty()} ▾")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        debts.forEach { debt ->
                            DropdownMenuItem(text = { Text(debt.name) }, onClick = {
                                selected = debt
                                expanded = false
                            })
                        }
                    }
                }
                OutlinedTextField(value = amountText, onValueChange = { amountText = it; error = null },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    singleLine = true, label = { Text("Outstanding balance (KES)") },
                    placeholder = { Text("0.00") })
                if (error != null) Text(error.orEmpty(), modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = {
            TextButton(enabled = !saving, onClick = {
                val amount = EverydayMoneyPolicy.money(amountText)
                val reference = selected?.providerKey?.let(FinancialProviderRegistry::referenceForProviderKey)
                if (amount == null || amount < 0L) {
                    error = "Enter a valid amount, including zero only if fully repaid."
                } else if (reference == null || reference.nature != FinancialAccountNature.LIABILITY) {
                    error = "This lender cannot be confirmed from this screen yet."
                } else {
                    saving = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                DebtStore.get(context).recordObservedOutstanding(reference, amount,
                                    System.currentTimeMillis())
                            }
                            LedgerUpdateBus.notifyChanged()
                            onDismiss()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            error = "Balance could not be saved. Nothing has been confirmed."
                        } finally {
                            saving = false
                        }
                    }
                }
            }) { Text(if (saving) "Saving…" else "Confirm") }
        }, dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") }
        })
}
