package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.PaydayAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality
import com.kedevjimmy.pesatrack.data.local.PaydaySpendability
import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivityBalancePolicyTest {
    @Test fun knownSpendableAccountsSumWithoutAddingProtectedSavings() {
        assertEquals(878_322L, activityRecordedSpendableBalance(listOf(
            balance("bank", 600_000L),
            balance("wallet", 278_322L),
            balance("locked", 900_000L, spendability = PaydaySpendability.PROTECTED)
        )))
    }

    @Test fun unknownSpendableAccountCannotBeHiddenByAnotherKnownAccount() {
        assertNull(activityRecordedSpendableBalance(listOf(
            balance("bank", 600_000L),
            balance("wallet", null, PaydayBalanceQuality.UNKNOWN)
        )))
        assertNull(activityRecordedSpendableBalance(emptyList()))
    }

    @Test fun overflowAndProtectedOnlyWithholdBalance() {
        assertNull(activityRecordedSpendableBalance(listOf(
            balance("bank", Long.MAX_VALUE), balance("wallet", 1L)
        )))
        assertNull(activityRecordedSpendableBalance(listOf(
            balance("locked", 500_000L, spendability = PaydaySpendability.PROTECTED)
        )))
    }

    private fun balance(
        id: String,
        minor: Long?,
        quality: PaydayBalanceQuality = PaydayBalanceQuality.OBSERVED,
        spendability: PaydaySpendability = PaydaySpendability.SPENDABLE
    ) = PaydayAccountSnapshot(id, id, AccountType.BANK, minor, quality,
        referenceEpochMs = 1_000L, spendability = spendability)
}
