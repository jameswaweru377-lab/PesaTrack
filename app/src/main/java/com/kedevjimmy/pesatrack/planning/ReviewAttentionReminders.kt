package com.kedevjimmy.pesatrack.planning

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kedevjimmy.pesatrack.domain.rules.SmartNoticeKind

/** Compatibility entry point for the worker, preference and deep link merged in PR #360. */
object ReviewAttentionReminders {
    const val OPEN_ACTIVITY_REVIEW = "open_activity_review"
    fun enabled(c: Context) = SmartNoticePreferences(c).enabled(SmartNoticeKind.ACTIVITY)
    fun setEnabled(c: Context, value: Boolean) = SmartNoticeCoordinator.setEnabled(c,SmartNoticeKind.ACTIVITY,value)
    fun schedule(c: Context) = SmartNoticeCoordinator.schedule(c)
    fun check(c: Context) = SmartNoticeCoordinator.check(c)
}

class ReviewAttentionWorker(c: Context, p: WorkerParameters) : Worker(c,p) {
    override fun doWork(): Result = try {
        ReviewAttentionReminders.check(applicationContext)
        Result.success()
    } catch (_: Exception) { Result.retry() }
}
