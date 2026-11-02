package com.kedevjimmy.pesatrack.domain.statements

import java.util.Locale

/**
 * A non-posting accounting projection over current, explicitly confirmed review evidence.
 * Counts cash legs rather than source observations; never mutates Activity or Statements.
 */
object StatementReconciledLegProjectionPolicy {
    data class Projection(
        val observationCount: Int,
        val uniqueCashLegCount: Int,
        val confirmedSameLegGroups: Int,
        val confirmedInternalTransfers: Int,
        val confirmedInternalTransferMinor: Long,
        val ambiguousObservationCount: Int,
        /** An internal transfer never creates earned income or external spending. */
        val externalIncomeFromConfirmedTransfersMinor: Long = 0L,
        val externalSpendingFromConfirmedTransfersMinor: Long = 0L
    )

    private const val MAX_GAP_MS = 48L * 60L * 60L * 1000L
    private val stableReference = Regex("[A-Z0-9]{8,48}")

    private fun safeLeg(rows: List<StatementEvidenceCorrelationPolicy.Observation>): Boolean {
        if (rows.isEmpty()) return false
        val first = rows.first()
        val reference = first.reference?.trim()?.uppercase(Locale.ROOT) ?: return false
        if (!stableReference.matches(reference) || reference.none(Char::isDigit) ||
            first.movementMinor == 0L || first.movementMinor == Long.MIN_VALUE ||
            first.occurredAt <= 0L || first.accountId.isBlank() || first.currency.isBlank()) return false
        if (rows.count { it.source == StatementEvidenceCorrelationPolicy.Source.ACTIVITY } > 1 ||
            rows.filter { it.source != StatementEvidenceCorrelationPolicy.Source.ACTIVITY }
                .map { it.sourceId }.distinct().size !=
                rows.count { it.source != StatementEvidenceCorrelationPolicy.Source.ACTIVITY }) return false
        if (rows.maxOf { it.occurredAt } - rows.minOf { it.occurredAt } > MAX_GAP_MS) return false
        return rows.all {
            it.id.isNotBlank() && it.sourceId.isNotBlank() && it.ownershipConfirmed && !it.roleProtected &&
                it.accountId == first.accountId && it.movementMinor == first.movementMinor &&
                it.currency.equals(first.currency, ignoreCase = true) &&
                it.reference?.trim()?.uppercase(Locale.ROOT) == reference && it.occurredAt > 0L &&
                (it.source == StatementEvidenceCorrelationPolicy.Source.ACTIVITY || it.sourceTotalsVerified)
        }
    }

    fun project(review: StatementMatchReviewPolicy.Review,
                decisions: Map<Set<String>, StatementReconciliationPreviewPolicy.Decision>): Projection {
        val used = mutableSetOf<String>()
        var observations = 0
        var legs = 0
        var copies = 0
        var transfers = 0
        var transferMinor = 0L

        for (group in review.sameMovements) {
            val ids = group.map { it.id }.toSet()
            if (ids.size < 2 || ids.size != group.size || ids.any(used::contains) || !safeLeg(group) ||
                decisions[ids] != StatementReconciliationPreviewPolicy.Decision.CONFIRMED) continue
            used.addAll(ids)
            observations = Math.addExact(observations, group.size)
            legs = Math.addExact(legs, 1)
            copies = Math.addExact(copies, 1)
        }
        for (transfer in review.transfers) {
            val out = transfer.outgoing
            val incoming = transfer.incoming
            val rows = out + incoming
            val ids = rows.map { it.id }.toSet()
            if (ids.size != rows.size || ids.any(used::contains) ||
                decisions[ids] != StatementReconciliationPreviewPolicy.Decision.CONFIRMED ||
                !safeLeg(out) || !safeLeg(incoming)) continue
            val debit = out.first()
            val credit = incoming.first()
            if (debit.movementMinor >= 0 || credit.movementMinor <= 0 ||
                debit.accountId == credit.accountId ||
                !debit.currency.equals("KES", ignoreCase = true) ||
                !credit.currency.equals(debit.currency, ignoreCase = true) ||
                debit.reference?.trim()?.uppercase(Locale.ROOT) !=
                    credit.reference?.trim()?.uppercase(Locale.ROOT) ||
                -debit.movementMinor != credit.movementMinor ||
                rows.maxOf { it.occurredAt } - rows.minOf { it.occurredAt } > MAX_GAP_MS) continue
            val updatedTransfer = try { Math.addExact(transferMinor, credit.movementMinor) }
                catch (_: ArithmeticException) { continue }
            used.addAll(ids)
            transferMinor = updatedTransfer
            observations = Math.addExact(observations, rows.size)
            legs = Math.addExact(legs, 2)
            transfers = Math.addExact(transfers, 1)
        }
        return Projection(observations, legs, copies, transfers, transferMinor, review.ambiguousCount)
    }
}
