package com.kedevjimmy.pesatrack.domain.statements

/** Typed outcomes of known issuer-specific import routes, not guessed exception text. */
enum class StatementKnownImportOutcome {
    UNSUPPORTED_LAYOUT,
    ISSUER_REJECTED,
    ACCOUNT_REJECTED,
    FINANCIAL_PROOF_REJECTED,
    PASSWORD_REJECTED,
    UNSUPPORTED_DEVICE,
    OTHER_FAILURE
}

/** Allows only a provisional read-only probe, never financial admission or persistence. */
object StatementFallbackProbeGate {
    fun mayRunReadOnlyProbe(outcome: StatementKnownImportOutcome): Boolean = when (outcome) {
        StatementKnownImportOutcome.UNSUPPORTED_LAYOUT -> true
        StatementKnownImportOutcome.ISSUER_REJECTED,
        StatementKnownImportOutcome.ACCOUNT_REJECTED,
        StatementKnownImportOutcome.FINANCIAL_PROOF_REJECTED,
        StatementKnownImportOutcome.PASSWORD_REJECTED,
        StatementKnownImportOutcome.UNSUPPORTED_DEVICE,
        StatementKnownImportOutcome.OTHER_FAILURE -> false
    }
}
