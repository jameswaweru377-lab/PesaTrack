package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.FinancialSmsSenderApprovalStore

data class HistoricalImportCheckpoint(
    val resumeKey: String,
    val sinceEpochMs: Long,
    val lastSmsDateEpochMs: Long,
    val lastSmsId: Long,
    val updatedAtEpochMs: Long,
    val approvalRevision: Long = 0L
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
    private val approvals = FinancialSmsSenderApprovalStore(context)

    fun load(
        resumeKey: String,
        nowEpochMs: Long =
            System.currentTimeMillis(),
        expectedSinceEpochMs: Long? = null,
        expectedApprovalRevision: Long = approvals.revision()
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
        val approvalRevision = prefs.getLong(KEY_APPROVAL_REVISION, -1L)

        val age =
            nowEpochMs - updatedAtEpochMs

        if (
            sinceEpochMs < 0L ||
            approvalRevision != expectedApprovalRevision ||
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
            updatedAtEpochMs = updatedAtEpochMs,
            approvalRevision = approvalRevision
        )
    }

    fun save(
        resumeKey: String,
        sinceEpochMs: Long,
        lastSmsDateEpochMs: Long,
        lastSmsId: Long,
        nowEpochMs: Long =
            System.currentTimeMillis(),
        approvalRevision: Long = approvals.revision()
    ) {
        if (
            resumeKey.isBlank() ||
            sinceEpochMs < 0L ||
            lastSmsDateEpochMs <= 0L ||
            lastSmsDateEpochMs < sinceEpochMs ||
            lastSmsId < 0L
        ) {
            return
        }

        approvals.withApprovedSenders {
            // An in-flight scan using an older sender policy must never overwrite
            // a checkpoint that a future import could incorrectly resume.
            if (approvals.revision() != approvalRevision) return@withApprovedSenders
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
            .putLong(KEY_APPROVAL_REVISION, approvalRevision)
            .apply()
        }
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
        private const val KEY_APPROVAL_REVISION = "sender_policy_revision"

        private const val MAX_CHECKPOINT_AGE_MS =
            7L * 24L * 60L * 60L * 1_000L
    }
}
