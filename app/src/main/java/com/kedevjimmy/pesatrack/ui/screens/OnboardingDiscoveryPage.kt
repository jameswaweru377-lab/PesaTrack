package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.FinancialInstitutionDirectory
import com.kedevjimmy.pesatrack.domain.model.FinancialInstitutionOption
import com.kedevjimmy.pesatrack.ingestion.HistoricalImportResult
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard

/** The five most active detections remain suggestions until the user selects them. */
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
    val ranked = remember(sources) {
        sources
            .filterNot { OnboardingInstitutionPresentationPolicy.isSafaricomProduct(it.reference.providerKey) }
            .sortedWith(
                compareByDescending<DetectedSetupSource> { it.messageCount }
                    .thenBy { it.name.lowercase() }
                    .thenBy { it.reference.entityKey }
            )
    }
    val topFive = remember(ranked) {
        OnboardingInstitutionPresentationPolicy.topFive(ranked)
    }
    val sourceKeys = remember(ranked) { ranked.mapTo(mutableSetOf()) { it.reference.entityKey } }
    val queryResults = remember(query, ranked) {
        if (query.isBlank()) emptyList()
        else ranked.drop(topFive.size).filter { it.name.contains(query, ignoreCase = true) }
    }
    val directoryResults = remember(query, sourceKeys) {
        if (query.isBlank()) emptyList()
        else FinancialInstitutionDirectory.search(query)
            .filterNot { it.reference.entityKey in sourceKeys }
            .filterNot { OnboardingInstitutionPresentationPolicy.isSafaricomProduct(it.reference.providerKey) }
            .distinctBy { it.reference.entityKey }
            .take(12)
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text(
            "Which accounts are yours?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "These are the five institutions with the most financial activity in your selected messages. A mention alone doesn’t mean you own an account.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (topFive.isEmpty()) {
            PesaCard {
                Text(
                    if (importResult == null) {
                        "No institutions were identified yet. Search for one you use, or continue and add accounts later."
                    } else {
                        "No financial institutions could be confidently identified. Search for one you use, or continue without adding an account."
                    },
                    Modifier.padding(15.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                "Most active in your messages · Top 5",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            topFive.forEach { source ->
                val phoneHint = if (source.reference.providerKey == "SAFARICOM_MPESA") {
                    OnboardingInstitutionPresentationPolicy.maskIdentifier(mpesaIdentifierHint)
                } else null
                DetectedAccountCheckRow(
                    key = source.reference.entityKey,
                    name = source.name,
                    type = source.type,
                    checked = source.reference.entityKey in selectedOwnedKeys,
                    enabled = !applying,
                    messageCount = source.messageCount,
                    identifierHint = phoneHint,
                    onChecked = { onToggleAccount(source.reference.entityKey, it) }
                )
            }
        }

        Text(
            "${selectedOwnedKeys.size} selected · Select only accounts you use",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("onboarding-selected-institution-count")
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(60) },
            modifier = Modifier.fillMaxWidth().testTag("onboarding-institution-search"),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text("Search all banks and money services") },
            singleLine = true,
            enabled = !applying
        )

        queryResults.forEach { source ->
            DetectedAccountCheckRow(
                key = source.reference.entityKey,
                name = source.name,
                type = source.type,
                checked = source.reference.entityKey in selectedOwnedKeys,
                enabled = !applying,
                messageCount = source.messageCount,
                onChecked = { onToggleAccount(source.reference.entityKey, it) }
            )
        }
        directoryResults.forEach { option ->
            val reference = option.reference
            DetectedAccountCheckRow(
                key = reference.entityKey,
                name = reference.displayName,
                type = institutionType(option),
                checked = reference.entityKey in selectedOwnedKeys,
                enabled = !applying,
                onChecked = { onSelectInstitution(reference, it) }
            )
        }
        if (query.isNotBlank() && queryResults.isEmpty() && directoryResults.isEmpty()) {
            Text(
                "No matching institution found. Try another name.",
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            "Suggestions are ranked by transaction-message count. Search to add an institution that wasn’t detected.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onContinue,
            enabled = !applying,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(15.dp)
        ) {
            Text(if (applying) "Saving choices…" else "Continue with selected")
        }
    }
}

private fun institutionType(option: FinancialInstitutionOption): String =
    when (option.reference.accountTypeHint) {
        AccountType.BANK -> "Bank"
        AccountType.MOBILE_WALLET -> "Wallet"
        else -> "Savings"
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
) {
    PesaCard {
        Row(
            modifier = Modifier.fillMaxWidth()
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = onChecked
                )
                .testTag("detected-account-$key")
                .padding(horizontal = 12.dp, vertical = 9.dp)
                .heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                icon = when (type) {
                    "Bank" -> Icons.Rounded.AccountBalance
                    "Wallet" -> Icons.Rounded.Wallet
                    else -> Icons.Rounded.Savings
                },
                size = 38.dp
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                val supporting = buildList {
                    identifierHint?.let(::add)
                    messageCount?.let { add("$it messages") }
                    if (isEmpty()) add("Possible account · detected from messages")
                }.joinToString(" · ")
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}
