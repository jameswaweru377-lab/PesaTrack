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
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { ProfileCoverHeader(name, avatar, back, personal) }
        item { ProfileQuickActions(personal, accounts, payCycle, goals, privacy) }
        item { ProfileShortcutRow(personal, accounts, payCycle, goals) }
        item {
            Box(Modifier.padding(horizontal = 16.dp)) {
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
    val title: String,
    val subtitle: String,
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
        Triple(Icons.Rounded.PersonOutline, "Personal information", personal),
        Triple(Icons.Rounded.AccountBalance, "My accounts", accounts),
        Triple(Icons.Rounded.CalendarMonth, "Pay cycle", payCycle),
        Triple(Icons.Rounded.Flag, "Your goals", goals),
        Triple(Icons.Rounded.Shield, "Privacy", privacy)
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        actions.forEach { (icon, label, action) ->
            Box(Modifier.size(48.dp).clickable(role = Role.Button, onClick = action),
                contentAlignment = Alignment.Center) {
                Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = C.sand) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, label, tint = C.coffee, modifier = Modifier.size(24.dp))
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
        ProfileShortcut("Personal info", "Name & photo", Icons.Rounded.PersonOutline, personal),
        ProfileShortcut("My accounts", "Linked balances", Icons.Rounded.AccountBalance, accounts),
        ProfileShortcut("Pay cycle", "Salary dates", Icons.Rounded.CalendarMonth, payCycle),
        ProfileShortcut("Your goals", "Plans ahead", Icons.Rounded.Flag, goals)
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        shortcuts.forEach { shortcut ->
            Surface(
                modifier = Modifier.weight(1f).heightIn(min = 72.dp)
                    .clickable(role = Role.Button, onClick = shortcut.action),
                shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
                color = C.card,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, C.sandBorder)
            ) {
                Column(Modifier.fillMaxWidth().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(shortcut.icon, null, tint = C.coffee, modifier = Modifier.size(20.dp))
                    Text(shortcut.title, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = C.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(shortcut.subtitle, modifier = Modifier.weight(1f), fontSize = 9.sp,
                            color = C.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Rounded.ChevronRight, null, tint = C.coffee,
                            modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileNairobiWellnessFeature(
    state: ProfileMoneyHabitsPresentation,
    onClick: () -> Unit
) {
    Box(
        Modifier.fillMaxWidth().height(164.dp)
            .clip(RoundedCornerShape(PesaDesignTokens.cardRadius))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { stateDescription = state.label }
    ) {
        Image(
            painterResource(R.drawable.profile_money_wellness_nairobi),
            contentDescription = "Nairobi city at sunset",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(
            Color.Black.copy(alpha = .87f), Color.Black.copy(alpha = .46f),
            Color.Transparent
        ))))
        Column(
            Modifier.fillMaxHeight().fillMaxWidth(.82f).padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(shape = CircleShape, color = C.coffee.copy(alpha = .80f)) {
                    Text("MONEY WELLNESS", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = C.cream, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text("Better\nmoney habits", color = Color.White, fontSize = 25.sp,
                    fontFamily = FontFamily.Serif, lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold)
                Text("Make sense of your money", color = Color.White.copy(alpha = .94f),
                    fontSize = 11.sp, maxLines = 1)
                Box(Modifier.width(48.dp).height(2.dp).background(C.leaf, CircleShape))
            }
            Surface(
                shape = CircleShape,
                color = C.cream
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
            Icon(Icons.Rounded.Shield, null, tint = C.coffee, modifier = Modifier.size(32.dp))
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
