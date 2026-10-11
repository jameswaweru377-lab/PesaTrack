package com.kedevjimmy.pesatrack.domain.evidence

/** An actual user receipt may stand alone; an expected payday never invokes this adapter. */
object ReceivedSalaryEntryPolicy {
    fun validate(amountMinor: Long, receivedAt: Long, now: Long, enabledReceivingAccount: Boolean) {
        require(amountMinor > 0 && receivedAt > 0 && receivedAt <= now) {
            "Enter an amount and a date when salary arrived."
        }
        require(enabledReceivingAccount) { "Choose an enabled receiving account before recording salary." }
        FinancialEvidenceContract.requireCashCandidate(FinancialEvidenceRequest(
            FinancialEvidenceSource.MANUAL, FinancialEvidenceClaim.CASH_RECEIVED,
            userConfirmed = true, ownAccountResolved = enabledReceivingAccount
        ))
    }
}
