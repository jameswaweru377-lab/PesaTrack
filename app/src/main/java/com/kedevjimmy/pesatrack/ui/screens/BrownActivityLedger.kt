package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens
import java.text.SimpleDateFormat
import java.util.Date

/** A day's amount is calculated elsewhere from all trusted cash legs, not visible row filters. */
internal fun brownActivityNetLabel(netMinor: Long?): String = when {
    netMinor == null -> "Net unavailable"
    netMinor > 0 -> "Net +${activityKesLabel(netMinor)}"
    netMinor < 0 -> CheckedMoney.orNull { CheckedMoney.negate(netMinor) }
        ?.let { "Net −${activityKesLabel(it)}" } ?: "Net unavailable"
    else -> "Net KES 0"
}

/** Every day retains its right-aligned recorded net and its own differentiated ledger group. */
@Composable
internal fun BrownActivityDateHeader(dayEpochMs: Long, netMinor: Long?) {
    val p = homeColors()
    val dayName = dayLabel(dayEpochMs)
    val shortDate = SimpleDateFormat("d MMM", LocalConfiguration.current.locales[0])
        .format(Date(dayEpochMs))
    val title = if (dayName == "Today" || dayName == "Yesterday")
        "$dayName · $shortDate" else dayName
    val panel = if (p.canvas == PremiumVisualTokens.darkCanvas) p.cream else p.white
    Surface(modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = panel) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, Modifier.weight(1f), color = p.ink,
                fontFamily = FontFamily.Serif, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(brownActivityNetLabel(netMinor), color = p.coffee, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun BrownActivityRow(
    item: StoredTransaction,
    position: ActivityGroupPosition,
    debtLink: ReceivableActivityLink?,
    accountName: String?,
    onClick: () -> Unit
) {
    val p = homeColors()
    val context = LocalContext.current
    val categories = remember { CategoryStore.get(context) }
    val categoryId = categories.canonicalId(item.category)
    val direction = runCatching { MoneyDirection.valueOf(item.direction) }
        .getOrDefault(MoneyDirection.NEUTRAL)
    val needsReview = item.reviewState == ReviewState.NEEDS_REVIEW.name
    val refund = item.relationType == TransactionStore.RELATION_REFUND
    val dark = p.canvas == PremiumVisualTokens.darkCanvas
    val panel = if (dark) p.cream else p.white
    val displayName = financialActivityDisplayName(
        item = item,
        rawDisplayName = item.evidenceLabel?.takeIf {
            item.evidenceSource?.startsWith("entity_intelligence:") == true
        } ?: item.counterparty ?: humanize(item.type),
        accountName = accountName
    )
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = if (position == ActivityGroupPosition.LAST ||
            position == ActivityGroupPosition.ONLY)
            RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
        else RoundedCornerShape(0.dp), color = panel) {
        Column {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(PesaDesignTokens.iconBubble),
                    contentAlignment = Alignment.Center) {
                    Surface(shape = RoundedCornerShape(PesaDesignTokens.iconRadius),
                        color = if (dark) p.white else p.cream) {
                        Box(Modifier.size(PesaDesignTokens.iconBubble),
                            contentAlignment = Alignment.Center) {
                            val visual = activityCategoryIconVisual(categories.labelFor(categoryId), direction)
                            Icon(visual.icon, null, Modifier.size(21.dp), tint = visual.tint)
                        }
                    }
                    if (needsReview) {
                        Surface(Modifier.align(Alignment.BottomEnd).size(8.dp),
                            shape = RoundedCornerShape(8.dp), color = p.coffee,
                            border = BorderStroke(1.dp, p.white)) {}
                    }
                }
                val subtitle = when {
                    needsReview -> "Needs Review"
                    refund -> "Refund"
                    debtLink?.active == true && debtLink.kind == "LENT" -> "Money lent"
                    debtLink?.active == true && debtLink.kind == "REPAID" -> "Repayment"
                    direction == MoneyDirection.TRANSFER -> "Transfer"
                    else -> categories.labelFor(categoryId)
                }
                BrownActivityRowDescription(displayName, subtitle,
                    activityPaymentMethod(item), item.occurredAtEpochMs, dark,
                    modifier = Modifier.weight(1f))
                BrownActivityRowAmount(item, direction, refund)
            }
            if (position == ActivityGroupPosition.FIRST ||
                position == ActivityGroupPosition.MIDDLE) {
                HorizontalDivider(Modifier.padding(start = 64.dp, end = 12.dp),
                    color = p.outline.copy(alpha = .75f), thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun BrownActivityRowDescription(
    title: String, category: String, method: String?, epochMs: Long,
    dark: Boolean, modifier: Modifier = Modifier
) {
    val p = homeColors()
    val time = SimpleDateFormat("h:mm a", LocalConfiguration.current.locales[0])
        .format(Date(epochMs))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (method != null) {
                Surface(color = if (dark) p.white else p.cream,
                    shape = RoundedCornerShape(PesaDesignTokens.iconRadius)) {
                    Text(method, Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = p.ink, fontWeight = FontWeight.Medium,
                        fontSize = 9.sp, maxLines = 1)
                }
                Text("·", color = p.muted, fontSize = 10.sp)
            }
            Text(category, Modifier.weight(1f, fill = false),
                fontSize = 10.sp, color = p.muted, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text("·", color = p.muted, fontSize = 10.sp)
            Text(time, fontSize = 10.sp, color = p.muted, maxLines = 1)
        }
    }
}

@Composable
private fun BrownActivityRowAmount(
    item: StoredTransaction, direction: MoneyDirection, refund: Boolean
) {
    Column(horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(amountLabel(item, direction)
            .replace("+ KES", "+KES").replace("- KES", "−KES"),
            modifier = Modifier.widthIn(max = 124.dp),
            color = themeMoneyMovementColor(direction, refund),
            fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
