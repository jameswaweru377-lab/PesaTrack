package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KenyanTransactionRecognizerTest {
    @Test
    fun tunukiwaOfferIsPhoneAndData() {
        val result = KenyanTransactionRecognizer.classify(
            sample("Safaricom offers for account Tunukiwa")
        )
        assertEquals(TransactionCategory.PHONE_AND_DATA, result?.category)
    }

    @Test
    fun safaricomDataBundleIsPhoneAndData() {
        val result = KenyanTransactionRecognizer.classify(
            sample("Safaricom data bundles for account")
        )
        assertEquals(TransactionCategory.PHONE_AND_DATA, result?.category)
    }

    @Test
    fun smsBundleIsPhoneAndDataWithoutRequiringSafaricomName() {
        val result = KenyanTransactionRecognizer.classify(
            sample("SMS bundles")
        )
        assertEquals(TransactionCategory.PHONE_AND_DATA, result?.category)
    }

    @Test
    fun genericSafaricomPaymentIsNotAutoClassified() {
        assertNull(
            KenyanTransactionRecognizer.classify(
                sample("Safaricom PLC")
            )
        )
    }

    private fun sample(counterparty: String) =
        ParsedMpesaTransaction(
            transactionCode = "TQK12ABC34",
            type = MpesaTransactionType.OTHER,
            amountMinor = 2_000L,
            counterparty = counterparty,
            counterpartyPhone = null,
            feeMinor = 0L,
            balanceMinor = null,
            occurredAtText = null,
            rawMessage = counterparty
        )
}
