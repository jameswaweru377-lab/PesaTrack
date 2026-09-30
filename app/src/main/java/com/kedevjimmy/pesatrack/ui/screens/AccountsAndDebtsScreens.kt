package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.DebtAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.DebtBalanceQuality
import com.kedevjimmy.pesatrack.data.local.DebtStore
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.ObservedAccountBalance
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import com.kedevjimmy.pesatrack.domain.rules.TrackedAccountGroup
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.premiumCategoryColor
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class AccountBalancePresentation(
    val group: TrackedAccountGroup,
    val balanceMinor: Long?,
    val observedAtEpochMs: Long?,
    val observedSubaccounts: Int
)

@Composable
fun AccountsScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val accountStore =
        remember {
            AccountStore.get(context)
        }
    val understandingStore =
        remember {
            FinancialUnderstandingStore
                .get(context)
        }
    val transactionStore =
        remember {
            TransactionStore.get(context)
        }
    val ledgerUpdateState by
        LedgerUpdateBus.state.collectAsState()

    val revision =
        ledgerUpdateState.revision

    val presentations =
        remember(revision) {
            val accounts =
                accountStore.listAccounts(
                    enabledOnly = true
                )
            val confirmedAccountIds =
                understandingStore
                    .confirmedAccountIds()
            val groups =
                AccountDisplayPolicy.groups(
                    accounts
                ).filter { group ->
                    group.accounts.any {
                        it.id in
                            confirmedAccountIds ||
                            !it.identifier
                                .isNullOrBlank()
                    }
                }
            val balances =
                understandingStore
                    .latestBalances()
            val transactionSnapshot =
                transactionStore.snapshot()

            groups.map { group ->
                accountBalancePresentation(
                    group = group,
                    balances = balances,
                    fallbackMpesaBalanceMinor =
                        transactionSnapshot
                            .latestBalanceMinor,
                    fallbackMpesaObservedAt =
                        transactionSnapshot
                            .latestBalanceObservedAtEpochMs
                )
            }
        }

    val knownTotalMinor =
        presentations
            .mapNotNull {
                it.balanceMinor
            }
            .sum()

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(
                MaterialTheme.colorScheme
                    .background
            ),
        contentPadding =
            PaddingValues(
                horizontal = 16.dp,
                vertical = 14.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {
        item {
            DetailPageHeader(
                title = "Wallets & balances",
                subtitle =
                    "Only accounts PesaTrack has confirmed as yours.",
                onBack = onBack
            )
        }

        item {
            PesaCard {
                Column(
                    modifier =
                        Modifier.padding(16.dp)
                ) {
                    Text(
                        "Known balances",
                        style =
                            MaterialTheme.typography
                                .labelLarge,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                    Text(
                        if (
                            presentations.any {
                                it.balanceMinor != null
                            }
                        ) {
                            formatDetailMoney(
                                knownTotalMinor
                            )
                        } else {
                            "Waiting for balance observations"
                        },
                        modifier =
                            Modifier.padding(top = 4.dp),
                        style =
                            MaterialTheme.typography
                                .headlineMedium,
                        fontWeight =
                            FontWeight.Bold
                    )
                    Text(
                        "Balances are based on the latest account observations PesaTrack has actually seen. Missing balances are not guessed.",
                        modifier =
                            Modifier.padding(top = 5.dp),
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }

        if (presentations.isEmpty()) {
            item {
                PesaCard {
                    Column(
                        modifier =
                            Modifier.padding(18.dp)
                    ) {
                        Text(
                            "No confirmed accounts yet",
                            style =
                                MaterialTheme.typography
                                    .titleMedium
                        )
                        Text(
                            "When PesaTrack confidently identifies one of your wallets, banks or savings accounts, it will appear here.",
                            modifier =
                                Modifier.padding(top = 5.dp),
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                count = presentations.size,
                key = { index ->
                    presentations[index]
                        .group.key
                }
            ) { index ->
                AccountBalanceCard(
                    presentation =
                        presentations[index],
                    tone = index
                )
            }
        }
    }
}

@Composable
fun DebtsScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val debtStore =
        remember {
            DebtStore.get(context)
        }
    val ledgerUpdateState by
        LedgerUpdateBus.state.collectAsState()
    val revision =
        ledgerUpdateState.revision

    val activeDebts =
        remember(revision) {
            debtStore.accounts()
                .filter {
                    it.outstandingMinor > 0L
                }
                .sortedWith(
                    compareByDescending<
                        DebtAccountSnapshot
                    > {
                        it.outstandingMinor
                    }.thenBy {
                        it.name.lowercase()
                    }
                )
        }

    val totalDebtMinor =
        activeDebts.sumOf {
            it.outstandingMinor
        }

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(
                MaterialTheme.colorScheme
                    .background
            ),
        contentPadding =
            PaddingValues(
                horizontal = 16.dp,
                vertical = 14.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {
        item {
            DetailPageHeader(
                title = "Debt",
                subtitle =
                    "Outstanding borrowing only. Fully repaid or zero-balance accounts stay out of the way.",
                onBack = onBack
            )
        }

        item {
            PesaCard {
                Column(
                    modifier =
                        Modifier.padding(16.dp)
                ) {
                    Text(
                        "Total outstanding debt",
                        style =
                            MaterialTheme.typography
                                .labelLarge,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                    Text(
                        formatDetailMoney(
                            totalDebtMinor
                        ),
                        modifier =
                            Modifier.padding(top = 4.dp),
                        style =
                            MaterialTheme.typography
                                .headlineMedium,
                        fontWeight =
                            FontWeight.Bold
                    )
                    Text(
                        if (activeDebts.isEmpty()) {
                            "No outstanding debt is currently recorded."
                        } else {
                            activeDebts.size
                                .toString() +
                                " active debt account" +
                                if (
                                    activeDebts.size == 1
                                ) {
                                    ""
                                } else {
                                    "s"
                                }
                        },
                        modifier =
                            Modifier.padding(top = 4.dp),
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }

        if (activeDebts.isEmpty()) {
            item {
                PesaCard {
                    Column(
                        modifier =
                            Modifier.padding(18.dp)
                    ) {
                        Text(
                            "You're clear here",
                            style =
                                MaterialTheme.typography
                                    .titleMedium
                        )
                        Text(
                            "A loan will appear only when PesaTrack has a positive outstanding balance.",
                            modifier =
                                Modifier.padding(top = 5.dp),
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                count = activeDebts.size,
                key = { index ->
                    activeDebts[index].id
                }
            ) { index ->
                DebtAccountCard(
                    debt =
                        activeDebts[index],
                    tone = index
                )
            }
        }
    }
}

@Composable
private fun DetailPageHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack
        ) {
            Icon(
                Icons.Rounded.ArrowBack,
                contentDescription = "Back"
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        ) {
            Text(
                title,
                style =
                    MaterialTheme.typography
                        .headlineSmall,
                fontWeight =
                    FontWeight.Bold
            )
            Text(
                subtitle,
                modifier =
                    Modifier.padding(top = 2.dp),
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountBalanceCard(
    presentation:
        AccountBalancePresentation,
    tone: Int
) {
    val group =
        presentation.group
    val accent =
        premiumCategoryColor(tone)

    PesaCard {
        Row(
            modifier =
                Modifier.padding(15.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            IconBubble(
                icon =
                    when (group.type) {
                        AccountType.MOBILE_WALLET ->
                            Icons.Rounded
                                .AccountBalanceWallet
                        AccountType.BANK ->
                            Icons.Rounded
                                .AccountBalance
                        else ->
                            Icons.Rounded.Savings
                    },
                size = 44.dp,
                background =
                    accent.copy(alpha = .16f),
                tint = accent
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    group.displayName,
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.SemiBold
                )
                Text(
                    buildString {
                        append(
                            when (group.type) {
                                AccountType.MOBILE_WALLET ->
                                    "Wallet"
                                AccountType.BANK ->
                                    "Bank"
                                AccountType.SAVINGS ->
                                    "Savings"
                                else ->
                                    "Account"
                            }
                        )
                        if (
                            group.accounts.size >
                            1
                        ) {
                            append(
                                " • includes " +
                                    group.accounts
                                        .size +
                                    " linked savings balances"
                            )
                        }
                    },
                    modifier =
                        Modifier.padding(top = 2.dp),
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
                Text(
                    presentation
                        .observedAtEpochMs
                        ?.let {
                            "Last observed " +
                                formatDetailDateTime(
                                    it
                                )
                        }
                        ?: "No balance observed yet",
                    modifier =
                        Modifier.padding(top = 3.dp),
                    style =
                        MaterialTheme.typography
                            .labelSmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            Text(
                presentation.balanceMinor
                    ?.let(
                        ::formatDetailMoney
                    )
                    ?: "—",
                style =
                    MaterialTheme.typography
                        .titleMedium,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DebtAccountCard(
    debt: DebtAccountSnapshot,
    tone: Int
) {
    val accent =
        premiumCategoryColor(tone)

    PesaCard {
        Row(
            modifier =
                Modifier.padding(15.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            IconBubble(
                icon =
                    Icons.Rounded.CreditCard,
                size = 44.dp,
                background =
                    accent.copy(alpha = .16f),
                tint = accent
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    debt.name,
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.SemiBold
                )
                Text(
                    if (
                        debt.balanceQuality ==
                        DebtBalanceQuality.OBSERVED
                    ) {
                        "Observed outstanding balance"
                    } else {
                        "Estimated from recorded loan activity"
                    },
                    modifier =
                        Modifier.padding(top = 2.dp),
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
                debt.observedAtEpochMs
                    ?.let {
                        Text(
                            "Last updated " +
                                formatDetailDateTime(
                                    it
                                ),
                            modifier =
                                Modifier.padding(
                                    top = 3.dp
                                ),
                            style =
                                MaterialTheme.typography
                                    .labelSmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
            }
            Text(
                formatDetailMoney(
                    debt.outstandingMinor
                ),
                style =
                    MaterialTheme.typography
                        .titleMedium,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

private fun accountBalancePresentation(
    group: TrackedAccountGroup,
    balances:
        List<ObservedAccountBalance>,
    fallbackMpesaBalanceMinor: Long?,
    fallbackMpesaObservedAt: Long?
): AccountBalancePresentation {
    val relevant =
        balances.filter {
            it.accountId in
                group.accountIds
        }

    val latestByAccount =
        relevant
            .groupBy {
                it.accountId
            }
            .mapNotNull {
                    (_, observations) ->
                observations.maxByOrNull {
                    it.observedAtEpochMs
                }
            }

    val observedBalance =
        latestByAccount
            .takeIf {
                it.isNotEmpty()
            }
            ?.sumOf {
                it.balanceMinor
            }

    val isMpesa =
        AccountStore.DEFAULT_MPESA_ID in
            group.accountIds

    return AccountBalancePresentation(
        group = group,
        balanceMinor =
            observedBalance
                ?: if (isMpesa) {
                    fallbackMpesaBalanceMinor
                } else {
                    group.accounts
                        .mapNotNull {
                            it.openingBalanceMinor
                        }
                        .takeIf {
                            it.isNotEmpty()
                        }
                        ?.sum()
                },
        observedAtEpochMs =
            latestByAccount
                .maxOfOrNull {
                    it.observedAtEpochMs
                }
                ?: if (isMpesa) {
                    fallbackMpesaObservedAt
                } else {
                    group.accounts
                        .mapNotNull {
                            it.openingBalanceAsOfEpochMs
                        }
                        .maxOrNull()
                },
        observedSubaccounts =
            latestByAccount.size
    )
}

private fun formatDetailMoney(
    minor: Long
): String {
    val formatter =
        NumberFormat.getNumberInstance(
            Locale.US
        ).apply {
            maximumFractionDigits = 0
        }
    return "KES " +
        formatter.format(
            minor / 100.0
        )
}

private fun formatDetailDateTime(
    epochMs: Long
): String =
    SimpleDateFormat(
        "d MMM, h:mm a",
        Locale.getDefault()
    ).format(
        Date(epochMs)
    )
