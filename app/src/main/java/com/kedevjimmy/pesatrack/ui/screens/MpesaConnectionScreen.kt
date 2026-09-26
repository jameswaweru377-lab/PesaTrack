package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.core.app.NotificationManagerCompat
import com.kedevjimmy.pesatrack.data.local.LedgerSnapshot
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MpesaConnectionScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var accessGranted by remember { mutableStateOf(hasNotificationAccess(context)) }
    var snapshot by remember { mutableStateOf(TransactionStore.get(context).snapshot()) }

    fun refresh() {
        accessGranted = hasNotificationAccess(context)
        snapshot = TransactionStore.get(context).snapshot()
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refresh()
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
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
            }
            Column {
            Text("M-Pesa connection", style = MaterialTheme.typography.headlineMedium)
            Text(
            "Real-time, on-device transaction capture",
            color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            }
            }
        }

        item {
            ConnectionStatusCard(accessGranted)
        }

        item {
            if (!accessGranted) {
            Button(
            onClick = {
            launcher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
            ) {
            Icon(Icons.Rounded.Notifications, contentDescription = null)
            Text(
            "Grant notification access",
            modifier = Modifier.padding(start = 8.dp)
            )
            }
            } else {
            OutlinedButton(
            onClick = {
            launcher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
            ) {
            Icon(Icons.Rounded.Notifications, contentDescription = null)
            Text(
            "Manage notification access",
            modifier = Modifier.padding(start = 8.dp)
            )
            }
            }
        }

        item {
            SectionHeader("Live diagnostics")
            LedgerDiagnostics(snapshot)
        }

        item {
            OutlinedButton(
            onClick = { refresh() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
            ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Text("Refresh", modifier = Modifier.padding(start = 8.dp))
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
            Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Column(Modifier.padding(start = 10.dp)) {
            Text(
            "Local by design",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
            "PesaTrack filters notifications on this phone. Unrelated notifications are ignored, and raw M-Pesa message bodies are not saved. Only structured transaction fields are kept locally.",
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
private fun ConnectionStatusCard(granted: Boolean) {
    PesaCard {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.Notifications,
                size = 48.dp
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    if (granted) "M-Pesa listening is enabled" else "Notification access is required",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (granted) {
                        "New M-Pesa confirmation notifications can now be parsed locally."
                    } else {
                        "Grant PesaTrack notification access so it can see new M-Pesa confirmation messages."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LedgerDiagnostics(snapshot: LedgerSnapshot) {
    PesaCard {
        Column(Modifier.padding(16.dp)) {
            DiagnosticRow(
                icon = Icons.Rounded.ReceiptLong,
                label = "Captured transactions",
                value = snapshot.transactionCount.toString()
            )

            Spacer(Modifier.height(12.dp))

            DiagnosticRow(
                icon = Icons.Rounded.Paid,
                label = "Last known M-Pesa balance",
                value = snapshot.latestBalanceMinor?.let(::formatMoney) ?: "Not observed yet"
            )

            if (snapshot.latestBalanceObservedAtEpochMs != null) {
                Text(
                    "Observed ${formatTimestamp(snapshot.latestBalanceObservedAtEpochMs)}",
                    modifier = Modifier.padding(start = 54.dp, top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            snapshot.latestTransaction?.let { latest ->
                Spacer(Modifier.height(14.dp))
                Text(
                    "Latest captured transaction",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    latest.counterparty ?: latest.type.replace('_', ' '),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${formatMoney(latest.amountMinor)} • ${latest.type.replace('_', ' ')}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBubble(icon, size = 42.dp)
        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun hasNotificationAccess(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context)
        .contains(context.packageName)

private fun formatMoney(minor: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "KES ${formatter.format(minor / 100.0)}"
}

private fun formatTimestamp(epochMs: Long): String =
    SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(epochMs))
