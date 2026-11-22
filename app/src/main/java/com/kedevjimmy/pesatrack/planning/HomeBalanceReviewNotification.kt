package com.kedevjimmy.pesatrack.planning

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kedevjimmy.pesatrack.MainActivity
import com.kedevjimmy.pesatrack.data.local.CanonicalStoreLock
import com.kedevjimmy.pesatrack.domain.rules.BalanceReviewReminderPolicy
import com.kedevjimmy.pesatrack.domain.rules.SmartNoticeKind
import com.kedevjimmy.pesatrack.security.BackupRecoveryStatus
import java.util.Calendar

/** A quiet, permission-aware alternative to persistent balance-check cards on Home. */
object HomeBalanceReviewNotification {
    private const val CHANNEL = "home-balance-review"
    private const val ID = 947
    private const val PREFS = "home_balance_review_notice"

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID)
    }

    fun update(context: Context, missingAccountIds: List<String>) =
        synchronized(CanonicalStoreLock.monitor) {
            updateLocked(context, missingAccountIds)
        }

    private fun updateLocked(context: Context, missingAccountIds: List<String>) {
        val appContext = context.applicationContext
        val manager = NotificationManagerCompat.from(appContext)
        if (missingAccountIds.isEmpty()) {
            manager.cancel(ID)
            return
        }

        val preferences = SmartNoticePreferences(appContext)
        val now = System.currentTimeMillis()
        val signature = SmartNoticePreferences.hash(missingAccountIds.distinct().sorted().joinToString("\n"))
        val state = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val postedSignature = state.getString("signature", "").orEmpty()
        if (!BalanceReviewReminderPolicy.postedSetIsCurrent(postedSignature, signature)) {
            // Withdraw the old count even when a new reminder is still on cooldown.
            manager.cancel(ID)
        }
        if (!preferences.enabled(SmartNoticeKind.ACCOUNT) || preferences.mode() == "off" ||
            preferences.snoozeUntil() > now) {
            cancel(appContext)
            return
        }
        if (BackupRecoveryStatus.failure != null || !preferences.importReady()) {
            cancel(appContext)
            return
        }
        if (!PhoneNotificationChannels.canPost(appContext)) return

        // Persist only a hash; account IDs and balances never enter notification history.
        if (!BalanceReviewReminderPolicy.shouldPost(
                signature, postedSignature,
                state.getLong("shown_at", 0L), now, preferences.lastBanner(),
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            )) return

        PhoneNotificationChannels.ensure(
            appContext, CHANNEL, "Balance checks", NotificationManager.IMPORTANCE_LOW
        )
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = appContext.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(CHANNEL)
            if (channel == null || channel.importance == NotificationManager.IMPORTANCE_NONE) {
                // A disabled or missing channel cannot deliver; preserve the daily allowance.
                return
            }
        }
        val intent = Intent(appContext, MainActivity::class.java)
            .putExtra(PhoneNotificationPreview.OPEN_PAGE, "accounts")
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val open = PendingIntent.getActivity(
            appContext, ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val count = missingAccountIds.distinct().size
        val title = if (count == 1) "1 account needs a balance check"
            else "$count accounts need a balance check"
        val notice = PhoneNoticeDesign.apply(
            NotificationCompat.Builder(appContext, CHANNEL), appContext,
            "ACCOUNTS", title,
            "Add the missing balances when you're ready.",
            "Review accounts", PhoneNoticeDesign.Illustration.WALLET, open
        )
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(PhoneNotificationChannels.privatePublicVersion(appContext, CHANNEL))
            .build()
        try {
            manager.notify(ID, notice)
        } catch (_: SecurityException) {
            // Android permission may be revoked between checking and posting.
            Log.w("PesaTrackNotices", "Balance notification blocked by Android permission; no account data logged")
            return
        }
        state.edit().putString("signature", signature).putLong("shown_at", now).apply()
        LocalNotificationInboxStore.record(appContext, "balance:$signature", "ACCOUNTS", title,
            "Add missing balances when you're ready.", "accounts", now)
        preferences.recordBalanceNotice(now)
    }
}
