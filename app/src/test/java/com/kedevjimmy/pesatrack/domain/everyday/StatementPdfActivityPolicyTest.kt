package com.kedevjimmy.pesatrack.domain.everyday

import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StatementPdfActivityPolicyTest {
    private fun line(
        row: Int,
        paidIn: Long = 0L,
        withdrawn: Long = 0L
    ) = StatementActivityLine(
        page = 1,
        row = row,
        reference = "QAB12345${row.toString().padStart(2, '0')}",
        occurredAt = 1_791_234_000_000L + row * 60_000L,
        description = "Verified source movement $row",
        paidInMinor = paidIn,
        withdrawnMinor = withdrawn,
        balanceMinor = 200_000L,
        sourceCells = listOf("code", "date", "movement", "Completed", "", "", "balance")
    )

    @Test
    fun onlyVerifiedNonzeroMovementsBecomeActivityCandidates() {
        val lines = listOf(
            line(1, paidIn = 52_000L),
            line(2, withdrawn = 10_500L),
            line(3)
        )

        val result = StatementPdfActivityPolicy.toActivityRows(
            lines,
            StatementActivityProof(receiptRows = 3, cashRows = 2, totalsVerified = true),
            AccountType.MOBILE_WALLET
        )

        assertEquals(
            listOf(52_000L, -10_500L),
            result.map { it.signedMinor }
        )
        assertEquals(listOf("QAB1234501", "QAB1234502"), result.map { it.reference })
        assertEquals(listOf(1_791_234_060_000L, 1_791_234_120_000L), result.map { it.date })
    }

    @Test
    fun incompleteSourceProofCannotCreateActivityCandidates() {
        assertThrows(IllegalArgumentException::class.java) {
            StatementPdfActivityPolicy.toActivityRows(
                listOf(line(1, paidIn = 100L)),
                StatementActivityProof(receiptRows = 1, cashRows = 1, totalsVerified = false),
                AccountType.MOBILE_WALLET
            )
        }
    }

    @Test
    fun inconsistentSourceRowCountsCannotCreateActivityCandidates() {
        assertThrows(IllegalArgumentException::class.java) {
            StatementPdfActivityPolicy.toActivityRows(
                listOf(line(1, paidIn = 100L), line(2)),
                StatementActivityProof(receiptRows = 1, cashRows = 1, totalsVerified = true),
                AccountType.MOBILE_WALLET
            )
        }
    }

    @Test
    fun mpesaCashRowsCannotBePreparedForABankAccount() {
        assertThrows(IllegalArgumentException::class.java) {
            StatementPdfActivityPolicy.toActivityRows(
                listOf(line(1, paidIn = 100L)),
                StatementActivityProof(receiptRows = 1, cashRows = 1, totalsVerified = true),
                AccountType.BANK
            )
        }
    }
}
