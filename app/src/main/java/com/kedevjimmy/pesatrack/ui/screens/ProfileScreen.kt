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
    onPageChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("pesatrack_preferences", 0) }
    var page by rememberSaveable { mutableStateOf("home") }
    var privacyReturnPage by rememberSaveable { mutableStateOf("security") }
    var accessReturnPage by rememberSaveable { mutableStateOf("security") }

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
            "policy", "terms", "financial_notice" -> "about"
            else -> "home"
        }
    }

    if (page == "personal") {
        PersonalDetailsScreen(scaffoldPadding, onBack = { page = "home" }, title = "Personal information")
        return
    }

    when (page) {
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
            { page = "archive" }
        )
        "archive" -> StatementArchivePrivacyPage(scaffoldPadding) { page = "privacy" }
        "access" -> MoneyAccessPage(
            scaffoldPadding,
            { page = accessReturnPage },
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
    email: String,
    avatar: String,
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
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            ProfileGradientBanner(name = name, email = email, avatar = avatar, onEdit = personal)
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
                    ProfileMenuItem(Icons.Rounded.SupportAgent, "Help & support",
                        "Guidance and troubleshooting", "violet", support),
                    ProfileMenuItem(Icons.Rounded.Info, "About PesaTrack",
                        "App details and important notices", "blue", about)
                )
            )
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
    val context = LocalContext.current
    val photo = remember(context) { ProfilePhotoStore(context).load() }
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
                        if (photo != null) {
                            Image(photo.asImageBitmap(), "Profile photo", Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop)
                        } else {
                            Text(avatar, style = MaterialTheme.typography.headlineMedium)
                        }
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
                Column(Modifier.fillMaxWidth().padding(17.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    IconBubble(Icons.Rounded.Shield, size = 58.dp,
                        background = pesaToneContainer("violet"), tint = pesaToneContent("violet"))
                    Text(if (enabled) "Your app lock is on" else "Protect your financial records",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text("Your financial records stay on this phone. Control how PesaTrack asks you to unlock it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { SectionHeader("App protection") }
        item { EverydayRow(Icons.Rounded.Lock, "App lock PIN",
            if (enabled) "Change or remove your PIN" else "Set a 6–8 digit PIN to protect your records") { lock = true } }
        item {
            ChoiceSwitch(
                "Unlock with biometrics",
                when {
                    !enabled -> "Set up your app lock PIN first"
                    !available -> "Biometrics are unavailable or not enrolled on this phone"
                    else -> "Use your fingerprint or device biometrics; your PIN still works"
                }, biometrics && enabled && available, enabled = enabled && available) { value ->
                    biometrics = value
                    manager.setBiometricUnlockEnabled(value)
                }
        }
        item {
            PesaCard {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Lock when I leave PesaTrack", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Text("Choose when the app should ask for your PIN again.",
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
            ProfileMenuItem(Icons.Rounded.Backup, "Backup & restore", "Create or restore an encrypted backup", "blue") { backup = true },
            ProfileMenuItem(Icons.Rounded.Security, "Privacy & data", "Review privacy choices and saved data", "green", privacy),
            ProfileMenuItem(Icons.Rounded.Message, "Money message access", "Choose which money messages PesaTrack can read", "amber", moneyAccess)
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
