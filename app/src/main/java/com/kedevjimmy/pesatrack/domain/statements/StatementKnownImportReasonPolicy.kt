package com.kedevjimmy.pesatrack.domain.statements

/** Only known table-shape refusals may lead to a *read-only* native fallback. */
object StatementKnownImportReasonPolicy {
    fun classify(reasonCode: String): StatementKnownImportOutcome = when (reasonCode) {
        "unverified_header", "unverified_columns", "unverified_physical_table" ->
            StatementKnownImportOutcome.UNSUPPORTED_LAYOUT
        "unverified_issuer" -> StatementKnownImportOutcome.ISSUER_REJECTED
        "unverified_source_account", "account_changes_between_pages", "legacy_account_unverified" ->
            StatementKnownImportOutcome.ACCOUNT_REJECTED
        "balance_mismatch", "printed_totals_mismatch", "unverified_statement_coverage",
        "invalid_source_regions", "missing_or_ambiguous_summary", "misaligned_summary",
        "legacy_balance_chain_mismatch", "legacy_grand_total_mismatch",
        "legacy_transaction_coverage_incomplete" -> StatementKnownImportOutcome.FINANCIAL_PROOF_REJECTED
        "pdf_password_rejected" -> StatementKnownImportOutcome.PASSWORD_REJECTED
        else -> StatementKnownImportOutcome.OTHER_FAILURE
    }
}
