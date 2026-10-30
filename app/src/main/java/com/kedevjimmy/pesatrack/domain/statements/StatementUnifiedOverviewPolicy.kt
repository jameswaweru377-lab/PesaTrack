package com.kedevjimmy.pesatrack.domain.statements

/**
 * Read-only presentation of imported statement coverage. It deliberately has
 * no combined cash/income/spending totals: a bank-to-wallet transfer can have
 * two independently valid cash legs without representing two financial events.
 */
object StatementUnifiedOverviewPolicy {
    enum class MonthState { ON_FILE, NEEDS_REVIEW, NO_IMPORT }

    data class AccountMonth(
        val accountId: String,
        val month: Int,
        val state: MonthState,
        val observedLegs: Int,
        val verifiedRepeatCopies: Int,
        val unresolvedCandidates: Int
    )

    data class AccountRow(
        val accountId: String,
        val monthsOnFile: Int,
        val monthsNeedingReview: Int
    )

    data class Overview(
        val year: Int,
        val accountRows: List<AccountRow>,
        val accountMonths: List<AccountMonth>,
        val monthsOnFile: Int,
        val monthsNeedingReview: Int,
        val verifiedRepeatCopies: Int,
        val unresolvedCandidates: Int
    )

    /** Missing months are UI gaps, never invented zero-value transactions. */
    fun project(year: Int, coverage: List<StatementSourceCoveragePolicy.MonthCoverage>): Overview {
        require(year in 1970..2100)
        val scoped = coverage.filter { row ->
            row.monthKey.length == 7 && row.monthKey.startsWith("$year-") &&
                row.monthKey.substring(5).toIntOrNull()?.let { it in 1..12 } == true
        }
        val byAccount = scoped.groupBy { it.accountId }.toSortedMap()
        val months = byAccount.flatMap { (account, rows) ->
            val byMonth = rows.groupBy { it.monthKey.substring(5).toInt() }
            (1..12).map { month ->
                val evidence = byMonth[month].orEmpty()
                AccountMonth(
                    accountId = account,
                    month = month,
                    state = when {
                        evidence.isEmpty() -> MonthState.NO_IMPORT
                        evidence.any { it.status == StatementSourceCoveragePolicy.Status.NEEDS_REVIEW } ->
                            MonthState.NEEDS_REVIEW
                        else -> MonthState.ON_FILE
                    },
                    observedLegs = evidence.fold(0) { count, row ->
                        Math.addExact(count, row.observedLegCount)
                    },
                    verifiedRepeatCopies = evidence.fold(0) { count, row ->
                        Math.addExact(count, row.confirmedRepeatCopies)
                    },
                    unresolvedCandidates = evidence.fold(0) { count, row ->
                        Math.addExact(count, row.unresolvedCandidateCount)
                    }
                )
            }
        }
        val rows = byAccount.keys.map { account ->
            val entries = months.filter { it.accountId == account }
            AccountRow(account, entries.count { it.state != MonthState.NO_IMPORT },
                entries.count { it.state == MonthState.NEEDS_REVIEW })
        }
        return Overview(
            year = year,
            accountRows = rows,
            accountMonths = months,
            monthsOnFile = months.filter { it.state != MonthState.NO_IMPORT }
                .map { it.month }.distinct().size,
            monthsNeedingReview = months.filter { it.state == MonthState.NEEDS_REVIEW }
                .map { it.month }.distinct().size,
            verifiedRepeatCopies = scoped.fold(0) { total, row ->
                Math.addExact(total, row.confirmedRepeatCopies)
            },
            unresolvedCandidates = scoped.fold(0) { total, row ->
                Math.addExact(total, row.unresolvedCandidateCount)
            }
        )
    }
}
