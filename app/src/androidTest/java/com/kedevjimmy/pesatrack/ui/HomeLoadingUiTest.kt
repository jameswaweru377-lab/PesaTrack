package com.kedevjimmy.pesatrack.ui

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.ui.screens.*
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeLoadingUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context

    @Before fun freshDatabase() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @Test fun failedLoadShowsRecoveryAndNavigationWithoutInventingZeroBalances() {
        var accountsOpened = false
        compose.setContent {
            PesaTrackTheme(darkTheme = true) {
                HomeScreen(PaddingValues(0.dp), onAccountsClick = { accountsOpened = true },
                    homeStateLoader = { error("test database failure") })
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Your overview is unavailable").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Try again").assertIsEnabled()
        compose.onNodeWithText("Open accounts").performClick()
        compose.runOnIdle { assertTrue(accountsOpened) }
        compose.onNodeWithText("Waiting for salary").assertDoesNotExist()
    }

    @Test fun slowLoadOffersNavigationAndEventuallyShowsTheCompletedOverview() {
        val result = CompletableDeferred<HomeLoadedState>()
        var activityOpened = false
        compose.setContent {
            PesaTrackTheme(darkTheme = false) {
                HomeScreen(PaddingValues(0.dp), onActivityClick = { activityOpened = true },
                    homeStateLoader = { result.await() }, loadingTimeoutMs = 200L)
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Your overview is unavailable").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Open activity").performClick()
        compose.runOnIdle { assertTrue(activityOpened) }
        compose.onNodeWithText("Try again").assertIsNotEnabled()
        result.complete(HomeStateReader.read(context))
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Waiting for salary").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Your overview is unavailable").assertDoesNotExist()
    }

    @Test fun firstSalaryCanBeAddedFromHomeAndAppearsInThePersistedCycle() {
        compose.setContent {
            PesaTrackTheme(darkTheme = true) { HomeScreen(PaddingValues(0.dp)) }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Waiting for salary").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Add salary").performScrollTo().performClick()
        compose.onNodeWithText("Salary amount").performTextInput("42000")
        compose.onAllNodes(hasText("Add salary") and hasClickAction()).onLast().performClick()
        compose.waitUntil(5_000) { PayCycleStore.get(context).currentCycle() != null }
        assertEquals(4_200_000L, PayCycleStore.get(context).readSnapshot().cycle?.openingIncomeMinor)
        assertNull(PayCycleStore.get(context).readSnapshot().pendingSalary)
    }

    @Test fun homeDisplaysNetSpendingWithoutSubtractingTheRefundAgain() {
        val base = HomeStateReader.read(context)
        val state = base.copy(activitySnapshot = base.activitySnapshot.copy(
            summary = base.activitySnapshot.summary.copy(
                spentMinor = 100_000L, feesMinor = 1_000L, refundMinor = 40_000L
            )
        ))
        compose.setContent {
            PesaTrackTheme(darkTheme = false) {
                HomeScreen(PaddingValues(0.dp), homeStateLoader = { state })
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Waiting for salary").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("KES 1,010 this month").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("KES 610 this month").assertDoesNotExist()
    }
}
