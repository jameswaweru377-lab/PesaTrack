package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot
import com.kedevjimmy.pesatrack.data.local.ActivitySummary
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** Does not connect to a financial database: snapshot equality guards only. */
class ActivityCoreWarmupCacheTest {
    private fun core() = ActivityCoreData(
        monthBuckets = emptyList(),
        snapshot = ActivityLedgerSnapshot(
            transactions = emptyList(),
            summary = ActivitySummary(0, 0, 0, 0, 0, 0)
        ),
        previousSnapshot = ActivityLedgerSnapshot(
            transactions = emptyList(),
            summary = ActivitySummary(0, 0, 0, 0, 0, 0)
        ),
        accountsById = emptyMap(),
        confirmedAccountIds = emptySet()
    )

    @Test fun cachedCoreIsReusableOnSameMonthAndRevisionOnly() {
        val cache = ActivityScreenCache()
        val original = core()
        cache.saveCore(42, 1_790_000_000_000L, original)
        assertSame(original, cache.coreFor(42, 1_790_000_000_000L))
        assertNull(cache.coreFor(43, 1_790_000_000_000L))
        assertNull(cache.coreFor(42, 1_790_100_000_000L))
    }

    @Test fun newerCompletedSnapshotReplacesOldPreloadWithoutLeakingOldMonth() {
        val cache = ActivityScreenCache()
        cache.saveCore(42, 1_790_000_000_000L, core())
        val latest = core()
        cache.saveCore(43, 1_790_100_000_000L, latest)
        assertSame(latest, cache.coreFor(43, 1_790_100_000_000L))
        assertNull(cache.coreFor(42, 1_790_000_000_000L))
        assertNull(cache.coreFor(43, 1_790_000_000_000L))
    }
}
