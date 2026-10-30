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
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf("settings") }
    var accessReturnPage by rememberSaveable { mutableStateOf("settings") }
    var privacyReturnPage by rememberSaveable { mutableStateOf("settings") }
    var lock by remember { mutableStateOf(false) }
    var backup by rememberSaveable { mutableStateOf(false) }
    val back = { page = "settings" }
    BackHandler(enabled = page != "settings", onBack = {
        page = when (page) {
            "archive_files" -> "privacy"
            "privacy" -> privacyReturnPage
            "access" -> accessReturnPage
            else -> "settings"
        }
    })
    if (lock) AppLockSettingsSheet(onDismiss = { lock = false }, onChanged = {})
    if (backup) BackupRestoreSheet(onDismiss = { backup = false })
    when (page) {
        "appearance" -> AppearancePage(scaffoldPadding, themeMode, onThemeModeChange, back)
        "privacy" -> PrivacyPage(scaffoldPadding, { page = privacyReturnPage },
            { accessReturnPage = "privacy"; page = "access" }, { page = "archive_files" })
        "archive_files" -> StatementArchivePrivacyPage(scaffoldPadding, { page = "privacy" })
        "terms" -> LegalInformationPage(scaffoldPadding, back, LegalNotice.TERMS)
        "legal_privacy" -> LegalInformationPage(scaffoldPadding, back, LegalNotice.PRIVACY)
        "financial_notice" -> LegalInformationPage(scaffoldPadding, back, LegalNotice.FINANCIAL)
        "access" -> MoneyAccessPage(scaffoldPadding, { page = accessReturnPage }, onHistoricalImportClick)
        "reminders" -> RemindersPage(scaffoldPadding, back)
        "help" -> HelpPage(scaffoldPadding, back, onAdvancedClick,
            onMoneyAccess = { accessReturnPage = "help"; page = "access" },
            onPrivacy = { privacyReturnPage = "help"; page = "privacy" })
        "about" -> EverydayPage("About PesaTrack", scaffoldPadding, back) {
            item { Text("PesaTrack helps you understand your spending, savings and bills from money messages on this phone.") }
            item { Text("Version " + com.kedevjimmy.pesatrack.BuildConfig.VERSION_NAME) }
            item { Text("Balances reflect the messages and entries available to PesaTrack. Check your bank or M-Pesa app for the latest balance.") }
        }
        else -> EverydayPage("Settings", scaffoldPadding, onBack) {
            item {
                PesaCard(Modifier.fillMaxWidth().clickable(onClick = onProfileClick)) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(context.getSharedPreferences("pesatrack_preferences", 0).getString("profile_name", "").orEmpty().ifBlank { "Your profile" },
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Personal details and profile security", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Text("Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            item { SettingsGroup {
                SettingsCompactRow(Icons.Rounded.Notifications, "Reminders", "Bills, budgets and savings") { page = "reminders" }
            } }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Privacy & data", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Stored on this phone", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { SettingsGroup {
                SettingsCompactRow(Icons.Rounded.Security, "Privacy", "Your data and your choices") { privacyReturnPage = "settings"; page = "privacy" }
                SettingsCompactRow(Icons.Rounded.Info, "Privacy Policy", "Pre-release policy and personal-data rights") { page = "legal_privacy" }
                SettingsCompactRow(Icons.Rounded.Message, "Money message access", "SMS history and bank-app updates") { accessReturnPage = "settings"; page = "access" }
                SettingsCompactRow(Icons.Rounded.Lock, "App lock", "Protect PesaTrack with a PIN") { lock = true }
                SettingsCompactRow(Icons.Rounded.Backup, "Backup & restore", "Encrypted backup stored where you choose") { backup = true }
            } }
            item {
                Text("Help & more", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            item { SettingsGroup {
                SettingsCompactRow(Icons.Rounded.Tune, "Automatic sorting", "Salary, regular payments and transfers", onSmartRulesClick)
                SettingsCompactRow(Icons.Rounded.HelpOutline, "Help", "Answers and troubleshooting") { page = "help" }
                SettingsCompactRow(Icons.Rounded.Info, "Terms of Service", "Pre-release draft · Your responsibilities and rights") { page = "terms" }
                SettingsCompactRow(Icons.Rounded.Info, "Financial information notice", "Accuracy, debt evidence and financial decisions") { page = "financial_notice" }
                SettingsCompactRow(Icons.Rounded.Info, "About PesaTrack", "App information") { page = "about" }
            } }
            item { Text("Each setting opens a focused page. Sensitive actions explain what changes before you confirm.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) }
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
