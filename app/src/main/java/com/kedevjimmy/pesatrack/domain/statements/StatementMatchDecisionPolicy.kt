package com.kedevjimmy.pesatrack.domain.statements

import java.nio.ByteBuffer
import java.security.MessageDigest

/** Decisions are valid only for the exact evidence and ownership revisions reviewed. */
object StatementMatchDecisionPolicy {
    fun fingerprint(
        observations: List<StatementEvidenceCorrelationPolicy.Observation>,
        ownershipRevisions: Map<String, Long>
    ): String {
        require(observations.size in 2..32)
        require(observations.map { it.id }.distinct().size == observations.size)
        require(observations.all { row ->
            row.id.isNotBlank() && row.sourceId.isNotBlank() && row.accountId.isNotBlank() &&
                row.currency.isNotBlank() && row.occurredAt > 0 && row.movementMinor != 0L &&
                row.movementMinor != Long.MIN_VALUE && row.ownershipConfirmed && !row.roleProtected &&
                (ownershipRevisions[row.accountId] ?: 0L) > 0 &&
                (row.source == StatementEvidenceCorrelationPolicy.Source.ACTIVITY || row.sourceTotalsVerified)
        })
        val digest = MessageDigest.getInstance("SHA-256")
        fun feed(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            digest.update(ByteBuffer.allocate(4).putInt(bytes.size).array())
            digest.update(bytes)
        }
        feed("statement-match-decision-v1")
        observations.sortedBy { it.id }.forEach { row ->
            feed(row.id); feed(row.source.name); feed(row.sourceId); feed(row.accountId)
            feed(row.currency); feed(row.occurredAt.toString()); feed(row.movementMinor.toString())
            feed(if (row.reference == null) "missing" else "present")
            row.reference?.let(::feed)
            feed(row.sourceTotalsVerified.toString())
            feed(requireNotNull(ownershipRevisions[row.accountId]).toString())
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
