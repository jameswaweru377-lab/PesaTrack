package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.PayCycleSnapshot
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.*

class PlanCacheUiTest {
    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Before fun prepare() { PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db") }
    @After fun cleanup() { PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db") }

    @Test fun failedRefreshRetainsCurrentRevisionPlanAndOffersRetry() {
        val cache = cachedPlan(LedgerUpdateBus.state.value.revision)
        PesaTrackDatabase.get(context).writableDatabase.execSQL("DROP TABLE transactions")
        compose.setContent { PesaTrackTheme { PlanScreen(PaddingValues(), stateCache = cache) } }
        waitFor("Showing your last loaded plan. Could not load your plan. Try again.")
        compose.onNodeWithText("Plan your month").assertIsDisplayed()
        compose.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test fun changedRevisionCannotDisplayOldCachedPlanAfterReadFailure() {
        val cache = cachedPlan(LedgerUpdateBus.state.value.revision - 1)
        PesaTrackDatabase.get(context).writableDatabase.execSQL("DROP TABLE transactions")
        compose.setContent { PesaTrackTheme { PlanScreen(PaddingValues(), stateCache = cache) } }
        waitFor("Could not load your plan. Try again.")
        compose.onNodeWithText("Plan your month").assertDoesNotExist()
    }

    private fun cachedPlan(revision: Long) = PlanScreenCache().apply {
        this.revision = revision
        data = PlanReferenceData(PayCycleSnapshot(null, 0, 0, 0, 0, 0, 0, 0, null, null, emptyList(), null),
            emptyList(), emptyList(), emptyMap(), emptyList(), emptyList())
    }
    private fun waitFor(text: String) { compose.waitUntil(15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
}
