package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class StatementKnownImportReasonPolicyTest {
    @Test fun unsupportedTableLayoutCanOnlyStartReadOnlyProbe() {
        for (reason in listOf("unverified_header", "unverified_columns", "unverified_physical_table")) {
            val outcome = StatementKnownImportReasonPolicy.classify(reason)
            assertEquals(StatementKnownImportOutcome.UNSUPPORTED_LAYOUT, outcome)
            assertTrue(StatementFallbackProbeGate.mayRunReadOnlyProbe(outcome))
        }
    }

    @Test fun unverifiedIssuerNeverTriggersFallback() {
        val outcome = StatementKnownImportReasonPolicy.classify("unverified_issuer")
        assertEquals(StatementKnownImportOutcome.ISSUER_REJECTED, outcome)
        assertFalse(StatementFallbackProbeGate.mayRunReadOnlyProbe(outcome))
    }

    @Test fun accountBalanceTotalsPasswordAndUnknownErrorsNeverTriggerFallback() {
        val refused = mapOf(
            "unverified_source_account" to StatementKnownImportOutcome.ACCOUNT_REJECTED,
            "account_changes_between_pages" to StatementKnownImportOutcome.ACCOUNT_REJECTED,
            "balance_mismatch" to StatementKnownImportOutcome.FINANCIAL_PROOF_REJECTED,
            "printed_totals_mismatch" to StatementKnownImportOutcome.FINANCIAL_PROOF_REJECTED,
            "unverified_statement_coverage" to StatementKnownImportOutcome.FINANCIAL_PROOF_REJECTED,
            "pdf_password_rejected" to StatementKnownImportOutcome.PASSWORD_REJECTED,
            "unverified_currency" to StatementKnownImportOutcome.OTHER_FAILURE,
            "" to StatementKnownImportOutcome.OTHER_FAILURE,
            "unsupported_provider" to StatementKnownImportOutcome.OTHER_FAILURE,
            "some_future_parser_reason" to StatementKnownImportOutcome.OTHER_FAILURE
        )
        for ((reason, expected) in refused) {
            val outcome = StatementKnownImportReasonPolicy.classify(reason)
            assertEquals(reason, expected, outcome)
            assertFalse(reason, StatementFallbackProbeGate.mayRunReadOnlyProbe(outcome))
        }
    }
}
