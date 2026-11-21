package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.FinancialInstitutionDirectory
import com.kedevjimmy.pesatrack.ingestion.HistoricalImportResult
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens

/** Top five are ranked by actual financial transactions, never by promotional SMS. */
@Composable
internal fun ReviewDiscoveryPage(
    sources: List<DetectedSetupSource>,
    selectedOwnedKeys: Set<String>,
    mpesaIdentifierHint: String?,
    importResult: HistoricalImportResult?,
    applying: Boolean,
    selectionsLocked: Boolean = false,
    error: String? = null,
    onToggleAccount: (String, Boolean) -> Unit,
    onSelectInstitution: (FinancialAccountReference, Boolean) -> Unit,
    onContinue: () -> Unit,
    onContinueWithAccountsOnly: (() -> Unit)? = null
) {
    var query by rememberSaveable { mutableStateOf("") }
    val topFive = remember(sources) { OnboardingInstitutionPresentationPolicy.topFive(sources) }
    val sourceKeys = remember(sources) { sources.mapTo(mutableSetOf()) { it.reference.entityKey } }
    val matchingDetected = remember(query, sources, topFive) {
        if (query.isBlank()) topFive else sources.filter {
            it.name.contains(query, ignoreCase = true)
        }
    }
    val directory = remember(query, sourceKeys) {
        if (query.isBlank()) emptyList() else FinancialInstitutionDirectory.search(query)
            .filterNot { it.reference.entityKey in sourceKeys }
            .filterNot { OnboardingInstitutionPresentationPolicy.isSafaricomProduct(it.reference.providerKey) }
            .distinctBy { it.reference.entityKey }.take(10)
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        OnboardingBrandHeader()
        Spacer(Modifier.height(15.dp))
        Text("Which accounts are yours?", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = PremiumVisualTokens.darkText)
        Text("Select only the services you personally use.",
            style = MaterialTheme.typography.bodyMedium, color = PremiumVisualTokens.darkMuted)
        TextField(
            value = query, onValueChange = { query = it.take(60) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp)
                .testTag("onboarding-institution-search"),
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            placeholder = { Text("Search banks and money services") },
            singleLine = true, enabled = !applying && !selectionsLocked,
            shape = RoundedCornerShape(17.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = PremiumVisualTokens.darkCard,
                unfocusedContainerColor = PremiumVisualTokens.darkCard,
                focusedTextColor = PremiumVisualTokens.darkText,
                unfocusedTextColor = PremiumVisualTokens.darkText,
                focusedPlaceholderColor = PremiumVisualTokens.darkMuted,
                unfocusedPlaceholderColor = PremiumVisualTokens.darkMuted,
                focusedLeadingIconColor = PremiumVisualTokens.darkMuted,
                unfocusedLeadingIconColor = PremiumVisualTokens.darkMuted,
                cursorColor = PremiumVisualTokens.darkCoffee,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        matchingDetected.forEach { source ->
            val mpesa = source.reference.providerKey == "SAFARICOM_MPESA"
            DetectedInstitutionCard(
                source.name,
                if (mpesa) "Mobile money" else source.type,
                source.reference.entityKey,
                source.messageCount,
                if (mpesa) OnboardingInstitutionPresentationPolicy.maskIdentifier(mpesaIdentifierHint) else null,
                source.reference.entityKey in selectedOwnedKeys, !applying && !selectionsLocked
            ) { onToggleAccount(source.reference.entityKey, it) }
        }
        directory.forEach { option ->
            val reference = option.reference
            val type = when (reference.accountTypeHint) {
                AccountType.BANK -> "Bank"
                AccountType.MOBILE_WALLET -> "Mobile money"
                else -> "Savings"
            }
            DetectedInstitutionCard(reference.displayName, type, reference.entityKey,
                null, null, reference.entityKey in selectedOwnedKeys, !applying && !selectionsLocked) {
                onSelectInstitution(reference, it)
            }
        }
        if (matchingDetected.isEmpty() && directory.isEmpty()) {
            Text(if (query.isBlank()) "No transaction sources found. Search for the bank or wallet you use."
                else "No matching institution. Try another name.",
                color = PremiumVisualTokens.darkMuted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
        }
        Text("${selectedOwnedKeys.size} selected · Only confirm accounts you use",
            Modifier.testTag("onboarding-selected-institution-count"),
            style = MaterialTheme.typography.bodySmall, color = PremiumVisualTokens.darkMuted)
        if (selectionsLocked) {
            Text("Your account choices are saved. Finish importing messages or continue with accounts only.",
                style = MaterialTheme.typography.bodySmall, color = PremiumVisualTokens.darkMuted)
        }
        error?.let { Text(it, color = PremiumVisualTokens.darkMoneyOut,
            style = MaterialTheme.typography.bodySmall) }
        Button(
            onClick = onContinue, enabled = !applying && selectedOwnedKeys.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PremiumVisualTokens.darkCoffee,
                contentColor = PremiumVisualTokens.ink,
                disabledContainerColor = PremiumVisualTokens.darkSurface,
                disabledContentColor = PremiumVisualTokens.darkMuted
            )
        ) { Text(if (applying) "Saving accounts…" else "Confirm my accounts", fontWeight = FontWeight.Bold) }
        if (onContinueWithAccountsOnly != null) {
            TextButton(
                onClick = onContinueWithAccountsOnly,
                enabled = !applying,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Continue with accounts only") }
        }
    }
}

@Composable
internal fun DetectedAccountCheckRow(
    key: String, name: String, type: String, checked: Boolean, enabled: Boolean,
    messageCount: Int? = null, identifierHint: String? = null,
    onChecked: (Boolean) -> Unit
) = DetectedInstitutionCard(name, type, key, messageCount, identifierHint, checked, enabled, onChecked)

@Composable
private fun DetectedInstitutionCard(
    name: String, type: String, key: String, messageCount: Int?,
    identifierHint: String?, checked: Boolean, enabled: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 77.dp)
            .background(if (checked) PremiumVisualTokens.darkSurface else PremiumVisualTokens.darkCard,
                RoundedCornerShape(17.dp))
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onChecked)
            .testTag("detected-account-$key")
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(Modifier.size(43.dp), shape = RoundedCornerShape(13.dp),
            color = PremiumVisualTokens.darkCanvas) {
            Box(contentAlignment = Alignment.Center) {
                Icon(if (type.contains("Bank", ignoreCase = true)) Icons.Rounded.AccountBalance
                    else Icons.Rounded.Wallet, null, Modifier.size(23.dp),
                    tint = PremiumVisualTokens.darkCoffee)
            }
        }
        Column(Modifier.weight(1f).padding(start = 13.dp)) {
            Text(onboardingShortBankName(name), color = PremiumVisualTokens.darkText,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(listOfNotNull(type, messageCount?.let { "$it transactions found" }, identifierHint)
                .joinToString(" · "), color = PremiumVisualTokens.darkMuted,
                style = MaterialTheme.typography.labelSmall)
        }
        Icon(if (checked) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null, tint = if (checked) PremiumVisualTokens.darkCoffee
                else PremiumVisualTokens.darkMuted, modifier = Modifier.size(26.dp))
    }
}

private fun onboardingShortBankName(name: String): String =
    name.removeSuffix(" Bank Kenya").let { shortened ->
        if (shortened != name) "$shortened Bank"
        else name.removeSuffix(" of Kenya").removeSuffix(" Kenya")
    }
