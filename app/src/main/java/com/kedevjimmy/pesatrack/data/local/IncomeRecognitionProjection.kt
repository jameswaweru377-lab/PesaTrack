package com.kedevjimmy.pesatrack.data.local

import com.kedevjimmy.pesatrack.domain.model.checkedMoneySumOf

/** One shared, read-only interpretation of canonical rows, with checked arithmetic. */
object IncomeRecognitionProjection {
    fun confirmedIncomeMinor(
        trustedRows: Iterable<StoredTransaction>,
        confirmedOwnedAccountIds: Set<String>,
        repaidReceivableMinor: (Long) -> Long
    ): Long = trustedRows
        .filter { IncomeRecognitionPolicy.isConfirmedIncome(it, confirmedOwnedAccountIds) }
        .checkedMoneySumOf { row ->
            Math.subtractExact(row.amountMinor, repaidReceivableMinor(row.id))
        }

    fun unresolvedCreditsMinor(
        observedRows: Iterable<StoredTransaction>,
        suppressedIds: Set<Long>,
        confirmedOwnedAccountIds: Set<String>
    ): Long = observedRows
        .filter { it.id !in suppressedIds && it.reviewState != "EXCLUDED" }
        .filter {
            IncomeRecognitionPolicy.classify(it, confirmedOwnedAccountIds).status ==
                IncomeRecognitionStatus.UNRESOLVED
        }
        .checkedMoneySumOf { it.amountMinor }
}
