package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kedevjimmy.pesatrack.data.local.StatementActivityStore
import com.kedevjimmy.pesatrack.domain.everyday.StatementActivityLine
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Read-only private statement archive. Never queries the SMS/Activity ledger. */
@Composable
internal fun StatementActivityDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { StatementActivityStore(context) }
    var batches by remember { mutableStateOf<List<StatementActivityStore.Batch>>(emptyList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var lines by remember { mutableStateOf<List<StatementActivityLine>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }

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
        } catch (_: Exception) {
            failed = true
        } finally {
            loading = false
        }
    }

    val chosen = batches.firstOrNull { it.id == selectedId }
    val timeFormatter = remember {
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Africa/Nairobi")
        }
    }
    fun money(minor: Long) = "KES " + BigDecimal.valueOf(minor, 2).toPlainString()

    Dialog(
        onDismissRequest = { if (!loading) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(8.dp)) {
                    IconButton(onClick = {
                        if (selectedId != null) selectedId = null else onDismiss()
                    }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text("Statement Activity", style = MaterialTheme.typography.titleLarge)
                        Text("Separate from SMS Activity and your budgets",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider()
                if (loading) {
                    Text("Opening local statement records…", Modifier.padding(16.dp))
                } else if (failed) {
                    Text("Could not open Statement Activity. Your ordinary transactions are unchanged.",
                        Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
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
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("M-Pesa statement", style = MaterialTheme.typography.titleMedium)
                                    Text("${batch.rows} source rows · ${batch.movements} money movements · ${batch.informational} informational events")
                                    Text("Paid In ${money(batch.paidInMinor)} · Paid Out ${money(batch.paidOutMinor)}",
                                        style = MaterialTheme.typography.bodySmall)
                                    Text(if (batch.totalsIndependentlyVerified)
                                        "Detailed amounts match official statement totals"
                                    else "Official totals not independently verified",
                                        style = MaterialTheme.typography.bodySmall)
                                    Button(onClick = { selectedId = batch.id }) {
                                        Text("View statement")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text("${chosen.rows} source rows, including ${chosen.informational} zero-value events",
                                style = MaterialTheme.typography.titleMedium)
                        }
                        items(lines) { line ->
                            PesaCard {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(line.description, style = MaterialTheme.typography.bodyMedium)
                                    Text("${timeFormatter.format(Date(line.occurredAt))} · ${line.reference} · Page ${line.page}, row ${line.row}",
                                        style = MaterialTheme.typography.bodySmall)
                                    Text(if (line.informational) "Informational · no wallet movement"
                                        else if (line.movementMinor > 0L) "Paid In ${money(line.movementMinor)}"
                                        else "Withdrawn ${money(Math.negateExact(line.movementMinor))}")
                                    Text("Reported balance ${money(line.balanceMinor)}",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
