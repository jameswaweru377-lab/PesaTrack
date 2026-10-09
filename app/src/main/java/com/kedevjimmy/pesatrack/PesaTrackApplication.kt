package com.kedevjimmy.pesatrack

import android.app.Application
import com.kedevjimmy.pesatrack.security.BackupRecoveryStatus

class PesaTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Restore must reconcile before activities or workers read restored settings.
        BackupRecoveryStatus.retry(this)
    }
}
