package com.kedevjimmy.pesatrack.planning

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

/** Process-local observation; persisted cooldowns and WorkManager survive process death. */
object SmartNoticeLifecycle {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    fun start(app: Application) {
        SmartNoticePreferences(app).recoverInterruptedImport()
        var started = 0
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                started++; SmartNoticeCoordinator.foreground = true
                scope.launch { runCatching { SmartNoticeCoordinator.check(app,allowPost = false) } }
            }
            override fun onActivityStopped(activity: Activity) {
                started = (started - 1).coerceAtLeast(0)
                SmartNoticeCoordinator.foreground = started > 0
            }
            override fun onActivityCreated(activity: Activity, state: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
        scope.launch {
            var previous = LedgerUpdateBus.state.value
            LedgerUpdateBus.state.collect { state ->
                runCatching {
                    if (state.isProcessing) SmartNoticeCoordinator.cancel(app)
                    else if (state.revision != previous.revision || previous.isProcessing)
                        SmartNoticeCoordinator.dataChanged(app)
                    else SmartNoticeCoordinator.check(app,allowPost = false)
                }
                previous = state
            }
        }
    }
}
