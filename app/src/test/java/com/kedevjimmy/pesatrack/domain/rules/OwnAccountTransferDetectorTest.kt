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
            type = AccountType.BANK,
            identifier = "1234567890"
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
    fun bankNameAloneCannotProveAnOwnAccountTransfer() {
        val match = OwnAccountTransferDetector.matchMpesa(
            parsed = sample(
                type = MpesaTransactionType.RECEIVED,
                counterparty = "EQUITY BANK"
            ),
            accounts = accounts,
            observedWalletId = "account-mpesa"
        )

        assertNull(match)
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
    fun mpesaNotificationToOwnedBankIdentifierIsTransfer() {
        val match =
            OwnAccountTransferDetector
                .matchConfiguredNotification(
                    title = "MPESA",
                    body =
                        "Ksh 2,500 transferred to Equity account 1234567890 successfully",
                    accounts = accounts
                )

        assertEquals(
            "account-mpesa",
            match?.sourceAccountId
        )
        assertEquals(
            "equity",
            match?.destinationAccountId
        )
        assertEquals(
            OwnTransferEvidence
                .EXACT_IDENTIFIER,
            match?.evidence
        )
    }

    @Test
    fun bankNameWithoutOwnedIdentifierDoesNotBecomeDestination() {
        assertNull(
            OwnAccountTransferDetector
                .matchConfiguredNotification(
                    title = "MPESA",
                    body =
                        "Ksh 2,500 transferred to Equity account 9999999999 successfully",
                    accounts = accounts
                )
        )
    }

    @Test
    fun shortOwnedIdentifierRequiresAccountContext() {
        val shortIdAccounts =
            accounts.map {
                if (it.id == "equity") {
                    it.copy(
                        identifier = "7890"
                    )
                } else {
                    it
                }
            }

        assertNull(
            OwnAccountTransferDetector
                .matchConfiguredNotification(
                    title = "MPESA",
                    body =
                        "Ksh 2,500 sent successfully. Reference ABC7890.",
                    accounts =
                        shortIdAccounts
                )
        )

        val match =
            OwnAccountTransferDetector
                .matchConfiguredNotification(
                    title = "MPESA",
                    body =
                        "Ksh 2,500 transferred to Equity account ****7890 successfully",
                    accounts =
                        shortIdAccounts
                )
        assertEquals(
            "equity",
            match?.destinationAccountId
        )
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

    @Test
    fun fullBankIdentifierKeepsFormattingButRequiresAccountContext() {
        val match = OwnAccountTransferDetector.matchConfiguredNotification(
            "MPESA", "Ksh 250.00 transferred to Equity account no. 123 456-7890 successfully", accounts)
        assertEquals("equity", match?.destinationAccountId)
        assertNull(OwnAccountTransferDetector.matchConfiguredNotification(
            "MPESA", "Ksh 250.00 sent successfully. Reference 1234567890.", accounts))
        assertNull(OwnAccountTransferDetector.matchConfiguredNotification(
            "MPESA", "Ksh 250.00 transferred to Equity account 91234567890 successfully", accounts))
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

