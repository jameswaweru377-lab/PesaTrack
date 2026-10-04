package com.kedevjimmy.pesatrack

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityNavigationSmokeTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase(DATABASE_NAME)
        preferences().edit().clear().commit()
    }

    @After
    fun tearDown() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase(DATABASE_NAME)
        preferences().edit().clear().commit()
    }

    @Test
    fun freshStateShowsOnboarding() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithText("Get started")
                .assertExists()
        }
    }

    @Test
    fun completedSetupStartsOnHome() {
        preferences()
            .edit()
            .putBoolean(SETUP_COMPLETE_KEY, true)
            .commit()

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitUntil(timeoutMillis = 10_000L) {
                composeRule
                    .onAllNodesWithText(
                        "Available after commitments"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithText(
                    "Available after commitments"
                )
                .assertExists()
        }
    }

    private fun preferences() =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    private companion object {
        const val DATABASE_NAME = "pesatrack.db"
        const val PREFS_NAME = "pesatrack_preferences"
        const val SETUP_COMPLETE_KEY = "setup_complete"
    }
}
