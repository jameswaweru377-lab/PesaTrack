package com.kedevjimmy.pesatrack.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Checkbox
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kedevjimmy.pesatrack.ingestion.MpesaNotificationListenerService
import com.kedevjimmy.pesatrack.ui.components.FinancialMessagesIllustration
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent

@Composable
internal fun SmartSmsPermissionPage(
    accepted: Boolean,
    busy: Boolean,
    denied: Boolean,
    restrictedHelp: Boolean,
    consentError: String? = null,
    selectedRangeDescription: String? = null,
    onAccepted: (Boolean) -> Unit,
    onOpenAppInfo: () -> Unit,
    onTryAgain: () -> Unit,
    onContinue: () -> Unit,
    onContinueWithout: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val compact = maxHeight < 600.dp
    val constrained = maxHeight < 480.dp || LocalDensity.current.fontScale > 1.3f
    val disclosureScroll = rememberScrollState()
    var showMoreDetails by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(denied) { if (denied) disclosureScroll.scrollTo(0) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)) {
        if (!constrained) MessageAccessIntroduction(compact)
        PesaCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(disclosureScroll).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (constrained) MessageAccessIntroduction(compact = true)
                consentError?.let { Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall) }
        if (denied) {
            Text(if (restrictedHelp) "Android did not allow SMS access. If access is restricted, open App info → ⋮ → Allow restricted settings, then return."
                else "SMS access wasn't allowed. You can retry or continue without importing history.",
                style = MaterialTheme.typography.bodySmall)
            Row {
                if (restrictedHelp) TextButton(onClick = onOpenAppInfo) { Text("Open App info") }
                TextButton(onClick = onTryAgain) { Text("Try SMS again") }
            }
        }
                Text("Your messages. Your choice.", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text("Before Android asks for SMS access, here's exactly what PesaTrack will do.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                selectedRangeDescription?.let {
                    Text("Selected scan range: $it",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
                MessageAccessDisclosureItem(
                    icon = Icons.AutoMirrored.Rounded.Message,
                    title = "What we check",
                    body = "SMS on this phone, within your chosen range, for financial activity. Other messages aren't added as transactions."
                )
                MessageAccessDisclosureItem(
                    icon = Icons.Rounded.Shield,
                    title = "Private by default",
                    body = "Full message text is processed on this device and isn't uploaded. Online business-name lookup is optional."
                )
                MessageAccessDisclosureItem(
                    icon = Icons.Rounded.Notifications,
                    title = "You stay in control",
                    body = "Android requests SMS access next. Notification access is a separate setting later. You can decline or revoke access."
                )
                TextButton(onClick = { showMoreDetails = !showMoreDetails }) {
                    Text(if (showMoreDetails) "− Show less" else "+ More about how your data is used")
                }
                if (showMoreDetails) {
                    Text("PesaTrack uses matching financial messages to build your income, spending, savings and debt picture. Newer SMS can be checked when you reopen the app. If Android blocks access, you may need to allow restricted settings yourself.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Optional online business-name lookup sends only a sanitized business descriptor. It does not send your full message, amount, balance, account number or transaction code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (constrained) MessageAccessAgreement(accepted, busy, onAccepted)
            }
        }
        if (!constrained) MessageAccessAgreement(accepted, busy, onAccepted)
        Button(onClick = onContinue, enabled = accepted && !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(if (busy) "Waiting for Android…" else "Continue to SMS access")
        }
        TextButton(onClick = onContinueWithout, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Continue without SMS access")
        }
    }
    }
}

@Composable
private fun MessageAccessDisclosureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MessageAccessIntroduction(compact: Boolean) {
    Text("Build your money picture privately",
        style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold)
    Text("PesaTrack checks financial SMS on this device to build your money picture.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    FinancialMessagesIllustration(compact = compact)
}

@Composable
private fun MessageAccessAgreement(accepted: Boolean, busy: Boolean, onAccepted: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = accepted, enabled = !busy,
        role = Role.Checkbox, onValueChange = onAccepted), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = accepted, onCheckedChange = null, enabled = !busy)
        Text("I agree to let PesaTrack read financial messages in my selected range to organize my money activity.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun SmartLiveCapturePage(
    alreadyEnabled: Boolean,
    settingsVisited: Boolean,
    restrictedInstall: Boolean,
    restrictedPrepared: Boolean,
    onEnable: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onLater: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
    SmartSetupHero(
        icon = Icons.Rounded.Notifications,
        title = "Keep your money picture current",
        body =
            "Notification access lets PesaTrack understand new financial confirmations as they arrive, without reading them again manually."
    )

    PesaCard {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
            SmartFeatureRow(
                icon =
                    Icons.AutoMirrored.Rounded.Message,
                title = "New money messages",
                body =
                    "Processed locally when they arrive"
            )
            SmartFeatureRow(
                icon =
                    Icons.Rounded.Shield,
                title = "No extra daily work",
                body =
                    "Your Activity view keeps itself current"
            )
        }
    }

    if (alreadyEnabled) {
        Surface(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(18.dp),
            color =
                pesaToneContainer("green")
        ) {
            Row(
                Modifier.padding(15.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint =
                        pesaToneContent(
                            "green"
                        )
                )
                Text(
                    "Live tracking is already enabled.",
                    modifier =
                        Modifier.padding(
                            start = 10.dp
                        )
                )
            }
        }
    }

    Button(
        onClick = onEnable,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            when {
                alreadyEnabled ->
                    "Continue"
                restrictedInstall &&
                    !restrictedPrepared ->
                    "Open App info first"
                else ->
                    "Enable live tracking"
            }
        )
    }

    if (
        restrictedInstall &&
        !restrictedPrepared &&
        !alreadyEnabled
    ) {
        Surface(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(18.dp),
            color =
                pesaToneContainer("amber")
        ) {
            Column(
                Modifier.padding(15.dp)
            ) {
                Text(
                    "Debug install protection",
                    style =
                        MaterialTheme.typography
                            .titleMedium
                )
                Text(
                    "Because this APK was installed outside Google Play, Android may block Notification Access until you manually turn on “Allow restricted settings” in App info.",
                    modifier =
                        Modifier.padding(top = 4.dp),
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }

    if (
        settingsVisited &&
        !alreadyEnabled
    ) {
        Surface(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(18.dp),
            color =
                pesaToneContainer("amber")
        ) {
            Column(
                Modifier.padding(15.dp)
            ) {
                Text(
                    "Still not enabled?",
                    style =
                        MaterialTheme.typography
                            .titleMedium
                )
                Text(
                    "Sideloaded debug builds can require Android's “Allow restricted settings” switch first.",
                    modifier =
                        Modifier.padding(top = 4.dp),
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
                TextButton(
                    onClick =
                        onOpenAppInfo
                ) {
                    Text("Open App info")
                }
            }
        }
    }

    OutlinedButton(
        onClick = onLater,
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Text("Maybe later")
    }

    Text(
        "Live tracking is optional. PesaTrack remains usable without it.",
        style =
            MaterialTheme.typography
                .bodySmall,
        color =
            MaterialTheme.colorScheme
                .onSurfaceVariant
    )
    }
}

internal fun smartIsLikelySideloaded(
    context: Context
): Boolean {
    val installer =
        runCatching {
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R
            ) {
                context.packageManager
                    .getInstallSourceInfo(
                        context.packageName
                    )
                    .installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager
                    .getInstallerPackageName(
                        context.packageName
                    )
            }
        }.getOrNull()

    return installer
        ?.contains(
            "vending",
            ignoreCase = true
        ) != true
}

internal fun smartHasSmsPermission(
    context: Context
): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_SMS
    ) ==
        PackageManager.PERMISSION_GRANTED

internal fun smartHasNotificationAccess(
    context: Context
): Boolean =
    NotificationManagerCompat
        .getEnabledListenerPackages(
            context
        )
        .contains(context.packageName)

internal fun smartNotificationAccessIntent(
    context: Context
): Intent {
    val component =
        ComponentName(
            context,
            MpesaNotificationListenerService::
                class.java
        )

    if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.R
    ) {
        val detail =
            Intent(
                Settings
                    .ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS
            ).putExtra(
                Settings
                    .EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                component.flattenToString()
            )

        if (
            detail.resolveActivity(
                context.packageManager
            ) != null
        ) {
            return detail
        }
    }

    return Intent(
        Settings
            .ACTION_NOTIFICATION_LISTENER_SETTINGS
    )
}
