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
import com.kedevjimmy.pesatrack.data.profile.ProfilePhotoStore
import com.kedevjimmy.pesatrack.ui.screens.ActivityMoneyActions
import com.kedevjimmy.pesatrack.ui.screens.ReferenceHomeBottomBar
import com.kedevjimmy.pesatrack.ui.screens.ReferenceHomeHeader
import com.kedevjimmy.pesatrack.ui.screens.BrownHomeHeader
import com.kedevjimmy.pesatrack.ui.screens.BrownHomeBottomBar
import com.kedevjimmy.pesatrack.ui.screens.BrownHomePalette
import com.kedevjimmy.pesatrack.ui.screens.HomeReferencePalette
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap

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
 * Primary navigation includes Home, Activity, Plan, Accounts and More as
 * shown by the approved premium design. The anchored header and bottom bar
 * are owned by this scaffold, not by each scrolling destination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    currentTab: MainTab?,
    onTabSelected: (MainTab) -> Unit,
    onProfileClick: () -> Unit = {},
    onPaycheckClick: () -> Unit = {},
    onStatementsClick: (() -> Unit)? = null,
    onNotificationsClick: () -> Unit = {},
    showTopBar: Boolean = true,
    useHomeHeader: Boolean = false,
    useBrownHome: Boolean = false,
    content: @Composable (PaddingValues) -> Unit
) {
    var toolsOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val profileInitial = remember(context) {
        context.getSharedPreferences("pesatrack_preferences", android.content.Context.MODE_PRIVATE)
            .getString("profile_name", "J").orEmpty().trim().take(1).ifBlank { "J" }
    }
    // Reload when returning to Home from another tab after changing the profile photo.
    val profilePhoto = remember(context, currentTab) {
        ProfilePhotoStore(context).load()?.asImageBitmap()
    }
    Scaffold(
        containerColor = if (useBrownHome) BrownHomePalette.canvas
            else if (currentTab == MainTab.ACTIVITY || useHomeHeader) HomeReferencePalette.canvas
            else MaterialTheme.colorScheme.background,
        topBar = {
            if (showTopBar && (currentTab == MainTab.HOME || currentTab == MainTab.ACTIVITY || useHomeHeader)) {
                Box(
                    modifier = Modifier
                        .background(if (useBrownHome) BrownHomePalette.canvas
                            else HomeReferencePalette.canvas)
                        .statusBarsPadding()
                ) {
                    if (useBrownHome) {
                        BrownHomeHeader(
                            onMenu = { onTabSelected(MainTab.MORE) },
                            onNotifications = onNotificationsClick
                        )
                    } else {
                        ReferenceHomeHeader(
                            onNotifications = onNotificationsClick,
                            onProfile = onProfileClick,
                            profileInitial = profileInitial,
                            profilePhoto = profilePhoto
                        )
                    }
                }
            } else if (showTopBar && currentTab != null) {
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
            if (currentTab != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    color = if (useBrownHome) BrownHomePalette.canvas
                        else HomeReferencePalette.canvas
                ) {
                    if (useBrownHome) {
                        BrownHomeBottomBar(currentTab, onTabSelected, onProfileClick)
                    } else {
                        ReferenceHomeBottomBar(currentTab, onTabSelected)
                    }
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
