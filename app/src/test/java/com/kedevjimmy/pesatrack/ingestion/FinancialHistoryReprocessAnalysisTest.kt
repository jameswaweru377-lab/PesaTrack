package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.data.local.DebtAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.DebtBalanceQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialHistoryReprocessAnalysisTest {
    @Test
    fun windowIsClampedToOneThroughThreeMonths() {
        val now =
            1_800_000_000_000L

        assertEquals(
            FinancialHistoryReprocessAnalysis
                .windowStartEpochMs(
                    nowEpochMs = now,
                    months = 1
                ),
            FinancialHistoryReprocessAnalysis
                .windowStartEpochMs(
                    nowEpochMs = now,
                    months = 0
                )
        )

        assertEquals(
            FinancialHistoryReprocessAnalysis
                .windowStartEpochMs(
                    nowEpochMs = now,
                    months = 3
                ),
            FinancialHistoryReprocessAnalysis
                .windowStartEpochMs(
                    nowEpochMs = now,
                    months = 99
                )
        )
    }

    @Test
    fun reportsOnlyDebtStateThatActuallyChanged() {
        val before =
            listOf(
                debt(
                    id = "mshwari",
                    name = "M-Shwari Loan",
                    outstandingMinor =
                        100_000L,
                    observedAt = 10L
                ),
                debt(
                    id = "fuliza",
                    name = "Fuliza",
                    outstandingMinor =
                        20_000L,
                    observedAt = 20L
                )
            )
        val after =
            listOf(
                debt(
                    id = "mshwari",
                    name = "M-Shwari Loan",
                    outstandingMinor = 0L,
                    observedAt = 30L
                ),
                debt(
                    id = "fuliza",
                    name = "Fuliza",
                    outstandingMinor =
                        20_000L,
                    observedAt = 20L
                )
            )

        val changes =
            FinancialHistoryReprocessAnalysis
                .debtBalanceChanges(
                    before = before,
                    after = after
                )

        assertEquals(1, changes.size)
        assertEquals(
            "M-Shwari Loan",
            changes.single().name
        )
        assertEquals(
            100_000L,
            changes.single()
                .beforeOutstandingMinor
        )
        assertEquals(
            0L,
            changes.single()
                .afterOutstandingMinor
        )
        assertTrue(
            changes.single()
                .observedAfter
        )
    }

    @Test
    fun newlyDiscoveredDebtStateIsReported() {
        val changes =
            FinancialHistoryReprocessAnalysis
                .debtBalanceChanges(
                    before = emptyList(),
                    after =
                        listOf(
                            debt(
                                id = "new",
                                name =
                                    "New Loan",
                                outstandingMinor =
                                    25_000L,
                                observedAt = 50L
                            )
                        )
                )

        assertEquals(1, changes.size)
        assertEquals(
            null,
            changes.single()
                .beforeOutstandingMinor
        )
        assertEquals(
            25_000L,
            changes.single()
                .afterOutstandingMinor
        )
    }

    private fun debt(
        id: String,
        name: String,
        outstandingMinor: Long,
        observedAt: Long?
    ) =
        DebtAccountSnapshot(
            id = id,
            providerKey = id,
            name = name,
            outstandingMinor =
                outstandingMinor,
            balanceQuality =
                if (observedAt == null) {
                    DebtBalanceQuality
                        .ESTIMATED
                } else {
                    DebtBalanceQuality
                        .OBSERVED
                },
            observedAtEpochMs =
                observedAt,
            availableCreditMinor = null,
            availableCreditObservedAtEpochMs =
                null
        )
}
