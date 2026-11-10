package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import com.kedevjimmy.pesatrack.ui.theme.ProfileVisualColors as C

@Composable
internal fun ProfileEditorialHome(
    padding: PaddingValues,
    back: () -> Unit,
    name: String,
    avatar: String,
    moneyHabits: ProfileMoneyHabitsPresentation,
    personal: () -> Unit,
    accounts: () -> Unit,
    payCycle: () -> Unit,
    goals: () -> Unit,
    privacy: () -> Unit,
    onMoneyHabits: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(C.cream).padding(padding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { ProfileCoverHeader(name, avatar, back, personal) }
        item { ProfileQuickActions(personal, accounts, payCycle, goals, privacy) }
        item { ProfileShortcutRow(personal, accounts, payCycle, goals) }
        item {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                ProfileNairobiWellnessFeature(moneyHabits, onMoneyHabits)
            }
        }
        item {
            Box(Modifier.padding(horizontal = 16.dp)) {
                ProfileDevicePrivacy(privacy)
            }
        }
        item { ProfileEditorialFooter() }
    }
}

private data class ProfileShortcut(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val action: () -> Unit
)

private data class ProfileQuickAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val action: () -> Unit
)

@Composable
private fun ProfileQuickActions(
    personal: () -> Unit,
    accounts: () -> Unit,
    payCycle: () -> Unit,
    goals: () -> Unit,
    privacy: () -> Unit
) {
    val actions = listOf(
        ProfileQuickAction("personal", "Personal information", Icons.Outlined.PersonOutline, personal),
        ProfileQuickAction("accounts", "My accounts", Icons.Outlined.AccountBalance, accounts),
        ProfileQuickAction("pay_cycle", "Pay cycle", Icons.Outlined.CalendarMonth, payCycle),
        ProfileQuickAction("goals", "Your goals", Icons.Outlined.TrackChanges, goals),
        ProfileQuickAction("privacy", "Privacy", Icons.Outlined.VerifiedUser, privacy)
    )
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(Modifier.widthIn(max = 264.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween) {
            actions.forEach { item ->
                Box(Modifier.size(48.dp).testTag("profile_quick_${item.id}")
                    .clickable(role = Role.Button, onClick = item.action),
                    contentAlignment = Alignment.Center) {
                    Surface(modifier = Modifier.size(32.dp), shape = CircleShape,
                        color = C.card.copy(alpha = .68f)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(item.icon, item.label,
                                tint = C.coffee.copy(alpha = .84f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileShortcutRow(
    personal: () -> Unit,
    accounts: () -> Unit,
    payCycle: () -> Unit,
    goals: () -> Unit
) {
    val shortcuts = listOf(
        ProfileShortcut("personal", "Personal info", "Name & photo", Icons.Outlined.PersonOutline, personal),
        ProfileShortcut("accounts", "My accounts", "Linked balances", Icons.Outlined.AccountBalance, accounts),
        ProfileShortcut("pay_cycle", "Pay cycle", "Salary dates", Icons.Outlined.CalendarMonth, payCycle),
        ProfileShortcut("goals", "Your goals", "Plans ahead", Icons.Outlined.TrackChanges, goals)
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        shortcuts.forEach { shortcut ->
            Surface(
                modifier = Modifier.weight(1f).heightIn(min = 60.dp)
                    .testTag("profile_shortcut_${shortcut.id}")
                    .clickable(role = Role.Button, onClick = shortcut.action),
                shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
                color = C.card,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, C.sandBorder)
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(shortcut.icon, null, tint = C.coffee, modifier = Modifier.size(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(shortcut.title, modifier = Modifier.weight(1f), fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, color = C.ink,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Rounded.ChevronRight, null, tint = C.coffee,
                            modifier = Modifier.size(12.dp))
                    }
                    Text(shortcut.subtitle, fontSize = 9.sp, color = C.muted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
internal fun ProfileNairobiWellnessFeature(
    state: ProfileMoneyHabitsPresentation,
    onClick: () -> Unit
) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 144.dp)
            .clip(RoundedCornerShape(PesaDesignTokens.cardRadius))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { stateDescription = state.label }
    ) {
        Image(
            painterResource(R.drawable.profile_money_wellness_nairobi),
            contentDescription = "Nairobi city at sunset",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(
            Color.Black.copy(alpha = .87f), Color.Black.copy(alpha = .46f),
            Color.Transparent
        ))))
        Column(
            Modifier.fillMaxWidth(.82f).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Surface(shape = CircleShape, color = C.coffee.copy(alpha = .80f)) {
                    Text("MONEY WELLNESS", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = C.cream, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text("Better\nmoney habits", color = Color.White, fontSize = 22.sp,
                    fontFamily = FontFamily.Serif, lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold)
                Text("Make sense of your money", color = Color.White.copy(alpha = .94f),
                    fontSize = 11.sp, maxLines = 1)
                Row(Modifier.width(160.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(2) {
                        Box(Modifier.width(28.dp).height(2.dp)
                            .background(C.leaf.copy(alpha = .80f), CircleShape))
                    }
                }
            }
            Surface(
                shape = CircleShape,
                color = C.cream
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.BarChart, null, tint = C.coffee,
                        modifier = Modifier.size(16.dp))
                    Text("Review insights", modifier = Modifier.padding(horizontal = 8.dp),
                        color = C.ink, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold)
                    Icon(Icons.Rounded.ChevronRight, null, tint = C.coffee,
                        modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileDevicePrivacy(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
        color = C.sand
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.VerifiedUser, null, tint = C.coffee,
                modifier = Modifier.size(28.dp))
            Box(Modifier.width(1.dp).height(32.dp)
                .background(C.coffee.copy(alpha = .28f)))
            Column(Modifier.weight(1f)) {
                Text("Only on this device", fontSize = 16.sp,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = C.ink)
                Text("Your personal details stay on this phone",
                    fontSize = 11.sp, color = C.muted)
            }
            Button(onClick = onClick, shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = C.ink,
                    contentColor = Color.White), contentPadding = PaddingValues(horizontal = 12.dp)) {
                Text("Privacy", fontSize = 12.sp)
                Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ProfileEditorialFooter(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().background(C.cream).padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(Modifier.weight(1f), color = C.sandBorder)
        Text("PesaTrack", modifier = Modifier.padding(start = 12.dp),
            color = C.coffee, fontSize = 12.sp,
            fontWeight = FontWeight.Bold)
        Text(" • Your money, your plans", color = C.muted, fontSize = 11.sp,
            textAlign = TextAlign.Center)
        HorizontalDivider(Modifier.weight(1f).padding(start = 12.dp), color = C.sandBorder)
    }
}
