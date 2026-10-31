package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
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
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
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
internal fun ChoiceSwitch(title: String, description: String, checked: Boolean, enabled: Boolean = true, change: (Boolean) -> Unit) {
    PesaCard { Row(
        Modifier.fillMaxWidth().toggleable(value = checked, enabled = enabled, role = Role.Switch,
            onValueChange = change).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF7027E8),
                checkedBorderColor = Color(0xFF7027E8),
                uncheckedThumbColor = Color(0xFF604C8B),
                uncheckedTrackColor = Color(0xFFE5DDF5),
                uncheckedBorderColor = Color(0xFF8774A7),
                disabledUncheckedTrackColor = Color(0xFFE5E1EB),
                disabledUncheckedThumbColor = Color(0xFF8D849B)
            )
        )
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
internal fun PrivacyPage(padding: PaddingValues, back: () -> Unit, access: () -> Unit,
    manageStatements: () -> Unit, privacyPolicy: (() -> Unit)? = null) {
    val context = LocalContext.current
    val prefs = remember { EntityEnrichmentPreferences.get(context) }
    var lookup by remember { mutableStateOf(prefs.isOnlineLookupEnabled()) }
    var details by rememberSaveable { mutableStateOf(false) }
    var backup by rememberSaveable { mutableStateOf(false) }
    if (backup) BackupRestoreSheet(onDismiss = { backup = false })
    EverydayPage("Privacy & data", padding, back) {
        item {
            PesaCard {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Shield, size = 58.dp,
                        background = pesaToneContainer("violet"),
                        tint = pesaToneContent("violet"))
                    Column(Modifier.padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Your data belongs to you", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text("Your money records are kept on this phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { SectionHeader("What the app can read") }
        item { EverydayRow(Icons.Rounded.Message, "Money messages",
            "Only the sources you allow", access) }
        item { EverydayRow(Icons.Rounded.Inventory2, "Bank statements",
            "Only the files you choose to import", manageStatements) }
        item { SectionHeader("Keep your records safe") }
        item { EverydayRow(Icons.Rounded.Backup, "Encrypted backup & restore",
            "Save or restore a private copy") { backup = true } }
        if (privacyPolicy != null) item {
            EverydayRow(Icons.Rounded.Policy, "Privacy policy",
                "How PesaTrack handles your data", privacyPolicy)
        }
        item { SectionHeader("Your choices") }
        item { ChoiceSwitch("Help identify businesses online",
            "Optional. Sends only a cleaned business name, never your messages or amounts.",
            lookup) { lookup = it; prefs.setOnlineLookupEnabled(it) } }
        item { TextButton(onClick = { details = !details }) {
            Text(if (details) "Show less" else "How my data is handled")
        } }
        if (details) {
            item { PesaCard { Column(Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("You choose what we read", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Text("Turn off money message access any time. Your saved records stay here.")
                Text("Voice entry uses your phone's speech service, which may work online. You can type instead.")
                Text("Keep your backup passphrase somewhere safe. App lock is separate from backup protection.")
            } } }
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
    var moreOptions by rememberSaveable { mutableStateOf(false) }
    var allowed by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { allowed = NotificationManagerCompat.from(context).areNotificationsEnabled() }
    fun ask() { if (Build.VERSION.SDK_INT >= 33 && !allowed) permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
    EverydayPage(title, padding, back) {
        item {
            PesaCard {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.NotificationsActive, size = 58.dp,
                        background = pesaToneContainer("violet"),
                        tint = pesaToneContent("violet"))
                    Column(Modifier.padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Stay one step ahead", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text("A little reminder when it matters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (!allowed) item {
            PesaCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Notifications are off on your phone", fontWeight = FontWeight.Bold)
                    Text("Your choices below stay saved. Allow notifications to receive alerts.")
                    Button(onClick = {
                        settings.launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    }, modifier = Modifier.fillMaxWidth()) { Text("Open notification settings") }
                }
            }
        }
        item { SectionHeader("Plan reminders") }
        item { ChoiceSwitch("Upcoming bills", "Remind me before a scheduled bill is due", billOn) { billOn = it; bills.setGlobalReminderLeadDays(lead, it); if(it) ask() } }
        item { Text("Remind me before a bill", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { listOf(1,3,7).forEach { days -> FilterChip(selected = lead == days, enabled = billOn, onClick = { lead = days; bills.setGlobalReminderLeadDays(days, billOn) }, label = { Text(pluralStringResource(R.plurals.bill_reminder_days, days, days)) }) } } }
        item { ChoiceSwitch("Budget warnings", "When spending reaches or passes your budget", budgetOn) { budgetOn = it; PlanningReminders.setEnabled(context, "budgets", it); if(it) ask() } }
        item { ChoiceSwitch("Savings reminders", "When a monthly goal still needs a contribution", goalOn) { goalOn = it; PlanningReminders.setEnabled(context, "goals", it); if(it) ask() } }
        item { SectionHeader("Things to review") }
        item { ChoiceSwitch("Transactions to review", "A gentle reminder about items needing your attention", activityReviewOn) { activityReviewOn = it; ReviewAttentionReminders.setEnabled(context, it); if(it) ask() } }
        item { ChoiceSwitch("Account review alerts", "When an account needs your confirmation",accountReviewOn) {
            accountReviewOn = it; com.kedevjimmy.pesatrack.planning.SmartNoticeCoordinator.setEnabled(context,com.kedevjimmy.pesatrack.domain.rules.SmartNoticeKind.ACCOUNT,it); if (it) ask()
        } }
        item { ChoiceSwitch("Money owed to me", "When a possible repayment needs checking",paymentOn) {
            paymentOn = it; com.kedevjimmy.pesatrack.planning.SmartNoticeCoordinator.setEnabled(context,com.kedevjimmy.pesatrack.domain.rules.SmartNoticeKind.PAYMENT,it); if (it) ask()
        } }
        item { SectionHeader("Other updates") }
        item { ChoiceSwitch("Helpful tips", "Occasional tips about PesaTrack features",discoveryOn) {
            discoveryOn = it; com.kedevjimmy.pesatrack.planning.SmartNoticeCoordinator.setEnabled(context,com.kedevjimmy.pesatrack.domain.rules.SmartNoticeKind.DISCOVERY,it)
        } }
        item {
            PesaCard {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Inventory2, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Statement imports", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text("You'll see the result when each import finishes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { OutlinedButton(onClick = { moreOptions = !moreOptions }, modifier = Modifier.fillMaxWidth()) {
            Text(if (moreOptions) "Hide reminder options" else "More reminder options")
        } }
        if (moreOptions) item {
            Text("How reminders appear",style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("banner" to "Banner if allowed","quiet" to "Quiet summary","off" to "Off").forEach { (value,label) ->
                    FilterChip(selected = smartMode == value,onClick = {
                        smartMode = value; smart.setMode(value)
                        com.kedevjimmy.pesatrack.planning.SmartNoticeCoordinator.cancel(context)
                        ReviewAttentionReminders.schedule(context)
                    },label = { Text(label) })
                }
            }
            Text("These alerts have a daily limit. Quiet hours are 8 pm to 9 am. Your phone decides how banners look.")
            TextButton(onClick = {
                snoozed = !snoozed
                smart.snooze(if (snoozed) System.currentTimeMillis() + com.kedevjimmy.pesatrack.domain.rules.ReviewReminderPolicy.DAY_MS else 0)
                com.kedevjimmy.pesatrack.planning.SmartNoticeCoordinator.cancel(context)
            }) { Text(if (snoozed) "Resume smart alerts" else "Snooze smart alerts for a day") }
            TextButton(onClick = { smart.resetDiscovery() }) { Text("Show the They Owe Me tip again") }
        }
        item { Text("Money amounts are hidden on the lock screen. Your phone may delay reminders to save battery.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
