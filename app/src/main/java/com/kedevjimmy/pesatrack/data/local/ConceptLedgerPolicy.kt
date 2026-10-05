package com.kedevjimmy.pesatrack.data.local

import com.kedevjimmy.pesatrack.domain.model.AccountType

/** Signed net allocations: unlocking/moving existing savings never becomes fresh saving. */
object ConceptLedgerPolicy {
    fun savingsDelta(row: StoredTransaction, accountTypes: Map<String,AccountType>): Long {
        if(row.direction!="TRANSFER" || row.relationType=="REFUND")return 0
        val source=accountTypes[row.accountId]
        val destination=accountTypes[row.linkedAccountId]
        return when {
            source==AccountType.SAVINGS && destination==AccountType.SAVINGS -> 0
            source==AccountType.SAVINGS -> -row.amountMinor
            destination==AccountType.SAVINGS -> row.amountMinor
            row.category=="SAVINGS" -> row.amountMinor
            else -> 0
        }
    }
}
