package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import com.kedevjimmy.pesatrack.ui.theme.ProfileVisualColors as C
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
            "Spending" -> "Spending is running ahead of plan."
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
            "Your spending looks on track.",
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
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp), shadowElevation = 2.dp,
        color = Color.White) {
      Box(Modifier.background(Brush.horizontalGradient(listOf(Color.White, C.greenSoft.copy(alpha = .52f), C.lilac.copy(alpha = .45f))))) {
        Row(Modifier.fillMaxWidth().heightIn(min = 94.dp).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(C.greenSoft),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.BarChart, null, tint = C.green,
                    modifier = Modifier.size(36.dp).align(Alignment.BottomCenter))
                Icon(Icons.Rounded.Eco, null, tint = C.green,
                    modifier = Modifier.size(28.dp).align(Alignment.TopEnd))
            }
            Column(Modifier.weight(1f).padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Better money habits", style = MaterialTheme.typography.titleSmall,
                        color = C.ink, fontWeight = FontWeight.Bold)
                    Surface(shape = CircleShape, color = when (state.kind) {
                        ProfileMoneyHabitsKind.AT_RISK -> C.redSoft
                        ProfileMoneyHabitsKind.LOOKING_GOOD -> C.greenSoft
                        else -> C.lilac
                    }) {
                        Text(state.label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = when (state.kind) {
                                ProfileMoneyHabitsKind.AT_RISK -> C.red
                                ProfileMoneyHabitsKind.LOOKING_GOOD -> C.green
                                else -> C.violet
                            }, style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold)
                    }
                }
                Text(state.subtitle, style = MaterialTheme.typography.bodySmall,
                    color = C.muted, maxLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("See what changed", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, color = C.violet)
                    androidx.compose.material3.Icon(Icons.Rounded.ChevronRight, null,
                        modifier = Modifier.size(16.dp), tint = C.violet)
                }
            }
            androidx.compose.material3.Icon(Icons.Rounded.ChevronRight,
                contentDescription = "View money habits",
                tint = C.muted)
        }
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
    val risky = state.kind == ProfileMoneyHabitsKind.AT_RISK
    val healthy = state.kind == ProfileMoneyHabitsKind.LOOKING_GOOD
    val headline = when (state.kind) {
        ProfileMoneyHabitsKind.AT_RISK -> "At risk"
        ProfileMoneyHabitsKind.LOOKING_GOOD -> "Looking good"
        ProfileMoneyHabitsKind.NEEDS_INFORMATION -> "Let's build a clearer picture"
    }
    ProfilePremiumPage("Better money habits", padding, back) {
        item {
            ProfileMoneyStatusHero(headline, state.subtitle, state.kind)
        }
        item { ProfileSection(when { risky -> "Needs attention"; healthy -> "What's going well"; else -> "What we can see" }) }
        item { ProfilePremiumGroup(listOf(
            ProfilePremiumItem(if (risky) Icons.Rounded.TrendingUp else Icons.Rounded.CalendarMonth,
                if (risky) "Spending pace" else "Bills and plans",
                signals?.indicators?.firstOrNull { it.title == "Spending" }?.reason
                    ?: if (risky) "Review what's going out" else if (healthy) "Check what is coming up" else "Add bills to see your progress",
                if (risky) ProfileTone.PINK else ProfileTone.GREEN,
                onClick = { onAction("plan") }),
            ProfilePremiumItem(if (risky) Icons.Rounded.CalendarMonth else Icons.Rounded.TrendingUp,
                if (risky) "Upcoming bills" else "Spending pace",
                if (risky) "Review what is due" else if (healthy) "See how your spending compares" else "More records will make this clearer",
                if (risky) ProfileTone.AMBER else ProfileTone.BLUE,
                onClick = { onAction("plan") })
        )) }
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = C.lilac) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProfileTile(Icons.Rounded.Lightbulb, ProfileTone.VIOLET, 56.dp)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(if (risky) "Try this first" else if (healthy) "Keep it up" else "Your next step",
                            color = C.ink, fontWeight = FontWeight.Bold)
                        Text(if (risky) "Check what you can pause this week"
                            else if (healthy) "Check your plan once a week"
                            else "Review your plan and recent activity", color = C.muted)
                    }
                }
            }
        }
        item {
            ProfileGradientButton(if (risky) "Review my plan" else "View my plan") { onAction("plan") }
        }
        if (state.isProvisional) item {
            ProfileHint("This is based only on the records available on your phone.", Icons.Rounded.Info)
        }
    }
}

@Composable
private fun ProfileMoneyStatusHero(title: String, detail: String, kind: ProfileMoneyHabitsKind) {
    val risk = kind == ProfileMoneyHabitsKind.AT_RISK
    val accent = when (kind) {
        ProfileMoneyHabitsKind.AT_RISK -> C.coral
        ProfileMoneyHabitsKind.LOOKING_GOOD -> C.green
        ProfileMoneyHabitsKind.NEEDS_INFORMATION -> C.violet
    }
    Box(Modifier.fillMaxWidth().height(232.dp).clip(RoundedCornerShape(20.dp))
        .background(Brush.linearGradient(listOf(if (risk) C.coralSoft else C.greenSoft, Color.White,
            if (risk) C.redSoft else C.blueSoft)))) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width; val h = size.height
            val wave = Path().apply {
                moveTo(0f, h * .68f)
                cubicTo(w * .27f, h * .9f, w * .52f, h * .96f, w, h * .53f)
                lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(wave, accent.copy(alpha = .08f))
            val line = Path().apply {
                moveTo(0f, h * .79f)
                cubicTo(w * .27f, h * 1.08f, w * .48f, h * .72f, w * .69f, h * .58f)
                cubicTo(w * .78f, h * .49f, w * .91f, h * .51f, w, h * .55f)
            }
            drawPath(line, accent, style = Stroke(width = 3.dp.toPx()))
            drawCircle(accent, radius = 9.dp.toPx(), center = Offset(w * .69f, h * .58f))
            drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(w * .69f, h * .58f))
        }
        Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(72.dp), shape = CircleShape, color = accent,
                border = androidx.compose.foundation.BorderStroke(5.dp, Color.White), shadowElevation = 8.dp) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (risk) Icons.Rounded.WarningAmber else if (kind == ProfileMoneyHabitsKind.LOOKING_GOOD)
                        Icons.Rounded.Check else Icons.Rounded.Info, null, tint = Color.White,
                        modifier = Modifier.size(42.dp))
                }
            }
            Column(Modifier.padding(start = 16.dp)) {
                Text(title, color = C.ink, style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold)
                Text(detail, color = C.muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
