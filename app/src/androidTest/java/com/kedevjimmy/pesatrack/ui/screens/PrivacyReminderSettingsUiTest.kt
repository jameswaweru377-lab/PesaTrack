package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.EntityEnrichmentPreferences
import com.kedevjimmy.pesatrack.planning.BillSchedules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrivacyReminderSettingsUiTest {
    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before fun prepare() = clearPreferences()
    @After fun cleanup() = clearPreferences()

    private fun clearPreferences() {
        context.getSharedPreferences("bill_schedules", Context.MODE_PRIVATE).edit().clear().commit()
        EntityEnrichmentPreferences.get(context).setOnlineLookupEnabled(false)
    }

    @Test fun brandedSwitchHasOneAccessibleToggleAndWorksFromTheWholeRow() {
        compose.setContent {
            MaterialTheme {
                var checked by remember { mutableStateOf(false) }
                ChoiceSwitch("Sample reminder", "Optional reminder", checked) { checked = it }
            }
        }
        compose.onNode(isToggleable()).assertIsOff().performClick().assertIsOn()
    }

    @Test fun disabledRemindersKeepOffsetAndUseSingularDay() {
        BillSchedules(context).setGlobalReminderLeadDays(3, enable = false)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                MaterialTheme { RemindersPage(PaddingValues(), {}) }
            }
        }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Remind me before a bill"))
        compose.onNodeWithText("1 day").assertIsNotEnabled()
        compose.onNodeWithText("3 days").assertIsNotEnabled().assertIsSelected()
        compose.onNodeWithText("7 days").assertIsNotEnabled()
        compose.onNodeWithText("1 days").assertDoesNotExist()
        compose.onNodeWithText("7 days").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            assertFalse(BillSchedules(context).remindersEnabled())
            assertEquals(3, BillSchedules(context).globalReminderLeadDays())
        }
    }

    @Test fun privacyDisclosesSharingBeforeOptInAndExpandsDetails() {
        compose.setContent { MaterialTheme { PrivacyPage(PaddingValues(), {}, {}, {}) } }
        compose.onNodeWithText("Optional. Sends only a cleaned business name, never your messages or amounts.")
            .performScrollTo().assertIsDisplayed()
        compose.onNode(isToggleable()).assertIsOff()
        compose.onNodeWithText("Voice entry").assertDoesNotExist()
        compose.onNodeWithText("How my data is handled").performScrollTo().performClick()
        compose.onNodeWithText("Voice entry uses your phone's speech service", substring = true)
            .performScrollTo().assertIsDisplayed()
    }
}
