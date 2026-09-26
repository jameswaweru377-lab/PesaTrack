package com.kedevjimmy.pesatrack.domain.ingestion

import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TransactionFingerprintTest {
    @Test
    fun usesTransactionCodeWhenPresent() {
        val parsed = sample(code = " tqk12abc34 ")
        assertEquals("code:TQK12ABC34", TransactionFingerprint.of(parsed))
    }

    @Test
    fun fallbackFingerprintIsStableButChangesWithTransaction() {
        val first = sample(code = null)
        val duplicate = sample(code = null)
        val other = sample(code = null, amount = 99_900L)

        assertEquals(TransactionFingerprint.of(first), TransactionFingerprint.of(duplicate))
        assertNotEquals(TransactionFingerprint.of(first), TransactionFingerprint.of(other))
    }

    private fun sample(
        code: String?,
        amount: Long = 45_000L
    ) = ParsedMpesaTransaction(
        transactionCode = code,
        type = MpesaTransactionType.SENT_MONEY,
        amountMinor = amount,
        counterparty = "ELIZABETH W",
        feeMinor = 0L,
        balanceMinor = 1_285_000L,
        occurredAtText = "21/9/26 at 1:24 PM",
        rawMessage = "Confirmed sample transaction"
    )
}
