package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.FinancialInstitutionDirectory
import com.kedevjimmy.pesatrack.domain.model.FinancialInstitutionOption
import com.kedevjimmy.pesatrack.ingestion.HistoricalImportResult
import com.kedevjimmy.pesatrack.ui.components.PesaCard

@Composable
internal fun ReviewDiscoveryPage(
    sources: List<DetectedSetupSource>,
    selectedOwnedKeys: Set<String>,
    mpesaIdentifierHint: String?,
    importResult: HistoricalImportResult?,
    applying: Boolean,
    onToggleAccount: (String, Boolean) -> Unit,
    onSelectInstitution: (FinancialAccountReference, Boolean) -> Unit,
    onContinue: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val ranked = remember(sources) { OnboardingInstitutionPresentationPolicy.topFive(sources) }
    val sourceKeys = remember(sources) { sources.mapTo(mutableSetOf()) { it.reference.entityKey } }
    val moreDetected = remember(query, sources, ranked) {
        if (query.isBlank()) emptyList() else sources.filter { source ->
            source.reference.entityKey !in ranked.map { it.reference.entityKey } && source.name.contains(query, true)
        }
    }
    val directoryResults = remember(query, sourceKeys) {
        if (query.isBlank()) emptyList() else FinancialInstitutionDirectory.search(query)
            .filterNot { it.reference.entityKey in sourceKeys }
            .filterNot { OnboardingInstitutionPresentationPolicy.isSafaricomProduct(it.reference.providerKey) }
            .distinctBy { it.reference.entityKey }.take(10)
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Which accounts are yours?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Text("Select only the services you personally use.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(60) },
            modifier = Modifier.fillMaxWidth().testTag("onboarding-institution-search"),
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            placeholder = { Text("Search banks and money services") },
            singleLine = true,
            enabled = !applying
        )
        if (ranked.isNotEmpty()) {
            ranked.forEach { source ->
                val isMpesa = source.reference.providerKey == "SAFARICOM_MPESA"
                DetectedInstitutionCard(
                    name = source.name,
                    type = if (isMpesa) "Mobile money · possible wallet" else "${source.type} · possible account",
                    key = source.reference.entityKey,
                    messageCount = source.messageCount,
                    identifierHint = if (isMpesa) OnboardingInstitutionPresentationPolicy.maskIdentifier(mpesaIdentifierHint) else null,
                    checked = source.reference.entityKey in selectedOwnedKeys,
                    enabled = !applying,
                    onChecked = { onToggleAccount(source.reference.entityKey, it) }
                )
            }
        } else {
            PesaCard {
                Text(
                    if (importResult == null) "No institutions were identified yet. Search for one you use, or continue and add accounts later."
                    else "No financial institutions could be confidently identified. Search for one you use, or continue without adding an account.",
                    Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Text("${selectedOwnedKeys.size} selected · Select only accounts you use", modifier = Modifier.padding(start = 1.dp).testTag("onboarding-selected-institution-count"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        moreDetected.forEach { source ->
            DetectedInstitutionCard(source.name, "${source.type} · detected from messages", source.reference.entityKey, source.messageCount, null, source.reference.entityKey in selectedOwnedKeys, !applying) { onToggleAccount(source.reference.entityKey, it) }
        }
        directoryResults.forEach { option ->
            val ref = option.reference
            val type = when (ref.accountTypeHint) {
                AccountType.BANK -> "Bank · possible account"
                AccountType.MOBILE_WALLET -> "Mobile money · possible wallet"
                else -> "Savings · possible account"
            }
            DetectedInstitutionCard(ref.displayName, type, ref.entityKey, null, null, ref.entityKey in selectedOwnedKeys, !applying) { onSelectInstitution(ref, it) }
        }
        if (query.isNotBlank() && moreDetected.isEmpty() && directoryResults.isEmpty()) {
            Text("No matches in your supported institution list. Try another name.", modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = onContinue, enabled = !applying, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(15.dp)) {
            Text(if (applying) "Saving choices…" else "Continue with selected")
        }
        TextButton(onClick = onContinue, enabled = !applying, modifier = Modifier.fillMaxWidth()) { Text("I’ll choose later") }
    }
}

@Composable
internal fun DetectedAccountCheckRow(
    key: String,
    name: String,
    type: String,
    checked: Boolean,
    enabled: Boolean,
    messageCount: Int? = null,
    identifierHint: String? = null,
    onChecked: (Boolean) -> Unit
) = DetectedInstitutionCard(name, type, key, messageCount, identifierHint, checked, enabled, onChecked)

@Composable
private fun DetectedInstitutionCard(
    name: String,
    type: String,
    key: String,
    messageCount: Int?,
    identifierHint: String?,
    checked: Boolean,
    enabled: Boolean,
    onChecked: (Boolean) -> Unit
) {
    val outline = if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier.fillMaxWidth().heightIn(min = 58.dp)
            .background(if (checked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .25f) else MaterialTheme.colorScheme.surface, RoundedCornerShape(15.dp))
            .border(1.dp, outline, RoundedCornerShape(15.dp))
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onChecked)
            .testTag("detected-account-$key").padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(Modifier.size(34.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) {
                val icon = when {
                    type.startsWith("Bank") -> Icons.Rounded.AccountBalance
                    type.startsWith("Mobile") || type == "Wallet" -> Icons.Rounded.Wallet
                    else -> Icons.Rounded.AccountBalance
                }
                Icon(icon, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Column(Modifier.weight(1f).padding(start = 9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(onboardingShortBankName(name), modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold)
            }
            val details = listOfNotNull(type, identifierHint).joinToString(" · ")
            Text(details, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** Display-only shortening; institution identity and ownership keys remain unchanged. */
private fun onboardingShortBankName(name: String): String =
    name.removeSuffix(" Bank Kenya").let { shortened ->
        if (shortened != name) "$shortened Bank"
        else name.removeSuffix(" of Kenya").removeSuffix(" Kenya")
    }
