package com.kedevjimmy.pesatrack.domain.everyday

/**
 * Read-only account report built from the immutable Statement Activity batches.
 *
 * Each account/month uses exactly ONE saved document. Combining PDFs with
 * overlapping time ranges would double-count cash legs; combining accounts
 * would also double-count bank-to-wallet transfers. The selected source is
 * exposed so that the UI can disclose alternatives instead of hiding them.
 *
 * Money Out is wallet withdrawal, not automatically consumer spending.
 */
object StatementHistoryReport {
    data class SourceMonth(
        val accountId: String,
        val batchId: String,
        val recordedAt: Long,
        val independentlyVerified: Boolean,
        val usage: StatementMonthlyUsage.Month,
        val firstTransactionAt: Long,
        val lastTransactionAt: Long
    ) {
        init {
            require(accountId.isNotBlank() && batchId.isNotBlank())
            require(firstTransactionAt > 0L && lastTransactionAt >= firstTransactionAt)
        }
    }

    data class SelectedMonth(val source: SourceMonth, val sourceCount: Int) {
        init { require(sourceCount > 0) }
        val usage: StatementMonthlyUsage.Month get() = source.usage
        val key: String get() = usage.key
    }

    data class Year(
        val year: Int,
        val months: List<SelectedMonth>,
        val paidInMinor: Long,
        val paidOutMinor: Long
    ) {
        val importedMonthCount: Int get() = months.size
        val missingMonthCount: Int get() = 12 - months.size
        val averageOutMinor: Long get() =
            if (months.isEmpty()) 0L else paidOutMinor / months.size
        val netMinor: Long get() = Math.subtractExact(paidInMinor, paidOutMinor)
    }

    /** Newest first. Never sums alternative/overlapping PDFs for a month. */
    fun selectForAccount(sources: List<SourceMonth>, accountId: String): List<SelectedMonth> {
        if (accountId.isBlank()) return emptyList()
        return sources.asSequence().filter { it.accountId == accountId }
            .groupBy { it.usage.key }
            .map { (_, matching) ->
                // Prefer the source containing more actual dated rows; break
                // ties on elapsed period, independent total verification and
                // finally recency. Never pretend this proves full month cover.
                val chosen = matching.maxWithOrNull(
                    compareBy<SourceMonth> { it.usage.sourceRows }
                        .thenBy { it.lastTransactionAt - it.firstTransactionAt }
                        .thenBy { it.independentlyVerified }
                        .thenBy { it.recordedAt }
                        .thenBy { it.batchId }
                )!!
                SelectedMonth(chosen, matching.size)
            }
            .sortedByDescending { it.key }
    }

    fun years(selected: List<SelectedMonth>): List<Int> =
        selected.map { it.usage.year }.distinct().sortedDescending()

    /** Only imported months contribute. An absent month is NOT zero usage. */
    fun year(selected: List<SelectedMonth>, year: Int): Year {
        require(year in 1900..2200)
        val months = selected.filter { it.usage.year == year }
        val paidIn = months.fold(0L) { total, entry ->
            Math.addExact(total, entry.usage.paidInMinor)
        }
        val paidOut = months.fold(0L) { total, entry ->
            Math.addExact(total, entry.usage.paidOutMinor)
        }
        return Year(year, months, paidIn, paidOut)
    }
}
