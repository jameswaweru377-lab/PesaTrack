package com.kedevjimmy.pesatrack.domain.statements

/**
 * One reusable, non-posting accounting view over independently scoped evidence.
 * It does not infer a consolidated total or reclassify any canonical transaction.
 */
object SharedAccountingViewPolicy {
    enum class Status { INDEPENDENT_SCOPES, REVIEW_REQUIRED }

    data class View(
        val year: Int,
        val activityIncomeMinor: Long?,
        val activitySpendingMinor: Long?,
        val statementGrossInMinor: Long?,
        val statementGrossOutMinor: Long?,
        val reviewedInternalTransfersMinor: Long,
        val confirmedUniqueCashLegs: Int,
        val confirmedInternalTransfers: Int,
        val confirmedSameLegGroups: Int,
        val unresolvedEvidenceCount: Int,
        val status: Status,
        val postingAllowed: Boolean = false,
        val comparableTotals: Boolean = false
    )

    fun project(
        year: Int,
        totals: CrossSourceTotalsPreviewPolicy.Preview,
        cashLegs: StatementReconciledLegProjectionPolicy.Projection
    ): View {
        require(year in 1970..2100)
        require(cashLegs.confirmedInternalTransferMinor >= 0L)
        require(cashLegs.uniqueCashLegCount >= 0 && cashLegs.confirmedInternalTransfers >= 0 &&
            cashLegs.confirmedSameLegGroups >= 0 && cashLegs.ambiguousObservationCount >= 0)
        val complete = totals.activity != null &&
            totals.statementStatus == StatementUnifiedOverviewPolicy.CashStatus.AVAILABLE &&
            totals.statementMoneyInMinor != null && totals.statementMoneyOutMinor != null &&
            totals.unresolvedCount == 0 && cashLegs.ambiguousObservationCount == 0
        return View(
            year = year,
            activityIncomeMinor = totals.activity?.incomeMinor,
            activitySpendingMinor = totals.activity?.spendingMinor,
            statementGrossInMinor = totals.statementMoneyInMinor,
            statementGrossOutMinor = totals.statementMoneyOutMinor,
            reviewedInternalTransfersMinor = cashLegs.confirmedInternalTransferMinor,
            confirmedUniqueCashLegs = cashLegs.uniqueCashLegCount,
            confirmedInternalTransfers = cashLegs.confirmedInternalTransfers,
            confirmedSameLegGroups = cashLegs.confirmedSameLegGroups,
            unresolvedEvidenceCount = maxOf(totals.unresolvedCount, cashLegs.ambiguousObservationCount),
            status = if (complete) Status.INDEPENDENT_SCOPES else Status.REVIEW_REQUIRED
        )
    }
}
