package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.ui.components.MainTab

/** A self-contained Home treatment: other approved tab designs remain unchanged. */
internal object BrownHomePalette {
    val canvas = Color(0xFFFCF8F4)
    val ink = Color(0xFF201610)
    val coffee = Color(0xFF652F10)
    val muted = Color(0xFF766C64)
    val cream = Color(0xFFF8EBDE)
    val outline = Color(0xFFE3BE92)
    val white = Color.White
}

@Composable
internal fun BrownHomeHeader(
    onMenu: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    profileInitial: String,
    profilePhoto: ImageBitmap?
) {
    val p = BrownHomePalette
    Row(Modifier.fillMaxWidth().height(76.dp).background(p.canvas)
        .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clickable(onClickLabel = "Open More", onClick = onMenu),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Menu, "More menu", Modifier.size(28.dp), tint = p.ink)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text("P", color = p.coffee, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black, fontSize = 26.sp, lineHeight = 26.sp)
            Text("PesaTrack", color = p.ink, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 23.sp)
            Text("YOUR MONEY, YOUR PLANS", color = p.coffee, fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp, fontSize = 8.sp, lineHeight = 12.sp)
        }
        Box(Modifier.size(48.dp).clickable(onClickLabel = "Notifications", onClick = onNotifications),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.NotificationsNone, "Notifications", Modifier.size(26.dp), tint = p.ink)
        }
        Box(Modifier.size(48.dp).clickable(onClickLabel = "Your profile", onClick = onProfile),
            contentAlignment = Alignment.Center) {
            Surface(Modifier.size(36.dp), shape = CircleShape, color = Color(0xFFEEDAC7)) {
                Box(contentAlignment = Alignment.Center) {
                    if (profilePhoto != null) {
                        Image(profilePhoto, "Profile photo", Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop)
                    } else {
                        Text(profileInitial.uppercase(), color = p.ink,
                            fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun BrownHomeBottomBar(
    currentTab: MainTab,
    onSelected: (MainTab) -> Unit,
    onProfile: () -> Unit
) {
    val p = BrownHomePalette
    Surface(color = p.canvas, shadowElevation = 4.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Row(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            BrownNavTab(MainTab.HOME, currentTab, "Home", Icons.Rounded.Home,
                Modifier.weight(1f)) { onSelected(MainTab.HOME) }
            BrownNavTab(MainTab.ACTIVITY, currentTab, "Activity", Icons.Rounded.FormatListBulleted,
                Modifier.weight(1f)) { onSelected(MainTab.ACTIVITY) }
            Box(Modifier.weight(1.1f).fillMaxHeight().selectable(currentTab == MainTab.PLAN,
                role = Role.Tab, onClick = { onSelected(MainTab.PLAN) }),
                contentAlignment = Alignment.Center) {
                Column(Modifier.size(64.dp).clip(CircleShape).background(p.coffee),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Rounded.CalendarMonth, "Plan", Modifier.size(26.dp), tint = p.white)
                    Text("Plan", fontSize = 11.sp, color = p.white)
                }
            }
            BrownNavTab(MainTab.ACCOUNTS, currentTab, "Accounts", Icons.Rounded.AccountBalanceWallet,
                Modifier.weight(1f)) { onSelected(MainTab.ACCOUNTS) }
            BrownNavTab(MainTab.MORE, currentTab, "Profile", Icons.Rounded.PersonOutline,
                Modifier.weight(1f), onProfile)
        }
    }
}

@Composable
private fun BrownNavTab(
    tab: MainTab,
    current: MainTab,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val selected = current == tab
    val p = BrownHomePalette
    Column(modifier.fillMaxHeight().selectable(selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
            .background(if (selected) p.coffee else Color.Transparent),
            contentAlignment = Alignment.Center) {
            Icon(icon, label, Modifier.size(23.dp),
                tint = if (selected) p.white else p.ink)
        }
        Text(label, color = if (selected) p.coffee else p.ink, fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
