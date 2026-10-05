package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ProfileMoneyPicture(
    val receipts: Int = 0,
    val understood: Int = 0,
    val accountCount: Int = 0,
    val goalCount: Int = 0
) {
    val coverage: Int
        get() = if (receipts == 0) 0 else ((understood * 100.0) / receipts).toInt().coerceIn(0, 100)
}

@Composable
fun ProfileScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onAccountsClick: (() -> Unit)? = null,
    onDataHealthClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val revision by LedgerUpdateBus.state.collectAsState()
    var picture by remember { mutableStateOf(ProfileMoneyPicture()) }

    LaunchedEffect(revision.revision) {
        picture = withContext(Dispatchers.IO) {
            val rows = TransactionStore.get(context).recent(100)
                .filter { it.reviewState != ReviewState.EXCLUDED.name }
            val understood = rows.count {
                it.reviewState != ReviewState.NEEDS_REVIEW.name &&
                    it.cycleCandidateReason == null
            }
            val accounts = AccountStore.get(context)
                .listAccounts(enabledOnly = true)
                .count { it.type != AccountType.CASH }
            val goals = RuleStore.get(context)
                .listRules(enabledOnly = true)
                .count {
                    it.category == TransactionCategory.SAVINGS &&
                        (it.plannedAmountMinor ?: 0L) > 0L
                }
            ProfileMoneyPicture(
                receipts = rows.size,
                understood = understood,
                accountCount = accounts,
                goalCount = goals
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Profile",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(84.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        modifier = Modifier.background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primary.copy(alpha = .32f)
                                )
                            )
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Text(
                        "PesaTrack user",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "My money, my way",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(98.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { picture.coverage / 100f },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 11.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = .55f)
                        )
                        Text(
                            picture.coverage.toString() + "%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(
                            "Your money picture",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Receipt coverage",
                            modifier = Modifier.padding(top = 7.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            if (picture.receipts == 0) {
                                "Waiting for transaction messages"
                            } else {
                                picture.understood.toString() + " of " +
                                    picture.receipts + " recent receipts understood"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileMetricCard(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    value = picture.accountCount.toString(),
                    label = "spending accounts",
                    modifier = Modifier.weight(1f)
                )
                ProfileMetricCard(
                    icon = Icons.Rounded.TrackChanges,
                    value = picture.goalCount.toString(),
                    label = "active goals",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            PesaCard {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Rounded.Person,
                        label = "Personal information",
                        onClick = onSettingsClick
                    )
                    ProfileMenuRow(
                        icon = Icons.Rounded.CreditCard,
                        label = "Accounts",
                        onClick = onAccountsClick ?: onSettingsClick
                    )
                    ProfileMenuRow(
                        icon = Icons.Rounded.Shield,
                        label = "Privacy & permissions",
                        onClick = onSettingsClick
                    )
                    ProfileMenuRow(
                        icon = Icons.Rounded.Notifications,
                        label = "Notifications",
                        onClick = onSettingsClick
                    )
                    ProfileMenuRow(
                        icon = Icons.Rounded.Palette,
                        label = "Appearance",
                        onClick = onSettingsClick
                    )
                    ProfileMenuRow(
                        icon = Icons.Rounded.HelpOutline,
                        label = "Data health & help",
                        onClick = onDataHealthClick ?: onSettingsClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileMetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier
) {
    PesaCard(modifier) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                icon,
                size = 40.dp,
                background = pesaToneContainer("green"),
                tint = pesaToneContent("green")
            )
            Column(Modifier.padding(start = 9.dp)) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
