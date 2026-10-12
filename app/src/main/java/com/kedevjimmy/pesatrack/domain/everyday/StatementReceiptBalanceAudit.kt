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
            } else current.add(leg)
        }
        if(printed.size < 2) return StatementReceiptBalanceAssessment(0,0,printed.flatten().map {it.reference}.toSet())
        val groups = if(descending) printed else printed.asReversed()
        // A group must have no more than a bounded number of distinct statement
        // legs; this guards broken row assembly and prevents unbounded DP work.
        require(groups.all {it.size in 1..12}) { "An event has too many statement lines to verify." }
        val movements=groups.map {g->g.fold(0L) { acc, leg->Math.addExact(acc,leg.signedMinor) } }
        // Each group's observed balance is its balance *after that group's*\n        // movement. Thus newer.balance = older.balance + newer.movement.\n        // Select one consistent balance observation per receipt group, maximizing
        // exact adjacent event arithmetic over the entire statement (not per row).
        var scores=IntArray(groups[0].size)
        val parents=mutableListOf<IntArray>()
        for(i in 0 until groups.lastIndex) {
            val newer=groups[i]
            val older=groups[i+1]
            val next=IntArray(older.size) { Int.MIN_VALUE }
            val parent=IntArray(older.size) {-1}
            for(j in older.indices) for(k in newer.indices) {
                val expected=runCatching {Math.addExact(older[j].balanceMinor,movements[i+1])}.getOrNull()
                val proof=if(expected!=null && newer[k].balanceMinor==expected) 1 else 0
                val score=scores[k]+proof
                if(score>next[j]) {
                    next[j]=score
                    parent[j]=k
                }
            }
            parents.add(parent)
            scores=next
        }
        var selected=scores.indices.maxByOrNull {scores[it]} ?: error("No statement balance evidence.")
        val positions=IntArray(groups.size)
        positions[groups.lastIndex]=selected
        for(i in parents.lastIndex downTo 0) {
            selected=parents[i][selected]
            require(selected>=0) { "Statement balance proof has no valid path." }
            positions[i]=selected
        }
        val unresolved=linkedSetOf<String>()
        var verified=0
        for(i in 0 until groups.lastIndex) {
            val newer=groups[i][positions[i]]
            val older=groups[i+1][positions[i+1]]
            val expected=runCatching {Math.addExact(older.balanceMinor,movements[i+1])}.getOrNull()
            if(expected!=null && expected==newer.balanceMinor) verified++
            else {
                unresolved.add(newer.reference)
                unresolved.add(older.reference)
            }
        }
        return StatementReceiptBalanceAssessment(verified,groups.size-1,unresolved)
    }
}
