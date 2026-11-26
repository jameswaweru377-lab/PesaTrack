package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens

internal enum class ActivityAttentionKind { UNRESOLVED_CREDITS, NEEDS_REVIEW }

/** No card is shown when both sources of attention have been resolved. */
internal fun activityAttentionKinds(unresolvedMinor: Long, reviewCount: Int) = buildList {
    if (unresolvedMinor > 0L) add(ActivityAttentionKind.UNRESOLVED_CREDITS)
    if (reviewCount > 0) add(ActivityAttentionKind.NEEDS_REVIEW)
}

/** Swipe between local attention notices; never promote unresolved cash to income. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BrownActivityAttentionNotices(
    unresolvedMinor: Long,
    reviewCount: Int,
    onUnresolved: () -> Unit,
    onReview: () -> Unit
) {
    val notices = activityAttentionKinds(unresolvedMinor, reviewCount)
    if (notices.isEmpty()) return
    val p = homeColors()
    val pager = rememberPagerState(pageCount = { notices.size })
    HorizontalPager(
        state = pager,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) { page ->
        val unresolved = notices[page] == ActivityAttentionKind.UNRESOLVED_CREDITS
        Surface(
            onClick = if (unresolved) onUnresolved else onReview,
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = PesaDesignTokens.minimumTouchTarget),
            shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
            color = p.cream
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.Insights, null,
                    modifier = Modifier.padding(4.dp), tint = p.coffee)
                androidx.compose.foundation.layout.Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Financial insights", color = p.ink,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (unresolved) "Unresolved credits · ${activityKesLabel(unresolvedMinor)}"
                        else "$reviewCount ${if (reviewCount == 1) "activity needs" else "activities need"} review",
                        color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (unresolved) "Verify where this period's received money came from."
                        else "Check unclear transactions and categories.",
                        color = p.muted, fontSize = 12.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Rounded.ChevronRight,
                    if (unresolved) "Review unresolved credits" else "Review activity",
                    tint = p.coffee)
            }
        }
    }
}
