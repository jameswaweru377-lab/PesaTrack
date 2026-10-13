package com.kedevjimmy.pesatrack

import android.app.Application
import com.kedevjimmy.pesatrack.security.BackupRecoveryStatus

class PesaTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // The PDF sandbox must not initialize backup recovery or open the ledger.
        if (android.os.Build.VERSION.SDK_INT >= 28 && android.os.Process.isIsolated()) return
        com.kedevjimmy.pesatrack.documents.PdfTextReviewReader.clearAbandonedFiles(this)
        // Restore must reconcile before activities or workers read restored settings.
        BackupRecoveryStatus.retry(this)
        com.kedevjimmy.pesatrack.planning.SmartNoticeLifecycle.start(this)
    }
}
