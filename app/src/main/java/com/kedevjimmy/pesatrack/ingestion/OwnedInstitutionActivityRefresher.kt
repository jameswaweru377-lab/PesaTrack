package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore

data class OwnedInstitutionRefreshResult(
    val staleTransfersReclassified: Int,
    val pendingTransfersApplied: Int,
    val historyScanned: Int,
    val historyStateRefreshed: Int,
    val historyInserted: Int
)

class OwnedInstitutionActivityRefresher(
    context: Context
) {
    private val appContext =
        context.applicationContext
    private val understandingStore =
        FinancialUnderstandingStore.get(
            appContext
        )
    private val transactionStore =
        TransactionStore.get(
            appContext
        )
    private val coordinator =
        FinancialMessageIngestionCoordinator
            .get(appContext)

    fun refresh(
        selectedEntityKeys: Set<String>,
        nowEpochMs: Long =
            System.currentTimeMillis()
    ): OwnedInstitutionRefreshResult {
        val ownedIds =
            understandingStore
                .confirmedAccountIds() +
                AccountStore
                    .DEFAULT_MPESA_ID +
                AccountStore
                    .DEFAULT_CASH_ID

        val staleReclassified =
            transactionStore
                .reconcileAutoTransfersWithOwnedAccounts(
                    ownedIds
                )

        var appliedPending = 0
        selectedEntityKeys.forEach {
                entityKey ->
            appliedPending +=
                coordinator
                    .reconcilePendingTransfers(
                        entityKey =
                            entityKey,
                        rebuildPayCycles =
                            false
                    )
        }

        val historyResult =
            if (
                AutomaticHistoryRepair
                    .hasSmsAccess(
                        appContext
                    )
            ) {
                AutomaticHistoryRepair
                    .startEpochMs(
                        appContext,
                        nowEpochMs
                    )
                    ?.let { since ->
                        HistoricalSmsImporter(
                            appContext
                        ).importSince(
                            sinceEpochMs = since,
                            rebuildPayCycles =
                                false,
                            mode =
                                HistoricalImportMode
                                    .REPROCESS
                        )
                    }
            } else {
                null
            }

        // Reprocessing can surface an old pending transfer after the first
        // reconciliation pass, so make one final cheap pass.
        selectedEntityKeys.forEach {
                entityKey ->
            appliedPending +=
                coordinator
                    .reconcilePendingTransfers(
                        entityKey =
                            entityKey,
                        rebuildPayCycles =
                            false
                    )
        }

        PayCycleStore.get(
            appContext
        ).rebuild()
        LedgerUpdateBus
            .notifyChanged()

        return OwnedInstitutionRefreshResult(
            staleTransfersReclassified =
                staleReclassified,
            pendingTransfersApplied =
                appliedPending,
            historyScanned =
                historyResult?.scanned
                    ?: 0,
            historyStateRefreshed =
                historyResult
                    ?.stateRefreshed
                    ?: 0,
            historyInserted =
                historyResult?.inserted
                    ?: 0
        )
    }
}
