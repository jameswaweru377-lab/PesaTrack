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
import com.kedevjimmy.pesatrack.security.AppLockManager

@Composable
fun SettingsScreen(scaffoldPadding: PaddingValues, themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit, onProfileClick: () -> Unit, onMpesaConnectionClick: () -> Unit,
    onSmartRulesClick: () -> Unit, onHistoricalImportClick: () -> Unit, onAdvancedClick: () -> Unit = {}) {
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf("settings") }
    var lock by remember { mutableStateOf(false) }
    var backup by remember { mutableStateOf(false) }
    val back = { page = "settings" }
    BackHandler(enabled = page != "settings", onBack = back)
    if (lock) AppLockSettingsSheet(onDismiss = { lock = false }, onChanged = {})
    if (backup) BackupRestoreSheet(onDismiss = { backup = false })
    when (page) {
        "appearance" -> AppearancePage(scaffoldPadding, themeMode, onThemeModeChange, back)
        "privacy" -> PrivacyPage(scaffoldPadding, back, { page = "access" })
        "access" -> MoneyAccessPage(scaffoldPadding, back, onHistoricalImportClick)
        "reminders" -> RemindersPage(scaffoldPadding, back)
        "help" -> HelpPage(scaffoldPadding, back, onAdvancedClick)
        "about" -> EverydayPage("About PesaTrack", scaffoldPadding, back) {
            item { Text("PesaTrack helps you understand your spending, savings and bills from money messages on this phone.") }
            item { Text("Version " + com.kedevjimmy.pesatrack.BuildConfig.VERSION_NAME) }
            item { Text("Balances reflect the messages and entries available to PesaTrack. Check your bank or M-Pesa app for the latest balance.") }
        }
        else -> EverydayPage("Settings", scaffoldPadding, onBack) {
            item { EverydayRow(Icons.Rounded.Person, context.getSharedPreferences("pesatrack_preferences", 0).getString("profile_name", "").orEmpty().ifBlank { "Your profile" }, "Personal details and money progress", onProfileClick) }
            item { Text("Make it yours", style = MaterialTheme.typography.titleMedium) }
            item { EverydayRow(Icons.Rounded.Palette, "Appearance", "Light, dark or your phone's setting") { page = "appearance" } }
            item { EverydayRow(Icons.Rounded.Notifications, "Reminders", "Bills, budgets and savings") { page = "reminders" } }
            item { Text("Privacy and data", style = MaterialTheme.typography.titleMedium) }
            item { EverydayRow(Icons.Rounded.Security, "Privacy", "Your data and your choices") { page = "privacy" } }
            item { EverydayRow(Icons.Rounded.Message, "Money message access", "SMS history and bank-app updates") { page = "access" } }
            item { EverydayRow(Icons.Rounded.Lock, "App lock", "Protect PesaTrack with a PIN") { lock = true } }
            item { EverydayRow(Icons.Rounded.Backup, "Backup and restore", "Save a protected copy of your data") { backup = true } }
            item { Text("Help and more", style = MaterialTheme.typography.titleMedium) }
            item { EverydayRow(Icons.Rounded.Tune, "Automatic sorting", "Salary, regular payments and transfers", onSmartRulesClick) }
            item { EverydayRow(Icons.Rounded.HelpOutline, "Help", "Answers and troubleshooting") { page = "help" } }
            item { EverydayRow(Icons.Rounded.Info, "About PesaTrack", "App information") { page = "about" } }
        }
    }
}
