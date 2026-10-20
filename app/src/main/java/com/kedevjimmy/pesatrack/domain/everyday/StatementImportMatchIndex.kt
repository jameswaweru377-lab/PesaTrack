package com.kedevjimmy.pesatrack.domain.everyday

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Pre-indexed canonical rows for statement duplicate and possible-match review. */
internal class StatementImportMatchIndex private constructor(
    private val byReference: Map<String, List<ExistingLeg>>,
    private val possibleMatchKeys: Set<MatchKey>,
    private val timeZone: TimeZone
) {
    internal data class ExistingLeg(
        val accountId: String?,
        val amountMinor: Long,
        val direction: String,
        val occurredAtEpochMs: Long,
        val reference: String?
    )

    private data class MatchKey(
        val accountId: String,
        val amountMinor: Long,
        val side: Side,
        val localDay: Int
    )

    private enum class Side { INCOMING, OUTGOING }

    fun hasExactReference(accountId: String, row: StatementRow): Boolean {
        val reference = row.reference?.trim()?.uppercase(Locale.ROOT)
            ?.takeIf(String::isNotEmpty) ?: return false
        return byReference[reference].orEmpty().any { existing ->
            exactReferenceLeg(accountId, row, existing)
        }
    }

    fun hasPossibleMatch(accountId: String, row: StatementRow): Boolean {
        if (row.signedMinor == 0L || row.signedMinor == Long.MIN_VALUE) return false
        val amount = kotlin.math.abs(row.signedMinor)
        return possibleMatchKeys.contains(
            MatchKey(accountId, amount, side(row.signedMinor), localDay(row.date, timeZone))
        )
    }

    private fun exactReferenceLeg(
        accountId: String,
        row: StatementRow,
        existing: ExistingLeg
    ): Boolean {
        if (existing.accountId != accountId ||
            existing.amountMinor != kotlin.math.abs(row.signedMinor) ||
            localDay(existing.occurredAtEpochMs, timeZone) != localDay(row.date, timeZone)
        ) return false
        return when {
            row.signedMinor > 0L -> existing.direction == "INCOME"
            else -> existing.direction == "EXPENSE" || existing.direction == "TRANSFER"
        }
    }

    companion object {
        fun from(rows: List<ExistingLeg>, timeZone: TimeZone = TimeZone.getDefault()): StatementImportMatchIndex {
            val codes = HashMap<String, MutableList<ExistingLeg>>()
            val possible = HashSet<MatchKey>(rows.size)
            rows.forEach { row ->
                row.reference?.trim()?.uppercase(Locale.ROOT)
                    ?.takeIf(String::isNotEmpty)
                    ?.let { codes.getOrPut(it) { ArrayList() }.add(row) }
                val side = when (row.direction) {
                    "INCOME" -> Side.INCOMING
                    "EXPENSE", "TRANSFER" -> Side.OUTGOING
                    else -> null
                }
                if (side != null && row.accountId != null && row.amountMinor > 0L && row.occurredAtEpochMs > 0L) {
                    possible += MatchKey(
                        row.accountId,
                        row.amountMinor,
                        side,
                        localDay(row.occurredAtEpochMs, timeZone)
                    )
                }
            }
            return StatementImportMatchIndex(codes, possible, timeZone)
        }

        private fun side(signedMinor: Long) =
            if (signedMinor > 0L) Side.INCOMING else Side.OUTGOING

        private fun localDay(epochMs: Long, timeZone: TimeZone): Int =
            Calendar.getInstance(timeZone).run {
                timeInMillis = epochMs
                get(Calendar.YEAR) * 1_000 + get(Calendar.DAY_OF_YEAR)
            }
    }
}
