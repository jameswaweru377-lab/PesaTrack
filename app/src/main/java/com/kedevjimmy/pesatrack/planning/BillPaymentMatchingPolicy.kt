package com.kedevjimmy.pesatrack.planning

import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.rules.SmartRuleEngine

/** Keeps one explicitly classified ledger payment from satisfying multiple bills. */
internal object BillPaymentMatchingPolicy {
    fun matches(
        transactionRuleId: String?,
        rule: SmartRule,
        counterparty: String?,
        counterpartyPhone: String?,
        amountMinor: Long,
        candidateBills: List<SmartRule> = listOf(rule)
    ): Boolean {
        if (transactionRuleId == rule.id) return true
        if (!transactionRuleId.isNullOrBlank()) return false

        val matchingBills = candidateBills.filter { candidate ->
            SmartRuleEngine.matchTransaction(
                counterparty = counterparty,
                counterpartyPhone = counterpartyPhone,
                amountMinor = amountMinor,
                rules = listOf(candidate)
            )?.id == candidate.id
        }
        return matchingBills.singleOrNull()?.id == rule.id
    }
}
