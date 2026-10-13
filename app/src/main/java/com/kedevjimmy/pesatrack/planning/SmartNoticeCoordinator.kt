package com.kedevjimmy.pesatrack.planning

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
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.rules.*
import com.kedevjimmy.pesatrack.security.BackupRecoveryStatus
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Extends #360: one worker, one posted slot and one daily budget for smart attention. */
object SmartNoticeCoordinator {
    const val OPEN_KEY = "open_smart_key"
    const val OPEN_ACTION = "open_smart_action"
    const val OPEN_REVISION = "open_smart_revision"
    private const val WORK = "review-attention-daily"
    private const val CHANGE_WORK = "review-attention-settled"
    private const val CHANNEL = "review-attention"
    private const val QUIET_CHANNEL = "review-attention-quiet"
    private const val ID = 12730
    @Volatile var foreground = false

    fun setEnabled(c: Context, kind: SmartNoticeKind, value: Boolean) {
        synchronized(CanonicalStoreLock.monitor) {
            SmartNoticePreferences(c).setEnabled(kind,value)
            cancel(c); schedule(c)
        }
    }
    fun cancel(c: Context) {
        c.getSystemService(NotificationManager::class.java).cancel(ID)
        SmartNoticePreferences(c).clearPosted()
    }
    fun schedule(c: Context) {
        val prefs = SmartNoticePreferences(c)
        val work = WorkManager.getInstance(c)
        if (!prefs.anyEnabled() || prefs.mode() == "off") {
            work.cancelUniqueWork(WORK); work.cancelUniqueWork(CHANGE_WORK); cancel(c); return
        }
        work.enqueueUniquePeriodicWork(WORK,ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequest.Builder(ReviewAttentionWorker::class.java,1,TimeUnit.DAYS).build())
    }
    fun dataChanged(c: Context) {
        synchronized(CanonicalStoreLock.monitor) {
            SmartNoticePreferences(c).changed()
            check(c,allowPost = false)
            if (SmartNoticePreferences(c).anyEnabled() && SmartNoticePreferences(c).mode() != "off")
                WorkManager.getInstance(c).enqueueUniqueWork(CHANGE_WORK,ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequest.Builder(ReviewAttentionWorker::class.java)
                        .setInitialDelay(ReviewReminderPolicy.IMPORT_SETTLE_MS,TimeUnit.MILLISECONDS).build())
        }
    }
    /** Revalidation runs even with permission off, in quiet hours and while foreground. */
    fun check(c: Context, allowPost: Boolean = true) = synchronized(CanonicalStoreLock.monitor) {
        val prefs = SmartNoticePreferences(c)
        if (BackupRecoveryStatus.failure != null || LedgerUpdateBus.state.value.isProcessing || !prefs.importReady()) {
            cancel(c); return@synchronized
        }
        if (!prefs.anyEnabled()) { cancel(c); return@synchronized }
        val now = System.currentTimeMillis()
        val ready = c.getSharedPreferences("pesatrack_preferences",Context.MODE_PRIVATE).getBoolean("setup_complete",false)
        if (!ready || prefs.mode() == "off" || prefs.snoozeUntil() > now) { cancel(c); return@synchronized }
        val notices = SmartNoticeCandidates.read(c,now)
        val posted = notices.firstOrNull { it.candidate.key == prefs.postedKey() &&
            it.candidate.revision == prefs.postedRevision() && it.candidate.enabled }
        if (posted == null) cancel(c)
        if (!allowPost) return@synchronized
        val manager = c.getSystemService(NotificationManager::class.java)
        val quiet = prefs.mode() == "quiet"
        val channel = if (quiet) QUIET_CHANNEL else CHANNEL
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(channel,
                if (quiet) "Quiet review summaries" else "Smart review alerts",
                if (quiet) NotificationManager.IMPORTANCE_LOW else NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Optional account, Activity and They Owe Me review prompts"
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
                })
            if (manager.getNotificationChannel(channel)?.importance == NotificationManager.IMPORTANCE_NONE) return@synchronized
        }
        val permitted = NotificationManagerCompat.from(c).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(c,Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        val selected = SmartNotificationPolicy.choose(notices.map { it.candidate },
            notices.associate { it.candidate.key to prefs.state(it.candidate.key) },now,
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),prefs.lastBanner(),prefs.snoozeUntil(),
            ready,permitted,foreground,LedgerUpdateBus.state.value.isProcessing) ?: return@synchronized
        val notice = notices.first { it.candidate == selected }
        val title = when (selected.kind) {
            SmartNoticeKind.ACCOUNT -> "An account needs your review"
            SmartNoticeKind.ACTIVITY -> "A few transactions need your review"
            SmartNoticeKind.PAYMENT -> "A payment may relate to They Owe Me"
            SmartNoticeKind.DISCOVERY -> "Track money owed to you"
        }
        val text = when (selected.kind) {
            SmartNoticeKind.ACCOUNT -> "Confirm its type or ownership to help keep your records accurate."
            SmartNoticeKind.ACTIVITY -> "Review uncategorized activity when convenient."
            SmartNoticeKind.PAYMENT -> "Review this payment before deciding whether it relates to a loan."
            SmartNoticeKind.DISCOVERY -> "Lent money to someone? Explore They Owe Me to track what's still owed."
        }
        fun open(action: String) = PendingIntent.getActivity(c,action.hashCode(),
            Intent(c,MainActivity::class.java).setAction("smart:$action:${selected.key}")
                .putExtra(OPEN_KEY,selected.key).putExtra(OPEN_ACTION,action)
                .putExtra(OPEN_REVISION,selected.revision)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val deleted = PendingIntent.getBroadcast(c,ID,
            Intent(c,SmartNoticeDismissReceiver::class.java).setAction("dismiss:${selected.key}:${selected.revision}")
                .putExtra("key",selected.key).putExtra("revision",selected.revision),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val public = NotificationCompat.Builder(c,channel).setSmallIcon(R.drawable.ic_pesatrack_logo)
            .setContentTitle("PesaTrack").setContentText("Open PesaTrack when convenient.").build()
        val builder = NotificationCompat.Builder(c,channel).setSmallIcon(R.drawable.ic_pesatrack_logo)
            .setContentTitle(title).setContentText(text).setContentIntent(open("review"))
            .setDeleteIntent(deleted).setAutoCancel(true).setOnlyAlertOnce(true)
            .setPriority(if (quiet) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
        if (Build.VERSION.SDK_INT < 26 && !quiet) builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
        if (selected.kind == SmartNoticeKind.PAYMENT) {
            if (!notice.incoming || notice.repaymentAllowed) builder.addAction(0,
                if (notice.incoming) "Review repayment" else "Add as loan",open("entry"))
            builder.addAction(0,"Gift",open("gift")).addAction(0,
                if (notice.incoming) "Not repayment" else "Not a loan",open("other"))
        } else builder.addAction(0,if (selected.kind == SmartNoticeKind.ACCOUNT) "Review account" else "Open",open("review"))
        try { manager.notify(ID,builder.build()) } catch (_: SecurityException) { return@synchronized }
        prefs.shown(selected,now)
        notice.personId?.let { prefs.personShown(it,now) }
    }
    /** Current evidence remains readable after delivery caps have been consumed. */
    fun resolve(c: Context, key: String, revision: String = ""): SmartNotice? = synchronized(CanonicalStoreLock.monitor) {
        val prefs = SmartNoticePreferences(c)
        if (BackupRecoveryStatus.failure != null || LedgerUpdateBus.state.value.isProcessing || !prefs.importReady()) return@synchronized null
        if (key == "discovery") return@synchronized SmartNotice(
            SmartNoticeCandidate(key,"1",SmartNoticeKind.DISCOVERY,prefs.settledAt()))
        SmartNoticeCandidates.read(c,System.currentTimeMillis()).firstOrNull {
            it.candidate.key == key && (revision.isEmpty() || it.candidate.revision == revision) &&
                !prefs.state(key).dismissed
        }
    }
}

class SmartNoticeDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        synchronized(CanonicalStoreLock.monitor) {
            val prefs = SmartNoticePreferences(context)
            val key = intent.getStringExtra("key") ?: return
            val revision = intent.getStringExtra("revision") ?: return
            prefs.dismissed(key,revision)
            if (key == "discovery") prefs.completeDiscovery()
            if (prefs.postedKey() == key && prefs.postedRevision() == revision) prefs.clearPosted()
        }
    }
}
