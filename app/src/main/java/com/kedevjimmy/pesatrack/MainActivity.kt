package com.kedevjimmy.pesatrack

import android.os.Bundle
import android.content.Intent
import com.kedevjimmy.pesatrack.salary.SalaryReminders
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kedevjimmy.pesatrack.ingestion.AutomaticHistoryRepair
import com.kedevjimmy.pesatrack.ui.PesaTrackApp
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import com.kedevjimmy.pesatrack.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    private var salaryOpenRequest by mutableStateOf(0)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(SalaryReminders.OPEN_SALARY, false)) salaryOpenRequest++
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(SalaryReminders.OPEN_SALARY, false)) salaryOpenRequest++
        SalaryReminders.schedule(this)
        enableEdgeToEdge()
        AutomaticHistoryRepair.scheduleIfAllowed(this)
        setContent {
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
            val systemDark = isSystemInDarkTheme()
            val useDark = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            PesaTrackTheme(darkTheme = useDark) {
                PesaTrackApp(
                    salaryOpenRequest = salaryOpenRequest,
                    themeMode = themeMode,
                    onThemeModeChange = { newMode ->
                        themeMode = newMode
                        preferences.edit().putString("theme_mode", newMode.name).apply()
                    },
                    setupComplete = setupComplete,
                    onSetupComplete = {
                        setupComplete = true
                        preferences.edit()
                            .putBoolean(
                                "setup_complete",
                                true
                            )
                            .apply()
                        SalaryReminders.schedule(this)
                        AutomaticHistoryRepair.scheduleIfAllowed(this)
                    }
                )
            }
        }
    }
}


