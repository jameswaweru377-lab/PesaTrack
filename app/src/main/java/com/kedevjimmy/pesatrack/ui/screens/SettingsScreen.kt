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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.theme.ProfileVisualColors as C
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens

import com.kedevjimmy.pesatrack.ui.theme.ThemeMode

@Composable
fun SettingsScreen(scaffoldPadding: PaddingValues, themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit, onProfileClick: () -> Unit, onMpesaConnectionClick: () -> Unit,
    onSmartRulesClick: () -> Unit, onHistoricalImportClick: () -> Unit, onAdvancedClick: () -> Unit = {}) {
    var page by rememberSaveable { mutableStateOf("settings") }
    var accessReturnPage by rememberSaveable { mutableStateOf("settings") }
    var privacyReturnPage by rememberSaveable { mutableStateOf("settings") }
    var legalReturnPage by rememberSaveable { mutableStateOf("settings") }
    var lock by remember { mutableStateOf(false) }
    var backup by rememberSaveable { mutableStateOf(false) }
    val back = { page = "settings" }
    BackHandler(enabled = page != "settings", onBack = {
        page = when (page) {
            "archive_files" -> "privacy"
            "privacy" -> privacyReturnPage
            "access" -> accessReturnPage
            "terms", "legal_privacy", "financial_notice" -> legalReturnPage
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
        "terms" -> LegalInformationPage(scaffoldPadding, { page = legalReturnPage }, LegalNotice.TERMS)
        "legal_privacy" -> LegalInformationPage(scaffoldPadding, { page = legalReturnPage }, LegalNotice.PRIVACY)
        "financial_notice" -> LegalInformationPage(scaffoldPadding, { page = legalReturnPage }, LegalNotice.FINANCIAL)
        "access" -> MoneyAccessPage(scaffoldPadding, { page = accessReturnPage }, onHistoricalImportClick)
        "reminders" -> RemindersPage(scaffoldPadding, back)
        "help" -> HelpPage(scaffoldPadding, back, onAdvancedClick,
            onMoneyAccess = { accessReturnPage = "help"; page = "access" },
            onPrivacy = { privacyReturnPage = "help"; page = "privacy" })
        "about" -> ProfileAboutPage(scaffoldPadding, back,
            privacyPolicy = { legalReturnPage = "about"; page = "legal_privacy" },
            terms = { legalReturnPage = "about"; page = "terms" },
            financialNotice = { legalReturnPage = "about"; page = "financial_notice" })
        else -> EverydayPage("Settings", scaffoldPadding, onBack) {
            item {
                Text("Automation & message access",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
            }
            item {
                SettingsGroup {
                    SettingsCompactRow(Icons.Rounded.Tune, "Automatic sorting",
                        "Salary, regular payments and transfers", tone = "violet", onClick = onSmartRulesClick)
                    SettingsCompactRow(Icons.Rounded.Message, "Money message access",
                        "Review SMS permissions and allowed sources", tone = "blue") {
                        accessReturnPage = "settings"
                        page = "access"
                    }
                }
            }
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
    tone: String = "violet",
    onClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick)
        .heightIn(min = PesaDesignTokens.minimumTouchTarget)
        .padding(horizontal = PesaDesignTokens.cardContentPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconBubble(icon, tint = C.ink, background = C.lilac)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
