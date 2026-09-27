package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OwnAccountTransferDetectorTest {
    private val accounts = listOf(
        MoneyAccount(
            id = "account-mpesa",
            name = "M-Pesa",
            type = AccountType.MOBILE_WALLET,
            identifier = "+254712345678"
        ),
        MoneyAccount(
            id = "equity",
            name = "Equity Bank",
            type = AccountType.BANK
        ),
        MoneyAccount(
            id = "other-wallet",
            name = "Second M-Pesa",
            type = AccountType.MOBILE_WALLET,
            identifier = "+254711111111"
        )
    )

    @Test
    fun normalizesKenyanPhoneFormats() {
        assertEquals(
            "+254712345678",
            OwnAccountTransferDetector.normalizeKenyanPhone("0712 345 678")
        )
        assertEquals(
            "+254712345678",
            OwnAccountTransferDetector.normalizeKenyanPhone("254712345678")
        )
        assertNull(
            OwnAccountTransferDetector.normalizeKenyanPhone("12345")
        )
    }

    @Test
    fun outgoingMpesaToOwnedWalletIsTransfer() {
        val match = OwnAccountTransferDetector.matchMpesa(
            parsed = sample(
                type = MpesaTransactionType.SENT_MONEY,
                phone = "0711111111",
                counterparty = "James W"
            ),
            accounts = accounts,
            observedWalletId = "account-mpesa"
        )

        assertEquals("account-mpesa", match?.sourceAccountId)
        assertEquals("other-wallet", match?.destinationAccountId)
        assertEquals(OwnTransferEvidence.EXACT_IDENTIFIER, match?.evidence)
    }

    @Test
    fun incomingMpesaFromOwnedBankCanUseBankName() {
        val match = OwnAccountTransferDetector.matchMpesa(
            parsed = sample(
                type = MpesaTransactionType.RECEIVED,
                counterparty = "EQUITY BANK"
            ),
            accounts = accounts,
            observedWalletId = "account-mpesa"
        )

        assertEquals("equity", match?.sourceAccountId)
        assertEquals("account-mpesa", match?.destinationAccountId)
        assertEquals(OwnTransferEvidence.ACCOUNT_NAME, match?.evidence)
    }

    @Test
    fun bankNotificationToOwnedMpesaIsTransfer() {
        val match = OwnAccountTransferDetector.matchConfiguredNotification(
            title = "Equity",
            body = "KES 20,000 transferred to M-PESA 0712345678 successfully",
            accounts = accounts
        )

        assertEquals("equity", match?.sourceAccountId)
        assertEquals("account-mpesa", match?.destinationAccountId)
    }

    @Test
    fun unrelatedBankNotificationIsNotTransfer() {
        assertNull(
            OwnAccountTransferDetector.matchConfiguredNotification(
                title = "Equity",
                body = "KES 58,000 received from BRAEBURN SCHOOLS",
                accounts = accounts
            )
        )
    }

    private fun sample(
        type: MpesaTransactionType,
        counterparty: String?,
        phone: String? = null
    ) = ParsedMpesaTransaction(
        transactionCode = "TQK12ABC34",
        type = type,
        amountMinor = 20_000_00L,
        counterparty = counterparty,
        counterpartyPhone = phone,
        feeMinor = 0L,
        balanceMinor = null,
        occurredAtText = null,
        rawMessage = "sample"
    )
}
