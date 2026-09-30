package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDisplayPolicyTest {
    @Test
    fun groupsMshwariFamilyIntoOneVisibleAccount() {
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

        assertEquals(1, groups.size)
        assertEquals(
            "M-Shwari",
            groups.single().displayName
        )
        assertEquals(
            setOf("mshwari", "lock"),
            groups.single().accountIds
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
}
