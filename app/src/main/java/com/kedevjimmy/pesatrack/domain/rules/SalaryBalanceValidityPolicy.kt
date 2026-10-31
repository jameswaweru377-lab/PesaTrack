package com.kedevjimmy.pesatrack.domain.rules

object SalaryBalanceValidityPolicy {
    private const val MAX_AGE_MS = 86_400_000L

    /** Manual balance is a point-in-time estimate, never a live ledger balance. */
    fun validManualBalance(
        confirmedAtEpochMs: Long,
        nowEpochMs: Long,
        savedCycleStartId: Long?,
        currentCycleStartId: Long?,
        savedLedgerFingerprint: String,
        currentLedgerFingerprint: String
    ): Boolean {
        val age = nowEpochMs - confirmedAtEpochMs
        return age in 0L..MAX_AGE_MS && savedCycleStartId == currentCycleStartId &&
            savedLedgerFingerprint.isNotBlank() &&
            savedLedgerFingerprint == currentLedgerFingerprint
    }
}
