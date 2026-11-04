package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun ReportExportButton(content: ReportPdfContent?, csvRows: List<StoredTransaction>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val primary = MaterialTheme.colorScheme.primary
    var showConfirm by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<ReportPdfContent?>(null) }
    var pendingCsv by remember { mutableStateOf(emptyList<StoredTransaction>()) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val selected = pending
        if (uri != null && selected != null) scope.launch {
            saving = true
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        writePesaTrackReportPdf(context, selected, stream, primary.toArgb())
                    } ?: error("Unable to open the selected destination")
                }
                message = "PDF report saved to your chosen location."
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                android.util.Log.e("PesaTrackReports", "Could not export local report")
                message = "Couldn't save the PDF. Please choose another location and try again."
            } finally {
                saving = false
                pending = null
            }
        } else pending = null
    }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) scope.launch {
            saving = true
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                        writePesaTrackReportCsv(pendingCsv, it)
                    } ?: error("Unable to open the selected destination")
                }
                message = "CSV report saved to your chosen location."
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                android.util.Log.e("PesaTrackReports", "Could not export CSV")
                message = "Couldn't save the CSV. Please try again."
            } finally {
                saving = false
                pendingCsv = emptyList()
            }
        } else pendingCsv = emptyList()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PesaCard(Modifier.fillMaxWidth().clickable(enabled = content != null && !saving) {
            showConfirm = true
        }) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBubble(Icons.Rounded.PictureAsPdf)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Export report", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text("Save a PesaTrack PDF for this period",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (saving) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else Icon(Icons.Rounded.ChevronRight, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
        message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (showConfirm && content != null) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Export personal PDF?") },
            text = {
                Text("Your report includes trusted Activity totals, category and spending trends, " +
                    "and a short list of large expenses. It is not a bank statement. " +
                    "The exported file is not encrypted, so keep it private.")
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = content
                    showConfirm = false
                    val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                    launcher.launch("PesaTrack-Money-Report-$date.pdf")
                }) { Text("Choose save location") }
            },
            dismissButton = {
                Row {
                    if (csvRows.isNotEmpty()) TextButton(onClick = {
                        pendingCsv = csvRows
                        showConfirm = false
                        csvLauncher.launch("PesaTrack-report.csv")
                    }) { Text("CSV instead") }
                    TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
                }
            }
        )
    }
}
