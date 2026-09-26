package com.kedevjimmy.pesatrack.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kedevjimmy.pesatrack.ui.components.AppScaffold
import com.kedevjimmy.pesatrack.ui.components.MainTab
import com.kedevjimmy.pesatrack.ui.screens.ActivityScreen
import com.kedevjimmy.pesatrack.ui.screens.GoalDetailScreen
import com.kedevjimmy.pesatrack.ui.screens.GoalsScreen
import com.kedevjimmy.pesatrack.ui.screens.HomeScreen
import com.kedevjimmy.pesatrack.ui.screens.MoneySetupScreen
import com.kedevjimmy.pesatrack.ui.screens.MpesaConnectionScreen
import com.kedevjimmy.pesatrack.ui.screens.OnboardingScreen
import com.kedevjimmy.pesatrack.ui.screens.PlanScreen
import com.kedevjimmy.pesatrack.ui.screens.ProfileScreen
import com.kedevjimmy.pesatrack.ui.screens.SettingsScreen
import com.kedevjimmy.pesatrack.ui.screens.TransactionDetailScreen
import com.kedevjimmy.pesatrack.ui.theme.ThemeMode

@Composable
fun PesaTrackApp(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    setupComplete: Boolean,
    onSetupComplete: () -> Unit
) {
    val navController = rememberNavController()

    fun navigateToTab(tab: MainTab) {
        navController.navigate(tab.route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(MainTab.HOME.route) {
                saveState = true
            }
        }
    }

    fun finishSetup() {
        onSetupComplete()
        navController.navigate(MainTab.HOME.route) {
            popUpTo("onboarding") { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (setupComplete) MainTab.HOME.route else "onboarding"
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onContinue = {
                    navController.navigate("money-setup-initial")
                },
                onSkip = ::finishSetup
            )
        }

        composable("money-setup-initial") {
            MoneySetupScreen(
                scaffoldPadding = PaddingValues(),
                initialSetup = true,
                onDone = ::finishSetup
            )
        }

        composable(MainTab.HOME.route) {
            AppScaffold(
                currentTab = MainTab.HOME,
                onTabSelected = ::navigateToTab
            ) { padding ->
                HomeScreen(
                    scaffoldPadding = padding,
                    onProfileClick = { navController.navigate("profile") }
                )
            }
        }

        composable(MainTab.ACTIVITY.route) {
            AppScaffold(
                currentTab = MainTab.ACTIVITY,
                onTabSelected = ::navigateToTab
            ) { padding ->
                ActivityScreen(
                    scaffoldPadding = padding,
                    onTransactionClick = { id ->
                        navController.navigate("transaction-detail/$id")
                    }
                )
            }
        }

        composable(MainTab.PLAN.route) {
            AppScaffold(
                currentTab = MainTab.PLAN,
                onTabSelected = ::navigateToTab
            ) { padding ->
                PlanScreen(padding)
            }
        }

        composable(MainTab.GOALS.route) {
            AppScaffold(
                currentTab = MainTab.GOALS,
                onTabSelected = ::navigateToTab
            ) { padding ->
                GoalsScreen(
                    scaffoldPadding = padding,
                    onGoalClick = { navController.navigate("goal-detail") },
                    onProfileClick = { navController.navigate("profile") }
                )
            }
        }

        composable(
            route = "transaction-detail/{transactionId}",
            arguments = listOf(
                navArgument("transactionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId")
                ?: return@composable
            AppScaffold(
                currentTab = MainTab.ACTIVITY,
                onTabSelected = ::navigateToTab
            ) { padding ->
                TransactionDetailScreen(
                    scaffoldPadding = padding,
                    transactionId = transactionId,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("goal-detail") {
            AppScaffold(
                currentTab = MainTab.GOALS,
                onTabSelected = ::navigateToTab
            ) { padding ->
                GoalDetailScreen(
                    scaffoldPadding = padding,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("profile") {
            AppScaffold(
                currentTab = null,
                onTabSelected = ::navigateToTab
            ) { padding ->
                ProfileScreen(
                    scaffoldPadding = padding,
                    onBack = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
        }

        composable("settings") {
            AppScaffold(
                currentTab = null,
                onTabSelected = ::navigateToTab
            ) { padding ->
                SettingsScreen(
                    scaffoldPadding = padding,
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    onBack = { navController.popBackStack() },
                    onProfileClick = { navController.navigate("profile") },
                    onMpesaConnectionClick = { navController.navigate("mpesa-connection") },
                    onSmartRulesClick = { navController.navigate("smart-rules") }
                )
            }
        }

        composable("smart-rules") {
            AppScaffold(
                currentTab = null,
                onTabSelected = ::navigateToTab
            ) { padding ->
                MoneySetupScreen(
                    scaffoldPadding = padding,
                    initialSetup = false,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("mpesa-connection") {
            AppScaffold(
                currentTab = null,
                onTabSelected = ::navigateToTab
            ) { padding ->
                MpesaConnectionScreen(
                    scaffoldPadding = padding,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
