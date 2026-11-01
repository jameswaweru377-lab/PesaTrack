package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.material3.MaterialTheme
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kedevjimmy.pesatrack.domain.statements.StatementEvidenceCorrelationPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementMatchReviewPolicy
import org.junit.Rule
import org.junit.Test

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
        compose.onNodeWithText("Invented bank → Invented wallet").assertExists()
        compose.onNodeWithText("Same reference and amount; money out on one account and in on the other.").assertExists()
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "match-review-synthetic.png")
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun noMatchesDoesNotClaimTransactionsAreUnrelated() {
        val review = StatementMatchReviewPolicy.Review(emptyList(), emptyList(), 0)
        compose.setContent { MaterialTheme { StatementMatchReviewContent(2026, emptyMap(), review, false, {}, {}) } }
        compose.onNodeWithText("No strong matches found").assertExists()
        compose.onNodeWithText("Some movements may have different references or missing evidence. This does not mean they are unrelated.").assertExists()
    }
}
