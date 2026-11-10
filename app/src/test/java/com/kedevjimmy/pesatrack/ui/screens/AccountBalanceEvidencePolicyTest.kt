package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality
import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountBalanceEvidencePolicyTest {
    @Test fun recordedAndManualBalancesAreIncludedWithoutGuessedBalances() {
        val summary = accountVerifiedBalanceSummary(listOf(
            line("M-Pesa", 184_635L, PaydayBalanceQuality.OBSERVED),
            line("Sample Bank", 678_020L, PaydayBalanceQuality.USER_ENTERED)
        ))
        assertEquals(862_655L, summary.totalMinor)
        assertTrue(summary.complete)
        assertEquals(2, summary.recordedCount)
    }

    @Test fun reconstructedBalancesDoNotInflateTheRecordedTotal() {
        val summary = accountVerifiedBalanceSummary(listOf(
            line("M-Pesa", 184_635L, PaydayBalanceQuality.OBSERVED),
            line("Bank", 333_300L, PaydayBalanceQuality.RECONSTRUCTED)
        ))
        assertEquals(184_635L, summary.totalMinor)
        assertFalse(summary.complete)
        assertEquals(1, summary.recordedCount)
    }

    @Test fun absentOrUnrepresentableEvidenceNeverBecomesAConfirmedTotal() {
        val unknown = accountVerifiedBalanceSummary(listOf(
            line("Bank", null, PaydayBalanceQuality.UNKNOWN)
        ))
        assertNull(unknown.totalMinor)
        assertFalse(unknown.complete)
        val overflow = accountVerifiedBalanceSummary(listOf(
            line("A", Long.MAX_VALUE, PaydayBalanceQuality.OBSERVED),
            line("B", 1L, PaydayBalanceQuality.USER_ENTERED)
        ))
        assertNull(overflow.totalMinor)
        assertFalse(overflow.complete)
    }

    private fun line(name: String, balance: Long?, quality: PaydayBalanceQuality) =
        AccountDisplayLine(name, name, AccountType.BANK, balance, quality)
}
