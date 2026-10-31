package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Invented account identities; source-only bank history must not disappear. */
class StatementSavedAccountVisibilityTest {
    @Test fun limitedOnlyBankIsSelectableAndOpensTheBankSourceView() {
        val index = StatementSavedAccountVisibility.index(
            walletAccountIds = emptyList(), verifiedBankAccountIds = emptySet(),
            limitedBankAccountIds = setOf("invented-equity")
        )
        assertEquals(listOf("invented-equity"), index.accountIds)
        assertTrue(index.isBank("invented-equity"))
    }

    @Test fun existingWalletAndVerifiedBanksKeepTheirRoutesWithoutDuplicateAccounts() {
        val index = StatementSavedAccountVisibility.index(
            walletAccountIds = listOf("invented-wallet"),
            verifiedBankAccountIds = setOf("invented-equity"),
            limitedBankAccountIds = setOf("invented-equity")
        )
        assertEquals(listOf("invented-wallet", "invented-equity"), index.accountIds)
        assertFalse(index.isBank("invented-wallet"))
        assertTrue(index.isBank("invented-equity"))
        assertFalse(index.isBank("unknown"))
    }

    @Test fun limitedOnlyBankAppearsInOverviewCountAndAccountPicker() {
        val overview = StatementSavedAccountVisibility.overview(
            reportAccountIds = listOf("invented-wallet"),
            limitedAccountIds = listOf("invented-equity")
        )
        assertEquals(listOf("invented-wallet", "invented-equity"), overview.accountIds)
        assertEquals(2, overview.accountIds.size)
        assertEquals(1, overview.reportCount)
        assertEquals(setOf("invented-equity"), overview.limitedOnlyIds)
    }

    @Test fun bankWithBothVerifiedAndLimitedHistoryIsNeverCountedTwice() {
        val overview = StatementSavedAccountVisibility.overview(
            reportAccountIds = listOf("invented-wallet", "invented-bank"),
            limitedAccountIds = listOf("invented-bank", "invented-bank")
        )
        assertEquals(listOf("invented-wallet", "invented-bank"), overview.accountIds)
        assertEquals(2, overview.reportCount)
        assertTrue(overview.limitedOnlyIds.isEmpty())
    }
}
