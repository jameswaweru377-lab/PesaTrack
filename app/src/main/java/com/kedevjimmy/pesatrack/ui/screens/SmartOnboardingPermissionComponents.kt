package com.kedevjimmy.pesatrack.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kedevjimmy.pesatrack.ingestion.MpesaNotificationListenerService
import com.kedevjimmy.pesatrack.ui.components.ConceptMessagesIllustration
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent

@Composable
internal fun SmartSmsPermissionPage(
    smsGranted: Boolean,
    denied: Boolean,
    restrictedHelp: Boolean,
    notificationEnabled: Boolean,
    onOpenAppInfo: () -> Unit,
    onTryAgain: () -> Unit,
    onEnableNotifications: () -> Unit,
    onContinue: () -> Unit,
    onContinueWithout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        SmartSetupHero(
            icon = Icons.AutoMirrored.Rounded.Message,
            title = "Let your messages do the work",
            body =
                "PesaTrack finds and organises transaction messages from M-Pesa, banks and other supported money sources."
        )

        ConceptMessagesIllustration()

        PesaCard {
            Column(
                Modifier.padding(18.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                PermissionFeatureRow(
                    icon = Icons.AutoMirrored.Rounded.Message,
                    title = "Read transaction messages",
                    body = "Find and extract transaction details from the history you choose",
                    enabled = smsGranted,
                    actionLabel =
                        if (restrictedHelp) "Open App info" else "Enable",
                    onAction =
                        if (restrictedHelp) onOpenAppInfo else onTryAgain
                )
                PermissionFeatureRow(
                    icon = Icons.Rounded.Notifications,
                    title = "Keep tracking in the background",
                    body = "Keep your money picture current as new confirmations arrive",
                    enabled = notificationEnabled,
                    actionLabel = "Enable",
                    onAction = onEnableNotifications
                )
                IconBubble(
                    Icons.Rounded.Shield,
                    size = 48.dp,
                    background =
                        pesaToneContainer("green"),
                    tint =
                        pesaToneContent("green")
                )
                Text(
                    when {
                        restrictedHelp ->
                            "Android blocked SMS access"
                        smsGranted ->
                            "Message access is ready"
                        denied ->
                            "SMS access wasn't allowed"
                        else ->
                            "Waiting for Android permission"
                    },
                    style =
                        MaterialTheme.typography
                            .titleLarge,
                    color =
                        MaterialTheme.colorScheme
                            .onSurface,
                    textAlign =
                        TextAlign.Center
                )
                Text(
                    when {
                        restrictedHelp ->
                            "This debug APK was installed outside a trusted app store. On this phone, Android requires you to enable “Allow restricted settings” in App info first."
                        smsGranted ->
                            "You can continue to choose how much recent history PesaTrack should import."
                        denied ->
                            "You can try again or continue without importing old messages."
                        else ->
                            "The system Allow / Don't allow dialog should appear automatically."
                    },
                    textAlign =
                        TextAlign.Center,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }

        if (restrictedHelp) {
            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(18.dp),
                color =
                    pesaToneContainer("amber")
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "One Android step is required",
                        style =
                            MaterialTheme.typography
                                .titleMedium,
                        color =
                            MaterialTheme.colorScheme
                                .onSurface
                    )
                    Text(
                        "Open App info → turn on Allow restricted settings → return to PesaTrack. The SMS permission request will open again automatically.",
                        style =
                            MaterialTheme.typography
                                .bodyMedium,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                    Button(
                        onClick = onOpenAppInfo,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                    ) {
                        Text("Open App info")
                    }
                }
            }
        } else if (denied) {
            Button(
                onClick = onTryAgain,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("Try again")
            }
        }

        if (smsGranted) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Continue")
            }
        } else if (denied || restrictedHelp) {
            OutlinedButton(
                onClick = onContinueWithout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue without import")
            }
        }

        Text(
            "PesaTrack cannot switch Android's restricted-settings protection on by itself. That decision always belongs to you.",
            style =
                MaterialTheme.typography
                    .bodySmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    enabled: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(icon, size = 42.dp)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (enabled) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = "Enabled",
                tint = pesaToneContent("green")
            )
        } else {
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
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
