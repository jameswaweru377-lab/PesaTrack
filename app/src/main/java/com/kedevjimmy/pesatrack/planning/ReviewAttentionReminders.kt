package com.kedevjimmy.pesatrack.planning

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kedevjimmy.pesatrack.MainActivity
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.rules.ReviewReminderPolicy
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Optional Android banner for unresolved expense classification, never every message. */
object ReviewAttentionReminders {
    const val OPEN_ACTIVITY_REVIEW = "open_activity_review"
    private const val WORK = "review-attention-daily"
    private const val CHANNEL = "review-attention"
    private const val ID = 12730
    private const val KEY_ENABLED = "activity_enabled"
    private fun preferences(c: Context) = c.getSharedPreferences("review_attention_reminders", Context.MODE_PRIVATE)

    fun enabled(c: Context): Boolean = preferences(c).getBoolean(KEY_ENABLED, false)

    fun setEnabled(c: Context, value: Boolean) {
        preferences(c).edit().putBoolean(KEY_ENABLED, value).apply()
        if (!value) c.getSystemService(NotificationManager::class.java).cancel(ID)
        schedule(c)
    }

    fun schedule(c: Context) {
        if (!enabled(c)) {
            WorkManager.getInstance(c).cancelUniqueWork(WORK)
            return
        }
        WorkManager.getInstance(c).enqueueUniquePeriodicWork(
            WORK, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequest.Builder(ReviewAttentionWorker::class.java, 1, TimeUnit.DAYS).build()
        )
    }

    fun check(c: Context) {
        if (!enabled(c) || !c.getSharedPreferences("pesatrack_preferences", Context.MODE_PRIVATE)
                .getBoolean("setup_complete", false)) return
        val now = System.currentTimeMillis()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val permitted = (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(c).areNotificationsEnabled()
        if (!permitted || hour !in 9..19) return
        // Only recognized expense records which still require classification; exclude
        // mirrored/duplicate, reversal, allocation, loan and transfer evidence.
        val snapshot = TransactionStore.get(c).activitySnapshotBetween(
            startEpochMs = now - 30L * ReviewReminderPolicy.DAY_MS,
            endEpochMs = now + 1L,
            nowEpochMs = now,
            performMaintenance = false
        )
        val ambiguous = snapshot.possibleDuplicates.flatMap { listOf(it.first.id, it.second.id) }.toSet()
        val pending = snapshot.transactions.filter {
            it.direction == MoneyDirection.EXPENSE.name &&
                it.reviewState == ReviewState.NEEDS_REVIEW.name &&
                it.category in setOf(TransactionCategory.OTHER.name, TransactionCategory.UNCATEGORIZED.name) &&
                it.id !in ambiguous &&
                it.reversalOfCode == null &&
                it.relationType == null &&
                it.cycleCandidateReason == null
        }
        val manager = c.getSystemService(NotificationManager::class.java)
        if (pending.isEmpty()) {
            manager.cancel(ID)
            return
        }
        val prefs = preferences(c)
        val ids = pending.map { it.id }.toSet()
        if (!ReviewReminderPolicy.shouldNotify(
                enabled = true,
                setupComplete = true,
                permissionsAllow = permitted,
                hourOfDay = hour,
                nowEpochMs = now,
                lastShownEpochMs = prefs.getLong("last_shown", 0L),
                latestEvidenceReceivedEpochMs = pending.maxOf { it.receivedAtEpochMs },
                pendingIds = ids,
                lastShownFingerprint = prefs.getString("last_fingerprint", "") ?: ""
            )) return

        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Activity review tips", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Occasional review prompts when financial activity remains uncategorized"
            }
        )
        val intent = Intent(c, MainActivity::class.java)
            .putExtra(OPEN_ACTIVITY_REVIEW, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val open = PendingIntent.getActivity(c, ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val public = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_pesatrack_logo)
            .setContentTitle("PesaTrack")
            .setContentText("Open the app to review your money activity.")
            .build()
        val note = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_pesatrack_logo)
            .setContentTitle("A few transactions need your review")
            .setContentText("Review uncategorized activity when convenient to improve your spending insights.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .build()
        manager.notify(ID, note)
        prefs.edit().putLong("last_shown", now)
            .putString("last_fingerprint", ReviewReminderPolicy.fingerprint(ids)).apply()
    }
}

class ReviewAttentionWorker(c: Context, p: WorkerParameters) : Worker(c, p) {
    override fun doWork(): Result = try {
        ReviewAttentionReminders.check(applicationContext)
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }
}
