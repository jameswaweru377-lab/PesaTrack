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
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.saveable.rememberSaveable
import com.kedevjimmy.pesatrack.data.local.SmsAccessConsentStore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ingestion.HistoricalImportProgress
import com.kedevjimmy.pesatrack.ingestion.HistoricalImportResult
import com.kedevjimmy.pesatrack.ingestion.HistoricalSmsImporter
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import java.util.Calendar
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

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
    var showDateHelp by rememberSaveable { mutableStateOf(false) }
    var consentAccepted by rememberSaveable { mutableStateOf(SmsAccessConsentStore.hasConsent(context)) }
    var permissionGranted by remember {
        mutableStateOf(hasSmsPermission(context))
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var selectedWindow by remember { mutableStateOf(ImportWindow.THIS_MONTH) }
    var importing by remember { mutableStateOf(false) }
    var stopping by remember { mutableStateOf(false) }
    var progress by remember {
        mutableStateOf<HistoricalImportProgress?>(null)
    }
    var result by remember { mutableStateOf<HistoricalImportResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val cancelRequested = remember {
        AtomicBoolean(false)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        permissionDenied = !granted
    }

    fun runImport() {
        if (!permissionGranted || importing || !consentAccepted) return
        if (!SmsAccessConsentStore.setAccepted(context, true)) {
            error = "Your SMS consent could not be saved. Please try again."
            return
        }
        importing = true
        stopping = false
        cancelRequested.set(false)
        progress = null
        error = null
        result = null

        val since = importStart(selectedWindow)
        val mainHandler = Handler(Looper.getMainLooper())
        val executor = Executors.newSingleThreadExecutor()

        executor.execute {
            val outcome = runCatching {
                HistoricalSmsImporter(
                    context.applicationContext
                ).importSince(
                    sinceEpochMs = since,
                    onProgress = { update ->
                        mainHandler.post {
                            progress = update
                        }
                    },
                    isCancelled = {
                        cancelRequested.get()
                    },
                    resumeKey =
                        selectedWindow.name
                )
            }

            mainHandler.post {
                importing = false
                stopping = false
                outcome.onSuccess {
                    result = it
                }.onFailure {
                    error = when (it) {
                        is SecurityException ->
                            if (!SmsAccessConsentStore.hasConsent(context)) "SMS consent is off. Accept the disclosure before importing history."
                            else "Android did not allow PesaTrack to read the SMS inbox on this installation."
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("Add past transactions", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Read older money messages from this phone.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            PesaCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Before reading SMS", style = MaterialTheme.typography.titleMedium)
                    Text("With your agreement and Android permission, PesaTrack reads financial SMS to organise income, spending, transfers, balances and debts on this device. Full messages are not uploaded. Optional online merchant lookup is a separate choice using limited descriptors. You choose the import window; recent SMS catch-up can run when you reopen the app. Unrelated messages are not added to your ledger. You can continue using the app without this access.",
                        style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = consentAccepted, enabled = !importing, onCheckedChange = { accepted ->
                            if (accepted || SmsAccessConsentStore.setAccepted(context, false)) {
                                consentAccepted = accepted
                            } else error = "Your choice could not be saved. Please try again."
                        })
                        Text("I agree to this use of my financial SMS", style = MaterialTheme.typography.bodySmall)
                    }
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
                                "SMS access is on"
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
                        if (consentAccepted && SmsAccessConsentStore.setAccepted(context, true)) {
                            permissionLauncher.launch(Manifest.permission.READ_SMS)
                        } else error = "Accept the disclosure before allowing SMS access."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = consentAccepted,
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
                    enabled = !importing && consentAccepted,
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
                        Text(
                            progress?.let {
                                "Scanning ${it.scanned} of ${it.total}…"
                            } ?: "Preparing import…"
                        )
                    } else {
                        Icon(Icons.Rounded.History, contentDescription = null)
                        Text(
                            "Add transactions",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            if (importing) {
                item {
                    PesaCard {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                progress?.let {
                                    if (it.resumed) {
                                        "Resuming import • ${it.scanned} of ${it.total} remaining SMS scanned"
                                    } else {
                                        "${it.scanned} of ${it.total} SMS scanned"
                                    }
                                } ?: "Reading the selected SMS history…",
                                style =
                                    MaterialTheme.typography
                                        .titleMedium
                            )
                            progress?.let {
                                Text(
                                    "${it.matched} money messages matched • ${it.inserted} new records • ${it.duplicates} duplicates",
                                    color =
                                        MaterialTheme.colorScheme
                                            .onSurfaceVariant
                                )
                            }
                            Text(
                                "You can stop safely. Records already added stay valid. The next run can resume from a recent saved checkpoint, and duplicate protection remains active.",
                                style =
                                    MaterialTheme.typography
                                        .bodySmall,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    stopping = true
                                    cancelRequested.set(true)
                                },
                                enabled = !stopping,
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (stopping) {
                                        "Stopping…"
                                    } else {
                                        "Stop import"
                                    }
                                )
                            }
                        }
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
                        ImportResultRow("Duplicates skipped", importResult.duplicates)
                        ImportResultRow("Unrelated messages ignored", importResult.ignored)
                        if (importResult.resumed) {
                            Text(
                                "This run resumed from the most recent saved checkpoint instead of rescanning the completed part of the interrupted import.",
                                style =
                                    MaterialTheme.typography
                                        .bodySmall,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                            )
                        }
                        if (importResult.cancelled) {
                            Text(
                                "Import stopped before the selected history was fully scanned. Records already added were kept and a recent checkpoint is retained so the next run can continue without rescanning the completed part.",
                                style =
                                    MaterialTheme.typography
                                        .bodySmall,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                            )
                        }
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
                        TextButton(onClick = { showDateHelp = !showDateHelp }) {
                            Text(if (showDateHelp) "Hide date explanation" else "About transaction dates")
                        }
                        if (showDateHelp) Text(
                            "Reports use the date the money moved. The message may arrive later, and adding it to PesaTrack later does not change that date.",
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
