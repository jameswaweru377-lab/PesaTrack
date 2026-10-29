package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatementBankCategoryLabelPolicyTest {
    @Test fun reviewedDebitAcceptsOnlyExpenseLabels() {
        assertTrue(StatementBankCategoryLabelPolicy.canLabel(-12_500L, "EXPENSE"))
        listOf("INCOME", "ALLOCATION", "TRANSFER", "SYSTEM", "unknown")
            .forEach { assertFalse(StatementBankCategoryLabelPolicy.canLabel(-12_500L, it)) }
    }

    @Test fun reviewedCreditAcceptsOnlyIncomeLabels() {
        assertTrue(StatementBankCategoryLabelPolicy.canLabel(53_000L, "INCOME"))
        listOf("EXPENSE", "ALLOCATION", "TRANSFER", "SYSTEM", "unknown")
            .forEach { assertFalse(StatementBankCategoryLabelPolicy.canLabel(53_000L, it)) }
    }

    @Test fun informationalOrZeroMovementCannotReceiveFinancialLabel() {
        listOf("EXPENSE", "INCOME", "TRANSFER", "SYSTEM")
            .forEach { assertFalse(StatementBankCategoryLabelPolicy.canLabel(0L, it)) }
    }

    @Test fun bankLabelCannotMakeUnreferencedEquityNarrationLinkToActivity() {
        val t = 1_780_000_000_000L
        val bank = StatementEvidenceCorrelationPolicy.Observation(
            "bank-leg", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
            "bank-source", "my-bank", "KES", t, -25_000L, null, true
        )
        val sms = StatementEvidenceCorrelationPolicy.Observation(
            "sms-leg", StatementEvidenceCorrelationPolicy.Source.ACTIVITY,
            "activity-source", "my-bank", "KES", t, -25_000L, "MNP1234567", true
        )
        assertTrue(StatementBankCategoryLabelPolicy.canLabel(bank.movementMinor, "EXPENSE"))
        val result = StatementEvidenceCorrelationPolicy.reconcile(
            listOf(bank, sms), setOf("my-bank")
        )
        assertTrue(result.uniqueLinks.isEmpty())
        assertEquals(setOf("bank-leg", "sms-leg"), result.unlinkedIds)
    }
}
