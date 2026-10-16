package com.kedevjimmy.pesatrack.domain.everyday

import com.kedevjimmy.pesatrack.domain.identity.IdentityGraphPolicy
import com.kedevjimmy.pesatrack.domain.learning.RememberedCategoryMatchPolicy

/**
 * Receipt evidence verifies only a counterpart identity to share a category.
 * It NEVER transfers the financial event itself into a second journal.
 * A same-code collision, mismatched amount/account or stale date is rejected.
 */
object StatementReceiptEvidence {
    data class ActivityReceipt(
        val code: String,
        val accountId: String?,
        val amountMinor: Long,
        val occurredAtEpochMs: Long,
        val direction: String,
        val counterparty: String?,
        val phone: String?,
        val excluded: Boolean,
        val structurallyLinked: Boolean,
        val transactionId: Long = 0L,
        val reviewState: String? = null,
        val categoryId: String? = null,
        val linkedAccountId: String? = null,
        val lentMinor: Long = 0L,
        val repaidMinor: Long = 0L,
        val sharedBill: Boolean = false,
        val approvedLearnedCategory: Boolean = false,
        val confirmedOwnedTransferMirror: Boolean = false
    )

    fun corroborated(
        statement: StatementActivityLine,
        statementAccountId: String,
        candidates: List<ActivityReceipt>
    ): ActivityReceipt? {
        if (statement.withdrawnMinor <= 0L) return null
        // No arbitrary code substrings: the complete 10-character receipt is required.
        if (!statement.reference.matches(Regex("[A-Z0-9]{10}"))) return null
        // Reject ambiguity: multiple canonical entries with the same code may
        // be separate financial legs and must not be used as identity proof.
        if (candidates.size != 1) return null
        val activity = candidates.single()
        if (!statement.reference.equals(activity.code, ignoreCase = true) ||
            activity.direction != "EXPENSE" || activity.excluded ||
            activity.structurallyLinked || activity.amountMinor != statement.withdrawnMinor)
            return null
        if (activity.accountId != null && activity.accountId != statementAccountId)
            return null
        val delta = kotlin.math.abs(statement.occurredAt - activity.occurredAtEpochMs)
        if (delta > 24L * 60L * 60L * 1000L) return null
        if (activity.counterparty.isNullOrBlank()) return null
        // A verified receipt is a fallback for missing/unparseable descriptions,
        // never a license to ignore a DIFFERENT clearly named recipient.
        val printedName = StatementFinancialPolicy.merchantKey(statement.description)
        if (printedName != null && !RememberedCategoryMatchPolicy.matches(
                activity.counterparty, null, printedName, null
            )) return null
        val printedPhone = StatementFinancialPolicy.recipientPhone(statement.description)
        val activityPhone = IdentityGraphPolicy.normalizeKenyanPhone(activity.phone)
        if (printedPhone != null && activityPhone != null && printedPhone != activityPhone)
            return null
        return activity
    }

    fun sameIdentity(
        name: String?, phone: String?, receipt: ActivityReceipt
    ): Boolean {
        val a = IdentityGraphPolicy.normalizeKenyanPhone(phone)
        val b = IdentityGraphPolicy.normalizeKenyanPhone(receipt.phone)
        if (a != null && b != null && a != b) return false
        // Require the FULL name in addition to non-conflicting phone evidence.
        // This protects phone-only identifiers, aliases and account owners.
        return RememberedCategoryMatchPolicy.matches(
            name, null, receipt.counterparty, null
        )
    }

    fun categoryFor(
        statement: StatementActivityLine,
        statementAccountId: String,
        receipts: Map<String, List<ActivityReceipt>>,
        memories: List<StatementCategoryMemoryPolicy.Memory>
    ): String? {
        val activity = corroborated(
            statement, statementAccountId, receipts[statement.reference].orEmpty()
        ) ?: return null
        val candidates = memories.filter { sameIdentity(it.name, it.phone, activity) }
        if (candidates.isEmpty()) return null
        // No silent choice between conflicting learned categories.
        val uniqueIdentities = candidates.map { memory ->
            IdentityGraphPolicy.normalizeKenyanPhone(memory.phone) ?: "NAME-ONLY"
        }.distinct()
        if (uniqueIdentities.size != 1) return null
        return candidates.map { it.categoryId }.distinct().singleOrNull()
    }
}
