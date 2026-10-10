package com.kedevjimmy.pesatrack.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.ui.screens.ReviewDiscoveryPage
import com.kedevjimmy.pesatrack.ui.screens.OnboardingSalaryChoicePage
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingSalarySuggestion
import com.kedevjimmy.pesatrack.ui.screens.DetectedAccountCheckRow
import com.kedevjimmy.pesatrack.ui.screens.MpesaConfirmationPage
import com.kedevjimmy.pesatrack.data.local.DiscoveredFinancialEntity
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingOwnershipChoice
import com.kedevjimmy.pesatrack.domain.interpretation.AccountOwnershipHint
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountNature
import org.junit.Assert.assertEquals
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingDiscoveryUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun detectedAccountsAreUncheckedAndOnlyUserCanSelectThem() {
        var selected = false
        compose.setContent {
            PesaTrackTheme(darkTheme = true) {
                DetectedAccountCheckRow(
                    key = "provider:synthetic-bank",
                    name = "Example Bank",
                    type = "Bank",
                    checked = selected,
                    enabled = true,
                    onChecked = { selected = it }
                )
            }
        }
        compose.runOnIdle { assertTrue(!selected) }
        compose.onNodeWithTag("detected-account-provider:synthetic-bank").performClick()
        compose.runOnIdle { assertTrue(selected) }
    }

    @Test fun accountListSupportsSkippingWithoutAnyOwnershipSelection() {
        var continued = false
        compose.setContent {
            PesaTrackTheme(darkTheme = true) {
                ReviewDiscoveryPage(
                    sources = emptyList(), ownershipCandidates = emptyList(),
                    selectedOwnedKeys = emptySet(), importResult = null, applying = false,
                    onToggleAccount = { _, _ -> },
                    onContinue = { continued = true }
                )
            }
        }
        compose.onNodeWithText("We found these financial sources").assertExists()
        compose.onNodeWithText("+  Add another bank or service").assertExists()
        compose.onNodeWithText("Continue").performClick()
        compose.runOnIdle { assertTrue(continued) }
    }

    @Test fun missingBankDirectoryOpensOnlyWhenRequested() {
        var openCount = 0
        compose.setContent {
            PesaTrackTheme(darkTheme = true) {
                ReviewDiscoveryPage(
                    sources = emptyList(), ownershipCandidates = emptyList(),
                    selectedOwnedKeys = emptySet(), importResult = null, applying = false,
                    onToggleAccount = { _, _ -> }, onContinue = {},
                    onAddMissingInstitution = { openCount++ }
                )
            }
        }
        compose.onNodeWithText("+  Add another bank or service").performClick()
        compose.runOnIdle { assertTrue(openCount == 1) }
    }

    @Test fun optionalSalaryRemainsUnconfirmedUntilRadioChoiceAndContinue() {
        var confirmed = false
        var skipped = false
        val synthetic = OnboardingSalarySuggestion(
            label = "SYNTHETIC PAYROLL",
            accountId = "synthetic-bank",
            occurrenceCount = 3,
            monthCount = 3,
            representativeAmountMinor = 5_200_000L
        )
        compose.setContent {
            PesaTrackTheme(darkTheme = true) {
                OnboardingSalaryChoicePage(
                    suggestion = synthetic, saving = false, error = null,
                    onConfirmDetected = { confirmed = true },
                    onEnterManually = {}, onNotMySalary = {},
                    onSkip = { skipped = true }
                )
            }
        }
        compose.runOnIdle { assertTrue(!confirmed && !skipped) }
        compose.onNodeWithText("Continue").performClick()
        compose.runOnIdle { assertTrue(skipped && !confirmed) }
        compose.onNodeWithText("Yes, set up my salary").performClick()
        compose.onNodeWithText("Continue").performClick()
        compose.runOnIdle { assertTrue(confirmed) }
    }

    @Test fun mpesaLockSavingsToggleRequiresExplicitUserAction() {
        var locked = false
        compose.setContent {
            PesaTrackTheme(darkTheme = false) {
                MpesaConfirmationPage(
                    suggestedPhone = "0712345678",
                    manualMode = false,
                    manualPhone = "",
                    error = null,
                    busy = false,
                    lockedAccountChecked = locked,
                    onLockedAccountChanged = { locked = it },
                    onManualChanged = {},
                    onConfirmSuggested = {},
                    onChooseAnother = {},
                    onSaveManual = {},
                    onSkip = {}
                )
            }
        }
        compose.onNodeWithText("M-Pesa account details").assertExists()
        compose.runOnIdle { assertTrue(!locked) }
        compose.onNodeWithTag("mpesa-lock-savings-toggle").performClick()
        compose.runOnIdle { assertTrue(locked) }
    }
}
