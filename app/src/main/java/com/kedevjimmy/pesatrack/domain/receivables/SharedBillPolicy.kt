package com.kedevjimmy.pesatrack.domain.receivables

/** Shared intent is explicit; a historical partial loan alone does not establish it. */
object SharedBillPolicy {
    const val EVENT_PREFIX = "shared:"
    fun validAmount(paymentMinor: Long, shareMinor: Long): Boolean =
        paymentMinor > 0 && shareMinor > 0 && shareMinor < paymentMinor
    fun isSharedEvent(eventKey: String): Boolean = eventKey.startsWith(EVENT_PREFIX)
    fun ownShare(paymentMinor: Long, shareMinor: Long): Long {
        require(validAmount(paymentMinor,shareMinor)) { "The shared amount must be greater than zero and less than the bill." }
        return Math.subtractExact(paymentMinor,shareMinor)
    }
}
