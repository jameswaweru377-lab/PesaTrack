package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import com.kedevjimmy.pesatrack.data.local.StatementMatchDecisionStore
import com.kedevjimmy.pesatrack.domain.statements.StatementEvidenceCorrelationPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementMatchReviewPolicy
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class StatementMatchReviewUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun showsTransferEvidenceAndReadOnlyExplanation() {
        fun row(id: String, account: String, movement: Long, source: StatementEvidenceCorrelationPolicy.Source) =
            StatementEvidenceCorrelationPolicy.Observation(id, source, id, account, "KES",
                1_700_000_000_000L, movement, "ABC1234567", true, sourceTotalsVerified = true)
        val bank = row("bank", "bank", -5_000, StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT)
        val wallet = row("wallet", "wallet", 5_000, StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT)
        val review = StatementMatchReviewPolicy.Review(
            listOf(StatementMatchReviewPolicy.Transfer(listOf(bank), listOf(wallet))), emptyList(), 0)
        compose.setContent { MaterialTheme { StatementMatchReviewContent(2026,
            mapOf("bank" to "Invented bank", "wallet" to "Invented wallet"), review, false, {}, {}) } }
        compose.onNodeWithText("Your records stay unchanged").assertExists()
        compose.onNodeWithText("Older messages and M-Pesa statements may not identify your account number. Check the originals in Activity or Statements.").assertExists()
        compose.onNodeWithText("Invented bank → Invented wallet").assertExists()
        compose.onNodeWithText("Same reference and amount; money out on one account and in on the other.").assertExists()
    }

    @Test fun noMatchesDoesNotClaimTransactionsAreUnrelated() {
        val review = StatementMatchReviewPolicy.Review(emptyList(), emptyList(), 0)
        compose.setContent { MaterialTheme { StatementMatchReviewContent(2026, emptyMap(), review, false, {}, {}) } }
        compose.onNodeWithText("No strong matches found").assertExists()
        compose.onNodeWithText("Some movements may have different references or missing evidence. This does not mean they are unrelated.").assertExists()
    }

    @Test fun confirmingMatchAsksBeforeSavingAndExplainsNoFinancialChanges() {
        fun row(id: String, account: String, movement: Long) =
            StatementEvidenceCorrelationPolicy.Observation(id, StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                id, account, "KES", 1_700_000_000_000L, movement, "ABC1234567", true, sourceTotalsVerified = true)
        val out = row("out", "bank", -500)
        val incoming = row("in", "wallet", 500)
        val review = StatementMatchReviewPolicy.Review(listOf(StatementMatchReviewPolicy.Transfer(listOf(out), listOf(incoming))), emptyList(), 0)
        val candidate = StatementMatchDecisionStore.Candidate(StatementMatchDecisionStore.Kind.TRANSFER, listOf(out, incoming), "a".repeat(64))
        val snapshot = StatementMatchDecisionStore.Snapshot(review, listOf(candidate), emptyMap(), false)
        var decision: StatementMatchDecisionStore.Decision? = null
        compose.setContent { MaterialTheme { StatementMatchReviewContent(2026, emptyMap(), review, false, {}, {},
            snapshot = snapshot, onDecision = { _, _, value, _ -> decision = value }) } }
        compose.onNodeWithTag("statement-match-review").performScrollToNode(hasText("Confirm"))
        compose.onNodeWithText("Confirm").performClick()
        compose.onNodeWithText("This saves your review choice. It does not yet change Activity, balances, categories or report totals.").assertExists()
        compose.onNodeWithText("Confirm match").performClick()
        assertEquals(StatementMatchDecisionStore.Decision.CONFIRMED, decision)
    }

    @Test fun walletHistoryRequiresExplicitAccountCheckBeforeConfirming() {
        fun row(id: String, account: String, amount: Long, source: StatementEvidenceCorrelationPolicy.Source) =
            StatementEvidenceCorrelationPolicy.Observation(id, source, id, account, "KES",
                1_700_000_000_000L, amount, "ABC1234567", true, sourceTotalsVerified = true)
        val out = row("bank", "bank", -500, StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT)
        val incoming = row("wallet", "wallet", 500, StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT)
        val review = StatementMatchReviewPolicy.Review(listOf(StatementMatchReviewPolicy.Transfer(listOf(out), listOf(incoming))), emptyList(), 0)
        val candidate = StatementMatchDecisionStore.Candidate(StatementMatchDecisionStore.Kind.TRANSFER, listOf(out, incoming), "a".repeat(64))
        val snapshot = StatementMatchDecisionStore.Snapshot(review, listOf(candidate), emptyMap(), false)
        var attested = false
        compose.setContent { MaterialTheme { StatementMatchReviewContent(2026, emptyMap(), review, false, {}, {},
            snapshot = snapshot, onDecision = { _, _, _, value -> attested = value }) } }
        compose.onNodeWithTag("statement-match-review").performScrollToNode(hasText("Confirm"))
        compose.onNodeWithText("Confirm").performClick()
        compose.onNodeWithText("Confirm match").assertIsNotEnabled()
        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText("Confirm match").performClick()
        assertEquals(true, attested)
    }
}
