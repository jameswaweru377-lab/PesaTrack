package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
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

    @Test
    fun kplcPayBillIsElectricityAndAutoReviewEligible() {
        val result =
            KenyanTransactionRecognizer.classify(
                sample(
                    counterparty = "KPLC PREPAID",
                    type = MpesaTransactionType.PAY_BILL
                )
            )

        assertEquals(
            TransactionCategory.ELECTRICITY,
            result?.category
        )
        assertEquals(
            "Kenya Power",
            result?.evidenceLabel
        )
        assertEquals(
            EvidenceConfidence.HIGH,
            result?.evidenceConfidence
        )
    }

    @Test
    fun naivasBuyGoodsIsGroceries() {
        val result =
            KenyanTransactionRecognizer.classify(
                sample(
                    counterparty = "Naivas Supermarket",
                    type = MpesaTransactionType.BUY_GOODS
                )
            )

        assertEquals(
            TransactionCategory.GROCERIES,
            result?.category
        )
        assertEquals(
            "Naivas",
            result?.evidenceLabel
        )
    }

    @Test
    fun javaHousePaymentIsEatingOut() {
        val result =
            KenyanTransactionRecognizer.classify(
                sample(
                    counterparty = "Java House",
                    type = MpesaTransactionType.BUY_GOODS
                )
            )

        assertEquals(
            TransactionCategory.EATING_OUT,
            result?.category
        )
    }

    @Test
    fun boltPaymentIsTransport() {
        val result =
            KenyanTransactionRecognizer.classify(
                sample(
                    counterparty = "Bolt",
                    type = MpesaTransactionType.BUY_GOODS
                )
            )

        assertEquals(
            TransactionCategory.TRANSPORT,
            result?.category
        )
    }

    @Test
    fun mshwariNameAloneNeverBecomesExpenseCategory() {
        assertNull(
            KenyanTransactionRecognizer.classify(
                sample(
                    counterparty = "M-Shwari",
                    type = MpesaTransactionType.SENT_MONEY
                )
            )
        )
    }

    @Test
    fun branchNameAloneNeverBecomesExpenseCategory() {
        assertNull(
            KenyanTransactionRecognizer.classify(
                sample(
                    counterparty = "Branch",
                    type = MpesaTransactionType.SENT_MONEY
                )
            )
        )
    }

    @Test
    fun personalNameDoesNotAutoClassify() {
        assertNull(
            KenyanTransactionRecognizer.classify(
                sample(
                    counterparty = "JOHN KAMAU",
                    type = MpesaTransactionType.SENT_MONEY
                )
            )
        )
    }

    @Test
    fun safaricomAirtimeStillAutoClassifiesPhoneAndData() {
        val result =
            KenyanTransactionRecognizer.classify(
                sample(
                    counterparty = "Safaricom PLC",
                    type = MpesaTransactionType.AIRTIME,
                    rawMessage = "Airtime purchase"
                )
            )

        assertNotNull(result)
        assertEquals(
            TransactionCategory.PHONE_AND_DATA,
            result?.category
        )
    }

    private fun sample(
        counterparty: String,
        type: MpesaTransactionType =
            MpesaTransactionType.OTHER,
        rawMessage: String = counterparty
    ) =
        ParsedMpesaTransaction(
            transactionCode = "TQK12ABC34",
            type = type,
            amountMinor = 2_000L,
            counterparty = counterparty,
            counterpartyPhone = null,
            feeMinor = 0L,
            balanceMinor = null,
            occurredAtText = null,
            rawMessage = rawMessage
        )
}
