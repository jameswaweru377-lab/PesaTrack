package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImportedOwnedPaymentPolicyTest {
    private val bank = MoneyAccount("bank-one", "Example Bank", AccountType.BANK, identifier = "1234567890")
    private val wallet = MoneyAccount("wallet-one", "M-Pesa", AccountType.MOBILE_WALLET, identifier = "0711111111")
    private val accounts = listOf(bank, wallet)

    @Test fun confirmedBankPaymentToOwnedPhoneWithNameVariantIsTransfer() {
        val result = resolve("+254711111111", "SAMPLE OWNER", "Sample Owner Person")
        assertEquals(bank.id, result?.sourceAccountId)
        assertEquals(wallet.id, result?.destinationAccountId)
        assertEquals(OwnTransferEvidence.EXACT_IDENTIFIER, result?.evidence)
    }

    @Test fun anotherRecipientsNumberOrConflictingNameIsNotMyTransfer() {
        assertNull(resolve("0712222222", "Sample Owner Person"))
        assertNull(resolve("0711111111", "Different Recipient Person"))
        assertNull(resolve(null, "Sample Owner Person"))
    }

    @Test fun accountOwnershipMustBeExplicitAndUnambiguous() {
        assertNull(resolve("0711111111", "Sample Owner Person", owned = emptySet()))
        assertNull(resolve("0711111111", "Sample Owner Person", owned = setOf(bank.id)))
        assertNull(resolve("0711111111", "Sample Owner Person", uniqueSource = false))
        assertNull(resolve("0711111111", "Sample Owner Person", accounts = accounts + wallet.copy(id = "wallet-two"),
            owned = setOf(bank.id, wallet.id, "wallet-two")))
    }

    @Test fun airtimeMerchantAndUntrustedSourcesNeverBecomeTransfers() {
        assertNull(resolve("0711111111", "Sample Owner Person", type = MpesaTransactionType.AIRTIME))
        assertNull(resolve("0711111111", "Sample Owner Person", type = MpesaTransactionType.BUY_GOODS))
        assertNull(resolve("0711111111", "Sample Owner Person", trustedInterpretedSource = false))
        assertNull(resolve("0711111111", "Sample Owner Person", sourceId = wallet.id))
    }

    private fun resolve(
        phone: String?,
        name: String?,
        profile: String = "Sample Owner Person",
        accounts: List<MoneyAccount> = this.accounts,
        owned: Set<String> = setOf(bank.id, wallet.id),
        uniqueSource: Boolean = true,
        trustedInterpretedSource: Boolean = true,
        sourceId: String = bank.id,
        type: MpesaTransactionType = MpesaTransactionType.SENT_MONEY
    ): OwnAccountTransferMatch? = ImportedOwnedPaymentPolicy.match(
        parsed = ParsedMpesaTransaction(
            transactionCode = "TESTREF123", type = type, amountMinor = 64_000,
            counterparty = name, counterpartyPhone = phone, feeMinor = 0,
            balanceMinor = null, occurredAtText = null, rawMessage = "invented test message"
        ),
        observedSourceId = sourceId,
        accounts = accounts,
        confirmedAccountIds = owned,
        profileName = profile,
        uniqueSourceAtProvider = uniqueSource,
        trustedInterpretedSource = trustedInterpretedSource
    )
}
