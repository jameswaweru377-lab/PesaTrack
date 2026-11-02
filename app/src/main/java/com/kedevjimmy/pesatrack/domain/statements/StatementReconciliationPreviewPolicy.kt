package com.kedevjimmy.pesatrack.domain.statements

/** Review-only proposals; deliberately contains no amounts or ledger identifiers to write. */
object StatementReconciliationPreviewPolicy {
    enum class Decision { CONFIRMED, NOT_SAME }
    data class Preview(val confirmedCopies: Int, val confirmedTransfers: Int,
                       val unresolvedEvidenceCount: Int, val pendingSuggestions: Int)

    fun project(review: StatementMatchReviewPolicy.Review,
                decisions: Map<Set<String>, Decision>): Preview {
        val groups = review.sameMovements.map { false to it } + review.transfers.map { true to (it.outgoing + it.incoming) }
        val claimed = mutableSetOf<String>()
        var copies = 0
        var transfers = 0
        var pending = 0
        for ((transfer, rows) in groups) {
            val ids = rows.map { it.id }.toSet()
            val valid = ids.size == rows.size && rows.size >= 2 && rows.none {
                it.roleProtected || !it.ownershipConfirmed || it.movementMinor == 0L ||
                    (it.source != StatementEvidenceCorrelationPolicy.Source.ACTIVITY && !it.sourceTotalsVerified)
            } && ids.none { it in claimed }
            val decision = decisions[ids]
            if (!valid) continue
            if (decision == Decision.CONFIRMED) {
                claimed.addAll(ids)
                if (transfer) transfers++ else copies++
            } else if (decision == null) pending++
        }
        return Preview(copies, transfers, review.ambiguousCount, pending)
    }
}
