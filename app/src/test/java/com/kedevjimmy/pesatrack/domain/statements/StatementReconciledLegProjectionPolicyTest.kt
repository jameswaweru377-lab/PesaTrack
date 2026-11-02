package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class StatementReconciledLegProjectionPolicyTest {
    private fun row(id: String, source: StatementEvidenceCorrelationPolicy.Source, account: String,
                    amount: Long, reference: String = "QBC1234567", protected: Boolean = false) =
        StatementEvidenceCorrelationPolicy.Observation(id, source, id, account, "KES", 1_790_000_000_000L,
            amount, reference, ownershipConfirmed = true, roleProtected = protected,
            sourceTotalsVerified = source != StatementEvidenceCorrelationPolicy.Source.ACTIVITY)

    @Test fun confirmedBankWalletSmsTriadHasTwoCashLegsAndZeroExternalIncome() {
        val bank = row("bank", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT, "equity", -500_000)
        val wallet = row("wallet", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT, "mpesa", 500_000)
        val sms = row("sms", StatementEvidenceCorrelationPolicy.Source.ACTIVITY, "mpesa", 500_000)
        val review = StatementMatchReviewPolicy.Review(
            listOf(StatementMatchReviewPolicy.Transfer(listOf(bank), listOf(wallet, sms))), emptyList(), 0)
        val decision = mapOf(setOf("bank", "wallet", "sms") to StatementReconciliationPreviewPolicy.Decision.CONFIRMED)
        val result = StatementReconciledLegProjectionPolicy.project(review, decision)
        assertEquals(3, result.observationCount)
        assertEquals(2, result.uniqueCashLegCount)
        assertEquals(1, result.confirmedInternalTransfers)
        assertEquals(500_000L, result.confirmedInternalTransferMinor)
        assertEquals(0L, result.externalIncomeFromConfirmedTransfersMinor)
        assertEquals(0L, result.externalSpendingFromConfirmedTransfersMinor)
    }

    @Test fun undoOrRejectRestoresNoAccountingProjection() {
        val bank = row("bank", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT, "equity", -500_000)
        val wallet = row("wallet", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT, "mpesa", 500_000)
        val review = StatementMatchReviewPolicy.Review(
            listOf(StatementMatchReviewPolicy.Transfer(listOf(bank), listOf(wallet))), emptyList(), 0)
        val confirmed = mapOf(setOf("bank", "wallet") to StatementReconciliationPreviewPolicy.Decision.CONFIRMED)
        assertEquals(1, StatementReconciledLegProjectionPolicy.project(review, confirmed).confirmedInternalTransfers)
        assertEquals(0, StatementReconciledLegProjectionPolicy.project(review, emptyMap()).confirmedInternalTransfers)
        assertEquals(0, StatementReconciledLegProjectionPolicy.project(review,
            mapOf(setOf("bank", "wallet") to StatementReconciliationPreviewPolicy.Decision.NOT_SAME)).confirmedInternalTransfers)
    }

    @Test fun protectedFeesAndUnverifiedStatementsCannotBeCounted() {
        val bank = row("bank", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT, "equity", -500_000)
        val fee = row("fee", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT, "mpesa", 500_000, protected = true)
        val review = StatementMatchReviewPolicy.Review(
            listOf(StatementMatchReviewPolicy.Transfer(listOf(bank), listOf(fee))), emptyList(), 1)
        val result = StatementReconciledLegProjectionPolicy.project(review,
            mapOf(setOf("bank", "fee") to StatementReconciliationPreviewPolicy.Decision.CONFIRMED))
        assertEquals(0, result.confirmedInternalTransfers)
        assertEquals(0L, result.confirmedInternalTransferMinor)
        assertEquals(1, result.ambiguousObservationCount)
        val unverified = fee.copy(roleProtected = false, sourceTotalsVerified = false)
        assertEquals(0, StatementReconciledLegProjectionPolicy.project(
            review.copy(transfers = listOf(StatementMatchReviewPolicy.Transfer(listOf(bank), listOf(unverified)))),
            mapOf(setOf("bank", "fee") to StatementReconciliationPreviewPolicy.Decision.CONFIRMED)).confirmedInternalTransfers)
    }

    @Test fun sameAmountNearbyButDifferentReferenceNeverBecomesCopy() {
        val a = row("a", StatementEvidenceCorrelationPolicy.Source.ACTIVITY, "mpesa", -5_000)
        val b = row("b", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT, "mpesa", -5_000,
            reference = "QBC7654321")
        val review = StatementMatchReviewPolicy.Review(emptyList(), listOf(listOf(a, b)), 0)
        val result = StatementReconciledLegProjectionPolicy.project(review,
            mapOf(setOf("a", "b") to StatementReconciliationPreviewPolicy.Decision.CONFIRMED))
        assertEquals(0, result.confirmedSameLegGroups)
        assertEquals(0, result.uniqueCashLegCount)
    }

    @Test fun overflowOrOverlappingGroupsCannotProduceMisleadingAmounts() {
        val out = row("out", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT, "equity", Long.MIN_VALUE)
        val `in` = row("in", StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT, "mpesa", Long.MAX_VALUE)
        val review = StatementMatchReviewPolicy.Review(
            listOf(StatementMatchReviewPolicy.Transfer(listOf(out), listOf(`in`))), emptyList(), 0)
        val result = StatementReconciledLegProjectionPolicy.project(review,
            mapOf(setOf("out", "in") to StatementReconciliationPreviewPolicy.Decision.CONFIRMED))
        assertEquals(0L, result.confirmedInternalTransferMinor)
        assertEquals(0, result.confirmedInternalTransfers)
    }
}
