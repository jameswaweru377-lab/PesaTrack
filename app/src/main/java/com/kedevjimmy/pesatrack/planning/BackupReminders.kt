package com.kedevjimmy.pesatrack.planning

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.kedevjimmy.pesatrack.MainActivity
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.domain.rules.BackupReminderPolicy
import com.kedevjimmy.pesatrack.domain.rules.PhoneAlertPolicy
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Backup reminders never read private backup contents and never back up automatically. */
object BackupReminders {
    private const val WORK = "pesatrack-backup-reminder"
    private const val PREFS = "backup_reminder_preferences"
    private const val NOTICE_ID = 941

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun enabled(context: Context): Boolean = prefs(context).getLong("enabled_at", 0) > 0

    fun setEnabled(context: Context, enable: Boolean) {
        val edit = prefs(context).edit()
        if (enable && !enabled(context)) edit.putLong("enabled_at", System.currentTimeMillis())
        if (!enable) edit.remove("enabled_at")
        edit.apply()
        if (enable) schedule(context) else {
            WorkManager.getInstance(context).cancelUniqueWork(WORK)
            NotificationManagerCompat.from(context).cancel(NOTICE_ID)
        }
    }

    fun schedule(context: Context) {
        if (!enabled(context)) return
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BackupReminderWorker>(1, TimeUnit.DAYS).build()
        )
    }

    /** Called only after a newly exported backup file has been reread and verified. */
    fun recordVerifiedBackup(context: Context) {
        prefs(context).edit().putLong("last_verified_backup_at", System.currentTimeMillis()).apply()
        NotificationManagerCompat.from(context).cancel(NOTICE_ID)
    }

    fun postIfDue(context: Context): Boolean {
        if (!PhoneNotificationChannels.canPost(context)) return false
        val now = System.currentTimeMillis()
        val prefs = prefs(context)
        if (!BackupReminderPolicy.isDue(
                now, prefs.getLong("enabled_at", 0),
                prefs.getLong("last_verified_backup_at", 0),
                prefs.getLong("last_shown_at", 0),
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            )) return false

        PhoneNotificationChannels.ensure(context, PhoneAlertPolicy.BACKUP_CHANNEL, "Backup reminders")
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(PhoneNotificationPreview.OPEN_PAGE, "backup-reminder")
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val open = PendingIntent.getActivity(
            context, NOTICE_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val detail = "Your last verified backup is over a month old, or you haven't saved one yet."
        val notification = NotificationCompat.Builder(context, PhoneAlertPolicy.BACKUP_CHANNEL)
            .setSmallIcon(R.drawable.ic_pesatrack_logo)
            .setContentTitle("Time to back up your records")
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(PhoneNotificationChannels.privatePublicVersion(context, PhoneAlertPolicy.BACKUP_CHANNEL))
            .build()
        NotificationManagerCompat.from(context).notify(NOTICE_ID, notification)
        prefs.edit().putLong("last_shown_at", now).apply()
        return true
    }
}

class BackupReminderWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result = try {
        BackupReminders.postIfDue(applicationContext)
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }
}
