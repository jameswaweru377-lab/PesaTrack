package com.kedevjimmy.pesatrack.security

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.data.local.CanonicalStoreLock

/** Startup UI gate; database opening itself also refuses access while recovery fails. */
internal object BackupRecoveryStatus {
    @Volatile
    var failure: Throwable? = null

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
