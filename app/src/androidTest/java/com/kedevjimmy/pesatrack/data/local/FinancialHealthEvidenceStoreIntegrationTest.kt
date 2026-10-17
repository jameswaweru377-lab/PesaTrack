package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinancialHealthEvidenceStoreIntegrationTest {
    private lateinit var context: Context
    private lateinit var store: FinancialHealthEvidenceStore

    @Before fun setup() {
        context=ApplicationProvider.getApplicationContext()
        val prefs=context.getSharedPreferences("pesatrack_preferences",Context.MODE_PRIVATE)
        prefs.edit()
            .remove("financial_health_v2_confirmed_at")
            .remove("financial_health_v2_emergency_minor")
            .remove("financial_health_v2_essentials_minor")
            .remove("financial_health_v2_required_debt_minor").commit()
        store=FinancialHealthEvidenceStore(context)
    }

    @After fun teardown() {
        val prefs=context.getSharedPreferences("pesatrack_preferences",Context.MODE_PRIVATE)
        prefs.edit()
            .remove("financial_health_v2_confirmed_at")
            .remove("financial_health_v2_emergency_minor")
            .remove("financial_health_v2_essentials_minor")
            .remove("financial_health_v2_required_debt_minor").commit()
    }

    @Test fun userEnteredAmountsSurviveReloadButExpireWithoutRenewingTheClock() {
        store.save("15000.50","20000","2500",nowMs=10_000_000_000L)
        val restored=FinancialHealthEvidenceStore(context).snapshot(10_000_000_000L)
        assertEquals(1_500_050L,restored.emergencyReserveMinor)
        assertEquals(2_000_000L,restored.essentialMonthlyMinor)
        assertEquals(250_000L,restored.requiredDebtPaymentsMinor)
        assertFalse(restored.requiresRefresh)

        val expired=store.snapshot(10_000_000_000L + 31L*24L*60L*60L*1000L)
        assertTrue(expired.requiresRefresh)
        assertNull(expired.emergencyReserveMinor)
        assertNull(expired.requiredDebtPaymentsMinor)
    }

    @Test fun invalidAmountsDoNotOverwritePreviouslySavedAmounts() {
        store.save("5000","10000","",nowMs=10_000_000_000L)
        assertNotNull(runCatching {
            store.save("7000","not-money","",nowMs=10_000_000_001L)
        }.exceptionOrNull())
        val old=store.snapshot(10_000_000_002L)
        assertEquals(500_000L,old.emergencyReserveMinor)
        assertEquals(1_000_000L,old.essentialMonthlyMinor)
        assertNull(old.requiredDebtPaymentsMinor)
    }

    @Test fun missingRequiredEssentialsAndNegativeValuesAreRejected() {
        assertTrue(runCatching {store.save("5000","","")}.isFailure)
        assertTrue(runCatching {store.save("-1","2000","")}.isFailure)
        assertTrue(runCatching {store.save("1.234","2000","")}.isFailure)
    }

    @Test fun explicitZeroEmergencyReserveIsNotMissing() {
        store.save("0","10000","0",nowMs=10_000_000_000L)
        val value=store.snapshot(10_000_000_000L)
        assertEquals(0L,value.emergencyReserveMinor)
        assertEquals(0L,value.requiredDebtPaymentsMinor)
    }
}
