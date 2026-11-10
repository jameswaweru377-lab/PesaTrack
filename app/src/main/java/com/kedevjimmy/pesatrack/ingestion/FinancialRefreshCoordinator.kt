package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import com.kedevjimmy.pesatrack.BuildConfig
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus

/** Presentation-only refresh: no inferred balances, new transactions or network calls. */
internal object FinancialRefreshCoordinator {
    private const val PREFS = "financial_presentation_refresh"
    private const val LAST_VERSION = "last_version_code"
    private const val LAST_REFRESH = "last_refresh_epoch_ms"

    @Synchronized
    fun onForeground(context: Context) {
        val app = context.applicationContext
        if (!app.getSharedPreferences("pesatrack_preferences", Context.MODE_PRIVATE)
                .getBoolean("setup_complete", false)) return

        val preferences = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val version = BuildConfig.VERSION_CODE.toLong()
        if (!FinancialRefreshPolicy.shouldRefresh(
                now, preferences.getLong(LAST_REFRESH, 0L),
                version, preferences.getLong(LAST_VERSION, -1L)
            )) return

        preferences.edit()
            .putLong(LAST_REFRESH, now)
            .putLong(LAST_VERSION, version)
            .apply()
        LedgerUpdateBus.requestPresentationRefresh()
    }
}
