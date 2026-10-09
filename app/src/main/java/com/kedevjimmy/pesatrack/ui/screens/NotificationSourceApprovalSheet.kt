package com.kedevjimmy.pesatrack.ui.screens

import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ingestion.NotificationSourceTrustPolicy
import com.kedevjimmy.pesatrack.ingestion.NotificationSourceTrustStore
import com.kedevjimmy.pesatrack.ingestion.ObservedNotificationSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSourceApprovalSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val trust = remember { NotificationSourceTrustStore(context) }
    val sources = remember {
        val visible = runCatching {
            context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA).map { info ->
                ObservedNotificationSource(info.packageName, context.packageManager.getApplicationLabel(info).toString())
            }
        }.getOrDefault(emptyList())
        val sms = runCatching {
            Telephony.Sms.getDefaultSmsPackage(context)?.let { name ->
                val info = context.packageManager.getApplicationInfo(name, 0)
                ObservedNotificationSource(name, context.packageManager.getApplicationLabel(info).toString())
            }
        }.getOrNull()
        (trust.observedSources() + listOfNotNull(sms) + visible)
            .filter { NotificationSourceTrustPolicy.isEligible(it.packageName, context.packageName) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
    var query by remember { mutableStateOf("") }
    var details by remember { mutableStateOf(false) }
    val observed = remember { trust.observedSources().map { it.packageName }.toSet() }
    val filtered = sources.filter { source -> if(query.isNotBlank()) source.label.contains(query,true) || source.packageName.contains(query,true) else source.packageName in observed || trust.isApproved(source.packageName) || Regex("mpesa|m-pesa|equity|kcb|ncba|co.op|absa|bank",RegexOption.IGNORE_CASE).containsMatchIn(source.label) }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Apps to read money updates from", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Turn on your bank, mobile-money or SMS app. We only add money updates from apps you choose.")
                Spacer(Modifier.height(8.dp))
                Text("Choose the actual app you use. Avoid chat apps or forwarded messages.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(query, { query=it }, label = { Text("Find an app") }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { details=!details }) { Text(if(details) "Hide technical names" else "Show technical names") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            if (filtered.isEmpty()) item {
                Text("Apps will appear here after posting a notification. Only their app name and package are retained before approval.")
            }
            items(filtered, key = { it.packageName }) { source ->
                var checked by remember(source.packageName) { mutableStateOf(trust.isApproved(source.packageName)) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(source.label, style = MaterialTheme.typography.titleMedium)
                        if (details) Text(source.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        modifier = Modifier.semantics {
                            contentDescription = "Approve financial notifications from ${source.label}"
                        },
                        checked = checked,
                        onCheckedChange = { enabled ->
                            if (trust.setApproved(source.packageName, enabled)) {
                                trust.observe(source.packageName, source.label)
                                checked = enabled
                                error = null
                            } else error = "Could not save this approval. Please try again."
                        }
                    )
                }
            }
            item {
                Text("Turning an app off stops new reading. Your saved transactions remain. Updates received while an app was off are not added later.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
