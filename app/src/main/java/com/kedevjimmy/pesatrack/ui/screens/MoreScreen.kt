package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader

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
    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "More",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Everything you need to manage PesaTrack.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("Bring your records in")
                ImportFeatureCard(
                    title = "Import a statement",
                    subtitle = "Bank or M-Pesa statement",
                    formats = "PDF  ·  CSV",
                    supportingText = "Review records before adding them to your money history.",
                    onClick = onStatementActivityClick
                )
                if (onPaycheckClick != null) {
                    ImportSecondaryRow(
                        icon = Icons.Rounded.Description,
                        title = "Import a payslip",
                        subtitle = "PDF salary and deductions",
                        onClick = onPaycheckClick
                    )
                }
            }
        }

        item { SectionHeader("Money tools") }
        item {
            MenuGroup {
                MenuRow(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    title = "Accounts & debts",
                    subtitle = "Banks, wallets and money owed",
                    onClick = onAccountsClick
                )
                if (onReportsClick != null) MenuRow(
                    icon = Icons.Rounded.Insights,
                    title = "Reports & insights",
                    subtitle = "Understand your spending patterns",
                    onClick = onReportsClick
                )
                if (onBillsClick != null) MenuRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Bills & reminders",
                    subtitle = "Keep upcoming payments in view",
                    onClick = onBillsClick
                )
                if (onGoalsClick != null) MenuRow(
                    icon = Icons.Rounded.Savings,
                    title = "Saving goals",
                    subtitle = "Follow progress towards your targets",
                    onClick = onGoalsClick
                )
            }
        }

        item { SectionHeader("Your account") }
        item {
            MenuGroup {
                MenuRow(
                    icon = Icons.Rounded.Person,
                    title = "Profile",
                    subtitle = "Personal details and financial progress",
                    onClick = onProfileClick
                )
                MenuRow(
                    icon = Icons.Rounded.Settings,
                    title = "Settings",
                    subtitle = "Appearance, privacy and notifications",
                    onClick = onSettingsClick
                )
            }
        }

        item { SectionHeader("Records & support") }
        item {
            MenuGroup {
                MenuRow(
                    icon = Icons.Rounded.ReceiptLong,
                    title = "Statement history",
                    subtitle = "View saved statement records",
                    onClick = onStatementActivityClick
                )
                MenuRow(
                    icon = Icons.Rounded.CalendarMonth,
                    title = "Import older SMS",
                    subtitle = "Read earlier money messages on this phone",
                    onClick = onImportClick
                )
                MenuRow(
                    icon = Icons.Rounded.Info,
                    title = "Troubleshooting",
                    subtitle = "Diagnostics and repair tools",
                    onClick = onDataHealthClick
                )
            }
        }

        item {
            Text(
                text = "PesaTrack",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ImportFeatureCard(
    title: String,
    subtitle: String,
    formats: String,
    supportingText: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formats,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    Icons.Rounded.ArrowForward,
                    contentDescription = "Open statement import",
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .13f)
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .82f)
            )
        }
    }
}

@Composable
private fun ImportSecondaryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    PesaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun MenuGroup(content: @Composable () -> Unit) {
    PesaCard(Modifier.fillMaxWidth()) {
        Column { content() }
    }
}

@Composable
private fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 68.dp)
                .padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .8f),
                modifier = Modifier.size(20.dp)
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 66.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f),
            thickness = .7.dp
        )
    }
}
