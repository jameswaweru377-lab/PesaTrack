package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens

/** Swipe between local attention notices; never promote unresolved cash to income. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BrownActivityAttentionNotices(
    unresolvedMinor: Long,
    reviewCount: Int,
    onUnresolved: () -> Unit,
    onReview: () -> Unit
) {
    val notices = buildList {
        if (unresolvedMinor > 0) add(true)
        if (reviewCount > 0) add(false)
    }
    if (notices.isEmpty()) return
    val p = homeColors()
    val pager = rememberPagerState(pageCount = { notices.size })
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth()) { page ->
            val unresolved = notices[page]
            Surface(
                onClick = if (unresolved) onUnresolved else onReview,
                modifier = Modifier.fillMaxWidth()
                    .heightIn(min = PesaDesignTokens.minimumTouchTarget),
                shape = RoundedCornerShape(PesaDesignTokens.iconRadius),
                color = p.white, border = BorderStroke(1.dp, p.outline)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (unresolved) "Unresolved credits · ${activityKesLabel(unresolvedMinor)}"
                            else "$reviewCount ${if (reviewCount == 1) "item needs" else "items need"} review",
                            color = p.ink, fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (unresolved) "Received money with an unverified origin. Tap to review this period's credits."
                            else "Review transactions and possible transfers that need your decision.",
                            color = p.muted
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight,
                        if (unresolved) "Review unresolved credits" else "Review activity",
                        tint = p.coffee)
                }
            }
        }
        if (notices.size > 1) Text(
            "${pager.currentPage + 1} of ${notices.size} · Swipe for more",
            color = p.muted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
