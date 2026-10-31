package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.FinancialHealthEvidenceStore
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.checkedMoneySumOf
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import java.util.Calendar

internal enum class ProfileMoneyHabitsKind { LOOKING_GOOD, AT_RISK, NEEDS_INFORMATION }

internal data class ProfileMoneyHabitsPresentation(
    val kind: ProfileMoneyHabitsKind,
    val label: String,
    val subtitle: String,
    val isProvisional: Boolean,
    val callToAction: String
)

/** Never describe missing observations as a healthy financial assessment. */
internal fun presentProfileMoneyHabits(signals: HomeFinancialSignals?): ProfileMoneyHabitsPresentation {
    if (signals == null) return ProfileMoneyHabitsPresentation(
        ProfileMoneyHabitsKind.NEEDS_INFORMATION,
        "Not enough information yet",
        "Open Home to refresh your money picture.",
        isProvisional = true,
        callToAction = "Open my plan"
    )
    val risk = signals.indicators.firstOrNull { it.score != null && it.score < 60 }
    val atRisk = signals.indicators.any { it.score != null && it.score < 45 } ||
        signals.dataSufficient && signals.status != "On track"
    if (atRisk) return ProfileMoneyHabitsPresentation(
        ProfileMoneyHabitsKind.AT_RISK,
        "At risk",
        when (risk?.title) {
            "Spending" -> "Spending pace needs attention."
            "Cash flow" -> "Money going out needs attention."
            "Savings" -> "Your savings buffer needs attention."
            "Debt" -> "Your repayments need attention."
            else -> "Your money plan needs attention."
        },
        isProvisional = !signals.dataSufficient,
        callToAction = "Review my plan"
    )
    if (signals.dataSufficient && signals.status == "On track") {
        return ProfileMoneyHabitsPresentation(
            ProfileMoneyHabitsKind.LOOKING_GOOD, "Looking good",
            "Your tracked money habits are on course.",
            isProvisional = false,
            callToAction = "Keep it going"
        )
    }
    return ProfileMoneyHabitsPresentation(
        ProfileMoneyHabitsKind.NEEDS_INFORMATION,
        "Not enough information yet",
        "A few details are missing from your money picture.",
        isProvisional = true,
        callToAction = "Improve my picture"
    )
}

/**
 * Reuse the Home evaluator with Home's same reviewed current-month snapshot.
 * If another screen changed the ledger, an old cached result is never shown.
 */
internal fun profileFinancialSignals(
    context: Context,
    homeCache: HomeScreenCache?,
    ledgerRevision: Long
): HomeFinancialSignals? {
    val state = homeCache?.data?.takeIf { homeCache.revision == ledgerRevision }
        ?: return null
    val calendar = Calendar.getInstance()
    val historyKey = calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
    val previous = homeCache.historyFor(ledgerRevision, 0, historyKey)?.previousAligned
    val category = CategoryStore.get(context)
    val activity = state.activitySnapshot
    val trusted = activity.trustedTransactions.filter {
        it.reviewState != ReviewState.EXCLUDED.name
    }
    val reviewedExpenses = trusted.filter {
        it.direction == MoneyDirection.EXPENSE.name &&
            it.reviewState != ReviewState.NEEDS_REVIEW.name
    }
    val outflow = CheckedMoney.orNull {
        maxOf(0L, CheckedMoney.total(
            activity.summary.spentMinor,
            activity.summary.feesMinor,
            CheckedMoney.negate(activity.summary.refundMinor)
        ))
    }
    val allExpense = CheckedMoney.orNull { reviewedExpenses.checkedMoneySumOf { it.amountMinor } }
    val uncategorizedExpense = CheckedMoney.orNull {
        reviewedExpenses.filter {
            category.canonicalId(it.category) in setOf("OTHER", "UNCATEGORIZED")
        }.checkedMoneySumOf { it.amountMinor }
    }
    val unknownFraction = if (allExpense != null && allExpense > 0L &&
        uncategorizedExpense != null) uncategorizedExpense.toDouble() / allExpense.toDouble()
    else null
    val budgets = financialHealthBudgetTotals(
        state.monthlyBudgets,
        state.budgetSpendingByCategory,
        category::canonicalId
    )
    val activeDebt = state.debtAccounts.filter { it.isActive }
    val ownedGroups = AccountDisplayPolicy.groups(state.trackedAccounts, includeCash = true)
        .count { group ->
            group.accounts.any { account ->
                AccountDisplayPolicy.isConfirmedOwned(account, state.confirmedAccountIds, includeCash = true)
            }
        }
    val evidence = FinancialHealthEvidenceStore(context).snapshot()
    return evaluateHomeFinancialSignals(HomeFinancialSignalsInput(
        budgetLimitMinor = budgets.limitMinor,
        budgetSpentMinor = budgets.spentMinor,
        uncategorizedShare = unknownFraction,
        currentSpentMinor = outflow,
        previousAlignedSpentMinor = null,
        incomeMinor = activity.summary.incomeMinor,
        debtOutstandingMinor = null,
        unknownDebtBalances = activeDebt.any {
            it.balanceQuality == com.kedevjimmy.pesatrack.data.local.DebtBalanceQuality.UNKNOWN
        },
        feesMinor = activity.summary.feesMinor,
        currentTrustedRows = trusted.count { it.reviewState != ReviewState.NEEDS_REVIEW.name },
        previousTrustedRows = previous?.trustedTransactions?.size ?: 0,
        pendingReviews = activity.summary.needsReviewCount,
        confirmedAccountCount = ownedGroups,
        hasConfirmedSalaryCycle = state.cycleSnapshot.cycle != null,
        debtAccountCount = activeDebt.size,
        remainingCommitmentsMinor = if (state.cycleSnapshot.cycle != null)
            state.cycleSnapshot.remainingCommitmentsMinor else null,
        confirmedEmergencyReserveMinor = evidence.emergencyReserveMinor,
        confirmedEssentialCostsMinor = evidence.essentialMonthlyMinor,
        confirmedMonthlyDebtPaymentsMinor = evidence.requiredDebtPaymentsMinor,
        elapsedMonthFraction = calendar.get(Calendar.DAY_OF_MONTH).toDouble() /
            calendar.getActualMaximum(Calendar.DAY_OF_MONTH).toDouble()
    ))
}

@Composable
internal fun ProfileMoneyHabitsCard(
    state: ProfileMoneyHabitsPresentation,
    onClick: () -> Unit
) {
    val tone = when (state.kind) {
        ProfileMoneyHabitsKind.LOOKING_GOOD -> "green"
        ProfileMoneyHabitsKind.AT_RISK -> "amber"
        ProfileMoneyHabitsKind.NEEDS_INFORMATION -> "violet"
    }
    PesaCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Rounded.Spa, size = 48.dp,
                background = pesaToneContainer("green"), tint = pesaToneContent("green"))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Better money habits", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Surface(shape = MaterialTheme.shapes.small, color = pesaToneContainer(tone)) {
                    Text(state.label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = pesaToneContent(tone), style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold)
                }
                Text(state.subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            androidx.compose.material3.Icon(Icons.Rounded.ChevronRight,
                contentDescription = "View money habits",
                tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
internal fun ProfileMoneyHabitsDetails(
    padding: PaddingValues,
    back: () -> Unit,
    signals: HomeFinancialSignals?,
    onAction: (String) -> Unit
) {
    val state = presentProfileMoneyHabits(signals)
    EverydayPage("Better money habits", padding, back) {
        item {
            PesaCard {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(when (state.kind) {
                            ProfileMoneyHabitsKind.LOOKING_GOOD -> Icons.Rounded.TrendingUp
                            ProfileMoneyHabitsKind.AT_RISK -> Icons.Rounded.WarningAmber
                            ProfileMoneyHabitsKind.NEEDS_INFORMATION -> Icons.Rounded.Info
                        }, size = 48.dp, background = pesaToneContainer(when (state.kind) {
                            ProfileMoneyHabitsKind.LOOKING_GOOD -> "green"
                            ProfileMoneyHabitsKind.AT_RISK -> "amber"
                            ProfileMoneyHabitsKind.NEEDS_INFORMATION -> "violet"
                        }))
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(state.label, style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                            Text(state.subtitle, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (state.isProvisional) {
                        Text("This is a partial picture. We only use records you have saved or reviewed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Based on your saved records, not a live bank balance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { SectionHeader("What we can see") }
        if (signals == null) {
            item {
                PesaCard {
                    Text("Open Home to refresh your records, then come back to see your money habits.",
                        modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            signals.indicators.forEach { indicator ->
                item {
                    PesaCard {
                        Column(Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(indicator.title, style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                            Text(indicator.reason, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item { SectionHeader("A good next step") }
        item {
            val action = signals?.actions?.firstOrNull()
            PesaCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(action?.title ?: "Review your monthly plan",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(action?.detail ?: "Your plan can help you build a clearer money picture.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { onAction(action?.destination ?: "plan") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(action?.title ?: state.callToAction)
                    }
                }
            }
        }
        item {
            Text("This is guidance from your own records, not a credit score or financial advice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
