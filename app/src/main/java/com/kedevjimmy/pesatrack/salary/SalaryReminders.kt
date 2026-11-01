package com.kedevjimmy.pesatrack.salary

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.kedevjimmy.pesatrack.MainActivity
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.rules.SalaryReminderPolicy
import com.kedevjimmy.pesatrack.domain.rules.PhoneAlertPolicy
import com.kedevjimmy.pesatrack.planning.PhoneNotificationChannels
import java.util.concurrent.TimeUnit

object SalaryReminders {
    const val OPEN_SALARY = "open_salary"
    private const val CHANNEL = PhoneAlertPolicy.SALARY_CHANNEL
    private const val WORK = "pesatrack-salary-check"
    fun preferences(context: Context) = context.getSharedPreferences("pesatrack_preferences", Context.MODE_PRIVATE)
    fun schedule(context: Context) {
        if (!preferences(context).getBoolean("setup_complete", false) ||
            !preferences(context).getBoolean("salary_reminders_enabled", false)) return
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequest.Builder(SalaryReminderWorker::class.java, 12, TimeUnit.HOURS).build())
    }
    fun setEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean("salary_reminders_enabled", enabled).apply()
        if (enabled) checkSoon(context) else {
            WorkManager.getInstance(context).cancelUniqueWork(WORK)
            WorkManager.getInstance(context).cancelUniqueWork("$WORK-now")
            NotificationManagerCompat.from(context).cancel(910)
        }
    }
    fun checkSoon(context: Context) {
        if (!preferences(context).getBoolean("setup_complete", false) ||
            !preferences(context).getBoolean("salary_reminders_enabled", false)) return
        schedule(context)
        WorkManager.getInstance(context).enqueueUniqueWork("$WORK-now", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequest.Builder(SalaryReminderWorker::class.java).build())
    }
    fun defer(context: Context) {
        preferences(context).edit().putLong("salary_prompt_deferred_until",
            System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)).apply()
        NotificationManagerCompat.from(context).cancel(910)
        LedgerUpdateBus.notifyChanged()
    }
    fun receiptRecorded(context: Context) {
        preferences(context).edit().remove("salary_prompt_deferred_until").apply()
        NotificationManagerCompat.from(context).cancel(910)
    }
    fun postIfDue(context: Context): Boolean {
        val prefs = preferences(context)
        val now = System.currentTimeMillis()
        if (!prefs.getBoolean("setup_complete", false) || !prefs.getBoolean("salary_reminders_enabled", false) ||
            now < prefs.getLong("salary_prompt_deferred_until", 0)) return false
        if (!PhoneNotificationChannels.canPost(context)) return false
        val rules = RuleStore.get(context).listRules(true).filter { it.startsPayCycle }
        for (rule in rules) {
            val lastSalary = TransactionStore.get(context).latestSalaryReceipt(rule.id)?.occurredAtEpochMs
            val window = SalaryReminderPolicy.dueWindow(lastSalary, rule.payWindowStartDay, rule.payWindowEndDay, now) ?: continue
            val key = "salary_notification_${rule.id}"
            val signature = "${window.startEpochMs}:${lastSalary ?: 0}"
            val lastNotifiedAt = prefs.getLong("${key}_at", 0)
            // One alert per window. 'Not yet received' explicitly allows a follow-up after three days.
            val deferred = prefs.getLong("salary_prompt_deferred_until", 0)
            if (prefs.getString(key, null) == signature && !(deferred > lastNotifiedAt && now >= deferred)) continue
            PhoneNotificationChannels.ensure(context, CHANNEL, "Salary checks")
            val open = PendingIntent.getActivity(context, 910, Intent(context, MainActivity::class.java)
                .putExtra(OPEN_SALARY, true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val defer = PendingIntent.getBroadcast(context, 911, Intent(context, SalaryReminderReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_pesatrack_logo).setContentTitle("Has your salary arrived?")
                .setContentText("No salary entry detected after your expected payday. Add a receipt if it arrived.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("No salary entry detected after your expected payday. If it arrived without a message, record its actual receipt date and amount."))
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setContentIntent(open).setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setPublicVersion(PhoneNotificationChannels.privatePublicVersion(context, CHANNEL))
                .addAction(0, "Add salary", open).addAction(0, "Not yet received", defer).build()
            try {
                NotificationManagerCompat.from(context).notify(910, notification)
            } catch (_: SecurityException) {
                // Notifications may be revoked while a reminder is being prepared.
                return false
            }
            prefs.edit().putString(key, signature).putLong("${key}_at", now).apply()
            return true
        }
        return false
    }
}

class SalaryReminderWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result = try {
        SalaryReminders.postIfDue(applicationContext)
        Result.success()
    } catch (_: Exception) { Result.retry() }
}

class SalaryReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { SalaryReminders.defer(context) }
}
