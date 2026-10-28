package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.everyday.*
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatementEvidenceDetailUiTest {
    @get:Rule val compose = createComposeRule()
    private val row = StatementClassified("synthetic",
        StatementActivityLine(1, 1, "AAA0000001", 1_800_000_000_000L,
            "Invented shop purchase", 0L, 4_000_00L, 8_000_00L),
        StatementFlow.EXPENSE, "GROCERIES")

    @Test fun ambiguousReceiptDoesNotOfferActivityLink() {
        compose.setContent { PesaTrackTheme {
            StatementEvidenceDetailPage(row, "M-Pesa", "Groceries", null,
                onReviewCategory = {}, onOpenActivity = {}, onBack = {})
        } }
        compose.onNodeWithText("View Activity record").assertDoesNotExist()
        compose.onNodeWithText("Review category").assertExists()
    }

    @Test fun confirmedReceiptOpensOnlyTheVerifiedId() {
        var opened: Long? = null
        compose.setContent { PesaTrackTheme {
            StatementEvidenceDetailPage(row.copy(activityReceiptVerified = true),
                "M-Pesa", "Groceries", 17L,
                onReviewCategory = {}, onOpenActivity = { opened = it }, onBack = {})
        } }
        compose.onNodeWithText("View Activity record").performClick()
        assertEquals(17L, opened)
    }
}
