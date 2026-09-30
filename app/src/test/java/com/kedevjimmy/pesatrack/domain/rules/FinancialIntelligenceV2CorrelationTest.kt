package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageNormalizer
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialSemanticAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialIntelligenceV2CorrelationTest {
    @Test
    fun semanticReferencesLinkBankAndWalletObservations() {
        val bankMessage =
            FinancialMessageNormalizer.normalize(
                title = "Example Bank",
                body =
                    "Your transaction of KES 5,000 has been " +
                        "credited to your M-Pesa account. " +
                        "Bank Ref EQA12ABC34. " +
                        "M-Pesa Ref UFT12ABC34."
            )
        val walletMessage =
            FinancialMessageNormalizer.normalize(
                title = "M-Pesa",
                body =
                    "UFT12ABC34 Confirmed. You have received " +
                        "KES 5,000 from your bank account."
            )

        val bankReferences =
            FinancialSemanticAnalyzer
                .analyze(bankMessage)
                .references
                .map { it.value }
                .toSet()
        val walletReferences =
            FinancialSemanticAnalyzer
                .analyze(walletMessage)
                .references
                .map { it.value }
                .toSet()

        val match =
            MirrorTransactionMatcher.match(
                MirrorTransactionObservation(
                    amountMinor = 500_000L,
                    occurredAtEpochMs = 1_000_000L,
                    receivedAtEpochMs = 1_000_000L,
                    sourceKind = "sms",
                    sourcePackage = "messages",
                    transactionCode = "EQA12ABC34",
                    type = "OTHER",
                    counterparty = "M-Pesa",
                    counterpartyPhone = null,
                    direction = "TRANSFER",
                    externalReferences = bankReferences,
                    accountIds =
                        setOf(
                            "account-bank",
                            "account-mpesa"
                        )
                ),
                MirrorTransactionObservation(
                    amountMinor = 500_000L,
                    occurredAtEpochMs = 1_300_000L,
                    receivedAtEpochMs = 1_300_000L,
                    sourceKind = "notification",
                    sourcePackage = "mpesa",
                    transactionCode = "UFT12ABC34",
                    type = "RECEIVED",
                    counterparty = "Example Bank",
                    counterpartyPhone = null,
                    direction = "INCOME",
                    externalReferences = walletReferences,
                    accountIds =
                        setOf("account-mpesa")
                )
            )

        assertTrue(
            bankReferences.contains("UFT12ABC34")
        )
        assertTrue(
            walletReferences.contains("UFT12ABC34")
        )
        assertTrue(match.autoLink)
        assertEquals(
            MirrorMatchReason.EXACT_SHARED_REFERENCE,
            match.reason
        )
    }

    @Test
    fun amountConflictBlocksEvenAnExactReference() {
        val first =
            MirrorTransactionObservation(
                amountMinor = 500_000L,
                occurredAtEpochMs = 1_000_000L,
                receivedAtEpochMs = 1_000_000L,
                sourceKind = "bank_sms",
                sourcePackage = "messages",
                transactionCode = "AAA12ABC34",
                type = "OTHER",
                counterparty = null,
                counterpartyPhone = null,
                direction = "TRANSFER",
                externalReferences =
                    setOf("UFT12ABC34")
            )
        val second =
            first.copy(
                amountMinor = 700_000L,
                sourceKind = "wallet_notification",
                sourcePackage = "wallet",
                transactionCode = "UFT12ABC34"
            )

        assertFalse(
            MirrorTransactionMatcher
                .match(first, second)
                .autoLink
        )
    }

    @Test
    fun codelessTimeOnlySimilarityIsNotAutoLinked() {
        val first =
            MirrorTransactionObservation(
                amountMinor = 200_000L,
                occurredAtEpochMs = 1_000_000L,
                receivedAtEpochMs = 1_000_000L,
                sourceKind = "bank_sms",
                sourcePackage = "messages",
                transactionCode = null,
                type = "OTHER",
                counterparty = null,
                counterpartyPhone = null,
                direction = "NEUTRAL"
            )
        val second =
            first.copy(
                occurredAtEpochMs = 1_003_000L,
                receivedAtEpochMs = 1_003_000L,
                sourceKind = "wallet_notification",
                sourcePackage = "wallet"
            )

        val match =
            MirrorTransactionMatcher.match(
                first,
                second
            )

        assertEquals(
            MirrorMatchConfidence.MEDIUM,
            match.confidence
        )
        assertFalse(match.autoLink)
    }
}
