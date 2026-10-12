package com.kedevjimmy.pesatrack

import android.os.Bundle
import android.os.Build
import android.os.CancellationSignal
import android.hardware.biometrics.BiometricPrompt
import android.app.KeyguardManager
import android.hardware.biometrics.BiometricManager
import android.content.pm.PackageManager
import android.content.Intent
import android.view.WindowManager
import com.kedevjimmy.pesatrack.salary.SalaryReminders
import com.kedevjimmy.pesatrack.security.AppLockManager
import com.kedevjimmy.pesatrack.planning.BillSchedules
import com.kedevjimmy.pesatrack.planning.ReviewAttentionReminders
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kedevjimmy.pesatrack.ingestion.AutomaticHistoryRepair
import com.kedevjimmy.pesatrack.ingestion.RecentSmsCatchUp
import com.kedevjimmy.pesatrack.ui.PesaTrackApp
import com.kedevjimmy.pesatrack.ui.screens.StartupSplashScreen
import com.kedevjimmy.pesatrack.ui.screens.AppLockScreen
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import com.kedevjimmy.pesatrack.ui.theme.ThemeMode
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var salaryOpenRequest by mutableStateOf(0)
    private var billsOpenRequest by mutableStateOf(0)
    private var planningOpenRequest by mutableStateOf(0)
    private var reviewOpenRequest by mutableStateOf(0)
    private var planningRoute by mutableStateOf("budgets")
    private lateinit var appLockManager:
        AppLockManager
    private var appLocked by mutableStateOf(false)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(SalaryReminders.OPEN_SALARY, false)) salaryOpenRequest++
        if (intent.getBooleanExtra(BillSchedules.OPEN_BILLS, false)) billsOpenRequest++
        if (intent.getBooleanExtra(ReviewAttentionReminders.OPEN_ACTIVITY_REVIEW, false)) reviewOpenRequest++
        intent.getStringExtra(com.kedevjimmy.pesatrack.planning.PlanningReminders.OPEN_PLAN)?.takeIf { it in setOf("budgets","saving-goals") }?.let { planningRoute=it; planningOpenRequest++ }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.kedevjimmy.pesatrack.security.BackupRecoveryStatus.retry(this)
        if (com.kedevjimmy.pesatrack.security.BackupRecoveryStatus.failure != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            setContentView(android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(32, 64, 32, 32)
                addView(android.widget.TextView(this@MainActivity).apply {
                    text = getString(R.string.backup_recovery_retry_message)
                    textSize = 18f
                })
                addView(android.widget.Button(this@MainActivity).apply {
                    text = getString(R.string.backup_recovery_retry_action)
                    setOnClickListener {
                        com.kedevjimmy.pesatrack.security.BackupRecoveryStatus.retry(this@MainActivity)
                        recreate()
                    }
                })
            })
            return
        }
        appLockManager =
            AppLockManager(this)
        appLocked =
            appLockManager.isEnabled()
        updateSecureWindowState()
        if (intent.getBooleanExtra(SalaryReminders.OPEN_SALARY, false)) salaryOpenRequest++
        if (intent.getBooleanExtra(BillSchedules.OPEN_BILLS, false)) billsOpenRequest++
        if (intent.getBooleanExtra(ReviewAttentionReminders.OPEN_ACTIVITY_REVIEW, false)) reviewOpenRequest++
        intent.getStringExtra(com.kedevjimmy.pesatrack.planning.PlanningReminders.OPEN_PLAN)?.takeIf { it in setOf("budgets","saving-goals") }?.let { planningRoute=it; planningOpenRequest++ }
        SalaryReminders.schedule(this)
        BillSchedules.schedule(this)
        com.kedevjimmy.pesatrack.planning.PlanningReminders.schedule(this)
        ReviewAttentionReminders.schedule(this)
        enableEdgeToEdge()
        AutomaticHistoryRepair.scheduleIfAllowed(this)
        setContent {
            val recoveryFailure by com.kedevjimmy.pesatrack.security.BackupRecoveryStatus.failures.collectAsState()
            if (recoveryFailure != null) {
                com.kedevjimmy.pesatrack.ui.screens.BackupRecoveryBlockingScreen(onRetry = {
                    com.kedevjimmy.pesatrack.security.BackupRecoveryStatus.retry(this)
                    if (com.kedevjimmy.pesatrack.security.BackupRecoveryStatus.failure == null) recreate()
                })
                return@setContent
            }
            val preferences = remember {
                getSharedPreferences("pesatrack_preferences", MODE_PRIVATE)
            }
            var themeMode by remember {
                mutableStateOf(
                    runCatching {
                        ThemeMode.valueOf(
                            preferences.getString("theme_mode", ThemeMode.SYSTEM.name)
                                ?: ThemeMode.SYSTEM.name
                        )
                    }.getOrDefault(ThemeMode.SYSTEM)
                )
            }
            var setupComplete by remember {
                mutableStateOf(preferences.getBoolean("setup_complete", false))
            }
            var welcomeVisible by remember { mutableStateOf(!setupComplete) }
            var showLaunchSplash by remember {
                mutableStateOf(true)
            }
            LaunchedEffect(Unit) {
                delay(650)
                showLaunchSplash = false
            }

            val systemDark = isSystemInDarkTheme()
            val useDark = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            androidx.compose.runtime.SideEffect {
                val darkSurface = useDark || showLaunchSplash || (appLocked && appLockManager.isEnabled()) || welcomeVisible
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !darkSurface
                controller.isAppearanceLightNavigationBars = !darkSurface
            }
            PesaTrackTheme(darkTheme = useDark) {
                if (showLaunchSplash) {
                    StartupSplashScreen()
                } else if (
                    appLocked &&
                    appLockManager.isEnabled()
                ) {
                    AppLockScreen(
                        biometricAvailable =
                            canUseBiometrics(),
                        onPinUnlock = { pin ->
                            val unlocked =
                                appLockManager
                                    .verifyPin(pin)
                            if (unlocked) {
                                appLockManager
                                    .markUnlocked()
                                appLocked =
                                    false
                            }
                            unlocked
                        },
                        pinLockoutRemainingMs = {
                            appLockManager
                                .remainingLockoutMs()
                        },
                        onBiometricUnlock = {
                            requestBiometricUnlock()
                        }
                    )
                } else {
                    PesaTrackApp(
                    salaryOpenRequest = salaryOpenRequest,
                    billsOpenRequest = billsOpenRequest,
                    reviewOpenRequest = reviewOpenRequest,
                            planningOpenRequest = planningOpenRequest, planningRoute = planningRoute,
                    themeMode = themeMode,
                    onThemeModeChange = { newMode ->
                        themeMode = newMode
                        preferences.edit().putString("theme_mode", newMode.name).apply()
                    },
                    setupComplete = setupComplete,
                    onWelcomeVisibilityChange = { welcomeVisible = it },
                    onSetupComplete = {
                        setupComplete = true
                        preferences.edit()
                            .putBoolean(
                                "setup_complete",
                                true
                            )
                            .apply()
                        SalaryReminders.schedule(this)
        BillSchedules.schedule(this)
        com.kedevjimmy.pesatrack.planning.PlanningReminders.schedule(this)
        ReviewAttentionReminders.schedule(this)
                        AutomaticHistoryRepair.scheduleIfAllowed(this)
                    }
                )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (com.kedevjimmy.pesatrack.security.BackupRecoveryStatus.failure != null) return
        updateSecureWindowState()
        RecentSmsCatchUp.schedule(this)
        if (
            ::appLockManager
                .isInitialized &&
            appLockManager
                .shouldRequireUnlock()
        ) {
            appLocked = true
        }
    }

    override fun onStop() {
        if (
            ::appLockManager
                .isInitialized &&
            !isChangingConfigurations
        ) {
            appLockManager
                .markBackgrounded()
        }
        super.onStop()
    }

    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {
        super.onWindowFocusChanged(
            hasFocus
        )
        if (
            hasFocus &&
            ::appLockManager.isInitialized
        ) {
            updateSecureWindowState()
        }
    }

    private fun updateSecureWindowState() {
        if (
            !::appLockManager
                .isInitialized
        ) {
            return
        }
        if (
            appLockManager.isEnabled()
        ) {
            window.addFlags(
                WindowManager.LayoutParams
                    .FLAG_SECURE
            )
        } else {
            window.clearFlags(
                WindowManager.LayoutParams
                    .FLAG_SECURE
            )
        }
    }

    private fun canUseBiometrics(): Boolean {
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.P
        ) {
            return false
        }
        val keyguard =
            getSystemService(
                KeyguardManager::class.java
            )
        if (
            keyguard
                ?.isKeyguardSecure !=
                true
        ) {
            return false
        }

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {
            getSystemService(
                BiometricManager::class.java
            )
                ?.canAuthenticate() ==
                BiometricManager
                    .BIOMETRIC_SUCCESS
        } else {
            packageManager
                .hasSystemFeature(
                    PackageManager
                        .FEATURE_FINGERPRINT
                )
        }
    }

    private fun requestBiometricUnlock() {
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.P
        ) {
            return
        }

        val prompt =
            BiometricPrompt.Builder(this)
                .setTitle(
                    "Unlock PesaTrack"
                )
                .setSubtitle(
                    "Confirm your identity to view your money data."
                )
                .setNegativeButton(
                    "Use PIN",
                    mainExecutor
                ) { _, _ -> }
                .build()

        prompt.authenticate(
            CancellationSignal(),
            mainExecutor,
            object :
                BiometricPrompt
                    .AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result:
                        BiometricPrompt
                            .AuthenticationResult
                ) {
                    super
                        .onAuthenticationSucceeded(
                            result
                        )
                    appLockManager
                        .markUnlocked()
                    appLocked = false
                }
            }
        )
    }

}
