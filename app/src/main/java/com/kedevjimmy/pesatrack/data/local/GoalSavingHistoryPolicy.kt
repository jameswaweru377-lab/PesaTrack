package com.kedevjimmy.pesatrack.data.local

import com.kedevjimmy.pesatrack.domain.intelligence.SavingObservation
import com.kedevjimmy.pesatrack.domain.model.AccountType
import java.math.BigInteger

/** Selects only trusted goal-linked allocations; category edits cannot hide withdrawals. */
object GoalSavingHistoryPolicy {
    fun observations(trustedRows: List<StoredTransaction>, ruleId: String,
        accountTypes: Map<String, AccountType>): List<SavingObservation> = trustedRows
        .filter { it.ruleId == ruleId && it.direction == "TRANSFER" &&
            it.reviewState !in setOf("EXCLUDED", "NEEDS_REVIEW") &&
            it.type != "REVERSAL" && it.cycleCandidateReason == null }
        .map { SavingObservation(it.occurredAtEpochMs, ConceptLedgerPolicy.savingsDelta(it, accountTypes)) }

    fun netSaved(observations: List<SavingObservation>): Long? {
        val total = observations.fold(BigInteger.ZERO) { sum, item -> sum + BigInteger.valueOf(item.deltaMinor) }
        if (total > BigInteger.valueOf(Long.MAX_VALUE) || total < BigInteger.valueOf(Long.MIN_VALUE)) return null
        return total.toLong().coerceAtLeast(0L)
    }
}
