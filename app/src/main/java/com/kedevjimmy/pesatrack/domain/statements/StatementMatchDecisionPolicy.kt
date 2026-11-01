package com.kedevjimmy.pesatrack.domain.statements

import java.nio.ByteBuffer
import java.security.MessageDigest

/** Decisions are valid only for the exact evidence and ownership revisions reviewed. */
object StatementMatchDecisionPolicy {
    data class Ownership(val revision: Long, val type: String, val identifier: String?, val name: String)
    fun fingerprint(
        observations: List<StatementEvidenceCorrelationPolicy.Observation>,
        ownership: Map<String, Ownership>
    ): String {
        require(observations.size in 2..32)
        require(observations.map { it.id }.distinct().size == observations.size)
        require(observations.all { row ->
            row.id.isNotBlank() && row.sourceId.isNotBlank() && row.accountId.isNotBlank() &&
                row.currency.isNotBlank() && row.occurredAt > 0 && row.movementMinor != 0L &&
                row.movementMinor != Long.MIN_VALUE && row.ownershipConfirmed && !row.roleProtected &&
                (ownership[row.accountId]?.revision ?: 0L) > 0 &&
                !ownership[row.accountId]?.type.isNullOrBlank() && !ownership[row.accountId]?.name.isNullOrBlank() &&
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
            val account = requireNotNull(ownership[row.accountId])
            feed(account.revision.toString()); feed(account.type); feed(account.name)
            feed(if (account.identifier == null) "missing" else "present")
            account.identifier?.let(::feed)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
