package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import java.text.SimpleDateFormat
import java.util.Date

/** A daily ledger section is visually one white inset surface, but rows stay individually lazy. */
@Composable
internal fun BrownActivityDateHeader(dayEpochMs: Long, netMinor: Long?) {
    val p = BrownHomePalette
    val dayName = dayLabel(dayEpochMs)
    val shortDate = SimpleDateFormat("d MMM", LocalConfiguration.current.locales[0])
        .format(Date(dayEpochMs))
    val title = if (dayName == "Today" || dayName == "Yesterday")
        "$dayName · $shortDate" else dayName
    val netLabel = when {
        netMinor == null -> "Net unavailable"
        netMinor > 0 -> "Net +${activityKesLabel(netMinor)}"
        netMinor < 0 -> CheckedMoney.orNull { CheckedMoney.negate(netMinor) }
            ?.let { "Net −${activityKesLabel(it)}" } ?: "Net unavailable"
        else -> "Net KES 0"
    }
    Surface(modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = p.white) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, Modifier.weight(1f), color = p.ink,
                fontFamily = FontFamily.Serif, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(netLabel, color = p.coffee, fontSize = 12.sp,
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
    val p = BrownHomePalette
    val context = LocalContext.current
    val categories = remember { CategoryStore.get(context) }
    val categoryId = categories.canonicalId(item.category)
    val direction = runCatching { MoneyDirection.valueOf(item.direction) }
        .getOrDefault(MoneyDirection.NEUTRAL)
    val needsReview = item.reviewState == ReviewState.NEEDS_REVIEW.name
    val refund = item.relationType == TransactionStore.RELATION_REFUND
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
        else RoundedCornerShape(0.dp), color = p.white) {
        Column {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(PesaDesignTokens.iconBubble),
                    contentAlignment = Alignment.Center) {
                    Surface(shape = RoundedCornerShape(PesaDesignTokens.iconRadius),
                        color = p.cream) {
                        Box(Modifier.size(PesaDesignTokens.iconBubble),
                            contentAlignment = Alignment.Center) {
                            Icon(brownActivityCategoryIcon(categoryId, direction), null,
                                Modifier.size(21.dp), tint = p.ink)
                        }
                    }
                    if (needsReview) {
                        Surface(Modifier.align(Alignment.BottomEnd).size(8.dp),
                            shape = RoundedCornerShape(8.dp), color = p.coffee,
                            border = BorderStroke(1.dp, p.white)) {}
                    }
                }
                Column(Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(displayName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val subtitle = when {
                        needsReview -> "Needs Review"
                        refund -> "Refund"
                        debtLink?.active == true && debtLink.kind == "LENT" -> "Money lent"
                        debtLink?.active == true && debtLink.kind == "REPAID" -> "Repayment"
                        direction == MoneyDirection.TRANSFER -> "Transfer"
                        else -> categories.labelFor(categoryId)
                    }
                    Text("$subtitle · " + SimpleDateFormat("h:mm a",
                        LocalConfiguration.current.locales[0]).format(Date(item.occurredAtEpochMs)),
                        fontSize = 11.sp, color = p.muted, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
                val isCredit = refund || direction == MoneyDirection.INCOME ||
                    item.type == MpesaTransactionType.REVERSAL.name
                Text(amountLabel(item, direction)
                    .replace("+ KES", "+KES").replace("- KES", "−KES"),
                    modifier = Modifier.widthIn(max = 124.dp),
                    color = moneyMovementColor(direction, refund),
                    fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (position == ActivityGroupPosition.FIRST ||
                position == ActivityGroupPosition.MIDDLE) {
                HorizontalDivider(Modifier.padding(start = 64.dp, end = 12.dp),
                    color = p.outline.copy(alpha = .5f), thickness = .5.dp)
            }
        }
    }
}

private fun brownActivityCategoryIcon(category: String, direction: MoneyDirection): ImageVector =
    when {
        direction == MoneyDirection.TRANSFER -> Icons.Rounded.SyncAlt
        direction == MoneyDirection.INCOME -> Icons.Rounded.Work
        category.contains("GROCER", true) || category.contains("SHOP", true) ->
            Icons.Rounded.Storefront
        category.contains("RENT", true) || category.contains("HOUS", true) -> Icons.Rounded.Home
        category.contains("FOOD", true) || category.contains("EAT", true) -> Icons.Rounded.Restaurant
        category.contains("ELECT", true) -> Icons.Rounded.Bolt
        category.contains("INTERNET", true) -> Icons.Rounded.Wifi
        category.contains("PHONE", true) -> Icons.Rounded.PhoneAndroid
        else -> Icons.AutoMirrored.Rounded.HelpOutline
    }
