package com.kedevjimmy.pesatrack.planning

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.kedevjimmy.pesatrack.MainActivity
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.domain.rules.PhoneAlertPolicy
import com.kedevjimmy.pesatrack.domain.rules.PhoneAlertType
import java.util.concurrent.TimeUnit

/** Manual, clearly labelled examples; never creates a pretend financial event. */
object PhoneNotificationPreview {
    const val OPEN_PAGE = "open_phone_notification_page"
    private const val CHANNEL = "notification_preview_banner_v1"
    private const val WORK = "pesatrack-user-requested-notification-preview"
    private const val TYPE = "phone_alert_type"

    fun send(context: Context, alert: PhoneAlertType, delayMinutes: Int): Boolean {
        if (delayMinutes !in PhoneAlertPolicy.previewDelaysMinutes ||
            !PhoneNotificationChannels.canPost(context)) return false
        if (delayMinutes == 0) return post(context, alert)

        val request = OneTimeWorkRequestBuilder<PhoneNotificationPreviewWorker>()
            .setInitialDelay(delayMinutes.toLong(), TimeUnit.MINUTES)
            .setInputData(Data.Builder().putString(TYPE, alert.name).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, request)
        return true
    }

    fun post(context: Context, alert: PhoneAlertType): Boolean {
        if (!PhoneNotificationChannels.canPost(context)) return false
        PhoneNotificationChannels.ensure(context, CHANNEL, "Test phone notifications")
        val open = PendingIntent.getActivity(
            context, 950 + alert.ordinal,
            Intent(context, MainActivity::class.java)
                .putExtra(OPEN_PAGE, alert.route)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val public = PhoneNotificationChannels.privatePublicVersion(context, CHANNEL)
        val notice = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_pesatrack_logo)
            .setContentTitle(alert.previewTitle)
            .setContentText(alert.previewBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.previewBody))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .build()
        return runCatching {
            NotificationManagerCompat.from(context).notify(950 + alert.ordinal, notice)
            true
        }.getOrDefault(false)
    }

    internal fun fromWork(context: Context, rawType: String?): Boolean =
        PhoneAlertPolicy.previewType(rawType)?.let { post(context, it) } ?: false

    internal fun workTypeKey(): String = TYPE
}

class PhoneNotificationPreviewWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result {
        PhoneNotificationPreview.fromWork(applicationContext, inputData.getString(PhoneNotificationPreview.workTypeKey()))
        return Result.success()
    }
}
