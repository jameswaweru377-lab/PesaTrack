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
import com.kedevjimmy.pesatrack.ui.screens.AccountsScreen
import com.kedevjimmy.pesatrack.ui.screens.ActivityScreen
import com.kedevjimmy.pesatrack.ui.screens.BudgetScreen
import com.kedevjimmy.pesatrack.ui.screens.DebtsScreen
import com.kedevjimmy.pesatrack.ui.screens.DataHealthScreen
import com.kedevjimmy.pesatrack.ui.screens.HistoricalImportScreen
import com.kedevjimmy.pesatrack.ui.screens.IdentityReviewScreen
import com.kedevjimmy.pesatrack.ui.screens.HomeScreen
import com.kedevjimmy.pesatrack.ui.screens.MoneySetupScreen
import com.kedevjimmy.pesatrack.ui.screens.MoreScreen
import com.kedevjimmy.pesatrack.ui.screens.MpesaConnectionScreen
import com.kedevjimmy.pesatrack.ui.screens.OnboardingScreen
import com.kedevjimmy.pesatrack.ui.screens.PlanScreen
import com.kedevjimmy.pesatrack.ui.screens.ProfileScreen
import com.kedevjimmy.pesatrack.ui.screens.SettingsScreen
import com.kedevjimmy.pesatrack.ui.screens.SmartOnboardingSetupScreen
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

    val startDestination =
        if (!setupComplete) {
            "onboarding"
        } else {
            MainTab.HOME.route
        }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onContinue = {
                    navController.navigate(
                        "smart-onboarding-setup"
                    )
                }
            )
        }

        composable("smart-onboarding-setup") {
            SmartOnboardingSetupScreen(
                scaffoldPadding =
                    PaddingValues(),
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
                    onProfileClick = {
                        navController.navigate("profile")
                    },
                    onAttentionClick = {
                        navController.navigate(
                            "activity-attention"
                        )
                    },
                    onActivityClick = {
                        navigateToTab(
                            MainTab.ACTIVITY
                        )
                    },
                    onTransactionClick = { id ->
                        navController.navigate(
                            "transaction-detail/$id"
                        )
                    },
                    onPlanClick = {
                        navigateToTab(MainTab.PLAN)
                    },
                    onAccountsClick = {
                        navigateToTab(
                            MainTab.ACCOUNTS
                        )
                    },
                    onDebtsClick = {
                        navController.navigate(
                            "debts"
                        )
                    }
                )
            }
        }

        composable(MainTab.ACCOUNTS.route) {
            AppScaffold(
                currentTab = MainTab.ACCOUNTS,
                onTabSelected = ::navigateToTab
            ) { padding ->
                AccountsScreen(
                    scaffoldPadding = padding,
                    onDebtsClick = {
                        navController.navigate(
                            "debts"
                        )
                    }
                )
            }
        }

        composable("debts") {
            AppScaffold(
                currentTab = MainTab.ACCOUNTS,
                onTabSelected = ::navigateToTab
            ) { padding ->
                DebtsScreen(
                    scaffoldPadding = padding,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable("budgets") {
            AppScaffold(
                currentTab = MainTab.PLAN,
                onTabSelected = ::navigateToTab
            ) { padding ->
                BudgetScreen(
                    scaffoldPadding = padding,
                    onBack = { navController.popBackStack() }
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
                        navController.navigate(
                            "transaction-detail/$id"
                        )
                    },
                    onProfileClick = {
                        navController.navigate(
                            "profile"
                        )
                    },
                    onIdentityReviewClick = {
                        navController.navigate(
                            "identity-review"
                        )
                    },
                    onDataHealthClick = {
                        navController.navigate(
                            "data-health"
                        )
                    }
                )
            }
        }

        composable("activity-attention") {
            AppScaffold(
                currentTab = MainTab.ACTIVITY,
                onTabSelected = ::navigateToTab
            ) { padding ->
                ActivityScreen(
                    scaffoldPadding = padding,
                    onTransactionClick = { id ->
                        navController.navigate(
                            "transaction-detail/$id"
                        )
                    },
                    onProfileClick = {
                        navController.navigate(
                            "profile"
                        )
                    },
                    onIdentityReviewClick = {
                        navController.navigate(
                            "identity-review"
                        )
                    },
                    onDataHealthClick = {
                        navController.navigate(
                            "data-health"
                        )
                    },
                    openNeedsAttention = true
                )
            }
        }

        composable(MainTab.PLAN.route) {
            AppScaffold(
                currentTab = MainTab.PLAN,
                onTabSelected = ::navigateToTab
            ) { padding ->
                PlanScreen(
                    scaffoldPadding = padding,
                    onBudgetClick = {
                        navController.navigate(
                            "budgets"
                        )
                    }
                )
            }
        }

        composable(MainTab.MORE.route) {
            AppScaffold(
                currentTab = MainTab.MORE,
                onTabSelected = ::navigateToTab
            ) { padding ->
                MoreScreen(
                    scaffoldPadding = padding,
                    onProfileClick = {
                        navController.navigate("profile")
                    },
                    onImportClick = {
                        navController.navigate(
                            "historical-import"
                        )
                    },
                    onDataHealthClick = {
                        navController.navigate("data-health")
                    },
                    onSettingsClick = {
                        navController.navigate("settings")
                    }
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

        composable("data-health") {
            AppScaffold(
                currentTab = MainTab.MORE,
                onTabSelected = ::navigateToTab
            ) { padding ->
                DataHealthScreen(
                    scaffoldPadding = padding,
                    onBack = {
                        navController.popBackStack()
                    },
                    onIdentityReviewClick = {
                        navController.navigate(
                            "identity-review"
                        )
                    }
                )
            }
        }

        composable("identity-review") {
            AppScaffold(
                currentTab = MainTab.MORE,
                onTabSelected = ::navigateToTab
            ) { padding ->
                IdentityReviewScreen(
                    scaffoldPadding = padding,
                    onBack = {
                        navController.popBackStack()
                    }
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
                    onSmartRulesClick = { navController.navigate("smart-rules") },
                    onHistoricalImportClick = { navController.navigate("historical-import") }
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

        composable("historical-import") {
            AppScaffold(
                currentTab = null,
                onTabSelected = ::navigateToTab
            ) { padding ->
                HistoricalImportScreen(
                    scaffoldPadding = padding,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
