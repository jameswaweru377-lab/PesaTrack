package com.kedevjimmy.pesatrack.planning

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kedevjimmy.pesatrack.MainActivity
import com.kedevjimmy.pesatrack.data.local.CanonicalStoreLock
import com.kedevjimmy.pesatrack.domain.rules.BalanceReviewReminderPolicy
import com.kedevjimmy.pesatrack.domain.rules.SmartNoticeKind

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
        if (!preferences.enabled(SmartNoticeKind.ACCOUNT) || preferences.mode() == "off" ||
            preferences.snoozeUntil() > now) {
            cancel(appContext)
            return
        }
        if (!PhoneNotificationChannels.canPost(appContext)) return

        // Persist only a hash; account IDs and balances never enter notification history.
        val signature = SmartNoticePreferences.hash(missingAccountIds.distinct().sorted().joinToString("\n"))
        val state = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!BalanceReviewReminderPolicy.shouldPost(
                signature, state.getString("signature", "").orEmpty(),
                state.getLong("shown_at", 0L), now, preferences.lastBanner()
            )) return

        PhoneNotificationChannels.ensure(
            appContext, CHANNEL, "Balance checks", NotificationManager.IMPORTANCE_LOW
        )
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
            return
        }
        state.edit().putString("signature", signature).putLong("shown_at", now).apply()
        preferences.recordBalanceNotice(now)
    }
}
