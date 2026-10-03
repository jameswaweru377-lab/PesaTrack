package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialHistoryReprocessFreshnessPolicyTest {
    @Test
    fun noSuccessfulReplayIsRecommended() {
        assertTrue(
            FinancialHistoryReprocessFreshnessPolicy
                .isRecommended(
                    lastSuccessfulAtEpochMs = null,
                    appLastUpdateAtEpochMs =
                        2_000L
                )
        )
    }

    @Test
    fun appUpdateAfterReplayIsRecommended() {
        assertTrue(
            FinancialHistoryReprocessFreshnessPolicy
                .isRecommended(
                    lastSuccessfulAtEpochMs =
                        1_000L,
                    appLastUpdateAtEpochMs =
                        2_000L
                )
        )
    }

    @Test
    fun replayAfterCurrentInstallIsUpToDate() {
        assertFalse(
            FinancialHistoryReprocessFreshnessPolicy
                .isRecommended(
                    lastSuccessfulAtEpochMs =
                        3_000L,
                    appLastUpdateAtEpochMs =
                        2_000L
                )
        )
    }

    @Test
    fun replayAtSameTimestampIsUpToDate() {
        assertFalse(
            FinancialHistoryReprocessFreshnessPolicy
                .isRecommended(
                    lastSuccessfulAtEpochMs =
                        2_000L,
                    appLastUpdateAtEpochMs =
                        2_000L
                )
        )
    }
}
