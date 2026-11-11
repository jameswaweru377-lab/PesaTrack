package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.PaydayAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality
import com.kedevjimmy.pesatrack.data.local.PaydaySpendability
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.rules.PlanSpendableBalancePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic minor-unit values only; never use real messages or account numbers. */
class VerifiedAccountBalancePolicyTest {
    private val now = 1_800_000_000_000L
    private val bank = MoneyAccount("bank", "Sample Bank", AccountType.BANK)
    private val wallet = MoneyAccount("wallet", "Sample Wallet", AccountType.MOBILE_WALLET)
    private val locked = MoneyAccount("locked", "Locked Savings", AccountType.SAVINGS)

    @Test fun `home activity and plan use the same verified spendable sources`() {
        val snapshots = listOf(
            snapshot("bank", 3_000_000L),
            snapshot("wallet", 500_000L),
            snapshot("locked", 1_200_000L, protected = true)
        )
        val accounts = listOf(bank, wallet, locked)
        val confirmed = accounts.map { it.id }.toSet()
        val planInputs = VerifiedAccountBalancePolicy.planAccounts(accounts, confirmed, snapshots, now)
        val plan = PlanSpendableBalancePolicy.calculate(planInputs, 200_000L, nowEpochMs = now)

        assertEquals(3_500_000L, activityRecordedSpendableBalance(snapshots))
        assertEquals(3_500_000L, plan.liquidMinor)
        assertEquals(4_700_000L, plan.totalAccountsMinor)
        assertEquals(3_300_000L, plan.spendableMinor)
        assertFalse(plan.needsReview)
    }

    @Test fun `missing or reconstructed bank balance never becomes invented spendable money`() {
        val accounts = listOf(bank, wallet)
        val snapshots = listOf(snapshot("wallet", 600_000L))
        val missing = VerifiedAccountBalancePolicy.planAccounts(accounts,
            setOf("bank", "wallet"), snapshots, now)
        assertEquals(2, missing.size)
        assertEquals(600_000L, PlanSpendableBalancePolicy.calculate(missing, 0L, nowEpochMs = now).spendableMinor)

        val reconstructed = listOf(snapshot("bank", 200_000L,
            quality = PaydayBalanceQuality.RECONSTRUCTED), snapshot("wallet", 600_000L))
        val plan = VerifiedAccountBalancePolicy.planAccounts(accounts,
            setOf("bank", "wallet"), reconstructed, now)
        assertNull(plan.single { it.id == "bank" }.balanceMinor)
        assertEquals(600_000L, VerifiedAccountBalancePolicy.spendableTotal(reconstructed))
        assertEquals(600_000L, PlanSpendableBalancePolicy.calculate(plan, 0L, nowEpochMs = now).spendableMinor)
    }

    @Test fun `old or conflicting evidence requires review without overwriting the history`() {
        val old = snapshot("wallet", 600_000L).copy(referenceEpochMs = now - 15L * 86_400_000L)
        val stale = VerifiedAccountBalancePolicy.planAccounts(listOf(wallet),
            setOf("wallet"), listOf(old), now)
        assertTrue(stale.single().provisional)
        assertNull(PlanSpendableBalancePolicy.calculate(stale, 0L, nowEpochMs = now).spendableMinor)

        val conflicting = listOf(snapshot("wallet", 600_000L), snapshot("wallet", 200_000L))
        assertNull(VerifiedAccountBalancePolicy.spendableTotal(conflicting))
        assertNull(VerifiedAccountBalancePolicy.groupTotal(setOf("wallet"), conflicting))
        assertNull(VerifiedAccountBalancePolicy.planAccounts(listOf(wallet),
            setOf("wallet"), conflicting, now).single().balanceMinor)
    }

    @Test fun `only eligible non-cash owned accounts enter the live total`() {
        val accounts = listOf(wallet,
            MoneyAccount("cash", "Cash", AccountType.CASH),
            MoneyAccount("disabled", "Dormant", AccountType.BANK, enabled = false),
            MoneyAccount("debt", "Loan", AccountType.OTHER),
            MoneyAccount("other", "Unknown", AccountType.BANK))
        val selected = VerifiedAccountBalancePolicy.eligibleAccounts(accounts, setOf("wallet", "debt"))
        assertEquals(listOf("wallet"), selected.map { it.id })
        assertNull(VerifiedAccountBalancePolicy.groupTotal(setOf("bank", "wallet"),
            listOf(snapshot("wallet", 600_000L))))
    }

    private fun snapshot(id: String, minor: Long,
        quality: PaydayBalanceQuality = PaydayBalanceQuality.OBSERVED,
        protected: Boolean = false
    ) = PaydayAccountSnapshot(id, id, if (id == "locked") AccountType.SAVINGS else AccountType.BANK,
        minor, quality, now - 1_000L,
        if (protected) PaydaySpendability.PROTECTED else PaydaySpendability.SPENDABLE)
}
