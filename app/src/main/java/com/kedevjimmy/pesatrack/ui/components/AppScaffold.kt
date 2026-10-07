package com.kedevjimmy.pesatrack.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
    ACCOUNTS("accounts", "Accounts", Icons.Rounded.AccountBalanceWallet),
    ACTIVITY("activity", "Transactions", Icons.AutoMirrored.Rounded.ReceiptLong),
    PLAN("plan", "Plan", Icons.Rounded.CalendarMonth),
    MORE("more", "More", Icons.Rounded.MoreHoriz)
}

@Composable
fun AppScaffold(
    currentTab: MainTab?,
    onTabSelected: (MainTab) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor =
                    MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                MainTab.entries.forEach { tab ->
                    val selected =
                        currentTab == tab
                    val centerAction =
                        tab == MainTab.ACTIVITY

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            onTabSelected(tab)
                        },
                        icon = {
                            if (centerAction) {
                                Surface(
                                    modifier =
                                        Modifier
                                            .offset(
                                                y =
                                                    if (
                                                        selected
                                                    ) {
                                                        (-6).dp
                                                    } else {
                                                        (-3).dp
                                                    }
                                            )
                                            .size(
                                                if (
                                                    selected
                                                ) {
                                                    50.dp
                                                } else {
                                                    44.dp
                                                }
                                            ),
                                    shape =
                                        CircleShape,
                                    color =
                                        if (selected) {
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .primaryContainer
                                        },
                                    shadowElevation =
                                        if (selected) {
                                            8.dp
                                        } else {
                                            2.dp
                                        }
                                ) {
                                    Box(
                                        contentAlignment =
                                            Alignment.Center
                                    ) {
                                        Icon(
                                            tab.icon,
                                            contentDescription =
                                                tab.label,
                                            modifier =
                                                Modifier.size(
                                                    if (
                                                        selected
                                                    ) {
                                                        24.dp
                                                    } else {
                                                        21.dp
                                                    }
                                                ),
                                            tint =
                                                if (selected) {
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onPrimary
                                                } else {
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onPrimaryContainer
                                                }
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    tab.icon,
                                    contentDescription =
                                        tab.label,
                                    modifier =
                                        Modifier.size(
                                            21.dp
                                        ),
                                    tint =
                                        if (selected) {
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                        }
                                )
                            }
                        },
                        label = {
                            Text(
                                tab.label,
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelSmall
                            )
                        },
                        colors =
                            NavigationBarItemDefaults
                                .colors(
                                    selectedIconColor =
                                        MaterialTheme
                                            .colorScheme
                                            .primary,
                                    selectedTextColor =
                                        MaterialTheme
                                            .colorScheme
                                            .primary,
                                    indicatorColor =
                                        Color.Transparent,
                                    unselectedIconColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant,
                                    unselectedTextColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )
                    )
                }
            }
        },
        content = content
    )
}
