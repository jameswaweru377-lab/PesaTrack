package com.kedevjimmy.pesatrack.ui.screens

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.data.local.ActivitySummary
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

/** Read-only rotating financial context. Never derive a new accounting definition. */
@Composable
internal fun BrownActivityFlowTicker() {
    val context = LocalContext.current
    val store = remember(context) { TransactionStore.get(context) }
    val update by LedgerUpdateBus.state.collectAsState()
    var selected by remember { mutableStateOf(ActivityTickerPeriod.YEAR) }
    val metricScroll = rememberScrollState()
    val metricOpacity = remember { Animatable(1f) }
    val todayStart = activityTickerRange(ActivityTickerPeriod.TODAY,
        System.currentTimeMillis()).startEpochMs
    var totals by remember(update.revision, todayStart) {
        mutableStateOf<Map<ActivityTickerPeriod, ActivitySummary>>(emptyMap())
    }
    var unavailable by remember(update.revision, todayStart) {
        mutableStateOf<Set<ActivityTickerPeriod>>(emptySet())
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(ACTIVITY_TICKER_PERIOD_DURATION_MS)
            selected = ActivityTickerPeriod.values()[(selected.ordinal + 1) %
                ActivityTickerPeriod.values().size]
        }
    }
    LaunchedEffect(selected, update.revision, todayStart) {
        if (selected in totals || selected in unavailable) return@LaunchedEffect
        val target = selected
        try {
            val summary = withContext(Dispatchers.IO) {
                val range = activityTickerRange(target, System.currentTimeMillis())
                store.activitySnapshotBetween(range.startEpochMs, range.endEpochMsExclusive,
                    performMaintenance = false).summary
            }
            totals = totals + (target to summary)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Log.w("PesaTrackActivity", "Period summary unavailable")
            unavailable = unavailable + target
        }
    }

    LaunchedEffect(selected, totals[selected]) {
        if (totals[selected] == null) return@LaunchedEffect
        metricScroll.scrollTo(0)
        metricOpacity.snapTo(1f)
        snapshotFlow { metricScroll.maxValue }.first { it != Int.MAX_VALUE }
        while (true) {
            delay(1_000L)
            metricScroll.animateScrollTo(metricScroll.maxValue,
                tween(ACTIVITY_TICKER_SCROLL_DURATION_MS, easing = LinearEasing))
            delay(4_000L)
            metricOpacity.animateTo(0f, tween(800))
            metricScroll.scrollTo(0)
            metricOpacity.animateTo(1f, tween(800))
            delay(2_000L)
        }
    }

    ActivityTickerStrip(selected, totals[selected], metricScroll, metricOpacity)
}

@Composable
private fun ActivityTickerStrip(
    period: ActivityTickerPeriod,
    summary: ActivitySummary?,
    scroll: ScrollState,
    opacity: Animatable<Float, AnimationVector1D>
) {
    val p = homeColors()
    val isDark = p.canvas == PremiumVisualTokens.darkCanvas
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)) {
        Surface(color = p.white,
            shape = RoundedCornerShape(PesaDesignTokens.iconRadius)) {
            Text(period.label, color = p.muted, fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
        }
        Box(Modifier.width(1.dp).height(24.dp)
            .background(p.outline.copy(alpha = PesaDesignTokens.faintDividerAlpha)))
        BoxWithConstraints(Modifier.weight(1f)) {
            val netRunway = maxWidth * .42f
            Row(Modifier.horizontalScroll(scroll)
                .graphicsLayer { alpha = opacity.value },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                ActivityTickerMetric("Money in", summary?.incomeMinor,
                    if (isDark) PremiumVisualTokens.darkMoneyIn else PremiumVisualTokens.moneyIn)
                ActivityTickerMetric("Money out", summary?.let(::activityTickerMoneyOut),
                    if (isDark) PremiumVisualTokens.darkMoneyOut else PremiumVisualTokens.moneyOut)
                ActivityTickerMetric("Net", summary?.netMinor, p.ink)
                // Leave a little runway so Net rests near the centre, not clipped at the edge.
                Spacer(Modifier.width(netRunway))
            }
        }
    }
}

@Composable
private fun ActivityTickerMetric(label: String, minor: Long?, color: Color) {
    val p = homeColors()
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = p.muted, fontSize = 11.sp)
        Text(minor?.let(::activityKesLabel) ?: "—", color = color,
            fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}
