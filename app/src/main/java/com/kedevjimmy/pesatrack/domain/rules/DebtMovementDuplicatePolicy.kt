package com.kedevjimmy.pesatrack.domain.rules

import kotlin.math.abs

data class DebtMovementEvidence(
    val eventId: Long,
    val debtAccountId: String,
    val type: String,
    val amountMinor: Long,
    val feeMinor: Long,
    val linkedTransactionId: Long?,
    val occurredAtEpochMs: Long,
    val outstandingAfterMinor: Long? = null,
    val amountIsNetProceeds: Boolean? = null,
    val repaymentCompletion: String? = null,
    val hasUserDecision: Boolean = false
)

/** Dedupe only independently observed loan legs sharing a proven transaction ID. */
object DebtMovementDuplicatePolicy {
    private const val MAX_TIME_DIFFERENCE_MS = 5 * 60 * 1000L

    private data class CashLegKey(
        val accountId: String,
        val type: String,
        val transactionId: Long,
        val amountMinor: Long,
        val feeMinor: Long,
        val amountIsNetProceeds: Boolean?,
        val repaymentCompletion: String?
    )

    fun uniqueEventIds(events: List<DebtMovementEvidence>): Set<Long> {
        val retained = mutableMapOf<CashLegKey, MutableList<DebtMovementEvidence>>()
        val retainedIds = mutableSetOf<Long>()
        // If one copy includes a genuine resulting-balance observation, keep it.
        events.sortedWith(compareBy<DebtMovementEvidence> {
            it.outstandingAfterMinor == null
        }.thenBy { it.eventId }).forEach { current ->
            val link = current.linkedTransactionId
            if (link == null || current.hasUserDecision) {
                retainedIds.add(current.eventId)
                return@forEach
            }
            val key = CashLegKey(current.debtAccountId, current.type, link,
                current.amountMinor, current.feeMinor, current.amountIsNetProceeds,
                current.repaymentCompletion)
            val group = retained.getOrPut(key) { mutableListOf() }
            val duplicate = group.any { earlier ->
                    (earlier.outstandingAfterMinor == null || current.outstandingAfterMinor == null ||
                        earlier.outstandingAfterMinor == current.outstandingAfterMinor) &&
                    abs(earlier.occurredAtEpochMs - current.occurredAtEpochMs) <=
                        MAX_TIME_DIFFERENCE_MS
            }
            if (!duplicate) {
                group.add(current)
                retainedIds.add(current.eventId)
            }
        }
        return retainedIds
    }
}
