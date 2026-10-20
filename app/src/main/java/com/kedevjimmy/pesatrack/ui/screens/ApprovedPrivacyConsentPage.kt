package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Approved third mockup: centered disclosure, compact cards, consent above actions, large detail sheet. */
@Composable
internal fun ApprovedPrivacyConsentPage(
    accepted: Boolean,
    busy: Boolean,
    denied: Boolean,
    restrictedHelp: Boolean,
    consentError: String?,
    selectedRangeDescription: String?,
    onAccepted: (Boolean) -> Unit,
    onOpenAppInfo: () -> Unit,
    onTryAgain: () -> Unit,
    onContinue: () -> Unit,
    onContinueWithout: () -> Unit
) {
    var details by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()
    LaunchedEffect(denied) { if (denied) scroll.scrollTo(0) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            consentError?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error) }
            if (denied) {
                Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(8.dp)) {
                        Text(
                            if (restrictedHelp) "SMS access was blocked. In App info, open ⋮ → Allow restricted settings, then return."
                            else "SMS access wasn't allowed. Retry or continue without importing history.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Row {
                            if (restrictedHelp) TextButton(onClick = onOpenAppInfo) { Text("Open App info") }
                            TextButton(onClick = onTryAgain) { Text("Try SMS again") }
                        }
                    }
                }
            }
            Text(
                "Your messages. Your choice.",
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 23.sp, lineHeight = 27.sp),
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
            Text(
                "Give access only if you want PesaTrack to organize your financial SMS.",
                Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
            )
            ApprovedReferenceHero(privacy = true)
            ApprovedPrivacyCard(
                Icons.AutoMirrored.Rounded.Message,
                "Checks M-Pesa and bank transaction messages",
                "Looks for financial activity like income, spending, transfers and account activity."
            )
            ApprovedPrivacyCard(
                Icons.Rounded.Shield, "Processes full SMS text on this device",
                "Your messages stay on this phone and aren't uploaded."
            )
            ApprovedPrivacyCard(
                Icons.Rounded.AccountBalance, "Optional business-name lookup is separate",
                "Used to identify merchant names. You can keep this off."
            )
            Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(9.dp), horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Info, null, Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        "Android grants SMS access; PesaTrack scans only " +
                            (selectedRangeDescription ?: "your chosen dates") + ".",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().toggleable(value = accepted, enabled = !busy,
                role = Role.Checkbox, onValueChange = onAccepted),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = accepted, onCheckedChange = null, enabled = !busy,
                modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(6.dp))
            Text("I agree to scan financial SMS in my selected range.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp))
        }
        TextButton(onClick = { details = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 34.dp)) {
            Text("Learn how my data is used  ›", style = MaterialTheme.typography.labelMedium)
        }
        Button(onClick = onContinue, enabled = accepted && !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(15.dp)) {
            Text(if (busy) "Waiting for Android…" else "Allow SMS access", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onContinueWithout, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 43.dp)) {
            Text("Continue without SMS access", fontWeight = FontWeight.SemiBold)
        }
    }
    if (details) {
        ModalBottomSheet(
            onDismissRequest = { details = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(.86f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("How PesaTrack uses your data", Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { details = false }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close data-use details")
                    }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ApprovedPrivacyDetails("What gets scanned",
                        "If you give permission, PesaTrack checks SMS on your device for M-Pesa, bank and other financial activity. The initial import covers " +
                            (selectedRangeDescription ?: "your chosen dates") +
                            ". Unrelated messages are not added as transactions.")
                    ApprovedPrivacyDetails("What stays private",
                        "Full message text is processed on this phone. It is not uploaded to an online service for financial activity detection.")
                    ApprovedPrivacyDetails("Optional merchant lookup",
                        "Online business-name lookup is separate and optional. It sends only a sanitized business descriptor, never full messages, amounts, balances, account numbers or transaction codes.")
                    ApprovedPrivacyDetails("You stay in control",
                        "You can deny or revoke SMS permission through Android settings and continue without importing history. Notification access is a different permission requested separately later.")
                    ApprovedPrivacyDetails("After scanning",
                        "You review suggested institutions and accounts before confirming them. New financial messages can be checked when you reopen the app, if access remains enabled.")
                }
                Button(onClick = { details = false },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Got it") }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ApprovedPrivacyCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(35.dp), shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, lineHeight = 15.sp),
                    fontWeight = FontWeight.Bold)
                Text(body,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ApprovedPrivacyDetails(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(text, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
