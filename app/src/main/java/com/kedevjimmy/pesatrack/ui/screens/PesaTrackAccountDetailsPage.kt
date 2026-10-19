package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.ui.components.PesaCard

@Composable
internal fun PesaTrackAccountDetailsPage(
    accounts: List<MoneyAccount>,
    labels: Map<String, String>,
    lastFourHints: Map<String, String>,
    mpesaSelected: Boolean,
    suggestedMpesaNumber: String?,
    mpesaNumberAccepted: Boolean,
    mpesaPhoneInput: String,
    saving: Boolean,
    error: String?,
    onLabelChanged: (String, String) -> Unit,
    onLastFourChanged: (String, String) -> Unit,
    onMpesaNumberAccepted: (Boolean) -> Unit,
    onMpesaPhoneChanged: (String) -> Unit,
    onSaveAndContinue: () -> Unit,
    onSkip: () -> Unit
) {
    var editMpesaNumber by rememberSaveable { mutableStateOf(false) }
    val banks = accounts.filter { it.type == AccountType.BANK }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Column(Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
            Text("Help us match your accounts", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Add only what helps identify your own activity. These details are optional and can be added later.", Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        if (mpesaSelected) {
            AccountDetailCard(
                title = "M‑Pesa · ${maskedSuffix(suggestedMpesaNumber)}",
                subtitle = "Suggested from your messages",
                label = labels["${AccountType.MOBILE_WALLET.name}:mpesa"].orEmpty(),
                labelPlaceholder = "Name you use for this wallet (optional)",
                onLabelChanged = { onLabelChanged("${AccountType.MOBILE_WALLET.name}:mpesa", it) }
            ) {
                Text("Confirm the detected number or update it if it isn’t yours. Your full number is only needed to match your own transfers.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = {
                    if (mpesaNumberAccepted) {
                        editMpesaNumber = true
                        onMpesaNumberAccepted(false)
                    } else if (!editMpesaNumber && !suggestedMpesaNumber.isNullOrBlank()) {
                        onMpesaNumberAccepted(true)
                    } else {
                        editMpesaNumber = true
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (mpesaNumberAccepted) "Confirmed number ···· ${maskedSuffix(suggestedMpesaNumber)}  ·  Change" else "Confirm number ···· ${maskedSuffix(suggestedMpesaNumber)}  ⌄")
                }
                if (editMpesaNumber) {
                    OutlinedTextField(
                        value = mpesaPhoneInput,
                        onValueChange = onMpesaPhoneChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        label = { Text("Your M‑Pesa number") },
                        supportingText = { Text("Saved on this phone to identify your own transfers.") }
                    )
                    TextButton(onClick = {
                        if (mpesaPhoneInput.isNotBlank()) {
                            editMpesaNumber = false
                            onMpesaNumberAccepted(true)
                        }
                    }, enabled = mpesaPhoneInput.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Use this number") }
                }
            }
        }
        if (banks.isEmpty()) {
            PesaCard { Text("No selected banks. You can add account details later.", Modifier.padding(13.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else banks.forEach { account ->
            AccountDetailCard(
                title = account.name,
                subtitle = "Selected institution",
                label = labels[account.id].orEmpty(),
                labelPlaceholder = "Account name (optional)",
                onLabelChanged = { onLabelChanged(account.id, it) }
            ) {
                OptionalAccountField(
                    value = lastFourHints[account.id].orEmpty(),
                    placeholder = "Last 4 account digits (optional)",
                    onValueChange = { onLastFourChanged(account.id, it.filter(Char::isDigit).take(4)) },
                    keyboardType = KeyboardType.Number
                )
            }
        }
        Row(Modifier.padding(horizontal = 2.dp, vertical = 3.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 1.dp))
            Text("Account details help separate your own accounts from recipients. You can skip every field.", Modifier.padding(start = 7.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = onSaveAndContinue, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(15.dp)) {
            Text(if (saving) "Saving…" else "Save and continue")
        }
        TextButton(onClick = onSkip, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("Skip for now") }
    }
}

@Composable
private fun AccountDetailCard(
    title: String,
    subtitle: String,
    label: String,
    labelPlaceholder: String,
    onLabelChanged: (String) -> Unit,
    additionalFields: @Composable ColumnScope.() -> Unit
) {
    PesaCard {
        Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ApprovedMonogram(title)
                Column(Modifier.weight(1f).padding(start = 9.dp)) {
                    Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Optional", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OptionalAccountField(value = label, placeholder = labelPlaceholder, onValueChange = { onLabelChanged(it.take(60)) })
            additionalFields()
        }
    }
}

@Composable
private fun OptionalAccountField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(11.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(11.dp)).padding(horizontal = 10.dp, vertical = 10.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.labelSmall.merge(TextStyle(color = MaterialTheme.colorScheme.onSurface)),
        decorationBox = { inner ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (value.isBlank()) Text(placeholder, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
            }
        }
    )
}

private fun maskedSuffix(raw: String?): String = raw.orEmpty().filter(Char::isDigit).takeLast(3).ifBlank { "•••" }
