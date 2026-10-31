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
    EverydayPage("Appearance", padding, back) {
        item {
            PesaCard {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Palette, size = 58.dp,
                        background = pesaToneContainer("violet"),
                        tint = pesaToneContent("violet"))
                    Column(Modifier.padding(start = 16.dp)) {
                        Text("Make PesaTrack feel like yours",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("A clear, calm look for your money.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { SectionHeader("App preview") }
        item {
            PesaCard {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("PesaTrack", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Surface(shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("Available after commitments",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Your money, your plans.",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    val tabs = listOf(
                        Triple("Home", Icons.Rounded.Home, true),
                        Triple("Activity", Icons.Rounded.List, false),
                        Triple("Plan", Icons.Rounded.PieChart, false),
                        Triple("More", Icons.Rounded.MoreHoriz, false)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        tabs.forEach { (name, icon, selected) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp),
                                    tint = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(name, style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item { SectionHeader("Theme") }
        item {
            PesaCard {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.WbSunny, size = 48.dp,
                        background = pesaToneContainer("violet"),
                        tint = pesaToneContent("violet"))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Light", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Bright and easy to read",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Light theme selected",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { SectionHeader("Reading") }
        item { EverydayRow(Icons.Rounded.TextFields, "Text size",
            "Follow my phone · Change in display settings") {
            openPhoneSettings(Settings.ACTION_DISPLAY_SETTINGS)
        } }
        item { EverydayRow(Icons.Rounded.Accessibility, "Reduce animations",
            "Manage on your phone, if available") {
            openPhoneSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        } }
        if (error != null) item {
            Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
        }
    }
}
