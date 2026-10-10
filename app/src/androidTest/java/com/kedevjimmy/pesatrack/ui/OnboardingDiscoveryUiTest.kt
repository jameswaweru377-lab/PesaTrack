package com.kedevjimmy.pesatrack.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.ui.screens.ReviewDiscoveryPage
import com.kedevjimmy.pesatrack.ui.screens.OnboardingSalaryChoicePage
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingSalarySuggestion
import com.kedevjimmy.pesatrack.ui.screens.OwnershipCandidateCard
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

    @Test fun userCanContinueWithoutFillingSalaryOrAccountForms() {
        var continued = false
        compose.setContent {
            PesaTrackTheme(darkTheme = true) {
                ReviewDiscoveryPage(
                    sources = emptyList(), ownershipCandidates = emptyList(),
                    ownershipChoices = emptyMap(), importResult = null, applying = false,
                    onOwnershipChoice = { _, _ -> },
                    onContinue = { continued = true }
                )
            }
        }
        compose.onNodeWithText("Salary setup comes after confirming your accounts. You can skip it.").assertExists()
        compose.onNodeWithText("Hide account options").performClick()
        compose.onNodeWithText("Continue").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(continued) }
        compose.onNodeWithText("Review account options").performClick()
        compose.onNodeWithText("Add a missing bank or service").assertExists()
    }
    @Test fun accountOwnershipHasExplicitMineNotMineAndUnsureChoices() {
        var decision = OnboardingOwnershipChoice.UNSURE
        val synthetic = DiscoveredFinancialEntity(
            entityKey = "synthetic:recipient",
            displayName = "Example recipient bank",
            providerKey = "SYNTHETIC_PROVIDER",
            accountTypeHint = "BANK",
            ownershipHint = AccountOwnershipHint.UNKNOWN,
            natureHint = FinancialAccountNature.ASSET,
            confirmedAccountId = null,
            firstSeenEpochMs = 1L,
            lastSeenEpochMs = 2L
        )
        compose.setContent {
            PesaTrackTheme(darkTheme = true) {
                OwnershipCandidateCard(
                    entity = synthetic,
                    choice = decision,
                    enabled = true,
                    onChoice = { decision = it }
                )
            }
        }
        compose.onNodeWithTag("ownership-${synthetic.entityKey}-NOT_MINE").performClick()
        compose.runOnIdle { assertEquals(OnboardingOwnershipChoice.NOT_MINE, decision) }
        compose.onNodeWithTag("ownership-${synthetic.entityKey}-UNSURE").performClick()
        compose.runOnIdle { assertEquals(OnboardingOwnershipChoice.UNSURE, decision) }
        compose.onNodeWithTag("ownership-${synthetic.entityKey}-MINE").performClick()
        compose.runOnIdle { assertEquals(OnboardingOwnershipChoice.MINE, decision) }
    }
    @Test fun userCanOpenMissingInstitutionDirectoryOnlyOnDemand() {
        var directoryOpened = 0
        compose.setContent {
            PesaTrackTheme(darkTheme = true) {
                ReviewDiscoveryPage(
                    sources = emptyList(),
                    ownershipCandidates = emptyList(),
                    ownershipChoices = emptyMap(),
                    importResult = null,
                    applying = false,
                    onOwnershipChoice = { _, _ -> },
                    onContinue = {},
                    onAddMissingInstitution = { directoryOpened++ }
                )
            }
        }
        compose.onNodeWithText("Add a missing bank or service").performClick()
        compose.runOnIdle { assertEquals(1, directoryOpened) }
    }

    @Test fun optionalSalaryStepDoesNotAutomaticallyAcceptSuggestedIncome() {
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
                    suggestion = synthetic,
                    saving = false,
                    error = null,
                    onConfirmDetected = { confirmed = true },
                    onEnterManually = {},
                    onNotMySalary = {},
                    onSkip = { skipped = true }
                )
            }
        }
        compose.runOnIdle {
            assertTrue(!confirmed)
            assertTrue(!skipped)
        }
        compose.onNodeWithText("Not now — continue without salary").performClick()
        compose.runOnIdle { assertTrue(skipped); assertTrue(!confirmed) }
        compose.onNodeWithText("Yes, this is my salary").performClick()
        compose.runOnIdle { assertTrue(confirmed) }
    }
}
