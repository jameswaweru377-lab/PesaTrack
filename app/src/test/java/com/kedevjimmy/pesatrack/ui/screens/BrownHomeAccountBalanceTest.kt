package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.PaydayAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality
import com.kedevjimmy.pesatrack.data.local.PaydaySpendability
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.rules.TrackedAccountGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrownHomeAccountBalanceTest {
    private val group = TrackedAccountGroup("wallet", "M-PESA", AccountType.MOBILE_WALLET,
        listOf(MoneyAccount("main", "M-PESA", AccountType.MOBILE_WALLET)))

    @Test fun onlyCompleteConfirmedSnapshotsShowMoney() {
        assertEquals(425000L, trustedBrownHomeBalance(group, listOf(snapshot(425000L))))
        assertNull(trustedBrownHomeBalance(group, emptyList()))
        assertNull(trustedBrownHomeBalance(group, listOf(snapshot(null))))
        assertNull(trustedBrownHomeBalance(group, listOf(snapshot(425000L, PaydayBalanceQuality.UNKNOWN))))
    }

    @Test fun unrelatedAccountDoesNotBecomeDisplayedMoney() {
        assertNull(trustedBrownHomeBalance(group, listOf(snapshot(999000L).copy(accountId = "different"))))
    }

    private fun snapshot(balance: Long?, quality: PaydayBalanceQuality = PaydayBalanceQuality.OBSERVED) =
        PaydayAccountSnapshot("main", "M-PESA", AccountType.MOBILE_WALLET,
            balance, quality, null, PaydaySpendability.SPENDABLE)
}
