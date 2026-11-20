package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.ReviewGroup
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens

/** Visually grouped rows that remain individually keyed and virtualized by the parent LazyColumn. */
@Composable
internal fun ActivityReviewGroupRow(
    group: ReviewGroup,
    isFirst: Boolean,
    isLast: Boolean,
    onSelect: (Long) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(
            top = if (isFirst) 8.dp else 0.dp,
            bottom = if (isLast) 12.dp else 0.dp
        ),
        shape = RoundedCornerShape(
            topStart = if (isFirst) PesaDesignTokens.cardRadius else 0.dp,
            topEnd = if (isFirst) PesaDesignTokens.cardRadius else 0.dp,
            bottomEnd = if (isLast) PesaDesignTokens.cardRadius else 0.dp,
            bottomStart = if (isLast) PesaDesignTokens.cardRadius else 0.dp
        ),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = PesaDesignTokens.cardContentPadding)) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .clickable { onSelect(group.representativeTransactionId) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(PesaDesignTokens.controlRadius),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Rounded.HelpOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            group.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${group.transactionCount} ${if (group.transactionCount == 1) "transaction" else "transactions"} · ${formatTime(group.latestEpochMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        formatActivityMoney(group.totalMinor),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (!isLast) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = PesaDesignTokens.faintDividerAlpha)
                    )
                }
        }
    }
}
