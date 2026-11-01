package com.kedevjimmy.pesatrack.planning

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kedevjimmy.pesatrack.R

/** Android owns banners and the notification shade; this only makes a heads-up banner eligible. */
object PhoneNotificationChannels {
    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun ensure(context: Context, id: String, label: String) {
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(id, label, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Optional PesaTrack phone reminders"
                    setShowBadge(true)
                }
            )
        }
    }

    fun privatePublicVersion(context: Context, id: String): android.app.Notification =
        NotificationCompat.Builder(context, id)
            .setSmallIcon(R.drawable.ic_pesatrack_logo)
            .setContentTitle("PesaTrack reminder")
            .setContentText("Open PesaTrack to review your reminder.")
            .build()
}
