package com.kedevjimmy.pesatrack.ui

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.planning.SmartNoticePreferences
import com.kedevjimmy.pesatrack.ui.screens.SmartNoticeReviewScreen
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import androidx.compose.foundation.layout.PaddingValues
import org.junit.*
import org.junit.Assert.*

/** Synthetic UI coverage compiled in normal CI; device execution remains paused. */
class SmartNoticeJourneyTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("review_attention_reminders",Context.MODE_PRIVATE).edit().clear().commit()
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
    }
    @After fun cleanup() {
        compose.runOnIdle {}
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
    }
    @Test fun stalePaymentShowsResolvedStateWithoutFinancialDialog() {
        compose.setContent { PesaTrackTheme {
            SmartNoticeReviewScreen(PaddingValues(),"payment:deleted","entry",{}, {})
        } }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("smart-notice-resolved").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("This item no longer needs this review.").assertIsDisplayed()
        compose.onNodeWithText("Record money lent").assertDoesNotExist()
    }
    @Test fun cleanSlateDiscoveryOpensManualEntryAndCompletesTip() {
        compose.setContent { PesaTrackTheme {
            SmartNoticeReviewScreen(PaddingValues(),"discovery","review",{}, {})
        } }
        compose.waitUntil(5000) { compose.onAllNodesWithText("Record money lent").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Record money lent").assertIsDisplayed()
        compose.onNodeWithText("Find in transactions").assertIsDisplayed()
        assertFalse(SmartNoticePreferences(context).discoveryPending())
    }
}
