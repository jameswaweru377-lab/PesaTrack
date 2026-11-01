package com.kedevjimmy.pesatrack.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kedevjimmy.pesatrack.data.profile.ProfilePhotoStore
import com.kedevjimmy.pesatrack.ui.theme.MoreVisualTokens as M

/**
 * Approved More page (November 2026).
 *
 * Presentation only. Every destination stays wired to an existing feature;
 * profile name and photo are loaded from the device, never hardcoded sample data.
 */


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    scaffoldPadding: PaddingValues,
    onProfileClick: () -> Unit,
    onProfileSectionClick: (String) -> Unit,
    onAccountsClick: () -> Unit,
    onImportClick: () -> Unit,
    onPastTransactionsClick: () -> Unit,
    onStatementActivityClick: () -> Unit,
    onDataHealthClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onReportsClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences = remember(context) {
        context.getSharedPreferences("pesatrack_preferences", android.content.Context.MODE_PRIVATE)
    }
    val photoStore = remember(context) { ProfilePhotoStore(context) }
    var name by remember { mutableStateOf(preferences.getString("profile_name", "").orEmpty()) }
    var photo by remember { mutableStateOf<Bitmap?>(photoStore.load()) }
    var avatar by remember { mutableStateOf(preferences.getString("profile_avatar", "J").orEmpty()) }
    var importOptionsOpen by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner, photoStore, preferences) {
        val listener = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                name = preferences.getString("profile_name", "").orEmpty()
                avatar = preferences.getString("profile_avatar", "J").orEmpty()
                photo = photoStore.load()
            }
        }
        lifecycleOwner.lifecycle.addObserver(listener)
        onDispose { lifecycleOwner.lifecycle.removeObserver(listener) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(scaffoldPadding)
            .background(Brush.verticalGradient(listOf(M.canvasTop, M.canvasMiddle, M.canvasBottom))),
        contentPadding = PaddingValues(
            start = M.space14, end = M.space14, top = M.space8, bottom = M.space24
        ),
        verticalArrangement = Arrangement.spacedBy(M.space12)
    ) {
        item { MorePageHeading() }
        item { MoreProfileHero(name.ifBlank { "Your profile" }, avatar, photo, onProfileClick) }
        item { MoreWorkspaceSection(onStatementActivityClick, { importOptionsOpen = true },
            onAccountsClick, onReportsClick) }
        item { MorePreferencesSection(onProfileSectionClick, onSettingsClick) }
        item { MoreSupportSection(onProfileSectionClick, onDataHealthClick,
            onPastTransactionsClick, onAccountsClick) }
    }
    if (importOptionsOpen) {
        MoreImportChoiceSheet(
            onDismiss = { importOptionsOpen = false },
            onSms = { importOptionsOpen = false; onImportClick() },
            onStatement = { importOptionsOpen = false; onStatementActivityClick() }
        )
    }
}

@Composable
private fun MorePageHeading() {
    Column(verticalArrangement = Arrangement.spacedBy(M.space1)) {
        Text("More", color = M.ink,
            fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 32.sp)
        Text("Manage your account, data and app settings.",
            color = M.muted, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun MoreWorkspaceSection(
    statements: () -> Unit, imports: () -> Unit,
    accounts: () -> Unit, reports: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(M.space8)) {
        MoreSectionTitle("Money workspace")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(M.space5)) {
            MoreWorkspaceTile(Modifier.weight(1f), Icons.Rounded.Description,
                M.iconStatement, M.iconPurpleSoft, "Statements", "Imported evidence", statements)
            MoreWorkspaceTile(Modifier.weight(1f), Icons.Rounded.FileDownload,
                M.blue, M.iconBlueSoft, "Imports", "SMS, PDF & CSV", imports)
            MoreWorkspaceTile(Modifier.weight(1f), Icons.Rounded.AccountBalanceWallet,
                M.mint, M.iconMintSoft, "Accounts", "Wallets & banks", accounts)
            MoreWorkspaceTile(Modifier.weight(1f), Icons.Rounded.BarChart,
                M.purple, M.iconPurpleSoft, "Reports", "Trends & insights", reports)
        }
    }
}

@Composable
private fun MorePreferencesSection(openProfile: (String) -> Unit, settings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(M.space8)) {
        MoreSectionTitle("App preferences")
        MoreSettingsGroup {
            MoreSettingsRow(Icons.Rounded.NotificationsNone, "Notifications",
                "Bills, budgets & reminders", "purple") { openProfile("notifications") }
            MoreSettingsRow(Icons.Rounded.Lock, "App lock & security",
                "PIN, fingerprint & access", "green") { openProfile("security") }
            MoreSettingsRow(Icons.Rounded.Palette, "Appearance",
                "Theme, text size & display", "orange") { openProfile("appearance") }
            MoreSettingsRow(Icons.Rounded.Shield, "Privacy & data",
                "Permissions and data control", "blue") { openProfile("privacy") }
            MoreSettingsRow(Icons.Rounded.Settings, "Settings",
                "General app behavior", "purple", last = true, onClick = settings)
        }
    }
}

@Composable
private fun MoreSupportSection(
    openProfile: (String) -> Unit, diagnostics: () -> Unit,
    pastTransactions: () -> Unit, funds: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(M.space8)) {
        MoreSectionTitle("Support & tools")
        MoreSettingsGroup {
            MoreSettingsRow(Icons.Rounded.SupportAgent, "Help & support",
                "Guides, FAQs & contact", "blue") { openProfile("support") }
            MoreSettingsRow(Icons.Rounded.Build, "Advanced troubleshooting",
                "Diagnostics & data checks", "purple", onClick = diagnostics)
            MoreSettingsRow(Icons.Rounded.History, "Past transactions",
                "Review older activity", "blue", onClick = pastTransactions)
            MoreSettingsRow(Icons.Rounded.Wallet, "Funds",
                "Balances, sources & movement", "green", onClick = funds)
            MoreSettingsRow(Icons.Rounded.Info, "About PesaTrack",
                "Version, legal & product info", "purple", last = true) {
                openProfile("about")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreImportChoiceSheet(onDismiss: () -> Unit, onSms: () -> Unit,
    onStatement: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(
            start = M.space16, end = M.space16, bottom = M.space28
        ), verticalArrangement = Arrangement.spacedBy(M.space12)) {
            Text("Import money records", fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold, color = M.ink)
            Text("Choose how to add older records to PesaTrack.",
                color = M.muted, fontSize = 13.sp)
            MoreSettingsGroup {
                MoreSettingsRow(Icons.Rounded.History, "Import financial SMS",
                    "Bring in older money messages on this phone", "blue", onClick = onSms)
                MoreSettingsRow(Icons.Rounded.Description, "Import PDF or CSV statement",
                    "Use the statement import workspace", "purple",
                    last = true, onClick = onStatement)
            }
        }
    }
}

@Composable
private fun MoreSectionTitle(title: String) {
    Text(
        text = title,
        color = M.ink,
        fontSize = 17.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = M.space8)
    )
}

/** A low-profile premium banner, matching the approved compact reference proportions. */
@Composable
private fun MoreProfileHero(name: String, avatar: String, photo: Bitmap?, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(M.heroHeight).clickable(onClick = onClick),
        shape = RoundedCornerShape(M.space20),
        color = M.heroBase, shadowElevation = M.space6
    ) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(
            listOf(M.heroBright, M.heroDeep, M.heroBlue)
        ))) {
            MoreHeroWaves(Modifier.matchParentSize())
            MoreHeroContent(name, avatar, photo, onClick)
        }
    }
}

@Composable
private fun MoreHeroWaves(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val width = size.width
        val height = size.height
        val wave = Path().apply {
            moveTo(width * .46f, height)
            cubicTo(width * .77f, height * .70f, width * .78f, height * 1.11f,
                width, height * .54f)
            lineTo(width, height)
            close()
        }
        drawPath(wave, Brush.horizontalGradient(listOf(
            M.wavePurple.copy(alpha = .50f), M.wavePink.copy(alpha = .76f)
        )))
        val highlight = Path().apply {
            moveTo(width * .47f, height)
            cubicTo(width * .73f, height * .62f, width * .83f, height * .97f,
                width, height * .48f)
        }
        drawPath(highlight, M.waveCyan.copy(alpha = .68f),
            style = Stroke(width = M.waveStroke.toPx()))
    }
}

@Composable
private fun MoreHeroContent(name: String, avatar: String, photo: Bitmap?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = M.space13, vertical = M.space11),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(M.space11)
    ) {
        MoreHeroAvatar(name, avatar, photo)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(M.space6)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(M.space5)) {
                Text(name, color = Color.White, fontSize = 15.sp,
                    lineHeight = 19.sp, fontWeight = FontWeight.ExtraBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                MorePremiumBadge()
            }
            Text("Your money, your plans.", color = Color.White.copy(alpha = .94f),
                fontSize = 11.sp, maxLines = 1)
            MoreViewProfileAction(onClick)
        }
    }
}

@Composable
private fun MoreHeroAvatar(name: String, avatar: String, photo: Bitmap?) {
    Surface(
        modifier = Modifier.size(M.avatarSize),
        shape = CircleShape, color = M.avatarBackground,
        border = androidx.compose.foundation.BorderStroke(M.space2, Color.White)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (photo != null) {
                Image(photo.asImageBitmap(), contentDescription = "Your profile photo",
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(name.trim().firstOrNull()?.uppercaseChar()?.toString()
                    ?: avatar.take(1).ifBlank { "J" },
                    fontSize = 29.sp, fontWeight = FontWeight.Bold, color = M.avatarInk)
            }
        }
    }
}

@Composable
private fun MorePremiumBadge() {
    Surface(color = Color.White.copy(alpha = .10f), shape = RoundedCornerShape(M.space20),
        border = androidx.compose.foundation.BorderStroke(
            M.space1, Color.White.copy(alpha = .72f)
        )) {
        Row(Modifier.padding(horizontal = M.space7, vertical = M.space4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(M.space3)) {
            MoreCrownGlyph()
            Text("Premium", color = Color.White, fontSize = 9.sp,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MoreViewProfileAction(onClick: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(M.space18),
        modifier = Modifier.clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = M.space12, vertical = M.space6),
            horizontalArrangement = Arrangement.spacedBy(M.space5),
            verticalAlignment = Alignment.CenterVertically) {
            Text("View profile", fontSize = 11.sp,
                fontWeight = FontWeight.Bold, color = M.actionInk)
            Icon(Icons.Rounded.ArrowForward, contentDescription = null,
                modifier = Modifier.size(M.space14), tint = M.actionInk)
        }
    }
}

@Composable
private fun MoreWorkspaceTile(
    modifier: Modifier,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.heightIn(min = M.workspaceTileHeight).clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(M.space17),
        shadowElevation = M.space3,
        border = androidx.compose.foundation.BorderStroke(M.space1, M.workspaceBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = M.space4, vertical = M.space10),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(M.space5)
        ) {
            Surface(
                modifier = Modifier.size(M.iconMedium),
                shape = RoundedCornerShape(M.space15),
                color = iconBackground
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null,
                        tint = iconTint, modifier = Modifier.size(M.space23))
                }
            }
            Text(title, color = M.ink, fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(subtitle, color = M.muted, fontSize = 9.sp,
                lineHeight = 11.sp, maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun MoreSettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(M.space20),
        shadowElevation = M.space3,
        border = androidx.compose.foundation.BorderStroke(M.space1, M.groupBorder)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = M.space11), content = content)
    }
}

@Composable
private fun MoreSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tone: String,
    last: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = M.listRowHeight).clickable(onClick = onClick)
            .padding(horizontal = M.space3, vertical = M.space8),
        horizontalArrangement = Arrangement.spacedBy(M.space10),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (background, tint) = when (tone) {
            "green" -> M.greenSoft to M.green
            "orange" -> M.orangeSoft to M.orangeBright
            "blue" -> M.blueSoft to M.blueBright
            else -> M.purpleSoft to M.purpleBright
        }
        Surface(
            modifier = Modifier.size(M.iconSmall),
            color = background,
            shape = RoundedCornerShape(M.space12)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint,
                    modifier = Modifier.size(M.space20))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(M.space2)) {
            Text(title, color = M.ink, fontWeight = FontWeight.Bold,
                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = M.muted, fontSize = 10.sp,
                lineHeight = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null,
            tint = M.arrowInk, modifier = Modifier.size(M.space21))
    }
    if (!last) HorizontalDivider(
        color = M.rowDivider,
        thickness = M.dividerStroke,
        modifier = Modifier.padding(start = M.touchTarget)
    )
}


/** Tiny vector crown from the approved badge, no network image or icon dependency. */
@Composable
private fun MoreCrownGlyph() {
    Canvas(Modifier.size(M.space12)) {
        val crown = Path().apply {
            moveTo(size.width * .08f, size.height * .25f)
            lineTo(size.width * .31f, size.height * .52f)
            lineTo(size.width * .50f, size.height * .12f)
            lineTo(size.width * .69f, size.height * .52f)
            lineTo(size.width * .92f, size.height * .25f)
            lineTo(size.width * .84f, size.height * .80f)
            lineTo(size.width * .16f, size.height * .80f)
            close()
        }
        drawPath(crown, M.crownGold)
    }
}
