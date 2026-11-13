package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens as P
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens

@Composable
internal fun ProfileMoneyWorkspace(
    statements: () -> Unit, imports: () -> Unit,
    reports: () -> Unit, payCycle: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Money workspace", style = MaterialTheme.typography.titleLarge,
            color = P.ink, fontWeight = FontWeight.Bold)
        Surface(
            color = P.card, shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
            border = BorderStroke(1.dp, P.sandBorder)
        ) {
            Column {
                ProfileWorkspaceRow("Statements", "Your imported statements",
                    Icons.Rounded.Description, statements)
                HorizontalDivider(color = P.sandBorder)
                ProfileWorkspaceRow("Import statements", "PDF, CSV and financial records",
                    Icons.Rounded.FileDownload, imports)
                HorizontalDivider(color = P.sandBorder)
                ProfileWorkspaceRow("Reports", "Spending and money insights",
                    Icons.Rounded.BarChart, reports)
                HorizontalDivider(color = P.sandBorder)
                ProfileWorkspaceRow("Pay cycle", "Salary dates and pay history",
                    Icons.Rounded.CalendarMonth, payCycle)
            }
        }
    }
}

@Composable
internal fun ProfileSettingsEntry(onSettings: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge,
            color = P.ink, fontWeight = FontWeight.Bold)
        Surface(
            color = P.card, shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
            border = BorderStroke(1.dp, P.sandBorder)
        ) {
            ProfileWorkspaceRow(
                "App preferences", "Notifications, security, appearance and data",
                Icons.Rounded.Settings, onSettings
            )
        }
    }
}

@Composable
private fun ProfileWorkspaceRow(
    name: String, subtitle: String, icon: ImageVector, onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .heightIn(min = PesaDesignTokens.minimumTouchTarget)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = P.coffee,
            modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = P.ink, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = P.muted, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null,
            tint = P.coffee, modifier = Modifier.size(20.dp))
    }
}

private data class SettingsDestination(
    val name: String, val hint: String, val icon: ImageVector, val route: String
)

@Composable
internal fun ProfileSettingsHub(
    padding: PaddingValues, back: () -> Unit, onDestination: (String) -> Unit
) {
    val preferences = listOf(
        SettingsDestination("Notifications", "Bills, budgets and reminders",
            Icons.Rounded.NotificationsNone, "notifications"),
        SettingsDestination("App lock & security", "PIN and fingerprint",
            Icons.Rounded.Lock, "security"),
        SettingsDestination("Appearance", "Theme and display", Icons.Rounded.Palette, "appearance"),
        SettingsDestination("Privacy & data", "Permissions and data control",
            Icons.Rounded.Shield, "privacy"),
        SettingsDestination("Automatic sorting", "Message rules and access",
            Icons.Rounded.Tune, "legacy")
    )
    val tools = listOf(
        SettingsDestination("Backup & restore", "Protected local backups",
            Icons.Rounded.Backup, "backup"),
        SettingsDestination("Help & support", "Find answers",
            Icons.Rounded.HelpOutline, "support"),
        SettingsDestination("Advanced troubleshooting", "App diagnostics",
            Icons.Rounded.Build, "advanced"),
        SettingsDestination("Terms of service", "Your agreement",
            Icons.Rounded.Description, "terms"),
        SettingsDestination("About PesaTrack", "App information",
            Icons.Rounded.Info, "about")
    )
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = back) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text("Settings", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = P.ink)
            }
        }
        item { SettingsSection("App preferences", preferences, onDestination) }
        item { SettingsSection("Support & tools", tools, onDestination) }
    }
}

@Composable
private fun SettingsSection(
    title: String, entries: List<SettingsDestination>, onDestination: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, color = P.coffee)
        Surface(
            shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
            color = P.card, border = BorderStroke(1.dp, P.sandBorder)
        ) {
            Column {
                entries.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = P.sandBorder)
                    ProfileWorkspaceRow(item.name, item.hint, item.icon) {
                        onDestination(item.route)
                    }
                }
            }
        }
    }
}
