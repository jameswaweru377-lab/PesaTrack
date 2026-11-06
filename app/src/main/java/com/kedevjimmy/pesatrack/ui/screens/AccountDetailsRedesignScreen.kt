package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.PaydayAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality
import com.kedevjimmy.pesatrack.data.local.PaydaySnapshotStore
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import com.kedevjimmy.pesatrack.domain.rules.BalancePresentationPolicy
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun AccountDetailsRedesignScreen(
    scaffoldPadding: PaddingValues,
    accountId: String,
    onBack: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onAccountClick: (String) -> Unit,
    onImportStatement: () -> Unit
) {
    val context = LocalContext.current
    val revision by LedgerUpdateBus.state.collectAsState()
    var account by remember { mutableStateOf<MoneyAccount?>(null) }
    var balance by remember { mutableStateOf<PaydayAccountSnapshot?>(null) }
    var rows by remember { mutableStateOf(emptyList<StoredTransaction>()) }
    var linked by remember { mutableStateOf(emptyList<Pair<MoneyAccount, PaydayAccountSnapshot?>>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }

    LaunchedEffect(accountId, revision.revision, retry) {
        error = null
        try {
            val loaded = withContext(Dispatchers.IO) {
                val store = AccountStore.get(context)
                val a = store.listAccounts(false).firstOrNull { it.id == accountId }
                val b = a?.let { PaydaySnapshotStore.get(context).currentAccountSnapshot(it) }
                val t = TransactionStore.get(context).activitySnapshot().trustedTransactions.filter {
                    it.accountId == accountId || it.linkedAccountId == accountId
                }
                Triple(a, b, t)
            }
            linked = withContext(Dispatchers.IO) {
                if (accountId != AccountStore.DEFAULT_MPESA_ID) emptyList() else {
                    val ids = FinancialUnderstandingStore.get(context).confirmedAccountIds()
                    AccountStore.get(context).listAccounts(true).filter {
                        (it.name.contains("M-Shwari", true) || it.name.contains("KCB M-Pesa", true)) &&
                            AccountDisplayPolicy.isConfirmedOwned(it, ids)
                    }.map { it to PaydaySnapshotStore.get(context).currentAccountSnapshot(it) }
                }
            }
            account = loaded.first
            balance = loaded.second
            rows = loaded.third
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = "Couldn't load the latest account details. Your saved data is unchanged."
        }
    }
    AccountDetailsContent(scaffoldPadding, account, balance, rows, linked, error,
        onBack, onTransactionClick, onAccountClick, onImportStatement,
        onRetry = { retry++ })
}

@Composable
private fun AccountDetailsContent(
    padding: PaddingValues, account: MoneyAccount?, balance: PaydayAccountSnapshot?,
    transactions: List<StoredTransaction>, linked: List<Pair<MoneyAccount, PaydayAccountSnapshot?>>,
    error: String?, onBack: () -> Unit, onTransactionClick: (Long) -> Unit,
    onAccountClick: (String) -> Unit, onImportStatement: () -> Unit, onRetry: () -> Unit
) {
    var filter by remember { mutableStateOf("All") }
    val listState = rememberLazyListState()
    val scrollScope = rememberCoroutineScope()
    val activityIndex = 4 + (if (error != null) 1 else 0) +
        (if (linked.isEmpty()) 0 else 1 + linked.size)
    val visible = transactions.filter { transaction ->
        when (filter) {
            "Money In" -> transaction.direction == MoneyDirection.INCOME.name
            "Expenses" -> transaction.direction == MoneyDirection.EXPENSE.name
            "Transfers" -> transaction.direction == MoneyDirection.TRANSFER.name
            else -> true
        }
    }.sortedByDescending { it.occurredAtEpochMs }
    LazyColumn(state = listState, modifier = Modifier.padding(padding),
        contentPadding = PaddingValues(horizontal = PesaDesignTokens.screenHorizontalPadding,
            vertical = PesaDesignTokens.screenVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(PesaDesignTokens.sectionGap)) {
        item { AccountDetailTitle(account, onBack) }
        if (error != null) item {
            Text(error, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Retry") }
        }
        item { AccountRecordedBalance(account, balance) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    filter = "All"
                    scrollScope.launch { listState.animateScrollToItem(activityIndex) }
                }, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, AccountBlack)) {
                    Text("View activity", color = AccountBlack)
                }
                Column(Modifier.weight(1f)) { AccountBalanceCheckAction(account?.id) }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
                color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().clickable(onClick = onImportStatement).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Import a statement", Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                }
            }
        }
        if (linked.isNotEmpty()) {
            item { AccountsSectionTitle("Linked savings") }
            items(linked, key = { it.first.id }) { (child, snapshot) ->
                LinkedSavingsAccount(child, snapshot, onAccountClick)
            }
        }
        item { AccountsSectionTitle("Recent activity") }
        item {
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(4) { index ->
                    val label = listOf("All", "Money In", "Expenses", "Transfers")[index]
                    FilterChip(selected = filter == label, onClick = { filter = label },
                        label = { Text(label) })
                }
            }
        }
        if (visible.isEmpty()) item {
            Text("No ${filter.lowercase()} entries linked to this account yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            items(visible.take(30), key = { it.id }) { transaction ->
                AccountActivityRow(transaction, onTransactionClick)
            }
        }
        item {
            Text("Balances come from observed or reconstructed local evidence. Transfers are not new income.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccountDetailTitle(account: MoneyAccount?, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        AccountProviderMark(account?.name ?: "Account")
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(account?.name ?: "Account details", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Text(account?.identifier?.takeLast(4)?.let { "•••• $it" } ?: "Your financial account",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccountRecordedBalance(account: MoneyAccount?, balance: PaydayAccountSnapshot?) {
    val locale = LocalConfiguration.current.locales[0]
    val amount = BalancePresentationPolicy.savingsBalance(balance?.balanceMinor,
        account?.type == AccountType.SAVINGS, balance?.quality == PaydayBalanceQuality.RECONSTRUCTED)
    val quality = when (balance?.quality) {
        PaydayBalanceQuality.OBSERVED -> "Observed"
        PaydayBalanceQuality.RECONSTRUCTED -> "Estimated from activity"
        PaydayBalanceQuality.USER_ENTERED -> "Entered by you"
        else -> "No balance evidence yet"
    }
    Surface(shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
        color = AccountMint,
        border = BorderStroke(PesaDesignTokens.cardOutlineWidth, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AccountProviderMark(account?.name ?: "Account")
            Text("Recorded balance", style = MaterialTheme.typography.labelLarge,
                color = AccountBlack)
            Text(amount?.let(::formatKesAmount) ?: "Balance unavailable",
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = AccountBlack)
            Text(quality + (balance?.referenceEpochMs?.let { " · " +
                SimpleDateFormat("d MMM yyyy, h:mm a", locale).format(Date(it)) } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = AccountBlack)
            Text("Not live synced", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LinkedSavingsAccount(
    account: MoneyAccount, snapshot: PaydayAccountSnapshot?, onAccountClick: (String) -> Unit
) {
    val amount = BalancePresentationPolicy.savingsBalance(snapshot?.balanceMinor, true,
        snapshot?.quality == PaydayBalanceQuality.RECONSTRUCTED)
    Surface(onClick = { onAccountClick(account.id) },
        shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
        color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AccountProviderMark(account.name)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(account.name, fontWeight = FontWeight.SemiBold)
                Text(amount?.let(::formatKesAmount) ?: "Balance unavailable",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = "View savings")
        }
    }
}

@Composable
private fun AccountActivityRow(transaction: StoredTransaction, onClick: (Long) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val income = transaction.direction == MoneyDirection.INCOME.name
    val transfer = transaction.direction == MoneyDirection.TRANSFER.name
    Surface(onClick = { onClick(transaction.id) },
        shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
        color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBubble(when {
                income -> Icons.Rounded.SouthWest
                transfer -> Icons.Rounded.SwapHoriz
                else -> Icons.Rounded.NorthEast
            }, size = PesaDesignTokens.iconBubble)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(transaction.counterparty?.takeIf { it.isNotBlank() }
                    ?: transaction.category.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(SimpleDateFormat("d MMM yyyy", locale)
                    .format(Date(transaction.occurredAtEpochMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text((if (income) "+" else if (transfer) "" else "−") + formatKesAmount(transaction.amountMinor),
                style = MaterialTheme.typography.titleSmall)
        }
    }
}
