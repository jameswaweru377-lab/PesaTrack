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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
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
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent

@Composable
fun MoreScreen(
    scaffoldPadding: PaddingValues,
    onProfileClick: () -> Unit,
    onAccountsClick: () -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text(
                    "More",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "Money tools, profile and app controls.",
                    style = MaterialTheme.typography.bodyMedium,
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
        if (onReportsClick != null || onBillsClick != null || onGoalsClick != null) item {
            Column {
                Row(Modifier.fillMaxWidth().clickable { expandedMoneyTools = !expandedMoneyTools }
                    .heightIn(min = 48.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Money tools", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text(if (expandedMoneyTools) "Show less" else "Show all",
                        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Icon(if (expandedMoneyTools) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                if (expandedMoneyTools) MoreGroup {
                    if (onReportsClick != null) MoreGroupRow(Icons.Rounded.Insights, "Reports",
                        "Monthly summaries and export", "green", onReportsClick)
                    if (onBillsClick != null) MoreGroupRow(Icons.Rounded.NotificationsActive,
                        "Bills & reminders", "Upcoming bills and payments", "amber", onBillsClick)
                    if (onGoalsClick != null) MoreGroupRow(Icons.Rounded.Savings, "Saving goals",
                        "Track your savings targets", "blue", onGoalsClick)
                }
            }
        }
        item { SectionHeader("Account & app") }
        item { MoreGroup {
            MoreGroupRow(Icons.Rounded.AccountBalanceWallet, "Accounts & debts", "Wallets, banks and what you owe", "green", onAccountsClick)
            MoreGroupRow(Icons.Rounded.Person, "Profile", "Personal details and financial progress", "blue", onProfileClick)
            MoreGroupRow(Icons.Rounded.Settings, "Settings", "Privacy, reminders and appearance", "violet", onSettingsClick)
        } }
        item { SectionHeader("History & help") }
        item { MoreGroup {
            MoreGroupRow(Icons.Rounded.CalendarMonth, "Add past transactions", "Read older money messages from this phone", "green", onImportClick)
            MoreGroupRow(Icons.Rounded.Info, "Advanced troubleshooting", "Message tests and repair tools", "amber", onDataHealthClick)
        } }
    }
}

@Composable
private fun MoreGroup(content: @Composable () -> Unit) {
    PesaCard(Modifier.fillMaxWidth()) { Column { content() } }
}

@Composable
private fun MoreGroupRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tone: String,
    onClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 68.dp).padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconBubble(icon, size = 38.dp, background = pesaToneContainer(tone), tint = pesaToneContent(tone))
        Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f), thickness = .7.dp)
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
