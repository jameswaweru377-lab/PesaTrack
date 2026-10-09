package com.kedevjimmy.pesatrack.domain.rules
object BalancePresentationPolicy {
    fun totalKnown(balances: List<Long?>): Long? {
        val known = balances.filterNotNull()
        if (known.isEmpty()) return null
        val sum = known.fold(java.math.BigInteger.ZERO) { total, value -> total + java.math.BigInteger.valueOf(value) }
        return if (sum.bitLength() <= 63) sum.toLong() else null
    }
    fun savingsBalance(balance: Long?, savings: Boolean, reconstructed: Boolean): Long? = balance?.takeUnless { savings && reconstructed && it < 0L }
}
