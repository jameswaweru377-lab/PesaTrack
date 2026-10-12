package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenRefreshPolicyTest {
    @Test fun unchangedRevisionDoesNotReloadOnNavigation() {
        assertFalse(ScreenRefreshPolicy.needsRead(15L, 15L))
        assertFalse(ScreenRefreshPolicy.needsRead(0L, 0L))
    }

    @Test fun newRevisionNeverUsesOldPresentationAsFresh() {
        assertTrue(ScreenRefreshPolicy.needsRead(15L, 16L))
        assertTrue(ScreenRefreshPolicy.needsRead(null, 15L))
        assertTrue(ScreenRefreshPolicy.needsRead(-1L, 0L))
    }

    @Test fun explicitRetryAlwaysReloads() {
        assertTrue(ScreenRefreshPolicy.needsRead(15L, 15L, retryOrRefresh = 1))
        assertTrue(ScreenRefreshPolicy.needsRead(15L, 16L, retryOrRefresh = 2))
    }
}
