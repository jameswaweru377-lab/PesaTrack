package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot

/** The supplementary filters stay reachable without filling the first Activity viewport. */
internal val brownActivitySecondaryFilters = brownActivityAdvancedFilters + listOf(
    BrownActivityFilter("Owed to me", "Owed to me"),
    BrownActivityFilter("Repayments", "Repayments")
)

/** Activity owns financial state; this view model holds only presentation inputs. */
internal data class BrownActivityControlView(
    val periodLabel: String,
    val payCycleMode: Boolean,
    val snapshot: ActivityLedgerSnapshot,
    val balance: ActivityRecordedBalance?,
    val searchOpen: Boolean,
    val searchQuery: String,
    val extraFiltersOpen: Boolean,
    val selectedFilter: String
)

/** Callbacks deliberately delegate to the existing ledger/review owner, ActivityScreen. */
internal data class BrownActivityControlActions(
    val onMonthMode: () -> Unit,
    val onChooseMonth: () -> Unit,
    val onCycle: () -> Unit,
    val onToggleSearch: () -> Unit,
    val onQuery: (String) -> Unit,
    val onToggleFilters: () -> Unit,
    val onHeaderHeightChanged: (Int) -> Unit,
    val onShowAll: () -> Unit,
    val onSelectFilter: (String) -> Unit,
    val onToolsChanged: () -> Unit,
    val onPaycheckClick: () -> Unit,
    val onStatementsClick: (() -> Unit)?
)

/** A focused LazyList section preserves separate keys and the review scroll index. */
@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.brownActivityControlItems(
    view: BrownActivityControlView,
    actions: BrownActivityControlActions
) {
    stickyHeader(key = "activity-ledger-header") {
        BrownActivityHeader(
            month = view.periodLabel,
            payCycleMode = view.payCycleMode,
            searchOpen = view.searchOpen,
            searchQuery = view.searchQuery,
            filtersActive = view.extraFiltersOpen ||
                brownActivityPrimaryFilters.none { it.key == view.selectedFilter },
            onMonthMode = actions.onMonthMode,
            onChooseMonth = actions.onChooseMonth,
            onCycle = actions.onCycle,
            onToggleSearch = actions.onToggleSearch,
            onQuery = actions.onQuery,
            onToggleFilters = actions.onToggleFilters,
            onHeaderHeightChanged = actions.onHeaderHeightChanged
        )
    }
    item(key = "activity-period-overview") {
        Box(Modifier.padding(bottom = 8.dp)) {
            BrownActivityMonthlyHero(view.snapshot, view.payCycleMode, view.balance)
        }
    }
    item(key = "activity-transactions-heading") {
        BrownActivityTransactionsHeading(actions.onShowAll)
    }
    item(key = "premium-filters") {
        Box(Modifier.padding(bottom = 4.dp)) {
            BrownActivitySegmentedFilters(view.selectedFilter, actions.onSelectFilter)
        }
    }
    item(key = "activity-flow-ticker") {
        BrownActivityFlowTicker()
    }
    if (view.extraFiltersOpen) item(key = "activity-tools") {
        ActivityToolsDisclosure(
            onChanged = actions.onToolsChanged,
            onPaycheckClick = actions.onPaycheckClick,
            onStatementsClick = actions.onStatementsClick
        )
    }
    if (view.extraFiltersOpen) item(key = "additional-filters") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            brownActivitySecondaryFilters.forEach { filter ->
                item(key = filter.key) {
                    FilterChip(
                        selected = view.selectedFilter == filter.key,
                        onClick = { actions.onSelectFilter(filter.key) },
                        label = { Text(filter.label) }
                    )
                }
            }
        }
    }
}
