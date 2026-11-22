package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyPendingTransferReplayPolicyTest {
    @Test fun oldBankToWalletFactsWithoutRecipientCannotBeAutoApplied() {
        assertFalse(LegacyPendingTransferReplayPolicy.canApply(AccountType.BANK.name, AccountType.MOBILE_WALLET.name))
        assertFalse(LegacyPendingTransferReplayPolicy.canApply(AccountType.OTHER.name, AccountType.MOBILE_WALLET.name))
    }

    @Test fun otherConfirmedAccountMovementsKeepPriorReconciliation() {
        assertTrue(LegacyPendingTransferReplayPolicy.canApply(AccountType.MOBILE_WALLET.name, AccountType.BANK.name))
        assertTrue(LegacyPendingTransferReplayPolicy.canApply(AccountType.BANK.name, AccountType.BANK.name))
    }
}
