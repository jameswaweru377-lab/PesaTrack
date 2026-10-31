package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.kedevjimmy.pesatrack.data.local.SmsAccessConsentStore
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent

private data class ProfileHelpTopic(
    val title: String,
    val summary: String,
    val steps: List<String>
)

private val helpTopics = listOf(
    ProfileHelpTopic("Missing transactions", "SMS, bank updates and statement history", listOf(
        "Open Money message access and check whether you approved SMS and bank-app sources.",
        "For older activity, use Add past transactions. New bank-app updates only start after approval.",
        "Check Activity's Needs Review view. Duplicate confirmations may already be linked."
    )),
    ProfileHelpTopic("Incorrect account balance", "Why a balance might look different", listOf(
        "Check the latest transaction date for that account and compare it with your bank or M-Pesa app.",
        "Missing messages, partial statements or manual entries can affect recorded balances.",
        "PesaTrack shows balances from available records, not a live bank connection."
    )),
    ProfileHelpTopic("Wrong transaction category", "Fix sorting without losing the record", listOf(
        "Open Activity and choose the transaction.",
        "Select the correct category. Where offered, remember it for similar entries.",
        "Check linked statement evidence before changing an account or excluding an entry."
    )),
    ProfileHelpTopic("Backup and restore", "Keep encrypted copies safely", listOf(
        "Open App lock & security, then Encrypted backup & restore.",
        "Keep your backup file and passphrase in safe, separate places.",
        "Review a backup before restoring. Restoring may replace local records; read the confirmation first."
    )),
    ProfileHelpTopic("PIN and fingerprint", "Unlock your private records", listOf(
        "Open App lock & security to create or change your PIN.",
        "If supported by your phone, you can enable biometric unlock after setting a PIN.",
        "Your PIN remains the fallback; phone biometrics do not recover a forgotten PesaTrack PIN."
    )),
    ProfileHelpTopic("Privacy and permissions", "Understand what stays on your phone", listOf(
        "Your financial records are stored locally by default.",
        "Money message access is separate from notification reminders.",
        "Optional business-name lookups can be turned off under Privacy & data."
    )),
    ProfileHelpTopic("Import a statement", "Add bank or M-Pesa history", listOf(
        "Open More, then Statements & imports.",
        "Choose the account and pick your PDF or CSV file.",
        "Read the preview. Only save when the account and amounts are correct."
    )),
    ProfileHelpTopic("Change my profile", "Photo, name and email", listOf(
        "Open your Profile and choose Personal information.",
        "Change your photo, display name or optional email.",
        "Tap Save changes. Wait for Profile saved before leaving."
    ))
)

@Composable
internal fun HelpPage(
    padding: PaddingValues,
    back: () -> Unit,
    advanced: () -> Unit,
    title: String = "Help & support",
    onMoneyAccess: (() -> Unit)? = null,
    onPrivacy: (() -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf<ProfileHelpTopic?>(null) }
    var troubleshooting by remember { mutableStateOf(false) }
    val returnToHelp = { topic = null; troubleshooting = false }

    if (topic != null || troubleshooting) BackHandler(onBack = returnToHelp)

    when {
        troubleshooting -> ProfileTroubleshootingPage(padding, returnToHelp, advanced, onMoneyAccess)
        topic != null -> {
            val selected = topic ?: return
            EverydayPage(selected.title, padding, returnToHelp) {
                item { Text(selected.summary, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                selected.steps.forEachIndexed { index, step ->
                    item {
                        PesaCard {
                            Column(Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("Step ${index + 1}", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(step, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                if (selected.title == "Missing transactions" && onMoneyAccess != null) {
                    item { Button(onClick = onMoneyAccess, modifier = Modifier.fillMaxWidth()) {
                        Text("Open money message access")
                    } }
                }
                if (selected.title == "Privacy and permissions" && onPrivacy != null) {
                    item { Button(onClick = onPrivacy, modifier = Modifier.fillMaxWidth()) {
                        Text("Open privacy settings")
                    } }
                }
            }
        }
        else -> EverydayPage(title, padding, back) {
            item {
                PesaCard {
                    Column(Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBubble(Icons.Rounded.HelpOutline, size = 48.dp,
                                background = pesaToneContainer("violet"),
                                tint = pesaToneContent("violet"))
                            Column(Modifier.padding(start = 12.dp)) {
                                Text("How can we help?", style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold)
                                Text("Find simple answers, right here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        OutlinedTextField(value = query, onValueChange = { query = it.take(80) },
                            label = { Text("Search help topics") },
                            leadingIcon = { androidx.compose.material3.Icon(Icons.Rounded.Search, null) },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item { SectionHeader("Quick help") }
            item { EverydayRow(Icons.Rounded.Build, "Guided troubleshooting",
                "Find what's missing in a few steps") { troubleshooting = true } }
            val matching = helpTopics.filter {
                query.isBlank() || it.title.contains(query, ignoreCase = true) ||
                    it.summary.contains(query, ignoreCase = true)
            }
            if (matching.isEmpty()) {
                item { PesaCard {
                    Text("No results yet. Try a shorter word, or use Guided troubleshooting.",
                        modifier = Modifier.padding(16.dp))
                } }
            } else {
                item { SectionHeader("Popular questions") }
                matching.forEach { entry ->
                    item { EverydayRow(Icons.Rounded.HelpOutline, entry.title, entry.summary) { topic = entry } }
                }
            }
            item { SectionHeader("More help") }
            item { EverydayRow(Icons.Rounded.VerifiedUser, "Advanced diagnostics",
                "See more detailed checks", advanced) }
            item { Text("Your financial records stay on your phone. You decide what to share.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun ProfileTroubleshootingPage(
    padding: PaddingValues,
    back: () -> Unit,
    advanced: () -> Unit,
    onMoneyAccess: (() -> Unit)?
) {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    var checked by rememberSaveable { mutableStateOf(false) }
    val smsApproved = remember(revision) { SmsAccessConsentStore.canRead(context) }
    val listenerAllowed = remember(revision) {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
    EverydayPage("Troubleshooting", padding, back) {
        item {
            PesaCard {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Build, size = 48.dp,
                        background = pesaToneContainer("blue"),
                        tint = pesaToneContent("blue"))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Let's sort this out", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text("Safe checks for missing money updates.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { Text("These checks only look at access. They never change your records.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            Button(onClick = { checked = true; revision++ }, modifier = Modifier.fillMaxWidth()) {
                Text(if (checked) "Run checks again" else "Run safe checks")
            }
        }
        if (checked) item {
            PesaCard {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("What we found", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text("Money SMS: " + (if (smsApproved) "Allowed" else "Needs permission"))
                    Text("Bank-app updates: " + (if (listenerAllowed) "Allowed" else "Needs permission"))
                    Text(if (smsApproved && listenerAllowed)
                        "Access looks ready. Check Activity for items needing review."
                        else "Open Money message access to review what's allowed.")
                }
            }
        }
        if (onMoneyAccess != null) {
            item { EverydayRow(Icons.Rounded.Message, "Manage money message access",
                "Review each source and choose what to allow", onMoneyAccess) }
        }
        item { EverydayRow(Icons.Rounded.Build, "Advanced diagnostics",
            "Open existing checks; confirm before any repair", advanced) }
        item { Text("Still missing something? Check Activity → Needs Review, or import older transactions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
