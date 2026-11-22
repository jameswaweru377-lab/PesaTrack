package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import org.junit.Assert.assertEquals
import org.junit.Test

class OwnedTransferRecipientPolicyTest {
    private val wallet = MoneyAccount(
        id = "wallet-primary", name = "Mobile wallet", type = AccountType.MOBILE_WALLET,
        identifier = "+254711111111"
    )

    @Test fun exactOwnedPhoneAndReorderedNamesPermitAutomaticTransfer() {
        assertEquals(OwnedTransferRecipientPolicy.Verdict.OWNED,
            assess("0711111111", "PERSON SAMPLE OWNER", "Sample Owner Person"))
        assertEquals(OwnedTransferRecipientPolicy.Verdict.OWNED,
            assess("254711111111", "OWNER PERSON", "Sample Owner Person"))
        assertEquals(OwnedTransferRecipientPolicy.Verdict.OWNED,
            assess("+254711111111", "OWNER SAMPLE", "Sample Owner"))
    }

    @Test fun completeOtherPhoneIsExternalNotAnOwnedTransfer() {
        assertEquals(OwnedTransferRecipientPolicy.Verdict.EXTERNAL,
            assess("0712222222", "Sample Owner Person", "Sample Owner Person"))
        assertEquals(OwnedTransferRecipientPolicy.Verdict.EXTERNAL,
            assess("+254712222222", null, "Sample Owner Person"))
    }

    @Test fun missingPhoneAndNameOnlyNeverAuthorizeAutomaticTransfer() {
        assertEquals(OwnedTransferRecipientPolicy.Verdict.UNCERTAIN,
            assess(null, "Sample Owner Person", "Sample Owner Person"))
        assertEquals(OwnedTransferRecipientPolicy.Verdict.UNCERTAIN,
            assess("***1111", "Sample Owner Person", "Sample Owner Person"))
    }

    @Test fun differentNamedRecipientRemainsUncertainEvenWithOwnedPhone() {
        assertEquals(OwnedTransferRecipientPolicy.Verdict.UNCERTAIN,
            assess("0711111111", "Different Recipient Person", "Sample Owner Person"))
        assertEquals(OwnedTransferRecipientPolicy.Verdict.UNCERTAIN,
            assess("0711111111", "Sample Stranger", "Sample Owner"))
    }

    @Test fun unconfiguredOrDuplicatedOwnedWalletCannotResolveAutomatically() {
        assertEquals(OwnedTransferRecipientPolicy.Verdict.UNCERTAIN,
            assess("0711111111", null, "Sample Owner", wallets = listOf(wallet.copy(identifier = null))))
        assertEquals(OwnedTransferRecipientPolicy.Verdict.UNCERTAIN,
            assess("0711111111", null, "Sample Owner", wallets = listOf(wallet,
                wallet.copy(id = "wallet-other"))))
        assertEquals(OwnedTransferRecipientPolicy.Verdict.UNCERTAIN,
            assess("0711111111", null, "Sample Owner", wallets = emptyList()))
    }

    private fun assess(phone: String?, name: String?, profile: String,
                       wallets: List<MoneyAccount> = listOf(wallet)) =
        OwnedTransferRecipientPolicy.assess(phone, name, wallet.id, wallets, profile)
}
