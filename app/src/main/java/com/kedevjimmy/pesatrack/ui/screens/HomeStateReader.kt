package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.*

/** Reads saved money; never turns navigation into an ingestion or repair job. */
internal object HomeStateReader {
    fun read(context: Context): HomeLoadedState {
        val transactions = TransactionStore.get(context)
        val review = transactions.readActivityReviewOverview()
        return HomeLoadedState(
            cycleSnapshot = PayCycleStore.get(context).readSnapshot(),
            ledgerSnapshot = transactions.readSnapshot(),
            activitySnapshot = transactions.readActivitySnapshot(),
            debtAccounts = DebtStore.get(context).readAccounts(),
            trackedAccounts = AccountStore.get(context).listAccounts(enabledOnly = true),
            confirmedAccountIds = FinancialUnderstandingStore.get(context).confirmedAccountIds(),
            salaryRule = RuleStore.get(context).listRules(enabledOnly = true).firstOrNull { it.startsPayCycle },
            needsAttentionCount = review.reviewGroups.sumOf { it.transactionCount } +
                review.possibleDuplicates.size +
                RelationshipReviewStore.get(context).readPendingReviews().size +
                IdentityGraphStore.get(context).pendingReviewPrompts(limit = 100, includeOptional = false).size,
            categoryLabels = CategoryStore.get(context).all().associate { it.id to it.label }
        )
    }
}
