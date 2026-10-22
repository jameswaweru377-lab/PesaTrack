package com.kedevjimmy.pesatrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.ui.screens.ActivityMoneyActions
import com.kedevjimmy.pesatrack.ui.screens.ReferenceHomeBottomBar
import com.kedevjimmy.pesatrack.ui.screens.ReferenceHomeHeader
import com.kedevjimmy.pesatrack.ui.screens.HomeReferencePalette
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

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
    onNotificationsClick: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    var toolsOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val profileInitial = remember(context) {
        context.getSharedPreferences("pesatrack_preferences", android.content.Context.MODE_PRIVATE)
            .getString("profile_name", "J").orEmpty().trim().take(1).ifBlank { "J" }
    }
    Scaffold(
        containerColor = if (currentTab == MainTab.HOME || currentTab == MainTab.ACTIVITY) HomeReferencePalette.canvas
            else MaterialTheme.colorScheme.background,
        topBar = {
            if (currentTab == MainTab.HOME || currentTab == MainTab.ACTIVITY) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFF0EBFF),
                                    Color(0xFFF4F2FF),
                                    Color(0xFFE8F3FF)
                                )
                            )
                        )
                        .statusBarsPadding()
                ) {
                    ReferenceHomeHeader(
                        onNotifications = onNotificationsClick,
                        onProfile = onProfileClick,
                        profileInitial = profileInitial
                    )
                }
            } else if (currentTab != null) {
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
            if (currentTab == MainTab.HOME || currentTab == MainTab.ACTIVITY) {
                Surface(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    color = Color.White
                ) {
                    ReferenceHomeBottomBar(currentTab, onTabSelected)
                }
            } else Surface(
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
                        val addAccent = MaterialTheme.colorScheme.primary
                        val addFill = addAccent
                        val addIcon = MaterialTheme.colorScheme.onPrimary
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
