package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.PaydayAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality
import com.kedevjimmy.pesatrack.data.local.PaydaySpendability
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
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

    @Test fun missingSpendableAccountDoesNotHideKnownBalance() {
        assertEquals(600_000L, activityRecordedSpendableBalance(listOf(
            balance("bank", 600_000L),
            balance("wallet", null, PaydayBalanceQuality.UNKNOWN)
        )))
        assertNull(activityRecordedSpendableBalance(emptyList()))
        assertNull(activityRecordedSpendableBalance(listOf(
            balance("wallet", null, PaydayBalanceQuality.UNKNOWN)
        )))
        assertEquals(0L, activityRecordedSpendableBalance(listOf(
            balance("wallet", 0L), balance("bank", null, PaydayBalanceQuality.UNKNOWN)
        )))
    }

    @Test fun coverageTravelsWithTheRecordedAmount() {
        val wallet = balance("wallet", 0L)
        val unknownBank = balance("bank", null, PaydayBalanceQuality.UNKNOWN)
        assertEquals(ActivityRecordedBalance(0L, partial = true),
            activityRecordedBalance(listOf(wallet, unknownBank)))
        assertEquals(ActivityRecordedBalance(0L, partial = false),
            activityRecordedBalance(listOf(wallet)))
        assertNull(activityRecordedBalance(listOf(unknownBank)))
    }

    @Test fun reconstructedAccountAmountIsNotPresentedAsConfirmedCash() {
        assertEquals(184_635L, activityRecordedSpendableBalance(listOf(
            balance("mpesa", 184_635L),
            balance("bank", 678_020L, PaydayBalanceQuality.RECONSTRUCTED)
        )))
        assertEquals(862_655L, activityRecordedSpendableBalance(listOf(
            balance("mpesa", 184_635L),
            balance("bank", 678_020L, PaydayBalanceQuality.USER_ENTERED)
        )))
    }

    @Test fun overflowAndProtectedOnlyWithholdBalance() {
        assertNull(activityRecordedSpendableBalance(listOf(
            balance("bank", Long.MAX_VALUE), balance("wallet", 1L)
        )))
        assertNull(activityRecordedSpendableBalance(listOf(
            balance("locked", 500_000L, spendability = PaydaySpendability.PROTECTED)
        )))
    }

    @Test fun signedBankAmountsSumBeforeCheckingTheFinalRange() {
        assertEquals(Long.MAX_VALUE, activityRecordedSpendableBalance(listOf(
            balance("bank", Long.MAX_VALUE), balance("wallet", 1L), balance("other", -1L)
        )))
    }

    @Test fun monthlyRecordedAccountBalanceDoesNotAutomaticallyIncludeCash() {
        val accounts = listOf(
            MoneyAccount("wallet", "Sample Wallet", AccountType.MOBILE_WALLET, identifier = "0712000000"),
            MoneyAccount("bank", "Sample Bank", AccountType.BANK, identifier = "123456789"),
            MoneyAccount("disabled-bank", "Old Bank", AccountType.BANK, enabled = false),
            MoneyAccount("cash", "Cash", AccountType.CASH)
        )
        val chosen = activityRecordedBalanceAccounts(accounts, setOf("wallet", "bank", "disabled-bank", "cash"))
        assertEquals(setOf("wallet", "bank"), chosen.map { it.id }.toSet())
    }

    private fun balance(
        id: String,
        minor: Long?,
        quality: PaydayBalanceQuality = PaydayBalanceQuality.OBSERVED,
        spendability: PaydaySpendability = PaydaySpendability.SPENDABLE
    ) = PaydayAccountSnapshot(id, id, AccountType.BANK, minor, quality,
        referenceEpochMs = 1_000L, spendability = spendability)
}
