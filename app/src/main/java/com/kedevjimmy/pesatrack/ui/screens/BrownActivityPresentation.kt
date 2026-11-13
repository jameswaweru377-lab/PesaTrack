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
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

internal fun brownActivityFilterKey(label: String): String? =
    brownActivityFilters.firstOrNull { it.label == label }?.key

internal fun brownActivityPeriodCaption(payCycleMode: Boolean): String =
    if (payCycleMode) "THIS PAY CYCLE" else "THIS MONTH"

@Composable
internal fun BrownActivityHeading(
    month: String,
    payCycleMode: Boolean,
    onMonth: () -> Unit,
    onCycle: () -> Unit
) {
    val p = BrownHomePalette
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp)
        .heightIn(min = PesaDesignTokens.minimumTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Month", "Pay cycle").forEach { mode ->
                val active = (mode == "Pay cycle") == payCycleMode
                Surface(onClick = if (mode == "Pay cycle") onCycle else onMonth,
                    color = if (active) p.coffee else p.cream,
                    shape = RoundedCornerShape(PesaDesignTokens.controlRadius),
                    modifier = Modifier.heightIn(min = PesaDesignTokens.minimumTouchTarget)) {
                    Box(Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center) {
                        Text(mode, fontSize = 12.sp,
                            color = if (active) Color.White else p.coffee,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Surface(onClick = onMonth, color = p.canvas,
            modifier = Modifier.heightIn(min = PesaDesignTokens.minimumTouchTarget)
                .semantics { contentDescription = "Choose Activity month: $month" }) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(month, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = p.coffee, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Rounded.ExpandMore, null, Modifier.size(20.dp), tint = p.coffee)
            }
        }
    }
}

/** Decorative photography only. The four values still come from the trusted local ledger. */
@Composable
internal fun BrownActivityMonthlyHero(
    snapshot: ActivityLedgerSnapshot,
    payCycleMode: Boolean,
    balance: ActivityRecordedBalance?
) {
    val p = BrownHomePalette
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
                        Modifier.weight(1f), PremiumVisualTokens.moneyIn)
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
    valueColor: Color = BrownHomePalette.ink
) {
    val p = BrownHomePalette
    Column(modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, color = p.ink, fontSize = 10.sp, maxLines = 1)
        Text(minor?.let(::activityKesLabel) ?: "Unavailable", color = valueColor,
            fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun BrownActivityReview(count: Int, onReview: () -> Unit) {
    val p = BrownHomePalette
    Surface(onClick = onReview, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PesaDesignTokens.innerRadius), color = p.cream) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.CheckCircleOutline, null, Modifier.size(27.dp), tint = p.coffee)
            Text(if (count == 0) "All reviewed" else "$count need review",
                modifier = Modifier.weight(1f), color = p.ink,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Icon(Icons.Rounded.ChevronRight, "Open review", tint = p.coffee)
        }
    }
}

@Composable
internal fun BrownActivitySearch(
    query: String, onQuery: (String) -> Unit, onFilters: () -> Unit
) {
    val p = BrownHomePalette
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(Modifier.weight(1f).heightIn(min = 48.dp),
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
        Surface(onClick = onFilters, modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(PesaDesignTokens.controlRadius),
            color = p.white, border = BorderStroke(1.dp, p.outline.copy(alpha = .7f))) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Tune, "Advanced filters", Modifier.size(22.dp), tint = p.coffee)
            }
        }
    }
}

@Composable
internal fun BrownActivitySegmentedFilters(selected: String, onSelect: (String) -> Unit) {
    val p = BrownHomePalette
    val state = rememberLazyListState()
    LaunchedEffect(selected) {
        brownActivityFilters.indexOfFirst { it.key == selected }.takeIf { it >= 0 }
            ?.let { state.animateScrollToItem(it) }
    }
    Surface(modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PesaDesignTokens.controlRadius),
        color = p.canvas, border = BorderStroke(1.dp, p.outline.copy(alpha = .65f))) {
        LazyRow(state = state, verticalAlignment = Alignment.CenterVertically) {
            items(brownActivityFilters) { tab ->
                val active = selected == tab.key
                Surface(onClick = { onSelect(tab.key) },
                    modifier = Modifier.heightIn(min = PesaDesignTokens.minimumTouchTarget),
                    color = if (active) p.coffee else p.canvas,
                    shape = RoundedCornerShape(PesaDesignTokens.controlRadius)) {
                    Box(Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center) {
                        Text(tab.label, fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) p.white else p.ink, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
internal fun BrownActivityRecentHeading(onSeeAll: () -> Unit) {
    val p = BrownHomePalette
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
