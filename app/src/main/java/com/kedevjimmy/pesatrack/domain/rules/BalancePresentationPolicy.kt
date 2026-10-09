package com.kedevjimmy.pesatrack.domain.rules
object BalancePresentationPolicy {
    fun totalKnown(balances: List<Long?>): Long? {
        val known = balances.filterNotNull()
        if (known.isEmpty()) return null
        return try { known.fold(java.math.BigInteger.ZERO) { sum, value -> sum + java.math.BigInteger.valueOf(value) }.longValueExact() } catch (_: ArithmeticException) { null }
    }
    fun savingsBalance(balance: Long?, savings: Boolean, reconstructed: Boolean): Long? = balance?.takeUnless { savings && reconstructed && it < 0L }
}
