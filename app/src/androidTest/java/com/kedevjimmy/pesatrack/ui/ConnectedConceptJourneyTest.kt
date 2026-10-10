package com.kedevjimmy.pesatrack.ui

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.ui.screens.*
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectedConceptJourneyTest {
    @get:Rule val compose=createComposeRule()
    private lateinit var context:Context
    @Before fun setup(){context=ApplicationProvider.getApplicationContext();PesaTrackDatabase.resetForTests();context.deleteDatabase("pesatrack.db");context.getSharedPreferences("pesatrack_preferences",0).edit().remove("profile_name").commit()}
    @After fun cleanup(){compose.runOnIdle{};PesaTrackDatabase.resetForTests();context.deleteDatabase("pesatrack.db")}
    @Test fun goalCanBeCreatedEditedAndRemovedWithoutFabricatingMoney() {
        compose.setContent {PesaTrackTheme {SavingGoalsScreen(PaddingValues(),{}, {})}}
        compose.onNodeWithText("Create Goal").performScrollTo().performClick()
        compose.onNodeWithText("Goal name").performTextInput("Emergency fund")
        compose.onNodeWithText("Monthly saving target (KES)").performTextInput("5000")
        compose.onNodeWithText("Overall goal target (KES)").performScrollTo().performTextInput("50000")
        compose.onNodeWithText("Where will you save this money?").performScrollTo().performTextInput("Savings provider")
        compose.onNodeWithText("Save goal").performClick()
        compose.waitUntil(10000){
            val created=RuleStore.get(context).listRules(true).firstOrNull{it.name=="Emergency fund"}
            created != null &&
                SavingGoalMetadataStore.get(context).get(created.id)?.totalTargetMinor == 5000000L
        }
        val created=RuleStore.get(context).listRules(true).first{it.name=="Emergency fund"}
        assertEquals(5000000L,SavingGoalMetadataStore.get(context).get(created.id)?.totalTargetMinor)
        assertEquals(0,TransactionStore.get(context).snapshot().transactionCount)
        compose.waitForIdle()
        // Persistence completes before the screen's IO snapshot is applied.
        // Wait for the actual goal card, rather than scrolling a stale empty list.
        compose.waitUntil(10000) {
            try {
                compose.onNodeWithTag("saving-goals-list")
                    .performScrollToNode(hasText("Emergency fund"))
                true
            } catch (_: AssertionError) {
                false
            }
        }
        compose.waitUntil(10000) {
            compose.onAllNodesWithText(
                "Emergency fund"
            ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithText(
            "Emergency fund"
        ).performClick()
        compose.onNodeWithText(
            "Edit goal"
        ).performClick()
        compose.onNodeWithText("Remove goal").performScrollTo().performClick()
        compose.onNodeWithText("Remove this goal?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertTrue(RuleStore.get(context).listRules(true).any{it.name=="Emergency fund"})
        compose.onNodeWithText("Remove goal").performScrollTo().performClick()
        compose.onNodeWithText("Remove",useUnmergedTree=true).performClick()
        compose.waitUntil(10000){RuleStore.get(context).listRules(true).none{it.name=="Emergency fund"}}
    }
    @Test fun profileNameIsEditableAndPersisted() {
        compose.setContent {PesaTrackTheme(darkTheme=true) {ProfileScreen(PaddingValues(),{}, {})}}
        compose.onNodeWithText("Personal details").performScrollTo().performClick()
        compose.onNodeWithText("Your name").performTextInput("Test user")
        compose.onNodeWithText("Save details").performClick()
        compose.waitUntil(5000){context.getSharedPreferences("pesatrack_preferences",0).getString("profile_name","")=="Test user"}
    }
    @Test fun reportsSwitchBetweenPeriodsWithoutAStuckLoadingState() {
        compose.setContent {PesaTrackTheme {ReportsInsightsScreen(PaddingValues(),{})}}
        compose.waitUntil(10000){compose.onAllNodesWithText("No recorded spending in this period yet.").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Year").performClick()
        compose.onNodeWithText("Week").performClick()
        compose.onNodeWithText("Month").performClick()
        compose.onNodeWithText("Reports & insights").assertIsDisplayed()
        compose.onNodeWithText("Couldn't load this page. Your saved data is unchanged.").assertDoesNotExist()
    }
}
