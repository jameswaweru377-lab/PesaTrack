package com.kedevjimmy.pesatrack.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.ui.screens.ReviewDiscoveryPage
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
                    ownershipChoices = emptyMap(), salarySuggestion = null,
                    salaryAccepted = false, importResult = null, applying = false,
                    onSalaryAccepted = {}, onOwnershipChoice = { _, _ -> }, onManualSalary = {},
                    onContinue = { continued = true }
                )
            }
        }
        compose.onNodeWithText("Set up salary manually").assertExists()
        compose.onNodeWithText("Hide optional setup").performClick()
        compose.onNodeWithText("Set up salary manually").assertDoesNotExist()
        compose.onNodeWithText("Continue").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(continued) }
        compose.onNodeWithText("Personalize accounts and salary (optional)").performClick()
        compose.onNodeWithText("Set up salary manually").assertExists()
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
}
