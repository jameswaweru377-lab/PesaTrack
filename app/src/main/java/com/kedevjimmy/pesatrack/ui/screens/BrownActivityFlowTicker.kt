package com.kedevjimmy.pesatrack.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

/** Read-only rotating financial context. Never derive a new accounting definition. */
@Composable
internal fun BrownActivityFlowTicker() {
    val p = homeColors()
    val context = LocalContext.current
    val store = remember(context) { TransactionStore.get(context) }
    val update by LedgerUpdateBus.state.collectAsState()
    var selected by remember { mutableStateOf(ActivityTickerPeriod.YEAR) }
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
            delay(8_000L)
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

    AnimatedContent(targetState = selected,
        transitionSpec = {
            ContentTransform(
                slideInHorizontally(tween(700)) { it / 3 } + fadeIn(tween(700)),
                slideOutHorizontally(tween(700)) { -it / 3 } + fadeOut(tween(700))
            )
        }, label = "Activity totals rotation") { period ->
        val summary = totals[period]
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
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                ActivityTickerMetric("Money in", summary?.incomeMinor,
                    if (isDark) PremiumVisualTokens.darkMoneyIn else PremiumVisualTokens.moneyIn)
                ActivityTickerMetric("Money out", summary?.let(::activityTickerMoneyOut),
                    if (isDark) PremiumVisualTokens.darkMoneyOut else PremiumVisualTokens.moneyOut)
                ActivityTickerMetric("Net", summary?.netMinor, p.ink)
                Spacer(Modifier.width(8.dp))
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
