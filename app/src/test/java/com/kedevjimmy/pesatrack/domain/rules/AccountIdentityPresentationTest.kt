package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountIdentityPresentationTest {
    @Test
    fun mpesaUsesMaskedConfirmedNumber() {
        val account =
            MoneyAccount(
                id = "account-mpesa",
                name = "M-Pesa",
                type =
                    AccountType.MOBILE_WALLET,
                identifier =
                    "+254700008497"
            )

        assertEquals(
            "M-Pesa (•••• 8497)",
            AccountIdentityPresentation
                .title(account)
        )
    }

    @Test
    fun accountsWithoutIdentifierKeepCanonicalName() {
        val account =
            MoneyAccount(
                id = "bank-equity",
                name = "Equity",
                type = AccountType.BANK
            )

        assertEquals(
            "Equity Bank Kenya",
            AccountIdentityPresentation
                .title(account)
        )
    }
    @Test
    fun whitespaceInsideIdentifierDoesNotBreakMasking() {
        assertEquals(
            "Equity Bank Kenya (•••• 5678)",
            AccountIdentityPresentation
                .title(
                    base = "Equity Bank Kenya",
                    identifier = "1234 5678"
                )
        )
    }

}
