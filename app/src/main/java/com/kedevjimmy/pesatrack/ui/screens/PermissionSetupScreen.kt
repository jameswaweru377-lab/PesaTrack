package com.kedevjimmy.pesatrack.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kedevjimmy.pesatrack.ui.components.BrandWordmark
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent

@Composable
fun PermissionSetupScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var smsGranted by remember {
        mutableStateOf(hasSmsPermission(context))
    }
    var smsAsked by remember { mutableStateOf(smsGranted) }
    var notificationAccessGranted by remember {
        mutableStateOf(hasNotificationAccess(context))
    }
    var appInfoVisited by remember { mutableStateOf(false) }

    fun refreshAccess() {
        smsGranted = hasSmsPermission(context)
        notificationAccessGranted = hasNotificationAccess(context)
    }

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        smsAsked = true
        smsGranted = granted
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshAccess()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshAccess()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (!smsGranted && !smsAsked) {
            smsAsked = true
            smsLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    LazyColumn(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { BrandWordmark() }

        item {
            Column {
                Text(
                    "Connect PesaTrack",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    "A few Android permissions let PesaTrack import your history and capture new M-Pesa activity on this phone.",
                    modifier = Modifier.padding(top = 6.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            PermissionStepCard(
                title = "SMS history",
                body = if (smsGranted) {
                    "Allowed. PesaTrack can import the months you choose during setup."
                } else {
                    "Optional, but needed to import older M-Pesa and bank SMS."
                },
                complete = smsGranted,
                icon = Icons.Rounded.Message
            ) {
                if (!smsGranted) {
                    OutlinedButton(
                        onClick = {
                            smsAsked = true
                            smsLauncher.launch(Manifest.permission.READ_SMS)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Allow SMS access")
                    }
                }
            }
        }

        item {
            PermissionStepCard(
                title = "Restricted settings",
                body = if (notificationAccessGranted) {
                    "Notification access is already enabled, so this step is complete."
                } else {
                    "On sideloaded APKs Android may require you to open App info and switch on “Allow restricted settings” before Notification Access can be enabled. Android requires you to do this switch yourself."
                },
                complete = notificationAccessGranted,
                icon = Icons.Rounded.Lock
            ) {
                if (!notificationAccessGranted) {
                    OutlinedButton(
                        onClick = {
                            appInfoVisited = true
                            settingsLauncher.launch(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + context.packageName)
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Rounded.OpenInNew,
                            contentDescription = null
                        )
                        Text(
                            "Open App info",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        item {
            PermissionStepCard(
                title = "Live M-Pesa capture",
                body = if (notificationAccessGranted) {
                    "Enabled. PesaTrack can process new financial notifications locally."
                } else if (appInfoVisited) {
                    "Now enable PesaTrack under Android Notification Access."
                } else {
                    "After restricted settings are allowed, enable Notification Access so new M-Pesa confirmations can be captured while they arrive."
                },
                complete = notificationAccessGranted,
                icon = Icons.Rounded.Notifications
            ) {
                if (!notificationAccessGranted) {
                    Button(
                        onClick = {
                            settingsLauncher.launch(
                                Intent(
                                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enable notification access")
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = pesaToneContainer("green")
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = pesaToneContent("green")
                    )
                    Column(Modifier.padding(start = 10.dp)) {
                        Text(
                            "Private by design",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "PesaTrack does not need Accessibility or screen-reading permission. Financial messages are processed on your phone, and raw M-Pesa message bodies are not stored in the ledger.",
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
                enabled = notificationAccessGranted
            ) {
                if (notificationAccessGranted) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null
                    )
                }
                Text(
                    if (notificationAccessGranted) {
                        "Continue setup"
                    } else {
                        "Enable live capture to continue"
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        item {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue without live capture")
            }
        }
    }
}

@Composable
private fun PermissionStepCard(
    title: String,
    body: String,
    complete: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actions: @Composable () -> Unit
) {
    PesaCard {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(
                    if (complete) Icons.Rounded.CheckCircle else icon,
                    size = 42.dp,
                    background = pesaToneContainer(
                        if (complete) "green" else "blue"
                    ),
                    tint = pesaToneContent(
                        if (complete) "green" else "blue"
                    )
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 11.dp)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (complete) "Ready" else "Needs your approval",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                body,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            actions()
        }
    }
}

private fun hasSmsPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED

private fun hasNotificationAccess(context: Context): Boolean =
    NotificationManagerCompat
        .getEnabledListenerPackages(context)
        .contains(context.packageName)
