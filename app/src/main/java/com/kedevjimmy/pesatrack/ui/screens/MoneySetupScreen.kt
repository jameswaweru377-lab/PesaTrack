package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.RulePreset
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import java.math.RoundingMode

private enum class MatcherMode {
    MPESA_NAME,
    NOTIFICATION
}

private enum class PayWindowChoice {
    ANY_DAY,
    MONTH_END,
    CUSTOM
}

private data class PresetConfig(
    val label: String,
    val category: TransactionCategory,
    val direction: MoneyDirection,
    val startsPayCycle: Boolean = false
)

@Composable
fun MoneySetupScreen(
    scaffoldPadding: PaddingValues,
    initialSetup: Boolean,
    onDone: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val ruleStore = remember { RuleStore.get(context) }
    val accountStore = remember { AccountStore.get(context) }
    val transactionStore = remember { TransactionStore.get(context) }
    val payCycleStore = remember { PayCycleStore.get(context) }

    var rules by remember { mutableStateOf(ruleStore.listRules()) }
    var accounts by remember { mutableStateOf(accountStore.listAccounts()) }

    var preset by remember { mutableStateOf(RulePreset.SALARY) }
    var matcherMode by remember { mutableStateOf(MatcherMode.NOTIFICATION) }
    var customLabel by remember { mutableStateOf("") }
    var counterparty by remember { mutableStateOf("") }
    var notificationTitle by remember { mutableStateOf("") }
    var messagePhrase by remember { mutableStateOf("") }
    var plannedAmount by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull { it.type == AccountType.BANK }?.id) }
    var linkedAccountId by remember { mutableStateOf<String?>(null) }
    var customCategory by remember { mutableStateOf(TransactionCategory.OTHER) }
    var customDirection by remember { mutableStateOf(MoneyDirection.EXPENSE) }
    var payWindow by remember { mutableStateOf(PayWindowChoice.ANY_DAY) }
    var customWindowStart by remember { mutableStateOf("24") }
    var customWindowEnd by remember { mutableStateOf("1") }
    var error by remember { mutableStateOf<String?>(null) }

    var showAddAccount by remember { mutableStateOf(false) }
    var newAccountName by remember { mutableStateOf("") }
    var newAccountType by remember { mutableStateOf(AccountType.BANK) }
    var editingAccountId by remember { mutableStateOf<String?>(null) }
    var accountIdentifierDraft by remember { mutableStateOf("") }
    var accountOpeningBalanceDraft by remember { mutableStateOf("") }

    fun config(): PresetConfig = presetConfig(
        preset = preset,
        customCategory = customCategory,
        customDirection = customDirection
    )

    fun reload() {
        rules = ruleStore.listRules()
        accounts = accountStore.listAccounts()
    }

    fun beginAccountEdit(account: MoneyAccount) {
        editingAccountId = account.id
        accountIdentifierDraft = account.identifier.orEmpty()
        accountOpeningBalanceDraft = account.openingBalanceMinor
            ?.let(::formatEditableMoney)
            .orEmpty()
        error = null
    }

    fun saveAccountDetails(account: MoneyAccount) {
        error = null
        val openingBalanceMinor = accountOpeningBalanceDraft
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let(::parseEditableMoneyMinor)

        if (
            accountOpeningBalanceDraft.isNotBlank() &&
            openingBalanceMinor == null
        ) {
            error = "Enter the opening balance as a number."
            return
        }

        runCatching {
            accountStore.updateDetails(
                id = account.id,
                identifier = if (account.type == AccountType.MOBILE_WALLET) {
                    accountIdentifierDraft
                } else {
                    account.identifier
                },
                openingBalanceMinor = openingBalanceMinor
            )
        }.onSuccess {
            transactionStore.reconcileOwnAccountTransfers()
            reload()
            editingAccountId = null
            accountIdentifierDraft = ""
            accountOpeningBalanceDraft = ""
        }.onFailure {
            error = it.message
        }
    }

    fun choosePreset(newPreset: RulePreset) {
        preset = newPreset
        matcherMode = if (newPreset == RulePreset.SALARY) {
            MatcherMode.NOTIFICATION
        } else {
            MatcherMode.MPESA_NAME
        }

        selectedAccountId = when (newPreset) {
            RulePreset.SALARY ->
                accounts.firstOrNull { it.type == AccountType.BANK }?.id
            else ->
                accounts.firstOrNull { it.id == AccountStore.DEFAULT_MPESA_ID }?.id
        }

        linkedAccountId = if (newPreset == RulePreset.OWN_TRANSFER) {
            accounts.firstOrNull {
                it.id != selectedAccountId && it.type != AccountType.CASH
            }?.id
        } else {
            null
        }

        plannedAmount = ""
        customLabel = ""
        error = null
    }

    fun saveRule() {
        error = null
        val ruleConfig = config()

        val amountMinor = plannedAmount
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let { value ->
                runCatching {
                    value.replace(",", "")
                        .toBigDecimal()
                        .movePointRight(2)
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValueExact()
                }.getOrNull()
            }

        if (plannedAmount.isNotBlank() && amountMinor == null) {
            error = "Enter the expected amount as a number."
            return
        }

        val conditionsValid = when (matcherMode) {
            MatcherMode.MPESA_NAME -> counterparty.isNotBlank()
            MatcherMode.NOTIFICATION ->
                notificationTitle.isNotBlank() || messagePhrase.isNotBlank()
        }

        if (!conditionsValid) {
            error = if (matcherMode == MatcherMode.MPESA_NAME) {
                "Enter the M-Pesa name or merchant text to recognize."
            } else {
                "Enter the notification sender/title or a phrase inside the message."
            }
            return
        }

        if (preset == RulePreset.OWN_TRANSFER) {
            if (selectedAccountId == null || linkedAccountId == null) {
                error = "Choose both accounts for this transfer."
                return
            }
            if (selectedAccountId == linkedAccountId) {
                error = "The transfer needs two different accounts."
                return
            }
        }

        val window = when (payWindow) {
            PayWindowChoice.ANY_DAY -> null to null
            PayWindowChoice.MONTH_END -> 24 to 1
            PayWindowChoice.CUSTOM -> {
                val start = customWindowStart.toIntOrNull()
                val end = customWindowEnd.toIntOrNull()
                if (start == null || end == null || start !in 1..31 || end !in 1..31) {
                    error = "Pay-window days must be between 1 and 31."
                    return
                }
                start to end
            }
        }

        val display = customLabel.trim().takeIf { it.isNotBlank() }
            ?: ruleConfig.label

        ruleStore.save(
            name = display,
            counterpartyContains = if (matcherMode == MatcherMode.MPESA_NAME) counterparty else "",
            notificationTitleContains = if (matcherMode == MatcherMode.NOTIFICATION) notificationTitle else "",
            messageContains = if (matcherMode == MatcherMode.NOTIFICATION) messagePhrase else "",
            category = ruleConfig.category,
            direction = ruleConfig.direction,
            planningMonthOffset = 0,
            displayLabel = display,
            preset = preset,
            plannedAmountMinor = amountMinor,
            startsPayCycle = ruleConfig.startsPayCycle,
            accountId = selectedAccountId,
            linkedAccountId = linkedAccountId,
            payWindowStartDay = if (ruleConfig.startsPayCycle) window.first else null,
            payWindowEndDay = if (ruleConfig.startsPayCycle) window.second else null
        )

        transactionStore.reapplyCounterpartyRules(
            ruleStore.listRules(enabledOnly = true)
        )
        payCycleStore.rebuild()
        reload()

        counterparty = ""
        notificationTitle = ""
        messagePhrase = ""
        plannedAmount = ""
        customLabel = ""
    }

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (initialSetup) "Set up your money" else "Smart rules",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "Choose what something is first. Only type the details PesaTrack cannot know.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            SectionHeader("My accounts")
            Spacer(Modifier.height(6.dp))
            PesaCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    accounts.forEach { account ->
                        AccountRow(
                            account = account,
                            onEdit = { beginAccountEdit(account) }
                        )
                        if (editingAccountId == account.id) {
                            AccountDetailsEditor(
                                account = account,
                                identifier = accountIdentifierDraft,
                                openingBalance = accountOpeningBalanceDraft,
                                onIdentifierChanged = {
                                    accountIdentifierDraft = it
                                },
                                onOpeningBalanceChanged = {
                                    accountOpeningBalanceDraft = it
                                },
                                onSave = {
                                    saveAccountDetails(account)
                                },
                                onCancel = {
                                    editingAccountId = null
                                    accountIdentifierDraft = ""
                                    accountOpeningBalanceDraft = ""
                                }
                            )
                        }
                    }

                    if (showAddAccount) {
                        OutlinedTextField(
                            value = newAccountName,
                            onValueChange = { newAccountName = it },
                            label = { Text("Account name") },
                            placeholder = { Text("Example: Equity") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        SelectionField(
                            label = "Account type",
                            selectedLabel = accountTypeLabel(newAccountType),
                            options = AccountType.entries.map { it to accountTypeLabel(it) },
                            onSelected = { newAccountType = it }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    showAddAccount = false
                                    newAccountName = ""
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    runCatching {
                                        accountStore.save(newAccountName, newAccountType)
                                    }.onSuccess { created ->
                                        showAddAccount = false
                                        newAccountName = ""
                                        reload()
                                        beginAccountEdit(created)
                                    }.onFailure {
                                        error = it.message
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Add account")
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showAddAccount = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Text("Add bank or wallet", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }

        item {
            SectionHeader("Teach PesaTrack")
            Spacer(Modifier.height(6.dp))
            PesaCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelectionField(
                        label = "What is this?",
                        selectedLabel = presetConfig(preset, customCategory, customDirection).label,
                        options = rulePresetOptions(),
                        onSelected = ::choosePreset
                    )

                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it },
                        label = { Text("Custom name (optional)") },
                        placeholder = {
                            Text("Default: " + presetConfig(preset, customCategory, customDirection).label)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        "How should PesaTrack recognize it?",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = matcherMode == MatcherMode.MPESA_NAME,
                            onClick = { matcherMode = MatcherMode.MPESA_NAME },
                            label = { Text("M-Pesa name") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = matcherMode == MatcherMode.NOTIFICATION,
                            onClick = { matcherMode = MatcherMode.NOTIFICATION },
                            label = { Text("Notification") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (matcherMode == MatcherMode.MPESA_NAME) {
                        OutlinedTextField(
                            value = counterparty,
                            onValueChange = { counterparty = it },
                            label = { Text("Name contains") },
                            placeholder = { Text("Recipient or merchant name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        OutlinedTextField(
                            value = notificationTitle,
                            onValueChange = { notificationTitle = it },
                            label = { Text("Sender/title contains") },
                            placeholder = { Text("Example: your bank name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = messagePhrase,
                            onValueChange = { messagePhrase = it },
                            label = { Text("Message contains") },
                            placeholder = { Text("Example: employer or transfer phrase") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    if (preset == RulePreset.CUSTOM) {
                        Text("Custom behavior", style = MaterialTheme.typography.titleSmall)
                        CategoryChooser(
                            selected = customCategory,
                            onSelected = { customCategory = it }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                MoneyDirection.EXPENSE to "Expense",
                                MoneyDirection.INCOME to "Income",
                                MoneyDirection.TRANSFER to "Transfer"
                            ).forEach { option ->
                                FilterChip(
                                    selected = customDirection == option.first,
                                    onClick = { customDirection = option.first },
                                    label = { Text(option.second) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    AccountFields(
                        preset = preset,
                        accounts = accounts,
                        selectedAccountId = selectedAccountId,
                        linkedAccountId = linkedAccountId,
                        onAccountSelected = { selectedAccountId = it },
                        onLinkedAccountSelected = { linkedAccountId = it }
                    )

                    val config = config()
                    if (config.direction == MoneyDirection.EXPENSE ||
                        preset == RulePreset.SAVINGS
                    ) {
                        OutlinedTextField(
                            value = plannedAmount,
                            onValueChange = { plannedAmount = it },
                            label = { Text("Expected amount (optional)") },
                            placeholder = { Text("Used to reserve this money in your plan") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    if (config.startsPayCycle) {
                        Text(
                            "Usual payday window (optional)",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "The salary message identifies the salary. This window only helps PesaTrack spot unusual timing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SelectionField(
                            label = "Pay window",
                            selectedLabel = when (payWindow) {
                                PayWindowChoice.ANY_DAY -> "No fixed day"
                                PayWindowChoice.MONTH_END -> "Usually 24th to 1st"
                                PayWindowChoice.CUSTOM -> "Custom range"
                            },
                            options = listOf(
                                PayWindowChoice.ANY_DAY to "No fixed day",
                                PayWindowChoice.MONTH_END to "Usually 24th to 1st",
                                PayWindowChoice.CUSTOM to "Custom range"
                            ),
                            onSelected = { payWindow = it }
                        )

                        if (payWindow == PayWindowChoice.CUSTOM) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customWindowStart,
                                    onValueChange = { customWindowStart = it },
                                    label = { Text("From day") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = customWindowEnd,
                                    onValueChange = { customWindowEnd = it },
                                    label = { Text("To day") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        onClick = ::saveRule,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text("Save rule", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

        item {
            SectionHeader("Saved rules")
            if (rules.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                PesaCard {
                    Text(
                        "No rules yet. One-off transactions will safely appear as Needs review instead of being guessed.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(rules.size) { index ->
            RuleRow(
                rule = rules[index],
                accounts = accounts,
                onDelete = {
                    ruleStore.delete(rules[index].id)
                    payCycleStore.rebuild()
                    reload()
                }
            )
        }

        item {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(if (initialSetup) "Continue to PesaTrack" else "Done")
            }
            if (initialSetup) {
                OutlinedButton(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Set up more rules later")
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: MoneyAccount,
    onEdit: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(
            if (account.type == AccountType.BANK) {
                Icons.Rounded.AccountBalance
            } else {
                Icons.Rounded.Payments
            },
            size = 38.dp
        )
        Column(
            Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(account.name, style = MaterialTheme.typography.titleMedium)
            Text(
                buildString {
                    append(accountTypeLabel(account.type))
                    account.identifier?.let {
                        append(" • ")
                        append(it)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            account.openingBalanceMinor?.let { balance ->
                Text(
                    "Opening balance " + formatDisplayMoney(balance),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Rounded.Edit,
                contentDescription = "Edit account details"
            )
        }
    }
}

@Composable
private fun AccountDetailsEditor(
    account: MoneyAccount,
    identifier: String,
    openingBalance: String,
    onIdentifierChanged: (String) -> Unit,
    onOpeningBalanceChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (account.type == AccountType.MOBILE_WALLET) {
            OutlinedTextField(
                value = identifier,
                onValueChange = onIdentifierChanged,
                label = { Text("Your M-Pesa phone number") },
                placeholder = { Text("0712345678") },
                supportingText = {
                    Text(
                        "Used only to recognize transfers between accounts that belong to you."
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = openingBalance,
            onValueChange = onOpeningBalanceChanged,
            label = { Text("Balance when tracking starts (optional)") },
            placeholder = { Text("0") },
            supportingText = {
                Text(
                    "This is existing money, not income. It gives future reconciliation a starting point."
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save details")
            }
        }
    }
}

@Composable
private fun AccountFields(
    preset: RulePreset,
    accounts: List<MoneyAccount>,
    selectedAccountId: String?,
    linkedAccountId: String?,
    onAccountSelected: (String?) -> Unit,
    onLinkedAccountSelected: (String?) -> Unit
) {
    if (accounts.isEmpty()) return

    val primaryLabel = when (preset) {
        RulePreset.SALARY -> "Salary arrives in"
        RulePreset.OWN_TRANSFER -> "Move money from"
        else -> "Usually paid from"
    }

    SelectionField(
        label = primaryLabel,
        selectedLabel = accounts.firstOrNull { it.id == selectedAccountId }?.name ?: "Choose account",
        options = accounts.map { it.id to it.name },
        onSelected = onAccountSelected
    )

    if (preset == RulePreset.OWN_TRANSFER) {
        SelectionField(
            label = "Move money to",
            selectedLabel = accounts.firstOrNull { it.id == linkedAccountId }?.name ?: "Choose account",
            options = accounts
                .filter { it.id != selectedAccountId }
                .map { it.id to it.name },
            onSelected = onLinkedAccountSelected
        )
        Text(
            "Moving money between your own accounts is tracked, but it is not income or spending.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun <T> SelectionField(
    label: String,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    selectedLabel,
                    modifier = Modifier.weight(1f)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.second) },
                        onClick = {
                            expanded = false
                            onSelected(option.first)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChooser(
    selected: TransactionCategory,
    onSelected: (TransactionCategory) -> Unit
) {
    val categories = listOf(
        TransactionCategory.RENT,
        TransactionCategory.LUNCH,
        TransactionCategory.ELECTRICITY,
        TransactionCategory.INTERNET,
        TransactionCategory.TRANSPORT,
        TransactionCategory.SHOPPING,
        TransactionCategory.FAMILY,
        TransactionCategory.SAVINGS,
        TransactionCategory.OTHER
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        categories.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { item ->
                    FilterChip(
                        selected = selected == item,
                        onClick = { onSelected(item) },
                        label = { Text(item.prettyName()) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RuleRow(
    rule: SmartRule,
    accounts: List<MoneyAccount>,
    onDelete: () -> Unit
) {
    PesaCard {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                when (rule.direction) {
                    MoneyDirection.INCOME -> Icons.Rounded.Payments
                    MoneyDirection.TRANSFER -> Icons.Rounded.SwapHoriz
                    else -> Icons.Rounded.ReceiptLong
                },
                size = 42.dp
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp)
            ) {
                Text(
                    rule.name.ifBlank { rule.displayLabel ?: "Smart rule" },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (rule.counterpartyContains.isNotBlank()) {
                        "M-Pesa name contains “" + rule.counterpartyContains + "”"
                    } else {
                        buildString {
                            if (rule.notificationTitleContains.isNotBlank()) {
                                append("Sender “")
                                append(rule.notificationTitleContains)
                                append("”")
                            }
                            if (rule.messageContains.isNotBlank()) {
                                if (isNotEmpty()) append(" • ")
                                append("Message “")
                                append(rule.messageContains)
                                append("”")
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val account = accounts.firstOrNull { it.id == rule.accountId }?.name
                val linked = accounts.firstOrNull { it.id == rule.linkedAccountId }?.name
                Text(
                    buildString {
                        append(rule.preset.prettyName())
                        if (account != null) {
                            append(" • ")
                            append(account)
                        }
                        if (linked != null) {
                            append(" → ")
                            append(linked)
                        }
                        if (rule.startsPayCycle) {
                            append(" • starts pay cycle")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete rule",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun presetConfig(
    preset: RulePreset,
    customCategory: TransactionCategory,
    customDirection: MoneyDirection
): PresetConfig =
    when (preset) {
        RulePreset.SALARY -> PresetConfig(
            "Monthly salary",
            TransactionCategory.INCOME,
            MoneyDirection.INCOME,
            startsPayCycle = true
        )
        RulePreset.RENT -> PresetConfig("Rent", TransactionCategory.RENT, MoneyDirection.EXPENSE)
        RulePreset.INTERNET -> PresetConfig("Internet", TransactionCategory.INTERNET, MoneyDirection.EXPENSE)
        RulePreset.ELECTRICITY -> PresetConfig("Electricity", TransactionCategory.ELECTRICITY, MoneyDirection.EXPENSE)
        RulePreset.FOOD -> PresetConfig("Food / lunch", TransactionCategory.LUNCH, MoneyDirection.EXPENSE)
        RulePreset.TRANSPORT -> PresetConfig("Transport", TransactionCategory.TRANSPORT, MoneyDirection.EXPENSE)
        RulePreset.FAMILY_SUPPORT -> PresetConfig("Family support", TransactionCategory.FAMILY, MoneyDirection.EXPENSE)
        RulePreset.SAVINGS -> PresetConfig("Savings", TransactionCategory.SAVINGS, MoneyDirection.TRANSFER)
        RulePreset.OWN_TRANSFER -> PresetConfig("My own transfer", TransactionCategory.TRANSFER, MoneyDirection.TRANSFER)
        RulePreset.OTHER_RECURRING -> PresetConfig("Other recurring", TransactionCategory.OTHER, MoneyDirection.EXPENSE)
        RulePreset.CUSTOM -> PresetConfig("Custom", customCategory, customDirection)
    }

private fun rulePresetOptions(): List<Pair<RulePreset, String>> =
    listOf(
        RulePreset.SALARY,
        RulePreset.RENT,
        RulePreset.INTERNET,
        RulePreset.ELECTRICITY,
        RulePreset.FOOD,
        RulePreset.TRANSPORT,
        RulePreset.FAMILY_SUPPORT,
        RulePreset.SAVINGS,
        RulePreset.OWN_TRANSFER,
        RulePreset.OTHER_RECURRING,
        RulePreset.CUSTOM
    ).map { it to presetConfig(it, TransactionCategory.OTHER, MoneyDirection.EXPENSE).label }

private fun RulePreset.prettyName(): String =
    presetConfig(this, TransactionCategory.OTHER, MoneyDirection.EXPENSE).label

private fun TransactionCategory.prettyName(): String =
    name.lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }

private fun accountTypeLabel(type: AccountType): String =
    when (type) {
        AccountType.MOBILE_WALLET -> "Mobile wallet"
        AccountType.BANK -> "Bank account"
        AccountType.CASH -> "Cash"
        AccountType.SAVINGS -> "Savings account"
        AccountType.OTHER -> "Other"
    }


private fun parseEditableMoneyMinor(value: String): Long? =
    runCatching {
        value.replace(",", "")
            .toBigDecimal()
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }.getOrNull()

private fun formatEditableMoney(minor: Long): String =
    java.math.BigDecimal.valueOf(minor, 2)
        .stripTrailingZeros()
        .toPlainString()

private fun formatDisplayMoney(minor: Long): String =
    "KES " + java.text.NumberFormat.getNumberInstance().format(minor / 100.0)
