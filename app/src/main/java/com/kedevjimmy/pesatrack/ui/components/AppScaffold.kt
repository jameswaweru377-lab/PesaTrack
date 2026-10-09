package com.kedevjimmy.pesatrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Rounded.Home),
    ACTIVITY("activity", "Activity", Icons.AutoMirrored.Rounded.ReceiptLong),
    PLAN("plan", "Plan", Icons.Rounded.CalendarMonth),
    ACCOUNTS("accounts", "Accounts", Icons.Rounded.AccountBalanceWallet),
    MORE("more", "More", Icons.Rounded.MoreHoriz)
}

@Composable
fun AppScaffold(currentTab: MainTab?, onTabSelected: (MainTab) -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth().selectableGroup().padding(6.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            androidx.compose.foundation.layout.Column(
                                Modifier.weight(1f)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                                    .selectable(selected = selected, role = androidx.compose.ui.semantics.Role.Tab, onClick = { onTabSelected(tab) })
                                    .heightIn(min = 56.dp).padding(horizontal = 3.dp, vertical = 8.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp, androidx.compose.ui.Alignment.CenterVertically)
                            ) {
                                val foreground = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                Icon(tab.icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = foreground)
                                Text(tab.label, color = foreground, style = MaterialTheme.typography.labelSmall,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }, content = content
    )
}
