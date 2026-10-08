package com.kedevjimmy.pesatrack.ingestion

import android.content.Context

data class HistoricalImportCheckpoint(
    val resumeKey: String,
    val sinceEpochMs: Long,
    val lastSmsDateEpochMs: Long,
    val lastSmsId: Long,
    val updatedAtEpochMs: Long
)

class HistoricalImportCheckpointStore(
    context: Context
) {
    private val prefs =
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

    fun load(
        resumeKey: String,
        nowEpochMs: Long =
            System.currentTimeMillis(),
        expectedSinceEpochMs: Long? = null
    ): HistoricalImportCheckpoint? {
        val storedKey =
            prefs.getString(
                KEY_RESUME_KEY,
                null
            ) ?: return null

        if (storedKey != resumeKey) {
            return null
        }

        val sinceEpochMs =
            prefs.getLong(
                KEY_SINCE_EPOCH_MS,
                0L
            )
        val lastSmsDateEpochMs =
            prefs.getLong(
                KEY_LAST_SMS_DATE_EPOCH_MS,
                0L
            )
        val lastSmsId =
            prefs.getLong(
                KEY_LAST_SMS_ID,
                -1L
            )
        val updatedAtEpochMs =
            prefs.getLong(
                KEY_UPDATED_AT_EPOCH_MS,
                0L
            )

        val age =
            nowEpochMs - updatedAtEpochMs

        if (
            sinceEpochMs <= 0L ||
            (expectedSinceEpochMs != null &&
                sinceEpochMs != expectedSinceEpochMs) ||
            lastSmsDateEpochMs <= 0L ||
            lastSmsDateEpochMs < sinceEpochMs ||
            lastSmsId < 0L ||
            updatedAtEpochMs <= 0L ||
            age < 0L ||
            age > MAX_CHECKPOINT_AGE_MS
        ) {
            clear()
            return null
        }

        return HistoricalImportCheckpoint(
            resumeKey = storedKey,
            sinceEpochMs = sinceEpochMs,
            lastSmsDateEpochMs =
                lastSmsDateEpochMs,
            lastSmsId = lastSmsId,
            updatedAtEpochMs =
                updatedAtEpochMs
        )
    }

    fun save(
        resumeKey: String,
        sinceEpochMs: Long,
        lastSmsDateEpochMs: Long,
        lastSmsId: Long,
        nowEpochMs: Long =
            System.currentTimeMillis()
    ) {
        if (
            resumeKey.isBlank() ||
            sinceEpochMs <= 0L ||
            lastSmsDateEpochMs <= 0L ||
            lastSmsDateEpochMs < sinceEpochMs ||
            lastSmsId < 0L
        ) {
            return
        }

        prefs.edit()
            .putString(
                KEY_RESUME_KEY,
                resumeKey
            )
            .putLong(
                KEY_SINCE_EPOCH_MS,
                sinceEpochMs
            )
            .putLong(
                KEY_LAST_SMS_DATE_EPOCH_MS,
                lastSmsDateEpochMs
            )
            .putLong(
                KEY_LAST_SMS_ID,
                lastSmsId
            )
            .putLong(
                KEY_UPDATED_AT_EPOCH_MS,
                nowEpochMs
            )
            .apply()
    }

    fun clear() {
        prefs.edit()
            .clear()
            .apply()
    }

    companion object {
        private const val PREFS_NAME =
            "historical_import_checkpoint"

        private const val KEY_RESUME_KEY =
            "resume_key"
        private const val KEY_SINCE_EPOCH_MS =
            "since_epoch_ms"
        private const val KEY_LAST_SMS_DATE_EPOCH_MS =
            "last_sms_date_epoch_ms"
        private const val KEY_LAST_SMS_ID =
            "last_sms_id"
        private const val KEY_UPDATED_AT_EPOCH_MS =
            "updated_at_epoch_ms"

        private const val MAX_CHECKPOINT_AGE_MS =
            7L * 24L * 60L * 60L * 1_000L
    }
}
