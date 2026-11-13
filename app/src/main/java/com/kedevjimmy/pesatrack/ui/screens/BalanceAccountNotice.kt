package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.BalanceAccountVisibility
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.rules.PlanSpendableAccount
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun HomeBalanceNotice(accounts: List<MoneyAccount>, balances: List<PlanSpendableAccount>) {
    var dismissed by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    val visible = balances.filter {
        !it.isProtected && it.provisional && it.id !in dismissed
    }.mapNotNull { balance ->
        accounts.firstOrNull { it.id == balance.id }?.let { it to balance }
    }
    if (visible.isEmpty()) return
    if (visible.size == 1) {
        val (account, balance) = visible.first()
        key(account.id) {
            BalanceNoticeCard(account, balance) {
                dismissed = ArrayList(dismissed + account.id)
            }
        }
        return
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val palette = BrownHomePalette
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            color = palette.cream,
            shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
            border = BorderStroke(1.dp, palette.outline)
        ) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }
                    .heightIn(min = PesaDesignTokens.minimumTouchTarget)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${visible.size} accounts need balance review",
                        color = palette.coffee, style = MaterialTheme.typography.titleSmall)
                    Text("Tap to review the missing or uncertain balances",
                        color = palette.muted, style = MaterialTheme.typography.bodySmall)
                }
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Hide account notices" else "Review account notices",
                    tint = palette.coffee
                )
            }
        }
        if (expanded) visible.forEach { (account, balance) ->
            key(account.id) {
                BalanceNoticeCard(account, balance) {
                    dismissed = ArrayList(dismissed + account.id)
                }
            }
        }
    }
}

@Composable
private fun BalanceNoticeCard(
    account: MoneyAccount, balance: PlanSpendableAccount, onDismiss: () -> Unit
) {
    val palette = BrownHomePalette
    val name = compactAccountLabel(account.name)
    val missing = balance.balanceMinor == null && !balance.conflicting
    var showMenu by remember { mutableStateOf(false) }
    var showBalance by rememberSaveable { mutableStateOf(false) }
    var showArchive by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
        color = palette.cream,
        border = BorderStroke(1.dp, palette.outline)
    ) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (missing) "No recorded balance for $name" else "Check the balance for $name",
                    style = MaterialTheme.typography.titleSmall, color = palette.coffee)
                Text("Available money uses the account balances PesaTrack can verify.",
                    style = MaterialTheme.typography.bodySmall, color = palette.ink)
                TextButton(onClick = { showBalance = true }) {
                    Text(if (missing) "Add balance" else "Update balance", color = palette.coffee)
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Balance options for $name",
                        tint = palette.coffee)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Archive account") }, onClick = {
                        showMenu = false
                        showArchive = true
                    })
                    DropdownMenuItem(text = { Text("Dismiss") }, onClick = {
                        showMenu = false
                        onDismiss()
                    })
                }
            }
        }
    }
    if (showBalance) AccountBalanceCheckSheet(account.id) { showBalance = false }
    if (showArchive) BalanceArchiveDialog(account, archive = true) { showArchive = false }
}

@Composable
internal fun ArchivedBalanceAccounts(accounts: List<MoneyAccount>) {
    val palette = BrownHomePalette
    var restoreId by rememberSaveable { mutableStateOf<String?>(null) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
        color = palette.cream,
        border = BorderStroke(1.dp, palette.outline)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Archived from balances", style = MaterialTheme.typography.titleSmall, color = palette.coffee)
            accounts.forEach { account ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(compactAccountLabel(account.name), Modifier.weight(1f), color = palette.ink,
                        style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { restoreId = account.id }) { Text("Restore", color = palette.coffee) }
                }
            }
        }
    }
    accounts.firstOrNull { it.id == restoreId }?.let { account ->
        BalanceArchiveDialog(account, archive = false) { restoreId = null }
    }
}

@Composable
private fun BalanceArchiveDialog(account: MoneyAccount, archive: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val name = compactAccountLabel(account.name)
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (archive) "Archive $name from balances?" else "Restore $name to balances?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (archive) "This removes the account from current balance totals. You can restore it from Accounts. Past transactions and transfer decisions stay in Activity."
                    else "PesaTrack will include this account in current balance totals when it has usable balance evidence.")
                if (error) Text("Could not save this change. Try again.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                error = false
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            BalanceAccountVisibility(context).setArchived(account.id, archive)
                        }
                        onDismiss()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: IllegalStateException) {
                        error = true
                    } finally {
                        busy = false
                    }
                }
            }) { Text(if (archive) "Archive" else "Restore") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } }
    )
}
