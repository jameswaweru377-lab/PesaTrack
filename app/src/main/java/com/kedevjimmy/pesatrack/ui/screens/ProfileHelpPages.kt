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
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.kedevjimmy.pesatrack.ui.theme.ProfileVisualColors as C

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
    onPrivacy: (() -> Unit)? = null,
    onHistory: (() -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf<ProfileHelpTopic?>(null) }
    var troubleshooting by remember { mutableStateOf(false) }
    val returnToHelp = { topic = null; troubleshooting = false }

    if (topic != null || troubleshooting) BackHandler(onBack = returnToHelp)

    when {
        troubleshooting -> ProfileTroubleshootingPage(padding, returnToHelp, advanced, onMoneyAccess, onHistory)
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
        else -> ProfilePremiumPage(title, padding, back) {
            item {
                ProfileBanner("Let's get things working", "Find answers in a few taps.",
                    Icons.Rounded.HelpOutline, art = ProfileHeroArt.QUESTION)
            }
            item {
                OutlinedTextField(value = query, onValueChange = { query = it.take(80) },
                    placeholder = { Text("Search for help") },
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Rounded.Search, null,
                        tint = C.muted) }, singleLine = true,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
            }
            val matching = helpTopics.filter {
                query.isBlank() || it.title.contains(query, ignoreCase = true) ||
                    it.summary.contains(query, ignoreCase = true)
            }
            if (matching.isEmpty()) {
                item { PesaCard {
                    Text("No results. Try a shorter word, or open troubleshooting below.",
                        modifier = Modifier.padding(16.dp))
                } }
            } else {
                item { ProfileSection("Popular help") }
                item { ProfilePremiumGroup(matching.map { entry ->
                    val visual = when (entry.title) {
                        "Missing transactions" -> Triple(Icons.Rounded.Search, "Missing transactions", ProfileTone.VIOLET)
                        "Import a statement" -> Triple(Icons.Rounded.Description, "Statement imports", ProfileTone.BLUE)
                        "Incorrect account balance" -> Triple(Icons.Rounded.CompareArrows, "Balances & transfers", ProfileTone.GREEN)
                        "Backup and restore" -> Triple(Icons.Rounded.CloudUpload, "Backup & restore", ProfileTone.AMBER)
                        "PIN and fingerprint" -> Triple(Icons.Rounded.Lock, "App lock & privacy", ProfileTone.PINK)
                        else -> Triple(Icons.Rounded.HelpOutline, entry.title, ProfileTone.VIOLET)
                    }
                    ProfilePremiumItem(visual.first, visual.second, when (entry.title) {
                        "Missing transactions" -> "Find money messages that didn't appear"
                        "Import a statement" -> "Fix a PDF that won't import"
                        "Incorrect account balance" -> "Understand mismatched amounts"
                        "Backup and restore" -> "Keep your records safe"
                        "PIN and fingerprint" -> "Unlock and protect PesaTrack"
                        else -> entry.summary
                    }, visual.third, onClick = { topic = entry })
                }) }
            }
            item {
                OutlinedButton(onClick = { troubleshooting = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, C.violet)) {
                    androidx.compose.material3.Icon(Icons.Rounded.Settings, null, tint = C.violet)
                    Text("Advanced troubleshooting", modifier = Modifier.padding(start = 12.dp),
                        color = C.violet, fontWeight = FontWeight.Bold)
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    androidx.compose.material3.Icon(Icons.Rounded.ChevronRight, null, tint = C.violet)
                }
            }
        }
    }
}

@Composable
private fun ProfileTroubleshootingPage(
    padding: PaddingValues,
    back: () -> Unit,
    advanced: () -> Unit,
    onMoneyAccess: (() -> Unit)?,
    onHistory: (() -> Unit)?
) {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    var checked by rememberSaveable { mutableStateOf(false) }
    val smsApproved = remember(revision) { SmsAccessConsentStore.canRead(context) }
    val listenerAllowed = remember(revision) {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
    ProfilePremiumPage("Troubleshooting", padding, back) {
        item {
            ProfileBanner("Find missing transactions", "We'll check the usual causes.",
                Icons.Rounded.Build, art = ProfileHeroArt.WRENCH)
        }
        item {
            ProfileTroubleshootStep("1", "Check message access", "Make sure PesaTrack can read SMS",
                "Check", ProfileTone.VIOLET, onMoneyAccess)
        }
        item {
            ProfileTroubleshootStep("2", "Check allowed sources", "Choose the apps to follow",
                "Choose apps", ProfileTone.BLUE, onMoneyAccess)
        }
        item {
            ProfileTroubleshootStep("3", "Add older transactions",
                "Import messages or a statement", "Import history", ProfileTone.GREEN, onHistory)
        }
        item {
            Surface(color = C.orangeSoft.copy(alpha = .65f), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
                    ProfileTile(Icons.Rounded.WarningAmber, ProfileTone.AMBER, 40.dp)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Nothing is changed without asking you.",
                            color = C.ink, fontWeight = FontWeight.Bold)
                        Text("We only check and show what we find. You stay in control of your data.",
                            color = C.muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            ProfileGradientButton(if (checked) "Run safe checks again" else "Run safe checks") {
                checked = true; revision++
            }
        }
        if (checked) item {
            Surface(color = C.greenSoft, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("What we found", color = C.ink, fontWeight = FontWeight.Bold)
                    Text("Money messages: ${if (smsApproved) "Allowed" else "Permission needed"}", color = C.muted)
                    Text("Bank-app updates: ${if (listenerAllowed) "Allowed" else "Permission needed"}", color = C.muted)
                }
            }
        }
        item {
            OutlinedButton(onClick = advanced, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, C.violet)) {
                Text("View diagnostics", color = C.violet)
            }
        }
    }
}

@Composable
private fun ProfileTroubleshootStep(number: String, title: String, subtitle: String,
    label: String, tone: ProfileTone, action: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = androidx.compose.foundation.shape.CircleShape,
            color = tone.wash(), modifier = Modifier.size(40.dp)) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                Text(number, color = tone.ink(), fontWeight = FontWeight.Bold)
            }
        }
        Surface(Modifier.weight(1f).padding(start = 8.dp), color = Color.White,
            shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = C.ink, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = C.muted, style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { action?.invoke() }, enabled = action != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = C.violet)) { Text(label) }
            }
        }
    }
}
