package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.DebtAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.DebtBalanceQuality
import com.kedevjimmy.pesatrack.data.local.DebtStore
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.PaydayAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality
import com.kedevjimmy.pesatrack.data.local.PaydaySnapshotStore
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import com.kedevjimmy.pesatrack.domain.rules.AccountIdentityPresentation
import com.kedevjimmy.pesatrack.domain.rules.TrackedAccountGroup
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.premiumCategoryColor
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AccountComponentPresentation(
    val label: String,
    val balanceMinor: Long?,
    val observedAtEpochMs: Long?,
    val quality: PaydayBalanceQuality
)

private data class AccountBalancePresentation(
    val group: TrackedAccountGroup,
    val balanceMinor: Long?,
    val observedAtEpochMs: Long?,
    val quality: PaydayBalanceQuality?,
    val components:
        List<AccountComponentPresentation>
)

@Composable
fun AccountsScreen(
    scaffoldPadding: PaddingValues,
    onBack: (() -> Unit)? = null,
    onDebtsClick: (() -> Unit)? = null
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
    val balanceStore =
        remember {
            PaydaySnapshotStore.get(context)
        }
    val debtStore =
        remember {
            DebtStore.get(context)
        }
    val ledgerUpdateState by
        LedgerUpdateBus.state.collectAsState()

    val revision =
        ledgerUpdateState.revision

    var presentations:
        List<AccountBalancePresentation> by
        remember {
            mutableStateOf(emptyList())
        }
    var activeDebts:
        List<DebtAccountSnapshot> by
        remember {
            mutableStateOf(emptyList())
        }

    LaunchedEffect(revision) {
        presentations =
            withContext(Dispatchers.IO) {
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
                            AccountDisplayPolicy
                                .isConfirmedOwned(
                                    account = it,
                                    confirmedAccountIds =
                                        confirmedAccountIds
                                )
                        }
                    }
                groups.map { group ->
                    accountBalancePresentation(
                        group = group,
                        snapshots =
                            group.accounts.map {
                                account ->
                                balanceStore
                                    .currentAccountSnapshot(
                                        account
                                    )
                            }
                    )
                }
            }
    }

    LaunchedEffect(revision) {
        activeDebts =
            withContext(Dispatchers.IO) {
                debtStore.accounts()
                    .filter {
                        it.outstandingMinor >
                            0L
                    }
                    .sortedByDescending {
                        it.outstandingMinor
                    }
            }
    }

    val knownTotalMinor =
        presentations
            .mapNotNull {
                it.balanceMinor
            }
            .sum()
    val debtOutstandingMinor =
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
                title = "Accounts",
                subtitle =
                    "Wallets, banks, savings and debt PesaTrack can support with evidence.",
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

        if (onDebtsClick != null) {
            item {
                PesaCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClick =
                                    onDebtsClick
                            )
                ) {
                    Row(
                        modifier =
                            Modifier.padding(
                                15.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        IconBubble(
                            icon =
                                Icons.Rounded
                                    .CreditCard,
                            size = 42.dp
                        )
                        Column(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(
                                        start = 11.dp
                                    )
                        ) {
                            Text(
                                "Debt",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium
                            )
                            Text(
                                if (
                                    activeDebts
                                        .isEmpty()
                                ) {
                                    "No active tracked debt"
                                } else {
                                    activeDebts
                                        .size
                                        .toString() +
                                        " active account" +
                                        if (
                                            activeDebts
                                                .size ==
                                            1
                                        ) {
                                            ""
                                        } else {
                                            "s"
                                        }
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
                            if (
                                debtOutstandingMinor >
                                0L
                            ) {
                                formatDetailMoney(
                                    debtOutstandingMinor
                                )
                            } else {
                                "None"
                            },
                            style =
                                MaterialTheme
                                    .typography
                                    .titleSmall,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                        Icon(
                            Icons.Rounded
                                .ChevronRight,
                            contentDescription =
                                null,
                            modifier =
                                Modifier.padding(
                                    start = 5.dp
                                ),
                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
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

    var activeDebts:
        List<DebtAccountSnapshot> by
        remember {
            mutableStateOf(emptyList())
        }

    LaunchedEffect(revision) {
        activeDebts =
            withContext(Dispatchers.IO) {
                debtStore.accounts()
                    .filter {
                        it.outstandingMinor > 0L ||
                            it.hasUnresolvedDrawdown
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
    }

    val unknownBalanceCount =
        activeDebts.count(
            ::hasUnknownDebtBalance
        )
    val totalDebtMinor =
        activeDebts
            .filterNot(
                ::hasUnknownDebtBalance
            )
            .sumOf {
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
                        if (
                            unknownBalanceCount > 0
                        ) {
                            "Known outstanding debt"
                        } else {
                            "Total outstanding debt"
                        },
                        style =
                            MaterialTheme.typography
                                .labelLarge,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                    Text(
                        if (
                            activeDebts.isNotEmpty() &&
                            unknownBalanceCount ==
                                activeDebts.size
                        ) {
                            "Balance pending"
                        } else {
                            formatDetailMoney(
                                totalDebtMinor
                            )
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
                        if (activeDebts.isEmpty()) {
                            "No outstanding debt is currently recorded."
                        } else {
                            buildString {
                                append(
                                    activeDebts.size
                                )
                                append(
                                    " active debt account"
                                )
                                if (
                                    activeDebts.size != 1
                                ) {
                                    append("s")
                                }
                                if (
                                    unknownBalanceCount > 0
                                ) {
                                    append(
                                        " • "
                                    )
                                    append(
                                        unknownBalanceCount
                                    )
                                    append(
                                        " awaiting an observed balance"
                                    )
                                }
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
                            "A loan appears when PesaTrack has a positive outstanding balance or a confirmed drawdown whose exact balance is still waiting for evidence.",
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
    onBack: (() -> Unit)?
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start =
                        if (onBack != null) {
                            4.dp
                        } else {
                            0.dp
                        }
                )
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
                    AccountIdentityPresentation
                        .title(group),
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
                            group.displayName ==
                                "M-Shwari" &&
                            presentation
                                .components
                                .size > 1
                        ) {
                            append(
                                " • regular + lock savings"
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
                        ?.let { epoch ->
                            when (
                                presentation.quality
                            ) {
                                PaydayBalanceQuality.OBSERVED ->
                                    "Last observed " +
                                        formatDetailDateTime(
                                            epoch
                                        )
                                PaydayBalanceQuality.RECONSTRUCTED ->
                                    "Updated from account activity • base " +
                                        formatDetailDateTime(
                                            epoch
                                        )
                                PaydayBalanceQuality.USER_ENTERED ->
                                    "Entered " +
                                        formatDetailDateTime(
                                            epoch
                                        )
                                else ->
                                    "Balance evidence " +
                                        formatDetailDateTime(
                                            epoch
                                        )
                            }
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

        if (
            group.displayName ==
                "M-Shwari" &&
            presentation.components.size > 1
        ) {
            Column(
                modifier =
                    Modifier.padding(
                        start = 71.dp,
                        end = 15.dp,
                        bottom = 14.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                presentation.components
                    .forEach { component ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Text(
                                component.label,
                                modifier =
                                    Modifier.weight(1f),
                                style =
                                    MaterialTheme.typography
                                        .bodySmall,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                            )
                            Column(
                                horizontalAlignment =
                                    Alignment.End
                            ) {
                                Text(
                                    component.balanceMinor
                                        ?.let(
                                            ::formatDetailMoney
                                        )
                                        ?: "—",
                                    style =
                                        MaterialTheme.typography
                                            .bodySmall,
                                    fontWeight =
                                        FontWeight.SemiBold
                                )
                                if (
                                    component.quality ==
                                        PaydayBalanceQuality
                                            .RECONSTRUCTED
                                ) {
                                    Text(
                                        "from activity",
                                        style =
                                            MaterialTheme.typography
                                                .labelSmall,
                                        color =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
            }
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
                    when {
                        hasUnknownDebtBalance(
                            debt
                        ) ->
                            "Loan detected • exact outstanding balance not observed yet"
                        debt.balanceQuality ==
                            DebtBalanceQuality.OBSERVED ->
                            "Observed outstanding balance"
                        else ->
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
                if (
                    hasUnknownDebtBalance(
                        debt
                    )
                ) {
                    "—"
                } else {
                    formatDetailMoney(
                        debt.outstandingMinor
                    )
                },
                style =
                    MaterialTheme.typography
                        .titleMedium,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

private fun hasUnknownDebtBalance(
    debt: DebtAccountSnapshot
): Boolean =
    debt.hasUnresolvedDrawdown &&
        debt.outstandingMinor == 0L &&
        debt.balanceQuality ==
            DebtBalanceQuality.ESTIMATED

private fun accountBalancePresentation(
    group: TrackedAccountGroup,
    snapshots:
        List<PaydayAccountSnapshot>
): AccountBalancePresentation {
    val byAccountId =
        snapshots.associateBy {
            it.accountId
        }

    val components =
        group.accounts
            .map { account ->
                val snapshot =
                    byAccountId[
                        account.id
                    ]
                AccountComponentPresentation(
                    label =
                        mshwariComponentLabel(
                            account.name
                        ),
                    balanceMinor =
                        snapshot?.balanceMinor,
                    observedAtEpochMs =
                        snapshot
                            ?.referenceEpochMs,
                    quality =
                        snapshot?.quality
                            ?: PaydayBalanceQuality
                                .UNKNOWN
                )
            }
            .distinctBy {
                it.label
            }
            .sortedBy {
                when (it.label) {
                    "M-Shwari" -> 0
                    "M-Shwari Lock Savings" -> 1
                    else -> 2
                }
            }

    val knownComponents =
        components.filter {
            it.balanceMinor != null
        }
    val allKnown =
        components.isNotEmpty() &&
            knownComponents.size ==
                components.size

    val totalBalance =
        when {
            components.size == 1 ->
                components.single()
                    .balanceMinor
            allKnown ->
                knownComponents.sumOf {
                    it.balanceMinor ?: 0L
                }
            else -> null
        }

    val newest =
        components
            .filter {
                it.observedAtEpochMs !=
                    null
            }
            .maxByOrNull {
                it.observedAtEpochMs
                    ?: Long.MIN_VALUE
            }

    val overallQuality =
        when {
            components.any {
                it.quality ==
                    PaydayBalanceQuality
                        .RECONSTRUCTED
            } ->
                PaydayBalanceQuality
                    .RECONSTRUCTED
            components.any {
                it.quality ==
                    PaydayBalanceQuality
                        .USER_ENTERED
            } ->
                PaydayBalanceQuality
                    .USER_ENTERED
            components.any {
                it.quality ==
                    PaydayBalanceQuality
                        .OBSERVED
            } ->
                PaydayBalanceQuality
                    .OBSERVED
            else -> null
        }

    return AccountBalancePresentation(
        group = group,
        balanceMinor = totalBalance,
        observedAtEpochMs =
            newest?.observedAtEpochMs,
        quality = overallQuality,
        components = components
    )
}

private fun mshwariComponentLabel(
    raw: String
): String {
    val normalized =
        raw.uppercase()
            .replace(
                Regex("""[^A-Z0-9]+"""),
                " "
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()

    return when {
        "LOCK SAVING" in
            normalized ->
            "M-Shwari Lock Savings"

        "M SHWARI" in
            normalized ->
            "M-Shwari"

        else -> raw
    }
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
