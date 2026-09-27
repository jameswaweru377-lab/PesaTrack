package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialInterpretationPipelineTest {
    @Test
    fun normalizerAndExtractorKeepMeaningSeparate() {
        val normalized =
            FinancialMessageNormalizer.normalize(
                title = "  MPESA  ",
                body =
                    "TST12PIPE1  Confirmed.   Ksh 450.00 sent to SAMPLE PERSON 0712000000 on 28/9/26 at 7:10 PM. New M-PESA balance is Ksh 7,650.00. Transaction cost, Ksh 7.00."
            )

        assertEquals("MPESA", normalized.title)
        assertTrue(normalized.hasCurrencyHint)

        val candidates =
            FinancialCandidateExtractor.extract(
                normalized
            )

        assertEquals(
            "TST12PIPE1",
            candidates.transactionCode?.value
        )
        assertEquals(1, candidates.balances.size)
        assertEquals(700L, candidates.fee?.amountMinor)

        val action = candidates.actions.single {
            it.kind ==
                FinancialActionCandidateKind.PAYMENT_SENT_TO
        }
        assertEquals(45_000L, action.amountMinor)
        assertEquals("SAMPLE PERSON", action.partyLabel)
        assertEquals("+254712000000", action.partyPhone)
    }

    @Test
    fun resolverTurnsExplicitAccountLanguageIntoTransfer() {
        val normalized =
            FinancialMessageNormalizer.normalize(
                title = "MPESA",
                body =
                    "TST12PIPE2 Confirmed. Ksh 5,000.00 transferred from NewWallet account on 28/9/26 at 7:00 PM. NewWallet balance is Ksh 12,400.00. M-PESA balance is Ksh 8,100.00."
            )
        val resolved =
            FinancialRelationshipResolver.resolve(
                message = normalized,
                candidates =
                    FinancialCandidateExtractor.extract(
                        normalized
                    )
            )

        val relationship =
            resolved.relationship as
                ResolvedTransferRelationship

        assertEquals(
            "NewWallet",
            relationship.source.displayName
        )
        assertEquals(
            "SAFARICOM_MPESA",
            relationship.destination.providerKey
        )
    }

    @Test
    fun classifierCreatesReceiptWithoutCallingItIncome() {
        val normalized =
            FinancialMessageNormalizer.normalize(
                title = "MPESA",
                body =
                    "TST12PIPE3 Confirmed. You have received Ksh 2,000.00 from SAMPLE PERSON 0712000001 on 28/9/26 at 7:20 PM. New M-PESA balance is Ksh 9,650.00."
            )
        val classified =
            FinancialEventClassifier.classify(
                FinancialRelationshipResolver.resolve(
                    message = normalized,
                    candidates =
                        FinancialCandidateExtractor.extract(
                            normalized
                        )
                )
            )

        val receipt =
            classified.events.single() as
                ReceiptEventFact
        assertEquals(
            FinancialEventKind.RECEIPT,
            receipt.kind
        )
        assertEquals("SAMPLE PERSON", receipt.counterparty)
        assertNotNull(
            receipt.evidence.firstOrNull {
                it.field ==
                    FinancialField.COUNTERPARTY
            }
        )
    }
}
