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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.PesaCard

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyListScope
import androidx.core.app.NotificationManagerCompat
import com.kedevjimmy.pesatrack.data.local.SmsAccessConsentStore
import com.kedevjimmy.pesatrack.data.local.EntityEnrichmentPreferences
import com.kedevjimmy.pesatrack.ingestion.NotificationSourceTrustStore
import com.kedevjimmy.pesatrack.planning.BillSchedules
import com.kedevjimmy.pesatrack.planning.PlanningReminders
import com.kedevjimmy.pesatrack.planning.ReviewAttentionReminders
import com.kedevjimmy.pesatrack.ui.theme.ThemeMode
import com.kedevjimmy.pesatrack.R

@Composable
internal fun EverydayPage(title: String, padding: PaddingValues, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens.screenHorizontalPadding, vertical = com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens.screenVerticalPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        } }
        content()
    }
}
@Composable
internal fun EverydayRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, onClick: () -> Unit) {
    PesaCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null)
        }
    }
}
@Composable
internal fun ChoiceSwitch(title: String, description: String, checked: Boolean, change: (Boolean) -> Unit) {
    PesaCard { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked = checked, onCheckedChange = change)
    } }
}
@Composable
internal fun AppearancePage(padding: PaddingValues, mode: ThemeMode, change: (ThemeMode) -> Unit, back: () -> Unit) {
    EverydayPage("Appearance", padding, back) {
        item { Text("Choose a look that is comfortable for you.") }
        items(ThemeMode.entries.size) { index -> val option = ThemeMode.entries[index]
            PesaCard(Modifier.fillMaxWidth().clickable { change(option) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = option == mode, onClick = { change(option) })
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(when(option) { ThemeMode.SYSTEM -> "Use phone setting"; ThemeMode.LIGHT -> "Light"; ThemeMode.DARK -> "Dark" }, style = MaterialTheme.typography.titleMedium)
                        Text(when(option) { ThemeMode.SYSTEM -> "Changes with your phone's appearance"; ThemeMode.LIGHT -> "A bright background"; ThemeMode.DARK -> "A darker background" }, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
@Composable
internal fun PrivacyPage(padding: PaddingValues, back: () -> Unit, access: () -> Unit, manageStatements: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { EntityEnrichmentPreferences.get(context) }
    var lookup by remember { mutableStateOf(prefs.isOnlineLookupEnabled()) }
    var details by rememberSaveable { mutableStateOf(false) }
    EverydayPage("Privacy", padding, back) {
        item { Text("Saved on this phone", style = MaterialTheme.typography.titleMedium); Text("Your transactions stay on this phone. You choose which money messages PesaTrack can read.") }
        item { EverydayRow(Icons.Rounded.Message, "Manage message access", "See permissions and stop access", access) }
        item { EverydayRow(Icons.Rounded.Inventory2, "Manage saved statements", "Review and delete individual saved PDF statement archives", manageStatements) }
        item { ChoiceSwitch("Help identify businesses online", "Optional. A cleaned business name may be sent to look it up. Full messages, amounts, balances, transaction codes and phone or account numbers are not sent.", lookup) { lookup = it; prefs.setOnlineLookupEnabled(it) } }
        item { TextButton(onClick = { details = !details }) { Text(if (details) "Hide privacy details" else "More privacy details") } }
        if (details) {
            item { Text("You choose what we read", style = MaterialTheme.typography.titleMedium); Text("SMS needs your agreement and Android permission. You also choose which apps we can read money updates from. Turning access off stops new reading; your saved transactions remain. Full SMS and bank-app notification text is not uploaded.") }
            item { Text("Voice entry", style = MaterialTheme.typography.titleMedium); Text("Voice entry uses your phone's speech service, which may process audio online. You can type instead, and check the result before saving.") }
            item { Text("Backups and app lock", style = MaterialTheme.typography.titleMedium); Text("Backups are protected with the passphrase you choose. Keep it safe. A backup does not grant message access. App lock helps keep your records private on a shared phone.") }
        }
    }
}
@Composable
internal fun MoneyAccessPage(padding: PaddingValues, back: () -> Unit, history: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var sources by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var details by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh++ }
    val sms = remember(refresh) { SmsAccessConsentStore.canRead(context) }
    val notifications = remember(refresh) { NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName) }
    val approvedApps = remember(refresh) {
        val trust = NotificationSourceTrustStore(context)
        trust.observedSources().count { trust.isApproved(it.packageName) }
    }
    if (sources) NotificationSourceApprovalSheet { sources = false; refresh++ }
    EverydayPage("Money message access", padding, back) {
        item { Text("Read money messages to keep your transactions up to date. This is separate from reminders sent by PesaTrack.") }
        item { EverydayRow(Icons.Rounded.Message, "Add past transactions", if (sms) "SMS access is on" else "Choose whether to read older SMS", history) }
        item { EverydayRow(Icons.Rounded.Notifications, "Bank-app updates", if (notifications) "Android notification access is on" else "Allow access in Android settings") {
            launcher.launch(smartNotificationAccessIntent(context))
        } }
        item { EverydayRow(Icons.Rounded.Apps, "Apps to read money updates from", if (approvedApps == 0) "No apps approved · Choose your bank, mobile-money or SMS app" else pluralStringResource(R.plurals.approved_money_apps, approvedApps, approvedApps)) { sources = true } }
        item { OutlinedButton(onClick = {
            val smsSaved = SmsAccessConsentStore.setAccepted(context, false)
            val appsSaved = NotificationSourceTrustStore(context).revokeAll()
            refresh++
            error = if (smsSaved && appsSaved) "Message reading is off. Your saved transactions remain." else "Couldn't save every choice. Please try again."
        }, modifier = Modifier.fillMaxWidth()) { Text("Stop reading money messages") } }
        item { TextButton(onClick = { launcher.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))) }) { Text("Open Android app settings") } }
        item { error?.let { Text(it) }; TextButton(onClick = { details = !details }) { Text(if (details) "Hide access details" else "More about message access") } }
        if (details) item { Text("Android access alone does not approve an app: choose each source above. Stopping access keeps your saved transactions. If Android blocks access on a downloaded APK, open App info, then the menu, and choose Allow restricted settings.", style = MaterialTheme.typography.bodyMedium) }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RemindersPage(padding: PaddingValues, back: () -> Unit, title: String = "Reminders") {
    val context = LocalContext.current
    val bills = remember { BillSchedules(context) }
    var lead by remember { mutableIntStateOf(bills.globalReminderLeadDays()) }
    var billOn by remember { mutableStateOf(bills.remindersEnabled()) }
    var budgetOn by remember { mutableStateOf(PlanningReminders.enabled(context, "budgets")) }
    var goalOn by remember { mutableStateOf(PlanningReminders.enabled(context, "goals")) }
    var activityReviewOn by remember { mutableStateOf(ReviewAttentionReminders.enabled(context)) }
    val smart = remember { com.kedevjimmy.pesatrack.planning.SmartNoticePreferences(context) }
    var accountReviewOn by remember { mutableStateOf(smart.enabled(com.kedevjimmy.pesatrack.domain.rules.SmartNoticeKind.ACCOUNT)) }
    var paymentOn by remember { mutableStateOf(smart.enabled(com.kedevjimmy.pesatrack.domain.rules.SmartNoticeKind.PAYMENT)) }
    var discoveryOn by remember { mutableStateOf(smart.enabled(com.kedevjimmy.pesatrack.domain.rules.SmartNoticeKind.DISCOVERY)) }
    var smartMode by remember { mutableStateOf(smart.mode()) }
    var snoozed by remember { mutableStateOf(smart.snoozeUntil() > System.currentTimeMillis()) }
    var allowed by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { allowed = NotificationManagerCompat.from(context).areNotificationsEnabled() }
    fun ask() { if (Build.VERSION.SDK_INT >= 33 && !allowed) permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
    EverydayPage(title, padding, back) {
        item { Text("A little help staying on track. Reminders are checked during the day and may be delayed by your phone's battery settings.") }
        if (!allowed) item { Button(onClick = { settings.launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }, modifier = Modifier.fillMaxWidth()) { Text("Allow PesaTrack notifications") } }
        item { ChoiceSwitch("Upcoming bills", "Remind me before a scheduled bill is due", billOn) { billOn = it; bills.setGlobalReminderLeadDays(lead, it); if(it) ask() } }
        item { Text("Before a bill is due", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { listOf(1,3,7).forEach { days -> FilterChip(selected = lead == days, enabled = billOn, onClick = { lead = days; bills.setGlobalReminderLeadDays(days, billOn) }, label = { Text(pluralStringResource(R.plurals.bill_reminder_days, days, days)) }) } } }
        item { ChoiceSwitch("Budget warnings", "Tell me when a monthly category budget reaches 95% or goes over", budgetOn) { budgetOn = it; PlanningReminders.setEnabled(context, "budgets", it); if(it) ask() } }
        item { ChoiceSwitch("Savings reminders", "Remind me when a monthly goal has no contribution for a month", goalOn) { goalOn = it; PlanningReminders.setEnabled(context, "goals", it); if(it) ask() } }
        item { ChoiceSwitch("Activity review tips", "A quiet phone notification when several uncategorized transactions still need review. At most once a day, with longer pauses for the same items.", activityReviewOn) { activityReviewOn = it; ReviewAttentionReminders.setEnabled(context, it); if(it) ask() } }
        item { ChoiceSwitch("Account review alerts", "Review unresolved ownership or account type when trusted evidence could affect attribution.",accountReviewOn) {
            accountReviewOn = it; com.kedevjimmy.pesatrack.planning.SmartNoticeCoordinator.setEnabled(context,com.kedevjimmy.pesatrack.domain.rules.SmartNoticeKind.ACCOUNT,it); if (it) ask()
        } }
        item { ChoiceSwitch("They Owe Me payment alerts", "Optional review of new payments with a unique matching phone and recorded lending history. No automatic loans or repayments.",paymentOn) {
            paymentOn = it; com.kedevjimmy.pesatrack.planning.SmartNoticeCoordinator.setEnabled(context,com.kedevjimmy.pesatrack.domain.rules.SmartNoticeKind.PAYMENT,it); if (it) ask()
        } }
        item { ChoiceSwitch("Feature tips on your phone", "One occasional discovery tip. A discreet in-app tip is available without notification permission.",discoveryOn) {
            discoveryOn = it; com.kedevjimmy.pesatrack.planning.SmartNoticeCoordinator.setEnabled(context,com.kedevjimmy.pesatrack.domain.rules.SmartNoticeKind.DISCOVERY,it)
        } }
        item {
            Text("Smart notification delivery",style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("banner" to "Banner if allowed","quiet" to "Quiet summary","off" to "Off").forEach { (value,label) ->
                    FilterChip(selected = smartMode == value,onClick = {
                        smartMode = value; smart.setMode(value)
                        com.kedevjimmy.pesatrack.planning.SmartNoticeCoordinator.cancel(context)
                        ReviewAttentionReminders.schedule(context)
                    },label = { Text(label) })
                }
            }
            Text("Account, Activity, payment and discovery alerts share one daily limit, with quiet hours from 8 pm to 9 am. Your phone controls banner display.")
            TextButton(onClick = {
                snoozed = !snoozed
                smart.snooze(if (snoozed) System.currentTimeMillis() + com.kedevjimmy.pesatrack.domain.rules.ReviewReminderPolicy.DAY_MS else 0)
                com.kedevjimmy.pesatrack.planning.SmartNoticeCoordinator.cancel(context)
            }) { Text(if (snoozed) "Resume smart alerts" else "Snooze smart alerts for a day") }
            TextButton(onClick = { smart.resetDiscovery() }) { Text("Show the They Owe Me tip again") }
        }
        item { Text("Amounts are hidden on the lock screen. You can turn each reminder off here or in Android settings.", style = MaterialTheme.typography.bodyMedium) }
    }
}
@Composable
internal fun HelpPage(padding: PaddingValues, back: () -> Unit, advanced: () -> Unit, title: String = "Help") {
    EverydayPage(title, padding, back) {
        item { Text("Missing transactions?", style = MaterialTheme.typography.titleMedium); Text("Check Money message access, then add past transactions. PesaTrack ignores duplicates. Bank-app updates begin after you approve the app.") }
        item { Text("A balance looks wrong?", style = MaterialTheme.typography.titleMedium); Text("Open the account to check its latest balance and date. Older messages may be missing. An estimate is not a live bank balance.") }
        item { Text("Sorting needs a correction?", style = MaterialTheme.typography.titleMedium); Text("Open a transaction in Activity to change its category or confirm which account it belongs to.") }
        item { EverydayRow(Icons.Rounded.Build, "Advanced troubleshooting", "Message tests and repair tools", advanced) }
    }
}
