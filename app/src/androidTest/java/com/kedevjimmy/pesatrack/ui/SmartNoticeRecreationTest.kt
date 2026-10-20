package com.kedevjimmy.pesatrack.ui

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.kedevjimmy.pesatrack.MainActivity
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.planning.SmartNoticeCoordinator
import com.kedevjimmy.pesatrack.security.AppLockManager
import org.junit.*

/** Protect the pending destination across recreation while the existing app lock is shown. */
class SmartNoticeRecreationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var context: Context
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("review_attention_reminders",Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("pesatrack_preferences",Context.MODE_PRIVATE).edit().putBoolean("setup_complete",true).commit()
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
        AppLockManager(context).setPin("1234")
    }
    @After fun cleanup() {
        AppLockManager(context).disable("1234")
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
    }
    @Test fun pendingDiscoveryRequestSurvivesRecreationBeforeUnlock() {
        val intent = Intent(context,MainActivity::class.java)
            .putExtra(SmartNoticeCoordinator.OPEN_KEY,"discovery")
            .putExtra(SmartNoticeCoordinator.OPEN_ACTION,"review")
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            compose.waitUntil(10_000) { compose.onAllNodesWithText("PIN").fetchSemanticsNodes().isNotEmpty() }
            scenario.recreate()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("PIN").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("PIN").performTextInput("1234")
            compose.onNodeWithText("Unlock").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Record money lent").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Record money lent").assertIsDisplayed()
            compose.onNodeWithText("Find payments").assertIsDisplayed()
        }
    }
}
