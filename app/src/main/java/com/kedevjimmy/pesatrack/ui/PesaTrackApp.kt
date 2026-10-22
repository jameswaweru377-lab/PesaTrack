package com.kedevjimmy.pesatrack.ui

import androidx.navigation.compose.currentBackStackEntryAsState

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.ui.screens.monthStart
import com.kedevjimmy.pesatrack.ui.screens.readActivityCoreForMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.kedevjimmy.pesatrack.ui.screens.PayrollJournalSheet
import com.kedevjimmy.pesatrack.ui.screens.HomeScreen
import com.kedevjimmy.pesatrack.ui.screens.HomeScreenCache
import com.kedevjimmy.pesatrack.ui.screens.MoneySetupScreen
import com.kedevjimmy.pesatrack.ui.screens.MoreScreen
import com.kedevjimmy.pesatrack.ui.screens.StatementActivityDialog
import com.kedevjimmy.pesatrack.ui.screens.MpesaConnectionScreen
import com.kedevjimmy.pesatrack.ui.screens.OnboardingScreen
import com.kedevjimmy.pesatrack.ui.screens.PayCycleActivityScreen
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
    reviewOpenRequest: Int = 0,
    smartOpenRequest: Int = 0, smartKey: String = "", smartAction: String = "review", smartRevision: String = "",
    onSmartRequestHandled: () -> Unit = {},
    planningOpenRequest: Int = 0, planningRoute: String = "budgets"
) {
    val navController = rememberNavController()
    val planScreenCache = remember { com.kedevjimmy.pesatrack.ui.screens.PlanScreenCache() }
    val accountsScreenCache = remember { com.kedevjimmy.pesatrack.ui.screens.AccountsScreenCache() }
    val debtsScreenCache = remember { com.kedevjimmy.pesatrack.ui.screens.DebtsScreenCache() }
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

    LaunchedEffect(reviewOpenRequest, setupComplete) {
        if (reviewOpenRequest > 0 && setupComplete)
            navController.navigate("activity-attention") { launchSingleTop = true }
    }

    LaunchedEffect(smartOpenRequest, setupComplete) {
        if (smartOpenRequest > 0 && setupComplete && smartKey.isNotBlank()) {
            navController.navigate("smart-review/${android.net.Uri.encode(smartKey)}?action=$smartAction&revision=${android.net.Uri.encode(smartRevision)}") { launchSingleTop = true }
            onSmartRequestHandled()
        }
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
    // Warm Activity only after Home has settled at the current ledger revision.
    // The prefetch is cancelled when leaving Home, and all SQL is off the UI
    // thread. A stale or interrupted prefetch is never used for financial totals.
    val appContext = LocalContext.current.applicationContext
    val warmupRevision by LedgerUpdateBus.state.collectAsState()
    LaunchedEffect(currentEntry?.destination?.route, setupComplete, warmupRevision.revision) {
        if (!setupComplete || currentEntry?.destination?.route != MainTab.HOME.route)
            return@LaunchedEffect
        val revision = warmupRevision.revision
        // Wait for the Home money picture to finish before competing for the
        // shared ledger lock. Stop watching if the screen is too slow or fails.
        for (attempt in 0 until 24) {
            if (homeScreenCache.revision == revision && homeScreenCache.data != null) break
            delay(200L)
        }
        if (homeScreenCache.revision != revision || homeScreenCache.data == null)
            return@LaunchedEffect
        val month = activityScreenCache.selectedMonthEpochMs ?: monthStart(System.currentTimeMillis())
        if (activityScreenCache.coreFor(revision, month) != null ||
            (activityScreenCache.revision == revision &&
                activityScreenCache.selectedMonthEpochMs == month &&
                activityScreenCache.data != null)) return@LaunchedEffect
        try {
            val loaded = withContext(Dispatchers.IO) {
                readActivityCoreForMonth(appContext, month)
            }
            if (LedgerUpdateBus.state.value.revision == revision)
                activityScreenCache.saveCore(revision, month, loaded)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Preloading is best-effort; Activity retains its ordinary loader.
        }
    }


    LaunchedEffect(currentEntry?.destination?.route, setupComplete) {
        onWelcomeVisibilityChange((currentEntry?.destination?.route ?: startDestination) == "onboarding")
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("smart-review/{key}?action={action}&revision={revision}", arguments = listOf(
            navArgument("key") { type = NavType.StringType },
            navArgument("action") { type = NavType.StringType; defaultValue = "review" },
            navArgument("revision") { type = NavType.StringType; defaultValue = "" }
        )) { entry ->
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, currentTab = MainTab.ACCOUNTS, onTabSelected = ::navigateToTab) { padding ->
                com.kedevjimmy.pesatrack.ui.screens.SmartNoticeReviewScreen(
                    padding, entry.arguments?.getString("key").orEmpty(),
                    entry.arguments?.getString("action") ?: "review",
                    onBack = { navController.popBackStack() },
                    onTransaction = { navController.navigate("transaction-detail/$it") },
                    expectedRevision = entry.arguments?.getString("revision").orEmpty())
            }
        }
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
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
                SmartOnboardingSetupScreen(
                    scaffoldPadding =
                        PaddingValues(),
                    onDone = ::finishSetup
                )
            }
        }

        composable(MainTab.HOME.route) {
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } },
                onNotificationsClick = { navController.navigate("activity-attention") { launchSingleTop = true } },
                onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } },
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
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, currentTab = MainTab.HOME, onTabSelected = ::navigateToTab) { padding ->
                SalaryScreen(padding, onBack = { navController.popBackStack() },
                    onPaycheckClick = { navController.navigate("paycheck-analysis") })
            }
        }

        composable("paycheck-analysis") {
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, currentTab = MainTab.ACTIVITY, onTabSelected = ::navigateToTab) { padding ->
                PayrollJournalSheet(
                    onDismiss = { navController.popBackStack() },
                    asPage = true,
                    scaffoldPadding = padding
                )
            }
        }

        composable(MainTab.ACCOUNTS.route) {
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
                currentTab = MainTab.ACCOUNTS,
                onTabSelected = ::navigateToTab
            ) { padding ->
                AccountsScreen(
                    stateCache = accountsScreenCache,
                    debtStateCache = debtsScreenCache,
                    onDiscoverReceivables = { navController.navigate("smart-review/discovery") { launchSingleTop = true } },
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
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
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
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
                currentTab = MainTab.ACCOUNTS,
                onTabSelected = ::navigateToTab
            ) { padding ->
                DebtsScreen(
                    scaffoldPadding = padding,
                    stateCache = debtsScreenCache,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable("budgets") {
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
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
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
                currentTab = MainTab.ACTIVITY,
                onTabSelected = ::navigateToTab
            ) { padding ->
                ActivityScreen(
                    scaffoldPadding = padding,
                    onSalaryClick = { navController.navigate("salary") },
                    onPaycheckClick = { navController.navigate("paycheck-analysis") },
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
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
                currentTab = MainTab.ACTIVITY,
                onTabSelected = ::navigateToTab
            ) { padding ->
                ActivityScreen(
                    scaffoldPadding = padding,
                    onSalaryClick = { navController.navigate("salary") },
                    onPaycheckClick = { navController.navigate("paycheck-analysis") },
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
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
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
                    },
                    onBillsClick = { navController.navigate("bills") },
                    onPayCycleClick = { navController.navigate("pay-cycle") }
                )
            }
        }

        composable("pay-cycle") {
            AppScaffold(
                onProfileClick = { navController.navigate("profile") { launchSingleTop = true } },
                onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } },
                currentTab = MainTab.PLAN,
                onTabSelected = ::navigateToTab
            ) { padding ->
                PayCycleActivityScreen(
                    scaffoldPadding = padding,
                    onCalendar = { navController.popBackStack() },
                    onSalaryClick = { navController.navigate("salary") },
                    onTransactionClick = { id -> navController.navigate("transaction-detail/$" + "id") }
                )
            }
        }

        composable(MainTab.MORE.route) {
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
                currentTab = MainTab.MORE,
                onTabSelected = ::navigateToTab
            ) { padding ->
                MoreScreen(
                    scaffoldPadding = padding,
                    onAccountsClick = { navigateToTab(MainTab.ACCOUNTS) },
                    onProfileClick = {
                        navController.navigate("profile")
                    },
                    onImportClick = {
                        navController.navigate(
                            "historical-import"
                        )
                    },
                    onStatementActivityClick = {
                        navController.navigate("statement-activity") { launchSingleTop = true }
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

        composable("statement-activity") {
            // Read-only PDF statement archive. Never invokes SMS imports or account updates.
            StatementActivityDialog(
                onDismiss = { navController.popBackStack() },
                onTabSelected = ::navigateToTab
            )
        }

        composable("reports") {
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
                currentTab = MainTab.MORE,
                onTabSelected = ::navigateToTab
            ) { padding ->
                ReportsInsightsScreen(
                    scaffoldPadding = padding,
                    onBack = {
                        navController.popBackStack()
                    },
                    onTransactionClick = { id -> navController.navigate("transaction-detail/$id") },
                    onPaycheckClick = { navController.navigate("paycheck-analysis") },
                    onReviewClick = {
                        navigateToTab(
                            MainTab.ACTIVITY
                        )
                    }
                )
            }
        }

        composable("bills") {
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
                currentTab = MainTab.PLAN,
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
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
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
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
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
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
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
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
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
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
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
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
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
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
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
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
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
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
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
            AppScaffold(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }, onPaycheckClick = { navController.navigate("paycheck-analysis") { launchSingleTop = true } }, 
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
