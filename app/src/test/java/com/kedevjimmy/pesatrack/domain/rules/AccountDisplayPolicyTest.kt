package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDisplayPolicyTest {
    @Test
    fun keepsMshwariAndLockSavingsAsSeparateBalances() {
        val groups =
            AccountDisplayPolicy.groups(
                listOf(
                    MoneyAccount(
                        id = "mshwari",
                        name = "M-Shwari",
                        type = AccountType.SAVINGS
                    ),
                    MoneyAccount(
                        id = "lock",
                        name = "M-Shwari Lock Savings",
                        type = AccountType.SAVINGS
                    )
                )
            )

        assertEquals(2, groups.size)
        assertTrue(
            groups.any {
                it.displayName ==
                    "M-Shwari" &&
                    it.accountIds ==
                        setOf("mshwari")
            }
        )
        assertTrue(
            groups.any {
                it.displayName ==
                    "M-Shwari Lock Savings" &&
                    it.accountIds ==
                        setOf("lock")
            }
        )
    }

    @Test
    fun legacyLockSavingsAliasUsesCanonicalDisplayName() {
        val groups =
            AccountDisplayPolicy.groups(
                listOf(
                    MoneyAccount(
                        id = "legacy-lock",
                        name = "Lock Savings",
                        type = AccountType.SAVINGS
                    )
                )
            )

        assertEquals(
            "M-Shwari Lock Savings",
            groups.single().displayName
        )
    }

    @Test
    fun dynamicIdentifierIsMaskedInAccountLabel() {
        val label =
            AccountDisplayPolicy
                .displayLabel(
                    MoneyAccount(
                        id = "mpesa",
                        name = "M-Pesa",
                        type =
                            AccountType.MOBILE_WALLET,
                        identifier =
                            "+254700008497"
                    )
                )

        assertEquals(
            "M-Pesa (•••• 8497)",
            label
        )
    }

    @Test
    fun removesGenericPhantomAccounts() {
        val groups =
            AccountDisplayPolicy.groups(
                listOf(
                    MoneyAccount(
                        id = "your",
                        name = "Your",
                        type = AccountType.SAVINGS
                    ),
                    MoneyAccount(
                        id = "current",
                        name = "current",
                        type = AccountType.BANK
                    ),
                    MoneyAccount(
                        id = "equity",
                        name = "Equity Bank Kenya",
                        type = AccountType.BANK
                    )
                )
            )

        assertEquals(1, groups.size)
        assertEquals(
            "Equity Bank Kenya",
            groups.single().displayName
        )
    }

    @Test
    fun sourceMatchingUsesCanonicalBankAndMshwariNames() {
        val groups =
            AccountDisplayPolicy.groups(
                listOf(
                    MoneyAccount(
                        id = "equity",
                        name = "Equity Bank",
                        type = AccountType.BANK
                    ),
                    MoneyAccount(
                        id = "lock",
                        name = "M-Shwari Lock Savings",
                        type = AccountType.SAVINGS
                    )
                )
            )

        val equity =
            groups.first {
                it.type == AccountType.BANK
            }
        val mshwari =
            groups.first {
                it.type == AccountType.SAVINGS
            }

        assertTrue(
            AccountDisplayPolicy.matchesSource(
                equity,
                "Equity Bank Kenya"
            )
        )
        assertTrue(
            AccountDisplayPolicy.matchesSource(
                mshwari,
                "M-Shwari Lock Savings"
            )
        )
        assertFalse(
            AccountDisplayPolicy.matchesSource(
                mshwari,
                "M-Shwari"
            )
        )
        assertFalse(
            AccountDisplayPolicy.matchesSource(
                equity,
                "Sasa_pay"
            )
        )
    }

    @Test
    fun unconfirmedBankIsNotOwnedForDisplayOrSalaryUse() {
        val account =
            MoneyAccount(
                id = "equity",
                name = "Equity Bank Kenya",
                type = AccountType.BANK
            )

        assertFalse(
            AccountDisplayPolicy
                .isConfirmedOwned(
                    account = account,
                    confirmedAccountIds =
                        emptySet()
                )
        )
    }

    @Test
    fun explicitConfirmationMakesBankOwned() {
        val account =
            MoneyAccount(
                id = "equity",
                name = "Equity Bank Kenya",
                type = AccountType.BANK
            )

        assertTrue(
            AccountDisplayPolicy
                .isConfirmedOwned(
                    account = account,
                    confirmedAccountIds =
                        setOf("equity")
                )
        )
    }

    @Test
    fun confirmedIdentifierCanEstablishWalletOwnership() {
        val account =
            MoneyAccount(
                id = "mpesa",
                name = "M-Pesa",
                type =
                    AccountType.MOBILE_WALLET,
                identifier = "254700000000"
            )

        assertTrue(
            AccountDisplayPolicy
                .isConfirmedOwned(
                    account = account,
                    confirmedAccountIds =
                        emptySet()
                )
        )
    }

    @Test
    fun cashIsIncludedOnlyWhenCallerAllowsIt() {
        val account =
            MoneyAccount(
                id = "cash",
                name = "Cash",
                type = AccountType.CASH
            )

        assertFalse(
            AccountDisplayPolicy
                .isConfirmedOwned(
                    account = account,
                    confirmedAccountIds =
                        emptySet()
                )
        )
        assertTrue(
            AccountDisplayPolicy
                .isConfirmedOwned(
                    account = account,
                    confirmedAccountIds =
                        emptySet(),
                    includeCash = true
                )
        )
    }

}
