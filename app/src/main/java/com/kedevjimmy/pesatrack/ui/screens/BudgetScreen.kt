package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.BudgetStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.MonthlyBudget
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.premiumCategoryColor
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private data class BudgetCategoryOption(
    val category: TransactionCategory,
    val label: String,
    val icon: ImageVector
)

private val budgetCategoryOptions = listOf(
    BudgetCategoryOption(TransactionCategory.RENT, "Rent", Icons.Rounded.Home),
    BudgetCategoryOption(TransactionCategory.LUNCH, "Food & dining", Icons.Rounded.Restaurant),
    BudgetCategoryOption(TransactionCategory.TRANSPORT, "Transport", Icons.Rounded.DirectionsBus),
    BudgetCategoryOption(TransactionCategory.SHOPPING, "Shopping", Icons.Rounded.ShoppingBag),
    BudgetCategoryOption(TransactionCategory.ELECTRICITY, "Electricity", Icons.Rounded.Bolt),
    BudgetCategoryOption(TransactionCategory.INTERNET, "Internet", Icons.Rounded.Wifi),
    BudgetCategoryOption(TransactionCategory.PHONE_AND_DATA, "Phone & data", Icons.Rounded.PhoneAndroid),
    BudgetCategoryOption(TransactionCategory.FAMILY, "Family", Icons.Rounded.FamilyRestroom),
    BudgetCategoryOption(TransactionCategory.FEES, "Fees", Icons.Rounded.ReceiptLong),
    BudgetCategoryOption(TransactionCategory.OTHER, "Other", Icons.Rounded.MoreHoriz)
)

@Composable
fun BudgetScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val budgetStore = remember { BudgetStore.get(context) }
    val transactionStore = remember { TransactionStore.get(context) }
    val ledgerUpdateState by LedgerUpdateBus.state.collectAsState()

    var selectedCategory by remember {
        mutableStateOf<TransactionCategory?>(null)
    }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    // Reading the revision makes this screen recompose when a budget or ledger changes.
    val revision = ledgerUpdateState.revision
    val budgets = remember(revision) { budgetStore.list() }
    val activity = remember(revision) { transactionStore.activitySnapshot() }
    val spentByCategory = remember(activity) {
        activity.transactions
            .filter {
                it.direction == MoneyDirection.EXPENSE.name &&
                    it.reviewState != ReviewState.EXCLUDED.name &&
                    it.reviewState != ReviewState.NEEDS_REVIEW.name
            }
            .groupBy { it.category }
            .mapValues { (_, transactions) ->
                transactions.sumOf { it.amountMinor + it.feeMinor }
            }
    }

    fun selectCategory(category: TransactionCategory) {
        selectedCategory = category
        amount = budgets
            .firstOrNull { it.category == category }
            ?.limitMinor
            ?.let(::formatEditableMoney)
            .orEmpty()
        error = null
    }

    fun saveBudget() {
        val category = selectedCategory
        if (category == null) {
            error = "Choose a category for this budget."
            return
        }

        val limitMinor = parseBudgetMoneyMinor(amount)
        if (limitMinor == null || limitMinor <= 0L) {
            error = "Enter a budget amount above zero."
            return
        }

        budgetStore.save(category, limitMinor)
        amount = ""
        selectedCategory = null
        error = null
    }

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Budgets",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "Set a monthly limit. Activity does the tracking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            PesaCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Add monthly budget",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Choose one of the same categories used in Activity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    budgetCategoryOptions
                        .chunked(2)
                        .forEach { rowOptions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowOptions.forEach { option ->
                                    BudgetCategoryTile(
                                        option = option,
                                        selected =
                                            selectedCategory == option.category,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            selectCategory(option.category)
                                        }
                                    )
                                }
                                if (rowOptions.size == 1) {
                                    androidx.compose.foundation.layout.Spacer(
                                        Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                    selectedCategory?.let { category ->
                        val option = budgetCategoryOptions.first {
                            it.category == category
                        }
                        val spent = spentByCategory[category.name] ?: 0L
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column(Modifier.padding(13.dp)) {
                                Text(
                                    option.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Spent this month: " + formatBudgetMoney(spent),
                                    modifier = Modifier.padding(top = 2.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        OutlinedTextField(
                            value = amount,
                            onValueChange = {
                                amount = it
                                error = null
                            },
                            label = { Text("Monthly budget amount") },
                            prefix = { Text("KES ") },
                            placeholder = { Text("2,500") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = ::saveBudget,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                if (budgets.any { it.category == category }) {
                                    "Update budget"
                                } else {
                                    "Add budget"
                                }
                            )
                        }
                    }

                    error?.let { message ->
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            Text(
                "Your monthly budgets",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (budgets.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "No budgets yet. Add one above and PesaTrack will compare it with trusted Activity spending automatically.",
                        modifier = Modifier.padding(15.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(
                count = budgets.size,
                key = { index -> budgets[index].category.name }
            ) { index ->
                ExistingBudgetRow(
                    budget = budgets[index],
                    spentMinor =
                        spentByCategory[budgets[index].category.name] ?: 0L,
                    onEdit = {
                        selectCategory(budgets[index].category)
                    },
                    onDelete = {
                        budgetStore.delete(budgets[index].category)
                        if (selectedCategory == budgets[index].category) {
                            selectedCategory = null
                            amount = ""
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BudgetCategoryTile(
    option: BudgetCategoryOption,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 76.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                icon = option.icon,
                size = 36.dp
            )
            Text(
                option.label,
                modifier = Modifier.padding(start = 9.dp),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun ExistingBudgetRow(
    budget: MonthlyBudget,
    spentMinor: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val label = budgetCategoryOptions
        .firstOrNull { it.category == budget.category }
        ?.label
        ?: budget.category.name
    val remaining = (budget.limitMinor - spentMinor).coerceAtLeast(0L)
    val progress = if (budget.limitMinor <= 0L) {
        0f
    } else {
        spentMinor.toFloat() / budget.limitMinor.toFloat()
    }

    PesaCard {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    formatBudgetMoney(spentMinor) +
                        " / " +
                        formatBudgetMoney(budget.limitMinor),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                if (spentMinor > budget.limitMinor) {
                    formatBudgetMoney(spentMinor - budget.limitMinor) +
                        " over budget"
                } else {
                    formatBudgetMoney(remaining) + " remaining"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (spentMinor > budget.limitMinor) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Remove")
                }
            }
        }
    }
}

private fun parseBudgetMoneyMinor(raw: String): Long? =
    runCatching {
        raw.replace(",", "")
            .trim()
            .toBigDecimal()
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }.getOrNull()

private fun formatBudgetMoney(minor: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    return "KES " + formatter.format(minor / 100.0)
}

private fun formatEditableMoney(minor: Long): String =
    if (minor % 100L == 0L) {
        (minor / 100L).toString()
    } else {
        java.math.BigDecimal(minor)
            .movePointLeft(2)
            .stripTrailingZeros()
            .toPlainString()
    }
