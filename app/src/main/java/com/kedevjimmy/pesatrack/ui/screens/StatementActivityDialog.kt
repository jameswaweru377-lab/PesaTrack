package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kedevjimmy.pesatrack.data.local.StatementActivityStore
import com.kedevjimmy.pesatrack.domain.everyday.StatementActivityLine
import com.kedevjimmy.pesatrack.domain.everyday.StatementMonthlyUsage
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class StatementView { MONTHS, TRANSACTIONS }

/**
 * One-PDF-at-a-time, read-only statement history. Monthly Money Out is
 * withdrawn wallet movement, NOT necessarily purchase spending. We do not
 * combine overlapping PDFs or feed calculations into SMS Activity or budgets.
 */
@Composable
internal fun StatementActivityDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { StatementActivityStore(context) }
    var batches by remember { mutableStateOf<List<StatementActivityStore.Batch>>(emptyList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var lines by remember { mutableStateOf<List<StatementActivityLine>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var view by remember { mutableStateOf(StatementView.MONTHS) }
    var selectedMonth by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedId) {
        loading = true
        failed = false
        try {
            if (selectedId == null) {
                batches = withContext(Dispatchers.IO) { store.batches() }
                lines = emptyList()
            } else {
                val id = requireNotNull(selectedId)
                lines = withContext(Dispatchers.IO) { store.lines(id) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failed = true
        } finally {
            loading = false
        }
    }

    val chosen = batches.firstOrNull { it.id == selectedId }
    val months = remember(selectedId, lines) { StatementMonthlyUsage.summarize(lines) }
    val displayed = remember(selectedId, lines, selectedMonth) {
        if (selectedMonth == null) lines
        else lines.filter { StatementMonthlyUsage.monthKey(it.occurredAt) == selectedMonth }
    }
    val timeFormatter = remember {
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Africa/Nairobi")
        }
    }
    fun money(minor: Long): String =
        "KES " + BigDecimal.valueOf(minor, 2).toPlainString()

    fun back() {
        if (selectedId == null) onDismiss()
        else if (view == StatementView.TRANSACTIONS) {
            view = StatementView.MONTHS
            selectedMonth = null
        } else {
            selectedId = null
            view = StatementView.MONTHS
            selectedMonth = null
        }
    }

    Dialog(
        onDismissRequest = { if (!loading) back() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { back() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(
                            if (chosen == null) "Statement Activity"
                            else if (view == StatementView.MONTHS) "Monthly overview"
                            else "Statement transactions",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Saved PDF records · Separate from SMS Activity",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()

                if (loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Opening local statement records…", Modifier.padding(16.dp))
                } else if (failed) {
                    Text(
                        "Could not open the saved statement. Your ordinary transactions are unchanged.",
                        Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (chosen == null) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (batches.isEmpty()) item {
                            Text("No statement PDFs have been saved yet.")
                        }
                        items(batches, key = { it.id }) { batch ->
                            PesaCard {
                                Column(
                                    Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    Text("M-Pesa statement", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${batch.rows} source rows · ${batch.movements} money movements · " +
                                            "${batch.informational} informational events",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "Money In ${money(batch.paidInMinor)} · Money Out ${money(batch.paidOutMinor)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        if (batch.totalsIndependentlyVerified)
                                            "Detailed amounts match the printed statement totals"
                                        else "Official totals not independently verified",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(onClick = {
                                        selectedMonth = null
                                        view = StatementView.MONTHS
                                        selectedId = batch.id
                                    }) {
                                        Text("View monthly usage")
                                    }
                                }
                            }
                        }
                    }
                } else if (view == StatementView.MONTHS) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            PesaCard {
                                Column(
                                    Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    Text(
                                        "This statement",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${months.size} month(s) · ${chosen.rows} original rows",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text("Total Money In: ${money(chosen.paidInMinor)}")
                                    Text("Total Money Out: ${money(chosen.paidOutMinor)}")
                                    Text(
                                        "Net Movement: ${money(Math.subtractExact(chosen.paidInMinor, chosen.paidOutMinor))}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { selectedMonth = null; view = StatementView.TRANSACTIONS }
                                    ) {
                                        Text("View all ${chosen.rows} statement rows")
                                    }
                                }
                            }
                        }
                        item {
                            Text(
                                "Monthly money movement",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Money Out includes transfers, savings movements, fees and loan payments—not just purchases. " +
                                    "Partial statement months may not show a full month's activity.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (months.isEmpty()) item {
                            Text("This saved statement has no readable transaction dates.")
                        }
                        items(months, key = { it.key }) { month ->
                            PesaCard {
                                Column(
                                    Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    Text(
                                        month.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Money In", style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(money(month.paidInMinor), style = MaterialTheme.typography.titleSmall)
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text("Money Out", style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(money(month.paidOutMinor), style = MaterialTheme.typography.titleSmall)
                                        }
                                    }
                                    Text(
                                        "Net movement: ${money(month.netMinor)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "${month.financialMovements} money movements · " +
                                            "${month.informationalRows} informational · ${month.sourceRows} rows",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            selectedMonth = month.key
                                            view = StatementView.TRANSACTIONS
                                        }
                                    ) {
                                        Text("View ${month.label} transactions")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedMonth == null,
                                    onClick = { selectedMonth = null },
                                    label = { Text("All") }
                                )
                            }
                            items(months, key = { it.key }) { month ->
                                FilterChip(
                                    selected = selectedMonth == month.key,
                                    onClick = { selectedMonth = month.key },
                                    label = { Text(month.label) }
                                )
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    "${displayed.size} source rows" +
                                        (months.firstOrNull { it.key == selectedMonth }?.let { " · ${it.label}" } ?: " · All dates"),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "These rows are sourced from the saved PDF, not SMS Activity.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(displayed, key = { "${it.page}:${it.row}:${it.reference}" }) { line ->
                                PesaCard {
                                    Column(
                                        Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(line.description, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "${timeFormatter.format(Date(line.occurredAt))} · ${line.reference} · " +
                                                "Page ${line.page}, row ${line.row}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            if (line.informational) "Informational · no wallet movement"
                                            else if (line.movementMinor > 0L)
                                                "Money In ${money(line.movementMinor)}"
                                            else "Money Out ${money(Math.negateExact(line.movementMinor))}"
                                        )
                                        Text(
                                            "Reported balance ${money(line.balanceMinor)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
