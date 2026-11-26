package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.data.local.IncomeRecognitionPolicy
import com.kedevjimmy.pesatrack.data.local.IncomeRecognitionStatus
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState

/**
 * Activity list presentation filter. The canonical ledger, not this view,
 * decides whether a receipt contributes to trusted Money In.
 */
internal fun activityFilteredRows(
    transactions: List<StoredTransaction>,
    selectedFilter: String,
    searchQuery: String,
    possibleDuplicateIds: Set<Long>,
    trustedIncomeIds: Set<Long>,
    receivableLinks: Map<Long, ReceivableActivityLink>,
    categoryLabel: (String) -> String,
    confirmedOwnedAccountIds: Set<String> = emptySet()
): List<StoredTransaction> {
    val query = searchQuery.trim()
    return transactions.filter { item ->
        if (item.reviewState == ReviewState.EXCLUDED.name) return@filter false
        if (item.id in possibleDuplicateIds && selectedFilter != "Needs Review") return@filter false

        val matchesFilter = when (selectedFilter) {
            "Unresolved Credits" -> IncomeRecognitionPolicy.classify(
                item, confirmedOwnedAccountIds
            ).status == IncomeRecognitionStatus.UNRESOLVED
            "Owed to me" -> receivableLinks[item.id]?.let { it.active && it.kind == "LENT" } == true
            "Repayments" -> receivableLinks[item.id]?.let { it.active && it.kind == "REPAID" } == true
            "Needs Review" -> item.reviewState == ReviewState.NEEDS_REVIEW.name ||
                item.id in possibleDuplicateIds
            "Pending" -> item.reviewState == ReviewState.NEEDS_REVIEW.name &&
                item.direction == MoneyDirection.TRANSFER.name
            "Money Out" -> item.direction == MoneyDirection.EXPENSE.name
            "Invested" -> isActivityConfirmedInvestmentBuy(item)
            "Money In" -> item.id in trustedIncomeIds
            "Transfers" -> item.direction == MoneyDirection.TRANSFER.name &&
                item.relationType != TransactionStore.RELATION_REFUND
            else -> true
        }

        val matchesSearch = query.isBlank() ||
            item.counterparty?.contains(query, ignoreCase = true) == true ||
            item.evidenceLabel?.contains(query, ignoreCase = true) == true ||
            categoryLabel(item.category).contains(query, ignoreCase = true) ||
            receivableLinks[item.id]?.description?.contains(query, ignoreCase = true) == true
        matchesFilter && matchesSearch
    }
}
