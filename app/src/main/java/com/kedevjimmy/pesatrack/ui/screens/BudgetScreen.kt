package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.BudgetStore
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.MonthlyBudget
import com.kedevjimmy.pesatrack.data.local.PesaCategory
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class BudgetUiData(
    val budgets: List<MonthlyBudget> = emptyList(),
    val categories: List<PesaCategory> = emptyList(),
    val trackedByCategory: Map<String, Long> = emptyMap()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val budgetStore = remember { BudgetStore.get(context) }
    val categoryStore = remember { CategoryStore.get(context) }
    val transactionStore = remember { TransactionStore.get(context) }
    val ledgerUpdateState by LedgerUpdateBus.state.collectAsState()

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showAddCategory by remember { mutableStateOf(false) }
    var customCategoryName by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var showBudgetSheet by remember { mutableStateOf(false) }

    val revision = ledgerUpdateState.revision
    var uiData by remember {
        mutableStateOf(BudgetUiData())
    }

    LaunchedEffect(revision) {
        uiData =
            withContext(Dispatchers.IO) {
                val activity =
                    transactionStore.activitySnapshot()
                val trackedByCategory =
                    activity.transactions
                        .filter {
                            it.reviewState !=
                                ReviewState.EXCLUDED.name &&
                                it.reviewState !=
                                ReviewState.NEEDS_REVIEW.name
                        }
                        .groupBy {
                            categoryStore
                                .canonicalId(it.category)
                        }
                        .mapValues {
                                (_, transactions) ->
                            maxOf(
                                0L,
                                transactions.sumOf { item ->
                                    if (
                                        item.relationType ==
                                        TransactionStore
                                            .RELATION_REFUND
                                    ) {
                                        val original =
                                            item.relatedTransactionId
                                                ?.let(
                                                    transactionStore::getById
                                                )
                                        if (
                                            original != null &&
                                            original.direction ==
                                                com.kedevjimmy.pesatrack.domain.model.MoneyDirection.EXPENSE.name &&
                                            original.reviewState !=
                                                ReviewState.EXCLUDED.name &&
                                            original.reviewState !=
                                                ReviewState.NEEDS_REVIEW.name
                                        ) {
                                            -item.amountMinor
                                        } else {
                                            0L
                                        }
                                    } else {
                                        item.amountMinor +
                                            item.feeMinor
                                    }
                                }
                            )
                        }

                BudgetUiData(
                    budgets = budgetStore.list(),
                    categories =
                        categoryStore.budgetCategories(),
                    trackedByCategory =
                        trackedByCategory
                )
            }
    }

    val budgets = uiData.budgets
    val categories = uiData.categories
    val trackedByCategory =
        uiData.trackedByCategory

    fun selectCategory(
        category: PesaCategory,
        openEditor: Boolean = true
    ) {
        selectedCategoryId = category.id
        amount = budgets
            .firstOrNull { it.categoryId == category.id }
            ?.limitMinor
            ?.let(::formatEditableMoney)
            .orEmpty()
        error = null
        categoryExpanded = false
        if (openEditor) {
            showBudgetSheet = true
        }
    }

    fun saveBudget(): Boolean {
        val categoryId = selectedCategoryId
        if (categoryId == null) {
            error = "Choose a category for this budget."
            return false
        }

        val limitMinor = parseBudgetMoneyMinor(amount)
        if (limitMinor == null || limitMinor <= 0L) {
            error = "Enter a budget amount above zero."
            return false
        }

        budgetStore.save(categoryId, limitMinor)
        amount = ""
        selectedCategoryId = null
        error = null
        return true
    }

    if (showAddCategory) {
        AlertDialog(
            onDismissRequest = {
                showAddCategory = false
                customCategoryName = ""
            },
            title = { Text("Add category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "This category will appear in both Activity and Budgets.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = customCategoryName,
                        onValueChange = { customCategoryName = it },
                        label = { Text("Category name") },
                        placeholder = { Text("Example: Pets") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddCategory = false
                        customCategoryName = ""
                    }
                ) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        runCatching {
                            categoryStore.addCustom(customCategoryName)
                        }.onSuccess { category ->
                            showAddCategory = false
                            customCategoryName = ""
                            selectCategory(category)
                        }.onFailure {
                            error = it.message ?: "Could not add category."
                            showAddCategory = false
                        }
                    }
                ) {
                    Text("Add category")
                }
            }
        )
    }

    val selectedCategory = selectedCategoryId
        ?.let(categoryStore::find)

    if (showBudgetSheet && selectedCategory != null) {
        val currentSpent =
            trackedByCategory[selectedCategory.id] ?: 0L
        val existingBudget = budgets.firstOrNull {
            it.categoryId == selectedCategory.id
        }

        ModalBottomSheet(
            onDismissRequest = {
                showBudgetSheet = false
                selectedCategoryId = null
                amount = ""
                error = null
            }
        ) {
            BudgetEditorSheet(
                category = selectedCategory,
                spentMinor = currentSpent,
                amount = amount,
                isEditing = existingBudget != null,
                error = error,
                onAmountChanged = {
                    amount = it
                    error = null
                },
                onSave = {
                    if (saveBudget()) {
                        showBudgetSheet = false
                    }
                }
            )
        }
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
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text("Budgets", style = MaterialTheme.typography.headlineMedium)
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
                        "Choose a category. Activity will track the spending automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                categoryExpanded = !categoryExpanded
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 13.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Choose a category",
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Icon(
                                if (categoryExpanded) {
                                    Icons.Rounded.ExpandLess
                                } else {
                                    Icons.Rounded.ExpandMore
                                },
                                contentDescription = if (categoryExpanded) {
                                    "Collapse categories"
                                } else {
                                    "Expand categories"
                                },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (categoryExpanded) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Categories",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = {
                                        showAddCategory = true
                                    },
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("New")
                                }
                            }

                            categories.chunked(2).forEach { rowOptions ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(10.dp)
                                ) {
                                    rowOptions.forEach { category ->
                                        BudgetCategoryTile(
                                            category = category,
                                            selected = false,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                selectCategory(category)
                                            }
                                        )
                                    }
                                    if (rowOptions.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
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
                        "No budgets yet. Add one above and PesaTrack will compare it with trusted Activity transactions automatically.",
                        modifier = Modifier.padding(15.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(
                count = budgets.size,
                key = { index -> budgets[index].categoryId }
            ) { index ->
                ExistingBudgetRow(
                    budget = budgets[index],
                    categoryLabel =
                        categoryStore.labelFor(
                            budgets[index].categoryId
                        ),
                    spentMinor =
                        trackedByCategory[
                            budgets[index].categoryId
                        ] ?: 0L,
                    onEdit = {
                        categoryStore.find(
                            budgets[index].categoryId
                        )?.let(::selectCategory)
                    },
                    onDelete = {
                        budgetStore.delete(
                            budgets[index].categoryId
                        )
                        if (
                            selectedCategoryId ==
                            budgets[index].categoryId
                        ) {
                            selectedCategoryId = null
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
    category: PesaCategory,
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
                icon = categoryIcon(category.id),
                size = 36.dp
            )
            Text(
                category.label,
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
private fun BudgetEditorSheet(
    category: PesaCategory,
    spentMinor: Long,
    amount: String,
    isEditing: Boolean,
    error: String?,
    onAmountChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                end = 20.dp,
                bottom = 28.dp
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(
                icon = categoryIcon(category.id),
                size = 48.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    category.label,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    "Tracked this month: " +
                        formatBudgetMoney(spentMinor),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                "Set the most you want to use for " +
                    category.label.lowercase() +
                    " this month. PesaTrack will compare it with trusted Activity transactions.",
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChanged,
            label = { Text("Monthly budget") },
            prefix = { Text("KES ") },
            placeholder = { Text("2,500") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        error?.let { message ->
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                if (isEditing) {
                    "Update budget"
                } else {
                    "Add budget"
                }
            )
        }
    }
}

@Composable
private fun ExistingBudgetRow(
    budget: MonthlyBudget,
    categoryLabel: String,
    spentMinor: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                    categoryLabel,
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
                    formatBudgetMoney(
                        spentMinor - budget.limitMinor
                    ) + " over budget"
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

private fun categoryIcon(id: String): ImageVector =
    when (id) {
        TransactionCategory.GROCERIES.name ->
            Icons.Rounded.LocalGroceryStore
        TransactionCategory.EATING_OUT.name ->
            Icons.Rounded.Restaurant
        TransactionCategory.TRANSPORT.name ->
            Icons.Rounded.DirectionsBus
        TransactionCategory.RENT.name ->
            Icons.Rounded.Home
        TransactionCategory.ELECTRICITY.name ->
            Icons.Rounded.Bolt
        TransactionCategory.WATER.name ->
            Icons.Rounded.WaterDrop
        TransactionCategory.INTERNET.name ->
            Icons.Rounded.Wifi
        TransactionCategory.PHONE_AND_DATA.name ->
            Icons.Rounded.PhoneAndroid
        TransactionCategory.HOME_HOUSEHOLD.name ->
            Icons.Rounded.Chair
        TransactionCategory.SHOPPING.name ->
            Icons.Rounded.ShoppingBag
        TransactionCategory.HEALTH.name ->
            Icons.Rounded.Favorite
        TransactionCategory.EDUCATION.name ->
            Icons.Rounded.School
        TransactionCategory.ENTERTAINMENT.name ->
            Icons.Rounded.Movie
        TransactionCategory.FAMILY.name ->
            Icons.Rounded.FamilyRestroom
        TransactionCategory.DEBT_LOAN.name ->
            Icons.Rounded.CreditCard
        TransactionCategory.SAVINGS.name ->
            Icons.Rounded.Savings
        TransactionCategory.INVESTMENT.name ->
            Icons.AutoMirrored.Rounded.TrendingUp
        TransactionCategory.FEES.name ->
            Icons.Rounded.Paid
        else -> Icons.Rounded.MoreHoriz
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
