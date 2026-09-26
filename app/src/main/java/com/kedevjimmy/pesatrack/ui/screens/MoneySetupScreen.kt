package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
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
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader

private enum class RuleKind {
    SALARY,
    RECURRING_EXPENSE,
    OWN_TRANSFER
}

@Composable
fun MoneySetupScreen(
    scaffoldPadding: PaddingValues,
    initialSetup: Boolean,
    onDone: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val ruleStore = remember { RuleStore.get(context) }
    val transactionStore = remember { TransactionStore.get(context) }

    var rules by remember { mutableStateOf(ruleStore.listRules()) }
    var kind by remember { mutableStateOf(RuleKind.SALARY) }
    var ruleName by remember { mutableStateOf("") }
    var counterparty by remember { mutableStateOf("") }
    var notificationTitle by remember { mutableStateOf("") }
    var messagePhrase by remember { mutableStateOf("") }
    var displayLabel by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TransactionCategory.RENT) }
    var nextMonth by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun reloadRules() {
        rules = ruleStore.listRules()
    }

    fun saveRule() {
        error = null
        val rule = runCatching {
            when (kind) {
                RuleKind.SALARY -> {
                    require(notificationTitle.isNotBlank() || messagePhrase.isNotBlank()) {
                        "Add the notification sender/title or a phrase that identifies the salary alert."
                    }
                    ruleStore.save(
                        name = ruleName.ifBlank { "Salary source" },
                        counterpartyContains = "",
                        notificationTitleContains = notificationTitle,
                        messageContains = messagePhrase,
                        category = TransactionCategory.INCOME,
                        direction = MoneyDirection.INCOME,
                        planningMonthOffset = if (nextMonth) 1 else 0,
                        displayLabel = displayLabel.ifBlank { ruleName.ifBlank { "Salary" } }
                    )
                }

                RuleKind.RECURRING_EXPENSE -> {
                    require(counterparty.isNotBlank()) {
                        "Enter the M-Pesa name or merchant text to recognize."
                    }
                    ruleStore.save(
                        name = ruleName.ifBlank { counterparty },
                        counterpartyContains = counterparty,
                        category = category,
                        direction = MoneyDirection.EXPENSE,
                        displayLabel = displayLabel.ifBlank { null }
                    )
                }

                RuleKind.OWN_TRANSFER -> {
                    require(counterparty.isNotBlank()) {
                        "Enter the destination name or account label to recognize."
                    }
                    ruleStore.save(
                        name = ruleName.ifBlank { "Own-account transfer" },
                        counterpartyContains = counterparty,
                        category = TransactionCategory.TRANSFER,
                        direction = MoneyDirection.TRANSFER,
                        displayLabel = displayLabel.ifBlank { null }
                    )
                }
            }
        }.getOrElse {
            error = it.message ?: "Could not save this rule."
            return
        }

        transactionStore.reapplyCounterpartyRules(ruleStore.listRules(enabledOnly = true))
        reloadRules()

        ruleName = ""
        counterparty = ""
        notificationTitle = ""
        messagePhrase = ""
        displayLabel = ""
        if (rule.category == TransactionCategory.INCOME) {
            nextMonth = true
        }
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
                        if (initialSetup) {
                            "Teach PesaTrack what should be recognized automatically."
                        } else {
                            "Manage what PesaTrack should recognize automatically."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            PesaCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "What are you teaching PesaTrack?",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = kind == RuleKind.SALARY,
                            onClick = { kind = RuleKind.SALARY },
                            label = { Text("Salary") },
                            leadingIcon = { Icon(Icons.Rounded.Payments, null) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = kind == RuleKind.RECURRING_EXPENSE,
                            onClick = { kind = RuleKind.RECURRING_EXPENSE },
                            label = { Text("Expense") },
                            leadingIcon = { Icon(Icons.Rounded.ReceiptLong, null) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = kind == RuleKind.OWN_TRANSFER,
                            onClick = { kind = RuleKind.OWN_TRANSFER },
                            label = { Text("Transfer") },
                            leadingIcon = { Icon(Icons.Rounded.SwapHoriz, null) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            PesaCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = ruleName,
                        onValueChange = { ruleName = it },
                        label = { Text("Rule name") },
                        placeholder = {
                            Text(
                                when (kind) {
                                    RuleKind.SALARY -> "Monthly salary"
                                    RuleKind.RECURRING_EXPENSE -> "Home internet"
                                    RuleKind.OWN_TRANSFER -> "Transfer to savings"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (kind == RuleKind.SALARY) {
                        OutlinedTextField(
                            value = notificationTitle,
                            onValueChange = { notificationTitle = it },
                            label = { Text("Notification sender/title contains") },
                            placeholder = { Text("Example: your bank name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = messagePhrase,
                            onValueChange = { messagePhrase = it },
                            label = { Text("Message contains") },
                            placeholder = { Text("Example: employer or salary phrase") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = displayLabel,
                            onValueChange = { displayLabel = it },
                            label = { Text("Display as") },
                            placeholder = { Text("Salary") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text(
                            "Which plan should this income fund?",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !nextMonth,
                                onClick = { nextMonth = false },
                                label = { Text("Same month") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = nextMonth,
                                onClick = { nextMonth = true },
                                label = { Text("Next month") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            "If salary arrives near month-end but pays for the following month, choose Next month.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        OutlinedTextField(
                            value = counterparty,
                            onValueChange = { counterparty = it },
                            label = { Text("M-Pesa name contains") },
                            placeholder = { Text("Recipient or merchant name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = displayLabel,
                            onValueChange = { displayLabel = it },
                            label = { Text("Display as (optional)") },
                            placeholder = { Text("Friendly name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (kind == RuleKind.RECURRING_EXPENSE) {
                            Text("Category", style = MaterialTheme.typography.titleSmall)
                            CategoryChooser(
                                selected = category,
                                onSelected = { category = it }
                            )
                        } else {
                            Text(
                                "Transfers to your own account are tracked but excluded from spending.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                        Text("Save smart rule", modifier = Modifier.padding(start = 8.dp))
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
                onDelete = {
                    ruleStore.delete(rules[index].id)
                    reloadRules()
                }
            )
        }

        item {
            Spacer(Modifier.height(4.dp))
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
            }
        }
    }
}

@Composable
private fun RuleRow(
    rule: SmartRule,
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
                    buildString {
                        if (rule.counterpartyContains.isNotBlank()) {
                            append("Name contains “")
                            append(rule.counterpartyContains)
                            append("”")
                        } else {
                            if (rule.notificationTitleContains.isNotBlank()) {
                                append("Sender contains “")
                                append(rule.notificationTitleContains)
                                append("”")
                            }
                            if (rule.messageContains.isNotBlank()) {
                                if (isNotEmpty()) append(" • ")
                                append("Message contains “")
                                append(rule.messageContains)
                                append("”")
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    buildString {
                        append(rule.category.prettyName())
                        if (rule.direction == MoneyDirection.INCOME && rule.planningMonthOffset == 1) {
                            append(" • funds next month")
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

private fun TransactionCategory.prettyName(): String =
    name.lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }
