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
                        "TSTV28M001 Confirmed. Loan of Ksh575.00 repaid from M-PESA on 2/10/26 at 7:33 AM. Loan balance is Ksh0.00. Transaction cost Kshs 0.00"
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
                    "KES 575.00"
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
    fun weakLoanMarketingDoesNotBecomeUnsupportedEvidence() {
        val inspection =
            FinancialMessageInspector
                .inspect(
                    title = "Service",
                    body =
                        "Loan services are now available in the app."
                )

        assertFalse(
            inspection
                .wouldEnterMessageLab
        )
    }

    @Test
    fun sanitizedExportOmitsRawTransactionValues() {
        val inspection =
            FinancialMessageInspector
                .inspect(
                    title = "MPESA",
                    body =
                        "TSTV28M009 Confirmed. Loan of Ksh575.00 repaid from M-PESA on 2/10/26 at 7:33 AM. Loan balance is Ksh0.00. Transaction cost Kshs 0.00"
                )

        val exported =
            inspection
                .sanitizedExport()

        assertFalse(
            exported.contains(
                "TSTV28M009"
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
    }
}
