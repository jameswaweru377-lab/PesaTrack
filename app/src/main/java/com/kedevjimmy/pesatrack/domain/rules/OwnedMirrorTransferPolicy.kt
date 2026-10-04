package com.kedevjimmy.pesatrack.domain.rules

data class OwnedMirrorObservation(
    val amountMinor: Long,
    val direction: String,
    val accountId: String?,
    val linkedAccountId: String?,
    val references: Set<String>,
    val protected: Boolean = false
)

data class OwnedMirrorTransfer(val sourceAccountId: String, val destinationAccountId: String)

/** Correlation alone does not prove a transfer; require both owned sides and a shared reference. */
object OwnedMirrorTransferPolicy {
    fun resolve(first: OwnedMirrorObservation, second: OwnedMirrorObservation, ownedIds: Set<String>): OwnedMirrorTransfer? {
        if (first.protected || second.protected || first.amountMinor <= 0L || first.amountMinor != second.amountMinor) return null
        val shared = first.references.mapNotNull(::reference).toSet()
            .intersect(second.references.mapNotNull(::reference).toSet())
        if (shared.isEmpty()) return null
        existingPath(first, second, ownedIds)?.let { return it }
        existingPath(second, first, ownedIds)?.let { return it }
        if (setOf(first.direction, second.direction) != setOf("EXPENSE", "INCOME")) return null
        val outgoing = if (first.direction == "EXPENSE") first else second
        val incoming = if (first.direction == "INCOME") first else second
        val source = outgoing.accountId ?: return null
        val destination = incoming.accountId ?: return null
        if (source == destination || source !in ownedIds || destination !in ownedIds) return null
        return OwnedMirrorTransfer(source, destination)
    }

    private fun existingPath(first: OwnedMirrorObservation, second: OwnedMirrorObservation, ownedIds: Set<String>): OwnedMirrorTransfer? {
        if (first.direction != "TRANSFER") return null
        val source = first.accountId ?: return null
        val destination = first.linkedAccountId ?: return null
        if (source == destination || source !in ownedIds || destination !in ownedIds) return null
        if (second.direction == "EXPENSE" && second.accountId != source) return null
        if (second.direction == "INCOME" && second.accountId != destination) return null
        if (second.direction == "TRANSFER" && (second.accountId != source || second.linkedAccountId != destination)) return null
        if (second.direction !in setOf("EXPENSE", "INCOME", "TRANSFER")) return null
        return OwnedMirrorTransfer(source, destination)
    }

    private fun reference(value: String): String? = value.trim().uppercase().takeIf { it.length >= 5 }
}
