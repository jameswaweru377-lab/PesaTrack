package com.kedevjimmy.pesatrack.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kedevjimmy.pesatrack.ui.components.AppScaffold
import com.kedevjimmy.pesatrack.ui.components.MainTab
import com.kedevjimmy.pesatrack.ui.screens.ActivityScreen
import com.kedevjimmy.pesatrack.ui.screens.GoalsScreen
import com.kedevjimmy.pesatrack.ui.screens.HomeScreen
import com.kedevjimmy.pesatrack.ui.screens.OnboardingScreen
import com.kedevjimmy.pesatrack.ui.screens.PlanScreen

@Composable
fun PesaTrackApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "onboarding"
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onContinue = {
                    navController.navigate(MainTab.HOME.route) {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        MainTab.entries.forEach { tab ->
            composable(tab.route) {
                AppScaffold(
                    currentTab = tab,
                    onTabSelected = { destination ->
                        navController.navigate(destination.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                ) { padding ->
                    when (tab) {
                        MainTab.HOME -> HomeScreen(padding)
                        MainTab.ACTIVITY -> ActivityScreen(padding)
                        MainTab.PLAN -> PlanScreen(padding)
                        MainTab.GOALS -> GoalsScreen(padding)
                    }
                }
            }
        }
    }
}
