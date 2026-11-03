package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatementLegacyReviewIdentityPolicyTest {
    private fun row(id: String, accountId: String, amount: Long) =
        StatementEvidenceCorrelationPolicy.Observation(
            id, StatementEvidenceCorrelationPolicy.Source.ACTIVITY, "source-$id", accountId,
            "KES", 1_700_000_000_000L, amount, "TEST123456", ownershipConfirmed = true
        )

    private fun account(type: String, number: String?) =
        StatementMatchDecisionPolicy.Ownership(1L, type, number, "Synthetic account")

    @Test fun duplicateBankAccountAliasesCannotBecomeConfirmedOrPossibleTransfers() {
        val debit = row("out", "bank-one", -500)
        val credit = row("in", "bank-two", 500)
        val mirror = row("mirror", "bank-one", -500)
        val review = StatementMatchReviewPolicy.Review(
            transfers = listOf(StatementMatchReviewPolicy.Transfer(listOf(debit), listOf(credit))),
            sameMovements = listOf(listOf(debit, mirror)), ambiguousCount = 3
        )
        val possible = listOf(StatementPossibleTransferPolicy.Suggestion(debit, credit))
        val guarded = StatementLegacyReviewIdentityPolicy.guard(
            review, possible, mapOf(
                "bank-one" to account("BANK", "123456789012"),
                "bank-two" to account("BANK", "1234 5678 9012")
            )
        )

        assertTrue(guarded.review.transfers.isEmpty())
        assertTrue(guarded.possibleTransfers.isEmpty())
        assertEquals(review.sameMovements, guarded.review.sameMovements)
        assertEquals(3, guarded.review.ambiguousCount)
    }

    @Test fun duplicateWalletPhoneAliasesAlsoCannotBeSuggestedAsTransfers() {
        val debit = row("out", "wallet-one", -900)
        val credit = row("in", "wallet-two", 900)
        val review = StatementMatchReviewPolicy.Review(
            listOf(StatementMatchReviewPolicy.Transfer(listOf(debit), listOf(credit))),
            emptyList(), 0
        )
        val guarded = StatementLegacyReviewIdentityPolicy.guard(
            review, listOf(StatementPossibleTransferPolicy.Suggestion(debit, credit)),
            mapOf(
                "wallet-one" to account("MOBILE_WALLET", "0712345678"),
                "wallet-two" to account("MOBILE_WALLET", "+254 712 345 678")
            )
        )
        assertTrue(guarded.review.transfers.isEmpty())
        assertTrue(guarded.possibleTransfers.isEmpty())
    }

    @Test fun validDistinctOwnedAccountsRemainReviewableWithoutChangingEvidence() {
        val debit = row("out", "bank", -900)
        val credit = row("in", "wallet", 900)
        val transfer = StatementMatchReviewPolicy.Transfer(listOf(debit), listOf(credit))
        val review = StatementMatchReviewPolicy.Review(listOf(transfer), emptyList(), 0)
        val suggestion = StatementPossibleTransferPolicy.Suggestion(debit, credit)
        val guarded = StatementLegacyReviewIdentityPolicy.guard(
            review, listOf(suggestion), mapOf(
                "bank" to account("BANK", "123456789012"),
                "wallet" to account("MOBILE_WALLET", "0712345678")
            )
        )
        assertEquals(listOf(transfer), guarded.review.transfers)
        assertEquals(listOf(suggestion), guarded.possibleTransfers)
    }

    @Test fun unconfirmedAccountCannotBeUsedAsTransferEndpoint() {
        val debit = row("out", "bank", -900)
        val credit = row("in", "unknown", 900)
        val transfer = StatementMatchReviewPolicy.Transfer(listOf(debit), listOf(credit))
        val guarded = StatementLegacyReviewIdentityPolicy.guard(
            StatementMatchReviewPolicy.Review(listOf(transfer), emptyList(), 0),
            listOf(StatementPossibleTransferPolicy.Suggestion(debit, credit)),
            mapOf("bank" to account("BANK", "123456789012"))
        )
        assertTrue(guarded.review.transfers.isEmpty())
        assertTrue(guarded.possibleTransfers.isEmpty())
    }
}
