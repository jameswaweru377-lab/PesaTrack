package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens

internal data class BrownActivityFilter(val label: String, val key: String)

internal val brownActivityFilters = listOf(
    BrownActivityFilter("All", "All"),
    BrownActivityFilter("Money In", "Money In"),
    BrownActivityFilter("Money Out", "Money Out"),
    BrownActivityFilter("Transfers", "Transfers"),
    BrownActivityFilter("Review", "Needs Review")
)

internal val brownActivityPrimaryFilters = brownActivityFilters.take(3)
internal val brownActivityAdvancedFilters = brownActivityFilters.drop(3)

internal fun brownActivityReviewNotice(count: Int): String? = when {
    count <= 0 -> null
    count == 1 -> "1 item needs review"
    else -> "$count need review"
}

internal fun brownActivityFilterKey(label: String): String? =
    brownActivityFilters.firstOrNull { it.label == label }?.key

internal fun brownActivityPeriodCaption(payCycleMode: Boolean): String =
    if (payCycleMode) "THIS PAY CYCLE" else "THIS MONTH"

/** Ledger-first Activity header: its full search field and advanced tools are disclosed on demand. */
@Composable
internal fun BrownActivityHeader(
    month: String,
    payCycleMode: Boolean,
    searchOpen: Boolean,
    searchQuery: String,
    filtersActive: Boolean,
    onMonth: () -> Unit,
    onCycle: () -> Unit,
    onToggleSearch: () -> Unit,
    onQuery: (String) -> Unit,
    onToggleFilters: () -> Unit
) {
    val p = homeColors()
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = PesaDesignTokens.minimumTouchTarget),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Activity", Modifier.weight(1f), color = p.ink,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp)
            IconButton(onClick = onToggleSearch, modifier = Modifier.size(48.dp)) {
                Icon(if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                    if (searchOpen) "Close search" else "Search transactions",
                    Modifier.size(24.dp), tint = p.ink)
            }
            IconButton(onClick = onToggleFilters, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Tune, "Advanced filters and tools",
                    Modifier.size(24.dp), tint = if (filtersActive) p.coffee else p.ink)
            }
        }
        Row(Modifier.fillMaxWidth().heightIn(min = PesaDesignTokens.minimumTouchTarget),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onMonth, color = p.canvas,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    .semantics { contentDescription = "Choose Activity month: $month" }) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(month, color = p.coffee, fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Rounded.ExpandMore, null, Modifier.size(20.dp), tint = p.coffee)
                }
            }
            TextButton(onClick = if (payCycleMode) onMonth else onCycle,
                modifier = Modifier.heightIn(min = PesaDesignTokens.minimumTouchTarget)) {
                Text(if (payCycleMode) "Month" else "Pay cycle", color = p.muted,
                    fontSize = 12.sp)
            }
        }
        if (searchOpen) BrownActivityInlineSearch(searchQuery, onQuery)
    }
}

/** Decorative photography only. The four values still come from the trusted local ledger. */
@Composable
internal fun BrownActivityMonthlyHero(
    snapshot: ActivityLedgerSnapshot,
    payCycleMode: Boolean,
    balance: ActivityRecordedBalance?
) {
    val p = homeColors()
    val totals = remember(snapshot) { activityDesign3Totals(snapshot) }
    Box(Modifier.fillMaxWidth().height(172.dp)
        .clip(RoundedCornerShape(PesaDesignTokens.cardRadius))) {
        Image(painterResource(R.drawable.home_brown_monthly_coins), null,
            Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            0f to p.cream.copy(alpha = .96f),
            .57f to p.cream.copy(alpha = .75f),
            1f to p.cream.copy(alpha = .08f))))
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.padding(start = 16.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(brownActivityPeriodCaption(payCycleMode), color = p.coffee,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp, fontSize = 10.sp)
                val expenses = totals.expensesMinor?.let(::activityKesLabel) ?: "Unavailable"
                Text(expenses, color = p.ink, fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (expenses.length > 16) 22.sp else 27.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Total expenses", color = p.coffee, fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic, fontSize = 13.sp)
            }
            Column(Modifier.fillMaxWidth().background(p.cream.copy(alpha = .86f))
                .padding(horizontal = 12.dp, vertical = 6.dp)) {
                HorizontalDivider(color = p.coffee.copy(alpha = .24f), thickness = .5.dp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrownActivityMinorMetric("Money in", totals.incomeMinor,
                        Modifier.weight(1f),
                        if (p.canvas == PremiumVisualTokens.darkCanvas)
                            PremiumVisualTokens.darkMoneyIn else PremiumVisualTokens.moneyIn)
                    BrownActivityMinorMetric(
                        if (balance?.partial == true) "Partial balance" else "Balance",
                        balance?.amountMinor, Modifier.weight(1f)
                    )
                    BrownActivityMinorMetric("Invested", totals.investedMinor, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BrownActivityMinorMetric(
    label: String, minor: Long?, modifier: Modifier,
    valueColor: Color = Color.Unspecified
) {
    val p = homeColors()
    Column(modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, color = p.ink, fontSize = 10.sp, maxLines = 1)
        Text(minor?.let(::activityKesLabel) ?: "Unavailable", color = if (valueColor == Color.Unspecified) p.ink else valueColor,
            fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun BrownActivityReview(count: Int, onReview: () -> Unit) {
    val label = brownActivityReviewNotice(count) ?: return
    val p = homeColors()
    Surface(onClick = onReview, modifier = Modifier.fillMaxWidth(),
        color = p.canvas) {
        Column {
            HorizontalDivider(color = p.outline.copy(alpha = .65f))
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.CheckCircleOutline, null, Modifier.size(24.dp), tint = p.coffee)
                Text(label, modifier = Modifier.weight(1f), color = p.ink,
                    fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Icon(Icons.Rounded.ChevronRight, "Open review", tint = p.coffee)
            }
            HorizontalDivider(color = p.outline.copy(alpha = .65f))
        }
    }
}

@Composable
private fun BrownActivityInlineSearch(query: String, onQuery: (String) -> Unit) {
    val p = homeColors()
    Surface(Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(PesaDesignTokens.controlRadius),
        color = p.white, border = BorderStroke(1.dp, p.outline.copy(alpha = .7f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Search, null, Modifier.size(20.dp), tint = p.muted)
            BasicTextField(query, onQuery, Modifier.weight(1f), singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = p.ink),
                cursorBrush = SolidColor(p.coffee), decorationBox = { field ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text("Search transactions", color = p.muted,
                            style = MaterialTheme.typography.bodyMedium)
                        field()
                    }
                })
        }
    }
}

@Composable
internal fun BrownActivitySegmentedFilters(selected: String, onSelect: (String) -> Unit) {
    val p = homeColors()
    val state = rememberLazyListState()
    LaunchedEffect(selected) {
        brownActivityPrimaryFilters.indexOfFirst { it.key == selected }.takeIf { it >= 0 }
            ?.let { state.animateScrollToItem(it) }
    }
    LazyRow(state = state, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(brownActivityPrimaryFilters) { tab ->
            val active = selected == tab.key
            Surface(onClick = { onSelect(tab.key) },
                modifier = Modifier.heightIn(min = PesaDesignTokens.minimumTouchTarget),
                color = if (active) p.coffee else p.canvas,
                shape = RoundedCornerShape(PesaDesignTokens.controlRadius)) {
                Box(Modifier.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center) {
                    Text(tab.label, fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) MaterialTheme.colorScheme.onPrimary else p.ink,
                        maxLines = 1)
                }
            }
        }
    }
}

@Composable
internal fun BrownActivityRecentHeading(onSeeAll: () -> Unit) {
    val p = homeColors()
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("Recent activity", Modifier.weight(1f), color = p.ink,
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Surface(onClick = onSeeAll, color = p.canvas,
            modifier = Modifier.heightIn(min = 48.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("See all", color = p.coffee, fontSize = 12.sp)
                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = p.coffee)
            }
        }
    }
}
