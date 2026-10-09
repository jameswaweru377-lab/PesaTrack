package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.FinancialInstitutionDirectory
import com.kedevjimmy.pesatrack.domain.model.FinancialInstitutionOption
import com.kedevjimmy.pesatrack.ingestion.OwnedInstitutionActivityRefresher
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FinancialInstitutionsScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val understandingStore =
        remember {
            FinancialUnderstandingStore
                .get(context)
        }
    val scope =
        rememberCoroutineScope()
    var query by remember {
        mutableStateOf("")
    }
    var selectedProviderKeys by remember {
        mutableStateOf<Set<String>>(
            emptySet()
        )
    }
    var identifiers by remember {
        mutableStateOf<Map<String, String>>(
            emptyMap()
        )
    }
    var initialProviderKeys by remember {
        mutableStateOf<Set<String>>(
            emptySet()
        )
    }
    var loading by remember {
        mutableStateOf(true)
    }
    var saving by remember {
        mutableStateOf(false)
    }
    var error by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {
        val selections =
            withContext(Dispatchers.IO) {
                understandingStore
                    .ownedInstitutionSelections()
            }
        val byProvider =
            selections.associateBy {
                it.providerKey
            }
        val owned =
            byProvider.keys +
                M_PESA_PROVIDER_KEY

        initialProviderKeys = owned
        selectedProviderKeys = owned
        identifiers =
            byProvider.mapValues {
                (_, selection) ->
                selection.identifier
                    .orEmpty()
            }
        loading = false
    }

    val visibleOptions =
        remember(query) {
            FinancialInstitutionDirectory
                .search(query)
        }

    LazyColumn(
        modifier =
            Modifier.padding(
                scaffoldPadding
            ),
        contentPadding =
            PaddingValues(
                horizontal = 16.dp,
                vertical = 14.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {
        item {
            DetailPageHeader(
                title =
                    "My financial institutions",
                subtitle =
                    "Choose only accounts that belong to you.",
                onBack = onBack
            )
        }

        item {
            PesaCard {
                Column(
                    modifier =
                        Modifier.padding(
                            16.dp
                        )
                ) {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded
                                .AccountBalance,
                            contentDescription =
                                null,
                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )
                        Text(
                            "Choose your accounts",
                            modifier =
                                Modifier.padding(
                                    start = 10.dp
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                    Text(
                        "Choose banks and money services you use. The last four digits of an account number can help match transfers. A bank mentioned in a payment may belong to someone else.",
                        modifier =
                            Modifier.padding(
                                top = 8.dp
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                },
                modifier =
                    Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription =
                            null
                    )
                },
                label = {
                    Text(
                        "Search bank or wallet"
                    )
                }
            )
        }

        if (loading) {
            item {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 24.dp
                            ),
                    horizontalArrangement =
                        Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                24.dp
                            )
                    )
                }
            }
        } else {
            var lastSection: String? =
                null
            visibleOptions
                .forEach { option ->
                    if (
                        option.section !=
                        lastSection
                    ) {
                        item(
                            key =
                                "section:" +
                                    option.section
                        ) {
                            Text(
                                option.section,
                                modifier =
                                    Modifier.padding(
                                        top = 8.dp,
                                        bottom = 2.dp
                                    ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelLarge,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }
                        lastSection =
                            option.section
                    }

                    item(
                        key =
                            option.reference
                                .entityKey
                    ) {
                        InstitutionOwnershipRow(
                            option = option,
                            selected =
                                option.reference
                                    .providerKey in
                                    selectedProviderKeys,
                            identifier =
                                option.reference
                                    .providerKey
                                    ?.let {
                                        identifiers[
                                            it
                                        ]
                                    }
                                    .orEmpty(),
                            locked =
                                option.reference
                                    .providerKey ==
                                    M_PESA_PROVIDER_KEY,
                            onSelectedChange = {
                                    selected ->
                                val providerKey =
                                    option.reference
                                        .providerKey
                                if (
                                    providerKey !=
                                    null
                                ) {
                                    selectedProviderKeys =
                                        if (selected) {
                                            selectedProviderKeys +
                                                providerKey
                                        } else {
                                            selectedProviderKeys -
                                                providerKey
                                        }
                                }
                            },
                            onIdentifierChange = {
                                    value ->
                                val providerKey =
                                    option.reference
                                        .providerKey
                                if (
                                    providerKey !=
                                    null
                                ) {
                                    identifiers =
                                        identifiers +
                                            (
                                                providerKey to
                                                    value
                                                )
                                }
                            }
                        )
                    }
                }
        }

        error?.let {
            message ->
            item {
                Text(
                    message,
                    color =
                        MaterialTheme
                            .colorScheme
                            .error,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }

        item {
            Button(
                onClick = {
                    if (saving) {
                        return@Button
                    }
                    saving = true
                    error = null
                    scope.launch {
                        try {
                            withContext(
                                Dispatchers.IO
                            ) {
                                val allOptions =
                                    FinancialInstitutionDirectory
                                        .options
                                allOptions
                                    .forEach {
                                        option ->
                                        val providerKey =
                                            option.reference
                                                .providerKey
                                                ?: return@forEach
                                        if (providerKey in selectedProviderKeys || (M_PESA_PROVIDER_KEY in selectedProviderKeys && providerKey in setOf("NCBA_MSHWARI_SAVINGS", "NCBA_MSHWARI_LOCK_SAVINGS", "KCB_MPESA_SAVINGS"))) {
                                            understandingStore
                                                .setInstitutionOwned(
                                                    reference =
                                                        option.reference,
                                                    identifier =
                                                        identifiers[
                                                            providerKey
                                                        ]
                                                            ?.trim()
                                                            ?.takeIf {
                                                                it.isNotBlank()
                                                            }
                                                )
                                        } else if (
                                            providerKey in
                                            initialProviderKeys
                                        ) {
                                            understandingStore
                                                .markEntityExternal(
                                                    option.reference
                                                        .entityKey
                                                )
                                        }
                                    }

                                val selectedEntityKeys =
                                    allOptions
                                        .filter {
                                            it.reference
                                                .providerKey in
                                                selectedProviderKeys
                                        }
                                        .map {
                                            it.reference
                                                .entityKey
                                        }
                                        .toSet()

                                // Re-run the user's already-authorized imported
                                // history immediately. Existing Activity rows
                                // are promoted/demoted using the new ownership
                                // evidence, and the ledger update bus refreshes
                                // Activity/Home before the user returns there.
                                OwnedInstitutionActivityRefresher(
                                    context
                                ).refresh(
                                    selectedEntityKeys =
                                        selectedEntityKeys
                                )
                            }
                            onBack()
                        } catch (
                            failure: Exception
                        ) {
                            error =
                                failure.message
                                    ?: "Could not save your institutions."
                        } finally {
                            saving = false
                        }
                    }
                },
                enabled =
                    !loading &&
                        !saving,
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(
                        16.dp
                    )
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                18.dp
                            ),
                        strokeWidth =
                            2.dp
                    )
                    Text(
                        "Saving & updating Activity…",
                        modifier =
                            Modifier.padding(
                                start = 8.dp
                            )
                    )
                } else {
                    Text(
                        "Save my institutions"
                    )
                }
            }
        }
    }
}

@Composable
private fun InstitutionOwnershipRow(
    option: FinancialInstitutionOption,
    selected: Boolean,
    identifier: String,
    locked: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onIdentifierChange: (String) -> Unit
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = !locked
                ) {
                    onSelectedChange(
                        !selected
                    )
                },
        shape =
            RoundedCornerShape(
                16.dp
            ),
        color =
            MaterialTheme
                .colorScheme
                .surface
    ) {
        Column(
            modifier =
                Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                )
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selected,
                    onCheckedChange =
                        if (locked) {
                            null
                        } else {
                            onSelectedChange
                        }
                )
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(
                                start = 6.dp
                            )
                ) {
                    Text(
                        option.reference
                            .displayName,
                        style =
                            MaterialTheme
                                .typography
                                .titleSmall,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                    Text(
                        when (
                            option.reference
                                .accountTypeHint
                        ) {
                            AccountType.BANK ->
                                "Bank"
                            AccountType.MOBILE_WALLET ->
                                "Mobile wallet"
                            AccountType.SAVINGS ->
                                "Savings"
                            else ->
                                "Financial account"
                        } +
                            if (locked) {
                                " • primary wallet"
                            } else {
                                ""
                            },
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            if (
                selected &&
                option.requiresIdentifier
            ) {
                OutlinedTextField(
                    value = identifier,
                    onValueChange =
                        onIdentifierChange,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 48.dp,
                                top = 6.dp
                            ),
                    singleLine = true,
                    label = {
                        Text(
                            if (
                                option.reference
                                    .providerKey ==
                                M_PESA_PROVIDER_KEY
                            ) {
                                "Your M-Pesa number"
                            } else {
                                "Account number / unique last 4+ digits"
                            }
                        )
                    },
                    supportingText = {
                        Text(
                            "Optional, but improves own-transfer matching."
                        )
                    }
                )
            }
        }
    }
}

private const val M_PESA_PROVIDER_KEY =
    "SAFARICOM_MPESA"
