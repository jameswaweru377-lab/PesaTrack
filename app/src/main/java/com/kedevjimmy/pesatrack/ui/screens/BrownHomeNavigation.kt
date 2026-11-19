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
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens

/** Shared navigation palette, first approved on Home. */
internal object BrownHomePalette {
    val canvas = PremiumVisualTokens.cream
    val ink = PremiumVisualTokens.ink
    val coffee = PremiumVisualTokens.coffee
    val muted = PremiumVisualTokens.muted
    val cream = PremiumVisualTokens.sand
    val outline = PremiumVisualTokens.sandBorder
    val attention = Color(0xFF925B10)
    val white = Color.White
}

/** Dark variants reuse the same Home layout and ledger-backed values. */
internal data class HomeColors(
    val canvas: Color, val ink: Color, val coffee: Color, val muted: Color,
    val cream: Color, val outline: Color, val attention: Color, val white: Color
)

@Composable
internal fun homeColors(): HomeColors {
    val dark = MaterialTheme.colorScheme.background == PremiumVisualTokens.darkCanvas
    return if (dark) HomeColors(
        canvas = PremiumVisualTokens.darkCanvas,
        ink = PremiumVisualTokens.darkText,
        coffee = PremiumVisualTokens.darkCoffee,
        muted = PremiumVisualTokens.darkMuted,
        cream = PremiumVisualTokens.darkSurface,
        outline = PremiumVisualTokens.darkBorder,
        attention = Color(0xFFE6AD82),
        white = PremiumVisualTokens.darkCard
    ) else HomeColors(
        canvas = BrownHomePalette.canvas, ink = BrownHomePalette.ink,
        coffee = BrownHomePalette.coffee, muted = BrownHomePalette.muted,
        cream = BrownHomePalette.cream, outline = BrownHomePalette.outline,
        attention = BrownHomePalette.attention, white = BrownHomePalette.white
    )
}

@Composable
internal fun BrownHomeHeader(onNotifications: () -> Unit) {
    val p = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().height(76.dp).background(p.background)
        .padding(horizontal = 12.dp)) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(p.primaryContainer),
                contentAlignment = Alignment.Center) {
                Image(painter = painterResource(R.drawable.ic_pesatrack_home_mark),
                    contentDescription = null, modifier = Modifier.size(36.dp))
            }
            Text("PesaTrack", color = p.onBackground, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 23.sp)
            Text("YOUR MONEY, YOUR PLANS", color = p.primary, fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp, fontSize = 8.sp, lineHeight = 12.sp)
        }
        Box(Modifier.align(Alignment.CenterEnd).size(48.dp)
            .clickable(onClickLabel = "Notifications", onClick = onNotifications),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.NotificationsNone, "Notifications", Modifier.size(26.dp), tint = p.onBackground)
        }
    }
}

/** A title row shared by the pinned Plan/Accounts header and Activity's sticky tools. */
@Composable
internal fun BrownCompactTabTitle(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val palette = homeColors()
    Row(
        modifier.fillMaxWidth().heightIn(min = PesaDesignTokens.minimumTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PesaDesignTokens.sectionGap)
    ) {
        Image(
            painterResource(R.drawable.ic_pesatrack_home_mark), contentDescription = null,
            modifier = Modifier.size(PesaDesignTokens.compactWordmarkLogoSize)
                .clip(RoundedCornerShape(PesaDesignTokens.iconRadius))
        )
        Box(Modifier.width(1.dp).height(24.dp).background(palette.outline))
        Text(title, Modifier.weight(1f), color = palette.ink,
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        actions()
    }
}

@Composable
internal fun BrownHomeBottomBar(
    currentTab: MainTab,
    onSelected: (MainTab) -> Unit,
    onProfile: () -> Unit
) {
    val p = MaterialTheme.colorScheme
    Surface(color = p.background, shadowElevation = 4.dp,
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
    val p = MaterialTheme.colorScheme
    Column(modifier.fillMaxHeight().selectable(selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
            .background(if (selected) p.primary else Color.Transparent),
            contentAlignment = Alignment.Center) {
            Icon(icon, label, Modifier.size(23.dp),
                tint = if (selected) p.onPrimary else p.onBackground)
        }
        Text(label, color = if (selected) p.primary else p.onBackground, fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
