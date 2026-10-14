package com.kedevjimmy.pesatrack.domain.evidence

import com.kedevjimmy.pesatrack.domain.everyday.PdfBoxReceiptEvidence
import com.kedevjimmy.pesatrack.domain.everyday.PdfBoxStatementFinancialAudit
import com.kedevjimmy.pesatrack.domain.everyday.VerifiedMpesaWalletLedger
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Synthetic values only. Real customer statements and receipts are never fixtures. */
class VerifiedMpesaWalletLedgerTest {
    private fun leg(ref: String, time: String, description: String, movement: Long, balance: Long):
        PdfBoxReceiptEvidence = PdfBoxReceiptEvidence(
            page = 1, row = 1, reference = ref, dateText = time,
            description = description, cells = emptyMap(),
            paidInMinor = if (movement > 0) movement else 0L,
            withdrawnMinor = if (movement < 0) Math.negateExact(movement) else 0L,
            balanceMinor = balance
        )

    private fun timestamp(date: String): Long =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("Africa/Nairobi")
        }.parse(date)!!.time

    private fun run(rows: List<PdfBoxReceiptEvidence>): VerifiedMpesaWalletLedger.Result? {
        val balanceLegs = rows.map {
            com.kedevjimmy.pesatrack.domain.everyday.StatementBalanceLeg(
                it.reference, timestamp(it.dateText), it.movementMinor, it.balanceMinor
            )
        }
        return VerifiedMpesaWalletLedger.verify(rows, balanceLegs)
    }

    private val page29 = listOf(
        leg("QAB1234569", "2026-04-26 07:03:19", "Send Money Reversal via API", -30000L, 0L),
        leg("QAB1234568", "2026-04-25 22:48:09", "Fuliza Paybill payment", -10000L, 0L),
        leg("QAB1234568", "2026-04-25 22:48:09", "OverDraft of Credit Party", 10000L, 10000L),
        leg("QAB1234567", "2026-04-25 22:40:16", "Loan Payments", 30000L, 30000L)
    )

    @Test fun exactFulizaCyclePreservesBothLegsAndChecksSurroundingWallet() {
        val proof = run(page29)
        assertNotNull(proof)
        assertEquals(1, proof!!.separatelyVerifiedFulizaCycles)
        assertEquals(0L, proof.derivedOpeningMinor)
        assertEquals(0L, proof.derivedClosingMinor)
        assertEquals(1, proof.transitions)
        assertEquals(4, page29.size)
        assertNotNull(PdfBoxStatementFinancialAudit.verify(page29, emptyList()))
    }

    @Test fun cannotBypassOrdinaryPaymentOrDifferentAmountsAsFuliza() {
        assertNull(run(page29.map {
            if (it.description == "Fuliza Paybill payment") it.copy(description = "Ordinary Paybill") else it
        }))
        assertNull(run(page29.map {
            if (it.description == "Fuliza Paybill payment")
                it.copy(withdrawnMinor = 10100L)
            else it
        }))
        assertNull(run(page29.map {
            if (it.description == "OverDraft of Credit Party")
                it.copy(balanceMinor = 40000L)
            else it
        }))
    }

    @Test fun sharedEndingBalanceReceiptIsOneCheckingUnitNotLostFinancialRows() {
        val rows = listOf(
            leg("QAB1234569", "2026-04-27 10:00:00", "Fee", -300L, 9000L),
            leg("QAB1234568", "2026-04-26 10:00:00", "Customer Transfer", -20000L, 9300L),
            leg("QAB1234568", "2026-04-26 10:00:00", "Transfer Charge", -700L, 9300L),
            leg("QAB1234567", "2026-04-25 10:00:00", "Deposit", 30000L, 30000L)
        )
        val proof = run(rows)
        assertNotNull(proof)
        assertEquals(1, proof!!.sharedEndingBalanceGroups)
        assertEquals(0, proof.separatelyVerifiedFulizaCycles)
        assertEquals(2, proof.transitions)
        assertEquals(4, rows.size)
        assertNotNull(PdfBoxStatementFinancialAudit.verify(rows, emptyList()))
    }

    @Test fun acceptedInformationalRowsAreNotMistakenForCash() {
        val rows = listOf(
            leg("QAB1234569", "2026-04-26 10:00:00", "Withdrawal", -30000L, 0L),
            leg("QAB1234566", "2026-04-25 22:00:00", "M-Shwari Loan Request", 0L, 30000L),
            leg("QAB1234565", "2026-04-25 21:00:00", "M-Shwari Lock Withdraw to Savings", 0L, 30000L),
            leg("QAB1234564", "2026-04-25 20:00:00", "M-Shwari Lock Activate and Save from Savings", 0L, 30000L),
            leg("QAB1234567", "2026-04-25 10:00:00", "Deposit", 30000L, 30000L)
        )
        assertNotNull(run(rows))
        assertNotNull(PdfBoxStatementFinancialAudit.verify(rows, emptyList()))
        assertNull(run(rows.map {
            if (it.description == "M-Shwari Loan Request") it.copy(balanceMinor = 1200L) else it
        }))
        assertNull(run(rows.map {
            if (it.description == "M-Shwari Loan Request") it.copy(description = "Unclassified Zero Event") else it
        }))
    }

    @Test fun endpointDeclarationMismatchRemainsBlocking() {
        val evidence = page29
        val legs = evidence.map {
            com.kedevjimmy.pesatrack.domain.everyday.StatementBalanceLeg(
                it.reference, timestamp(it.dateText), it.movementMinor, it.balanceMinor
            )
        }
        assertNull(VerifiedMpesaWalletLedger.verify(evidence, legs, 100L, null))
        assertNull(VerifiedMpesaWalletLedger.verify(evidence, legs, null, 100L))
    }

    @Test fun sourceMovementMustExactlyEqualProvidedBalanceLeg() {
        val evidence = page29
        val legs = evidence.map {
            com.kedevjimmy.pesatrack.domain.everyday.StatementBalanceLeg(
                it.reference, timestamp(it.dateText), it.movementMinor, it.balanceMinor
            )
        }
        val changed = legs.toMutableList().also {
            it[1] = it[1].copy(signedMinor = -9900L)
        }
        assertNull(VerifiedMpesaWalletLedger.verify(evidence, changed))
    }
}
