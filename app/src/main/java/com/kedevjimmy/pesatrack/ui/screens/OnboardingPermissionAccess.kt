package com.kedevjimmy.pesatrack.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kedevjimmy.pesatrack.ingestion.MpesaNotificationListenerService

/** Shared permission checks used by the active onboarding and Settings journeys. */
internal fun smartHasSmsPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
        PackageManager.PERMISSION_GRANTED

internal fun smartHasNotificationAccess(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context)
        .contains(context.packageName)

internal fun smartNotificationAccessIntent(context: Context): Intent {
    val component = ComponentName(context, MpesaNotificationListenerService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                component.flattenToString()
            )
        if (detail.resolveActivity(context.packageManager) != null) {
            return detail
        }
    }
    return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
}
