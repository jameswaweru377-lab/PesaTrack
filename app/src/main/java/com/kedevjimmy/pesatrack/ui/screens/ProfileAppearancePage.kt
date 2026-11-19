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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Waves
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens
import com.kedevjimmy.pesatrack.ui.theme.ThemeMode
import com.kedevjimmy.pesatrack.ui.theme.ProfileVisualColors as C

@Composable
internal fun ProfileAppearancePage(padding: PaddingValues, back: () -> Unit,
    themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
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
        item { ProfileSection("Theme") }
        item {
            PesaCard {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    ThemeMode.entries.forEachIndexed { index, option ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                .clickable { onThemeModeChange(option) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = themeMode == option,
                                onClick = { onThemeModeChange(option) }
                            )
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(when (option) {
                                    ThemeMode.SYSTEM -> "Use phone setting"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                }, style = MaterialTheme.typography.titleSmall)
                                Text(when (option) {
                                    ThemeMode.SYSTEM -> "Changes with your phone"
                                    ThemeMode.LIGHT -> "Cream background"
                                    ThemeMode.DARK -> "Coffee-black background"
                                }, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (index != ThemeMode.entries.lastIndex) {
                            androidx.compose.material3.HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
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
