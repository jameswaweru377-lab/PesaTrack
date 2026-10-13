package com.kedevjimmy.pesatrack.domain.everyday

/**
 * A single provider receipt can contain several account movements with
 * different interim balance observations. Verify an event as a whole rather
 * than incorrectly requiring each printed leg's balance to be consecutive.
 *
 * A mismatch is *not* a license to invent a value. Its event references are
 * reported for explicit user review, while a low overall proof rate rejects
 * the entire layout as untrustworthy.
 */
data class StatementBalanceLeg(
    val reference: String,
    val occurredAt: Long,
    val signedMinor: Long,
    val balanceMinor: Long
)

data class StatementReceiptBalanceAssessment(
    val verifiedTransitions: Int,
    val totalTransitions: Int,
    val unverifiedReferences: Set<String>
) {
    val allVerified: Boolean get() = verifiedTransitions == totalTransitions
    /** Disagreeing groups remain exceptional even when the layout is mostly proven. */
    val credibleLayout: Boolean get() =
        totalTransitions > 0 && verifiedTransitions.toLong() * 100L >= totalTransitions.toLong() * 95L
}

object StatementReceiptBalanceAudit {
    fun verify(legs: List<StatementBalanceLeg>): StatementReceiptBalanceAssessment {
        require(legs.size in 2..5_000) { "At least two bounded statement entries are required." }
        require(legs.all { it.reference.isNotBlank() && it.occurredAt > 0L && it.balanceMinor >= 0L }) {
            "Invalid statement balance evidence."
        }
        val descending = legs.first().occurredAt >= legs.last().occurredAt
        require(legs.zipWithNext().all { (a,b) ->
            if(descending) a.occurredAt >= b.occurredAt else a.occurredAt <= b.occurredAt
        }) { "Statement receipt dates are not monotonic." }
        val printed = mutableListOf<MutableList<StatementBalanceLeg>>()
        val closedReferences = hashSetOf<String>()
        for(leg in legs) {
            val current=printed.lastOrNull()
            if(current?.firstOrNull()?.reference != leg.reference) {
                require(closedReferences.add(leg.reference)) {
                    "Non-contiguous repeated receipt references require review."
                }
                printed.add(mutableListOf(leg))
            } else {
                require(current.first().occurredAt==leg.occurredAt) {
                    "Financial legs sharing a receipt must have the same timestamp."
                }
                current.add(leg)
            }
        }
        if(printed.size < 2) return StatementReceiptBalanceAssessment(0,0,printed.flatten().map {it.reference}.toSet())
        // Traverse source groups from oldest to newest. The newer event's
        // total movement produces its closing balance from the preceding
        // event's closing balance, *not* the reverse.
        val groups = if(descending) printed.asReversed() else printed
        require(groups.all {it.size in 1..12}) { "An event has too many statement lines to verify." }
        val movements=groups.map { group ->
            group.fold(0L) { sum,leg->Math.addExact(sum,leg.signedMinor) }
        }
        // One event may expose several intermediate balance readings.
        // Choose a consistent representative by maximizing exact transitions,
        // then mark all transitions not proved by the chosen path.
        var scores=IntArray(groups.first().size)
        val parentIndexes=mutableListOf<IntArray>()
        for(i in 1 until groups.size) {
            val older=groups[i-1]
            val newer=groups[i]
            val nextScores=IntArray(newer.size) {Int.MIN_VALUE}
            val parent=IntArray(newer.size) {-1}
            for(k in newer.indices) for(j in older.indices) {
                val expected=runCatching {
                    Math.addExact(older[j].balanceMinor,movements[i])
                }.getOrNull()
                val proof=if(expected==newer[k].balanceMinor) 1 else 0
                val score=scores[j]+proof
                if(score>nextScores[k]) {
                    nextScores[k]=score
                    parent[k]=j
                }
            }
            parentIndexes.add(parent)
            scores=nextScores
        }
        var selected=scores.indices.maxByOrNull {scores[it]} ?: error("No balance evidence.")
        val selectedIndexes=IntArray(groups.size)
        selectedIndexes[groups.lastIndex]=selected
        for(i in parentIndexes.indices.reversed()) {
            selected=parentIndexes[i][selected]
            require(selected>=0) { "Unverifiable account balance path." }
            selectedIndexes[i]=selected
        }
        var verified=0
        val unresolved=linkedSetOf<String>()
        for(i in 1 until groups.size) {
            val older=groups[i-1][selectedIndexes[i-1]]
            val newer=groups[i][selectedIndexes[i]]
            val expected=runCatching {
                Math.addExact(older.balanceMinor,movements[i])
            }.getOrNull()
            if(expected==newer.balanceMinor) verified++
            else {
                unresolved.add(older.reference)
                unresolved.add(newer.reference)
            }
        }
        return StatementReceiptBalanceAssessment(verified,groups.size-1,unresolved)
    }
}
