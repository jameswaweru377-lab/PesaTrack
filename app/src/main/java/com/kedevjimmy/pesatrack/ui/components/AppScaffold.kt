package com.kedevjimmy.pesatrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME(
        "home",
        "Home",
        Icons.Rounded.Home
    ),
    ACCOUNTS(
        "accounts",
        "Accounts",
        Icons.Rounded.AccountBalanceWallet
    ),
    ACTIVITY(
        "activity",
        "Transactions",
        Icons.AutoMirrored.Rounded.ReceiptLong
    ),
    PLAN(
        "plan",
        "Plan",
        Icons.Rounded.CalendarMonth
    ),
    MORE(
        "more",
        "More",
        Icons.Rounded.MoreHoriz
    )
}

@Composable
fun AppScaffold(
    currentTab: MainTab?,
    onTabSelected: (MainTab) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor =
            MaterialTheme.colorScheme.background,
        bottomBar = {
            PesaBottomNavigation(
                currentTab =
                    currentTab,
                onTabSelected =
                    onTabSelected
            )
        },
        content = content
    )
}

@Composable
private fun PesaBottomNavigation(
    currentTab: MainTab?,
    onTabSelected: (MainTab) -> Unit
) {
    val dark =
        isPesaDarkTheme()
    val barColor =
        if (dark) {
            MaterialTheme
                .colorScheme
                .primary
        } else {
            MaterialTheme
                .colorScheme
                .surface
        }
    val barContent =
        if (dark) {
            MaterialTheme
                .colorScheme
                .onPrimary
        } else {
            MaterialTheme
                .colorScheme
                .onSurface
        }
    val centerFill =
        if (dark) {
            Color(0xFF55661E)
        } else {
            MaterialTheme
                .colorScheme
                .primary
        }
    val centerContent =
        if (dark) {
            MaterialTheme
                .colorScheme
                .primary
        } else {
            MaterialTheme
                .colorScheme
                .onPrimary
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(92.dp)
                .background(
                    MaterialTheme
                        .colorScheme
                        .background
                )
                .padding(
                    horizontal = 12.dp,
                    vertical = 4.dp
                )
    ) {
        Surface(
            modifier =
                Modifier
                    .align(
                        Alignment.BottomCenter
                    )
                    .fillMaxWidth()
                    .height(66.dp)
                    .shadow(
                        elevation =
                            if (dark) {
                                12.dp
                            } else {
                                6.dp
                            },
                        shape =
                            RoundedCornerShape(
                                26.dp
                            ),
                        clip = false
                    ),
            shape =
                RoundedCornerShape(
                    26.dp
                ),
            color = barColor,
            border =
                if (dark) {
                    null
                } else {
                    BorderStroke(
                        1.dp,
                        MaterialTheme
                            .colorScheme
                            .outline
                    )
                }
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 5.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                MainTab.entries
                    .forEach {
                            tab ->
                        if (
                            tab ==
                            MainTab.ACTIVITY
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(66.dp),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                                Spacer(
                                    Modifier.height(
                                        36.dp
                                    )
                                )
                                Text(
                                    tab.label,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelSmall,
                                    color =
                                        barContent.copy(
                                            alpha =
                                                if (
                                                    currentTab ==
                                                    tab
                                                ) {
                                                    1f
                                                } else {
                                                    .68f
                                                }
                                        ),
                                    fontWeight =
                                        if (
                                            currentTab ==
                                            tab
                                        ) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Medium
                                        }
                                )
                            }
                        } else {
                            BottomNavItem(
                                tab = tab,
                                selected =
                                    currentTab ==
                                    tab,
                                contentColor =
                                    barContent,
                                onClick = {
                                    onTabSelected(
                                        tab
                                    )
                                },
                                modifier =
                                    Modifier.weight(1f)
                            )
                        }
                    }
            }
        }

        Surface(
            modifier =
                Modifier
                    .align(
                        Alignment.TopCenter
                    )
                    .size(62.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        clip = false
                    )
                    .clickable {
                        onTabSelected(
                            MainTab.ACTIVITY
                        )
                    },
            shape = CircleShape,
            color = centerFill,
            border =
                BorderStroke(
                    width =
                        if (
                            currentTab ==
                            MainTab.ACTIVITY
                        ) {
                            3.dp
                        } else {
                            1.dp
                        },
                    color =
                        if (
                            currentTab ==
                            MainTab.ACTIVITY
                        ) {
                            MaterialTheme
                                .colorScheme
                                .primary
                        } else {
                            centerContent
                                .copy(
                                    alpha = .30f
                                )
                        }
                )
        ) {
            Box(
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    MainTab.ACTIVITY.icon,
                    contentDescription =
                        MainTab.ACTIVITY.label,
                    tint = centerContent,
                    modifier =
                        Modifier.size(
                            27.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: MainTab,
    selected: Boolean,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .height(66.dp)
                .clickable(
                    onClick = onClick
                )
                .padding(
                    top = 9.dp,
                    bottom = 7.dp
                ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Surface(
            shape =
                RoundedCornerShape(
                    13.dp
                ),
            color =
                if (selected) {
                    contentColor
                        .copy(alpha = .12f)
                } else {
                    Color.Transparent
                }
        ) {
            Box(
                modifier =
                    Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 4.dp
                    ),
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    tab.icon,
                    contentDescription =
                        tab.label,
                    tint =
                        contentColor.copy(
                            alpha =
                                if (selected) {
                                    1f
                                } else {
                                    .66f
                                }
                        ),
                    modifier =
                        Modifier.size(
                            21.dp
                        )
                )
            }
        }
        Text(
            tab.label,
            style =
                MaterialTheme
                    .typography
                    .labelSmall,
            color =
                contentColor.copy(
                    alpha =
                        if (selected) {
                            1f
                        } else {
                            .68f
                        }
                ),
            fontWeight =
                if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                }
        )
    }
}
