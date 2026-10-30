package com.kedevjimmy.pesatrack.ui.screens

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LifecycleOwner
import com.kedevjimmy.pesatrack.security.BackupSummary
import com.kedevjimmy.pesatrack.security.VerifiedBackupFile
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

internal enum class BackupPage { HOME, CREATE, RESTORE }
internal enum class RestoreStep { SELECT, PASSWORD, REVIEW, APP_PIN, NO_DEVICE_LOCK, COMPLETE }

/** Terminal results remain pending until a foreground activity can reload the local ledger. */
internal class BackupActivityReloadGate {
    private val pending = AtomicBoolean(false)
    fun requestReload() { pending.set(true) }
    fun consumeReload(): Boolean = pending.compareAndSet(true, false)
}

/** Retains in-memory secrets and in-flight state across Android configuration changes. */
internal class BackupRestoreStateViewModel : ViewModel() {
    val page = mutableStateOf(BackupPage.HOME)
    val restoreStep = mutableStateOf(RestoreStep.SELECT)
    val createPassphrase = mutableStateOf("")
    val confirmPassphrase = mutableStateOf("")
    val pendingCreatePassphrase = mutableStateOf("")
    val restorePassphrase = mutableStateOf("")
    val appPin = mutableStateOf("")
    val selectedUri = mutableStateOf<android.net.Uri?>(null)
    val approvedFile = mutableStateOf<VerifiedBackupFile?>(null)
    val created = mutableStateOf<BackupSummary?>(null)
    val createdFileName = mutableStateOf("")
    val createdProvider = mutableStateOf("")
    val selectedFileName = mutableStateOf("")
    val acknowledgedReplacement = mutableStateOf(false)
    val acknowledgedNoDeviceLock = mutableStateOf(false)
    val busy = mutableStateOf(false)
    val awaitingCredential = mutableStateOf(false)
    val credentialFallbackLaunched = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)

    private val operationClaimed = AtomicBoolean(false)
    private val reloadGate = BackupActivityReloadGate()
    private var registeredApplication: Application? = null
    private var activeActivity: WeakReference<Activity>? = null
    private val activityCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) {
            activeActivity = WeakReference(activity)
            reloadIfPossible()
        }
        override fun onActivityPaused(activity: Activity) {
            if (activeActivity?.get() === activity) activeActivity = null
        }
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    fun observeActivity(activity: Activity) {
        if (registeredApplication == null) {
            registeredApplication = activity.application.also {
                it.registerActivityLifecycleCallbacks(activityCallbacks)
            }
        }
        if ((activity as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
            activeActivity = WeakReference(activity)
        }
        reloadIfPossible()
    }

    fun scheduleLedgerReload() {
        reloadGate.requestReload()
        reloadIfPossible()
    }

    private fun reloadIfPossible() {
        val activity = activeActivity?.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        if (reloadGate.consumeReload()) activity.recreate()
    }

    fun claimRestore(): Boolean = claimOperation()
    fun claimBackup(): Boolean = claimOperation()

    private fun claimOperation(): Boolean = operationClaimed.compareAndSet(false, true).also {
        if (it) busy.value = true
    }

    fun finishRestoreJob(keepBlocked: Boolean = false) {
        busy.value = keepBlocked
        operationClaimed.set(keepBlocked)
    }

    fun clearSecrets() {
        createPassphrase.value = ""
        confirmPassphrase.value = ""
        pendingCreatePassphrase.value = ""
        restorePassphrase.value = ""
        appPin.value = ""
        selectedUri.value = null
        approvedFile.value = null
        acknowledgedReplacement.value = false
        acknowledgedNoDeviceLock.value = false
        awaitingCredential.value = false
        credentialFallbackLaunched.value = false
        selectedFileName.value = ""
        createdFileName.value = ""
        createdProvider.value = ""
        error.value = null
    }

    override fun onCleared() {
        registeredApplication?.unregisterActivityLifecycleCallbacks(activityCallbacks)
        registeredApplication = null
        activeActivity = null
        clearSecrets()
        super.onCleared()
    }
}
