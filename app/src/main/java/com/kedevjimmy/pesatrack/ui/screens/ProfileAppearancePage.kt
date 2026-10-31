package com.kedevjimmy.pesatrack.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import com.kedevjimmy.pesatrack.ui.theme.ProfileVisualColors as C

@Composable
internal fun ProfileAppearancePage(padding: PaddingValues, back: () -> Unit) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    fun openPhoneSettings(action: String) {
        try {
            context.startActivity(Intent(action))
            error = null
        } catch (failure: ActivityNotFoundException) {
            error = "Your phone couldn't open that setting."
        } catch (failure: SecurityException) {
            error = "Your phone doesn't allow that setting to open here."
        }
    }
    ProfilePremiumPage("Appearance", padding, back) {
        item {
            ProfileBanner("Make PesaTrack feel like yours",
                "Choose how the app looks and feels on your device.", Icons.Rounded.Palette,
                art = ProfileHeroArt.PALETTE)
        }
        item {
            PesaCard {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("App preview", color = C.muted, style = MaterialTheme.typography.bodyMedium)
                    Surface(color = C.lilac, shape = RoundedCornerShape(16.dp)) {
                      Column(Modifier.fillMaxWidth().padding(24.dp),
                          verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = Color.White, shape = RoundedCornerShape(20.dp), shadowElevation = 6.dp) {
                          Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                com.kedevjimmy.pesatrack.ui.components.PesaLogoMark(size = 28.dp)
                                Text("PesaTrack", Modifier.weight(1f).padding(start = 8.dp),
                                    color = C.ink, fontWeight = FontWeight.Bold)
                                Icon(Icons.Rounded.Notifications, null, tint = C.ink)
                            }
                            Box(Modifier.fillMaxWidth().height(86.dp).clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(listOf(C.violetBright, C.violetDeep)))) {
                                Row(Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.AccountBalanceWallet, null, tint = Color.White,
                                        modifier = Modifier.size(32.dp))
                                    Column(Modifier.padding(start = 12.dp)) {
                                        Text("Your money", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("A clearer picture", color = Color.White.copy(alpha = .85f))
                                    }
                                }
                            }
                            val tabs = listOf(
                                Triple("Home", Icons.Rounded.Home, ProfileTone.VIOLET),
                                Triple("Activity", Icons.Rounded.List, ProfileTone.GREEN),
                                Triple("Plan", Icons.Rounded.PieChart, ProfileTone.PINK),
                                Triple("More", Icons.Rounded.MoreHoriz, ProfileTone.BLUE)
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                tabs.forEach { (name, icon, tone) ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        ProfileTile(icon, tone, 36.dp)
                                        Text(name, color = C.muted, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                      }
                    }
                }
            }
        }
        item { ProfileSection("Theme") }
        item {
            ProfilePremiumGroup(listOf(ProfilePremiumItem(Icons.Rounded.WbSunny, "Light",
                "Bright and easy to read", ProfileTone.VIOLET, selected = true)))
        }
        item { ProfileSection("Reading") }
        item { ProfilePremiumGroup(listOf(
            ProfilePremiumItem(Icons.Rounded.TextFields, "Text size", "Follow my phone",
                ProfileTone.BLUE, onClick = { openPhoneSettings(Settings.ACTION_DISPLAY_SETTINGS) }),
            ProfilePremiumItem(Icons.Rounded.Waves, "Reduce movement", "Make screens feel calmer",
                ProfileTone.GREEN, onClick = { openPhoneSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) })
        )) }
        if (error != null) item {
            Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
        }
    }
}
