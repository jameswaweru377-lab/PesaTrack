package com.kedevjimmy.pesatrack.ingestion

import android.content.Context

data class FinancialHistoryReprocessFreshness(
    val lastSuccessfulAtEpochMs: Long?,
    val lastMonths: Int?,
    val appLastUpdateAtEpochMs: Long,
    val reprocessRecommended: Boolean
)

object FinancialHistoryReprocessFreshnessPolicy {
    fun isRecommended(
        lastSuccessfulAtEpochMs: Long?,
        appLastUpdateAtEpochMs: Long
    ): Boolean =
        lastSuccessfulAtEpochMs == null ||
            lastSuccessfulAtEpochMs <
            appLastUpdateAtEpochMs
}

class FinancialHistoryReprocessStateStore(
    context: Context
) {
    private val appContext =
        context.applicationContext
    private val preferences =
        appContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun snapshot():
        FinancialHistoryReprocessFreshness {
        val lastSuccessful =
            preferences.getLong(
                KEY_LAST_SUCCESS_AT,
                0L
            ).takeIf {
                it > 0L
            }
        val lastMonths =
            preferences.getInt(
                KEY_LAST_MONTHS,
                0
            ).takeIf {
                it in
                    FinancialHistoryReprocessor
                        .MIN_MONTHS..
                        FinancialHistoryReprocessor
                            .MAX_MONTHS
            }
        val appLastUpdate =
            runCatching {
                @Suppress("DEPRECATION")
                appContext.packageManager
                    .getPackageInfo(
                        appContext.packageName,
                        0
                    )
                    .lastUpdateTime
            }.getOrDefault(0L)

        return FinancialHistoryReprocessFreshness(
            lastSuccessfulAtEpochMs =
                lastSuccessful,
            lastMonths =
                lastMonths,
            appLastUpdateAtEpochMs =
                appLastUpdate,
            reprocessRecommended =
                FinancialHistoryReprocessFreshnessPolicy
                    .isRecommended(
                        lastSuccessfulAtEpochMs =
                            lastSuccessful,
                        appLastUpdateAtEpochMs =
                            appLastUpdate
                    )
        )
    }

    fun recordSuccess(
        atEpochMs: Long,
        months: Int
    ) {
        preferences.edit()
            .putLong(
                KEY_LAST_SUCCESS_AT,
                atEpochMs
            )
            .putInt(
                KEY_LAST_MONTHS,
                months.coerceIn(
                    FinancialHistoryReprocessor
                        .MIN_MONTHS,
                    FinancialHistoryReprocessor
                        .MAX_MONTHS
                )
            )
            .apply()
    }

    companion object {
        private const val PREFS_NAME =
            "financial_history_reprocess_state"
        private const val KEY_LAST_SUCCESS_AT =
            "last_success_at"
        private const val KEY_LAST_MONTHS =
            "last_months"
    }
}
