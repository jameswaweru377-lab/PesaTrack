package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.planning.LocalInboxNotice
import com.kedevjimmy.pesatrack.planning.LocalNotificationInboxPolicy
import com.kedevjimmy.pesatrack.planning.LocalNotificationInboxStore
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens

/** Read-only history of locally delivered reminders; no access to SMS contents. */
@Composable
fun NotificationCenterScreen(
    padding: PaddingValues,
    onBack: () -> Unit,
    onOpen: (String) -> Unit
) {
    val context = LocalContext.current
    val revision by LocalNotificationInboxStore.revision.collectAsState()
    val alerts = remember(context, revision) { LocalNotificationInboxStore.list(context) }
    val unread = LocalNotificationInboxPolicy.unreadCount(alerts)
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().background(colors.background).padding(padding)
            .padding(horizontal = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
            }
            Text("Notifications", modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        if (alerts.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(if (unread == 0) "All caught up" else "$unread unread",
                    Modifier.weight(1f), color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
                if (unread > 0) TextButton(onClick = {
                    LocalNotificationInboxStore.markAllRead(context)
                }) { Text("Mark all read") }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(alerts, key = { it.key }) { alert ->
                    InboxNotificationRow(alert) {
                        LocalNotificationInboxStore.markRead(context, alert.key)
                        onOpen(alert.route)
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.NotificationsNone, null,
                    modifier = Modifier.size(48.dp), tint = colors.primary)
                Spacer(Modifier.height(16.dp))
                Text("No notifications yet", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("New reminders from PesaTrack will appear here.",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun InboxNotificationRow(alert: LocalInboxNotice, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp)
            .background(colors.surfaceVariant, RoundedCornerShape(PesaDesignTokens.cardRadius))
            .clickable(onClick = onClick).padding(16.dp)
            .heightIn(min = PesaDesignTokens.minimumTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(alert.category, color = colors.primary,
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            Text(alert.title, color = colors.onSurface, maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (alert.read) FontWeight.Medium else FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge)
            Text(alert.summary, color = colors.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall)
        }
        if (!alert.read) Badge(containerColor = colors.primary)
        Icon(Icons.Rounded.ChevronRight, null, tint = colors.onSurfaceVariant)
    }
}
