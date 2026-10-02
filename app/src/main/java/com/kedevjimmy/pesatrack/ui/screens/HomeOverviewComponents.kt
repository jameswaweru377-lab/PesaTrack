package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SmallStatusPill
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class HomeSpendingItem(
    val label: String,
    val amountMinor: Long,
    val color: Color
)

@Composable
fun HomeTodayCard(
    spentTodayMinor: Long,
    safeTodayMinor: Long?,
    remainingCommitmentsMinor: Long,
    remainingCommitmentCount: Int,
    activeCycle: Boolean,
    onProtectMoney: () -> Unit
) {
    PesaCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                IconBubble(
                    Icons.Rounded.Today,
                    size = 42.dp,
                    background =
                        pesaToneContainer("blue"),
                    tint =
                        pesaToneContent("blue")
                )
                Column(
                    modifier =
                        Modifier.padding(
                            start = 11.dp
                        )
                ) {
                    Text(
                        "Today",
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )
                    Text(
                        if (activeCycle) {
                            "A quick daily pace check."
                        } else {
                            "Daily spending from the current month."
                        },
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                HomeMetricTile(
                    label = "Spent today",
                    value =
                        formatHomeMoney(
                            spentTodayMinor
                        ),
                    helper =
                        "Trusted spending",
                    modifier =
                        Modifier.weight(1f)
                )
                HomeMetricTile(
                    label = "Safe daily pace",
                    value =
                        safeTodayMinor
                            ?.let(
                                ::formatHomeMoney
                            )
                            ?: "—",
                    helper =
                        if (activeCycle) {
                            "Based on money available"
                        } else {
                            "Starts with a pay cycle"
                        },
                    modifier =
                        Modifier.weight(1f)
                )
            }

            if (
                activeCycle &&
                (
                    remainingCommitmentsMinor >
                        0L ||
                        remainingCommitmentCount >
                        0
                )
            ) {
                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(16.dp),
                    color =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                            .copy(alpha = .52f)
                ) {
                    Row(
                        modifier =
                            Modifier.padding(
                                horizontal = 13.dp,
                                vertical = 11.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Shield,
                            contentDescription = null,
                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )
                        Column(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(
                                        start = 10.dp
                                    )
                        ) {
                            Text(
                                "Still protected",
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelLarge
                            )
                            Text(
                                remainingCommitmentCount
                                    .toString() +
                                    " commitment" +
                                    if (
                                        remainingCommitmentCount ==
                                        1
                                    ) {
                                        ""
                                    } else {
                                        "s"
                                    },
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }
                        Text(
                            formatHomeMoney(
                                remainingCommitmentsMinor
                            ),
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }

            if (activeCycle) {
                OutlinedButton(
                    onClick = onProtectMoney,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text("Protect extra money")
                }
            }
        }
    }
}

@Composable
private fun HomeMetricTile(
    label: String,
    value: String,
    helper: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color =
            MaterialTheme
                .colorScheme
                .surfaceVariant
                .copy(alpha = .5f)
    ) {
        Column(
            modifier =
                Modifier.padding(12.dp)
        ) {
            Text(
                label,
                style =
                    MaterialTheme
                        .typography
                        .labelMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
            Text(
                value,
                modifier =
                    Modifier.padding(top = 3.dp),
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight =
                    FontWeight.Bold
            )
            Text(
                helper,
                modifier =
                    Modifier.padding(top = 2.dp),
                style =
                    MaterialTheme
                        .typography
                        .labelSmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HomeWhereMoneyWentCard(
    items: List<HomeSpendingItem>,
    totalMinor: Long,
    onBudgetClick: () -> Unit
) {
    PesaCard {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        "Where money went",
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )
                    Text(
                        formatHomeMoney(
                            totalMinor
                        ) +
                            " this month",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
                Text(
                    "Budget",
                    modifier =
                        Modifier.clickable(
                            onClick =
                                onBudgetClick
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }

            if (items.isEmpty()) {
                Text(
                    "No trusted spending categories yet.",
                    modifier =
                        Modifier.padding(top = 14.dp),
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            } else {
                val denominator =
                    items.sumOf {
                        it.amountMinor
                    }.coerceAtLeast(1L)

                items.take(3)
                    .forEachIndexed {
                            index,
                            item ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier =
                                    Modifier.padding(
                                        vertical = 9.dp
                                    )
                            )
                        }
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top =
                                            if (
                                                index ==
                                                0
                                            ) {
                                                12.dp
                                            } else {
                                                0.dp
                                            }
                                    ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier =
                                    Modifier.size(10.dp),
                                shape =
                                    CircleShape,
                                color = item.color
                            ) {}
                            Text(
                                item.label,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(
                                            start = 9.dp
                                        ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium,
                                maxLines = 1,
                                overflow =
                                    TextOverflow.Ellipsis
                            )
                            Text(
                                formatHomeMoney(
                                    item.amountMinor
                                ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelLarge
                            )
                            Text(
                                (
                                    item.amountMinor *
                                        100L /
                                        denominator
                                ).toString() +
                                    "%",
                                modifier =
                                    Modifier.padding(
                                        start = 8.dp
                                    ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelSmall,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }
            }
        }
    }
}

@Composable
fun HomeMoneyPictureCard(
    walletLabel: String,
    knownBalanceMinor: Long?,
    balanceObservedAtEpochMs: Long?,
    accountCount: Int,
    debtOutstandingMinor: Long,
    debtAccountCount: Int,
    onAccountsClick: () -> Unit,
    onDebtsClick: () -> Unit
) {
    PesaCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Money picture",
                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )
            Text(
                "Known balances stay separate from debt. Missing balances are never guessed.",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            HomePictureRow(
                icon =
                    Icons.Rounded
                        .AccountBalanceWallet,
                title = walletLabel,
                subtitle =
                    balanceFreshnessLabel(
                        balanceObservedAtEpochMs
                    ),
                value =
                    knownBalanceMinor
                        ?.let(::formatHomeMoney)
                        ?: "Unknown",
                tone = "green",
                onClick = onAccountsClick
            )

            HomePictureRow(
                icon =
                    Icons.Rounded.Paid,
                title = "Accounts",
                subtitle =
                    accountCount.toString() +
                        " confirmed account" +
                        if (accountCount == 1) {
                            ""
                        } else {
                            "s"
                        },
                value = "View",
                tone = "blue",
                onClick = onAccountsClick
            )

            HomePictureRow(
                icon =
                    Icons.Rounded.CreditCard,
                title = "Debt",
                subtitle =
                    debtAccountCount.toString() +
                        " active debt account" +
                        if (
                            debtAccountCount ==
                            1
                        ) {
                            ""
                        } else {
                            "s"
                        },
                value =
                    if (
                        debtOutstandingMinor >
                        0L
                    ) {
                        formatHomeMoney(
                            debtOutstandingMinor
                        )
                    } else {
                        "None"
                    },
                tone = "amber",
                onClick = onDebtsClick
            )
        }
    }
}

@Composable
private fun HomePictureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    value: String,
    tone: String,
    onClick: () -> Unit
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                ),
        shape =
            RoundedCornerShape(17.dp),
        color =
            MaterialTheme
                .colorScheme
                .surfaceVariant
                .copy(alpha = .5f)
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = 13.dp,
                    vertical = 11.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            IconBubble(
                icon,
                size = 38.dp,
                background =
                    pesaToneContainer(tone),
                tint =
                    pesaToneContent(tone)
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(
                            start = 10.dp
                        )
            ) {
                Text(
                    title,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )
                Text(
                    subtitle,
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }
            Text(
                value,
                style =
                    MaterialTheme
                        .typography
                        .titleSmall,
                fontWeight =
                    FontWeight.SemiBold,
                maxLines = 1
            )
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(start = 5.dp)
                        .size(18.dp),
                tint =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
fun HomeRecentActivityCard(
    transactions: List<StoredTransaction>,
    categoryLabel: (String) -> String,
    onTransactionClick: (Long) -> Unit,
    onSeeAll: () -> Unit
) {
    PesaCard {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    "Recent activity",
                    modifier =
                        Modifier.weight(1f),
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )
                Text(
                    "See all",
                    modifier =
                        Modifier.clickable(
                            onClick = onSeeAll
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }

            if (transactions.isEmpty()) {
                Text(
                    "No canonical transactions yet.",
                    modifier =
                        Modifier.padding(top = 12.dp),
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            } else {
                transactions.take(5)
                    .forEachIndexed {
                            index,
                            item ->
                        if (index > 0) {
                            HorizontalDivider()
                        }
                        HomeRecentActivityRow(
                            item = item,
                            categoryLabel =
                                categoryLabel(
                                    item.category
                                ),
                            onClick = {
                                onTransactionClick(
                                    item.id
                                )
                            }
                        )
                    }
            }
        }
    }
}

@Composable
private fun HomeRecentActivityRow(
    item: StoredTransaction,
    categoryLabel: String,
    onClick: () -> Unit
) {
    val direction =
        runCatching {
            MoneyDirection.valueOf(
                item.direction
            )
        }.getOrDefault(
            MoneyDirection.NEUTRAL
        )
    val tone =
        when (direction) {
            MoneyDirection.INCOME ->
                "green"
            MoneyDirection.EXPENSE ->
                "amber"
            MoneyDirection.TRANSFER ->
                "blue"
            else ->
                "violet"
        }
    val icon =
        when (direction) {
            MoneyDirection.INCOME ->
                Icons.Rounded.TrendingUp
            MoneyDirection.EXPENSE ->
                Icons.Rounded.TrendingDown
            MoneyDirection.TRANSFER ->
                Icons.Rounded.SwapHoriz
            else ->
                Icons.Rounded.Paid
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                )
                .padding(
                    vertical = 11.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        IconBubble(
            icon = icon,
            size = 38.dp,
            background =
                pesaToneContainer(tone),
            tint =
                pesaToneContent(tone)
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(
                        start = 10.dp
                    )
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    item.counterparty
                        ?: categoryLabel,
                    modifier =
                        Modifier.weight(1f),
                    style =
                        MaterialTheme
                            .typography
                            .titleSmall,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )
                if (
                    item.reviewState ==
                    ReviewState
                        .NEEDS_REVIEW
                        .name
                ) {
                    SmallStatusPill(
                        "Uncertain",
                        "amber"
                    )
                }
            }
            Text(
                categoryLabel +
                    " • " +
                    formatActivityTime(
                        item.occurredAtEpochMs
                    ),
                style =
                    MaterialTheme
                        .typography
                        .labelSmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }
        Text(
            signedHomeMoney(
                item.amountMinor +
                    if (
                        direction ==
                        MoneyDirection.EXPENSE
                    ) {
                        item.feeMinor
                    } else {
                        0L
                    },
                direction
            ),
            modifier =
                Modifier.padding(
                    start = 8.dp
                ),
            style =
                MaterialTheme
                    .typography
                    .titleSmall,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}

private fun balanceFreshnessLabel(
    observedAtEpochMs: Long?
): String {
    if (observedAtEpochMs == null) {
        return "No balance observation yet"
    }

    val ageMs =
        (
            System.currentTimeMillis() -
                observedAtEpochMs
        ).coerceAtLeast(0L)
    val hours =
        TimeUnit.MILLISECONDS
            .toHours(ageMs)

    return when {
        hours < 1L ->
            "Updated within the last hour"

        hours < 24L ->
            "Updated " + hours + "h ago"

        else -> {
            val days =
                TimeUnit.MILLISECONDS
                    .toDays(ageMs)
            "Stale • updated " +
                days +
                "d ago"
        }
    }
}

private fun signedHomeMoney(
    minor: Long,
    direction: MoneyDirection
): String =
    when (direction) {
        MoneyDirection.INCOME ->
            "+ " +
                formatHomeMoney(minor)

        MoneyDirection.EXPENSE ->
            "- " +
                formatHomeMoney(minor)

        else ->
            formatHomeMoney(minor)
    }

private fun formatHomeMoney(
    minor: Long
): String {
    val formatter =
        NumberFormat
            .getNumberInstance(Locale.US)
            .apply {
                maximumFractionDigits = 0
            }

    return if (minor < 0L) {
        "-KES " +
            formatter.format(
                -minor / 100.0
            )
    } else {
        "KES " +
            formatter.format(
                minor / 100.0
            )
    }
}

private fun formatActivityTime(
    epochMs: Long
): String =
    SimpleDateFormat(
        "d MMM, HH:mm",
        Locale.getDefault()
    ).format(
        Date(epochMs)
    )
