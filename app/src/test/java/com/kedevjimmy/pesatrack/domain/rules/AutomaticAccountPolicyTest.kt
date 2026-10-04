package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.interpretation.*
import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.Assert.*
import org.junit.Test

class AutomaticAccountPolicyTest {
    @Test fun resolvedOwnedAssetsDoNotRequireIdentifiers() {
        listOf("M-Pesa", "M-Shwari", "M-Shwari Lock Savings").forEach {
            assertTrue(AutomaticAccountPolicy.canResolve(FinancialProviderRegistry.resolveAccount(it), false))
        }
        assertTrue(AutomaticAccountPolicy.canResolve(
            FinancialProviderRegistry.resolveOwnedMessageSourceAccount("EQUITY", true)!!, false
        ))
        // The policy depends on financial meaning, including a future bank, not provider branches.
        assertTrue(AutomaticAccountPolicy.canResolve(reference(AccountOwnershipHint.USER_OWNED), false))
    }

    @Test fun recognizedProviderAloneDoesNotEstablishOwnership() {
        assertFalse(AutomaticAccountPolicy.canResolve(
            FinancialProviderRegistry.resolveMessageSourceAccount("EQUITY", true)!!, false
        ))
        assertFalse(AutomaticAccountPolicy.canResolve(reference(AccountOwnershipHint.UNKNOWN), false))
        assertFalse(AutomaticAccountPolicy.canResolve(reference(AccountOwnershipHint.EXTERNAL), false))
    }

    @Test fun externalDecisionAndLiabilitiesAreProtected() {
        assertFalse(AutomaticAccountPolicy.canResolve(reference(AccountOwnershipHint.USER_OWNED), true))
        assertFalse(AutomaticAccountPolicy.canResolve(
            reference(AccountOwnershipHint.USER_OWNED).copy(nature = FinancialAccountNature.LIABILITY), false
        ))
        assertFalse(AutomaticAccountPolicy.canResolve(
            reference(AccountOwnershipHint.USER_OWNED).copy(displayName = ""), false
        ))
    }

    private fun reference(ownership: AccountOwnershipHint) = FinancialAccountReference(
        "account:future-bank", "Future Bank", null, AccountType.BANK,
        ownership, FinancialAccountNature.ASSET
    )
}
