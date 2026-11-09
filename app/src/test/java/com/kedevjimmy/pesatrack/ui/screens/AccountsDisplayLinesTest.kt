package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountsDisplayLinesTest {
    @Test
    fun mshwariAndLockSavingsKeepSeparateValuesAndNavigation() {
        val group = AccountDisplayPolicy.groups(listOf(
            MoneyAccount("regular", "M-Shwari", AccountType.SAVINGS),
            MoneyAccount("locked", "M-Shwari Lock Savings", AccountType.SAVINGS)
        )).single()
        val presentation = AccountBalancePresentation(group, null, null, null, listOf(
            AccountComponentPresentation("M-Shwari", 12345L, 1L, PaydayBalanceQuality.OBSERVED),
            AccountComponentPresentation("M-Shwari Lock Savings", null, null, PaydayBalanceQuality.UNKNOWN)
        ))

        val lines = accountDisplayLines(listOf(presentation))

        assertEquals(2, lines.size)
        assertEquals("regular", lines[0].accountId)
        assertEquals(12345L, lines[0].balanceMinor)
        assertEquals("locked", lines[1].accountId)
        assertNull(lines[1].balanceMinor)
    }

    @Test
    fun bankBalanceRemainsUnavailableWhenSnapshotIsUnknown() {
        val group = AccountDisplayPolicy.groups(listOf(
            MoneyAccount("equity", "Equity Bank Kenya", AccountType.BANK)
        )).single()
        val lines = accountDisplayLines(listOf(
            AccountBalancePresentation(group, null, null, PaydayBalanceQuality.UNKNOWN, emptyList())
        ))

        assertEquals(1, lines.size)
        assertEquals("equity", lines.single().accountId)
        assertNull(lines.single().balanceMinor)
    }

    @Test
    fun lockSavingsDoesNotShowAsRegularMshwariWhenItIsTheOnlyOwnedMember() {
        val group = AccountDisplayPolicy.groups(listOf(
            MoneyAccount("lock", "M-Shwari Lock Savings", AccountType.SAVINGS)
        )).single()

        val lines = accountDisplayLines(listOf(
            AccountBalancePresentation(group, null, null, PaydayBalanceQuality.UNKNOWN, emptyList())
        ))

        assertEquals("M-Shwari Lock Savings", lines.single().name)
        assertEquals("lock", lines.single().accountId)
    }

    @Test
    fun accountLabelsUseSmallThreeDotMaskAndShortBankNames() {
        assertEquals("M-Pesa ··· 497", compactAccountLabel("M-Pesa (•••• 8497)"))
        assertEquals("Equity Bank ··· 689",
            compactAccountLabel("Equity Bank Kenya (•••• 5689)"))
        assertEquals("KCB Bank", compactAccountLabel("KCB Bank Kenya"))
        assertEquals("I&M Bank", compactAccountLabel("I&M Bank Kenya"))
        assertEquals("Equity Bank", compactAccountLabel("Equity Bank Kenya (•••• 68...)"))
        assertEquals("M-Shwari", compactAccountLabel("M-Shwari"))
    }

    @Test
    fun portfolioShowsOnlyKnownTickerLabels() {
        assertEquals("Safaricom", investmentCompanyName("SCOM"))
        assertEquals("SCOM", investmentTicker("Safaricom PLC"))
        assertEquals("KCB Group", investmentCompanyName("KCB"))
        assertEquals("NSE", investmentTicker("Acme Investment Fund"))
    }
}
