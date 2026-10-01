package com.kedevjimmy.pesatrack.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ingestion.HistoricalImportResult
import com.kedevjimmy.pesatrack.ingestion.HistoricalSmsImporter
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import java.util.Calendar
import java.util.concurrent.Executors

private enum class ImportWindow {
    THIS_MONTH,
    LAST_30_DAYS,
    LAST_90_DAYS
}

@Composable
fun HistoricalImportScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(hasSmsPermission(context))
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var selectedWindow by remember { mutableStateOf(ImportWindow.THIS_MONTH) }
    var importing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<HistoricalImportResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        permissionDenied = !granted
    }

    fun runImport() {
        if (!permissionGranted || importing) return
        importing = true
        error = null
        result = null

        val since = importStart(selectedWindow)
        val mainHandler = Handler(Looper.getMainLooper())
        val executor = Executors.newSingleThreadExecutor()

        executor.execute {
            val outcome = runCatching {
                HistoricalSmsImporter(context.applicationContext).importSince(since)
            }

            mainHandler.post {
                importing = false
                outcome.onSuccess {
                    result = it
                }.onFailure {
                    error = when (it) {
                        is SecurityException ->
                            "Android did not allow PesaTrack to read the SMS inbox on this installation."
                        else ->
                            it.message ?: "Historical import could not be completed."
                    }
                }
                executor.shutdown()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("Import M-Pesa history", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Read older M-Pesa and configured money SMS already on this phone.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            PesaCard {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBubble(
                        if (permissionGranted) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.Message,
                        size = 48.dp
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            if (permissionGranted) {
                                "SMS access is available"
                            } else {
                                "SMS access is not granted"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (permissionGranted) {
                                "PesaTrack can scan the selected history locally."
                            } else {
                                "Android may restrict this permission on some sideloaded installations."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (!permissionGranted) {
            item {
                Button(
                    onClick = {
                        permissionDenied = false
                        permissionLauncher.launch(Manifest.permission.READ_SMS)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null)
                    Text(
                        "Request SMS access",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            if (permissionDenied) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Column(Modifier.padding(start = 10.dp)) {
                                Text(
                                    "Historical SMS access was not granted",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "Real-time Notification Access still works. PesaTrack will not try to bypass Android security restrictions.",
                                    modifier = Modifier.padding(top = 4.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                SectionHeader("Import range")
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WindowChip(
                            "This month",
                            selectedWindow == ImportWindow.THIS_MONTH,
                            { selectedWindow = ImportWindow.THIS_MONTH },
                            Modifier.weight(1f)
                        )
                        WindowChip(
                            "30 days",
                            selectedWindow == ImportWindow.LAST_30_DAYS,
                            { selectedWindow = ImportWindow.LAST_30_DAYS },
                            Modifier.weight(1f)
                        )
                        WindowChip(
                            "90 days",
                            selectedWindow == ImportWindow.LAST_90_DAYS,
                            { selectedWindow = ImportWindow.LAST_90_DAYS },
                            Modifier.weight(1f)
                        )
                    }
                    Text(
                        "Imported transactions keep their original SMS/transaction date. Re-importing the same messages is safe because duplicates are ignored.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Button(
                    onClick = ::runImport,
                    enabled = !importing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (importing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(20.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Importing…")
                    } else {
                        Icon(Icons.Rounded.History, contentDescription = null)
                        Text(
                            "Import selected history",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        result?.let { importResult ->
            item {
                SectionHeader("Import result")
                Spacer(Modifier.height(6.dp))
                PesaCard {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ImportResultRow("SMS scanned", importResult.scanned)
                        ImportResultRow("Money messages matched", importResult.matched)
                        ImportResultRow("New transactions added", importResult.inserted)
                        if (importResult.fulizaMessages > 0) {
                            ImportResultRow("Fuliza financing messages", importResult.fulizaMessages)
                        }
                        ImportResultRow("Duplicates skipped", importResult.duplicates)
                        ImportResultRow("Unrelated messages ignored", importResult.ignored)
                    }
                }
            }
        }

        error?.let { message ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column(Modifier.padding(start = 10.dp)) {
                        Text(
                            "Three dates stay separate",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Transaction date = when money moved. SMS received date = when the phone received the message. Import time = when PesaTrack learned about it. Monthly reports use the transaction date.",
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WindowChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier
    )
}

@Composable
private fun ImportResultRow(
    label: String,
    value: Int
) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value.toString(), style = MaterialTheme.typography.titleSmall)
    }
}

private fun hasSmsPermission(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

private fun importStart(window: ImportWindow): Long {
    val calendar = Calendar.getInstance()

    when (window) {
        ImportWindow.THIS_MONTH -> {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
        ImportWindow.LAST_30_DAYS -> {
            calendar.add(Calendar.DAY_OF_YEAR, -30)
        }
        ImportWindow.LAST_90_DAYS -> {
            calendar.add(Calendar.DAY_OF_YEAR, -90)
        }
    }

    return calendar.timeInMillis
}
