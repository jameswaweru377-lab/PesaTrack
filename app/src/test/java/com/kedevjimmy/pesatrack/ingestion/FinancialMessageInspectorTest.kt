package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.messagepack.MessageParseRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialMessageInspectorTest {
    @Test
    fun inspectsRealMshwariRepaymentThroughCurrentTemplate() {
        val inspection =
            FinancialMessageInspector
                .inspect(
                    title = "MPESA",
                    body =
                        "TSTV30M001 Confirmed. Loan of Ksh575.00 repaid from M-PESA on 2/10/26 at 7:33 AM. Loan balance is Ksh0.00. Transaction cost Kshs 0.00"
                )

        assertTrue(
            inspection.recognized
        )
        assertEquals(
            MessageParseRoute.TEMPLATE,
            inspection.parseRoute
        )
        assertEquals(
            "ke.mshwari.loan.repaid-from-mpesa",
            inspection.templateId
        )
        assertTrue(
            inspection.eventSummaries
                .single()
                .contains(
                    "DEBT_REPAYMENT"
                )
        )
        assertTrue(
            inspection.eventSummaries
                .single()
                .contains(
                    "remaining KES 0.00"
                )
        )
        assertFalse(
            inspection
                .wouldEnterMessageLab
        )
    }

    @Test
    fun savedConfirmedFinancialDecisionAffectsMessageLabDiagnosis() {
        val inspection =
            FinancialMessageInspector
                .inspect(
                    title = "Service",
                    body =
                        "Loan services are now available in the app.",
                    savedUserDecision =
                        MessageLabUserDecision
                            .CONFIRMED_FINANCIAL
                )

        assertFalse(
            inspection
                .heuristicUnsupported
        )
        assertTrue(
            inspection
                .wouldEnterMessageLab
        )
    }

    @Test
    fun savedNotFinancialDecisionSuppressesMessageLabDiagnosis() {
        val inspection =
            FinancialMessageInspector
                .inspect(
                    title = "Service",
                    body =
                        "Loan services are now available in the app.",
                    savedUserDecision =
                        MessageLabUserDecision
                            .NOT_FINANCIAL
                )

        assertFalse(
            inspection
                .wouldEnterMessageLab
        )
        assertEquals(
            MessageLabUserDecision
                .NOT_FINANCIAL,
            inspection
                .savedUserDecision
        )
    }

    @Test
    fun sanitizedExportNeverCopiesRawSender() {
        val inspection =
            FinancialMessageInspector
                .inspect(
                    title =
                        "+254712345678",
                    body =
                        "Loan services are now available in the app."
                )

        val exported =
            inspection
                .sanitizedExport()

        assertFalse(
            exported.contains(
                "+254712345678"
            )
        )
        assertTrue(
            exported.contains(
                "\"sender\": \"<SENDER>\""
            )
        )
    }

    @Test
    fun sanitizedExportRedactsTemplateExtractedCounterparty() {
        val inspection =
            FinancialMessageInspector
                .inspect(
                    title = "KCB",
                    body =
                        "KES 1200.00 transaction made on KCB card 1234 at PRIVATE MERCHANT on 2/10/26."
                )

        assertEquals(
            "ke.kcb.card.purchase",
            inspection.templateId
        )

        val exported =
            inspection
                .sanitizedExport()

        assertFalse(
            exported.contains(
                "PRIVATE MERCHANT"
            )
        )
        assertFalse(
            exported.contains(
                "1200.00"
            )
        )
        assertTrue(
            exported.contains(
                "<ENTITY>"
            )
        )
        assertTrue(
            exported.contains(
                "<AMOUNT>"
            )
        )
    }

    @Test
    fun sanitizedExportOmitsRawTransactionValues() {
        val inspection =
            FinancialMessageInspector
                .inspect(
                    title = "MPESA",
                    body =
                        "TSTV30M009 Confirmed. Loan of Ksh575.00 repaid from M-PESA on 2/10/26 at 7:33 AM. Loan balance is Ksh0.00. Transaction cost Kshs 0.00",
                    savedSemanticLabel =
                        MessageLabSemanticLabel
                            .LOAN_REPAYMENT
                )

        val exported =
            inspection
                .sanitizedExport()

        assertFalse(
            exported.contains(
                "TSTV30M009"
            )
        )
        assertFalse(
            exported.contains(
                "575.00"
            )
        )
        assertTrue(
            exported.contains(
                "<TX_CODE>"
            )
        )
        assertTrue(
            exported.contains(
                "<AMOUNT>"
            )
        )
        assertTrue(
            exported.contains(
                "LOAN_REPAYMENT"
            )
        )
    }
}
