package com.kedevjimmy.pesatrack.domain.statements

/** Keeps legacy match-review leads from treating duplicate account records as separate money. */
internal object StatementLegacyReviewIdentityPolicy {
    data class Guarded(
        val review: StatementMatchReviewPolicy.Review,
        val possibleTransfers: List<StatementPossibleTransferPolicy.Suggestion>
    ) {
        override fun toString(): String = "[private statement match review]"
    }

    fun guard(
        review: StatementMatchReviewPolicy.Review,
        possibleTransfers: List<StatementPossibleTransferPolicy.Suggestion>,
        confirmedOwnership: Map<String, StatementMatchDecisionPolicy.Ownership>
    ): Guarded {
        val distinctAccountIds = StatementOwnedAccountUniquenessPolicy.unambiguous(confirmedOwnership).keys
        val safeTransfers = review.transfers.filter { transfer ->
            (transfer.outgoing + transfer.incoming).all { it.accountId in distinctAccountIds }
        }
        val safePossibilities = possibleTransfers.filter { suggestion ->
            suggestion.outgoing.accountId in distinctAccountIds &&
                suggestion.incoming.accountId in distinctAccountIds
        }
        // A rejected lead is not deleted evidence; original copies and uncertainty remain visible.
        return Guarded(review.copy(transfers = safeTransfers), safePossibilities)
    }
}
