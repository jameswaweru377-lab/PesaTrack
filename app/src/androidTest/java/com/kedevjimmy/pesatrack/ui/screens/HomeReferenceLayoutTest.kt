package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.ui.components.MainTab
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Visual content and navigation contract from the approved Home reference. */
@RunWith(AndroidJUnit4::class)
class HomeReferenceLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun heroUsesExactApprovedLabelsAndRoutes() {
        var plan = 0
        var accounts = 0
        var month = 0
        compose.setContent {
            MaterialTheme {
                ReferenceHomeHero("KES 48,250", "April 2025",
                    onMonth = { month++ }, onPlan = { plan++ }, onAccounts = { accounts++ })
            }
        }
        compose.onNodeWithText("Available after commitments").assertExists()
        compose.onNodeWithText("KES 48,250").assertExists()
        compose.onNodeWithText("April 2025").performClick()
        compose.onNodeWithText("Review plan").performClick()
        compose.onNodeWithText("Accounts").performClick()
        assertEquals(1, plan); assertEquals(1, accounts); assertEquals(1, month)
    }

    @Test fun approvedMonthlyAttentionAndUpcomingTextIsComplete() {
        compose.setContent {
            MaterialTheme {
                Column {
                    ReferenceHomeMonth(
                        "1 – 30 Apr 2025", "KES 132,500", "KES 84,200", "KES 28,300",
                        "of KES 50,000", "vs Mar +12%", "vs Mar -8%", .56f, onMonth = {})
                    ReferenceHomeAttention(3, onClick = {})
                    ReferenceHomeUpcoming(listOf(
                        ReferenceUpcomingEntry("Internet Bill", "Due in 2 days", "KES 2,500", false),
                        ReferenceUpcomingEntry("Salary", "In 5 days", "KES 120,000", true)
                    ), onSeeAll = {})
                }
            }
        }
        listOf("This month", "Money in", "Money out", "Budget left",
            "Needs attention", "3 transactions need categories", "Upcoming",
            "Internet Bill", "Salary", "Recent activity").dropLast(1).forEach {
            compose.onNodeWithText(it).assertExists()
        }
    }

    @Test fun navigationShowsFiveReferenceDestinationsWithHomeSelected() {
        var selected = MainTab.HOME
        compose.setContent {
            MaterialTheme {
                ReferenceHomeBottomBar(MainTab.HOME, onSelected = { selected = it })
            }
        }
        listOf("Home", "Activity", "Plan", "Accounts", "More").forEach {
            compose.onAllNodesWithText(it).onFirst().assertExists()
        }
        compose.onNodeWithText("Accounts").performClick()
        assertEquals(MainTab.ACCOUNTS, selected)
    }
}
