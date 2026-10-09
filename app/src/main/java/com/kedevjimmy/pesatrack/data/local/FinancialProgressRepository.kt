package com.kedevjimmy.pesatrack.data.local
import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.model.checkedMoneySum
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import java.util.Calendar

data class ProgressItem(val id: String, val name: String, val saved: Long, val target: Long)
data class FinancialProgress(val accounts: Int, val budgets: List<ProgressItem>, val goals: List<ProgressItem>)
class FinancialProgressRepository(private val context: Context) {
    fun read(): FinancialProgress = synchronized(CanonicalStoreLock.monitor) {
        val transactions = TransactionStore.get(context)
        val categories = CategoryStore.get(context)
        val start = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH,1); set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }
        val end = (start.clone() as Calendar).apply { add(Calendar.MONTH,1) }
        val spending = BudgetReadRepository(context).spendingByCategory()
        val allAccounts = AccountStore.get(context).listAccounts(false)
        val types = allAccounts.associate { it.id to it.type }
        val confirmed = FinancialUnderstandingStore.get(context).confirmedAccountIds()
        val count = AccountDisplayPolicy.groups(AccountStore.get(context).listAccounts(true)).count { group -> group.accounts.any { AccountDisplayPolicy.isConfirmedOwned(it, confirmed) } }
        val rules = RuleStore.get(context).listRules(true).filter { it.category == TransactionCategory.SAVINGS && (it.plannedAmountMinor ?: 0) > 0 }
        val metadata = SavingGoalMetadataStore.get(context).listForRules(rules.map { it.id })
        val rows = transactions.activitySnapshotBetween(0, System.currentTimeMillis() + 1, System.currentTimeMillis(), performMaintenance = false).trustedTransactions
        val goals = rules.mapNotNull { rule ->
            val hasTotal = (metadata[rule.id]?.totalTargetMinor ?: 0L) > 0L
            val goalRows = if (hasTotal) rows else rows.filter { it.occurredAtEpochMs >= start.timeInMillis && it.occurredAtEpochMs < end.timeInMillis }
            val saved = GoalSavingHistoryPolicy.netSaved(GoalSavingHistoryPolicy.observations(goalRows, rule.id, types)) ?: return@mapNotNull null
            val target = metadata[rule.id]?.totalTargetMinor?.takeIf { it > 0 } ?: rule.plannedAmountMinor ?: return@mapNotNull null
            ProgressItem(rule.id, (rule.displayLabel?.takeIf { it.isNotBlank() } ?: rule.name.ifBlank { "Savings goal" }) + if (hasTotal) "" else " (this month)", saved, target)
        }
        FinancialProgress(count, BudgetStore.get(context).list().map { ProgressItem(it.categoryId, categories.labelFor(it.categoryId), spending[it.categoryId] ?: 0, it.limitMinor) }, goals)
    }
}
