package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalAtm
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot
import com.kedevjimmy.pesatrack.data.local.ActivityMonthBucket
import com.kedevjimmy.pesatrack.data.local.ActivityReviewOverview
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.DiscoveredFinancialEntity
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.PossibleDuplicateGroup
import com.kedevjimmy.pesatrack.data.local.ReviewGroup
import com.kedevjimmy.pesatrack.data.local.RelationshipReviewCandidate
import com.kedevjimmy.pesatrack.data.local.RelationshipReviewItem
import com.kedevjimmy.pesatrack.data.local.RelationshipReviewKind
import com.kedevjimmy.pesatrack.data.local.RelationshipReviewStore
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.KenyanBankDirectory
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import com.kedevjimmy.pesatrack.domain.rules.TrackedAccountGroup
import com.kedevjimmy.pesatrack.ingestion.FinancialMessageIngestionCoordinator
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnostic
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnosticCode
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnosticsStore
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialProviderRegistry
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.PesaTrackTopBar
import com.kedevjimmy.pesatrack.ui.components.SmallStatusPill
import com.kedevjimmy.pesatrack.ui.components.isPesaDarkTheme
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    scaffoldPadding: PaddingValues,
    onTransactionClick: (Long) -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val store = remember { TransactionStore.get(context) }
    val financialStore = remember {
        FinancialUnderstandingStore.get(context)
    }
    val financialCoordinator = remember {
        FinancialMessageIngestionCoordinator.get(context)
    }
    val diagnosticsStore = remember {
        FinancialIngestionDiagnosticsStore.get(context)
    }
    val accountStore = remember {
        AccountStore.get(context)
    }
    val relationshipReviewStore = remember {
        RelationshipReviewStore.get(context)
    }
    val ledgerUpdateState by LedgerUpdateBus.state.collectAsState()
    val currentMonthEpochMs = remember {
        monthStart(System.currentTimeMillis())
    }

    fun loadMonthBuckets(): List<ActivityMonthBucket> {
        val existing = store.activityMonths().toMutableList()
        if (
            existing.none {
                it.monthStartEpochMs == currentMonthEpochMs
            }
        ) {
            existing.add(
                0,
                ActivityMonthBucket(
                    monthStartEpochMs = currentMonthEpochMs,
                    transactionCount = 0
                )
            )
        }
        return existing.sortedByDescending {
            it.monthStartEpochMs
        }
    }

    var monthBuckets by remember {
        mutableStateOf(loadMonthBuckets())
    }
    var selectedMonthEpochMs by rememberSaveable {
        mutableStateOf(currentMonthEpochMs)
    }
    var expandedMonthEpochMs by rememberSaveable {
        mutableStateOf<Long?>(currentMonthEpochMs)
    }
    var snapshot by remember {
        mutableStateOf(
            store.activitySnapshot(
                nowEpochMs = selectedMonthEpochMs
            )
        )
    }
    var previousSnapshot by remember {
        mutableStateOf(
            store.activitySnapshot(
                nowEpochMs = shiftMonth(
                    selectedMonthEpochMs,
                    -1
                )
            )
        )
    }
    var globalOverview by remember {
        mutableStateOf(store.activityReviewOverview())
    }
    var relationshipReviews by remember {
        mutableStateOf(
            relationshipReviewStore.pendingReviews()
        )
    }
    var pendingFinancialEntities by remember {
        mutableStateOf(financialStore.pendingEntities())
    }
    var selectedFilter by remember {
        mutableStateOf("All")
    }
    var selectedFinancialSource by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    var financialDiagnostics by remember {
        mutableStateOf(diagnosticsStore.recent(500))
    }
    var accountsById by remember {
        mutableStateOf(
            accountStore.listAccounts(enabledOnly = false)
                .associateBy { it.id }
        )
    }
    var selectedGlobalQueue by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    var showMonthPicker by remember {
        mutableStateOf(false)
    }
    var accountReviewEntity by remember {
        mutableStateOf<DiscoveredFinancialEntity?>(
            null
        )
    }

    val reviewableFinancialEntities =
        remember(
            pendingFinancialEntities,
            financialDiagnostics
        ) {
            pendingFinancialEntities
                .filter {
                    shouldOfferOwnershipReview(
                        entity = it,
                        diagnostics =
                            financialDiagnostics
                    )
                }
        }

    fun selectMonth(epochMs: Long) {
        val normalizedMonth = monthStart(epochMs)
        selectedMonthEpochMs = normalizedMonth
        expandedMonthEpochMs = normalizedMonth
        selectedFilter = "All"
        selectedFinancialSource = null
        selectedGlobalQueue = null
        snapshot = store.activitySnapshot(
            nowEpochMs = normalizedMonth
        )
        previousSnapshot = store.activitySnapshot(
            nowEpochMs = shiftMonth(
                normalizedMonth,
                -1
            )
        )
    }

    fun refresh() {
        monthBuckets = loadMonthBuckets()
        snapshot = store.activitySnapshot(
            nowEpochMs = selectedMonthEpochMs
        )
        previousSnapshot = store.activitySnapshot(
            nowEpochMs = shiftMonth(
                selectedMonthEpochMs,
                -1
            )
        )
        globalOverview = store.activityReviewOverview()
        relationshipReviews =
            relationshipReviewStore.pendingReviews()
        pendingFinancialEntities =
            financialStore.pendingEntities()
        financialDiagnostics =
            diagnosticsStore.recent(500)
        accountsById =
            accountStore.listAccounts(
                enabledOnly = false
            ).associateBy { it.id }
    }

    LaunchedEffect(ledgerUpdateState.revision) {
        if (ledgerUpdateState.revision > 0L) {
            refresh()
        }
    }

    val selectedMonthLabel =
        monthLabel(selectedMonthEpochMs)
    val nextMonthEpochMs =
        shiftMonth(selectedMonthEpochMs, 1)
    val monthDiagnostics =
        financialDiagnostics.filter { diagnostic ->
            diagnostic.receivedAtEpochMs >=
                selectedMonthEpochMs &&
                diagnostic.receivedAtEpochMs <
                    nextMonthEpochMs
        }
    val ownedAccountGroups =
        AccountDisplayPolicy.groups(
            accountsById.values.toList()
        )
    val ownedAccountDiagnostics =
        monthDiagnostics.mapNotNull {
                diagnostic ->
            val sourceName =
                financialSourceDisplayName(
                    diagnostic
                )
            val group =
                ownedAccountGroups
                    .firstOrNull {
                        AccountDisplayPolicy
                            .matchesSource(
                                it,
                                sourceName
                            )
                    }
                ?: return@mapNotNull null
            group.key to diagnostic
        }
    val sourceSummaries =
        ownedAccountDiagnostics
            .groupBy { it.first }
            .mapNotNull { (key, entries) ->
                val group =
                    ownedAccountGroups
                        .firstOrNull {
                            it.key == key
                        }
                        ?: return@mapNotNull null
                FinancialSourceSummary(
                    key = key,
                    displayName =
                        group.displayName,
                    sourceType =
                        accountGroupTypeLabel(
                            group
                        ),
                    messageCount =
                        entries.size
                )
            }
            .sortedWith(
                compareByDescending<
                    FinancialSourceSummary
                > { it.messageCount }
                    .thenBy {
                        it.displayName
                    }
            )
    val selectedSourceDiagnostics =
        selectedFinancialSource
            ?.let { selectedKey ->
                ownedAccountDiagnostics
                    .filter {
                        it.first ==
                            selectedKey
                    }
                    .map { it.second }
            }
            .orEmpty()
            .sortedByDescending {
                it.receivedAtEpochMs
            }
    val possibleDuplicateIds = snapshot.possibleDuplicates
        .flatMap { listOf(it.first.id, it.second.id) }
        .toSet()
    val filtered = snapshot.transactions.filter { item ->
        if (item.reviewState == ReviewState.EXCLUDED.name) {
            return@filter false
        }
        if (item.id in possibleDuplicateIds) {
            return@filter false
        }

        when (selectedFilter) {
            "Expenses" ->
                item.direction == MoneyDirection.EXPENSE.name
            "Income" ->
                item.direction == MoneyDirection.INCOME.name
            "Transfers" ->
                item.direction == MoneyDirection.TRANSFER.name &&
                    item.relationType !=
                        TransactionStore.RELATION_REFUND
            else -> true
        }
    }

    val groupedFiltered = filtered
        .sortedByDescending { it.occurredAtEpochMs }
        .groupBy { dayStart(it.occurredAtEpochMs) }
        .toList()
        .sortedByDescending { it.first }

    Column(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 1.dp
        ) {
            PesaTrackTopBar(
                onProfileClick = onProfileClick,
                modifier = Modifier.padding(
                    horizontal = 18.dp,
                    vertical = 12.dp
                )
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = 18.dp,
                vertical = 10.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (ledgerUpdateState.isProcessing) {
                item {
                    LiveActivityUpdateStatus()
                }
            }

            item {
                ActivityMonthNavigator(
                    selectedMonthEpochMs =
                        selectedMonthEpochMs,
                    currentMonthEpochMs =
                        currentMonthEpochMs,
                    onPrevious = {
                        selectMonth(
                            shiftMonth(
                                selectedMonthEpochMs,
                                -1
                            )
                        )
                    },
                    onNext = {
                        if (
                            selectedMonthEpochMs <
                            currentMonthEpochMs
                        ) {
                            selectMonth(
                                shiftMonth(
                                    selectedMonthEpochMs,
                                    1
                                )
                            )
                        }
                    },
                    onPickMonth = {
                        showMonthPicker = true
                    }
                )
            }

            item {
                ActivitySummaryCard(
                    snapshot = snapshot,
                    previousSnapshot = previousSnapshot,
                    monthLabel = selectedMonthLabel
                )
            }

            if (
                reviewableFinancialEntities
                    .isNotEmpty()
            ) {
                item {
                    FinancialAccountDiscoveryCard(
                        entity =
                            reviewableFinancialEntities
                                .first(),
                        diagnostics =
                            financialDiagnostics,
                        remainingCount =
                            reviewableFinancialEntities
                                .size - 1,
                        onReview = {
                            accountReviewEntity =
                                reviewableFinancialEntities
                                    .first()
                        }
                    )
                }
            }

            item {
                GlobalReviewNavigationBar(
                    overview = globalOverview,
                    relationshipReviewCount =
                        relationshipReviews.size,
                    selectedQueue = selectedGlobalQueue,
                    onQueueSelected = { queue ->
                        selectedGlobalQueue =
                            if (
                                selectedGlobalQueue ==
                                queue
                            ) {
                                null
                            } else {
                                queue
                            }
                    }
                )
            }

            when (selectedGlobalQueue) {
                "Review" -> {
                    item {
                        GlobalReviewHeader(globalOverview)
                    }

                    if (
                        globalOverview.reviewGroups.isEmpty()
                    ) {
                        item {
                            EmptyGlobalReviewState()
                        }
                    } else {
                        items(
                            count =
                                globalOverview
                                    .reviewGroups
                                    .size,
                            key = { index ->
                                "global-review-" +
                                    globalOverview
                                        .reviewGroups[index]
                                        .key
                            }
                        ) { index ->
                            val group =
                                globalOverview
                                    .reviewGroups[index]
                            ReviewGroupRow(
                                group = group,
                                onClick = {
                                    onTransactionClick(
                                        group
                                            .representativeTransactionId
                                    )
                                }
                            )
                        }
                    }
                }

                "Duplicates" -> {
                    item {
                        GlobalDuplicateHeader(
                            globalOverview
                        )
                    }

                    if (
                        globalOverview
                            .possibleDuplicates
                            .isEmpty()
                    ) {
                        item {
                            EmptyGlobalDuplicateState()
                        }
                    } else {
                        items(
                            count =
                                globalOverview
                                    .possibleDuplicates
                                    .size,
                            key = { index ->
                                "global-duplicate-" +
                                    globalOverview
                                        .possibleDuplicates[
                                            index
                                        ]
                                        .key
                            }
                        ) { index ->
                            val group =
                                globalOverview
                                    .possibleDuplicates[
                                        index
                                    ]
                            PossibleDuplicateRow(
                                group = group,
                                onSameTransaction = {
                                    store.resolvePossibleDuplicate(
                                        pairKey = group.key,
                                        sameTransaction = true
                                    )
                                    refresh()
                                },
                                onKeepBoth = {
                                    store.resolvePossibleDuplicate(
                                        pairKey = group.key,
                                        sameTransaction = false
                                    )
                                    refresh()
                                }
                            )
                        }
                    }
                }

                "Links" -> {
                    item {
                        RelationshipReviewHeader(
                            count =
                                relationshipReviews.size
                        )
                    }

                    if (relationshipReviews.isEmpty()) {
                        item {
                            EmptyRelationshipReviewState()
                        }
                    } else {
                        items(
                            count =
                                relationshipReviews.size,
                            key = { index ->
                                relationshipReviews[
                                    index
                                ].key
                            }
                        ) { index ->
                            val review =
                                relationshipReviews[
                                    index
                                ]
                            RelationshipReviewRow(
                                item = review,
                                onLink = { candidate ->
                                    when (review.kind) {
                                        RelationshipReviewKind
                                            .REPAYMENT_FUNDING -> {
                                            val eventId =
                                                review.debtEventId
                                            val targetId =
                                                candidate.targetId
                                            if (
                                                eventId != null &&
                                                targetId != null
                                            ) {
                                                relationshipReviewStore
                                                    .resolveFunding(
                                                        eventId,
                                                        targetId
                                                    )
                                            }
                                        }
                                        RelationshipReviewKind
                                            .REPAYMENT_BALANCE -> {
                                            val eventId =
                                                review.debtEventId
                                            val targetId =
                                                candidate.targetId
                                            if (
                                                eventId != null &&
                                                targetId != null
                                            ) {
                                                relationshipReviewStore
                                                    .resolveBalance(
                                                        eventId,
                                                        targetId
                                                    )
                                            }
                                        }
                                        RelationshipReviewKind
                                            .LIABILITY_PAYMENT -> {
                                            val transactionId =
                                                review.transactionId
                                            val debtAccountId =
                                                candidate.targetKey
                                            if (
                                                transactionId != null &&
                                                debtAccountId != null
                                            ) {
                                                relationshipReviewStore
                                                    .resolveLiabilityPayment(
                                                        transactionId,
                                                        debtAccountId
                                                    )
                                            }
                                        }
                                        RelationshipReviewKind
                                            .INVESTMENT_SETTLEMENT -> {
                                            val eventId =
                                                review
                                                    .investmentEventId
                                            val targetId =
                                                candidate.targetId
                                            if (
                                                eventId != null &&
                                                targetId != null
                                            ) {
                                                relationshipReviewStore
                                                    .resolveInvestmentSettlement(
                                                        eventId,
                                                        targetId
                                                    )
                                            }
                                        }
                                    }
                                    refresh()
                                },
                                onKeepUnlinked = {
                                    when (review.kind) {
                                        RelationshipReviewKind
                                            .REPAYMENT_FUNDING ->
                                            review.debtEventId
                                                ?.let {
                                                    relationshipReviewStore
                                                        .resolveFunding(
                                                            it,
                                                            null
                                                        )
                                                }
                                        RelationshipReviewKind
                                            .REPAYMENT_BALANCE ->
                                            review.debtEventId
                                                ?.let {
                                                    relationshipReviewStore
                                                        .resolveBalance(
                                                            it,
                                                            null
                                                        )
                                                }
                                        RelationshipReviewKind
                                            .LIABILITY_PAYMENT ->
                                            review.transactionId
                                                ?.let {
                                                    relationshipReviewStore
                                                        .resolveLiabilityPayment(
                                                            it,
                                                            null
                                                        )
                                                }
                                        RelationshipReviewKind
                                            .INVESTMENT_SETTLEMENT ->
                                            review.investmentEventId
                                                ?.let {
                                                    relationshipReviewStore
                                                        .resolveInvestmentSettlement(
                                                            it,
                                                            null
                                                        )
                                                }
                                    }
                                    refresh()
                                }
                            )
                        }
                    }
                }
            }

            monthBuckets.forEach { bucket ->
                val isExpanded =
                    expandedMonthEpochMs ==
                        bucket.monthStartEpochMs

                item(
                    key =
                        "month-" +
                            bucket.monthStartEpochMs
                ) {
                    ActivityMonthSectionHeader(
                        monthLabel = monthLabel(
                            bucket.monthStartEpochMs
                        ),
                        transactionCount =
                            bucket.transactionCount,
                        expanded = isExpanded,
                        onClick = {
                            if (isExpanded) {
                                expandedMonthEpochMs =
                                    null
                            } else {
                                selectMonth(
                                    bucket
                                        .monthStartEpochMs
                                )
                            }
                        }
                    )
                }

                if (isExpanded) {
                    item(
                        key =
                            "filters-" +
                                bucket
                                    .monthStartEpochMs
                    ) {
                        LazyRow(
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {
                            listOf(
                                "All",
                                "Expenses",
                                "Income",
                                "Transfers"
                            ).forEach { label ->
                                item {
                                    FilterChip(
                                        selected =
                                            selectedFilter ==
                                                label,
                                        onClick = {
                                            selectedFilter =
                                                label
                                        },
                                        label = {
                                            Text(label)
                                        },
                                        shape =
                                            RoundedCornerShape(
                                                12.dp
                                            ),
                                        colors =
                                            FilterChipDefaults
                                                .filterChipColors(
                                                    selectedContainerColor =
                                                        MaterialTheme
                                                            .colorScheme
                                                            .primary,
                                                    selectedLabelColor =
                                                        MaterialTheme
                                                            .colorScheme
                                                            .onPrimary
                                                )
                                    )
                                }
                            }
                        }
                    }

                    if (sourceSummaries.isNotEmpty()) {
                        item(
                            key =
                                "financial-sources-" +
                                    bucket.monthStartEpochMs
                        ) {
                            FinancialSourceNavigator(
                                sources = sourceSummaries,
                                selectedSourceKey =
                                    selectedFinancialSource,
                                onSourceSelected = { key ->
                                    selectedFinancialSource =
                                        key
                                    selectedFilter = "All"
                                }
                            )
                        }
                    }

                    if (selectedFinancialSource != null) {
                        item(
                            key =
                                "source-feed-header-" +
                                    bucket.monthStartEpochMs
                        ) {
                            FinancialSourceFeedHeader(
                                source =
                                    sourceSummaries
                                        .firstOrNull {
                                            it.key ==
                                                selectedFinancialSource
                                        },
                                messageCount =
                                    selectedSourceDiagnostics
                                        .size
                            )
                        }

                        if (
                            selectedSourceDiagnostics
                                .isEmpty()
                        ) {
                            item(
                                key =
                                    "source-feed-empty-" +
                                        bucket
                                            .monthStartEpochMs
                            ) {
                                EmptyFinancialSourceFeed()
                            }
                        } else {
                            selectedSourceDiagnostics
                                .groupBy {
                                    dayStart(
                                        it.receivedAtEpochMs
                                    )
                                }
                                .toList()
                                .sortedByDescending {
                                    it.first
                                }
                                .forEach {
                                        (
                                            dayEpochMs,
                                            messages
                                        ) ->
                                    item(
                                        key =
                                            "source-day-" +
                                                dayEpochMs
                                    ) {
                                        FinancialMessageDateHeader(
                                            dayEpochMs =
                                                dayEpochMs,
                                            count =
                                                messages.size
                                        )
                                    }
                                    items(
                                        count =
                                            messages.size,
                                        key = { index ->
                                            "source-message-" +
                                                messages[
                                                    index
                                                ]
                                                    .receivedAtEpochMs +
                                                "-" +
                                                index
                                        }
                                    ) { index ->
                                        CapturedFinancialMessageRow(
                                            diagnostic =
                                                messages[
                                                    index
                                                ]
                                        )
                                    }
                                }
                        }
                    } else if (filtered.isEmpty()) {
                        item(
                            key =
                                "empty-" +
                                    bucket
                                        .monthStartEpochMs
                        ) {
                            EmptyActivityState(
                                selectedFilter =
                                    selectedFilter,
                                capturedCount =
                                    snapshot
                                        .transactions
                                        .size,
                                monthLabel =
                                    selectedMonthLabel
                            )
                        }
                    } else {
                        groupedFiltered.forEach {
                                (dayEpochMs, transactions) ->
                            item(
                                key = "day-" +
                                    bucket
                                        .monthStartEpochMs +
                                    "-" +
                                    dayEpochMs
                            ) {
                                ActivityDateHeader(
                                    dayEpochMs =
                                        dayEpochMs,
                                    transactions =
                                        transactions
                                )
                            }
                            items(
                                count =
                                    transactions.size,
                                key = { index ->
                                    transactions[index].id
                                }
                            ) { index ->
                                val transaction =
                                    transactions[index]
                                ActivityRow(
                                    item = transaction,
                                    accountName =
                                        transaction
                                            .accountId
                                            ?.let {
                                                accountsById[
                                                    it
                                                ]?.name
                                            },
                                    onClick = {
                                        onTransactionClick(
                                            transaction.id
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMonthPicker) {
        ActivityMonthPickerDialog(
            selectedMonthEpochMs =
                selectedMonthEpochMs,
            currentMonthEpochMs =
                currentMonthEpochMs,
            onDismiss = {
                showMonthPicker = false
            },
            onMonthSelected = { epochMs ->
                selectMonth(epochMs)
                showMonthPicker = false
            }
        )
    }

    accountReviewEntity?.let { entity ->
        FinancialAccountReviewDialog(
            entity = entity,
            diagnostics =
                financialDiagnostics,
            onDismiss = {
                accountReviewEntity = null
            },
            onConfirmOwned = {
                financialStore.confirmEntityOwned(
                    entity.entityKey
                )
                financialCoordinator
                    .reconcilePendingTransfers(
                        entity.entityKey
                    )
                accountReviewEntity = null
                refresh()
            },
            onNotMine = {
                financialStore.markEntityExternal(
                    entity.entityKey
                )
                financialCoordinator
                    .reconcileExternalEntity(
                        entity.entityKey
                    )
                accountReviewEntity = null
                refresh()
            }
        )
    }
}

@Composable
private fun FinancialAccountDiscoveryCard(
    entity: DiscoveredFinancialEntity,
    diagnostics:
        List<FinancialIngestionDiagnostic>,
    remainingCount: Int,
    onReview: () -> Unit
) {
    val displayName =
        financialEntityReviewDisplayName(
            entity = entity,
            diagnostics = diagnostics
        )

    PesaCard {
        Column(
            modifier =
                Modifier.padding(15.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                IconBubble(
                    icon =
                        Icons.Rounded
                            .AccountBalance,
                    size = 44.dp,
                    background =
                        pesaToneContainer("blue"),
                    tint =
                        pesaToneContent("blue")
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        "Account ownership needs review",
                        style =
                            MaterialTheme.typography
                                .titleMedium
                    )
                    Text(
                        displayName,
                        style =
                            MaterialTheme.typography
                                .titleLarge
                    )
                }
                SmallStatusPill("Review")
            }

            Text(
                "PesaTrack saw financial evidence connected to this account name, but it has not assumed the account belongs to you.",
                style =
                    MaterialTheme.typography
                        .bodyMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
            Text(
                ownershipEvidenceLabel(
                    entity
                ),
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            if (remainingCount > 0) {
                Text(
                    "+" + remainingCount +
                        " more account" +
                        if (
                            remainingCount == 1
                        ) {
                            ""
                        } else {
                            "s"
                        } +
                        " waiting for review",
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            Button(
                onClick = onReview,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("Review account")
            }
        }
    }
}

@Composable
private fun FinancialAccountReviewDialog(
    entity: DiscoveredFinancialEntity,
    diagnostics:
        List<FinancialIngestionDiagnostic>,
    onDismiss: () -> Unit,
    onConfirmOwned: () -> Unit,
    onNotMine: () -> Unit
) {
    val displayName =
        financialEntityReviewDisplayName(
            entity = entity,
            diagnostics = diagnostics
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Does this account belong to you?"
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    displayName,
                    style =
                        MaterialTheme.typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.SemiBold
                )
                Text(
                    "Choose “Yes, my account” only if this is one of your own bank, wallet or savings accounts."
                )
                Text(
                    "Why it matters: PesaTrack uses ownership to decide whether money is income/expense or simply moving between your own accounts.",
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
                Text(
                    "Nothing is changed until you confirm below.",
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick =
                    onConfirmOwned
            ) {
                Text("Yes, my account")
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                TextButton(
                    onClick =
                        onDismiss
                ) {
                    Text("Cancel")
                }
                OutlinedButton(
                    onClick =
                        onNotMine
                ) {
                    Text("No, not mine")
                }
            }
        }
    )
}

@Composable
private fun ActivityMonthNavigator(
    selectedMonthEpochMs: Long,
    currentMonthEpochMs: Long,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPickMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onPickMonth),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 13.dp,
                    vertical = 11.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    monthLabel(selectedMonthEpochMs),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 9.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Choose month",
                    tint =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        MonthArrowButton(
            icon = Icons.Rounded.ChevronLeft,
            contentDescription = "Previous month",
            enabled = true,
            onClick = onPrevious
        )
        MonthArrowButton(
            icon = Icons.Rounded.ChevronRight,
            contentDescription = "Next month",
            enabled =
                selectedMonthEpochMs <
                    currentMonthEpochMs,
            onClick = onNext
        )
    }
}

@Composable
private fun MonthArrowButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(42.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant
                .copy(alpha = .45f)
        }
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                        .copy(alpha = .45f)
                }
            )
        }
    }
}

@Composable
private fun ActivityMonthPickerDialog(
    selectedMonthEpochMs: Long,
    currentMonthEpochMs: Long,
    onDismiss: () -> Unit,
    onMonthSelected: (Long) -> Unit
) {
    val selectedCalendar =
        remember(selectedMonthEpochMs) {
            Calendar.getInstance().apply {
                timeInMillis =
                    selectedMonthEpochMs
            }
        }
    val currentCalendar =
        remember(currentMonthEpochMs) {
            Calendar.getInstance().apply {
                timeInMillis =
                    currentMonthEpochMs
            }
        }
    var displayedYear by remember(
        selectedMonthEpochMs
    ) {
        mutableStateOf(
            selectedCalendar.get(
                Calendar.YEAR
            )
        )
    }
    val selectedYear =
        selectedCalendar.get(Calendar.YEAR)
    val selectedMonth =
        selectedCalendar.get(Calendar.MONTH)
    val currentYear =
        currentCalendar.get(Calendar.YEAR)
    val monthNames = remember {
        (0..11).map { month ->
            Calendar.getInstance().apply {
                clear()
                set(2000, month, 1)
            }.let { calendar ->
                SimpleDateFormat(
                    "MMM",
                    Locale.getDefault()
                ).format(calendar.time)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Choose activity month")
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            displayedYear -= 1
                        }
                    ) {
                        Icon(
                            Icons.Rounded
                                .ChevronLeft,
                            contentDescription =
                                "Previous year"
                        )
                    }
                    Text(
                        displayedYear.toString(),
                        modifier =
                            Modifier.weight(1f),
                        style = MaterialTheme
                            .typography
                            .titleLarge,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = {
                            displayedYear += 1
                        },
                        enabled =
                            displayedYear <
                                currentYear
                    ) {
                        Icon(
                            Icons.Rounded
                                .ChevronRight,
                            contentDescription =
                                "Next year"
                        )
                    }
                }

                (0 until 12 step 3).forEach {
                    startMonth ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {
                        (
                            startMonth until
                                startMonth + 3
                            ).forEach { month ->
                            val candidate =
                                monthStart(
                                    displayedYear,
                                    month
                                )
                            FilterChip(
                                modifier =
                                    Modifier.weight(
                                        1f
                                    ),
                                selected =
                                    displayedYear ==
                                        selectedYear &&
                                        month ==
                                        selectedMonth,
                                onClick = {
                                    onMonthSelected(
                                        candidate
                                    )
                                },
                                enabled =
                                    candidate <=
                                        currentMonthEpochMs,
                                label = {
                                    Text(
                                        monthNames[
                                            month
                                        ],
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun GlobalReviewNavigationBar(
    overview: ActivityReviewOverview,
    relationshipReviewCount: Int,
    selectedQueue: String?,
    onQueueSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActivityQueueButton(
            label = "Review",
            count = overview.reviewGroups.size,
            icon = Icons.Rounded.FactCheck,
            selected = selectedQueue == "Review",
            tone = "green",
            modifier = Modifier.weight(1f),
            onClick = {
                onQueueSelected("Review")
            }
        )
        ActivityQueueButton(
            label = "Duplicates",
            count = overview.possibleDuplicates.size,
            icon = Icons.Rounded.ContentCopy,
            selected = selectedQueue == "Duplicates",
            tone = "violet",
            modifier = Modifier.weight(1f),
            onClick = {
                onQueueSelected("Duplicates")
            }
        )
        ActivityQueueButton(
            label = "Links",
            count = relationshipReviewCount,
            icon = Icons.Rounded.SyncAlt,
            selected = selectedQueue == "Links",
            tone = "amber",
            modifier = Modifier.weight(1f),
            onClick = {
                onQueueSelected("Links")
            }
        )
    }
}

@Composable
private fun ActivityQueueButton(
    label: String,
    count: Int,
    icon: ImageVector,
    selected: Boolean,
    tone: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = if (selected) {
        if (tone == "green") {
            MaterialTheme.colorScheme.primary
        } else {
            pesaToneContainer(tone)
        }
    } else {
        MaterialTheme.colorScheme.surface
    }
    val foreground = if (
        selected &&
        tone == "green"
    ) {
        MaterialTheme.colorScheme.onPrimary
    } else if (selected) {
        pesaToneContent(tone)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = background,
        border = if (selected) {
            null
        } else {
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline
                    .copy(alpha = .65f)
            )
        }
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 12.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = foreground
            )
            Text(
                label,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = foreground
            )
            if (count > 0) {
                Text(
                    count.toString(),
                    modifier = Modifier.padding(start = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = foreground.copy(alpha = .76f)
                )
            }
        }
    }
}

@Composable
private fun GlobalReviewHeader(
    overview: ActivityReviewOverview
) {
    val pendingTransactions =
        overview.reviewGroups.sumOf {
            it.transactionCount
        }

    Column {
        Text(
            "Review all months",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            pendingTransactions.toString() +
                " pending transaction" +
                (
                    if (pendingTransactions == 1) {
                        ""
                    } else {
                        "s"
                    }
                ) +
                " across all captured months. Teach an identity once and matching history can follow.",
            modifier = Modifier.padding(top = 3.dp),
            style = MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GlobalDuplicateHeader(
    overview: ActivityReviewOverview
) {
    Text(
        overview.possibleDuplicates.size.toString() +
            " possible duplicate pair" +
            (
                if (
                    overview.possibleDuplicates.size == 1
                ) {
                    ""
                } else {
                    "s"
                }
            ) +
            " across all captured months",
        style = MaterialTheme.typography.titleLarge
    )
}

@Composable
private fun EmptyGlobalReviewState() {
    PesaCard {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            IconBubble(
                Icons.Rounded.HelpOutline,
                size = 52.dp,
                background =
                    pesaToneContainer("green"),
                tint =
                    pesaToneContent("green")
            )
            Text(
                "Nothing needs review",
                modifier =
                    Modifier.padding(top = 12.dp),
                style =
                    MaterialTheme.typography
                        .titleMedium
            )
            Text(
                "There are no pending review groups across the captured months.",
                modifier =
                    Modifier.padding(top = 5.dp),
                style =
                    MaterialTheme.typography
                        .bodyMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyGlobalDuplicateState() {
    PesaCard {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            IconBubble(
                Icons.Rounded.WarningAmber,
                size = 52.dp,
                background =
                    pesaToneContainer("green"),
                tint =
                    pesaToneContent("green")
            )
            Text(
                "No possible duplicates",
                modifier =
                    Modifier.padding(top = 12.dp),
                style =
                    MaterialTheme.typography
                        .titleMedium
            )
            Text(
                "No unresolved duplicate pairs remain across the captured months.",
                modifier =
                    Modifier.padding(top = 5.dp),
                style =
                    MaterialTheme.typography
                        .bodyMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivityMonthSectionHeader(
    monthLabel: String,
    transactionCount: Int,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (expanded) 2.dp else 0.dp
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    monthLabel + " activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    transactionCount.toString() +
                        " transaction" +
                        if (transactionCount == 1) "" else "s",
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (expanded) {
                    Icons.Rounded.ExpandLess
                } else {
                    Icons.Rounded.ExpandMore
                },
                contentDescription = if (expanded) {
                    "Collapse month"
                } else {
                    "Expand month"
                }
            )
        }
    }
}

@Composable
private fun LiveActivityUpdateStatus() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(
                "Updating activity…",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "A financial notification is being checked and reconciled.",
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ActivitySummaryCard(
    snapshot: ActivityLedgerSnapshot,
    previousSnapshot: ActivityLedgerSnapshot,
    monthLabel: String
) {
    val summary = snapshot.summary
    val previous = previousSnapshot.summary
    val savingsMinor = monthSavings(snapshot)
    val previousSavingsMinor =
        monthSavings(previousSnapshot)
    val dark = isPesaDarkTheme()
    val gradient = if (dark) {
        listOf(
            Color(0xFF06453E),
            Color(0xFF0A6A59)
        )
    } else {
        listOf(
            Color(0xFF087A5B),
            Color(0xFF0A9B73)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(gradient)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(11.dp),
                    color = Color.White.copy(alpha = .14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                            tint = Color(0xFFF6C65B)
                        )
                    }
                }
                Text(
                    monthLabel + " Summary",
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(18.dp)
            ) {
                SummaryAmountWithChange(
                    label = "Money in",
                    value = formatMoney(
                        summary.incomeMinor
                    ),
                    current = summary.incomeMinor,
                    previous = previous.incomeMinor,
                    positiveWhenUp = true,
                    modifier = Modifier.weight(1f)
                )
                SummaryAmountWithChange(
                    label = "Money spent",
                    value = formatMoney(
                        summary.spentMinor
                    ),
                    current = summary.spentMinor,
                    previous = previous.spentMinor,
                    positiveWhenUp = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp),
                shape = RoundedCornerShape(15.dp),
                color = if (dark) {
                    Color(0xFF463718)
                } else {
                    Color(0xFFFFE7A8)
                }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = RoundedCornerShape(11.dp),
                        color = MaterialTheme
                            .colorScheme
                            .primary
                            .copy(alpha = .13f)
                    ) {
                        Box(
                            contentAlignment =
                                Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded
                                    .AccountBalanceWallet,
                                contentDescription = null,
                                modifier =
                                    Modifier.size(21.dp),
                                tint =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp)
                    ) {
                        Text(
                            "Saved & invested",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                        Text(
                            formatMoney(savingsMinor),
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    SavingsChangeBadge(
                        current = savingsMinor,
                        previous =
                            previousSavingsMinor
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryAmountWithChange(
    label: String,
    value: String,
    current: Long,
    previous: Long,
    positiveWhenUp: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = .72f)
        )
        Text(
            value,
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
        SummaryComparison(
            current = current,
            previous = previous,
            positiveWhenUp = positiveWhenUp
        )
    }
}

@Composable
private fun SummaryComparison(
    current: Long,
    previous: Long,
    positiveWhenUp: Boolean
) {
    val percent = percentageChange(
        current = current,
        previous = previous
    )

    if (percent == null) {
        Text(
            if (current == 0L) {
                "No change from prior month"
            } else {
                "New vs prior month"
            },
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = .68f)
        )
        return
    }

    val up = percent > 0
    val favorable =
        if (positiveWhenUp) up else !up
    val tint = if (percent == 0) {
        Color.White.copy(alpha = .66f)
    } else if (favorable) {
        Color(0xFF8CF2C6)
    } else {
        Color(0xFFFFA0AA)
    }

    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (percent != 0) {
            Icon(
                if (up) {
                    Icons.Rounded.ArrowUpward
                } else {
                    Icons.Rounded.ArrowDownward
                },
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            if (percent == 0) {
                "0% from prior month"
            } else {
                kotlin.math.abs(percent)
                    .toString() +
                    "% from prior month"
            },
            modifier = Modifier.padding(
                start = if (percent == 0) {
                    0.dp
                } else {
                    2.dp
                }
            ),
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

@Composable
private fun SavingsChangeBadge(
    current: Long,
    previous: Long
) {
    val percent = percentageChange(
        current = current,
        previous = previous
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme
            .colorScheme
            .primary
            .copy(alpha = .12f)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 9.dp,
                vertical = 5.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            if (
                percent != null &&
                percent != 0
            ) {
                Icon(
                    if (percent > 0) {
                        Icons.Rounded.ArrowUpward
                    } else {
                        Icons.Rounded.ArrowDownward
                    },
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (percent > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            Text(
                when {
                    percent == null &&
                        current > 0L ->
                        "New"
                    percent == null ->
                        "No change"
                    else ->
                        kotlin.math.abs(percent)
                            .toString() + "%"
                },
                modifier = Modifier.padding(
                    start = if (
                        percent != null &&
                        percent != 0
                    ) {
                        2.dp
                    } else {
                        0.dp
                    }
                ),
                style =
                    MaterialTheme.typography
                        .labelMedium,
                color = when {
                    percent == null ||
                        percent >= 0 ->
                        MaterialTheme
                            .colorScheme
                            .primary
                    else ->
                        MaterialTheme
                            .colorScheme
                            .error
                }
            )
        }
    }
}

private fun percentageChange(
    current: Long,
    previous: Long
): Int? {
    if (previous == 0L) return null
    return (
        ((current - previous) * 100.0) /
            kotlin.math.abs(previous.toDouble())
        ).toInt()
}

private fun monthSavings(
    snapshot: ActivityLedgerSnapshot
): Long =
    snapshot.transactions
        .filter {
            it.category in setOf(
                TransactionCategory.SAVINGS.name,
                TransactionCategory.INVESTMENT.name
            ) &&
                it.reviewState !=
                    ReviewState.EXCLUDED.name &&
                it.reviewState !=
                    ReviewState.NEEDS_REVIEW.name
        }
        .sumOf { it.amountMinor }

@Composable
private fun ResponsiveActivityMetrics(
    snapshot: ActivityLedgerSnapshot,
    monthLabel: String
) {
    val summary = snapshot.summary
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 600.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActivityMetric(
                        "Spent",
                        formatMoney(summary.spentMinor),
                        "$monthLabel",
                        Icons.Rounded.Paid,
                        "red",
                        Modifier.weight(1f)
                    )
                    ActivityMetric(
                        "Income",
                        formatMoney(summary.incomeMinor),
                        "$monthLabel",
                        Icons.Rounded.CallReceived,
                        "green",
                        Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActivityMetric(
                        "Fees",
                        formatMoney(summary.feesMinor),
                        "M-Pesa + Fuliza",
                        Icons.Rounded.ReceiptLong,
                        "amber",
                        Modifier.weight(1f)
                    )
                    ActivityMetric(
                        "Net",
                        formatSignedMoney(summary.netMinor),
                        "Cash flow",
                        Icons.Rounded.AccountBalance,
                        "blue",
                        Modifier.weight(1f)
                    )
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActivityMetric("Spent", formatMoney(summary.spentMinor), "$monthLabel", Icons.Rounded.Paid, "red", Modifier.weight(1f))
                ActivityMetric("Income", formatMoney(summary.incomeMinor), "$monthLabel", Icons.Rounded.CallReceived, "green", Modifier.weight(1f))
                ActivityMetric("Fees", formatMoney(summary.feesMinor), "M-Pesa fees", Icons.Rounded.ReceiptLong, "amber", Modifier.weight(1f))
                ActivityMetric("Net", formatSignedMoney(summary.netMinor), "Cash flow", Icons.Rounded.AccountBalance, "blue", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActivityMetric(
    label: String,
    value: String,
    helper: String,
    icon: ImageVector,
    tone: String,
    modifier: Modifier
) {
    Column(modifier) {
        IconBubble(
            icon,
            size = 34.dp,
            background = pesaToneContainer(tone),
            tint = pesaToneContent(tone)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Text(
            helper,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyActivityState(
    selectedFilter: String,
    capturedCount: Int,
    monthLabel: String
) {
    PesaCard {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBubble(
                if (selectedFilter == "Review") Icons.Rounded.HelpOutline else Icons.Rounded.ReceiptLong,
                size = 52.dp,
                background = pesaToneContainer(if (selectedFilter == "Review") "amber" else "green"),
                tint = pesaToneContent(if (selectedFilter == "Review") "amber" else "green")
            )
            Text(
                if (selectedFilter == "Review") {
                    "Nothing to review in $monthLabel"
                } else if (capturedCount == 0) {
                    "No activity in $monthLabel"
                } else {
                    "Nothing in $selectedFilter for $monthLabel"
                },
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                if (selectedFilter == "Review") {
                    "Good — this month's captured transactions currently have no pending review groups."
                } else if (capturedCount == 0) {
                    "Choose another month or refresh after importing older M-Pesa and bank activity."
                } else {
                    "Try another filter to see more activity from $monthLabel."
                },
                modifier = Modifier.padding(top = 5.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RelationshipReviewHeader(
    count: Int
) {
    Column {
        Text(
            "$count financial relationship" +
                if (count == 1) {
                    " needs review"
                } else {
                    "s need review"
                },
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "PesaTrack found plausible links but deliberately refused to choose automatically.",
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyRelationshipReviewState() {
    PesaCard {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            IconBubble(
                Icons.Rounded.SyncAlt,
                size = 52.dp,
                background =
                    pesaToneContainer("green"),
                tint =
                    pesaToneContent("green")
            )
            Text(
                "No ambiguous links",
                modifier =
                    Modifier.padding(top = 12.dp),
                style =
                    MaterialTheme.typography
                        .titleMedium
            )
            Text(
                "PesaTrack currently has no debt, liability-payment, or investment-settlement relationships that need a manual decision.",
                modifier =
                    Modifier.padding(top = 5.dp),
                style =
                    MaterialTheme.typography
                        .bodyMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RelationshipReviewRow(
    item: RelationshipReviewItem,
    onLink: (RelationshipReviewCandidate) -> Unit,
    onKeepUnlinked: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                IconBubble(
                    Icons.Rounded.SyncAlt,
                    size = 42.dp,
                    background =
                        pesaToneContainer("amber"),
                    tint =
                        pesaToneContent("amber")
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 11.dp)
                ) {
                    Text(
                        when (item.kind) {
                            RelationshipReviewKind
                                .REPAYMENT_FUNDING ->
                                "Which incoming funds paid this loan?"
                            RelationshipReviewKind
                                .REPAYMENT_BALANCE ->
                                "Which balance came after this repayment?"
                            RelationshipReviewKind
                                .LIABILITY_PAYMENT ->
                                "Is this payment a debt repayment?"
                            RelationshipReviewKind
                                .INVESTMENT_SETTLEMENT ->
                                "Which cash movement settled this investment?"
                        },
                        style =
                            MaterialTheme.typography
                                .titleMedium
                    )
                    Text(
                        item.debtName +
                            " • " +
                            formatMoney(
                                item.repaymentAmountMinor
                            ),
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            Text(
                (
                    when (item.kind) {
                        RelationshipReviewKind
                            .LIABILITY_PAYMENT ->
                            "Paid from: "
                        RelationshipReviewKind
                            .INVESTMENT_SETTLEMENT ->
                            "Investment event: "
                        else ->
                            "Funding account: "
                    }
                ) +
                    item.fundingAccountName +
                    " • " +
                    formatPreciseTime(
                        item.occurredAtEpochMs
                    ),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            item.candidates.forEachIndexed {
                    index,
                    candidate ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape =
                        RoundedCornerShape(14.dp),
                    color =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                            .copy(alpha = .45f)
                ) {
                    Column(
                        Modifier.padding(12.dp)
                    ) {
                        Text(
                            candidate.label,
                            style =
                                MaterialTheme
                                    .typography
                                    .labelLarge
                        )
                        Text(
                            formatMoney(
                                candidate
                                    .amountMinor
                            ) +
                                " • " +
                                formatPreciseTime(
                                    candidate
                                        .occurredAtEpochMs
                                ),
                            modifier =
                                Modifier.padding(
                                    top = 2.dp
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                        Text(
                            candidate.reason,
                            modifier =
                                Modifier.padding(
                                    top = 4.dp
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                pesaToneContent(
                                    "amber"
                                )
                        )
                        Button(
                            onClick = {
                                onLink(candidate)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                when (item.kind) {
                                    RelationshipReviewKind
                                        .REPAYMENT_FUNDING ->
                                        "Use as funding transaction"
                                    RelationshipReviewKind
                                        .REPAYMENT_BALANCE ->
                                        "Use this balance"
                                    RelationshipReviewKind
                                        .LIABILITY_PAYMENT ->
                                        "Mark as repayment to this debt"
                                    RelationshipReviewKind
                                        .INVESTMENT_SETTLEMENT ->
                                        "Use as investment settlement"
                                }
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onKeepUnlinked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text(
                    when (item.kind) {
                        RelationshipReviewKind
                            .REPAYMENT_FUNDING ->
                            "None of these funded it"
                        RelationshipReviewKind
                            .REPAYMENT_BALANCE ->
                            "None of these balances"
                        RelationshipReviewKind
                            .LIABILITY_PAYMENT ->
                            "This was not a debt repayment"
                        RelationshipReviewKind
                            .INVESTMENT_SETTLEMENT ->
                            "None of these settled it"
                    }
                )
            }

            Text(
                "Your choice is remembered and automatic reconciliation will not overwrite it.",
                modifier =
                    Modifier.padding(top = 8.dp),
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
private fun PossibleDuplicateCallout(
    pairCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                Icons.Rounded.WarningAmber,
                size = 42.dp,
                background =
                    MaterialTheme.colorScheme.surface.copy(alpha = .7f),
                tint = MaterialTheme.colorScheme.error
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp)
            ) {
                Text(
                    pairCount.toString() +
                        " possible duplicate pair" +
                        if (pairCount == 1) "" else "s",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "These observations are close enough to deserve reconciliation before they are treated as separate transactions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Review possible duplicates"
            )
        }
    }
}

@Composable
private fun PossibleDuplicateRow(
    group: PossibleDuplicateGroup,
    onSameTransaction: () -> Unit,
    onKeepBoth: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(
                    Icons.Rounded.WarningAmber,
                    size = 40.dp,
                    background = MaterialTheme.colorScheme.errorContainer,
                    tint = MaterialTheme.colorScheme.error
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 11.dp)
                ) {
                    Text(
                        formatMoney(group.amountMinor),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Two observations • " +
                            (group.timeDeltaMs / 1000L).toString() +
                            "s apart",
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DuplicateObservationLine(
                label = "Observation 1",
                item = group.first
            )
            DuplicateObservationLine(
                label = "Observation 2",
                item = group.second
            )

            Text(
                "PesaTrack will not remove either record unless you confirm they describe the same transaction.",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onKeepBoth,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Keep both")
                }
                Button(
                    onClick = onSameTransaction,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Same transaction")
                }
            }
        }
    }
}

@Composable
private fun DuplicateObservationLine(
    label: String,
    item: StoredTransaction
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            duplicateSourceLabel(item) +
                " • " +
                formatPreciseTime(item.receivedAtEpochMs),
            modifier = Modifier.padding(top = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            buildString {
                append(humanize(item.direction))
                if (!item.transactionCode.isNullOrBlank()) {
                    append(" • code ")
                    append(item.transactionCode)
                } else {
                    append(" • no transaction code")
                }
            },
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyDuplicateState(
    monthLabel: String
) {
    PesaCard {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBubble(
                Icons.Rounded.WarningAmber,
                size = 52.dp,
                background = pesaToneContainer("green"),
                tint = pesaToneContent("green")
            )
            Text(
                "No possible duplicates in " + monthLabel,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Any previously reviewed pairs marked Keep both are also kept out of this queue.",
                modifier = Modifier.padding(top = 5.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReviewQueueCallout(
    groupCount: Int,
    transactionCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = pesaToneContainer("amber")
    ) {
        Row(
            Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                Icons.Rounded.HelpOutline,
                size = 42.dp,
                background = MaterialTheme.colorScheme.surface.copy(alpha = .7f),
                tint = pesaToneContent("amber")
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp)
            ) {
                Text(
                    "$groupCount group" +
                        if (groupCount == 1) " to review" else "s to review",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "$transactionCount pending transaction" +
                        if (transactionCount == 1) "" else "s" +
                        " collapsed by full name or phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReviewGroupRow(
    group: ReviewGroup,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                Icons.Rounded.HelpOutline,
                size = 42.dp,
                background = pesaToneContainer("amber"),
                tint = pesaToneContent("amber")
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp)
            ) {
                Text(
                    group.label,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${group.transactionCount} transaction" +
                        if (group.transactionCount == 1) "" else "s" +
                        " • ${formatMoney(group.totalMinor)} total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Tap once to classify and optionally remember this identity.",
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = pesaToneContent("amber")
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivityDateHeader(
    dayEpochMs: Long,
    transactions: List<StoredTransaction>
) {
    val expenseTotal = transactions
        .filter { it.direction == MoneyDirection.EXPENSE.name }
        .sumOf { it.amountMinor + it.feeMinor }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            dayLabel(dayEpochMs),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (expenseTotal > 0L) {
            Text(
                formatMoney(expenseTotal),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivityRow(
    item: StoredTransaction,
    accountName: String?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val categoryStore = remember { CategoryStore.get(context) }
    val categoryId = categoryStore.canonicalId(item.category)
    val categoryLabel = categoryStore.labelFor(categoryId)
    val needsReview =
        item.reviewState == ReviewState.NEEDS_REVIEW.name
    val isRefund =
        item.relationType ==
            TransactionStore.RELATION_REFUND
    val direction = runCatching {
        MoneyDirection.valueOf(item.direction)
    }.getOrDefault(MoneyDirection.NEUTRAL)

    val tone = when {
        needsReview -> "amber"
        isRefund -> "green"
        direction == MoneyDirection.INCOME -> "green"
        direction == MoneyDirection.TRANSFER -> "blue"
        categoryId == TransactionCategory.SHOPPING.name -> "violet"
        categoryId == TransactionCategory.ELECTRICITY.name ||
            categoryId == TransactionCategory.RENT.name -> "amber"
        else -> "green"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MerchantBadge(
                item = item,
                categoryId = categoryId,
                tone = tone
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp)
            ) {
                val rawDisplayName =
                    item.evidenceLabel
                        ?.takeIf {
                            item.evidenceSource
                                ?.startsWith(
                                    "entity_intelligence:"
                                ) == true
                        }
                        ?: item.counterparty
                        ?: humanize(item.type)
                val displayName =
                    financialActivityDisplayName(
                        item = item,
                        rawDisplayName =
                            rawDisplayName,
                        accountName =
                            accountName
                    )

                Text(
                    displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatTime(item.occurredAtEpochMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "  •  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        when {
                            needsReview -> "Needs review"
                            isRefund ->
                                "Refund • " + categoryLabel
                            else ->
                                financialActivityEventLabel(
                                    item = item,
                                    categoryLabel =
                                        categoryLabel,
                                    rawDisplayName =
                                        rawDisplayName,
                                    accountName =
                                        accountName
                                )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = pesaToneContent(tone)
                    )
                }
                if (
                    item.counterparty.isNullOrBlank() &&
                    !item.evidenceLabel.isNullOrBlank()
                ) {
                    Text(
                        "Possible match: " + item.evidenceLabel,
                        modifier = Modifier.padding(top = 3.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.fulizaFinancedMinor > 0L) {
                    Text(
                        "Fuliza financed " +
                            formatMoney(item.fulizaFinancedMinor) +
                            if (item.fulizaAccessFeeMinor > 0L) {
                                " • access fee " +
                                    formatMoney(
                                        item.fulizaAccessFeeMinor
                                    )
                            } else {
                                ""
                            },
                        modifier = Modifier.padding(top = 3.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    amountLabel(item, direction),
                    style = MaterialTheme.typography.titleSmall,
                    color = when {
                        direction == MoneyDirection.INCOME ||
                            item.type ==
                            MpesaTransactionType.REVERSAL.name ->
                            MaterialTheme.colorScheme.primary
                        direction == MoneyDirection.EXPENSE ->
                            MaterialTheme.colorScheme.error
                        needsReview ||
                            direction == MoneyDirection.NEUTRAL ->
                            pesaToneContent("amber")
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(18.dp)
                )
            }
        }
    }
}

data class FinancialSourceSummary(
    val key: String,
    val displayName: String,
    val sourceType: String,
    val messageCount: Int
)

@Composable
private fun FinancialSourceNavigator(
    sources: List<FinancialSourceSummary>,
    selectedSourceKey: String?,
    onSourceSelected: (String?) -> Unit
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(7.dp)
    ) {
        Text(
            "Your account messages",
            style =
                MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Only confirmed accounts are shown. The badge is the number of captured messages for this month.",
            style =
                MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
        LazyRow(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected =
                        selectedSourceKey == null,
                    onClick = {
                        onSourceSelected(null)
                    },
                    label = {
                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Text("All accounts")
                            val total =
                                sources.sumOf {
                                    it.messageCount
                                }
                            if (total > 0) {
                                Badge(
                                    modifier =
                                        Modifier.padding(
                                            start = 6.dp
                                        )
                                ) {
                                    Text(
                                        total.toString()
                                    )
                                }
                            }
                        }
                    }
                )
            }
            sources.forEach { source ->
                item(key = source.key) {
                    FilterChip(
                        selected =
                            selectedSourceKey ==
                                source.key,
                        onClick = {
                            onSourceSelected(
                                source.key
                            )
                        },
                        label = {
                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Text(
                                    source.displayName
                                )
                                Badge(
                                    modifier =
                                        Modifier.padding(
                                            start = 6.dp
                                        )
                                ) {
                                    Text(
                                        source.messageCount
                                            .toString()
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialSourceFeedHeader(
    source: FinancialSourceSummary?,
    messageCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color =
            MaterialTheme.colorScheme
                .surfaceVariant
                .copy(alpha = .48f)
    ) {
        Column(
            Modifier.padding(
                horizontal = 14.dp,
                vertical = 12.dp
            )
        ) {
            Text(
                source?.displayName
                    ?: "Financial source",
                style =
                    MaterialTheme.typography
                        .titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                (
                    source?.sourceType
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Financial"
                    ) +
                    " • " +
                    messageCount +
                    " captured message" +
                    if (messageCount == 1) {
                        ""
                    } else {
                        "s"
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
        }
    }
}

@Composable
private fun EmptyFinancialSourceFeed() {
    PesaCard {
        Text(
            "No captured financial messages for this source in the selected month.",
            modifier = Modifier.padding(18.dp),
            style =
                MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun FinancialMessageDateHeader(
    dayEpochMs: Long,
    count: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            dayLabel(dayEpochMs),
            modifier = Modifier.weight(1f),
            style =
                MaterialTheme.typography
                    .titleSmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
        Text(
            count.toString() +
                " message" +
                if (count == 1) "" else "s",
            style =
                MaterialTheme.typography
                    .labelMedium,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun CapturedFinancialMessageRow(
    diagnostic: FinancialIngestionDiagnostic
) {
    val currentSourceType =
        financialSourceType(
            diagnostic
        )
    val currentEventLabel =
        if (
            currentSourceType != "Bank" &&
            diagnostic.eventLabel
                .startsWith(
                    "Bank ",
                    ignoreCase = true
                )
        ) {
            diagnostic.eventLabel
                .removePrefix("Bank ")
                .replaceFirstChar {
                    it.uppercase()
                }
        } else {
            diagnostic.eventLabel
        }

    val tone =
        when (diagnostic.code) {
            FinancialIngestionDiagnosticCode
                .INGESTED -> "green"
            FinancialIngestionDiagnosticCode
                .PENDING_REVIEW -> "amber"
            FinancialIngestionDiagnosticCode
                .UNDERSTOOD_NO_LEDGER_EFFECT ->
                "blue"
            FinancialIngestionDiagnosticCode
                .LINKED_OBSERVATION -> "green"
            FinancialIngestionDiagnosticCode
                .OBSERVATION_ONLY -> "blue"
            FinancialIngestionDiagnosticCode
                .UNRECOGNIZED_FINANCIAL_LANGUAGE ->
                "amber"
        }
    val status =
        when (diagnostic.code) {
            FinancialIngestionDiagnosticCode
                .INGESTED -> "Captured"
            FinancialIngestionDiagnosticCode
                .PENDING_REVIEW ->
                "Needs review"
            FinancialIngestionDiagnosticCode
                .UNDERSTOOD_NO_LEDGER_EFFECT ->
                "Understood"
            FinancialIngestionDiagnosticCode
                .LINKED_OBSERVATION -> "Linked"
            FinancialIngestionDiagnosticCode
                .OBSERVATION_ONLY -> "Observed"
            FinancialIngestionDiagnosticCode
                .UNRECOGNIZED_FINANCIAL_LANGUAGE ->
                "Not understood"
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 11.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            IconBubble(
                icon =
                    when {
                        currentEventLabel
                            .contains(
                                "loan",
                                ignoreCase = true
                            ) ->
                            Icons.Rounded.ReceiptLong
                        currentEventLabel
                            .contains(
                                "transfer",
                                ignoreCase = true
                            ) ->
                            Icons.Rounded.SyncAlt
                        currentEventLabel
                            .contains(
                                "received",
                                ignoreCase = true
                            ) ->
                            Icons.Rounded.CallReceived
                        else ->
                            Icons.Rounded.AccountBalance
                    },
                size = 40.dp,
                background =
                    pesaToneContainer(tone),
                tint =
                    pesaToneContent(tone)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp)
            ) {
                Text(
                    currentEventLabel,
                    style =
                        MaterialTheme.typography
                            .titleSmall,
                    fontWeight =
                        FontWeight.SemiBold
                )
                Text(
                    formatTime(
                        diagnostic
                            .receivedAtEpochMs
                    ) +
                        "  •  " +
                        currentSourceType,
                    modifier =
                        Modifier.padding(top = 3.dp),
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
            SmallStatusPill(status)
        }
    }
}

private fun accountGroupTypeLabel(
    group: TrackedAccountGroup
): String =
    when (group.type) {
        AccountType.BANK -> "Bank"
        AccountType.MOBILE_WALLET ->
            "Wallet"
        AccountType.SAVINGS ->
            "Savings"
        else -> "Account"
    }

private fun financialSourceDisplayName(
    diagnostic: FinancialIngestionDiagnostic
): String =
    FinancialProviderRegistry
        .messageSourceDisplayName(
            title = diagnostic.title,
            bankLike =
                diagnostic.sourceType == "Bank"
        )
        ?.takeIf(String::isNotBlank)
        ?: diagnostic.sourceDisplayName
            ?.takeIf(String::isNotBlank)
        ?: diagnostic.title
            ?.takeIf(String::isNotBlank)
        ?: "Unknown financial source"

private fun financialSourceType(
    diagnostic: FinancialIngestionDiagnostic
): String {
    val source =
        FinancialProviderRegistry
            .resolveMessageSourceAccount(
                title = diagnostic.title,
                bankLike =
                    diagnostic.sourceType ==
                        "Bank"
            )

    return when (
        source?.accountTypeHint
    ) {
        AccountType.BANK -> "Bank"
        AccountType.MOBILE_WALLET ->
            "Wallet"
        AccountType.SAVINGS ->
            "Savings"
        else ->
            if (
                diagnostic.sourceType ==
                "Lender"
            ) {
                "Lender"
            } else {
                "Financial"
            }
    }
}

private fun financialEntityReviewDisplayName(
    entity: DiscoveredFinancialEntity,
    diagnostics:
        List<FinancialIngestionDiagnostic>
): String {
    KenyanBankDirectory
        .matchSender(
            entity.displayName
        )
        ?.name
        ?.let {
            return it
        }

    val resolved =
        FinancialProviderRegistry
            .resolveAccount(
                entity.displayName
            )
    if (resolved.providerKey != null) {
        return resolved.displayName
    }

    val normalized =
        ownershipReviewKey(
            entity.displayName
        )
    if (
        normalized in
            setOf(
                "locksaving",
                "locksavings",
                "locksavingsaccount"
            ) &&
        diagnostics.any {
            val title =
                it.title.orEmpty()
            val source =
                it.sourceDisplayName
                    .orEmpty()
            val combined =
                (title + " " + source)
                    .uppercase()
                    .replace(
                        Regex("""[^A-Z0-9]+"""),
                        " "
                    )
            combined.contains(
                "M SHWARI"
            )
        }
    ) {
        return "M-Shwari Lock Savings"
    }

    return entity.displayName
}

private fun ownershipEvidenceLabel(
    entity: DiscoveredFinancialEntity
): String {
    val type =
        runCatching {
            AccountType.valueOf(
                entity.accountTypeHint
            )
        }.getOrDefault(
            AccountType.OTHER
        )
    val typeLabel =
        when (type) {
            AccountType.BANK -> "Bank"
            AccountType.MOBILE_WALLET ->
                "Wallet"
            AccountType.SAVINGS ->
                "Savings"
            else -> "Financial account"
        }

    val seen =
        SimpleDateFormat(
            "d MMM, h:mm a",
            Locale.getDefault()
        ).format(
            Date(
                entity.lastSeenEpochMs
            )
        )

    return "$typeLabel • last seen $seen"
}

private fun shouldOfferOwnershipReview(
    entity: DiscoveredFinancialEntity,
    diagnostics:
        List<FinancialIngestionDiagnostic>
): Boolean {
    if (
        !AccountDisplayPolicy
            .isMeaningfulAccountName(
                entity.displayName
            )
    ) {
        return false
    }

    val type =
        runCatching {
            AccountType.valueOf(
                entity.accountTypeHint
            )
        }.getOrDefault(
            AccountType.OTHER
        )
    if (
        type !in
            setOf(
                AccountType.BANK,
                AccountType.MOBILE_WALLET,
                AccountType.SAVINGS
            )
    ) {
        return false
    }

    if (entity.providerKey != null) {
        return true
    }

    val display =
        entity.displayName.trim()
    if (
        !display.endsWith(
            " Bank",
            ignoreCase = true
        )
    ) {
        return true
    }

    val base =
        display.substring(
            0,
            display.length - 5
        ).trim()
    val baseKey =
        ownershipReviewKey(base)

    val looksLikeLegacyPseudoBank =
        diagnostics.any { diagnostic ->
            val title =
                diagnostic.title
                    ?.trim()
                    ?.takeIf(
                        String::isNotBlank
                    )
                    ?: return@any false

            if (
                ownershipReviewKey(
                    title
                ) != baseKey
            ) {
                return@any false
            }

            FinancialProviderRegistry
                .resolveMessageSourceAccount(
                    title = title,
                    bankLike = true
                )
                ?.accountTypeHint !=
                AccountType.BANK
        }

    return !looksLikeLegacyPseudoBank
}

private fun ownershipReviewKey(
    value: String
): String =
    value.lowercase(Locale.ROOT)
        .replace(
            Regex("""[^a-z0-9]+"""),
            ""
        )

private fun financialSourceKey(
    displayName: String
): String =
    displayName
        .lowercase(Locale.ROOT)
        .replace(
            Regex("""[^a-z0-9]+"""),
            "-"
        )
        .trim('-')
        .ifBlank { "unknown" }

private fun financialActivityDisplayName(
    item: StoredTransaction,
    rawDisplayName: String,
    accountName: String?
): String {
    val senderStyleLabel =
        FINANCIAL_SENDER_SUFFIX_REGEX
            .containsMatchIn(rawDisplayName)
    val bankLike =
        senderStyleLabel &&
            (
                item.direction ==
                    MoneyDirection.TRANSFER.name ||
                    item.category ==
                        TransactionCategory
                            .TRANSFER.name
                )

    return if (senderStyleLabel) {
        FinancialProviderRegistry
            .messageSourceDisplayName(
                title = rawDisplayName,
                bankLike = bankLike
            )
            ?: rawDisplayName
    } else {
        rawDisplayName
    }
}

private fun financialActivityEventLabel(
    item: StoredTransaction,
    categoryLabel: String,
    rawDisplayName: String,
    accountName: String?
): String {
    val bankLike =
        listOfNotNull(
            rawDisplayName,
            accountName
        ).any {
            FINANCIAL_SENDER_SUFFIX_REGEX
                .containsMatchIn(it)
        }

    return when {
        item.category ==
            TransactionCategory.DEBT_LOAN.name &&
            item.direction ==
                MoneyDirection.INCOME.name ->
            "Loan"
        item.category ==
            TransactionCategory.DEBT_LOAN.name &&
            item.direction !=
                MoneyDirection.INCOME.name ->
            "Loan repayment"
        item.direction ==
            MoneyDirection.TRANSFER.name &&
            bankLike ->
            "Bank transfer"
        else -> categoryLabel
    }
}

private val FINANCIAL_SENDER_SUFFIX_REGEX =
    Regex(
        """\s+(?:BULK(?:\s+SMS)?|ALERTS?|SMS|NOTIFICATIONS?)\s*$""",
        RegexOption.IGNORE_CASE
    )

@Composable
private fun MerchantBadge(
    item: StoredTransaction,
    categoryId: String,
    tone: String
) {
    IconBubble(
        icon = iconFor(
            categoryId,
            item.type
        ),
        size = 40.dp,
        background = pesaToneContainer(tone),
        tint = pesaToneContent(tone)
    )
}

private fun iconFor(
    categoryId: String,
    type: String
): ImageVector = when (categoryId) {
    TransactionCategory.RENT.name ->
        Icons.Rounded.Home
    TransactionCategory.GROCERIES.name ->
        Icons.Rounded.ShoppingCart
    TransactionCategory.EATING_OUT.name,
    TransactionCategory.LUNCH.name ->
        Icons.Rounded.Restaurant
    TransactionCategory.ELECTRICITY.name ->
        Icons.Rounded.Bolt
    TransactionCategory.INTERNET.name ->
        Icons.Rounded.Wifi
    TransactionCategory.TRANSPORT.name ->
        Icons.Rounded.DirectionsBus
    TransactionCategory.HOME_HOUSEHOLD.name ->
        Icons.Rounded.Home
    TransactionCategory.SHOPPING.name ->
        Icons.Rounded.ShoppingCart
    TransactionCategory.FAMILY.name ->
        Icons.Rounded.Groups
    TransactionCategory.FEES.name,
    TransactionCategory.DEBT_LOAN.name,
    TransactionCategory.EDUCATION.name ->
        Icons.Rounded.ReceiptLong
    TransactionCategory.PHONE_AND_DATA.name ->
        Icons.Rounded.PhoneAndroid
    TransactionCategory.INCOME.name ->
        Icons.Rounded.CallReceived
    TransactionCategory.TRANSFER.name ->
        Icons.Rounded.SyncAlt
    TransactionCategory.CASH.name ->
        Icons.Rounded.LocalAtm
    TransactionCategory.SAVINGS.name ->
        Icons.Rounded.Savings
    TransactionCategory.INVESTMENT.name ->
        Icons.Rounded.AccountBalance
    TransactionCategory.WATER.name,
    TransactionCategory.HEALTH.name,
    TransactionCategory.ENTERTAINMENT.name,
    TransactionCategory.PERSONAL_CARE.name,
    TransactionCategory.OTHER.name ->
        Icons.Rounded.MoreHoriz
    TransactionCategory.UNCATEGORIZED.name ->
        if (type == MpesaTransactionType.REVERSAL.name) {
            Icons.Rounded.SyncAlt
        } else {
            Icons.Rounded.HelpOutline
        }
    else -> Icons.Rounded.MoreHoriz
}

private fun amountLabel(
    item: StoredTransaction,
    direction: MoneyDirection
): String {
    val sign = when {
        item.relationType ==
            TransactionStore.RELATION_REFUND -> "+"
        direction == MoneyDirection.INCOME -> "+"
        item.type == MpesaTransactionType.REVERSAL.name -> "+"
        direction == MoneyDirection.EXPENSE -> "-"
        else -> ""
    }
    return "$sign ${formatMoney(item.amountMinor)}".trim()
}

private fun formatMoney(minor: Long): String =
    "KES ${NumberFormat.getNumberInstance(Locale.US).format(minor / 100.0)}"

private fun formatSignedMoney(minor: Long): String =
    if (minor < 0) "- ${formatMoney(-minor)}" else formatMoney(minor)

private fun formatTime(epochMs: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(epochMs))

private fun dayStart(epochMs: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = epochMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun dayLabel(epochMs: Long): String {
    val today = dayStart(System.currentTimeMillis())
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = today
        add(Calendar.DAY_OF_MONTH, -1)
    }.timeInMillis

    return when (epochMs) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> SimpleDateFormat(
            "EEE, d MMM",
            Locale.getDefault()
        ).format(Date(epochMs))
    }
}

private fun monthLabel(epochMs: Long): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(epochMs))

private fun monthStart(epochMs: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = epochMs
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun shiftMonth(
    epochMs: Long,
    months: Int
): Long =
    Calendar.getInstance().apply {
        timeInMillis = epochMs
        add(Calendar.MONTH, months)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun monthStart(year: Int, month: Int): Long =
    Calendar.getInstance().apply {
        clear()
        set(year, month, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis


private fun duplicateSourceLabel(
    item: StoredTransaction
): String =
    item.counterparty
        ?.takeIf(String::isNotBlank)
        ?: item.evidenceSource
            ?.takeIf(String::isNotBlank)
        ?: item.sourcePackage
            ?.substringAfterLast('.')
            ?.takeIf(String::isNotBlank)
        ?: humanize(item.sourceKind)

private fun formatPreciseTime(epochMs: Long): String =
    SimpleDateFormat(
        "d MMM, HH:mm:ss",
        Locale.getDefault()
    ).format(Date(epochMs))

private fun humanize(value: String): String =
    value.lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }

private fun TransactionCategory.prettyName(): String =
    name.lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }
