package com.kedevjimmy.pesatrack.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityHeaderPolicyTest {
    @Test fun activityOwnsItsCompactHeaderWhileOtherMainTabsKeepBranding() {
        assertFalse(showBrandedTopHeader(MainTab.ACTIVITY, true))
        assertTrue(showBrandedTopHeader(MainTab.HOME, true))
        assertTrue(showBrandedTopHeader(MainTab.PLAN, true))
        assertFalse(showBrandedTopHeader(MainTab.HOME, false))
        assertFalse(showBrandedTopHeader(null, true))
    }
}
