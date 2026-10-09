package com.kedevjimmy.pesatrack.domain.interpretation

import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileMoneyPresentationTest {
    @Test fun mobileSavingsStayWithTheirWalletProducts() {
        assertTrue(FinancialProviderRegistry.isMobileMoneyAccount("M-Shwari", AccountType.SAVINGS))
        assertTrue(FinancialProviderRegistry.isMobileMoneyAccount("M-Shwari Lock Savings", AccountType.SAVINGS))
        assertTrue(FinancialProviderRegistry.isMobileMoneyAccount("KCB M-PESA", AccountType.SAVINGS))
    }
    @Test fun bankAndUnknownSavingsAreNotRelabeledMobileMoney() {
        assertFalse(FinancialProviderRegistry.isMobileMoneyAccount("Equity", AccountType.BANK))
        assertFalse(FinancialProviderRegistry.isMobileMoneyAccount("Savings account", AccountType.SAVINGS))
        assertTrue(FinancialProviderRegistry.isMobileMoneyAccount("Airtel Money", AccountType.MOBILE_WALLET))
    }
}
