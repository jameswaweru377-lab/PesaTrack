package com.kedevjimmy.pesatrack.domain.receivables

data class DebtMovement(val transactionId: Long?, val kind: String, val amountMinor: Long, val occurredAt: Long)
data class BatchTotals(val lentMinor: Long, val repaidMinor: Long, val outstandingMinor: Long)
object ReceivableBatchPolicy {
    fun validate(existing: List<DebtMovement>, selected: List<DebtMovement>): BatchTotals {
        val history = existing + selected
        val ids = history.mapNotNull { it.transactionId }
        require(ids.size == ids.distinct().size) { "A transaction is already linked or selected twice." }
        var lent = 0L
        var repaid = 0L
        var balance = 0L
        history.sortedWith(compareBy<DebtMovement> { it.occurredAt }
            .thenBy { if (it.kind == "LENT") 0 else 1 }
            .thenBy { it.transactionId ?: Long.MIN_VALUE }).forEach { item ->
            require(item.amountMinor > 0 && item.occurredAt > 0) { "Check the amount and date." }
            when (item.kind) {
                "LENT" -> { lent = Math.addExact(lent, item.amountMinor); balance = Math.addExact(balance,item.amountMinor) }
                "REPAID" -> {
                    require(balance >= item.amountMinor) { "A repayment exceeds the confirmed loans available at that date." }
                    repaid = Math.addExact(repaid,item.amountMinor); balance = Math.subtractExact(balance,item.amountMinor)
                }
                else -> throw IllegalArgumentException("Select money lent or repayment.")
            }
        }
        return BatchTotals(lent,repaid,balance)
    }
}
