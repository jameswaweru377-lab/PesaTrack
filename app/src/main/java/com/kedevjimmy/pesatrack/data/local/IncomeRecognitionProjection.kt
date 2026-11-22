package com.kedevjimmy.pesatrack.data.local

import com.kedevjimmy.pesatrack.domain.model.checkedMoneySumOf

/** One shared, read-only interpretation of canonical rows, with checked arithmetic. */
object IncomeRecognitionProjection {
    /** A repaid loan share is never earnings, even if the receipt itself was trusted. */
    fun confirmedAmountMinor(
        row: StoredTransaction,
        confirmedOwnedAccountIds: Set<String>,
        repaidReceivableMinor: Long
    ): Long = if (IncomeRecognitionPolicy.isConfirmedIncome(row, confirmedOwnedAccountIds)) {
        Math.subtractExact(row.amountMinor, repaidReceivableMinor)
    } else 0L

    fun confirmedIncomeMinor(
        trustedRows: Iterable<StoredTransaction>,
        confirmedOwnedAccountIds: Set<String>,
        repaidReceivableMinor: (Long) -> Long
    ): Long = trustedRows.checkedMoneySumOf { row ->
        confirmedAmountMinor(row, confirmedOwnedAccountIds, repaidReceivableMinor(row.id))
    }

    fun unresolvedCreditsMinor(
        observedRows: Iterable<StoredTransaction>,
        suppressedIds: Set<Long>,
        confirmedOwnedAccountIds: Set<String>,
        repaidReceivableMinor: (Long) -> Long = { 0L }
    ): Long = observedRows
        .filter { it.id !in suppressedIds && it.reviewState != "EXCLUDED" }
        .filter {
            IncomeRecognitionPolicy.classify(it, confirmedOwnedAccountIds).status ==
                IncomeRecognitionStatus.UNRESOLVED
        }
        .checkedMoneySumOf { Math.subtractExact(it.amountMinor, repaidReceivableMinor(it.id)) }
}
