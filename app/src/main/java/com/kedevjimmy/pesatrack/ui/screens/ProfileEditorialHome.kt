package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import com.kedevjimmy.pesatrack.ui.theme.ProfileVisualColors as C

@Composable
internal fun ProfileEditorialHome(
    padding: PaddingValues,
    back: () -> Unit,
    name: String,
    avatar: String,
    city: String,
    bio: String,
    moneyHabits: ProfileMoneyHabitsPresentation,
    personal: () -> Unit,
    accounts: () -> Unit,
    payCycle: () -> Unit,
    goals: () -> Unit,
    privacy: () -> Unit,
    onMoneyHabits: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(C.cream).padding(padding)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item { ProfileCoverHeader(name, avatar, back, personal) }
            item { ProfileIdentityDetails(name, city, bio, onEdit = personal) }
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
        }
        ProfileEditorialFooter(Modifier.align(Alignment.BottomCenter))
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
            Surface(
                modifier = Modifier.size(48.dp).clickable(role = Role.Button, onClick = action),
                shape = CircleShape,
                color = C.sand
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, label, tint = C.coffee, modifier = Modifier.size(24.dp))
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
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(shortcuts, key = { it.title }) { shortcut ->
            Surface(
                modifier = Modifier.width(148.dp).heightIn(min = 112.dp)
                    .clickable(role = Role.Button, onClick = shortcut.action),
                shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
                color = C.cream,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, C.sandBorder)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(shortcut.icon, null, tint = C.coffee, modifier = Modifier.size(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(shortcut.title, modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold, color = C.ink)
                        Icon(Icons.Rounded.ChevronRight, null, tint = C.coffee,
                            modifier = Modifier.size(16.dp))
                    }
                    Text(shortcut.subtitle, style = MaterialTheme.typography.bodySmall,
                        color = C.muted, maxLines = 2)
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
        Modifier.fillMaxWidth().height(216.dp)
            .clip(RoundedCornerShape(PesaDesignTokens.cardRadius))
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Image(
            painterResource(R.drawable.profile_money_wellness_nairobi),
            contentDescription = "Nairobi city at sunset",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(
            Color.Black.copy(alpha = .82f), Color.Black.copy(alpha = .55f),
            Color.Transparent
        ))))
        Column(
            Modifier.fillMaxHeight().fillMaxWidth(.84f).padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MONEY WELLNESS", color = C.sand, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium)
                Text("Better money habits", color = Color.White,
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(state.label, color = Color.White.copy(alpha = .94f),
                    style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            Surface(
                shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
                color = C.cream
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.BarChart, null, tint = C.coffee,
                        modifier = Modifier.size(16.dp))
                    Text("Review insights", modifier = Modifier.padding(horizontal = 8.dp),
                        color = C.ink, style = MaterialTheme.typography.labelLarge,
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
        shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
        color = C.sand
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.Shield, null, tint = C.coffee, modifier = Modifier.size(32.dp))
            Column(Modifier.weight(1f)) {
                Text("Only on this device", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = C.ink)
                Text("Your personal details stay on this phone",
                    style = MaterialTheme.typography.bodySmall, color = C.muted)
            }
            TextButton(onClick = onClick, colors = ButtonDefaults.textButtonColors(
                contentColor = C.coffee)) {
                Text("Privacy")
            }
        }
    }
}

@Composable
private fun ProfileEditorialFooter(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().background(C.cream).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Text("PesaTrack", color = C.coffee, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold)
        Text(" • Your money, your plans", color = C.muted,
            style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}
