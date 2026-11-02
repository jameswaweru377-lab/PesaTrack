package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithTag
import com.kedevjimmy.pesatrack.domain.statements.StatementMatchReviewPolicy
import com.kedevjimmy.pesatrack.data.local.FingerprintConflictReviewItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FingerprintConflictReviewUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun conflictDisplaysBothAmountsAndOpensOriginalWithoutPostingMoney() {
        var opened = -1L
        val example = FingerprintConflictReviewItem(
            existingTransactionId = 7L,
            transactionCode = "SYNTHETIC123",
            originalAmountMinor = 45_000L,
            incomingAmountMinor = 46_000L,
            sourceKind = "notification"
        )
        compose.setContent {
            MaterialTheme { FingerprintConflictReviewSection(listOf(example)) { opened = it } }
        }
        compose.onNodeWithText("Conflicting transaction evidence").assertExists()
        compose.onNodeWithText("Existing Activity: KES 450.00").assertExists()
        compose.onNodeWithText("Incoming notification: KES 460.00").assertExists()
        compose.onNodeWithText("Open Activity record").performClick()
        assertEquals(7L, opened)
    }

    @Test fun statementsReviewIncludesPendingConflict() {
        val example = FingerprintConflictReviewItem(7L, "SYNTHETIC123", 45_000L, 46_000L, "notification")
        compose.setContent {
            MaterialTheme {
                StatementMatchReviewContent(
                    2026, emptyMap(), StatementMatchReviewPolicy.Review(emptyList(), emptyList(), 0),
                    false, {}, {}, conflicts = listOf(example)
                )
            }
        }
        compose.onNodeWithTag("statement-match-review")
            .performScrollToNode(hasText("Conflicting transaction evidence"))
        compose.onNodeWithText("Conflicting transaction evidence").assertExists()
    }
}
