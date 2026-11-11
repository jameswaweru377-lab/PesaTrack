package com.kedevjimmy.pesatrack.domain.rules

import java.math.BigInteger

/** Account evidence only; income and salary-cycle figures must not enter this calculation. */
internal data class PlanSpendableAccount(
    val id: String,
    val balanceMinor: Long?,
    val isProtected: Boolean,
    val provisional: Boolean = false,
    val freshThroughEpochMs: Long? = null,
    val conflicting: Boolean = false
)

internal data class PlanSpendableBalance(
    val spendableMinor: Long?,
    val liquidMinor: Long?,
    val protectedMinor: Long?,
    val totalAccountsMinor: Long?,
    val needsReview: Boolean,
    val spendablePartial: Boolean
)

internal object PlanSpendableBalancePolicy {
    fun calculate(
        accounts: List<PlanSpendableAccount>,
        upcomingBillsMinor: Long?,
        nowEpochMs: Long = System.currentTimeMillis()
    ): PlanSpendableBalance {
        val distinct = accounts.distinctBy { it.id }
        val liquid = distinct.filterNot { it.isProtected }
        val protectedAccounts = distinct.filter { it.isProtected }
        fun needsBalanceCheck(account: PlanSpendableAccount): Boolean =
            account.provisional || account.balanceMinor == null ||
                account.freshThroughEpochMs?.let { nowEpochMs > it } == true
        val freshLiquid = liquid.filterNot(::needsBalanceCheck)
        val partial = freshLiquid.size < liquid.size
        val duplicateConflict = distinct.any { it.conflicting } || accounts.groupBy { it.id }
            .values.any { same -> same.distinct().size > 1 }
        val unsafeBills = upcomingBillsMinor == null || upcomingBillsMinor < 0L
        fun sum(values: List<PlanSpendableAccount>): Long? {
            if (values.isEmpty() || values.any { it.balanceMinor == null }) return null
            val total = values.fold(BigInteger.ZERO) { sum, row ->
                sum + BigInteger.valueOf(requireNotNull(row.balanceMinor))
            }
            return total.takeIf {
                it >= BigInteger.valueOf(Long.MIN_VALUE) &&
                    it <= BigInteger.valueOf(Long.MAX_VALUE)
            }?.toLong()
        }
        val liquidTotal = if (duplicateConflict) null else sum(liquid)
        val savedTotal = if (duplicateConflict) null else if (protectedAccounts.isEmpty()) 0L else sum(protectedAccounts)
        val totalAccounts = if (liquidTotal != null && savedTotal != null) {
            val total = BigInteger.valueOf(liquidTotal) + BigInteger.valueOf(savedTotal)
            total.takeIf { it >= BigInteger.valueOf(Long.MIN_VALUE) &&
                it <= BigInteger.valueOf(Long.MAX_VALUE) }?.toLong()
        } else null
        val freshLiquidTotal = if (duplicateConflict || unsafeBills) null else sum(freshLiquid)
        val possible = freshLiquidTotal?.let {
            BigInteger.valueOf(it) - BigInteger.valueOf(requireNotNull(upcomingBillsMinor))
        }
        val spendable = possible?.takeIf {
            it <= BigInteger.valueOf(Long.MAX_VALUE)
        }?.max(BigInteger.ZERO)?.toLong()
        return PlanSpendableBalance(
            spendableMinor = spendable,
            liquidMinor = liquidTotal,
            protectedMinor = savedTotal,
            totalAccountsMinor = totalAccounts,
            needsReview = spendable == null || savedTotal == null || possible?.signum() == -1 ||
                totalAccounts == null || distinct.any(::needsBalanceCheck),
            spendablePartial = spendable != null && partial
        )
    }
}
