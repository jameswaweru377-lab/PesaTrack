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

/**
 * Approved More page (November 2026).
 *
 * Presentation only. Every destination stays wired to an existing feature;
 * profile name and photo are loaded from the device, never hardcoded sample data.
 */
private object MorePalette {
    val ink = Color(0xFF12123E)
    val muted = Color(0xFF5B638E)
    val purple = Color(0xFF6212F5)
    val lavender = Color(0xFFEEE7FF)
    val blue = Color(0xFF106DFA)
    val mint = Color(0xFF00A96A)
    val orange = Color(0xFFFF7438)
}

@Composable
fun MoreScreen(
    scaffoldPadding: PaddingValues,
    onProfileClick: () -> Unit,
    onProfileSectionClick: (String) -> Unit,
    onAccountsClick: () -> Unit,
    onImportClick: () -> Unit,
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

    // After editing Profile, return to More with the updated real name and photo.
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
        modifier = Modifier.fillMaxSize()
            .padding(scaffoldPadding)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF7F6FF), Color(0xFFF7F8FF), Color(0xFFF0F3FF))
                )
            ),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    "More", color = MorePalette.ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp, lineHeight = 32.sp
                )
                Text(
                    "Manage your account, data and app settings.",
                    color = MorePalette.muted, fontSize = 12.sp, lineHeight = 16.sp
                )
            }
        }
        item {
            MoreProfileHero(
                name = name.ifBlank { "Your profile" },
                avatar = avatar,
                photo = photo,
                onClick = onProfileClick
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MoreSectionTitle("Money workspace")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    MoreWorkspaceTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Description,
                        iconTint = Color(0xFF6F19E9),
                        iconBackground = Color(0xFFEEE5FF),
                        title = "Statements",
                        subtitle = "Imported evidence",
                        onClick = onStatementActivityClick
                    )
                    MoreWorkspaceTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.FileDownload,
                        iconTint = MorePalette.blue,
                        iconBackground = Color(0xFFE6EEFF),
                        title = "Imports",
                        subtitle = "SMS, PDF & CSV",
                        onClick = onImportClick
                    )
                    MoreWorkspaceTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.AccountBalanceWallet,
                        iconTint = MorePalette.mint,
                        iconBackground = Color(0xFFDBFAEE),
                        title = "Accounts",
                        subtitle = "Wallets & banks",
                        onClick = onAccountsClick
                    )
                    MoreWorkspaceTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.BarChart,
                        iconTint = MorePalette.purple,
                        iconBackground = Color(0xFFEEE5FF),
                        title = "Reports",
                        subtitle = "Trends & insights",
                        onClick = onReportsClick
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MoreSectionTitle("App preferences")
                MoreSettingsGroup {
                    MoreSettingsRow(Icons.Rounded.NotificationsNone, "Notifications",
                        "Bills, budgets & reminders", "purple") {
                        onProfileSectionClick("notifications")
                    }
                    MoreSettingsRow(Icons.Rounded.Lock, "App lock & security",
                        "PIN, fingerprint & access", "green") {
                        onProfileSectionClick("security")
                    }
                    MoreSettingsRow(Icons.Rounded.Palette, "Appearance",
                        "Theme, text size & display", "orange") {
                        onProfileSectionClick("appearance")
                    }
                    MoreSettingsRow(Icons.Rounded.Shield, "Privacy & data",
                        "Permissions and data control", "blue") {
                        onProfileSectionClick("privacy")
                    }
                    MoreSettingsRow(Icons.Rounded.Settings, "Settings",
                        "General app behavior", "purple", onClick = onSettingsClick, last = true)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MoreSectionTitle("Support & tools")
                MoreSettingsGroup {
                    MoreSettingsRow(Icons.Rounded.SupportAgent, "Help & support",
                        "Guides, FAQs & contact", "blue") {
                        onProfileSectionClick("support")
                    }
                    MoreSettingsRow(Icons.Rounded.Build, "Advanced troubleshooting",
                        "Diagnostics & data checks", "purple", onClick = onDataHealthClick)
                    MoreSettingsRow(Icons.Rounded.History, "Past transactions",
                        "Review older activity", "blue", onClick = onImportClick)
                    MoreSettingsRow(Icons.Rounded.Wallet, "Funds",
                        "Balances, sources & movement", "green", onClick = onAccountsClick)
                    MoreSettingsRow(Icons.Rounded.Info, "About PesaTrack",
                        "Version, legal & product info", "purple", last = true) {
                        onProfileSectionClick("about")
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreSectionTitle(title: String) {
    Text(
        text = title,
        color = MorePalette.ink,
        fontSize = 17.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = 8.dp)
    )
}

/** A low-profile premium banner, matching the approved compact reference proportions. */
@Composable
private fun MoreProfileHero(name: String, avatar: String, photo: Bitmap?, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(104.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF5A0DF5),
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    listOf(Color(0xFF801DFF), Color(0xFF4204CB), Color(0xFF0956E8))
                )
            )
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val wave = Path().apply {
                    moveTo(width * .46f, height)
                    cubicTo(width * .77f, height * .70f, width * .78f, height * 1.11f, width, height * .54f)
                    lineTo(width, height)
                    close()
                }
                drawPath(wave, Brush.horizontalGradient(
                    listOf(Color(0xFF7C43FF).copy(alpha = .50f), Color(0xFFC34AFF).copy(alpha = .76f))
                ))
                val highlight = Path().apply {
                    moveTo(width * .47f, height)
                    cubicTo(width * .73f, height * .62f, width * .83f, height * .97f, width, height * .48f)
                }
                drawPath(highlight, Color(0xFF5CE2FF).copy(alpha = .68f),
                    style = Stroke(width = 1.3.dp.toPx()))
            }
            Row(
                Modifier.fillMaxSize().padding(horizontal = 13.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Surface(
                    modifier = Modifier.size(70.dp),
                    shape = CircleShape,
                    color = Color(0xFFDCD2FF),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (photo != null) {
                            Image(photo.asImageBitmap(), contentDescription = "Your profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize())
                        } else {
                            Text(
                                name.trim().firstOrNull()?.uppercaseChar()?.toString()
                                    ?: avatar.take(1).ifBlank { "J" },
                                fontSize = 29.sp, fontWeight = FontWeight.Bold,
                                color = Color(0xFF3709BD)
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            name, color = Color.White, fontSize = 15.sp,
                            lineHeight = 19.sp, fontWeight = FontWeight.ExtraBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = Color.White.copy(alpha = .10f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, Color.White.copy(alpha = .72f)
                            )
                        ) {
                            Row(
                                Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                MoreCrownGlyph()
                                Text("Premium", color = Color.White, fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("Your money, your plans.", color = Color.White.copy(alpha = .94f),
                        fontSize = 11.sp, maxLines = 1)
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.clickable(onClick = onClick)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("View profile", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = Color(0xFF4810C8))
                            Icon(Icons.Rounded.ArrowForward, contentDescription = null,
                                modifier = Modifier.size(14.dp), tint = Color(0xFF4810C8))
                        }
                    }
                }
            }
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
        modifier = modifier.heightIn(min = 108.dp).clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(17.dp),
        shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1EDFF))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(15.dp),
                color = iconBackground
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null,
                        tint = iconTint, modifier = Modifier.size(23.dp))
                }
            }
            Text(title, color = MorePalette.ink, fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(subtitle, color = MorePalette.muted, fontSize = 9.sp,
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
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0EEFC))
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp), content = content)
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
        Modifier.fillMaxWidth().heightIn(min = 58.dp).clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (background, tint) = when (tone) {
            "green" -> Color(0xFFDFF9EC) to Color(0xFF08A573)
            "orange" -> Color(0xFFFFEADF) to Color(0xFFFF6E2C)
            "blue" -> Color(0xFFE3EFFF) to Color(0xFF087BF8)
            else -> Color(0xFFEDE5FF) to Color(0xFF6725EB)
        }
        Surface(
            modifier = Modifier.size(36.dp),
            color = background,
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint,
                    modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = MorePalette.ink, fontWeight = FontWeight.Bold,
                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = MorePalette.muted, fontSize = 10.sp,
                lineHeight = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null,
            tint = Color(0xFF555E92), modifier = Modifier.size(21.dp))
    }
    if (!last) HorizontalDivider(
        color = Color(0xFFE4E5F2),
        thickness = .7.dp,
        modifier = Modifier.padding(start = 48.dp)
    )
}


/** Tiny vector crown from the approved badge, no network image or icon dependency. */
@Composable
private fun MoreCrownGlyph() {
    Canvas(Modifier.size(12.dp)) {
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
        drawPath(crown, Color(0xFFFFD34B))
    }
}
