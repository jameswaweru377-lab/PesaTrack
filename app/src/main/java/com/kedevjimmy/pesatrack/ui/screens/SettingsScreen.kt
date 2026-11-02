package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.PesaCard

import com.kedevjimmy.pesatrack.ui.theme.ThemeMode

@Composable
fun SettingsScreen(scaffoldPadding: PaddingValues, themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit, onProfileClick: () -> Unit, onMpesaConnectionClick: () -> Unit,
    onSmartRulesClick: () -> Unit, onHistoricalImportClick: () -> Unit, onAdvancedClick: () -> Unit = {}) {
    // Global navigation owns Profile, Notifications, Appearance, App Lock,
    // Privacy, Backup, Help and legal pages. Do not duplicate them here.
    EverydayPage("Settings", scaffoldPadding, onBack) {
        item {
            Text(
                "Automation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            SettingsGroup {
                SettingsCompactRow(
                    Icons.Rounded.Tune,
                    "Automatic sorting",
                    "Salary, regular payments and transfers",
                    onSmartRulesClick
                )
            }
        }
        item {
            Text(
                "Manage notifications, security, backups and support from More.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    PesaCard(Modifier.fillMaxWidth()) { Column { content() } }
}

@Composable
private fun SettingsCompactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 64.dp).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f), thickness = .7.dp)
}
