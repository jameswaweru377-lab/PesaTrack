package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class TransferReconciliationPolicyTest {
    private val at = 1_700_000_000_000L
    private val accounts = mapOf(
        "bank" to StatementMatchDecisionPolicy.Ownership(1, "BANK", "123456789012", "Bank"),
        "wallet" to StatementMatchDecisionPolicy.Ownership(1, "MOBILE_WALLET", "0712345678", "Wallet")
    )
    private fun row(id: String, account: String, amount: Long, reference: String? = null,
                    time: Long = at) = StatementEvidenceCorrelationPolicy.Observation(
        id, StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT, "source-$id", account,
        "KES", time, amount, reference, true, sourceTotalsVerified = true)
    private val debit get() = row("d", "bank", -500000)
    private val credit get() = row("c", "wallet", 500000)
    private fun reconcile(rows: List<StatementEvidenceCorrelationPolicy.Observation>,
                          descriptions: Map<String, String> = emptyMap()) =
        TransferReconciliationPolicy.reconcile(rows, accounts, descriptions, at - 1000, at + 1000)

    @Test fun amountAndTimeNeverEstablishTransfer() {
        assertTrue(reconcile(listOf(debit, credit)).pairs.isEmpty())
    }
    @Test fun exactDirectionalRouteFindsReferenceFreePair() {
        val result = reconcile(listOf(debit, credit), mapOf("d" to "Transfer to M-PESA 0712345678"))
        assertEquals(1, result.pairs.size)
        assertEquals(TransferReconciliationPolicy.Basis.ACCOUNT_ROUTE, result.pairs.single().basis)
        assertEquals(500000L, result.pairs.single().principalMinor)
    }
    @Test fun genericMentionAndNameCannotEstablishRoute() {
        assertTrue(reconcile(listOf(debit, credit), mapOf("d" to "Wallet 0712345678 reference")).pairs.isEmpty())
        assertTrue(reconcile(listOf(debit, credit), mapOf("d" to "Transfer to Wallet James")).pairs.isEmpty())
    }
    @Test fun referenceLinksOppositeOwnedMovements() {
        assertEquals(TransferReconciliationPolicy.Basis.SHARED_REFERENCE,
            reconcile(listOf(debit.copy(reference = "ABC123456"), credit.copy(reference = "ABC123456"))).pairs.single().basis)
    }
    @Test fun thirdIdenticalMovementMakesRouteAmbiguous() {
        val result = reconcile(listOf(debit, credit, credit.copy(id = "c2", sourceId = "other")),
            mapOf("d" to "Transfer to M-PESA 0712345678"))
        assertTrue(result.pairs.isEmpty())
        assertTrue(result.ambiguousObservationCount > 0)
    }
    @Test fun protectedCompetitorMustNotMakeAnotherPairLookUnique() {
        val result = reconcile(listOf(debit, credit, credit.copy(id = "c2", sourceId = "other", roleProtected = true)),
            mapOf("d" to "Transfer to M-PESA 0712345678"))
        assertTrue(result.pairs.isEmpty())
    }
    @Test fun protectedMovementCannotBeConfirmed() {
        assertTrue(reconcile(listOf(debit.copy(roleProtected = true), credit),
            mapOf("d" to "Transfer to M-PESA 0712345678")).pairs.isEmpty())
    }
    @Test fun sourceDuplicatesStayTwoObservationsOfOneLeg() {
        val d = debit.copy(reference = "ABC123456")
        val result = reconcile(listOf(d, d.copy(id = "d2", sourceId = "second"), credit.copy(reference = "ABC123456")))
        assertEquals(1, result.pairs.size)
        assertEquals(2, result.pairs.single().outgoing.size)
    }
    @Test fun oppositeDirectionNarrationIsNotRouteProof() {
        assertTrue(reconcile(listOf(debit, credit), mapOf("d" to "Transfer from M-PESA 0712345678")).pairs.isEmpty())
    }
    @Test fun missingCounterpartRemainsOneSided() {
        val result = reconcile(listOf(debit), mapOf("d" to "Transfer to M-PESA 0712345678"))
        assertEquals(1, result.unmatchedRoutes.size)
        assertTrue(result.pairs.isEmpty())
    }
    @Test fun currencyOrPrincipalDifferenceIsNotAssumedFee() {
        for (other in listOf(credit.copy(currency = "USD"), credit.copy(movementMinor = 499000))) {
            assertTrue(reconcile(listOf(debit, other), mapOf("d" to "Transfer to M-PESA 0712345678")).pairs.isEmpty())
        }
    }
    @Test fun timeSupportsButDoesNotProvePair() {
        val tomorrow = credit.copy(occurredAt = at + 24 * 60 * 60 * 1000L)
        assertEquals(1, reconcile(listOf(debit, tomorrow), mapOf("d" to "Transfer to M-PESA 0712345678")).pairs.size)
        assertTrue(reconcile(listOf(debit, tomorrow.copy(occurredAt = at + 49 * 60 * 60 * 1000L)),
            mapOf("d" to "Transfer to M-PESA 0712345678")).pairs.isEmpty())
    }
    @Test fun contextOnlyPairNotShownInPeriod() {
        assertTrue(reconcile(listOf(debit.copy(occurredAt = at + 2000), credit.copy(occurredAt = at + 3000)),
            mapOf("d" to "Transfer to M-PESA 0712345678")).pairs.isEmpty())
    }
    @Test fun narrationChangeInvalidatesDecisionIdentity() {
        val a = reconcile(listOf(debit, credit), mapOf("d" to "Transfer to M-PESA 0712345678")).pairs.single()
        val b = reconcile(listOf(debit, credit), mapOf("d" to "Transfer to M-PESA 0712345678 corrected")).pairs.single()
        assertNotEquals(a.fingerprint, b.fingerprint)
    }
    @Test fun ownershipRevisionInvalidatesDecisionIdentity() {
        val rows = listOf(debit, credit)
        val descriptions = mapOf("d" to "Transfer to M-PESA 0712345678")
        val first = reconcile(rows, descriptions).pairs.single()
        val changed = accounts + ("wallet" to accounts.getValue("wallet").copy(revision = 2))
        assertNotEquals(first.fingerprint, TransferReconciliationPolicy.reconcile(rows, changed, descriptions,
            at - 1000, at + 1000).pairs.single().fingerprint)
    }
    @Test fun duplicateConfiguredIdentifierCannotEstablishRoute() {
        val duplicate = accounts + ("another" to accounts.getValue("wallet"))
        assertTrue(TransferReconciliationPolicy.reconcile(listOf(debit, credit), duplicate,
            mapOf("d" to "Transfer to M-PESA 0712345678"), at - 1000, at + 1000).pairs.isEmpty())
    }
    @Test fun duplicateFullBankIdentityCannotLookLikeTwoSidesOfATransfer() {
        val accountsWithDuplicate = accounts + ("another-bank" to
            StatementMatchDecisionPolicy.Ownership(1, "BANK", "1234 5678 9012", "Other bank entry"))
        val rows = listOf(
            row("out", "bank", -500000, "ABC123456"),
            row("in", "another-bank", 500000, "ABC123456")
        )
        val result = TransferReconciliationPolicy.reconcile(rows, accountsWithDuplicate,
            emptyMap(), at - 1000, at + 1000)
        assertTrue("Two records for the same full bank account are not an internal transfer", result.pairs.isEmpty())
        assertEquals(2, result.withheldObservationCount)
    }
    @Test fun duplicateWalletAliasesCannotUseSharedReceiptToCreateTransfer() {
        val accountsWithDuplicate = accounts + ("other-wallet" to
            StatementMatchDecisionPolicy.Ownership(1, "MOBILE_WALLET", "+254712345678", "Second wallet entry"))
        val rows = listOf(
            row("out", "wallet", -500000, "ABC123456"),
            row("in", "other-wallet", 500000, "ABC123456")
        )
        val result = TransferReconciliationPolicy.reconcile(rows, accountsWithDuplicate,
            emptyMap(), at - 1000, at + 1000)
        assertTrue("Local and international formats are the same wallet", result.pairs.isEmpty())
        assertEquals(2, result.withheldObservationCount)
    }
    @Test fun sameDigitsAcrossDifferentAccountTypesRemainDistinct() {
        val differentTypes = mapOf(
            "bank" to StatementMatchDecisionPolicy.Ownership(1, "BANK", "254712345678", "Bank"),
            "wallet" to StatementMatchDecisionPolicy.Ownership(1, "MOBILE_WALLET", "+254712345678", "Wallet")
        )
        val rows = listOf(
            row("out", "bank", -500000, "ABC123456"),
            row("in", "wallet", 500000, "ABC123456")
        )
        assertEquals(1, TransferReconciliationPolicy.reconcile(rows, differentTypes,
            emptyMap(), at - 1000, at + 1000).pairs.size)
    }
    @Test fun maskedAndPartialNumbersNeverMatch() {
        for (text in listOf("to M-PESA ****0712345678", "to M-PESA 07123456789", "to M-PESA 0712345678.99")) {
            assertTrue(text, reconcile(listOf(debit, credit), mapOf("d" to text)).pairs.isEmpty())
        }
    }
    @Test fun internationalWalletAliasWorks() {
        assertEquals(1, reconcile(listOf(debit, credit), mapOf("d" to "Transfer to M-PESA +254712345678")).pairs.size)
    }
    @Test fun unownedOrUnverifiedRowsCannotPair() {
        for (other in listOf(credit.copy(ownershipConfirmed = false), credit.copy(sourceTotalsVerified = false))) {
            assertTrue(reconcile(listOf(debit, other), mapOf("d" to "Transfer to M-PESA 0712345678")).pairs.isEmpty())
        }
    }
    @Test fun invalidMoneyDoesNotOverflow() {
        assertTrue(reconcile(listOf(debit.copy(movementMinor = Long.MIN_VALUE), credit),
            mapOf("d" to "Transfer to M-PESA 0712345678")).pairs.isEmpty())
    }
    @Test fun orderDoesNotChangeDecisionIdentity() {
        val descriptions = mapOf("d" to "Transfer to M-PESA 0712345678")
        assertEquals(reconcile(listOf(debit, credit), descriptions).pairs.single().fingerprint,
            reconcile(listOf(credit, debit), descriptions).pairs.single().fingerprint)
    }
    @Test fun explicitLoanReversalFeeOrFailureNarrationCannotBecomeTransfer() {
        for (prefix in listOf("Loan repayment", "Reversal", "Transaction fee", "Failed", "Pending", "M-Shwari", "Fuliza")) {
            assertTrue(prefix, reconcile(listOf(debit, credit),
                mapOf("d" to "$prefix to M-PESA 0712345678")).pairs.isEmpty())
        }
    }
    @Test fun longerFormattedIdentifierCannotMatchOwnedPrefix() {
        for (suffix in listOf(" 9012", "/9012", "_9012", " / 9012", ".9012", "-9012")) {
            assertTrue(suffix, reconcile(listOf(debit, credit),
                mapOf("d" to "Transfer to M-PESA 0712345678$suffix")).pairs.isEmpty())
        }
    }
    @Test fun ordinarySentencePunctuationDoesNotInvalidateExactAccountRoute() {
        for (suffix in listOf(".", ", completed", "; completed")) {
            assertEquals(suffix, 1, reconcile(listOf(debit, credit),
                mapOf("d" to "Transfer to M-PESA 0712345678$suffix")).pairs.size)
        }
    }
}
