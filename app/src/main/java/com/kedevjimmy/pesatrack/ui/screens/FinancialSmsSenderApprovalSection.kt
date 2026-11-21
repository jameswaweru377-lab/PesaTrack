package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.kedevjimmy.pesatrack.data.local.FinancialSmsSenderApprovalStore
import com.kedevjimmy.pesatrack.domain.ingestion.FinancialSmsSenderPolicy
import com.kedevjimmy.pesatrack.ui.components.PesaCard

/** Optional sender approval lives in Import History, never in mandatory onboarding. */
@Composable
internal fun FinancialSmsSenderApprovalSection(importing: Boolean) {
    val context = LocalContext.current
    val approvals = remember { FinancialSmsSenderApprovalStore(context) }
    var approvedSenders by remember { mutableStateOf(approvals.listApproved().sorted()) }
    var senderInput by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    PesaCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Other financial SMS senders", style = MaterialTheme.typography.titleMedium)
            Text(
                "If your bank or lender uses a sender name PesaTrack does not recognise, approve that exact name here, then run the history import. Only approve financial businesses you know; personal phone numbers are not allowed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = senderInput,
                onValueChange = { senderInput = it; error = null },
                label = { Text("Sender name or shortcode") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !importing
            )
            Button(
                onClick = {
                    if (!FinancialSmsSenderPolicy.canApprove(senderInput)) {
                        error = "Use a financial business name or short code, not a phone number."
                    } else if (approvals.approve(senderInput)) {
                        approvedSenders = approvals.listApproved().sorted()
                        senderInput = ""
                        error = null
                    } else {
                        error = "Could not save this sender. Please try again."
                    }
                },
                enabled = senderInput.isNotBlank() && !importing
            ) { Text("Approve sender") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            approvedSenders.forEach { sender ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sender, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        if (approvals.remove(sender)) {
                            approvedSenders = approvals.listApproved().sorted()
                        } else {
                            error = "Could not remove this sender. Please try again."
                        }
                    }, enabled = !importing) { Text("Remove") }
                }
            }
        }
    }
}
