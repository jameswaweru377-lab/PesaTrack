package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.everyday.StatementActivityEvidencePolicy
import com.kedevjimmy.pesatrack.domain.everyday.StatementClassified
import com.kedevjimmy.pesatrack.domain.everyday.StatementFinancialPolicy
import com.kedevjimmy.pesatrack.domain.learning.PersonalLearningPolicy
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection

/**
 * Deliberate, consent-gated reverse category bridge. A PDF by itself is NOT an
 * authenticated canonical SMS event. Without a uniquely corroborated same
 * receipt, Statement corrections stay within Statements.
 */
class StatementCrossCategoryRepository(context: Context) {
    private val app = context.applicationContext
    private val history = StatementActivityStore(app)
    private val activity = TransactionStore.get(app)
    private val categories = CategoryStore.get(app)
    private val learning = RememberCategoryRepository(app)

    data class Preview(
        val activityTransactionId: Long,
        val activityMatches: Int,
        val statementMatches: Int,
        val reportMatches: Int
    )

    private fun corroboratedActivity(
        row: StatementClassified,
        accountId: String,
        categoryId: String
    ): StoredTransaction? {
        if (categories.find(categoryId)?.kind != CategoryKind.EXPENSE ||
            !StatementFinancialPolicy.canAssignCategory(
                row.line, categoryId,
                categories.all().associate { it.id to it.kind.name }
            )) return null
        val receipts = StatementReceiptEvidenceReader(app).read(listOf(row.line))
        val verified = StatementActivityEvidencePolicy.verified(
            row.line, accountId, receipts[row.line.reference].orEmpty()
        ) ?: return null
        val source = activity.getById(verified.transactionId) ?: return null
        if (source.transactionCode != row.line.reference ||
            source.direction != MoneyDirection.EXPENSE.name ||
            source.ruleId != null || source.relationType != null ||
            source.reversalOfCode != null ||
            !PersonalLearningPolicy.isLearnableCategory(
                categoryId, MoneyDirection.EXPENSE
            )) return null
        val eligibleOriginal = activity.categoryLearningCandidates()
            .any { it.id == source.id }
        val eligibleExisting = source.reviewState == "USER_CLASSIFIED" &&
            source.category !in setOf(
                "DEBT_LOAN", "SAVINGS", "INVESTMENT", "TRANSFER", "INCOME", "CASH"
            )
        if (!eligibleOriginal && !eligibleExisting) return null
        // Confirm the receipt is not an institutional or personal debt event.
        val db = PesaTrackDatabase.get(app).readableDatabase
        if (db.rawQuery(
            """SELECT 1 FROM debt_events WHERE linked_transaction_id=?
               UNION ALL SELECT 1 FROM personal_receivable_events
               WHERE linked_transaction_id=? LIMIT 1""",
            arrayOf(source.id.toString(), source.id.toString())
        ).use { it.moveToFirst() }) return null
        return source
    }

    fun preview(
        row: StatementClassified, accountId: String, categoryId: String
    ): Preview? {
        val source = corroboratedActivity(row, accountId, categoryId) ?: return null
        val result = learning.matchingPreview(source, categoryId)
        return Preview(source.id, result.activity, result.statements,
            result.reportStatementMatches)
    }

    fun rememberBoth(
        row: StatementClassified, accountId: String, categoryId: String,
        expectedActivityId: Long
    ): Int {
        val current = corroboratedActivity(row, accountId, categoryId)
            ?: error("The Activity receipt is no longer eligible for shared learning.")
        require(current.id == expectedActivityId) {
            "Transaction evidence has changed. Refresh and review it again."
        }
        // Save the approved Activity preference. The independent Statement reader
        // then resolves the same category through exact name/receipt evidence.
        // Removing the selected row's prior local exception lets future
        // confirmed learning changes flow to the same Statement receipt.
        if (current.reviewState == "USER_CLASSIFIED" &&
            current.category != categoryId) {
            // The user explicitly requested this correction in Statements and
            // verified the exact same cash leg. No structural events are linked.
            activity.classifyManually(
                current.id, categoryId, MoneyDirection.EXPENSE
            )
        }
        val updated = learning.remember(current.id, categoryId)
        // "Remember in both" is shared learning, not an immutable one-off
        // statement category. The row must follow later approved changes.
        history.followSharedCategory(row.batchId, row.line.page, row.line.row)
        return updated
    }
}
