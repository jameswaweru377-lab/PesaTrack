package com.kedevjimmy.pesatrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.ui.screens.ActivityMoneyActions

enum class MainTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    HOME("home", "Home", Icons.Rounded.Home),
    ACTIVITY("activity", "Activity", Icons.AutoMirrored.Rounded.ReceiptLong),
    PLAN("plan", "Plan", Icons.Rounded.CalendarMonth),
    ACCOUNTS("accounts", "Accounts", Icons.Rounded.AccountBalanceWallet),
    MORE("more", "More", Icons.Rounded.MoreHoriz)
}

/**
 * Primary navigation deliberately keeps the Accounts destination addressable,
 * but moves its entry point into More and Profile. The center action is a real
 * tools sheet, never a dead-end FAB. Top bar belongs to the scaffold, not a
 * scrolling screen, so it stays anchored during long Activity lists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    currentTab: MainTab?,
    onTabSelected: (MainTab) -> Unit,
    onProfileClick: () -> Unit = {},
    onPaycheckClick: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    var toolsOpen by remember { mutableStateOf(false) }
    val mainTabs = listOf(MainTab.HOME, MainTab.ACTIVITY, MainTab.PLAN, MainTab.MORE)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (currentTab != null) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    PesaTrackTopBar(
                        onProfileClick = onProfileClick,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(78.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                @Composable
                fun androidx.compose.foundation.layout.RowScope.tabItem(tab: MainTab) {
                    val selected = currentTab == tab ||
                        (tab == MainTab.MORE && currentTab == MainTab.ACCOUNTS)
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            Icon(tab.icon, contentDescription = tab.label,
                                modifier = Modifier.size(22.dp))
                        },
                        label = {
                            Text(tab.label, style = MaterialTheme.typography.labelSmall)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                tabItem(MainTab.HOME)
                tabItem(MainTab.ACTIVITY)
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Surface(
                        onClick = { toolsOpen = true },
                        modifier = Modifier.size(58.dp),
                        shape = CircleShape,
                        color = Color(0xFF06393A),
                        border = BorderStroke(2.dp, Color(0xFF47E7D2)),
                        shadowElevation = 7.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add & import tools",
                                modifier = Modifier.size(33.dp), tint = Color(0xFF57F0DD))
                        }
                    }
                }
                tabItem(MainTab.PLAN)
                tabItem(MainTab.MORE)
            }
        },
        content = content
    )
    if (toolsOpen) {
        ModalBottomSheet(onDismissRequest = { toolsOpen = false }) {
            Column(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Add & tools", style = MaterialTheme.typography.titleLarge)
                Text("Add entries, import statements or review your paychecks.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
                ActivityMoneyActions(
                    onChanged = {
                        LedgerUpdateBus.notifyChanged()
                        toolsOpen = false
                    },
                    onPaycheckClick = {
                        toolsOpen = false
                        onPaycheckClick()
                    }
                )
            }
        }
    }
}
