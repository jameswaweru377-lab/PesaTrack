package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferDetector

/** Never treat two owned account records for one full identifier as two cash accounts. */
internal object StatementOwnedAccountUniquenessPolicy {
    private data class Identity(val type: String, val number: String)
    private val allowedNumberFormatting = Regex("[+0-9 ()-]+")

    fun unambiguous(
        ownership: Map<String, StatementMatchDecisionPolicy.Ownership>
    ): Map<String, StatementMatchDecisionPolicy.Ownership> {
        val identities = ownership.mapNotNull { (id, account) ->
            val raw = account.identifier?.trim().orEmpty()
            if (!allowedNumberFormatting.matches(raw)) return@mapNotNull null
            val number = when (account.type) {
                "BANK" -> raw.filter { it in '0'..'9' }.takeIf { it.length in 8..16 }
                "MOBILE_WALLET" -> OwnAccountTransferDetector.normalizeKenyanPhone(raw)
                else -> null
            } ?: return@mapNotNull null
            id to Identity(account.type, number)
        }
        val duplicateIds = identities.groupBy({ it.second }, { it.first }).values
            .filter { it.size > 1 }.flatten().toSet()
        val completeIdentityIds = identities.mapTo(mutableSetOf()) { it.first }
        return ownership.filterKeys { it in completeIdentityIds && it !in duplicateIds }
    }
}
