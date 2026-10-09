package com.kedevjimmy.pesatrack.domain.rules
object BalancePresentationPolicy {
    fun savingsBalance(balance: Long?, savings: Boolean, reconstructed: Boolean): Long? = balance?.takeUnless { savings && reconstructed && it < 0L }
}
