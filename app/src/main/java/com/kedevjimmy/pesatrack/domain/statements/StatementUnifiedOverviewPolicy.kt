package com.kedevjimmy.pesatrack.domain.statements

/**
 * Read-only presentation of imported statement coverage and safe gross cash
 * movements. These are NOT external income/spending: a bank-to-wallet transfer
 * can have two independently valid cash legs without representing new money.
 */
object StatementUnifiedOverviewPolicy {
    enum class MonthState { ON_FILE, NEEDS_REVIEW, NO_IMPORT }
    enum class CashStatus { AVAILABLE, NEEDS_REVIEW, UNSUPPORTED_CURRENCY, NO_IMPORT }

    data class CashSummary(
        val status: CashStatus,
        val moneyInMinor: Long? = null,
        val moneyOutMinor: Long? = null
    )

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
        val cashSummary: CashSummary,
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
            cashSummary = summarizeCash(scoped),
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

    /**
     * Sum only independently verified, same-account deduplicated statement legs.
     * An uncertain period withholds the entire amount, not just the uncertain
     * portion. All currencies must be KES; no exchange rates are guessed.
     */
    private fun summarizeCash(
        scoped: List<StatementSourceCoveragePolicy.MonthCoverage>
    ): CashSummary {
        if (scoped.isEmpty()) return CashSummary(CashStatus.NO_IMPORT)
        val repeatedCoverage = scoped.map { Triple(it.accountId, it.currency.uppercase(), it.monthKey) }
            .distinct().size != scoped.size
        if (repeatedCoverage || scoped.any {
                it.status == StatementSourceCoveragePolicy.Status.NEEDS_REVIEW ||
                    it.unresolvedCandidateCount > 0 ||
                    it.safeMoneyInMinor == null || it.safeMoneyOutMinor == null ||
                    it.safeMoneyInMinor < 0L || it.safeMoneyOutMinor < 0L
            }) return CashSummary(CashStatus.NEEDS_REVIEW)
        if (scoped.any { !it.currency.equals("KES", ignoreCase = true) })
            return CashSummary(CashStatus.UNSUPPORTED_CURRENCY)
        return try {
            CashSummary(
                status = CashStatus.AVAILABLE,
                moneyInMinor = scoped.fold(0L) { sum, row ->
                    Math.addExact(sum, requireNotNull(row.safeMoneyInMinor))
                },
                moneyOutMinor = scoped.fold(0L) { sum, row ->
                    Math.addExact(sum, requireNotNull(row.safeMoneyOutMinor))
                }
            )
        } catch (_: ArithmeticException) {
            CashSummary(CashStatus.NEEDS_REVIEW)
        }
    }
}
