package com.kedevjimmy.pesatrack.domain.model

import java.math.BigInteger

/** Reject unrepresentable totals; a wrapped or saturated amount is not financial evidence. */
object CheckedMoney {
    private val minimum = BigInteger.valueOf(Long.MIN_VALUE)
    private val maximum = BigInteger.valueOf(Long.MAX_VALUE)
    fun add(first: Long, second: Long): Long = Math.addExact(first, second)
    fun subtract(first: Long, second: Long): Long = Math.subtractExact(first, second)
    fun negate(value: Long): Long = Math.negateExact(value)

    fun sum(values: Sequence<Long>): Long = exact(values.fold(BigInteger.ZERO) { total, amount ->
        total + BigInteger.valueOf(amount)
    })

    // BigInteger.longValueExact is unavailable on some supported Android APIs.
    fun exact(value: BigInteger): Long {
        if (value < minimum || value > maximum) throw ArithmeticException("Money total is outside the supported range")
        return value.toLong()
    }

    fun total(vararg values: Long): Long = sum(values.asSequence())

    fun orNull(calculate: () -> Long): Long? = try {
        calculate()
    } catch (_: ArithmeticException) {
        null
    }
}

fun <T> Iterable<T>.checkedMoneySumOf(amount: (T) -> Long): Long =
    CheckedMoney.sum(asSequence().map(amount))

fun Iterable<Long>.checkedMoneySum(): Long = CheckedMoney.sum(asSequence())
