package com.kedevjimmy.pesatrack.security

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.data.local.CanonicalStoreLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Startup UI gate; database opening itself also refuses access while recovery fails. */
internal object BackupRecoveryStatus {
    private val mutableFailures = MutableStateFlow<Throwable?>(null)
    val failures = mutableFailures.asStateFlow()
    var failure: Throwable?
        get() = mutableFailures.value
        set(value) { mutableFailures.value = value }

    fun retry(context: Context) {
        synchronized(CanonicalStoreLock.monitor) {
            if (BackupRestoreJournal.isPending(context)) {
                failure = runCatching {
                    PesaTrackDatabase.get(context).writableDatabase
                }.exceptionOrNull()
            } else {
                failure = null
            }
        }
    }
}
