package com.kedevjimmy.pesatrack.domain.onboarding

import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import org.junit.Assert.*
import org.junit.Test

class OnboardingAccountDetailsPolicyTest {
    private val equity = MoneyAccount("equity", "Equity", AccountType.BANK)
    private val kcb = MoneyAccount("kcb", "KCB", AccountType.BANK, identifier = "8765432100")
    private val savings = MoneyAccount("lock", "M-Shwari Lock Savings", AccountType.SAVINGS)

    @Test fun skipDoesNotModifyAccounts() {
        assertEquals(emptyMap<String, String>(),
            OnboardingAccountDetailsPolicy.validate(listOf(equity, kcb), emptyMap()).entries)
    }
    @Test fun suffixCannotBecomeCompleteBankIdentifier() {
        assertNotNull(OnboardingAccountDetailsPolicy.validate(listOf(equity), mapOf("equity" to "1234")).error)
    }
    @Test fun fullUserProvidedNumberCanBeStored() {
        val result = OnboardingAccountDetailsPolicy.validate(listOf(equity), mapOf("equity" to " 1234567890 "))
        assertNull(result.error)
        assertEquals("1234567890", result.entries["equity"])
    }
    @Test fun duplicateAcrossConfirmedAccountsIsRejected() {
        assertNotNull(OnboardingAccountDetailsPolicy.validate(
            listOf(equity, kcb), mapOf("equity" to "87654 32100")).error)
    }
    @Test fun cannotWriteUnknownOrSavingsAccountAsBank() {
        assertNotNull(OnboardingAccountDetailsPolicy.validate(listOf(equity, savings), mapOf("lock" to "12345678")).error)
        assertNotNull(OnboardingAccountDetailsPolicy.validate(listOf(equity), mapOf("other" to "12345678")).error)
    }
}
