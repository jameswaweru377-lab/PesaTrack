package com.kedevjimmy.pesatrack.ui

import androidx.navigation.compose.currentBackStackEntryAsState

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kedevjimmy.pesatrack.ui.components.AppScaffold
import com.kedevjimmy.pesatrack.ui.components.MainTab
import com.kedevjimmy.pesatrack.ui.screens.AccountDetailsScreen
import com.kedevjimmy.pesatrack.ui.screens.AccountsScreen
import com.kedevjimmy.pesatrack.ui.screens.ActivityScreen
import com.kedevjimmy.pesatrack.ui.screens.ActivityScreenCache
import com.kedevjimmy.pesatrack.ui.screens.BillsRemindersScreen
import com.kedevjimmy.pesatrack.ui.screens.BudgetScreen
import com.kedevjimmy.pesatrack.ui.screens.DebtsScreen
import com.kedevjimmy.pesatrack.ui.screens.DataHealthScreen
import com.kedevjimmy.pesatrack.ui.screens.HistoricalImportScreen
import com.kedevjimmy.pesatrack.ui.screens.FinancialInstitutionsScreen
import com.kedevjimmy.pesatrack.ui.screens.IdentityReviewScreen
import com.kedevjimmy.pesatrack.ui.screens.SalaryScreen
import com.kedevjimmy.pesatrack.ui.screens.HomeScreen
import com.kedevjimmy.pesatrack.ui.screens.HomeScreenCache
import com.kedevjimmy.pesatrack.ui.screens.MoneySetupScreen
import com.kedevjimmy.pesatrack.ui.screens.MoreScreen
import com.kedevjimmy.pesatrack.ui.screens.MpesaConnectionScreen
import com.kedevjimmy.pesatrack.ui.screens.OnboardingScreen
import com.kedevjimmy.pesatrack.ui.screens.PlanScreen
import com.kedevjimmy.pesatrack.ui.screens.ProfileScreen
import com.kedevjimmy.pesatrack.ui.screens.ReportsInsightsScreen
import com.kedevjimmy.pesatrack.ui.screens.SavingGoalsScreen
import com.kedevjimmy.pesatrack.ui.screens.SettingsScreen
import com.kedevjimmy.pesatrack.ui.screens.SmartOnboardingSetupScreen
import com.kedevjimmy.pesatrack.ui.screens.TransactionDetailScreen
import com.kedevjimmy.pesatrack.ui.theme.ThemeMode

@Composable
fun PesaTrackApp(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    setupComplete: Boolean,
    onWelcomeVisibilityChange: (Boolean) -> Unit = {},
    onSetupComplete: () -> Unit,
    salaryOpenRequest: Int = 0,
    billsOpenRequest: Int = 0,
    planningOpenRequest: Int = 0, planningRoute: String = "budgets"
) {
    val navController = rememberNavController()
    val planScreenCache = remember { com.kedevjimmy.pesatrack.ui.screens.PlanScreenCache() }
    val accountsScreenCache = remember { com.kedevjimmy.pesatrack.ui.screens.AccountsScreenCache() }
    val homeScreenCache =
        remember {
            HomeScreenCache()
        }
    val activityScreenCache =
        remember {
            ActivityScreenCache()
        }

    LaunchedEffect(salaryOpenRequest, setupComplete) {
        if (salaryOpenRequest > 0 && setupComplete) navController.navigate("salary") { launchSingleTop = true }
    }

    LaunchedEffect(billsOpenRequest, setupComplete) {
        if (billsOpenRequest > 0 && setupComplete) navController.navigate("bills") { launchSingleTop = true }
    }

    LaunchedEffect(planningOpenRequest, setupComplete) {
        if(planningOpenRequest > 0 && setupComplete && planningRoute in setOf("budgets", "saving-goals")) navController.navigate(planningRoute) { launchSingleTop = true }
    }

    fun navigateToTab(tab: MainTab) {
        val alreadyAtTab =
            navController.currentDestination
                ?.route == tab.route
        if (alreadyAtTab) {
            return
        }

        val returnedToExistingTab =
            navController.popBackStack(
                route = tab.route,
                inclusive = false
            )
        if (!returnedToExistingTab) {
            navController.navigate(
                tab.route
            ) {
                launchSingleTop = true
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

    val currentEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentEntry?.destination?.route, setupComplete) {
        onWelcomeVisibilityChange((currentEntry?.destination?.route ?: startDestination) == "onboarding")
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
                    onSalaryClick = { navController.navigate("salary") },
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
                    },
                    onBillsClick = {
                        navController.navigate("bills")
                    },
                    onGoalsClick = {
                        navController.navigate("saving-goals")
                    },
                    onReportsClick = {
                        navController.navigate("reports")
                    },
                    stateCache =
                        homeScreenCache
                )
            }
        }

        composable("salary") {
            AppScaffold(currentTab = MainTab.HOME, onTabSelected = ::navigateToTab) { padding ->
                SalaryScreen(padding, onBack = { navController.popBackStack() })
            }
        }

        composable(MainTab.ACCOUNTS.route) {
            AppScaffold(
                currentTab = MainTab.ACCOUNTS,
                onTabSelected = ::navigateToTab
            ) { padding ->
                AccountsScreen(
                    stateCache = accountsScreenCache,
                    scaffoldPadding = padding,
                    onDebtsClick = {
                        navController.navigate(
                            "debts"
                        )
                    },
                    onAccountClick = { accountId ->
                        navController.navigate(
                            "account-detail/$accountId"
                        )
                    },
                    onManageInstitutions = {
                        navController.navigate(
                            "financial-institutions"
                        )
                    }
                )
            }
        }

        composable("financial-institutions") {
            AppScaffold(
                currentTab = MainTab.ACCOUNTS,
                onTabSelected = ::navigateToTab
            ) { padding ->
                FinancialInstitutionsScreen(
                    scaffoldPadding = padding,
                    onBack = {
                        navController.popBackStack()
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
                    onSalaryClick = { navController.navigate("salary") },
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
                    stateCache =
                        activityScreenCache
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
                    onSalaryClick = { navController.navigate("salary") },
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
                    openNeedsAttention = true,
                    stateCache =
                        activityScreenCache
                )
            }
        }

        // Compatibility-only routes for restored navigation state from older builds.
        // Retired preview features stay inaccessible; old destinations redirect safely.
        composable("community") {
            LaunchedEffect(Unit) {
                navController.navigate(MainTab.MORE.route) {
                    popUpTo("community") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

        composable("goals") {
            LaunchedEffect(Unit) {
                navController.navigate("saving-goals") {
                    popUpTo("goals") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

        composable("goal-detail") {
            LaunchedEffect(Unit) {
                navController.navigate(MainTab.PLAN.route) {
                    popUpTo("goal-detail") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

        composable(MainTab.PLAN.route) {
            AppScaffold(
                currentTab = MainTab.PLAN,
                onTabSelected = ::navigateToTab
            ) { padding ->
                PlanScreen(
                    stateCache = planScreenCache,
                    scaffoldPadding = padding,
                    onBudgetClick = {
                        navController.navigate(
                            "budgets"
                        )
                    },
                    onGoalsClick = {
                        navController.navigate(
                            "saving-goals"
                        )
                    },
                    onSalaryClick = {
                        navController.navigate(
                            "salary"
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
                    },
                    onReportsClick = {
                        navController.navigate("reports")
                    },
                    onBillsClick = {
                        navController.navigate("bills")
                    },
                    onGoalsClick = {
                        navController.navigate("saving-goals")
                    }
                )
            }
        }

        composable("reports") {
            AppScaffold(
                currentTab = MainTab.MORE,
                onTabSelected = ::navigateToTab
            ) { padding ->
                ReportsInsightsScreen(
                    scaffoldPadding = padding,
                    onBack = {
                        navController.popBackStack()
                    },
                    onTransactionClick = { id -> navController.navigate("transaction-detail/$id") },
                    onReviewClick = {
                        navigateToTab(
                            MainTab.ACTIVITY
                        )
                    }
                )
            }
        }

        composable("bills") {
            AppScaffold(
                currentTab = MainTab.MORE,
                onTabSelected = ::navigateToTab
            ) { padding ->
                BillsRemindersScreen(
                    scaffoldPadding = padding,
                    onBack = { navController.popBackStack() },
                    onPlanClick = { navController.navigate("smart-rules") }
                )
            }
        }

        composable("saving-goals") {
            AppScaffold(
                currentTab = MainTab.PLAN,
                onTabSelected = ::navigateToTab
            ) { padding ->
                SavingGoalsScreen(
                    scaffoldPadding = padding,
                    onBack = { navController.popBackStack() },
                    onPlanClick = { navController.navigate("smart-rules") }
                )
            }
        }

        composable(
            route = "account-detail/{accountId}",
            arguments = listOf(
                navArgument("accountId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val accountId =
                backStackEntry.arguments
                    ?.getString("accountId")
                    ?: return@composable
            AppScaffold(
                currentTab = MainTab.ACCOUNTS,
                onTabSelected = ::navigateToTab
            ) { padding ->
                AccountDetailsScreen(
                    scaffoldPadding = padding,
                    accountId = accountId,
                    onAccountClick = { childId -> navController.navigate("account-detail/$childId") },
                    onBack = { navController.popBackStack() },
                    onTransactionClick = { id ->
                        navController.navigate("transaction-detail/$id")
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
                    onBack = {
                        navController.popBackStack()
                    },
                    onTransactionClick = { id ->
                        navController.navigate(
                            "transaction-detail/$id"
                        )
                    },
                    onAccountClick = { accountId ->
                        navController.navigate(
                            "account-detail/$accountId"
                        )
                    }
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
                    onSettingsClick = { navController.navigate("settings") },
                    onAccountsClick = { navigateToTab(MainTab.ACCOUNTS) },
                    onDataHealthClick = { navController.navigate("data-health") },
                    onGoalsClick = { navController.navigate("saving-goals") }
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
                    onHistoricalImportClick = { navController.navigate("historical-import") },
                    onAdvancedClick = { navController.navigate("data-health") }
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
