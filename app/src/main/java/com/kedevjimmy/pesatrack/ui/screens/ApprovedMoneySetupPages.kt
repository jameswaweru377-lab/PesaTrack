package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Savings
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.FinancialInstitutionOption
import com.kedevjimmy.pesatrack.ui.components.PesaCard

@Composable
internal fun ApprovedAnalysisPage(
    importing: Boolean,
    progressStep: Int,
    reviewMode: Boolean,
    onContinue: () -> Unit
) {
    val completed = progressStep.coerceAtLeast(0)
    val progress = when {
        reviewMode -> 1f
        completed >= 4 -> .92f
        completed == 3 -> .76f
        completed == 2 -> .58f
        completed == 1 -> .36f
        else -> .18f
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PremiumOnboardingHero("analysis")
        Text(
            "Building your money picture",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "We're turning your selected financial messages into a clear starting picture. Your messages stay on this phone.",
            modifier = Modifier.padding(horizontal = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Analyzing your messages",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            if (reviewMode) "Ready" else if (importing) "Scanning…" else "Preparing…",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    Modifier.fillMaxWidth().height(7.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Box(
                        Modifier.fillMaxWidth(progress).height(7.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
                ApprovedAnalysisRow(
                    "Reading selected messages",
                    complete = progressStep > 0 || reviewMode,
                    active = progressStep == 0 && !reviewMode
                )
                ApprovedAnalysisRow(
                    "Finding banks and wallets",
                    complete = reviewMode || progressStep >= 3,
                    active = progressStep in 1..2 && !reviewMode
                )
                ApprovedAnalysisRow(
                    "Preparing your review",
                    complete = reviewMode,
                    active = progressStep >= 3 && !reviewMode
                )
            }
        }
        Text(
            "This can take a little while. Review every suggested account before adding it.",
            modifier = Modifier.padding(horizontal = 10.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(5.dp))
        Button(
            onClick = onContinue,
            enabled = reviewMode,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Review detected institutions", fontWeight = FontWeight.Bold)
        }
        Text(
            if (reviewMode) "Analysis complete — ready for your review"
            else "Stay on this screen while scanning",
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ApprovedAnalysisRow(label: String, complete: Boolean, active: Boolean) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        when {
            complete -> Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
            active -> androidx.compose.material3.CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            else -> Surface(Modifier.padding(4.dp).size(8.dp), shape = CircleShape, color = MaterialTheme.colorScheme.outlineVariant) {}
        }
        Text(label, Modifier.padding(start = 9.dp), color = if (active || complete) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
internal fun ApprovedMpesaServicesPage(
    detectedServices: List<FinancialInstitutionOption>,
    allSupportedServices: List<FinancialInstitutionOption>,
    activityCounts: Map<String, Int>,
    selectedProviderKeys: Set<String>,
    saving: Boolean,
    error: String?,
    onToggle: (String, Boolean) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val detectedKeys = detectedServices.map { it.reference.entityKey }.toSet()
    val searched = remember(query, detectedKeys, allSupportedServices) {
        if (query.isBlank()) emptyList() else allSupportedServices.filter { option ->
            option.reference.entityKey !in detectedKeys && option.reference.displayName.contains(query, ignoreCase = true)
        }.take(8)
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CenteredSetupHero(Icons.Rounded.Wallet, "M‑Pesa services we found", "You selected M‑Pesa. These Safaricom services appeared in your messages—choose the ones you use.")
        Text("Detected Safaricom services", modifier = Modifier.padding(top = 2.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        if (detectedServices.isEmpty()) {
            PesaCard { Text("No supported Safaricom services were identified in these messages.", Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        detectedServices.forEach { option ->
            val provider = option.reference.providerKey ?: return@forEach
            MpesaServiceCard(option, activityCounts[option.reference.entityKey], provider in selectedProviderKeys) { onToggle(provider, it) }
        }
        if (searchOpen) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(50) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                placeholder = { Text("Search supported Safaricom services") }
            )
            searched.forEach { option ->
                val provider = option.reference.providerKey ?: return@forEach
                MpesaServiceCard(option, null, provider in selectedProviderKeys) { onToggle(provider, it) }
            }
            if (query.isNotBlank() && searched.isEmpty()) Text("No supported service matches that search.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        } else {
            TextButton(onClick = { searchOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("＋ Search for another Safaricom service") }
        }
        Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f)) {
            Text("These are separate M‑Pesa services, not extra banks. You can change your choices later.", Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = onContinue, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(15.dp)) { Text(if (saving) "Saving…" else "Continue") }
        TextButton(onClick = onSkip, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("Skip service choices") }
    }
}

@Composable
private fun MpesaServiceCard(option: FinancialInstitutionOption, messageCount: Int?, selected: Boolean, onSelected: (Boolean) -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier.fillMaxWidth().heightIn(min = 58.dp).background(if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .28f) else MaterialTheme.colorScheme.surface, RoundedCornerShape(15.dp)).border(1.dp, borderColor, RoundedCornerShape(15.dp)).clickable(role = Role.Checkbox) { onSelected(!selected) }.padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ApprovedMonogram(option.reference.displayName, selected)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(option.reference.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(messageCount?.let { "Found in $it messages" } ?: "Supported Safaricom service", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            Modifier.size(width = 34.dp, height = 20.dp)
                .background(if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = if (selected) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Surface(Modifier.padding(horizontal = 2.dp).size(16.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface) {}
        }
    }
}

@Composable
internal fun CenteredSetupHero(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, height: Int = 92) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(height.dp), contentAlignment = Alignment.Center) {
            Surface(Modifier.size(66.dp), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        Text(description, Modifier.padding(top = 7.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun ApprovedMonogram(name: String, selected: Boolean = false) {
    val letters = remember(name) {
        name.split(Regex("\\s+")).mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase().ifBlank { "₭" }
    }
    Surface(Modifier.size(34.dp), shape = RoundedCornerShape(12.dp), color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer) {
        Box(contentAlignment = Alignment.Center) { Text(letters, color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
    }
}

