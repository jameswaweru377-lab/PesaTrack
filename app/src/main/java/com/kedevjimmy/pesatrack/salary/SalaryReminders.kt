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
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.rules.SalaryReminderPolicy
import com.kedevjimmy.pesatrack.domain.rules.PhoneAlertPolicy
import com.kedevjimmy.pesatrack.planning.PhoneNotificationChannels
import com.kedevjimmy.pesatrack.planning.PhoneNoticeDesign
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
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
        // Only confirmed ledger salary, never an expected payday or unverified SMS.
        try {
            postConfirmedReceipt(context)
        } catch (_: RuntimeException) {
            android.util.Log.w("PesaTrackSalary", "Salary confirmation notification unavailable")
        }
    }

    private fun postConfirmedReceipt(context: Context) {
        val prefs = preferences(context)
        if (!prefs.getBoolean("salary_reminders_enabled", false) ||
            !PhoneNotificationChannels.canPost(context)) return
        val latest = RuleStore.get(context).listRules(true)
            .filter { it.startsPayCycle }
            .mapNotNull { TransactionStore.get(context).latestSalaryReceipt(it.id) }
            .maxByOrNull { it.occurredAtEpochMs } ?: return
        val now = System.currentTimeMillis()
        if (latest.occurredAtEpochMs > now ||
            now - latest.occurredAtEpochMs > TimeUnit.DAYS.toMillis(2) ||
            prefs.getLong("salary_notice_last_receipt_id", -1L) == latest.id) return
        val bank = AccountStore.get(context).listAccounts(false)
            .firstOrNull { it.id == latest.accountId }?.name ?: "Receiving account"
        val amount = NumberFormat.getNumberInstance(Locale.UK).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }.format(BigDecimal.valueOf(latest.amountMinor, 2))
        PhoneNotificationChannels.ensure(context, CHANNEL, "Salary checks")
        val open = PendingIntent.getActivity(
            context, 912, Intent(context, MainActivity::class.java)
                .putExtra(OPEN_SALARY, true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = PhoneNoticeDesign.apply(
            NotificationCompat.Builder(context, CHANNEL), context,
            "INCOME", "Salary received", "$bank • KES $amount",
            "Open income", PhoneNoticeDesign.Illustration.WALLET, open,
            supportingText = "Your confirmed pay is recorded. Review your plan for this cycle."
        ).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(PhoneNotificationChannels.privatePublicVersion(context, CHANNEL)).build()
        try {
            NotificationManagerCompat.from(context).notify(912, notification)
        } catch (_: SecurityException) {
            return // Android permission was revoked while preparing the alert.
        }
        prefs.edit().putLong("salary_notice_last_receipt_id", latest.id).apply()
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
            val notification = PhoneNoticeDesign.apply(
                NotificationCompat.Builder(context, CHANNEL), context,
                "SALARY", "Has your salary arrived?",
                "No salary entry found after your expected payday.",
                "Add salary", PhoneNoticeDesign.Illustration.WALLET, open
            )
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setContentIntent(open).setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setPublicVersion(PhoneNotificationChannels.privatePublicVersion(context, CHANNEL))
                // "Add salary" is already the primary pill inside the expanded
                // notification. Only expose the separate defer action to Android.
                .addAction(0, "Not yet received", defer).build()
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
