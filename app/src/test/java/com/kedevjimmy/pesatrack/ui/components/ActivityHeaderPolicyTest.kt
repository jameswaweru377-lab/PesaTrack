package com.kedevjimmy.pesatrack.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityHeaderPolicyTest {
    @Test fun onlyHomeKeepsTheCenteredBrandedHeader() {
        assertFalse(showBrandedTopHeader(MainTab.ACTIVITY, true))
        assertTrue(showBrandedTopHeader(MainTab.HOME, true))
        assertFalse(showBrandedTopHeader(MainTab.PLAN, true))
        assertFalse(showBrandedTopHeader(MainTab.ACCOUNTS, true))
        assertFalse(showBrandedTopHeader(MainTab.HOME, false))
        assertFalse(showBrandedTopHeader(null, true))
    }

    @Test fun planAndAccountsGetPinnedCompactTabHeadings() {
        assertTrue(showCompactTopHeader(MainTab.PLAN, true))
        assertTrue(showCompactTopHeader(MainTab.ACCOUNTS, true))
        assertFalse(showCompactTopHeader(MainTab.HOME, true))
        assertFalse(showCompactTopHeader(MainTab.ACTIVITY, true))
        assertFalse(showCompactTopHeader(MainTab.PLAN, false))
        assertFalse(showCompactTopHeader(null, true))
    }
}
