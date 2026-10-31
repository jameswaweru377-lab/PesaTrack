package com.kedevjimmy.pesatrack.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.PayCycleSnapshot
import com.kedevjimmy.pesatrack.data.local.SalaryForecastRead
import com.kedevjimmy.pesatrack.data.local.SalaryForecastRepository
import com.kedevjimmy.pesatrack.domain.rules.SalaryForecastStatus
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Small local-only Plan card; expected future pay never enters today's ledger. */
@Composable
internal fun MoneyUntilSalaryCard(snapshot: PayCycleSnapshot) {
    val context = LocalContext.current
    val repository = remember { SalaryForecastRepository(context) }
    val revision by LedgerUpdateBus.state.collectAsState()
    var current by remember { mutableStateOf<SalaryForecastRead?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(snapshot.cycle?.startTransactionId, revision.revision, refresh) {
        loading = current == null
        error = false
        try {
            current = withContext(Dispatchers.IO) { repository.read(snapshot) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Log.e("PesaTrackForecast", "Could not read local forecast evidence")
            error = true
        } finally {
            loading = false
        }
    }

    VibrantPlanSurface {
        Text("Money until next salary", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        when {
            loading -> {
                CircularProgressIndicator()
                Text("Checking your local balance and salary history…")
            }
            error -> {
                Text("Could not check this forecast. Your money records are unchanged.")
                TextButton(onClick = { refresh++ }) { Text("Try again") }
            }
            current != null -> {
                ForecastDetails(requireNotNull(current))
                TextButton(onClick = { editing = !editing }) {
                    Text(if (editing) "Close forecast inputs" else "Confirm payday & balance")
                }
            }
        }
        if (editing && current != null) {
            ForecastConfirmationForm(repository, requireNotNull(current),
                snapshot.cycle?.startTransactionId) {
                editing = false
                refresh++
            }
        }
    }
}

@Composable
private fun ForecastConfirmationForm(
    repository: SalaryForecastRepository,
    read: SalaryForecastRead,
    cycleStartId: Long?,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var date by remember { mutableStateOf(read.confirmation?.dateEpochMs
        ?: read.result.paydayStartEpochMs ?: System.currentTimeMillis()) }
    var expected by remember { mutableStateOf(read.confirmation?.amountMinor?.let(::forecastInputMoney).orEmpty()) }
    var balance by remember { mutableStateOf("") }
    var debtDue by remember { mutableStateOf(read.confirmedDebtDueMinor?.let(::forecastInputMoney).orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("These are expectations only. This will not record salary or change account balances.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SalaryReceiptDateButton(date, !busy, "Expected next payday", true) { date = it }
        OutlinedTextField(value = expected, onValueChange = { expected = it },
            label = { Text("Expected salary (KES)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(), enabled = !busy)
        OutlinedTextField(value = balance, onValueChange = { balance = it },
            label = { Text("Spendable balance now (KES)") }, singleLine = true,
            supportingText = {
                Text("All spendable accounts, excluding protected savings. Blank only when all accounts have fresh observations.")
            }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(), enabled = !busy)
        if (read.hasActiveDebt) {
            OutlinedTextField(value = debtDue, onValueChange = { debtDue = it },
                label = { Text("Loan/Fuliza due before payday (KES)") }, singleLine = true,
                supportingText = { Text("Enter total repayments including fees, or 0 only if none are due.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(), enabled = !busy)
        }
        formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
            val amount = parseSalaryAmount(expected)
            val cash = if (balance.isBlank()) null else parseForecastBalance(balance)
            val debt = if (read.hasActiveDebt) parseForecastBalance(debtDue) else null
            formError = when {
                amount == null -> "Enter a valid positive expected salary."
                balance.isNotBlank() && cash == null -> "Enter a valid spendable balance."
                cash == null && !read.allowsBlankBalance -> "Confirm the current total spendable balance."
                read.hasActiveDebt && (debt == null || debt < 0L) -> "Confirm the loan/Fuliza amount due before payday (0 if none)."
                else -> null
            }
            if (formError == null && amount != null) {
                busy = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { repository.confirm(date, amount, cash, debt, cycleStartId) }
                        onSaved()
                    } catch (cancelled: CancellationException) { throw cancelled
                    } catch (invalid: IllegalArgumentException) {
                        formError = invalid.message ?: "Review the date and amounts."
                    } catch (_: Exception) {
                        Log.e("PesaTrackForecast", "Could not save local forecast inputs")
                        formError = "Could not save these inputs. Try again."
                    } finally { busy = false }
                }
            }
        }) { Text(if (busy) "Saving…" else "Save forecast inputs") }
    }
}

@Composable
private fun ForecastDetails(read: SalaryForecastRead) {
    val result = read.result
    val period = result.paydayStartEpochMs?.let(::forecastDate)
    val end = result.paydayEndEpochMs?.let(::forecastDate)
    when (result.status) {
        SalaryForecastStatus.CONFIRMED -> Text("You confirmed the next payday: $period.",
            style = MaterialTheme.typography.bodyMedium)
        SalaryForecastStatus.ESTIMATED -> Text("Estimated usual payday window: $period – $end. Not confirmed.",
            style = MaterialTheme.typography.bodyMedium)
        SalaryForecastStatus.NEEDS_DATA -> {
            Text("Not enough verified data for a money range.",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (period != null && end != null) Text("Payday clue: $period – $end (not a received salary).",
                style = MaterialTheme.typography.bodySmall)
        }
    }
    if (result.lowMinor != null && result.highMinor != null) {
        Text("${forecastMoney(result.lowMinor)} – ${forecastMoney(result.highMinor)}",
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Estimated amount left immediately before payday; not your current balance.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (result.lowMinor < 0L) Text("At risk of a shortfall before payday.",
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
    }
    Text(read.balanceSource, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    read.confirmation?.let {
        Text("Expected salary: ${forecastMoney(it.amountMinor)} (not counted until received).",
            style = MaterialTheme.typography.bodySmall)
    }
    if (result.missing.isNotEmpty()) {
        Text("Needed:", style = MaterialTheme.typography.labelLarge)
        result.missing.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
    }
    Text("Included:", style = MaterialTheme.typography.labelLarge)
    (result.included.ifEmpty { listOf("No money estimate until the missing evidence is confirmed.") })
        .forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
    Text("Excluded:", style = MaterialTheme.typography.labelLarge)
    result.excluded.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
}

private fun parseForecastBalance(input: String): Long? = runCatching {
    input.trim().replace(",", "").toBigDecimal().movePointRight(2).longValueExact()
}.getOrNull()

private fun forecastInputMoney(minor: Long): String =
    BigDecimal.valueOf(minor, 2).toPlainString()

private fun forecastMoney(minor: Long): String =
    "KES " + NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(BigDecimal.valueOf(minor, 2))

private fun forecastDate(epochMs: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMs))
