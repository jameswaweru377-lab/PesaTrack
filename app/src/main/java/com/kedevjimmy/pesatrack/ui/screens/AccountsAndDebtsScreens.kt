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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialProviderRegistry
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
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    if (selectedTab == 3) {
        DebtsScreen(scaffoldPadding, onBack = { selectedTab = 0 },
            header = { AccountsNavigation(selectedTab, { selectedTab = it }, onBack, onManageInstitutions) },
            onReviewRelationships = onManageInstitutions)
        return
    }
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
                        accounts, includeCash = true
                    ).filter { group ->
                        group.accounts.any {
                            AccountDisplayPolicy
                                .isConfirmedOwned(
                                    account = it,
                                    confirmedAccountIds =
                                        confirmedAccountIds, includeCash = true
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
                        it.isActive
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

    val visible = presentations.filter {
        val mobileMoney = FinancialProviderRegistry.isMobileMoneyAccount(it.group.displayName, it.group.type)
        when (selectedTab) {
            1 -> !mobileMoney && it.group.type != AccountType.CASH
            2 -> mobileMoney
            else -> true
        }
    }
    val hasMpesa = visible.any { it.group.accounts.any { account -> account.id == AccountStore.DEFAULT_MPESA_ID } }
    val shown = visible.filterNot { hasMpesa && (it.group.displayName.contains("M-Shwari", true) || it.group.displayName.contains("KCB M-Pesa", true)) }
    val knownTotalMinor = CheckedMoney.orNull {
        visible
            .mapNotNull {
                it.balanceMinor
            }
            .checkedMoneySum()
    }
    val knownBalanceCount =
        visible.count {
            it.balanceMinor != null
        }
    LazyColumn(modifier = Modifier.padding(scaffoldPadding).background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AccountsNavigation(selectedTab, { selectedTab = it }, onBack, onManageInstitutions) }
        if (accountsLoading || accountsError) item {
            FinancialReadBanner("Accounts", accountsLoading, accountsError) { accountsRetry++ }
        }
        item {
            PesaCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Last known account balances", style = MaterialTheme.typography.titleMedium)
                    Text(if (knownBalanceCount == 0) "No balance yet" else formatDetailMoney(knownTotalMinor),
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Snapshots for $knownBalanceCount of ${visible.size} accounts · not a monthly total",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { AccountBalanceCheckAction() }
        if (visible.isEmpty()) item {
            PesaCard { Column(Modifier.padding(18.dp)) {
                Text("No confirmed accounts here yet", style = MaterialTheme.typography.titleMedium)
                Text("Accounts appear after their ownership is confirmed. Missing balances are never guessed.",
                    style = MaterialTheme.typography.bodySmall)
            } }
        }
        items(shown.size, key = { shown[it].group.key }) { index ->
            AccountBalanceCard(shown[index], index, onAccountClick?.let { callback ->
                { shown[index].group.accounts.firstOrNull()?.id?.let(callback); Unit }
            })
        }
        if (selectedTab == 0 && activeDebts.isNotEmpty()) {
            item { Text("Debts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(activeDebts.size, key = { "debt:" + activeDebts[it].id }) { index ->
                DebtAccountCard(activeDebts[index], index)
            }
            item { TextButton(onClick = { selectedTab = 3 }) { Text("View all debts") } }
        }
        if (onManageInstitutions != null) item { RelationshipReviewCard(onManageInstitutions) }
    }
}

@Composable
private fun AccountsNavigation(selected: Int, onSelected: (Int) -> Unit,
    onBack: (() -> Unit)?, onManage: (() -> Unit)?) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text("Accounts", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            if (onManage != null) IconButton(onClick = onManage) {
                Icon(Icons.Rounded.Add, contentDescription = "Add or confirm accounts")
            }
        }
        ScrollableTabRow(selectedTabIndex = selected, edgePadding = 0.dp) {
            listOf("All", "Banks", "Mobile", "Debts").forEachIndexed { index, label ->
                Tab(selected = selected == index, onClick = { onSelected(index) }, text = { Text(label) })
            }
        }
    }
}

@Composable
private fun RelationshipReviewCard(onClick: () -> Unit) {
    PesaCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Rounded.AccountBalance, size = 40.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Which accounts are yours?", style = MaterialTheme.typography.titleMedium)
                Text("Add or manage your accounts",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun DebtsScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit,
    header: (@Composable () -> Unit)? = null,
    onReviewRelationships: (() -> Unit)? = null
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
                        it.isActive
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
            if (header != null) header() else DetailPageHeader(
                title = "Debts", subtitle = "Borrowing supports payments; it is not income.", onBack = onBack)

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
                        } else {
                            "Known outstanding"
                        },
                        style =
                            MaterialTheme.typography
                                .labelLarge,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                    Text(
                        if (activeDebts.isNotEmpty() && activeDebts.all { it.needsSettlementConfirmation }) {
                            "Confirmation pending"
                        } else if (
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
                            "No current debt recorded",
                            style =
                                MaterialTheme.typography
                                    .titleMedium
                        )
                        Text(
                            "Add a debt or import its money messages to track it here.",
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
        if (onReviewRelationships != null) item { RelationshipReviewCard(onReviewRelationships) }

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
                                    "Balance from " +
                                        formatDetailDateTime(
                                            epoch
                                        )
                                PaydayBalanceQuality.RECONSTRUCTED ->
                                    "Estimate from " +
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
                        ?: "Balance not available",
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
private fun DebtAccountCard(debt: DebtAccountSnapshot, tone: Int) {
    val unknown = hasUnknownDebtBalance(debt)
    val accent = premiumCategoryColor(tone)
    PesaCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Rounded.CreditCard, size = 42.dp,
                    background = accent.copy(alpha = .16f), tint = accent)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(debt.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(when {
                        unknown -> "Excluded from known total"
                        debt.isSettled -> "Settled • confirmed zero"
                        debt.needsSettlementConfirmation -> "Settlement confirmation pending"
                        debt.balanceQuality == DebtBalanceQuality.OBSERVED -> "Observed"
                        else -> "Estimated from recorded activity"
                    }, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(shape = RoundedCornerShape(12.dp), color = if (unknown)
                MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer) {
                Text(if (unknown) "Outstanding unknown" else formatDetailMoney(debt.outstandingMinor),
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    color = if (unknown) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer)
            }
            Text(when {
                unknown -> "Loan activity recorded; balance evidence needed."
                debt.needsSettlementConfirmation -> "Estimated zero is not a confirmed settlement."
                debt.isSettled -> "No outstanding borrowing recorded."
                else -> "Borrowing supports payments; it is not income."
            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            debt.observedAtEpochMs?.let {
                Text((if (debt.balanceQuality == DebtBalanceQuality.OBSERVED) "Observed " else "Balance evidence ") +
                    formatDetailDateTime(it), style = MaterialTheme.typography.labelSmall)
            }
            if (debt.lastRepaymentMinor != null && debt.lastRepaymentAtEpochMs != null) {
                HorizontalDivider()
                Text("Last repayment", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDetailMoney(debt.lastRepaymentMinor), fontWeight = FontWeight.Bold)
                    Text("Recorded • " + formatDetailDateTime(debt.lastRepaymentAtEpochMs),
                        style = MaterialTheme.typography.labelSmall)
                }
            }
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
                        com.kedevjimmy.pesatrack.domain.rules.BalancePresentationPolicy.savingsBalance(snapshot?.balanceMinor, account.type == AccountType.SAVINGS, snapshot?.quality == PaydayBalanceQuality.RECONSTRUCTED),
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

private fun formatDetailMoney(minor: Long?): String =
    minor?.let(::formatKesAmount) ?: "Balance unavailable"

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
