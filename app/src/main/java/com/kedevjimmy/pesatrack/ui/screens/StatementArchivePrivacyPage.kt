package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.StatementActivityStore
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/** Per-file privacy control. Never deletes a canonical transaction or debt. */
@Composable
internal fun StatementArchivePrivacyPage(padding: PaddingValues, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember(context) { StatementActivityStore(context) }
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var batches by remember { mutableStateOf<List<StatementActivityStore.Batch>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<StatementActivityStore.Batch?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refresh) {
        loading = true
        try {
            batches = withContext(Dispatchers.IO) { store.batches() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            message = "Could not load saved statements. Try again."
        } finally {
            loading = false
        }
    }

    EverydayPage("Saved statement files", padding, onBack) {
        item {
            PesaCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your statement history", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(
                        "Remove individual imported PDF archives that you no longer want PesaTrack to store. " +
                            "Only the chosen saved statement and its per-row labels are removed. " +
                            "Previously imported Activity transactions, confirmed loans and saved merchant rules stay unchanged. " +
                            "Original PDFs and exported backups outside PesaTrack must be deleted separately.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        message?.let { status -> item { Text(status, style = MaterialTheme.typography.bodySmall) } }
        if (!loading && batches.isEmpty()) item {
            Text("No saved PDF statements on this device.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(batches.size, key = { batches[it].id }) { index ->
            val batch = batches[index]
            PesaCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(batch.recordedAt)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("${batch.rows} original rows · ${batch.movements} cash movements",
                        style = MaterialTheme.typography.bodySmall)
                    Text(
                        if (batch.totalsIndependentlyVerified) "Statement totals verified"
                        else "Statement totals not verified",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { selected = batch; message = null },
                        enabled = !deleting && !loading
                    ) { Text("Delete this saved PDF history") }
                }
            }
        }
    }

    val proposed = selected
    if (proposed != null) AlertDialog(
        onDismissRequest = { if (!deleting) selected = null },
        title = { Text("Delete this saved statement?") },
        text = {
            Text(
                "Permanently remove ${proposed.rows} archived statement rows saved on " +
                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(proposed.recordedAt)) +
                    "? The archive and its row-level labels will be removed. " +
                    "Activity transactions, loan records, exported backups, original PDFs and shared merchant rules stay unchanged. " +
                    "You can re-import the PDF later if you still have the original."
            )
        },
        confirmButton = {
            Button(
                enabled = !deleting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = {
                    deleting = true
                    scope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) {
                                store.deleteSavedStatement(proposed)
                            }
                            selected = null
                            message = if (result) "Saved statement deleted. Other financial records were unchanged."
                                else "This statement changed or no longer exists. Refresh and review before deleting."
                            refresh++
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            message = "Could not delete the saved statement. No other financial records were changed."
                        } finally {
                            deleting = false
                        }
                    }
                }
            ) { Text(if (deleting) "Deleting…" else "Delete saved statement") }
        },
        dismissButton = {
            TextButton(enabled = !deleting, onClick = { selected = null }) { Text("Keep statement") }
        }
    )
}
