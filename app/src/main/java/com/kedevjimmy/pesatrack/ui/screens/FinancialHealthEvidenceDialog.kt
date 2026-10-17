package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.FinancialHealthEvidenceStore

/**
 * Optional user-entered planning facts. Does not write transactions or access
 * financial messages. Values remain app-private and expire from assessments
 * until explicitly confirmed again after 30 days.
 */
@Composable
internal fun FinancialHealthEvidenceDialog(
    store: FinancialHealthEvidenceStore,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val initial = remember(store) { store.editableAmounts() }
    var reserve by remember(store) { mutableStateOf(initial.first) }
    var essentials by remember(store) { mutableStateOf(initial.second) }
    var requiredDebt by remember(store) { mutableStateOf(initial.third) }
    var error by remember { mutableStateOf<String?>(null) }
    val keyboard = KeyboardOptions(keyboardType=KeyboardType.Decimal)
    AlertDialog(
        onDismissRequest=onDismiss,
        title={ Text("Confirm financial context") },
        text={
            Column(Modifier.heightIn(max=490.dp).verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("Optional, on-device amounts in KES. Only include emergency money you can actually access, essentials you need each month, and required loan payments due each month. Do not include transfers twice.",
                    style=MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value=reserve,onValueChange={reserve=it;error=null},
                    label={Text("Accessible emergency savings (KES)")},
                    supportingText={Text("Blank means unknown; zero means you confirmed none.")},
                    keyboardOptions=keyboard,singleLine=true,modifier=Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value=essentials,onValueChange={essentials=it;error=null},
                    label={Text("Essential monthly living costs (KES)")},
                    supportingText={Text("Food, rent, transport, utilities and other essentials.")},
                    keyboardOptions=keyboard,singleLine=true,modifier=Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value=requiredDebt,onValueChange={requiredDebt=it;error=null},
                    label={Text("Required monthly debt repayments (KES)")},
                    supportingText={Text("Payments due, not total outstanding principal.")},
                    keyboardOptions=keyboard,singleLine=true,modifier=Modifier.fillMaxWidth()
                )
                Text("Entries are not bank-verified and never create income, expenses, debts or transfers. Reconfirm after 30 days. You can clear any field to mark it unknown.",
                    style=MaterialTheme.typography.labelSmall)
                error?.let { Text(it,color=MaterialTheme.colorScheme.error,
                    style=MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton={
            Button(onClick={
                runCatching { store.save(reserve,essentials,requiredDebt) }
                    .onSuccess { onSaved() }
                    .onFailure { error=it.message ?: "Could not save the details." }
            }) { Text("Save locally") }
        },
        dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}}
    )
}
