package com.kedevjimmy.pesatrack.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kedevjimmy.pesatrack.domain.identity.*
import com.kedevjimmy.pesatrack.ui.screens.IdentityReviewCard
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OwnershipReviewUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ownershipRequiresAnExplicitDecisionAndNotNowDoesNotConfirm() {
        var confirmed = 0
        var rejected = 0
        var deferred = 0
        compose.setContent {
            PesaTrackTheme {
                IdentityReviewCard(
                    prompt = IdentityReviewPrompt(IdentityReviewPromptType.CONFIRM_SELF_PHONE,
                        "person:sample", "0700000000", IdentityAliasType.PHONE,
                        "0700000000", 2, "Possible own-account transfer"),
                    busy = false, disabled = false,
                    onConfirmSelf = { confirmed++ }, onRejectSelf = { rejected++ },
                    onClassify = {}, onDefer = { deferred++ }
                )
            }
        }
        compose.runOnIdle { assertEquals(0, confirmed); assertEquals(0, rejected) }
        compose.onNodeWithText("Not now").performClick()
        compose.runOnIdle { assertEquals(1, deferred); assertEquals(0, confirmed); assertEquals(0, rejected) }
        compose.onNodeWithText("No, not me").performClick()
        compose.runOnIdle { assertEquals(1, rejected); assertEquals(0, confirmed) }
        compose.onNodeWithText("Yes, this is me").performClick()
        compose.runOnIdle { assertEquals(1, confirmed) }
    }

    @Test fun savingDisablesEveryDecision() {
        compose.setContent {
            PesaTrackTheme {
                IdentityReviewCard(
                    IdentityReviewPrompt(IdentityReviewPromptType.CONFIRM_SELF_NAME,
                        "person:sample", "Sample Person", IdentityAliasType.NAME,
                        "SAMPLE PERSON", 2, "Possible own-account transfer"),
                    busy = true, disabled = true,
                    onConfirmSelf = { error("Must stay disabled") },
                    onRejectSelf = { error("Must stay disabled") },
                    onClassify = {}, onDefer = { error("Must stay disabled") }
                )
            }
        }
        compose.onNodeWithText("Saving…").assertIsNotEnabled()
        compose.onNodeWithText("No, not me").assertIsNotEnabled()
        compose.onNodeWithText("Not now").assertIsNotEnabled()
    }
}
