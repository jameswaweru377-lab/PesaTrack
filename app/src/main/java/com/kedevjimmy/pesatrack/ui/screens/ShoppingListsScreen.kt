package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.ShoppingListStore
import com.kedevjimmy.pesatrack.domain.planning.ShoppingItem
import com.kedevjimmy.pesatrack.domain.planning.ShoppingList
import com.kedevjimmy.pesatrack.domain.planning.ShoppingListPolicy
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Only user-entered estimates; no path to the canonical transaction writer. */
@Composable
fun ShoppingListsScreen(scaffoldPadding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { ShoppingListsState(ShoppingListStore(context), scope) }
    val dialogs = remember { ShoppingDialogs() }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val currentMonth = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
    val selected = state.lists.firstOrNull { it.id == selectedId }
    LaunchedEffect(Unit) { state.reload() }
    BackHandler(selectedId != null) { selectedId = null }

    LazyColumn(Modifier.padding(scaffoldPadding).background(HomeMonochromePalette.canvas),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ShoppingTitle(if (selected == null) "Shopping lists" else selected.title) {
            if (selected == null) onBack() else selectedId = null
        } }
        if (state.error != null) item {
            Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { state.reload() }) { Text("Retry") }
        }
        if (state.loading) item { Text("Loading shopping lists…") }
        else if (selected == null) {
            item { ShoppingOverviewHeader(onNew = { dialogs.showNew = true }, disabled = state.busy) }
            if (state.lists.isEmpty()) item { ShoppingEmptyState() }
            items(state.lists, key = { it.id }) { list ->
                ShoppingOverviewRow(list, onOpen = { selectedId = list.id },
                    onReuse = { dialogs.reuseSource = list }, disabled = state.busy)
            }
        } else {
            item { ShoppingDetailHeader(selected, onAdd = {
                dialogs.editingItem = null; dialogs.showItemForm = true
            }, onRename = { dialogs.showRename = true },
                onDelete = { dialogs.confirmDelete = true },
                disabled = state.busy) }
            if (selected.items.isEmpty()) item {
                Text("No items yet. Add your first planned purchase.",
                    style = MaterialTheme.typography.bodyMedium)
            }
            items(selected.items, key = { it.id }) { item ->
                ShoppingItemRow(item, disabled = state.busy, onCheck = { bought ->
                    state.save(selected.copy(items = selected.items.map {
                        if (it.id == item.id) it.copy(bought = bought) else it
                    }))
                }, onEdit = { dialogs.editingItem = item; dialogs.showItemForm = true },
                    onRemove = { dialogs.removeItem = item })
            }
        }
    }

    ShoppingListDialogs(state, dialogs, selected, currentMonth) { selectedId = it }
}

private class ShoppingDialogs {
    var showNew by mutableStateOf(false)
    var showRename by mutableStateOf(false)
    var editingItem by mutableStateOf<ShoppingItem?>(null)
    var showItemForm by mutableStateOf(false)
    var removeItem by mutableStateOf<ShoppingItem?>(null)
    var confirmDelete by mutableStateOf(false)
    var reuseSource by mutableStateOf<ShoppingList?>(null)
}

@Composable
private fun ShoppingListDialogs(state: ShoppingListsState, dialogs: ShoppingDialogs,
    selected: ShoppingList?, currentMonth: String, onSelect: (String?) -> Unit) {
    if (dialogs.showNew || dialogs.showRename) ShoppingNameDialog(
        title = if (dialogs.showNew) "New shopping list" else "Rename list",
        initial = if (dialogs.showNew) "Monthly shopping" else selected?.title.orEmpty(),
        saveError = state.error,
        onDismiss = { dialogs.showNew = false; dialogs.showRename = false }, onSave = { title ->
            val list = if (dialogs.showNew) ShoppingList(UUID.randomUUID().toString(), title,
                currentMonth, System.currentTimeMillis()) else selected?.copy(title = title)
            if (list != null) state.save(list) {
                onSelect(list.id)
                dialogs.showNew = false; dialogs.showRename = false
            }
        })
    if (dialogs.showItemForm && selected != null) ShoppingItemDialog(dialogs.editingItem,
        saveError = state.error,
        onDismiss = { dialogs.showItemForm = false }, onSave = { name, quantity, priceMinor ->
            val value = ShoppingItem(dialogs.editingItem?.id ?: UUID.randomUUID().toString(),
                name, quantity, priceMinor, dialogs.editingItem?.bought ?: false)
            state.save(selected.copy(items = if (dialogs.editingItem == null) selected.items + value
                else selected.items.map { if (it.id == value.id) value else it })) {
                dialogs.showItemForm = false
            }
        })
    if (dialogs.confirmDelete && selected != null) ShoppingConfirmation(
        title = "Delete ${selected.title}?", body = "This removes the list and its items only.",
        confirm = "Delete list", onDismiss = { dialogs.confirmDelete = false }, onConfirm = {
            state.delete(selected.id) { onSelect(null) }; dialogs.confirmDelete = false
        })
    if (dialogs.removeItem != null && selected != null) ShoppingConfirmation(
        title = "Remove ${dialogs.removeItem?.name}?", body = "Remove this item from the plan?",
        confirm = "Remove item", onDismiss = { dialogs.removeItem = null }, onConfirm = {
            val itemId = dialogs.removeItem?.id
            state.save(selected.copy(items = selected.items.filterNot { it.id == itemId }))
            dialogs.removeItem = null
        })
    dialogs.reuseSource?.let { source -> ShoppingConfirmation(
        title = "Reuse this list?", body = "Copy ${source.title} to $currentMonth. All items will be unchecked.",
        confirm = "Make a copy", onDismiss = { dialogs.reuseSource = null }, onConfirm = {
            val copy = ShoppingListPolicy.reuse(source, UUID.randomUUID().toString(),
                currentMonth, System.currentTimeMillis())
            state.save(copy) { onSelect(copy.id) }; dialogs.reuseSource = null
        }) }
}

private class ShoppingListsState(private val store: ShoppingListStore, private val scope: CoroutineScope) {
    var lists by mutableStateOf<List<ShoppingList>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun reload() {
        scope.launch {
            loading = true
            try { lists = withContext(Dispatchers.IO) { store.list() }; error = null }
            catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                error = failure.message ?: "Shopping lists could not be loaded."
            } finally { loading = false }
        }
    }

    fun save(value: ShoppingList, onSaved: () -> Unit = {}) = write({ store.save(value) }, onSaved)
    fun delete(id: String, onSaved: () -> Unit = {}) = write({ store.delete(id) }, onSaved)

    private fun write(operation: () -> Unit, onSaved: () -> Unit) {
        if (busy) return
        busy = true; error = null
        scope.launch {
            try {
                val latest = withContext(Dispatchers.IO) { operation(); store.list() }
                lists = latest
                onSaved()
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                error = failure.message ?: "Could not save shopping list. Please try again."
            } finally { busy = false }
        }
    }
}

@Composable
private fun ShoppingTitle(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun ShoppingListsPlanEntry(onClick: () -> Unit) {
    PesaCard {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.ShoppingCart, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Shopping lists · Plan before you spend")
        }
    }
}

@Composable
private fun ShoppingOverviewHeader(onNew: () -> Unit, disabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Plan purchases before spending. These estimates do not change your balance.",
            style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onNew, enabled = !disabled) {
            Icon(Icons.Rounded.ShoppingCart, null)
            Text(" New list")
        }
    }
}

@Composable
private fun ShoppingEmptyState() {
    PesaCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Your first list starts here", style = MaterialTheme.typography.titleMedium)
        Text("Add items and prices, then reuse this list next month.",
            style = MaterialTheme.typography.bodySmall)
    } }
}

@Composable
private fun ShoppingOverviewRow(list: ShoppingList, onOpen: () -> Unit,
    onReuse: () -> Unit, disabled: Boolean) {
    val estimate = ShoppingListPolicy.total(list)
    PesaCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(list.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("${list.month} · ${list.items.size} items", style = MaterialTheme.typography.bodySmall)
        Text(shoppingEstimateLabel(estimate), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onOpen, enabled = !disabled) { Text("Open") }
            TextButton(onClick = onReuse, enabled = !disabled) { Text("Reuse this list") }
        }
    } }
}

@Composable
private fun ShoppingDetailHeader(list: ShoppingList, onAdd: () -> Unit,
    onRename: () -> Unit, onDelete: () -> Unit, disabled: Boolean) {
    val estimate = ShoppingListPolicy.total(list)
    PesaCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${list.month} · ${list.items.size} items", style = MaterialTheme.typography.bodySmall)
        Text(shoppingEstimateLabel(estimate), style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
        Text("Estimated only — no expense has been recorded.",
            style = MaterialTheme.typography.bodySmall)
        Button(onClick = onAdd, enabled = !disabled && list.items.size < ShoppingListPolicy.MAX_ITEMS) {
            Text("Add item")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onRename, enabled = !disabled) { Text("Rename") }
            TextButton(onClick = onDelete, enabled = !disabled) { Text("Delete list") }
        }
    } }
}

@Composable
private fun ShoppingItemRow(item: ShoppingItem, disabled: Boolean, onCheck: (Boolean) -> Unit,
    onEdit: () -> Unit, onRemove: () -> Unit) {
    PesaCard { Row(Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = item.bought, enabled = !disabled, onCheckedChange = onCheck)
        Column(Modifier.weight(1f)) {
            Text(item.name, fontWeight = FontWeight.Medium)
            Text("${item.quantity} × ${item.unitPriceMinor?.let(::formatShoppingMoney) ?: "price unknown"}",
                style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onEdit, enabled = !disabled) {
            Icon(Icons.Rounded.Edit, contentDescription = "Edit ${item.name}")
        }
        IconButton(onClick = onRemove, enabled = !disabled) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove ${item.name}")
        }
    } }
}

@Composable
private fun ShoppingNameDialog(title: String, initial: String, saveError: String?, onDismiss: () -> Unit,
    onSave: (String) -> Unit) {
    var name by remember(title, initial) { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("List name") }, singleLine = true)
            if (saveError != null) Text(saveError, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = {
        TextButton(onClick = { onSave(name.trim()) },
            enabled = name.trim().length in 1..60) { Text("Save") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun ShoppingItemDialog(existing: ShoppingItem?, saveError: String?, onDismiss: () -> Unit,
    onSave: (String, Int, Long?) -> Unit) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var quantity by remember(existing?.id) { mutableStateOf(existing?.quantity?.toString() ?: "1") }
    var price by remember(existing?.id) { mutableStateOf(existing?.unitPriceMinor?.let {
        BigDecimal.valueOf(it, 2).stripTrailingZeros().toPlainString()
    }.orEmpty()) }
    var error by remember(existing?.id) { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "Add item" else "Edit item") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, onValueChange = { name = it; error = null },
                label = { Text("Item name") }, singleLine = true)
            OutlinedTextField(quantity, onValueChange = { quantity = it; error = null },
                label = { Text("Quantity") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(price, onValueChange = { price = it; error = null },
                label = { Text("Unit price (KSh, optional)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            if (error != null || saveError != null) Text(error ?: saveError.orEmpty(),
                color = MaterialTheme.colorScheme.error)
        } }, confirmButton = { TextButton(onClick = {
            try {
                val count = quantity.toIntOrNull() ?: throw IllegalArgumentException("Enter a whole-number quantity.")
                val minor = ShoppingListPolicy.parsePriceMinor(price)
                ShoppingListPolicy.validate(ShoppingList("draft", "Draft", "2026-11", 1L,
                    listOf(ShoppingItem("draft-item", name.trim(), count, minor))))
                onSave(name.trim(), count, minor)
            } catch (failure: IllegalArgumentException) { error = failure.message }
        }) { Text("Save item") } }, dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        })
}

@Composable
private fun ShoppingConfirmation(title: String, body: String, confirm: String,
    onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

private fun shoppingEstimateLabel(value: com.kedevjimmy.pesatrack.domain.planning.ShoppingEstimate): String =
    when {
        value.knownTotalMinor == 0L && value.missingPrices > 0 -> "Estimate unavailable · ${value.missingPrices} prices missing"
        value.missingPrices > 0 -> "At least ${formatShoppingMoney(value.knownTotalMinor)} · ${value.missingPrices} prices missing"
        else -> "Estimated total: ${formatShoppingMoney(value.knownTotalMinor)}"
    }

private fun formatShoppingMoney(minor: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
    return "KSh ${formatter.format(BigDecimal.valueOf(minor, 2))}"
}
