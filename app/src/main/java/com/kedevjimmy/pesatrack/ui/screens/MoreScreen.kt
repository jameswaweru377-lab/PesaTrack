package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onGoalsClick: (() -> Unit)? = null,
    onPaycheckClick: (() -> Unit)? = null
) {
    var expandedMoneyTools by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "YOUR PESA SPACE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "More",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Keep your records, accounts and app preferences in one place.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                SectionHeader("Import your records")
                PesaCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            IconBubble(
                                icon = Icons.Rounded.FileUpload,
                                size = 42.dp,
                                background = pesaToneContainer("violet"),
                                tint = pesaToneContent("violet")
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 11.dp)
                            ) {
                                Text(
                                    text = "Bring your records together",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Choose an import type to review your records before they are added.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        ImportActionRow(
                            title = "Bank & M-Pesa statements",
                            subtitle = "PDF statements and CSV files",
                            formatLabel = "PDF · CSV",
                            onClick = onStatementActivityClick
                        )
                        if (onPaycheckClick != null) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f),
                                thickness = .7.dp
                            )
                            ImportActionRow(
                                title = "Payslip",
                                subtitle = "Review salary and deductions from a PDF",
                                formatLabel = "PDF",
                                onClick = onPaycheckClick
                            )
                        }
                    }
                }
            }
        }

        if (onReportsClick != null || onBillsClick != null || onGoalsClick != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedMoneyTools = !expandedMoneyTools }
                            .heightIn(min = 44.dp)
                            .padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Money tools", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Reports, bills and goals",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            if (expandedMoneyTools) "Show less" else "Show all",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Icon(
                            if (expandedMoneyTools) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (expandedMoneyTools) "Show fewer money tools" else "Show all money tools",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (expandedMoneyTools) {
                        MoreGroup {
                            if (onReportsClick != null) MoreGroupRow(
                                Icons.Rounded.Insights, "Reports & insights",
                                "Monthly summaries and spending patterns", "green", onReportsClick
                            )
                            if (onBillsClick != null) MoreGroupRow(
                                Icons.Rounded.NotificationsActive, "Bills & reminders",
                                "Upcoming bills and payments", "amber", onBillsClick
                            )
                            if (onGoalsClick != null) MoreGroupRow(
                                Icons.Rounded.Savings, "Saving goals",
                                "Track progress towards your targets", "blue", onGoalsClick
                            )
                        }
                    }
                }
            }
        }

        item { SectionHeader("Your account") }
        item {
            MoreGroup {
                MoreGroupRow(
                    Icons.Rounded.AccountBalanceWallet, "Accounts & debts",
                    "Banks, wallets and money owed", "green", onAccountsClick
                )
                MoreGroupRow(
                    Icons.Rounded.Person, "Profile",
                    "Personal details and financial progress", "blue", onProfileClick
                )
                MoreGroupRow(
                    Icons.Rounded.Settings, "Settings",
                    "Appearance, privacy and notifications", "violet", onSettingsClick,
                    showDivider = false
                )
            }
        }

        item { SectionHeader("History & support") }
        item {
            MoreGroup {
                MoreGroupRow(
                    Icons.Rounded.CalendarMonth, "Import older SMS",
                    "Bring in older money messages from this phone", "green", onImportClick
                )
                MoreGroupRow(
                    Icons.Rounded.Info, "Troubleshooting",
                    "Diagnostics and repair tools", "amber", onDataHealthClick,
                    showDivider = false
                )
            }
        }

        item {
            Text(
                text = "PesaTrack · Your money, more clearly",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ImportActionRow(
    title: String,
    subtitle: String,
    formatLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MoreGroup(content: @Composable () -> Unit) {
    PesaCard(Modifier.fillMaxWidth()) {
        Column { content() }
    }
}

@Composable
private fun MoreGroupRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tone: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 66.dp)
                .padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                icon,
                size = 38.dp,
                background = pesaToneContainer(tone),
                tint = pesaToneContent(tone)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
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
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 62.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f),
                thickness = .7.dp
            )
        }
    }
}
