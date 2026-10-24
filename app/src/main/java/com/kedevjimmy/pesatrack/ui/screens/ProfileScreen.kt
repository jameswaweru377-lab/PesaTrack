package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.BuildConfig
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProfileScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onAccountsClick: (() -> Unit)? = null,
    onDataHealthClick: (() -> Unit)? = null,
    onGoalsClick: (() -> Unit)? = null,
    onHistoricalImportClick: (() -> Unit)? = null,
    onPageChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("pesatrack_preferences", 0) }
    var page by rememberSaveable { mutableStateOf("home") }
    var progress by remember { mutableStateOf<FinancialProgress?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val ledger by LedgerUpdateBus.state.collectAsState()

    LaunchedEffect(page) {
        onPageChanged(page)
    }

    LaunchedEffect(ledger.revision, retry, page == "financial") {
        try {
            progress = withContext(Dispatchers.IO) { FinancialProgressRepository(context).read() }
            error = null
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = "Couldn't load your progress. Please try again."
        }
    }

    BackHandler(enabled = page == "home") {
        onBack()
    }
    BackHandler(enabled = page != "home" && page != "personal") {
        page = "home"
    }

    if (page == "personal") {
        PersonalDetailsScreen(scaffoldPadding, onBack = { page = "home" }, title = "Personal information")
        return
    }

    when (page) {
        "security" -> ProfileSecurityPage(
            padding = scaffoldPadding,
            back = { page = "home" },
            privacy = { page = "privacy" },
            moneyAccess = { page = "access" },
            settings = onSettingsClick
        )
        "appearance" -> ProfileAppearancePage(scaffoldPadding) { page = "home" }
        "notifications" -> RemindersPage(scaffoldPadding, { page = "home" }, title = "Notifications")
        "financial" -> ProfileFinancialPage(
            padding = scaffoldPadding,
            progress = progress,
            error = error,
            retry = { retry++ },
            back = { page = "home" },
            accounts = onAccountsClick ?: onSettingsClick,
            goals = onGoalsClick ?: onSettingsClick
        )
        "support" -> HelpPage(
            scaffoldPadding,
            { page = "home" },
            onDataHealthClick ?: onSettingsClick,
            title = "Help & support"
        )
        "about" -> ProfileAboutPage(
            padding = scaffoldPadding,
            back = { page = "home" },
            privacyPolicy = { page = "policy" },
            terms = { page = "terms" },
            financialNotice = { page = "financial_notice" }
        )
        "privacy" -> PrivacyPage(
            scaffoldPadding,
            { page = "security" },
            { page = "access" },
            { page = "archive" }
        )
        "archive" -> StatementArchivePrivacyPage(scaffoldPadding) { page = "privacy" }
        "access" -> MoneyAccessPage(
            scaffoldPadding,
            { page = "security" },
            onHistoricalImportClick ?: onSettingsClick
        )
        "policy" -> LegalInformationPage(scaffoldPadding, { page = "about" }, LegalNotice.PRIVACY)
        "terms" -> LegalInformationPage(scaffoldPadding, { page = "about" }, LegalNotice.TERMS)
        "financial_notice" -> LegalInformationPage(scaffoldPadding, { page = "about" }, LegalNotice.FINANCIAL)
        else -> ProfileHomeScreen(
            padding = scaffoldPadding,
            name = prefs.getString("profile_name", "").orEmpty().ifBlank { "Your profile" },
            email = prefs.getString("profile_email", "").orEmpty(),
            avatar = prefs.getString("profile_avatar", "🌿") ?: "🌿",
            progress = progress,
            error = error,
            retry = { retry++ },
            personal = { page = "personal" },
            security = { page = "security" },
            notifications = { page = "notifications" },
            appearance = { page = "appearance" },
            financial = { page = "financial" },
            support = { page = "support" },
            about = { page = "about" },
            settings = onSettingsClick
        )
    }
}

@Composable
private fun ProfileHomeScreen(
    padding: PaddingValues,
    name: String,
    email: String,
    avatar: String,
    progress: FinancialProgress?,
    error: String?,
    retry: () -> Unit,
    personal: () -> Unit,
    security: () -> Unit,
    notifications: () -> Unit,
    appearance: () -> Unit,
    financial: () -> Unit,
    support: () -> Unit,
    about: () -> Unit,
    settings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            ProfileGradientBanner(name = name, email = email, avatar = avatar, onEdit = personal)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileMiniStat("Accounts", progress?.accounts?.toString() ?: "—",
                    Icons.Rounded.AccountBalanceWallet, Modifier.weight(1f))
                ProfileMiniStat("Budgets", progress?.budgets?.size?.toString() ?: "—",
                    Icons.Rounded.PieChart, Modifier.weight(1f))
                ProfileMiniStat("Active goals", progress?.goals?.count { it.saved < it.target }?.toString() ?: "—",
                    Icons.Rounded.Savings, Modifier.weight(1f))
            }
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp))
                TextButton(onClick = retry) { Text("Try again") }
            }
        }
        item {
            PesaCard {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBubble(Icons.Rounded.AutoAwesome, background = pesaToneContainer("violet"),
                        tint = pesaToneContent("violet"), size = 42.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                        Text("Better money habits.", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Text("Brighter future.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.TrendingUp, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
                }
            }
        }
        item { SectionHeader("Your account") }
        item {
            ProfileMenuGroup(
                listOf(
                    ProfileMenuItem(Icons.Rounded.Person, "Personal information",
                        "Name, avatar and optional email", "violet", personal),
                    ProfileMenuItem(Icons.Rounded.Security, "Account security",
                        "App lock, backups and privacy", "red", security),
                    ProfileMenuItem(Icons.Rounded.NotificationsActive, "Notifications",
                        "Bills, budgets and savings reminders", "amber", notifications),
                    ProfileMenuItem(Icons.Rounded.Palette, "Appearance",
                        "Light theme and visual preferences", "blue", appearance),
                    ProfileMenuItem(Icons.Rounded.AccountBalanceWallet, "Financial overview",
                        "Accounts, budgets and savings goals", "green", financial),
                    ProfileMenuItem(Icons.Rounded.SupportAgent, "Help & support",
                        "Guidance and troubleshooting", "violet", support),
                    ProfileMenuItem(Icons.Rounded.Info, "About PesaTrack",
                        "App details and important notices", "blue", about)
                )
            )
        }
        item {
            PesaCard(Modifier.fillMaxWidth().clickable(onClick = settings)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Settings, background = MaterialTheme.colorScheme.surfaceVariant,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 38.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                        Text("More settings", style = MaterialTheme.typography.titleSmall)
                        Text("Manage app controls and data preferences", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.PhoneAndroid, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text("  Your financial records stay on this phone",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProfileGradientBanner(name: String, email: String, avatar: String, onEdit: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        contentColor = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(
            Modifier.background(
                Brush.linearGradient(
                    listOf(Color(0xFF8B43FF), Color(0xFF6825E8), Color(0xFF4C3BCB))
                )
            ).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = .18f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = .82f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(avatar, style = MaterialTheme.typography.headlineMedium)
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(email.ifBlank { "Your details are saved on this phone" },
                        style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .84f))
                    Surface(shape = RoundedCornerShape(50.dp), color = Color.White.copy(alpha = .17f)) {
                        Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Shield, contentDescription = null,
                                tint = Color.White, modifier = Modifier.size(13.dp))
                            Text("Stored on this phone", style = MaterialTheme.typography.labelSmall,
                                color = Color.White)
                        }
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit profile", tint = Color.White)
                }
            }
            Text("Your money, your plans.", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Text("Keep your details, privacy and financial picture in one place.",
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .86f))
        }
    }
}

@Composable
private fun ProfileMiniStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    PesaCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            IconBubble(icon, size = 32.dp, background = pesaToneContainer("violet"),
                tint = pesaToneContent("violet"))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

private data class ProfileMenuItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String,
    val tone: String,
    val onClick: () -> Unit
)

@Composable
private fun ProfileMenuGroup(items: List<ProfileMenuItem>) {
    PesaCard(Modifier.fillMaxWidth()) {
        Column {
            items.forEachIndexed { index, item ->
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = item.onClick)
                        .heightIn(min = 70.dp).padding(horizontal = 13.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBubble(item.icon, size = 40.dp, background = pesaToneContainer(item.tone),
                        tint = pesaToneContent(item.tone))
                    Column(Modifier.weight(1f).padding(horizontal = 11.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text(item.subtitle, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (index != items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 64.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f),
                        thickness = .7.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSecurityPage(
    padding: PaddingValues,
    back: () -> Unit,
    privacy: () -> Unit,
    moneyAccess: () -> Unit,
    settings: () -> Unit
) {
    var lock by remember { mutableStateOf(false) }
    var backup by remember { mutableStateOf(false) }
    if (lock) AppLockSettingsSheet(onDismiss = { lock = false }, onChanged = { lock = false })
    if (backup) BackupRestoreSheet(onDismiss = { backup = false })

    EverydayPage("Account security", padding, back) {
        item {
            PesaCard {
                Column(Modifier.fillMaxWidth().padding(17.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    IconBubble(Icons.Rounded.Shield, size = 58.dp,
                        background = pesaToneContainer("violet"), tint = pesaToneContent("violet"))
                    Text("Your records stay private", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text("PesaTrack stores your financial records locally on this phone. Review the protections below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { ProfileMenuGroup(listOf(
            ProfileMenuItem(Icons.Rounded.Lock, "App lock", "Protect PesaTrack with a PIN", "violet") { lock = true },
            ProfileMenuItem(Icons.Rounded.Backup, "Backup & restore", "Create or restore an encrypted backup", "blue") { backup = true },
            ProfileMenuItem(Icons.Rounded.Security, "Privacy & data", "Review data choices and optional lookups", "green", privacy),
            ProfileMenuItem(Icons.Rounded.Message, "Money message access", "Choose which money messages PesaTrack can read", "amber", moneyAccess),
            ProfileMenuItem(Icons.Rounded.Settings, "More app settings", "Additional controls and preferences", "violet", settings)
        )) }
    }
}

@Composable
private fun ProfileAppearancePage(padding: PaddingValues, back: () -> Unit) {
    EverydayPage("Appearance", padding, back) {
        item {
            Text("PesaTrack uses the approved light appearance throughout Home, Activity, Plan and Profile.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            PesaCard {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.WbSunny, size = 48.dp,
                        background = pesaToneContainer("violet"), tint = pesaToneContent("violet"))
                    Column(Modifier.weight(1f).padding(horizontal = 13.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Light", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Bright surfaces with soft lavender cards and purple accents",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Light theme selected",
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
                }
            }
        }
        item {
            PesaCard {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Palette, size = 40.dp,
                        background = pesaToneContainer("blue"), tint = pesaToneContent("blue"))
                    Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                        Text("PesaTrack purple", style = MaterialTheme.typography.titleSmall)
                        Text("The selected accent used across the app",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(shape = CircleShape, color = Color(0xFF7625FF), modifier = Modifier.size(24.dp)) {}
                }
            }
        }
    }
}

@Composable
private fun ProfileFinancialPage(
    padding: PaddingValues,
    progress: FinancialProgress?,
    error: String?,
    retry: () -> Unit,
    back: () -> Unit,
    accounts: () -> Unit,
    goals: () -> Unit
) {
    EverydayPage("Financial overview", padding, back) {
        item {
            Text("A summary of recorded accounts, budgets and savings goals. Values come from your local PesaTrack data.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (progress == null && error == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (error != null) item {
            Text(error, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = retry) { Text("Try again") }
        }
        progress?.let { data ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileMiniStat("Accounts", data.accounts.toString(), Icons.Rounded.AccountBalanceWallet,
                        Modifier.weight(1f))
                    ProfileMiniStat("Budgets", data.budgets.size.toString(), Icons.Rounded.PieChart,
                        Modifier.weight(1f))
                    ProfileMiniStat("Active goals", data.goals.count { it.saved < it.target }.toString(),
                        Icons.Rounded.Savings, Modifier.weight(1f))
                }
            }
            item { EverydayRow(Icons.Rounded.AccountBalanceWallet, "Accounts & balances",
                "Open your recorded accounts and latest observed balances", accounts) }
            item { SectionHeader("Budget progress") }
            if (data.budgets.isEmpty()) item {
                Text("No budgets yet. Create a budget in Plan to see progress here.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            data.budgets.forEach { budget -> item { FinancialProgressCard(budget, false) } }
            item { EverydayRow(Icons.Rounded.Savings, "Savings goals",
                "Review goals and recorded contributions", goals) }
            if (data.goals.isEmpty()) item {
                Text("No savings goals yet. Add a goal in Plan to track contributions here.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            data.goals.forEach { goal ->
                item { Box(Modifier.fillMaxWidth().clickable(onClick = goals)) { FinancialProgressCard(goal, true) } }
            }
        }
    }
}

@Composable
private fun ProfileAboutPage(
    padding: PaddingValues,
    back: () -> Unit,
    privacyPolicy: () -> Unit,
    terms: () -> Unit,
    financialNotice: () -> Unit
) {
    EverydayPage("About PesaTrack", padding, back) {
        item {
            PesaCard {
                Column(Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.kedevjimmy.pesatrack.ui.components.PesaLogoMark(size = 54.dp)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("PesaTrack", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Version " + BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("Personal finance, made simple. Track, plan and build a better tomorrow.",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Made with care in Kenya", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { ProfileMenuGroup(listOf(
            ProfileMenuItem(Icons.Rounded.Policy, "Privacy policy", "How PesaTrack handles your data", "violet", privacyPolicy),
            ProfileMenuItem(Icons.Rounded.Description, "Terms of service", "Your responsibilities and rights", "blue", terms),
            ProfileMenuItem(Icons.Rounded.Info, "Financial information notice", "Accuracy and financial decisions", "amber", financialNotice)
        )) }
    }
}

@Composable
private fun FinancialProgressCard(item: ProgressItem, saving: Boolean) {
    val ratio = if (item.target <= 0) 0f else (item.saved.toDouble() / item.target).toFloat().coerceIn(0f, 1f)
    val money = NumberFormat.getNumberInstance(Locale.UK).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    PesaCard {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "KES " + money.format(item.saved / 100.0) +
                    (if (saving) " saved of " else " spent of ") +
                    "KES " + money.format(item.target / 100.0),
                style = MaterialTheme.typography.bodyMedium
            )
            LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth())
            Text(
                if (!saving && item.saved > item.target) "Over this month's budget"
                else if (saving && item.saved >= item.target) "Goal reached"
                else (item.saved.toDouble() / item.target.coerceAtLeast(1) * 100).toInt().toString() +
                    "% " + if (saving) "saved" else "used",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
