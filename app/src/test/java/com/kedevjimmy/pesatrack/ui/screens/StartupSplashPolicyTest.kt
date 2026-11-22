package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupSplashPolicyTest {
    @Test
    fun returningUserReceivesBriefDailyBrandSplash() {
        assertTrue(StartupSplashPolicy.isVisibleAtColdStart(setupComplete = true))
        assertEquals(700L, StartupSplashPolicy.durationMs(setupComplete = true))
    }

    @Test
    fun firstRunKeepsItsExistingCafeSplashTiming() {
        assertTrue(StartupSplashPolicy.isVisibleAtColdStart(setupComplete = false))
        assertEquals(650L, StartupSplashPolicy.durationMs(setupComplete = false))
    }
}
