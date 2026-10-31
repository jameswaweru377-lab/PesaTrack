package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.BuildConfig
import com.kedevjimmy.pesatrack.data.profile.ProfilePhotoStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.security.AppLockManager
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent

@Composable
fun ProfileScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onAccountsClick: (() -> Unit)? = null,
    onDataHealthClick: (() -> Unit)? = null,
    onGoalsClick: (() -> Unit)? = null,
    onHistoricalImportClick: (() -> Unit)? = null,
    homeCache: HomeScreenCache? = null,
    onPlanClick: (() -> Unit)? = null,
    onActivityClick: (() -> Unit)? = null,
    onPageChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("pesatrack_preferences", 0) }
    var page by rememberSaveable { mutableStateOf("home") }
    var privacyReturnPage by rememberSaveable { mutableStateOf("security") }
    var accessReturnPage by rememberSaveable { mutableStateOf("security") }
    var legalReturnPage by rememberSaveable { mutableStateOf("about") }
    val ledgerRevision by LedgerUpdateBus.state.collectAsState()
    val financialSignals = remember(context, homeCache, ledgerRevision.revision, page) {
        profileFinancialSignals(context, homeCache, ledgerRevision.revision)
    }
    val financialPresentation = presentProfileMoneyHabits(financialSignals)

    LaunchedEffect(page) {
        onPageChanged(page)
    }

    BackHandler(enabled = page == "home") {
        onBack()
    }
    BackHandler(enabled = page != "home" && page != "personal") {
        page = when (page) {
            "privacy" -> privacyReturnPage
            "access" -> accessReturnPage
            "archive" -> "privacy"
            "policy" -> legalReturnPage
            "terms", "financial_notice" -> "about"
            else -> "home"
        }
    }

    if (page == "personal") {
        PersonalDetailsScreen(scaffoldPadding, onBack = { page = "home" }, title = "Personal information")
        return
    }

    when (page) {
        "habits" -> ProfileMoneyHabitsDetails(
            padding = scaffoldPadding,
            back = { page = "home" },
            signals = financialSignals,
            onAction = { destination ->
                when (destination) {
                    "activity" -> (onActivityClick ?: onSettingsClick).invoke()
                    "accounts", "debts" -> (onAccountsClick ?: onSettingsClick).invoke()
                    else -> (onPlanClick ?: onGoalsClick ?: onSettingsClick).invoke()
                }
            }
        )
        "security" -> ProfileSecurityPage(
            padding = scaffoldPadding,
            back = { page = "home" },
            privacy = { privacyReturnPage = "security"; page = "privacy" },
            moneyAccess = { accessReturnPage = "security"; page = "access" }
        )
        "appearance" -> ProfileAppearancePage(scaffoldPadding) { page = "home" }
        "notifications" -> RemindersPage(scaffoldPadding, { page = "home" }, title = "Notifications")
        "support" -> HelpPage(
            scaffoldPadding,
            { page = "home" },
            onDataHealthClick ?: onSettingsClick,
            title = "Help & support",
            onMoneyAccess = { accessReturnPage = "support"; page = "access" },
            onPrivacy = { privacyReturnPage = "support"; page = "privacy" }
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
            { page = privacyReturnPage },
            { accessReturnPage = "privacy"; page = "access" },
            { page = "archive" },
            privacyPolicy = { legalReturnPage = "privacy"; page = "policy" }
        )
        "archive" -> StatementArchivePrivacyPage(scaffoldPadding) { page = "privacy" }
        "access" -> MoneyAccessPage(
            scaffoldPadding,
            { page = accessReturnPage },
            onHistoricalImportClick ?: onSettingsClick
        )
        "policy" -> LegalInformationPage(scaffoldPadding, { page = legalReturnPage }, LegalNotice.PRIVACY)
        "terms" -> LegalInformationPage(scaffoldPadding, { page = "about" }, LegalNotice.TERMS)
        "financial_notice" -> LegalInformationPage(scaffoldPadding, { page = "about" }, LegalNotice.FINANCIAL)
        else -> ProfileHomeScreen(
            padding = scaffoldPadding,
            name = prefs.getString("profile_name", "").orEmpty().ifBlank { "Your profile" },
            avatar = prefs.getString("profile_avatar", "🌿") ?: "🌿",
            moneyHabits = financialPresentation,
            onMoneyHabits = { page = "habits" },
            personal = { page = "personal" },
            security = { page = "security" },
            notifications = { page = "notifications" },
            appearance = { page = "appearance" },
            support = { page = "support" },
            about = { page = "about" }
        )
    }
}

@Composable
private fun ProfileHomeScreen(
    padding: PaddingValues,
    name: String,
    avatar: String,
    moneyHabits: ProfileMoneyHabitsPresentation,
    onMoneyHabits: () -> Unit,
    personal: () -> Unit,
    security: () -> Unit,
    notifications: () -> Unit,
    appearance: () -> Unit,
    support: () -> Unit,
    about: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ProfileGradientBanner(name = name, avatar = avatar, onEdit = personal)
        }
        item {
            ProfileMoneyHabitsCard(moneyHabits, onMoneyHabits)
        }
        item { SectionHeader("Your account") }
        item {
            ProfileMenuGroup(
                listOf(
                    ProfileMenuItem(Icons.Rounded.Person, "Personal information",
                        "Photo, name & email", "violet", personal),
                    ProfileMenuItem(Icons.Rounded.Security, "App lock & security",
                        "PIN, fingerprint & lock timing", "green", security),
                    ProfileMenuItem(Icons.Rounded.NotificationsActive, "Notifications",
                        "Bills, budgets & reminders", "red", notifications),
                    ProfileMenuItem(Icons.Rounded.Palette, "Appearance",
                        "Light theme & display", "amber", appearance)
                )
            )
        }
        item { SectionHeader("Support") }
        item {
            ProfileMenuGroup(listOf(
                ProfileMenuItem(Icons.Rounded.SupportAgent, "Help & support",
                    "Guides & troubleshooting", "blue", support),
                ProfileMenuItem(Icons.Rounded.Info, "About PesaTrack",
                    "Version, privacy & app details", "violet", about)
            ))
        }
    }
}

@Composable
private fun ProfileGradientBanner(name: String, avatar: String, onEdit: () -> Unit) {
    val context = LocalContext.current
    val photo = remember(context) { ProfilePhotoStore(context).load() }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
        contentColor = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(
            Modifier.background(
                Brush.linearGradient(
                    listOf(Color(0xFF8B43FF), Color(0xFF6825E8), Color(0xFF4C3BCB))
                )
            ).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = .18f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = .82f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (photo != null) {
                            Image(photo.asImageBitmap(), "Profile photo", Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop)
                        } else {
                            Text(avatar, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Only on this device", style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = .88f))
                    OutlinedButton(onClick = onEdit,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .8f))) {
                        Text("Edit profile")
                    }
                }
            }
            Text("Your money, your plans.", style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = .9f))
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
    moneyAccess: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember(context) { AppLockManager(context) }
    var changed by remember { mutableIntStateOf(0) }
    val enabled = remember(changed) { manager.isEnabled() }
    var biometrics by remember(changed) { mutableStateOf(manager.isBiometricUnlockEnabled()) }
    var grace by remember(changed) { mutableLongStateOf(manager.backgroundLockGraceMs()) }
    val available = remember(context) { profileBiometricsAvailable(context) }
    var lock by remember { mutableStateOf(false) }
    var backup by rememberSaveable { mutableStateOf(false) }
    if (lock) AppLockSettingsSheet(onDismiss = { lock = false; changed++ }, onChanged = { lock = false; changed++ })
    if (backup) BackupRestoreSheet(onDismiss = { backup = false })

    EverydayPage("App lock & security", padding, back) {
        item {
            PesaCard {
                Row(Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Shield, size = 58.dp,
                        background = pesaToneContainer("violet"), tint = pesaToneContent("violet"))
                    Column(Modifier.padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (enabled) "Your app is protected" else "Your privacy comes first",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text(if (enabled) "App lock is on. You're in control."
                            else "Add a PIN to help keep your records private.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { SectionHeader("Unlock PesaTrack") }
        item { EverydayRow(Icons.Rounded.Lock, "App lock PIN",
            if (enabled) "Change or remove your PIN" else "Set a PIN to protect your records") { lock = true } }
        item {
            ChoiceSwitch(
                "Unlock with biometrics",
                when {
                    !enabled -> "Set up your app lock PIN first"
                    !available -> "Biometrics are unavailable or not enrolled on this phone"
                    else -> "Use your fingerprint. Your PIN still works."
                }, biometrics && enabled && available, enabled = enabled && available) { value ->
                    biometrics = value
                    manager.setBiometricUnlockEnabled(value)
                }
        }
        item {
            PesaCard {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Ask for PIN again", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Text("After you leave PesaTrack:",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(0L to "Immediately", 30_000L to "30 sec", 60_000L to "1 min")
                            .forEach { (value, label) ->
                                FilterChip(
                                    selected = grace == value,
                                    onClick = { grace = value; manager.setBackgroundLockGraceMs(value) },
                                    enabled = enabled,
                                    label = { Text(label) }
                                )
                            }
                    }
                    if (!enabled) Text("Turn on App lock PIN to change this setting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { SectionHeader("Privacy & recovery") }
        item { ProfileMenuGroup(listOf(
            ProfileMenuItem(Icons.Rounded.Backup, "Encrypted backup & restore", "Keep a private copy of your records", "violet") { backup = true },
            ProfileMenuItem(Icons.Rounded.Security, "Privacy & data", "Review privacy choices and saved data", "green", privacy),
            ProfileMenuItem(Icons.Rounded.Message, "Money message access", "Choose which messages the app can read", "amber", moneyAccess)
        )) }
    }
}
private fun profileBiometricsAvailable(context: android.content.Context): Boolean {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) return false
    val keyguard = context.getSystemService(android.app.KeyguardManager::class.java)
    if (keyguard?.isKeyguardSecure != true) return false
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
            ?.canAuthenticate() == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS
    } else context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT)
}

@Composable
internal fun ProfileAboutPage(
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.kedevjimmy.pesatrack.ui.components.PesaLogoMark(size = 58.dp)
                        Column(Modifier.padding(start = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("PesaTrack", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Your money, better days.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text("Understand your money, plan ahead and feel more in control.",
                        style = MaterialTheme.typography.bodyMedium)
                    Surface(color = pesaToneContainer("violet"), shape = MaterialTheme.shapes.small) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Info, contentDescription = null,
                                tint = pesaToneContent("violet"), modifier = Modifier.size(18.dp))
                            Text("Version " + BuildConfig.VERSION_NAME,
                                modifier = Modifier.padding(start = 8.dp),
                                color = pesaToneContent("violet"),
                                style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        item { SectionHeader("Good to know") }
        item { ProfileMenuGroup(listOf(
            ProfileMenuItem(Icons.Rounded.Policy, "Privacy policy", "Your data, your choices", "violet", privacyPolicy),
            ProfileMenuItem(Icons.Rounded.Description, "Terms of service", "Using PesaTrack responsibly", "blue", terms),
            ProfileMenuItem(Icons.Rounded.Info, "About money guidance", "How to understand the insights", "amber", financialNotice)
        )) }
        item {
            PesaCard {
                Row(Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Favorite, size = 48.dp,
                        background = pesaToneContainer("green"),
                        tint = pesaToneContent("green"))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Made with care in Kenya",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text("Built for everyday money decisions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
