package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.BalanceAccountVisibility
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.rules.PlanSpendableAccount
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun HomeBalanceNotice(accounts: List<MoneyAccount>, balances: List<PlanSpendableAccount>) {
    var dismissed by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    val balance = balances.firstOrNull {
        !it.isProtected && it.provisional && it.id !in dismissed
    } ?: return
    val account = accounts.firstOrNull { it.id == balance.id } ?: return
    key(account.id) {
        BalanceNoticeCard(account, balance) { dismissed = ArrayList(dismissed + account.id) }
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
        color = PremiumVisualTokens.card,
        border = BorderStroke(PesaDesignTokens.cardOutlineWidth, palette.outline)
    ) {
        Column(
            Modifier.padding(PesaDesignTokens.cardContentPadding),
            verticalArrangement = Arrangement.spacedBy(PesaDesignTokens.compactGap)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = PremiumVisualTokens.leaf.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(PesaDesignTokens.controlRadius)
                ) {
                    Text(
                        "BALANCE CHECK",
                        Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PremiumVisualTokens.leafReadable
                    )
                }
                Box(Modifier.weight(1f))
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (missing) "Balance needs attention" else "Check account balance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.coffee
                    )
                    Text(
                        if (missing) "$name has no recorded balance."
                        else "The recorded balance for $name needs checking.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.ink
                    )
                    Text(
                        "Available money uses only balances PesaTrack can verify.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PremiumVisualTokens.muted
                    )
                }
                Surface(
                    modifier = Modifier.padding(start = 8.dp),
                    shape = CircleShape,
                    color = PremiumVisualTokens.leaf.copy(alpha = 0.20f)
                ) {
                    Icon(
                        Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = palette.coffee,
                        modifier = Modifier.padding(12.dp).size(24.dp)
                    )
                }
            }
            Button(
                onClick = { showBalance = true },
                modifier = Modifier.heightIn(min = PesaDesignTokens.minimumTouchTarget),
                shape = RoundedCornerShape(PesaDesignTokens.controlRadius),
                colors = ButtonDefaults.buttonColors(containerColor = palette.coffee)
            ) {
                Text(if (missing) "Add balance  →" else "Update balance  →")
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
