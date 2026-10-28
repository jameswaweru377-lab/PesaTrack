package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.ui.components.AppScaffold
import com.kedevjimmy.pesatrack.ui.components.MainTab
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatementWorkspaceNavigationUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun statementsUsesOneGlobalFourTabBar() {
        compose.setContent { PesaTrackTheme {
            AppScaffold(currentTab = MainTab.MORE, onTabSelected = {}, useHomeHeader = true) {
                padding -> Box(Modifier.padding(padding)) { Text("Statement workspace") }
            }
        } }
        listOf("Home", "Activity", "Plan", "More").forEach { tab ->
            compose.onAllNodesWithText(tab).assertCountEquals(1)
        }
        compose.onNodeWithText("Accounts").assertDoesNotExist()
    }
}
