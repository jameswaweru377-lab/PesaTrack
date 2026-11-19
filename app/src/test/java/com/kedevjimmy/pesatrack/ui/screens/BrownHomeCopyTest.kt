package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class BrownHomeCopyTest {
    @Test fun availableMoneyExplainsUnpaidBillsAndMissingBalancesInPlainLanguage() {
        assertEquals("After unpaid bills", brownHomeBalanceSubtitle(partial = false))
        assertEquals("After unpaid bills (some balances missing)",
            brownHomeBalanceSubtitle(partial = true))
    }
}
