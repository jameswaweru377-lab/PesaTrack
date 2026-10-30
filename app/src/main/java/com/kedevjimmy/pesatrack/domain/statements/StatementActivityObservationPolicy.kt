package com.kedevjimmy.pesatrack.domain.statements

import java.util.Locale

/**
 * Admits original message cash legs to read-only Statement comparison.
 * Imported-statement and manually entered Activity rows are not independent
 * corroboration of a PDF and must never appear in this projection.
 */
object StatementActivityObservationPolicy {
    data class ActivityLeg(
        val transactionId: Long,
        val accountId: String?,
        val sourceKind: String,
        val type: String,
        val direction: String,
        val currency: String,
        val amountMinor: Long,
        val occurredAt: Long,
        val reference: String?,
        val reviewState: String,
        val category: String,
        val structurallyLinked: Boolean,
        val reversalOfCode: String?
    )

    private const val MAX_ROWS = 20_000
    private val outgoingTypes = setOf(
        "SENT_MONEY", "PAY_BILL", "BUY_GOODS", "AIRTIME", "WITHDRAWAL"
    )
    private val incomingTypes = setOf("RECEIVED", "DEPOSIT")
    private val protectedCategories = setOf(
        "DEBT_LOAN", "SAVINGS", "INVESTMENT", "REFUND", "FEES", "FEE"
    )

    fun project(
        rows: List<ActivityLeg>,
        ownedAccountIds: Set<String>
    ): List<StatementEvidenceCorrelationPolicy.Observation> {
        require(rows.size <= MAX_ROWS)
        return rows.mapNotNull { row ->
            val source = row.sourceKind.trim().lowercase(Locale.ROOT)
            val originalMessage = ("sms" in source || "notification" in source) &&
                "statement" !in source && "manual" !in source
            val account = row.accountId
            if (!originalMessage || account == null || account !in ownedAccountIds ||
                row.transactionId <= 0L || row.occurredAt <= 0L ||
                row.amountMinor <= 0L || row.currency.isBlank()) return@mapNotNull null

            val direction = row.direction.uppercase(Locale.ROOT)
            val type = row.type.uppercase(Locale.ROOT)
            val sign = when {
                type in outgoingTypes -> -1
                type in incomingTypes -> 1
                type == "OTHER" && direction == "EXPENSE" -> -1
                type == "OTHER" && direction == "INCOME" -> 1
                else -> return@mapNotNull null
            }
            if ((direction == "EXPENSE" && sign > 0) ||
                (direction == "INCOME" && sign < 0) ||
                direction !in setOf("INCOME", "EXPENSE", "TRANSFER")) return@mapNotNull null

            StatementEvidenceCorrelationPolicy.Observation(
                id = "activity:${row.transactionId}",
                source = StatementEvidenceCorrelationPolicy.Source.ACTIVITY,
                sourceId = "activity-ledger",
                accountId = account,
                currency = row.currency.uppercase(Locale.ROOT),
                occurredAt = row.occurredAt,
                movementMinor = if (sign < 0) -row.amountMinor else row.amountMinor,
                reference = row.reference,
                ownershipConfirmed = true,
                roleProtected = row.reviewState == "EXCLUDED" ||
                    row.category in protectedCategories || row.structurallyLinked ||
                    row.reversalOfCode != null
            )
        }.sortedWith(compareBy<StatementEvidenceCorrelationPolicy.Observation> { it.id })
    }
}
