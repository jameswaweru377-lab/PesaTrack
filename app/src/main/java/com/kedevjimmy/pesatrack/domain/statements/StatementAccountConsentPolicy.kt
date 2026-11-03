package com.kedevjimmy.pesatrack.domain.statements

/** Explicit rejection takes priority over an older account-recognition hint. */
internal object StatementAccountConsentPolicy {
    fun confirmedAccountIds(
        legacyConfirmed: Set<String>,
        b2Confirmed: Set<String>,
        b2Rejected: Set<String>
    ): Set<String> = (legacyConfirmed + b2Confirmed) - b2Rejected
}
