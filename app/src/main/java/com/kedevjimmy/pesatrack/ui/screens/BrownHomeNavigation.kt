package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.ui.components.MainTab

/** Shared navigation palette, first approved on Home. */
internal object BrownHomePalette {
    val canvas = Color(0xFFFCF8F4)
    val ink = Color(0xFF201610)
    val coffee = Color(0xFF652F10)
    val muted = Color(0xFF766C64)
    val cream = Color(0xFFF8EBDE)
    val outline = Color(0xFFE3BE92)
    val attention = Color(0xFF925B10)
    val white = Color.White
}

@Composable
internal fun BrownHomeDrawerContent(
    currentTab: MainTab?, onTab: (MainTab) -> Unit, onProfile: () -> Unit
) {
    ModalDrawerSheet(drawerContainerColor = BrownHomePalette.canvas) {
        Text("PesaTrack", Modifier.padding(24.dp),
            style = MaterialTheme.typography.headlineSmall, color = BrownHomePalette.coffee)
        MainTab.entries.forEach { tab ->
            NavigationDrawerItem(label = { Text(tab.label) }, selected = tab == currentTab,
                icon = { Icon(tab.icon, contentDescription = null) },
                onClick = { onTab(tab) }, modifier = Modifier.padding(horizontal = 12.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = BrownHomePalette.cream,
                    selectedTextColor = BrownHomePalette.coffee,
                    selectedIconColor = BrownHomePalette.coffee))
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        NavigationDrawerItem(label = { Text("Profile") }, selected = false,
            icon = { Icon(Icons.Rounded.PersonOutline, null) },
            onClick = onProfile, modifier = Modifier.padding(horizontal = 12.dp))
    }
}

@Composable
internal fun BrownHomeHeader(
    onMenu: () -> Unit,
    onNotifications: () -> Unit
) {
    val p = BrownHomePalette
    Box(Modifier.fillMaxWidth().height(76.dp).background(p.canvas)
        .padding(horizontal = 12.dp)) {
        Box(Modifier.align(Alignment.CenterStart).size(48.dp)
            .clickable(onClickLabel = "Open More", onClick = onMenu),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Menu, "More menu", Modifier.size(28.dp), tint = p.ink)
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(p.cream),
                contentAlignment = Alignment.Center) {
                Image(painter = painterResource(R.drawable.ic_pesatrack_home_mark),
                    contentDescription = null, modifier = Modifier.size(36.dp))
            }
            Text("PesaTrack", color = p.ink, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 23.sp)
            Text("YOUR MONEY, YOUR PLANS", color = p.coffee, fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp, fontSize = 8.sp, lineHeight = 12.sp)
        }
        Box(Modifier.align(Alignment.CenterEnd).size(48.dp)
            .clickable(onClickLabel = "Notifications", onClick = onNotifications),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.NotificationsNone, "Notifications", Modifier.size(26.dp), tint = p.ink)
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
            BrownNavTab(MainTab.PLAN, currentTab, "Plan", Icons.Rounded.CalendarMonth,
                Modifier.weight(1f)) { onSelected(MainTab.PLAN) }
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
