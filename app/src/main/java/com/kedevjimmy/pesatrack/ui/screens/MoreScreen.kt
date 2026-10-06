package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
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
    onDataHealthClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onReportsClick: (() -> Unit)? = null,
    onBillsClick: (() -> Unit)? = null,
    onGoalsClick: (() -> Unit)? = null
) {
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
        if (onReportsClick != null || onBillsClick != null || onGoalsClick != null) {
            item {
                SectionHeader("Money tools")
            }
        }
        if (onReportsClick != null) {
            item {
                MoreRow(
                    icon = Icons.Rounded.Insights,
                    title = "Reports & insights",
                    subtitle = "Week, month and year views from trusted activity",
                    tone = "green",
                    onClick = onReportsClick
                )
            }
        }
        if (onGoalsClick != null) {
            item {
                MoreRow(
                    icon = Icons.Rounded.Savings,
                    title = "Saving goals",
                    subtitle = "Targets backed by your real savings plan",
                    tone = "blue",
                    onClick = onGoalsClick
                )
            }
        }
        if (onBillsClick != null) {
            item {
                MoreRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Bills & reminders",
                    subtitle = "Protected commitments and payment status",
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
                subtitle = "Your money profile and account overview",
                tone = "blue",
                onClick = onProfileClick
            )
        }
        item {
            MoreRow(
                icon = Icons.Rounded.Settings,
                title = "Settings",
                subtitle = "Theme, permissions and smart rules",
                tone = "violet",
                onClick = onSettingsClick
            )
        }
        item {
            SectionHeader("Advanced tools")
        }
        item {
            MoreRow(
                icon = Icons.Rounded.CalendarMonth,
                title = "Import history",
                subtitle = "Bring in an older SMS window when you choose",
                tone = "green",
                onClick = onImportClick
            )
        }
        item {
            MoreRow(
                icon = Icons.Rounded.Info,
                title = "Data health",
                subtitle = "Message coverage and items PesaTrack could not understand",
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
