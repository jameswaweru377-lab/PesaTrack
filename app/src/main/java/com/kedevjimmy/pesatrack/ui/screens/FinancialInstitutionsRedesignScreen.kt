package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.FinancialInstitutionDirectory
import com.kedevjimmy.pesatrack.domain.model.FinancialInstitutionOption
import com.kedevjimmy.pesatrack.ingestion.OwnedInstitutionActivityRefresher
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PRIMARY_WALLET_KEY = "SAFARICOM_MPESA"

@Composable
fun FinancialInstitutionsRedesignScreen(scaffoldPadding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { FinancialUnderstandingStore.get(context) }
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var owned by remember { mutableStateOf(setOf(PRIMARY_WALLET_KEY)) }
    var identifiers by remember { mutableStateOf(emptyMap<String, String>()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<FinancialInstitutionOption?>(null) }

    LaunchedEffect(Unit) {
        try {
            val selections = withContext(Dispatchers.IO) { store.ownedInstitutionSelections() }
            owned = selections.mapNotNull { it.providerKey }.toSet() + PRIMARY_WALLET_KEY
            identifiers = selections.associate { it.providerKey to it.identifier.orEmpty() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = "Could not load your saved institutions. Try again later."
        } finally {
            loading = false
        }
    }

    fun persist(option: FinancialInstitutionOption, nextSelected: Boolean, newIdentifier: String?) {
        val key = option.reference.providerKey ?: return
        if (saving || loading) return
        saving = true
        error = null
        scope.launch {
            val nextKeys = if (nextSelected) owned + key else owned - key
            try {
                withContext(Dispatchers.IO) {
                    if (nextSelected) store.setInstitutionOwned(option.reference, newIdentifier?.trim()?.takeIf { it.isNotBlank() })
                    else store.markEntityExternal(option.reference.entityKey)
                }
                // Commit the displayed selection as soon as the local write succeeds.
                owned = nextKeys
                identifiers = if (nextSelected) identifiers + (key to newIdentifier.orEmpty())
                    else identifiers - key
                editing = null
                try {
                    withContext(Dispatchers.IO) {
                    val entities = FinancialInstitutionDirectory.options.filter {
                        it.reference.providerKey in nextKeys
                    }.map { it.reference.entityKey }.toSet()
                    OwnedInstitutionActivityRefresher(context).refresh(entities)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    error = "Selection saved, but Activity could not refresh. Please reopen this page to retry."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = "Could not save ${option.reference.displayName}. Please try again."
            } finally {
                saving = false
            }
        }
    }

    val options = FinancialInstitutionDirectory.search(query).filter { option ->
        when (filter) {
            "Selected" -> option.reference.providerKey in owned
            "Banks" -> option.reference.accountTypeHint == AccountType.BANK
            "Wallets" -> option.reference.accountTypeHint == AccountType.MOBILE_WALLET
            "Savings" -> option.reference.accountTypeHint == AccountType.SAVINGS
            else -> true
        }
    }
    val chosen = options.filter { it.reference.providerKey in owned }
    val available = options.filterNot { it.reference.providerKey in owned }

    LazyColumn(modifier = Modifier.padding(scaffoldPadding).background(BrownHomePalette.canvas),
        contentPadding = PaddingValues(horizontal = PesaDesignTokens.screenHorizontalPadding,
            vertical = PesaDesignTokens.screenVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(PesaDesignTokens.sectionGap)) {
        item { InstitutionPickerHeader(saving || loading, onBack) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null,
                    tint = BrownHomePalette.coffee)
                Text("Selections save automatically", modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search bank or wallet") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                shape = RoundedCornerShape(PesaDesignTokens.innerRadius), singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrownHomePalette.coffee,
                    focusedLeadingIconColor = BrownHomePalette.coffee))
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Selected", "Banks", "Wallets", "Savings").size) { index ->
                    val label = listOf("All", "Selected", "Banks", "Wallets", "Savings")[index]
                    FilterChip(selected = filter == label, onClick = { filter = label },
                        label = { Text(if (label == "Selected") "Selected (${owned.size})" else label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrownHomePalette.cream,
                            selectedLabelColor = BrownHomePalette.coffee))
                }
            }
        }
        if (loading || saving) item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 10.dp), strokeWidth = 2.dp)
                Text(if (loading) "Loading accounts…" else "Saving and updating Activity…",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        if (error != null) item {
            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        if (!loading && chosen.isNotEmpty()) {
            item { Text("Selected accounts", style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                color = BrownHomePalette.coffee) }
            item { InstitutionOptionGroup(chosen, owned, saving, identifiers,
                onToggle = { option, next -> persist(option, next, identifiers[option.reference.providerKey]) },
                onEdit = { editing = it }) }
        }
        if (!loading && available.isNotEmpty()) {
            item { Text("More institutions", style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                color = BrownHomePalette.coffee) }
            item { InstitutionOptionGroup(available, owned, saving, identifiers,
                onToggle = { option, next -> persist(option, next, identifiers[option.reference.providerKey]) },
                onEdit = { editing = it }) }
        }
        if (!loading && options.isEmpty()) item {
            Text("No institutions match your search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Text("Account numbers are optional here. Full account details may be required when importing a bank PDF.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    val editingOption = editing
    if (editingOption != null) {
        var draft by remember(editingOption) {
            mutableStateOf(identifiers[editingOption.reference.providerKey].orEmpty())
        }
        AlertDialog(onDismissRequest = { if (!saving) editing = null },
            title = { Text("Account identifier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(compactAccountLabel(editingOption.reference.displayName))
                    OutlinedTextField(draft, { draft = it }, singleLine = true,
                        label = { Text(if (editingOption.reference.providerKey == PRIMARY_WALLET_KEY)
                            "Your M-Pesa number" else "Account number") })
                    Text("Saved on this device to help identify your own transfers.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }, confirmButton = {
                TextButton(enabled = !saving, onClick = { persist(editingOption, true, draft) }) {
                    Text("Save")
                }
            }, dismissButton = {
                TextButton(enabled = !saving, onClick = { editing = null }) { Text("Cancel") }
            })
    }
}

@Composable
private fun InstitutionPickerHeader(busy: Boolean, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, enabled = !busy) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Text("Add account", modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif, color = BrownHomePalette.coffee)
        TextButton(onClick = onBack, enabled = !busy) {
            Text("Done", color = BrownHomePalette.coffee)
        }
    }
}

@Composable
private fun InstitutionOptionGroup(
    options: List<FinancialInstitutionOption>, owned: Set<String>, busy: Boolean,
    identifiers: Map<String, String>, onToggle: (FinancialInstitutionOption, Boolean) -> Unit,
    onEdit: (FinancialInstitutionOption) -> Unit
) {
    Surface(shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
        color = Color.White,
        border = BorderStroke(PesaDesignTokens.cardOutlineWidth, BrownHomePalette.outline)) {
        Column(Modifier.padding(horizontal = 12.dp)) {
            options.forEachIndexed { index, option ->
                InstitutionOptionRow(option, option.reference.providerKey in owned, busy,
                    identifiers[option.reference.providerKey].orEmpty(), onToggle, onEdit)
                if (index != options.lastIndex) HorizontalDivider(color = BrownHomePalette.outline.copy(alpha = .55f))
            }
        }
    }
}

@Composable
private fun InstitutionOptionRow(
    option: FinancialInstitutionOption, selected: Boolean, busy: Boolean, identifier: String,
    onToggle: (FinancialInstitutionOption, Boolean) -> Unit,
    onEdit: (FinancialInstitutionOption) -> Unit
) {
    val locked = option.reference.providerKey == PRIMARY_WALLET_KEY
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
        .clickable(enabled = !busy && !locked) { onToggle(option, !selected) }
        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AccountProviderMark(option.reference.displayName)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(compactAccountLabel(option.reference.displayName), style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = BrownHomePalette.ink)
            Text(when (option.reference.accountTypeHint) {
                AccountType.BANK -> "Bank"
                AccountType.SAVINGS -> "Savings"
                AccountType.MOBILE_WALLET -> "Mobile wallet"
                else -> "Financial account"
            } + if (locked) " · Primary wallet" else if (identifier.isNotBlank()) " · Details saved" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected && option.requiresIdentifier) IconButton(onClick = { onEdit(option) },
            enabled = !busy) { Icon(Icons.Rounded.Edit, contentDescription = "Edit account number") }
        Checkbox(checked = selected, colors = CheckboxDefaults.colors(
            checkedColor = BrownHomePalette.coffee),
            onCheckedChange = if (busy || locked) null else {
            { onToggle(option, it) }
        })
    }
}
