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
    fun paidToWithoutAccountReferenceResolvesAsMerchantPayment() {
        val normalized =
            FinancialMessageNormalizer.normalize(
                title = "MPESA",
                body =
                    "TST12PIPE4 Confirmed. Ksh 850.00 paid to SAMPLE SHOP on 28/9/26 at 7:25 PM. New M-PESA balance is Ksh 8,800.00. Transaction cost, Ksh 0.00."
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

        val payment =
            classified.events.single() as
                PaymentEventFact

        assertEquals(
            PaymentRouteHint.MERCHANT,
            payment.routeHint
        )
        assertEquals("SAMPLE SHOP", payment.counterparty)
        assertEquals(null, payment.accountReference)
    }

    @Test
    fun paidToForAccountResolvesAsBillPayment() {
        val normalized =
            FinancialMessageNormalizer.normalize(
                title = "MPESA",
                body =
                    "TST12PIPE5 Confirmed. Ksh 1,200.00 paid to SAMPLE UTILITY for account 123456 on 28/9/26 at 7:28 PM. New M-PESA balance is Ksh 7,600.00. Transaction cost, Ksh 20.00."
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

        val payment =
            classified.events.single() as
                PaymentEventFact

        assertEquals(
            PaymentRouteHint.BILL,
            payment.routeHint
        )
        assertEquals(
            "SAMPLE UTILITY",
            payment.counterparty
        )
        assertEquals("123456", payment.accountReference)
        assertEquals(2_000L, payment.feeMinor)
        assertTrue(
            payment.evidence.any {
                it.field ==
                    FinancialField.ACCOUNT_REFERENCE
            }
        )
    }

    @Test
    fun paymentCanResolveFromKnownOwnedBalanceWithoutMpesaTitle() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "NCBA",
                body =
                    "TST12ACCT1 Confirmed. Ksh 400.00 sent to SAMPLE PERSON 0712000000 on 28/9/26 at 7:10 PM. M-Shwari balance is Ksh 5,000.00."
            )

        val payment =
            interpretation.events.single() as
                PaymentEventFact

        assertEquals(
            "NCBA_MSHWARI_SAVINGS",
            payment.source.providerKey
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            payment.source.ownershipHint
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

    @Test
    fun maskedPhoneIsNotPartOfPersonCounterpartyName() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body =
                    "TST12PIPE6 Confirmed. Ksh 900.00 sent to SAMPLE PERSON 2547****321 on 28/9/26 at 8:10 AM. New M-PESA balance is Ksh 11,950.00."
            )

        val payment =
            interpretation.events.single() as
                PaymentEventFact

        assertEquals(
            "SAMPLE PERSON",
            payment.counterparty
        )
        assertEquals(null, payment.counterpartyPhone)
    }

    @Test
    fun transferredToUnknownAccountStaysTransferForReview() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body =
                    "TST12PIPE7 Confirmed. Ksh 10,000.00 transferred to KCB ACCOUNT on 28/9/26 at 9:54 PM. New M-PESA balance is Ksh 2,500.00."
            )

        val transfer =
            interpretation.events.single() as
                TransferEventFact

        assertEquals(
            "SAFARICOM_MPESA",
            transfer.source.providerKey
        )
        assertEquals(
            AccountOwnershipHint.UNKNOWN,
            transfer.destination.ownershipHint
        )
        assertEquals("KCB", transfer.destination.displayName)
        assertTrue(
            interpretation
                .consumesLegacyTransactionParsing
        )
    }

}
