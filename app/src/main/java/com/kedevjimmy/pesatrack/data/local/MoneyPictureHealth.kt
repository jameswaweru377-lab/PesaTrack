package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy

enum class MoneyPictureIssueType {
    REVIEW_TRANSACTIONS,
    CATEGORIZATION,
    ACCOUNT_BALANCES,
    SALARY_CYCLE
}

data class MoneyPictureIssue(
    val type: MoneyPictureIssueType,
    val title: String,
    val detail: String
)

data class MoneyPictureHealth(
    val score: Int,
    val understoodTransactions: Int,
    val consideredTransactions: Int,
    val needsReviewCount: Int,
    val lowQualitySpendingShare: Int,
    val confirmedAccountCount: Int,
    val knownBalanceAccountCount: Int,
    val salaryCycleActive: Boolean,
    val issues: List<MoneyPictureIssue>
) {
    val label: String
        get() =
            when {
                score >= 90 -> "Excellent"
                score >= 75 -> "Strong"
                score >= 55 -> "Improving"
                score >= 30 -> "Needs attention"
                else -> "Getting started"
            }
}

class MoneyPictureHealthEvaluator(
    context: Context
) {
    private val app =
        context.applicationContext

    fun evaluate(
        nowEpochMs: Long =
            System.currentTimeMillis()
    ): MoneyPictureHealth =
        synchronized(
            CanonicalStoreLock.monitor
        ) {
            val transactionStore =
                TransactionStore.get(app)
            val activity =
                transactionStore.activitySnapshot(
                    nowEpochMs = nowEpochMs
                )

            val considered =
                activity.transactions.filter {
                    it.reviewState !=
                        ReviewState.EXCLUDED.name &&
                        it.type != "REVERSAL"
                }
            val understood =
                activity.trustedTransactions.filter {
                    it.reviewState !=
                        ReviewState.EXCLUDED.name &&
                        it.reviewState !=
                        ReviewState.NEEDS_REVIEW.name &&
                        it.type != "REVERSAL"
                }

            val understandingScore =
                if (considered.isEmpty()) {
                    0
                } else {
                    (
                        understood.size * 40.0 /
                            considered.size
                        )
                        .toInt()
                        .coerceIn(0, 40)
                }

            val trustedSpending =
                understood.filter {
                    it.direction ==
                        MoneyDirection.EXPENSE.name
                }
            val spendingTotal =
                trustedSpending.sumOf {
                    it.amountMinor +
                        it.feeMinor
                }
            val lowQualityMinor =
                trustedSpending
                    .filter {
                        it.category ==
                            TransactionCategory
                                .UNCATEGORIZED.name ||
                            it.category ==
                            TransactionCategory
                                .OTHER.name
                    }
                    .sumOf {
                        it.amountMinor +
                            it.feeMinor
                    }
            val lowQualityShare =
                if (spendingTotal <= 0L) {
                    0
                } else {
                    (
                        lowQualityMinor *
                            100L /
                            spendingTotal
                        )
                        .toInt()
                        .coerceIn(0, 100)
                }
            val categoryScore =
                if (trustedSpending.isEmpty()) {
                    0
                } else {
                    (
                        25 *
                            (100 -
                                lowQualityShare) /
                            100
                        )
                        .coerceIn(0, 25)
                }

            val accountStore =
                AccountStore.get(app)
            val accounts =
                accountStore.listAccounts(
                    enabledOnly = true
                )
            val confirmedIds =
                FinancialUnderstandingStore
                    .get(app)
                    .confirmedAccountIds()
            val confirmedGroups =
                AccountDisplayPolicy
                    .groups(accounts)
                    .filter { group ->
                        group.accounts.any {
                            AccountDisplayPolicy
                                .isConfirmedOwned(
                                    account = it,
                                    confirmedAccountIds =
                                        confirmedIds
                                )
                        }
                    }

            val groupedAccounts =
                confirmedGroups
                    .flatMap {
                        it.accounts
                    }
                    .distinctBy {
                        it.id
                    }
            val snapshots =
                PaydaySnapshotStore
                    .get(app)
                    .currentAccountSnapshots(
                        accounts =
                            groupedAccounts,
                        nowEpochMs =
                            nowEpochMs
                    )
                    .associateBy {
                        it.accountId
                    }

            val knownGroups =
                confirmedGroups.count {
                        group ->
                    group.accounts.any {
                        snapshots[it.id]
                            ?.balanceMinor !=
                            null
                    }
                }
            val accountScore =
                if (
                    confirmedGroups.isEmpty()
                ) {
                    0
                } else {
                    (
                        knownGroups * 20 /
                            confirmedGroups.size
                        )
                        .coerceIn(0, 20)
                }

            val cycle =
                PayCycleStore.get(app)
                    .snapshot()
                    .cycle
            val hasSalaryRule =
                RuleStore.get(app)
                    .listRules(
                        enabledOnly = true
                    )
                    .any {
                        it.startsPayCycle
                    }
            val salaryScore =
                when {
                    cycle != null -> 15
                    hasSalaryRule -> 7
                    else -> 0
                }

            val issues =
                buildList {
                    val needsReview =
                        activity.summary
                            .needsReviewCount
                    if (needsReview > 0) {
                        add(
                            MoneyPictureIssue(
                                type =
                                    MoneyPictureIssueType
                                        .REVIEW_TRANSACTIONS,
                                title =
                                    "$needsReview transaction" +
                                        if (
                                            needsReview ==
                                            1
                                        ) {
                                            " needs review"
                                        } else {
                                            "s need review"
                                        },
                                detail =
                                    "Confirm unclear transactions so totals and plans stay trustworthy."
                            )
                        )
                    }

                    if (
                        lowQualityShare >= 20
                    ) {
                        add(
                            MoneyPictureIssue(
                                type =
                                    MoneyPictureIssueType
                                        .CATEGORIZATION,
                                title =
                                    "$lowQualityShare% of spending is still broad or uncategorized",
                                detail =
                                    "Classify Other and Uncategorized spending to improve reports and insights."
                            )
                        )
                    }

                    if (
                        confirmedGroups.isNotEmpty() &&
                        knownGroups <
                        confirmedGroups.size
                    ) {
                        val missing =
                            confirmedGroups.size -
                                knownGroups
                        add(
                            MoneyPictureIssue(
                                type =
                                    MoneyPictureIssueType
                                        .ACCOUNT_BALANCES,
                                title =
                                    "$missing confirmed account" +
                                        if (
                                            missing ==
                                            1
                                        ) {
                                            " has no known balance"
                                        } else {
                                            "s have no known balance"
                                        },
                                detail =
                                    "PesaTrack will keep these balances unknown until real evidence is available."
                            )
                        )
                    }

                    if (cycle == null) {
                        add(
                            MoneyPictureIssue(
                                type =
                                    MoneyPictureIssueType
                                        .SALARY_CYCLE,
                                title =
                                    if (
                                        hasSalaryRule
                                    ) {
                                        "Salary is configured but no active pay cycle has started"
                                    } else {
                                        "No salary cycle is active"
                                    },
                                detail =
                                    "A confirmed salary receipt starts safe-to-spend and pay-cycle planning."
                            )
                        )
                    }
                }

            MoneyPictureHealth(
                score =
                    (
                        understandingScore +
                            categoryScore +
                            accountScore +
                            salaryScore
                        )
                        .coerceIn(0, 100),
                understoodTransactions =
                    understood.size,
                consideredTransactions =
                    considered.size,
                needsReviewCount =
                    activity.summary
                        .needsReviewCount,
                lowQualitySpendingShare =
                    lowQualityShare,
                confirmedAccountCount =
                    confirmedGroups.size,
                knownBalanceAccountCount =
                    knownGroups,
                salaryCycleActive =
                    cycle != null,
                issues = issues
            )
        }
}
