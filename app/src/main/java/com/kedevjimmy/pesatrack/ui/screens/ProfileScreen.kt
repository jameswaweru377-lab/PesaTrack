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
import com.kedevjimmy.pesatrack.ui.theme.ProfileVisualColors as C

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
    onPageChanged: (String) -> Unit = {},
    initialPage: String = "home"
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("pesatrack_preferences", 0) }
    // Direct More shortcuts open the real, existing sub-pages without passing
    // through the Profile home dashboard; normal Profile opens remain unchanged.
    var page by rememberSaveable(initialPage) { mutableStateOf(initialPage) }
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
            onHistory = onHistoricalImportClick ?: onSettingsClick,
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
            back = onBack,
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
    back: () -> Unit,
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
    ProfilePremiumPage("Profile", padding, back) {
        item {
            ProfileGradientBanner(name = name, avatar = avatar, onEdit = personal)
        }
        item {
            ProfileMoneyHabitsCard(moneyHabits, onMoneyHabits)
        }
        item { ProfileSection("Your account") }
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
        item { ProfileSection("Support") }
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
        Box(Modifier.background(Brush.linearGradient(listOf(C.violetBright, C.violet, C.violetDeep)))) {
          ProfileBannerWavesPublic(Modifier.matchParentSize())
          Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(modifier = Modifier.size(108.dp), shape = CircleShape,
                        color = Color.White.copy(alpha = .18f),
                        border = androidx.compose.foundation.BorderStroke(3.dp, Color.White)) {
                      Box(contentAlignment = Alignment.Center) {
                        if (photo != null) {
                            Image(photo.asImageBitmap(), "Profile photo", Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop)
                        } else {
                            Text(avatar, style = MaterialTheme.typography.headlineMedium)
                        }
                      }
                    }
                    Surface(modifier = Modifier.size(34.dp).clickable(onClick = onEdit),
                        shape = CircleShape, color = Color.White) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PhotoCamera, null, tint = C.violet,
                                modifier = Modifier.size(19.dp))
                        }
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Smartphone, null, Modifier.size(16.dp), tint = Color.White)
                        Text("Only on this device", modifier = Modifier.padding(start = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = .88f))
                    }
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
    ProfilePremiumGroup(items.map { item -> ProfilePremiumItem(
        icon = item.icon, title = item.title, subtitle = item.subtitle,
        tone = when (item.tone) {
            "green" -> ProfileTone.GREEN
            "red" -> ProfileTone.PINK
            "amber" -> ProfileTone.AMBER
            "blue" -> ProfileTone.BLUE
            else -> ProfileTone.VIOLET
        }, onClick = item.onClick
    ) })
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
    var timing by rememberSaveable { mutableStateOf(false) }
    if (lock) AppLockSettingsSheet(onDismiss = { lock = false; changed++ }, onChanged = { lock = false; changed++ })
    if (backup) BackupRestoreSheet(onDismiss = { backup = false })
    if (timing) AlertDialog(
        onDismissRequest = { timing = false },
        title = { Text("Lock automatically") },
        text = {
            Column {
                listOf(0L to "Immediately", 30_000L to "After 30 seconds", 60_000L to "After 1 minute")
                    .forEach { (value, label) ->
                        Row(Modifier.fillMaxWidth().clickable {
                            grace = value; manager.setBackgroundLockGraceMs(value); timing = false
                        }.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(grace == value, onClick = {
                                grace = value; manager.setBackgroundLockGraceMs(value); timing = false
                            })
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
            }
        },
        confirmButton = { TextButton(onClick = { timing = false }) { Text("Done") } }
    )

    ProfilePremiumPage("App lock & security", padding, back) {
        item {
            ProfileBanner("Your money stays private", "Choose how you unlock PesaTrack.",
                Icons.Rounded.EnhancedEncryption, dark = true, art = ProfileHeroArt.SHIELD)
        }
        item { ProfileSection("App access") }
        item {
            ProfilePremiumGroup(listOf(
                ProfilePremiumItem(Icons.Rounded.Dialpad, "App PIN",
                    if (enabled) "Change PIN" else "Set a PIN to protect your records",
                    ProfileTone.VIOLET, onClick = { lock = true }),
                ProfilePremiumItem(Icons.Rounded.Fingerprint, "Use fingerprint",
                    when {
                        !enabled -> "Set your app PIN first"
                        !available -> "Not available on this phone"
                        else -> "Unlock without typing your PIN"
                    }, ProfileTone.GREEN, checked = biometrics && enabled && available,
                    enabled = enabled && available,
                    onToggle = { value ->
                        biometrics = value; manager.setBiometricUnlockEnabled(value)
                    }),
                ProfilePremiumItem(Icons.Rounded.LockClock, "Lock automatically",
                    when (grace) {
                        0L -> "Immediately"
                        30_000L -> "After 30 seconds"
                        else -> "After 1 minute"
                    }, ProfileTone.AMBER, onClick = { timing = true }, enabled = enabled)
            ))
        }
        item { ProfileSection("Protect your data") }
        item {
            ProfilePremiumGroup(listOf(
                ProfilePremiumItem(Icons.Rounded.CloudUpload, "Encrypted backup & restore",
                    "Save a protected copy", ProfileTone.BLUE, onClick = { backup = true }),
                ProfilePremiumItem(Icons.Rounded.GppGood, "Privacy & data",
                    "See what's kept on this phone", ProfileTone.PINK, onClick = privacy)
            ))
        }
        item { ProfileHint("Your backup password is separate from your PIN.", Icons.Rounded.Info) }
        item { TextButton(onClick = moneyAccess) { Text("Manage money message access") } }
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
    var details by rememberSaveable { mutableStateOf<String?>(null) }
    if (details != null) AlertDialog(
        onDismissRequest = { details = null },
        title = { Text(if (details == "version") "App version" else "Open-source licenses") },
        text = { Text(if (details == "version") "Installed version: ${BuildConfig.VERSION_NAME}"
            else "PesaTrack uses Kotlin, AndroidX, Jetpack Compose and other open-source libraries. Their license notices are available with the source code.") },
        confirmButton = { TextButton(onClick = { details = null }) { Text("Close") } }
    )
    ProfilePremiumPage("About PesaTrack", padding, back) {
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(80.dp).background(
                    Brush.linearGradient(listOf(C.violetBright, C.violetDeep)),
                    RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                    Text("P", color = Color.White, fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.displayLarge)
                }
                Text("Your money, better days.", color = C.ink,
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("A private way to understand your money.",
                    style = MaterialTheme.typography.bodyMedium, color = C.muted)
            }
        }
        item {
            ProfilePremiumGroup(listOf(
                ProfilePremiumItem(Icons.Rounded.BarChart, "Made for everyday money",
                    "Understand bills, spending and goals", ProfileTone.VIOLET),
                ProfilePremiumItem(Icons.Rounded.Lock, "Private by design",
                    "Your records stay in your control", ProfileTone.GREEN)
            ))
        }
        item { ProfileSection("App information") }
        item {
            ProfilePremiumGroup(listOf(
                ProfilePremiumItem(Icons.Rounded.Info, "App version",
                    "See installed version", ProfileTone.BLUE, onClick = { details = "version" }),
                ProfilePremiumItem(Icons.Rounded.Shield, "Privacy policy",
                    "How your data is handled", ProfileTone.PINK, onClick = privacyPolicy),
                ProfilePremiumItem(Icons.Rounded.Description, "Terms of use",
                    "The important details", ProfileTone.GREEN, onClick = terms),
                ProfilePremiumItem(Icons.Rounded.Code, "Licenses",
                    "The tools that help us build", ProfileTone.AMBER, onClick = { details = "licenses" })
            ))
        }
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.Favorite, null, tint = C.violet, modifier = Modifier.size(19.dp))
                Text("Made with care for everyday money.",
                    modifier = Modifier.padding(start = 8.dp), color = C.muted,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        item { TextButton(onClick = financialNotice) {
            Text("About money guidance", color = C.muted)
        } }
    }
}
