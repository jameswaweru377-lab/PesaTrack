package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.DebtAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.DebtBalanceQuality
import com.kedevjimmy.pesatrack.data.local.DebtEventRecord
import com.kedevjimmy.pesatrack.data.local.DebtEventType
import com.kedevjimmy.pesatrack.data.local.DebtStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Recorded debt history only: an SMS-derived estimate must never be presented as a live lender balance. */
@Composable
internal fun SelectedDebtDetailScreen(
    padding: PaddingValues, debt: DebtAccountSnapshot, store: DebtStore,
    onBack: () -> Unit, onConfirmBalance: () -> Unit
) {
    val revision by LedgerUpdateBus.state.collectAsState()
    val locale = LocalConfiguration.current.locales[0]
    var history by remember(debt.id) { mutableStateOf<List<DebtEventRecord>>(emptyList()) }
    var loaded by remember(debt.id) { mutableStateOf(false) }
    var error by remember(debt.id) { mutableStateOf(false) }
    var retry by remember(debt.id) { mutableIntStateOf(0) }
    LaunchedEffect(debt.id, revision.revision, retry) {
        try {
            history = withContext(Dispatchers.IO) { store.recentEvents(debt.id, limit = 50) }
            loaded = true
            error = false
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = true
        }
    }
    val unknown = hasUnknownDebtBalance(debt)
    val drawn = sumRecentDebtEvents(history, DebtEventType.DRAWDOWN)
    val repaid = sumRecentDebtEvents(history, DebtEventType.REPAYMENT)
    val fees = sumRecentDebtEvents(history, DebtEventType.FEE)
    LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                AccountProviderMark(debt.name)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(debt.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Borrowing history", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = AccountMint) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccountBalanceWallet, null,
                            modifier = Modifier.size(45.dp), tint = AccountBlack)
                        Column(Modifier.padding(start = 14.dp)) {
                            Text("Recorded outstanding", color = AccountBlack)
                            Text(if (unknown) "Balance needs confirmation"
                                else formatKesAmount(debt.outstandingMinor),
                                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    Text(when (debt.balanceQuality) {
                        DebtBalanceQuality.OBSERVED -> "Observed from recorded evidence"
                        DebtBalanceQuality.ESTIMATED -> "Estimated from saved activity"
                        DebtBalanceQuality.UNKNOWN -> "Amount could not be established"
                    }, style = MaterialTheme.typography.bodySmall)
                    Text("Not a live lender balance", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (debt.hasUnresolvedDrawdown || unknown) Text("⚠ Review borrowing and fee evidence",
                        color = Color(0xFF98601A), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0xFFE0E4E0)),
                color = Color.White) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Drawn" to drawn, "Repaid" to repaid, "Fees" to fees).forEach { (label, amount) ->
                        Column(Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.bodySmall)
                            Text(if (!loaded || amount == null) "—" else formatKesAmount(amount),
                                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Text("Totals above are from the latest ${history.size} recorded events; they may not cover all borrowing history.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { Text("History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (error) item {
            Text("Couldn't load recorded events.", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { retry++ }) { Text("Retry") }
        }
        if (loaded && history.isEmpty()) item {
            Text("No individual borrowing events recorded yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(history, key = { it.id }) { event ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ReceiptLong, null, tint = AccountBlack)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(when (event.type) {
                        DebtEventType.DRAWDOWN -> "Borrowing recorded"
                        DebtEventType.REPAYMENT -> "Repayment recorded"
                        DebtEventType.FEE -> "Service fee recorded"
                        DebtEventType.ADJUSTMENT -> "Balance adjustment"
                    }, fontWeight = FontWeight.SemiBold)
                    Text(SimpleDateFormat("d MMM yyyy", locale)
                        .format(Date(event.occurredAtEpochMs)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatKesAmount(event.amountMinor), fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(color = Color(0xFFE2E4E2))
        }
        item {
            OutlinedButton(onClick = onConfirmBalance, modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, AccountBlack)) {
                Text("Review balance", color = AccountBlack)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, null, tint = AccountBlack, modifier = Modifier.size(18.dp))
                Text("Linked transactions remain unchanged until you confirm a review.",
                    Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun sumRecentDebtEvents(history: List<DebtEventRecord>, type: DebtEventType): Long? =
    runCatching {
        history.filter { it.type == type }.fold(0L) { sum, row -> Math.addExact(sum, row.amountMinor) }
    }.getOrNull()
