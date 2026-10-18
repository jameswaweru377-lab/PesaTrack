package com.kedevjimmy.pesatrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.graphics.luminance
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
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (currentTab != null) {
                Surface(
                    modifier = Modifier.statusBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PesaTrackTopBar(
                        onProfileClick = onProfileClick,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Row(
                    Modifier.fillMaxWidth().height(76.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    @Composable
                    fun androidx.compose.foundation.layout.RowScope.tabItem(tab: MainTab) {
                        val selected = currentTab == tab ||
                            (tab == MainTab.MORE && currentTab == MainTab.ACCOUNTS)
                        val tint = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        Column(
                            Modifier.weight(1f).fillMaxHeight().clickable {
                                onTabSelected(tab)
                            }.padding(vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp,
                                Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                tab.icon, contentDescription = tab.label,
                                modifier = Modifier.size(25.dp), tint = tint
                            )
                            Text(tab.label, color = tint,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    tabItem(MainTab.HOME)
                    tabItem(MainTab.ACTIVITY)
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .testTag("bottom-add-tool-button"),
                        contentAlignment = Alignment.Center
                    ) {
                        val isLight = MaterialTheme.colorScheme.background.luminance() > .5f
                        val addAccent = if (isLight) MaterialTheme.colorScheme.primary
                            else Color(0xFF52E5D3)
                        val addFill = if (isLight) addAccent else Color(0xFF06393A)
                        val addIcon = if (isLight) MaterialTheme.colorScheme.onPrimary
                            else Color(0xFF57F0DD)
                        Canvas(Modifier.requiredSize(72.dp)) {
                            drawCircle(addAccent.copy(alpha = .10f))
                        }
                        Surface(
                            onClick = { toolsOpen = true },
                            modifier = Modifier.requiredSize(58.dp),
                            shape = CircleShape,
                            color = addFill,
                            border = BorderStroke(2.dp, addAccent),
                            shadowElevation = 10.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = "Add & import tools",
                                    modifier = Modifier.size(33.dp),
                                    tint = addIcon
                                )
                            }
                        }
                    }
                    tabItem(MainTab.PLAN)
                    tabItem(MainTab.MORE)
                }
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
