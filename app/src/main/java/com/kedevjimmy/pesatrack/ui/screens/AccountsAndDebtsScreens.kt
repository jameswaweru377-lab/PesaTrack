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
import androidx.compose.material3.TextButton
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
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney
import com.kedevjimmy.pesatrack.domain.model.checkedMoneySum
import com.kedevjimmy.pesatrack.domain.model.checkedMoneySumOf
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import com.kedevjimmy.pesatrack.domain.rules.AccountIdentityPresentation
import com.kedevjimmy.pesatrack.domain.rules.TrackedAccountGroup
import com.kedevjimmy.pesatrack.ui.components.ConceptEmeraldHero
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.premiumCategoryColor
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import androidx.compose.material3.LinearProgressIndicator

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
    onDebtsClick: (() -> Unit)? = null,
    onAccountClick: ((String) -> Unit)? = null,
    onManageInstitutions: (() -> Unit)? = null
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

    var accountsLoading by remember { mutableStateOf(true) }
    var accountsLoaded by remember { mutableStateOf(false) }
    var accountsError by remember { mutableStateOf(false) }
    var accountsRetry by remember { mutableStateOf(0) }
    LaunchedEffect(revision, accountsRetry) {
        accountsLoading = true
        accountsError = false
        try {
        val loadedPresentations =
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
                val referenceEpochMs =
                    System.currentTimeMillis()
                val allSnapshots =
                    balanceStore
                        .currentAccountSnapshots(
                            accounts =
                                groups.flatMap {
                                    it.accounts
                                },
                            nowEpochMs =
                                referenceEpochMs
                        )
                        .associateBy {
                            it.accountId
                        }
                groups.map { group ->
                    accountBalancePresentation(
                        group = group,
                        snapshots =
                            group.accounts
                                .mapNotNull {
                                    account ->
                                    allSnapshots[
                                        account.id
                                    ]
                                }
                    )
                }
            }
        val loadedDebts =
            withContext(Dispatchers.IO) {
                debtStore.accounts()
                    .filter {
                        !it.isSettled && (it.outstandingMinor > 0L ||
                            it.balanceQuality == DebtBalanceQuality.UNKNOWN)
                    }
                    .sortedByDescending {
                        it.outstandingMinor
                    }
            }
        presentations = loadedPresentations
        activeDebts = loadedDebts
        accountsLoaded = true
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (failure: Exception) {
            android.util.Log.e("PesaTrackAccounts", "Accounts could not load", failure)
            accountsError = true
        } finally { accountsLoading = false }
    }
    if (!accountsLoaded) {
        FinancialReadStatus(scaffoldPadding, "Accounts", accountsLoading, accountsError) { accountsRetry++ }
        return
    }

    val knownTotalMinor = CheckedMoney.orNull {
        presentations
            .mapNotNull {
                it.balanceMinor
            }
            .checkedMoneySum()
    }
    val knownBalanceCount =
        presentations.count {
            it.balanceMinor != null
        }
    val debtOutstandingMinor = if (activeDebts.any { it.balanceQuality == DebtBalanceQuality.UNKNOWN }) null
        else CheckedMoney.orNull { activeDebts.checkedMoneySumOf {
            it.outstandingMinor
        } }

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
        if (accountsLoading || accountsError) item { FinancialReadBanner("Accounts", accountsLoading, accountsError) { accountsRetry++ } }
        item {
            DetailPageHeader(
                title = "Your accounts",
                subtitle =
                    "Matched from your messages and confirmed account evidence.",
                onBack = onBack
            )
        }

        item {
            ConceptEmeraldHero(
                title = "Known balances",
                amount =
                    if (knownTotalMinor == null) {
                        "Balance unavailable"
                    } else if (knownBalanceCount > 0) {
                        formatDetailMoney(knownTotalMinor)
                    } else {
                        "Waiting for balances"
                    },
                subtitle =
                    when {
                        presentations.isEmpty() ->
                            "Accounts appear here after PesaTrack can match them with evidence."
                        knownBalanceCount == 0 ->
                            presentations.size.toString() +
                                " confirmed accounts • no balance observed yet"
                        else ->
                            knownBalanceCount.toString() +
                                " of " +
                                presentations.size +
                                " confirmed accounts currently have a known balance"
                    },
                artwork = true,
                footer = "Missing balances are never guessed."
            )
        }

        if (onManageInstitutions != null) {
            item {
                PesaCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClick =
                                    onManageInstitutions
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
                                    .AccountBalance,
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
                                "My financial institutions",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium
                            )
                            Text(
                                "Confirm which banks, wallets and savings accounts are actually yours.",
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
                        Icon(
                            Icons.Rounded
                                .ChevronRight,
                            contentDescription =
                                null,
                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
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
                                debtOutstandingMinor != null && debtOutstandingMinor >
                                0L
                            ) {
                                formatDetailMoney(
                                    debtOutstandingMinor
                                )
                            } else if (debtOutstandingMinor == null) {
                                "Balance pending"
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
                    tone = index,
                    onClick =
                        onAccountClick?.let { callback ->
                            {
                                presentations[index]
                                    .group.accounts
                                    .firstOrNull()
                                    ?.id
                                    ?.let(callback)
                            }
                        }
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

    var settledDebts by remember { mutableStateOf<List<DebtAccountSnapshot>>(emptyList()) }
    var showSettled by remember { mutableStateOf(false) }
    var activeDebts:
        List<DebtAccountSnapshot> by
        remember {
            mutableStateOf(emptyList())
        }

    var debtsLoading by remember { mutableStateOf(true) }
    var debtsLoaded by remember { mutableStateOf(false) }
    var debtsError by remember { mutableStateOf(false) }
    var debtsRetry by remember { mutableStateOf(0) }
    LaunchedEffect(revision, debtsRetry) {
        debtsLoading = true
        debtsError = false
        try {
        val loaded =
            withContext(Dispatchers.IO) {
                debtStore.accounts()
            }
        val nextActive = loaded
                    .filter {
                        !it.isSettled && (it.outstandingMinor > 0L ||
                            it.hasUnresolvedDrawdown || it.lastRepaymentAtEpochMs != null ||
                            it.balanceQuality == DebtBalanceQuality.UNKNOWN)
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
        settledDebts = loaded.filter { it.isSettled }
        activeDebts = nextActive
        debtsLoaded = true
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (failure: Exception) {
            android.util.Log.e("PesaTrackDebts", "Debt could not load", failure)
            debtsError = true
        } finally { debtsLoading = false }
    }
    if (!debtsLoaded) {
        FinancialReadStatus(scaffoldPadding, "Debt", debtsLoading, debtsError) { debtsRetry++ }
        return
    }

    val unknownBalanceCount =
        activeDebts.count(
            ::hasUnknownDebtBalance
        )
    val totalDebtMinor = CheckedMoney.orNull {
        activeDebts
            .filterNot(
                ::hasUnknownDebtBalance
            )
            .checkedMoneySumOf {
                it.outstandingMinor
            }
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
        if (debtsLoading || debtsError) item { FinancialReadBanner("Debt", debtsLoading, debtsError) { debtsRetry++ } }
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
                        if (totalDebtMinor == null) {
                            "Balance unavailable"
                        } else if (
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
        if (settledDebts.isNotEmpty()) {
            item { TextButton(onClick = { showSettled = !showSettled }) {
                Text(if (showSettled) "Hide settled debts" else "Settled debts (${settledDebts.size})")
            } }
            if (showSettled) items(settledDebts.size) { index ->
                DebtAccountCard(settledDebts[index], index)
            }
        }

    }
}

@Composable
internal fun DetailPageHeader(
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
    tone: Int,
    onClick: (() -> Unit)? = null
) {
    val group =
        presentation.group
    val accent =
        premiumCategoryColor(tone)

    PesaCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                )
    ) {
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
                            "Outstanding balance unknown • more loan history needed"
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
                if (debt.lastRepaymentMinor != null && debt.lastRepaymentAtEpochMs != null) {
                    Text("Last repayment ${formatDetailMoney(debt.lastRepaymentMinor)} • ${formatDetailDateTime(debt.lastRepaymentAtEpochMs)}",
                        style = MaterialTheme.typography.bodySmall)
                }
                debt.observedAtEpochMs
                    ?.let {
                        Text(
                            "Last observed " +
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
    debt.balanceQuality == DebtBalanceQuality.UNKNOWN ||
    (debt.hasUnresolvedDrawdown &&
        debt.outstandingMinor == 0L &&
        debt.balanceQuality ==
            DebtBalanceQuality.ESTIMATED)

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
                CheckedMoney.orNull { knownComponents.checkedMoneySumOf {
                    it.balanceMinor ?: 0L
                } }
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
            totalBalance == null -> PaydayBalanceQuality.UNKNOWN
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
    minor: Long?
): String {
    if (minor == null) return "Balance unavailable"
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

@Composable
private fun FinancialReadStatus(padding: PaddingValues, label: String, loading: Boolean,
    error: Boolean, retry: () -> Unit) {
    Column(Modifier.padding(padding).padding(16.dp)) {
        FinancialReadBanner(label, loading, error, retry)
    }
}

@Composable
private fun FinancialReadBanner(label: String, loading: Boolean, error: Boolean, retry: () -> Unit) {
    if (loading) {
        Text("Loading ${label.lowercase()}…")
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    if (error) {
        Text("$label could not load")
        Text("Your saved data is unchanged. Any amounts below are from the last successful load.")
        TextButton(onClick = retry) { Text("Try again") }
    }
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

