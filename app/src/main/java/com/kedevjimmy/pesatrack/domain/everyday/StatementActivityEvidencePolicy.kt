package com.kedevjimmy.pesatrack.domain.everyday

import com.kedevjimmy.pesatrack.domain.identity.IdentityGraphPolicy
import com.kedevjimmy.pesatrack.domain.learning.RememberedCategoryMatchPolicy

/**
 * Independent Statement projection from uniquely corroborated Activity cash-leg
 * evidence. This never creates a personal debt, transfer, exclusion or SMS row.
 */
object StatementActivityEvidencePolicy {
    fun verified(
        line: StatementActivityLine, account: String,
        candidates: List<StatementReceiptEvidence.ActivityReceipt>
    ): StatementReceiptEvidence.ActivityReceipt? {
        if (candidates.size != 1 || line.informational) return null
        val a = candidates.single()
        if (!line.reference.equals(a.code, ignoreCase = true) ||
            !line.reference.matches(Regex("[A-Z0-9]{10}")) ||
            a.accountId != account || a.structurallyLinked ||
            kotlin.math.abs(line.occurredAt - a.occurredAtEpochMs) > 86_400_000L) return null
        val amountValid = when (a.direction) {
            "EXPENSE", "TRANSFER" ->
                line.withdrawnMinor > 0L && line.withdrawnMinor == a.amountMinor
            "INCOME" -> line.paidInMinor > 0L && line.paidInMinor == a.amountMinor
            else -> false
        }
        if (!amountValid) return null
        val name = StatementFinancialPolicy.merchantKey(line.description)
        if (name != null && !a.counterparty.isNullOrBlank() &&
            !RememberedCategoryMatchPolicy.matches(name, null, a.counterparty, null)) return null
        val printedPhone = StatementFinancialPolicy.recipientPhone(line.description)
        val phone = IdentityGraphPolicy.normalizeKenyanPhone(a.phone)
        if (printedPhone != null && phone != null && printedPhone != phone) return null
        return a
    }

    fun apply(
        source: StatementClassified, account: String,
        receipts: List<StatementReceiptEvidence.ActivityReceipt>,
        categoryKinds: Map<String, String>,
        manuallyCorrected: Boolean
    ): StatementClassified {
        val a = verified(source.line, account, receipts) ?: return source
        val evidenced = source.copy(activityReceiptVerified = true)
        // A user's explicit Activity tracking exclusion wins over report
        // category choice; original source rows and debt provenance remain.
        if (a.excluded) return evidenced.copy(
            flow = StatementFlow.EXCLUDED, categoryId = null, economicAmountMinor = 0L,
            evidence = "Excluded from tracked Activity • same receipt verified")
        if (source.flow in setOf(StatementFlow.DEBT, StatementFlow.FEE,
                StatementFlow.REFUND, StatementFlow.SAVINGS, StatementFlow.INFORMATIONAL))
            return evidenced
        if (a.direction == "TRANSFER" &&
            (a.linkedAccountId != null ||
             (a.reviewState == "USER_CLASSIFIED" && a.categoryId == "TRANSFER")))
            return evidenced.copy(
                flow = StatementFlow.TRANSFER, categoryId = "TRANSFER",
                evidence = "Own-account transfer confirmed in Activity")
        if (a.lentMinor > 0L && source.line.withdrawnMinor > 0L) {
            if (a.sharedBill && a.lentMinor < source.line.withdrawnMinor) {
                val own = Math.subtractExact(source.line.withdrawnMinor, a.lentMinor)
                val category = a.categoryId?.takeIf { categoryKinds[it] == "EXPENSE" }
                    ?: source.categoryId?.takeIf { categoryKinds[it] == "EXPENSE" }
                if (category != null)
                    return evidenced.copy(flow = StatementFlow.EXPENSE,
                        categoryId = category, economicAmountMinor = own,
                        evidence = "Shared bill • confirmed receivable share excluded")
                return evidenced.copy(
                    evidence = "Shared bill verified; categorize own share in Activity")
            }
            if (a.lentMinor == source.line.withdrawnMinor)
                return evidenced.copy(flow = StatementFlow.TRANSFER,
                    categoryId = "MONEY_LENT",
                    evidence = "Money lent • owed to you (confirmed in Activity)")
        }
        if (a.repaidMinor > 0L && source.line.paidInMinor > 0L) {
            val remaining = Math.subtractExact(source.line.paidInMinor, a.repaidMinor)
            if (remaining == 0L)
                return evidenced.copy(flow = StatementFlow.TRANSFER,
                    categoryId = "RECEIVABLE_REPAYMENT",
                    evidence = "Personal loan repayment • not new income")
            if (source.flow == StatementFlow.INCOME)
                return evidenced.copy(economicAmountMinor = remaining,
                    evidence = "Income net of verified receivable repayment")
        }
        if (a.reviewState == "USER_CLASSIFIED" &&
            a.categoryId == "INVESTMENT")
            return evidenced.copy(flow = StatementFlow.INVESTMENT,
                categoryId = "INVESTMENT", evidence = "Investment verified in Activity")
        // A one-off Statement category retains priority over ordinary Activity
        // merchant labels, but cannot override a separately confirmed exclusion,
        // own transfer, investment, or receivable cash-flow adjustment above.
        if (manuallyCorrected) return evidenced
        if (a.reviewState == "USER_CLASSIFIED" && a.direction == "EXPENSE" &&
            a.categoryId?.let { categoryKinds[it] == "EXPENSE" } == true &&
            source.line.withdrawnMinor > 0L)
            return evidenced.copy(flow = StatementFlow.EXPENSE,
                categoryId = a.categoryId, evidence = "Activity category • same receipt verified")
        return evidenced
    }
}
