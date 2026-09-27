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
    fun genericDebtGrammarUnderstandsUnknownLiabilityProvider() {
        val message =
            "TST12PIPE6 Confirmed. FlexiCash Loan amount is Ksh 500.00. Access Fee charged Ksh 5.00. Total FlexiCash Loan outstanding amount is Ksh 505.00."

        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body = message
            )
        val canonical =
            interpretation.toCanonicalDraft()

        val debt = canonical.records
            .filterIsInstance<DebtMovementDraft>()
            .single()

        assertEquals(
            CanonicalFinancialRecordKind.DEBT_DRAWDOWN,
            debt.kind
        )
        assertEquals(50_000L, debt.amountMinor)
        assertEquals(
            "FlexiCash Loan",
            debt.liability.displayName
        )
        assertEquals(
            AccountOwnershipHint.UNKNOWN,
            debt.liability.ownershipHint
        )
        assertEquals(50_500L, debt.outstandingAfterMinor)
        assertEquals(
            500L,
            canonical.records
                .filterIsInstance<FeeDraft>()
                .single()
                .amountMinor
        )
        assertTrue(canonical.requiresLegacyFallback)
    }

    @Test
    fun genericDebtGrammarSeparatesRepaymentCreditAndWalletBalance() {
        val message =
            "TST12PIPE7 Confirmed. Ksh 1,381.00 from your M-PESA has been used to fully pay your outstanding Fuliza M-PESA. Available Fuliza balance limit is Ksh 1,440.00. Your M-PESA balance is Ksh 13,000.00."

        val canonical =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body = message
            ).toCanonicalDraft()

        val repayment = canonical.records
            .filterIsInstance<DebtMovementDraft>()
            .single()
        val credit = canonical.records
            .filterIsInstance<
                AvailableCreditObservationDraft
            >()
            .single()
        val balance = canonical.records
            .filterIsInstance<BalanceObservationDraft>()
            .single()

        assertEquals(
            CanonicalFinancialRecordKind.DEBT_REPAYMENT,
            repayment.kind
        )
        assertEquals(DebtCompletion.FULL, repayment.completion)
        assertEquals(0L, repayment.outstandingAfterMinor)
        assertEquals(144_000L, credit.availableCreditMinor)
        assertEquals(1_300_000L, balance.balanceMinor)
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
