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
import androidx.compose.runtime.saveable.rememberSaveable
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
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
import com.kedevjimmy.pesatrack.ui.components.ConceptLoadError
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
    val preferences = remember { context.getSharedPreferences("pesatrack_preferences", 0) }
    var name by rememberSaveable { mutableStateOf(preferences.getString("profile_name", "").orEmpty()) }
    var avatar by rememberSaveable { mutableStateOf(preferences.getString("profile_avatar", "🌿") ?: "🌿") }
    var editProfile by remember { mutableStateOf(false) }
    var draftName by remember { mutableStateOf("") }
    var draftAvatar by remember { mutableStateOf(avatar) }
    fun openEditor() { draftName = name; draftAvatar = avatar; editProfile = true }
    var picture by remember { mutableStateOf(ProfileMoneyPicture()) }

    var loadError by remember { mutableStateOf<String?>(null) }
    var retryLoad by remember { mutableIntStateOf(0) }
    LaunchedEffect(revision.revision, retryLoad) {
        loadError = null
        try {
        picture = withContext(Dispatchers.IO) {
            val snapshot = TransactionStore.get(context).activitySnapshot()
            val rows = snapshot.transactions.filter { it.reviewState != ReviewState.EXCLUDED.name && it.type != "REVERSAL" }
            val understood = snapshot.trustedTransactions.size
            val confirmed = FinancialUnderstandingStore.get(context).confirmedAccountIds()
            val accounts = AccountStore.get(context).listAccounts(true)
                .count {
                    AccountDisplayPolicy.isConfirmedOwned(
                        it,
                        confirmed
                    )
                }
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

        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (failure: Exception) {
            android.util.Log.e("PesaTrackScreens", "Could not load screen", failure)
            loadError = "Couldn't load this page. Your saved data is unchanged."
        }
    }

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ConceptLoadError(loadError) { retryLoad++ } }
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
                        Text(avatar, style = MaterialTheme.typography.displaySmall)

                    }
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Text(
                        name.ifBlank { "PesaTrack user" },
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
                            if (picture.receipts == 0) "—" else picture.coverage.toString() + "%",
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
                            "Money-data coverage",
                            modifier = Modifier.padding(top = 7.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            if (picture.receipts == 0) {
                                "Waiting for transaction messages"
                            } else {
                                picture.understood.toString() + " of " +
                                    picture.receipts + " recorded entries included this month"
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
                    label = "confirmed accounts",
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
                        onClick = ::openEditor
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
    if (editProfile) AlertDialog(onDismissRequest = { editProfile = false }, title = { Text("Your profile") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(draftName, { draftName = it.take(60) }, label = { Text("Your name") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("🌿", "😊", "🧑🏾", "👩🏾").forEach { value ->
                    FilterChip(selected = draftAvatar == value, onClick = { draftAvatar = value }, label = { Text(value) })
                }
            }
        } },
        confirmButton = { TextButton(onClick = {
            name = draftName.trim(); avatar = draftAvatar
            preferences.edit().putString("profile_name", name).putString("profile_avatar", avatar).apply()
            editProfile = false
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { editProfile = false }) { Text("Cancel") } })

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
