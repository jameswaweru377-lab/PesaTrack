package com.kedevjimmy.pesatrack.domain.messagepack

import com.kedevjimmy.pesatrack.domain.interpretation.FinancialActionCandidateKind
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialCandidateExtractor
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpreter
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageNormalizer
import com.kedevjimmy.pesatrack.domain.interpretation.MessagePackCandidateBridge
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.ReceiptEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.TransferEventFact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BankMessagePackBridgeTest {
    @Test
    fun kcbCardTemplateFillsMissingPaymentCandidate() {
        val body =
            "KES 2,300.00 transaction made on KCB card 1234 at SAMPLE STORE on 1/10/26."
        val normalized =
            FinancialMessageNormalizer
                .normalize(
                    title = "KCB",
                    body = body
                )
        val extracted =
            FinancialCandidateExtractor
                .extract(normalized)

        assertTrue(
            extracted.actions.isEmpty()
        )

        val bridged =
            MessagePackCandidateBridge
                .enrich(
                    candidates = extracted,
                    match =
                        KenyaMessagePack.route(
                            title = "KCB",
                            body = body
                        ),
                    message = normalized
                )

        assertEquals(
            FinancialActionCandidateKind
                .PAYMENT_PAID_TO,
            bridged.actions
                .single()
                .kind
        )

        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "KCB",
                    body = body
                )
        val event =
            interpretation.events
                .single() as
                PaymentEventFact

        assertEquals(
            230_000L,
            event.amountMinor
        )
        assertEquals(
            "SAMPLE STORE",
            event.counterparty
        )
        assertEquals(
            "KENYAN_BANK_KCB",
            event.source.providerKey
        )
        assertEquals(
            "ke.kcb.card.purchase",
            interpretation
                .parseProvenance
                .templateId
        )
    }

    @Test
    fun kcbAccountCreditTemplateFillsMissingReceiptCandidate() {
        val body =
            "Dear customer, your acc 1234 has been credited KES 5,000.00 at SAMPLE EMPLOYER at 10:20 AM on 1/10/26."

        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "KCB",
                    body = body
                )
        val event =
            interpretation.events
                .single() as
                ReceiptEventFact

        assertEquals(
            500_000L,
            event.amountMinor
        )
        assertEquals(
            "SAMPLE EMPLOYER",
            event.counterparty
        )
        assertEquals(
            "KENYAN_BANK_KCB",
            event.destination
                .providerKey
        )
    }

    @Test
    fun kcbSalaryCreditUsesExtractedPayer() {
        val body =
            "Your account has been credited with KES 60,000.00-Salary Payment 00-SAMPLE EMPLOYER. KCB Channel: CH001. Date: 1/10/26."

        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "KCB",
                    body = body
                )
        val event =
            interpretation.events
                .single() as
                ReceiptEventFact

        assertEquals(
            6_000_000L,
            event.amountMinor
        )
        assertEquals(
            "SAMPLE EMPLOYER",
            event.counterparty
        )
        assertEquals(
            "KENYAN_BANK_KCB",
            event.destination
                .providerKey
        )
    }

    @Test
    fun kcbSendToMpesaTemplateFillsExplicitTransferDestination() {
        val body =
            "SEND TO M-PESA request of KES 1,500.00 from 123456 has been processed. M-PESA REF: MPREF123. Transaction cost KES 20.00 Incl tax."

        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "KCB",
                    body = body
                )
        val event =
            interpretation.events
                .single() as
                TransferEventFact

        assertEquals(
            150_000L,
            event.amountMinor
        )
        assertEquals(
            "KENYAN_BANK_KCB",
            event.source.providerKey
        )
        assertEquals(
            "SAFARICOM_MPESA",
            event.destination
                .providerKey
        )
    }

    @Test
    fun pesalinkNetworkMessageDoesNotInventOwnedPesalinkAccount() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "PESALINK",
                    body =
                        "KES 4,000.00 sent to SAMPLE PERSON A/C 654321."
                )

        assertEquals(
            MessageParseRoute.TEMPLATE,
            interpretation
                .parseProvenance
                .route
        )
        assertEquals(
            "ke.pesalink.transfer.sent",
            interpretation
                .parseProvenance
                .templateId
        )
        assertTrue(
            interpretation.events.isEmpty()
        )
    }
}
