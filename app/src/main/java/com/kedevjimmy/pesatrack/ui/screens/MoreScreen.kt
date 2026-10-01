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
import androidx.compose.material.icons.rounded.Person
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
    onPlanClick: () -> Unit,
    onDataHealthClick: () -> Unit,
    onSettingsClick: () -> Unit
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
                    "Planning and app controls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            SectionHeader("Money tools")
        }
        item {
            MoreRow(
                icon = Icons.Rounded.CalendarMonth,
                title = "Plan",
                subtitle = "Pay-cycle plan, commitments and safe-to-live view",
                tone = "green",
                onClick = onPlanClick
            )
        }
        item {
            SectionHeader("Account & app")
        }
        item {
            MoreRow(
                icon = Icons.Rounded.Info,
                title = "Data Health",
                subtitle = "Message coverage, fallbacks and unsupported shapes",
                tone = "amber",
                onClick = onDataHealthClick
            )
        }
        item {
            MoreRow(
                icon = Icons.Rounded.Person,
                title = "Profile",
                subtitle = "Personal finance profile",
                tone = "blue",
                onClick = onProfileClick
            )
        }
        item {
            MoreRow(
                icon = Icons.Rounded.Settings,
                title = "Settings",
                subtitle = "Theme, permissions, data and smart rules",
                tone = "violet",
                onClick = onSettingsClick
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
