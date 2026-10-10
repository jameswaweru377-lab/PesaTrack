package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.DiscoveredFinancialEntity
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.ingestion.HistoricalImportResult
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard

/**
 * Approved PesaTrack 2.0 account checklist.
 * All items start unchecked. Detection never means ownership.
 */
@Composable
internal fun ReviewDiscoveryPage(
    sources: List<DetectedSetupSource>,
    ownershipCandidates: List<DiscoveredFinancialEntity>,
    selectedOwnedKeys: Set<String>,
    importResult: HistoricalImportResult?,
    applying: Boolean,
    onToggleAccount: (String, Boolean) -> Unit,
    onContinue: () -> Unit,
    onAddMissingInstitution: () -> Unit = {}
) {
    val sourceKeys = sources.map { it.reference.entityKey }.toSet()
    val sourceProviders = sources.mapNotNull { it.reference.providerKey }.toSet()
    val otherCandidates = ownershipCandidates.filter { entity ->
        entity.entityKey !in sourceKeys &&
            (entity.providerKey == null || entity.providerKey !in sourceProviders)
    }
    val optionCount = sources.size + otherCandidates.size
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "We found these financial sources",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Select the ones you actually use. You can edit or add more later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (optionCount == 0) {
            PesaCard {
                Text(
                    if (importResult == null)
                        "No accounts detected yet. You can add one later or continue without scanning."
                    else
                        "No accounts could be confidently identified. Add an account if you wish, or continue.",
                    Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            sources.forEach { source ->
                val key = source.reference.entityKey
                DetectedAccountCheckRow(
                    key = key,
                    name = source.name,
                    type = source.type,
                    checked = key in selectedOwnedKeys,
                    enabled = !applying,
                    onChecked = { onToggleAccount(key, it) }
                )
            }
            otherCandidates.forEach { candidate ->
                val key = candidate.entityKey
                val type = when (candidate.accountTypeHint) {
                    AccountType.BANK.name -> "Bank"
                    AccountType.MOBILE_WALLET.name -> "Wallet"
                    else -> "Savings"
                }
                DetectedAccountCheckRow(
                    key = key,
                    name = candidate.displayName,
                    type = type,
                    checked = key in selectedOwnedKeys,
                    enabled = !applying,
                    onChecked = { onToggleAccount(key, it) }
                )
            }
        }
        OutlinedButton(
            onClick = onAddMissingInstitution,
            enabled = !applying,
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("+  Add another bank or service") }
        Button(
            onClick = onContinue,
            enabled = !applying,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (applying) CircularProgressIndicator(modifier = Modifier.size(20.dp))
            else Text("Continue")
        }
    }
}

@Composable
internal fun DetectedAccountCheckRow(
    key: String,
    name: String,
    type: String,
    checked: Boolean,
    enabled: Boolean,
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
                .testTag("detected-account-${key}")
                .padding(horizontal = 12.dp, vertical = 8.dp)
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
                Text(
                    "Detected from messages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}
