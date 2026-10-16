package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.PesaTrackTopBar
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent

@Composable
fun MoreScreen(
    scaffoldPadding: PaddingValues,
    onProfileClick: () -> Unit,
    onImportClick: () -> Unit,
    onStatementActivityClick: () -> Unit,
    onDataHealthClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onReportsClick: (() -> Unit)? = null,
    onBillsClick: (() -> Unit)? = null,
    onGoalsClick: (() -> Unit)? = null
) {
    var expandedMoneyTools by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PesaTrackTopBar(onProfileClick = onProfileClick)
        }
        item {
            Column {
                Text(
                    "More",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    "Money tools, profile and app controls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            MoreRow(
                icon = Icons.Rounded.ReceiptLong,
                title = "Saved statements",
                subtitle = "Explore records and statement history",
                tone = "green",
                onClick = onStatementActivityClick
            )
        }
        if (onReportsClick != null || onBillsClick != null || onGoalsClick != null) {
            item {
                TextButton(onClick = { expandedMoneyTools = !expandedMoneyTools },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (expandedMoneyTools) "Hide additional money tools"
                        else "More money tools")
                    Icon(if (expandedMoneyTools)
                        Icons.Rounded.ExpandLess
                        else Icons.Rounded.ExpandMore,
                        contentDescription = null)
                }
            }
        }
        if (expandedMoneyTools && onReportsClick != null) {
            item {
                MoreRow(
                    icon = Icons.Rounded.Insights,
                    title = "Reports & insights",
                    subtitle = "See where your money goes",
                    tone = "green",
                    onClick = onReportsClick
                )
            }
        }
        if (expandedMoneyTools && onGoalsClick != null) {
            item {
                MoreRow(
                    icon = Icons.Rounded.Savings,
                    title = "Saving goals",
                    subtitle = "Track your savings targets",
                    tone = "blue",
                    onClick = onGoalsClick
                )
            }
        }
        if (expandedMoneyTools && onBillsClick != null) {
            item {
                MoreRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Bills & reminders",
                    subtitle = "Upcoming bills and payments",
                    tone = "amber",
                    onClick = onBillsClick
                )
            }
        }
        item {
            SectionHeader("Account & app")
        }
        item {
            MoreRow(
                icon = Icons.Rounded.Person,
                title = "Profile",
                subtitle = "Personal details and financial progress",
                tone = "blue",
                onClick = onProfileClick
            )
        }
        item {
            MoreRow(
                icon = Icons.Rounded.Settings,
                title = "Settings",
                subtitle = "Privacy, reminders and appearance",
                tone = "violet",
                onClick = onSettingsClick
            )
        }
        item {
            SectionHeader("History and help")
        }
        item {
            MoreRow(
                icon = Icons.Rounded.CalendarMonth,
                title = "Add past transactions",
                subtitle = "Read older money messages from this phone",
                tone = "green",
                onClick = onImportClick
            )
        }
        item {
            MoreRow(
                icon = Icons.Rounded.Info,
                title = "Advanced troubleshooting",
                subtitle = "Message tests and repair tools",
                tone = "amber",
                onClick = onDataHealthClick
            )
        }
    }
}

@Composable
private fun MoreRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tone: String,
    onClick: () -> Unit
) {
    PesaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                icon = icon,
                size = 42.dp,
                background = pesaToneContainer(tone),
                tint = pesaToneContent(tone)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
