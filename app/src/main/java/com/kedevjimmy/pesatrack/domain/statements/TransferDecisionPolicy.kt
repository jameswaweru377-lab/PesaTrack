package com.kedevjimmy.pesatrack.domain.statements

/** Pure optimistic-concurrency guard shared by metadata persistence and host tests. */
object TransferDecisionPolicy {
    enum class Decision { CONFIRMED, NOT_SAME }
    enum class Result { SAVED, STALE, CONFLICT, ACCOUNT_CHECK_REQUIRED }

    fun validate(expectedFingerprint: String, expectedDecision: Decision?, decision: Decision?,
                 pairs: List<TransferReconciliationPolicy.Pair>, decisions: Map<String, Decision>,
                 externallyConfirmedIds: Set<String>, accountAttested: Boolean): Result {
        val selected = pairs.singleOrNull { it.fingerprint == expectedFingerprint } ?: return Result.STALE
        if (decisions[expectedFingerprint] != expectedDecision) return Result.STALE
        if (decision != Decision.CONFIRMED) return Result.SAVED
        if (!accountAttested) return Result.ACCOUNT_CHECK_REQUIRED
        val ids = selected.rows.mapTo(mutableSetOf()) { it.id }
        if (ids.any { it in externallyConfirmedIds } || pairs.any { other ->
                other.fingerprint != expectedFingerprint && decisions[other.fingerprint] == Decision.CONFIRMED &&
                    other.rows.any { it.id in ids }
            }) return Result.CONFLICT
        return Result.SAVED
    }
}
