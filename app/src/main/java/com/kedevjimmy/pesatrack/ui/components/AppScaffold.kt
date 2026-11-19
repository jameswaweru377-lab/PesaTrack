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
import com.kedevjimmy.pesatrack.ui.screens.BrownHomeHeader
import com.kedevjimmy.pesatrack.ui.screens.BrownCompactTabTitle
import com.kedevjimmy.pesatrack.ui.screens.BrownHomeBottomBar
import com.kedevjimmy.pesatrack.ui.screens.BrownHomePalette
import com.kedevjimmy.pesatrack.ui.screens.HomeReferencePalette
import kotlinx.coroutines.launch

/** All tabs share one route for the notification bell. */
val LocalAppNotificationsDestination = compositionLocalOf<(() -> Unit)?> { null }

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

/** Only Home keeps the centered full brand; Activity owns its own sticky header. */
internal fun showBrandedTopHeader(currentTab: MainTab?, showTopBar: Boolean): Boolean =
    showTopBar && currentTab == MainTab.HOME

/** Plan and Accounts use a pinned mark-and-title header without a search control. */
internal fun showCompactTopHeader(currentTab: MainTab?, showTopBar: Boolean): Boolean =
    showTopBar && (currentTab == MainTab.PLAN || currentTab == MainTab.ACCOUNTS)

/**
 * Primary navigation uses Home, Activity, Plan, Accounts and Profile.
 * Home owns the full branded header; Plan and Accounts use compact fixed titles.
 * Activity owns a sticky ledger header and inline search.
 * The shared five-tab bottom navigation remains anchored by this scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    currentTab: MainTab?,
    onTabSelected: (MainTab) -> Unit,
    onProfileClick: () -> Unit = {},
    onPaycheckClick: () -> Unit = {},
    onStatementsClick: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
    showTopBar: Boolean = true,
    useHomeHeader: Boolean = false,
    useBrownHome: Boolean = false,
    content: @Composable (PaddingValues) -> Unit
) {
    var toolsOpen by remember { mutableStateOf(false) }
    val sharedNotifications = LocalAppNotificationsDestination.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (showBrandedTopHeader(currentTab, showTopBar)) {
                Box(Modifier.background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
                    BrownHomeHeader(
                        onNotifications = {
                            when {
                                onNotificationsClick != null -> onNotificationsClick()
                                sharedNotifications != null -> sharedNotifications()
                                else -> onTabSelected(MainTab.ACTIVITY)
                            }
                        }
                    )
                }
            } else if (showCompactTopHeader(currentTab, showTopBar)) {
                Box(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    currentTab?.let { BrownCompactTabTitle(it.label) }
                }
            }
        },
        bottomBar = {
            if (currentTab != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BrownHomeBottomBar(currentTab, onTabSelected, onProfileClick)
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
                    },
                    onStatementsClick = onStatementsClick?.let { goToStatements ->
                        { toolsOpen = false; goToStatements() }
                    }
                )
            }
        }
    }
}
