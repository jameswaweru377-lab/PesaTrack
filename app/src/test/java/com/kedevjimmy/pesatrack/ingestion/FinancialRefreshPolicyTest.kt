package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialRefreshPolicyTest {
    private val now = 1_700_000_000_000L

    @Test fun firstLaunchRefreshesEvenWithoutImportedSms() {
        assertTrue(FinancialRefreshPolicy.shouldRefresh(now, 0L, 22L, -1L))
    }

    @Test fun newApkVersionAlwaysRefreshes() {
        assertTrue(FinancialRefreshPolicy.shouldRefresh(now, now - 10_000L, 23L, 22L))
    }

    @Test fun foregroundReturnRefreshesAfterFiveMinutes() {
        assertTrue(FinancialRefreshPolicy.shouldRefresh(now, now - 300_000L, 23L, 23L))
    }

    @Test fun quickForegroundReturnKeepsExistingScreenCaches() {
        assertFalse(FinancialRefreshPolicy.shouldRefresh(now, now - 60_000L, 23L, 23L))
    }

    @Test fun reversedPhoneClockCannotPreventRefreshForever() {
        assertTrue(FinancialRefreshPolicy.shouldRefresh(now, now + 5_000L, 23L, 23L))
    }
}
