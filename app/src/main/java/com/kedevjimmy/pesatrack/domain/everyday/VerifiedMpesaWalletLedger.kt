package com.kedevjimmy.pesatrack.domain.everyday

import java.util.Locale

/**
 * Exact, bounded PDFBox-only financial proof for physically extracted M-Pesa rows.
 *
 * This does not alter the separate SMS/legacy statement parser. Printed source
 * legs remain distinct; only the *balance checking* model may combine a receipt
 * whose legs share one final balance. A narrowly recognized zero-net Fuliza
 * pair may be checked in its own balance scope only when BOTH surrounding
 * boundaries fail and bypassing it gives an exact wallet boundary.
 *
 * Neither a plausible sequence nor this proof establishes PDF authenticity.
 * All failures are fail-closed; no personal statement data is logged.
 */
internal object VerifiedMpesaWalletLedger {
    data class Result(
        val transitions: Int,
        val sharedEndingBalanceGroups: Int,
        val separatelyVerifiedFulizaCycles: Int,
        val derivedOpeningMinor: Long,
        val derivedClosingMinor: Long
    )

    private data class SourceLeg(
        val reference: String,
        val time: Long,
        val movement: Long,
        val balance: Long,
        val description: String
    )

    private data class UnitLeg(
        val reference: String,
        val time: Long,
        val movement: Long,
        val balance: Long
    )

    private data class BalanceOrder(val top: Long, val before: Long)
    private data class TimeBlock(val time: Long, val units: List<UnitLeg>)

    private const val MAX_UNITS_PER_TIMESTAMP = 9
    private const val MAX_ORDERS_PER_TIMESTAMP = 2048

    private fun normalized(text: String) =
        text.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()

    private fun recognizedZeroDescription(description: String): Boolean {
        val s = normalized(description)
        return s == "m-shwari loan request" ||
            s == "m-shwari lock withdraw to savings" ||
            s == "m-shwari lock activate and save from savings"
    }

    private fun isIsolatedFulizaPair(legs: List<SourceLeg>): Boolean {
        if (legs.size != 2 || legs.map { it.reference }.distinct().size != 1 ||
            legs.map { it.time }.distinct().size != 1) return false
        val credit = legs.singleOrNull { it.movement > 0L } ?: return false
        val debit = legs.singleOrNull { it.movement < 0L } ?: return false
        if (runCatching { Math.addExact(credit.movement, debit.movement) }.getOrNull() != 0L ||
            credit.balance != credit.movement || debit.balance != 0L) return false
        val funding = normalized(credit.description)
        val payment = normalized(debit.description)
        return "overdraft of credit party" in funding &&
            "fuliza" in payment && ("pay bill" in payment || "paybill" in payment)
    }

    /** All internally valid orders, retaining multiplicity for uniqueness. */
    private fun orders(block: TimeBlock): List<BalanceOrder>? {
        val units = block.units
        if (units.isEmpty() || units.size > MAX_UNITS_PER_TIMESTAMP) return null
        val answers = ArrayList<BalanceOrder>()
        val taken = BooleanArray(units.size)
        var overflow = false

        fun visit(depth: Int, newest: Long, previous: UnitLeg?) {
            if (overflow) return
            if (depth == units.size) {
                val before = runCatching {
                    Math.subtractExact(previous!!.balance, previous.movement)
                }.getOrNull() ?: return
                answers.add(BalanceOrder(newest, before))
                if (answers.size >= MAX_ORDERS_PER_TIMESTAMP) overflow = true
                return
            }
            for (i in units.indices) {
                if (taken[i]) continue
                val candidate = units[i]
                if (previous != null &&
                    runCatching {
                        Math.subtractExact(previous.balance, candidate.balance)
                    }.getOrNull() != previous.movement) continue
                taken[i] = true
                visit(depth + 1, if (depth == 0) candidate.balance else newest, candidate)
                taken[i] = false
                if (overflow) return
            }
        }
        visit(0, 0L, null)
        return if (overflow || answers.isEmpty()) null else answers
    }

    private fun connected(newer: List<BalanceOrder>, older: List<BalanceOrder>): Boolean =
        newer.any { n -> older.any { o -> n.before == o.top } }

    /**
     * Unique full-chain reconstruction by dynamic programming, not a greedy
     * choice at each timestamp. Two or more valid paths remain ambiguous.
     */
    private fun uniqueChain(options: List<List<BalanceOrder>>): List<BalanceOrder>? {
        if (options.isEmpty()) return null
        val ways = Array(options.size) { IntArray(options[it].size) }
        ways.last().fill(1)
        for (i in options.size - 2 downTo 0) {
            for (j in options[i].indices) {
                var n = 0
                for (k in options[i + 1].indices) {
                    if (options[i][j].before == options[i + 1][k].top) {
                        n = minOf(2, n + ways[i + 1][k])
                    }
                }
                ways[i][j] = n
            }
        }
        if (ways[0].sum() != 1) return null
        var index = ways[0].indexOfFirst { it == 1 }
        if (index < 0) return null
        val selected = mutableListOf(options[0][index])
        for (i in 1 until options.size) {
            val candidates = options[i].indices.filter { j ->
                ways[i][j] > 0 && options[i][j].top == selected.last().before
            }
            if (candidates.size != 1) return null
            index = candidates.single()
            selected.add(options[i][index])
        }
        return selected
    }

    fun verify(
        evidence: List<PdfBoxReceiptEvidence>,
        balanceLegs: List<StatementBalanceLeg>,
        declaredOpeningMinor: Long? = null,
        declaredClosingMinor: Long? = null
    ): Result? = runCatching {
        if (evidence.size !in 1..5_000 || evidence.size != balanceLegs.size) return null
        val source = evidence.zip(balanceLegs) { row, leg ->
            if (row.reference != leg.reference || row.balanceMinor != leg.balanceMinor ||
                row.movementMinor != leg.signedMinor || leg.occurredAt <= 0L ||
                leg.balanceMinor < 0L) return null
            SourceLeg(row.reference, leg.occurredAt, leg.signedMinor,
                leg.balanceMinor, row.description)
        }
        val descending = source.first().time >= source.last().time
        if (!source.zipWithNext().all { (a, b) ->
                if (descending) a.time >= b.time else a.time <= b.time
            }) return null
        val chronological = if (descending) source else source.asReversed()

        // Contiguous equal-reference groups: receipt references can identify
        // multiple monetary legs, never a unique ledger record.
        val groups = mutableListOf<List<SourceLeg>>()
        val seenReferences = HashSet<String>()
        var offset = 0
        while (offset < chronological.size) {
            val first = chronological[offset]
            var end = offset + 1
            while (end < chronological.size && chronological[end].reference == first.reference) end++
            val group = chronological.subList(offset, end)
            if (!seenReferences.add(first.reference) ||
                group.any { it.time != first.time } || group.size > 12) return null
            groups.add(group)
            offset = end
        }

        val zeroEvents = source.filter { it.movement == 0L }
        if (zeroEvents.any { !recognizedZeroDescription(it.description) }) return null

        val units = mutableListOf<UnitLeg>()
        var sharedGroups = 0
        val byReceipt = groups.associateBy { it.first().reference }

        for (group in groups) {
            val money = group.filter { it.movement != 0L }
            if (money.isEmpty()) continue
            if (money.size > 1 && money.all { it.balance == money.first().balance }) {
                val total = money.fold(0L) { sum, leg -> Math.addExact(sum, leg.movement) }
                units.add(UnitLeg(money.first().reference, money.first().time,
                    total, money.first().balance))
                sharedGroups++
            } else {
                units.addAll(money.map { UnitLeg(it.reference, it.time, it.movement, it.balance) })
            }
        }
        if (units.isEmpty()) return null

        val blocks = mutableListOf<TimeBlock>()
        var at = 0
        while (at < units.size) {
            var end = at + 1
            while (end < units.size && units[end].time == units[at].time) end++
            blocks.add(TimeBlock(units[at].time, units.subList(at, end)))
            at = end
        }
        val options = blocks.map { orders(it) ?: return null }
        val exceptions = mutableListOf<Int>()
        for (i in 1 until blocks.lastIndex) {
            val block = blocks[i]
            if (block.units.size != 2 ||
                block.units[0].reference != block.units[1].reference) continue
            val receipt = block.units[0].reference
            val original = byReceipt[receipt]?.filter { it.movement != 0L } ?: continue
            if (!isIsolatedFulizaPair(original) ||
                original.first().time != block.time) continue
            if (!connected(options[i-1], options[i]) &&
                !connected(options[i], options[i+1]) &&
                connected(options[i-1], options[i+1])) {
                exceptions.add(i)
            }
        }
        if (exceptions.zipWithNext().any { (a,b) -> b-a <= 1 }) return null
        val removed = exceptions.toHashSet()
        val activeBlocks = blocks.filterIndexed { i, _ -> i !in removed }
        val activeOptions = options.filterIndexed { i, _ -> i !in removed }
        val chain = uniqueChain(activeOptions) ?: return null

        val opening = chain.last().before
        val closing = chain.first().top
        if (opening < 0L || closing < 0L ||
            declaredOpeningMinor?.let { it != opening } == true ||
            declaredClosingMinor?.let { it != closing } == true ||
            (activeBlocks.size == 1 && declaredOpeningMinor == null)) return null
        val sum = source.fold(0L) { total, row -> Math.addExact(total, row.movement) }
        if (Math.addExact(opening, sum) != closing) return null

        // Zero cash movement does not imply "unimportant": validate both
        // the observed balance and the known informational event meaning.
        for (zero in zeroEvents) {
            val sameIndex = activeBlocks.indexOfFirst { it.time == zero.time }
            val compatible = if (sameIndex >= 0) {
                zero.balance == chain[sameIndex].before ||
                    activeBlocks[sameIndex].units.any { it.balance == zero.balance }
            } else {
                val between = (0 until activeBlocks.lastIndex).indexOfFirst { i ->
                    activeBlocks[i].time > zero.time && zero.time > activeBlocks[i+1].time
                }
                between >= 0 && zero.balance == chain[between].before
            }
            if (!compatible) return null
        }

        Result(activeBlocks.size - 1, sharedGroups, exceptions.size, opening, closing)
    }.getOrNull()
}
