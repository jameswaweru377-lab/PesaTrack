package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferDetector
import java.security.MessageDigest

/** Exact, explicitly directed bank narration only. Unknown grammar remains unknown. */
object TransferRoutePolicy {
    class Route(val observationId: String, val peerAccountId: String, val evidenceDigest: String) {
        override fun toString(): String = "[private transfer route]"
    }

    // A provider/paybill name alone is not a destination account. No partial or masked IDs.
    private val directed = Regex(
        "\\b(to|from)\\s+(account|a/c|m-pesa|mpesa|wallet)\\s*[:#]?\\s*(\\+?[0-9]{8,16})(?![A-Za-z0-9*])",
        RegexOption.IGNORE_CASE)
    private val allowedIdentifier = Regex("[+0-9 ()-]+")

    fun find(row: StatementEvidenceCorrelationPolicy.Observation, description: String,
             ownership: Map<String, StatementMatchDecisionPolicy.Ownership>): Route? {
        if (row.source != StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT ||
            !row.sourceTotalsVerified || !row.ownershipConfirmed || row.roleProtected ||
            row.movementMinor == 0L || row.movementMinor == Long.MIN_VALUE ||
            row.accountId !in ownership || description.length > 4096) return null
        val matches = directed.findAll(description).toList()
        val match = matches.singleOrNull() ?: return null
        if (hasIdentifierContinuation(description, match.range.last + 1)) return null
        val direction = match.groupValues[1]
        if ((row.movementMinor < 0 && !direction.equals("to", true)) ||
            (row.movementMinor > 0 && !direction.equals("from", true))) return null
        val token = match.groupValues[3]
        val peers = ownership.filter { (_, account) ->
            account.revision > 0 && token.removePrefix("+") in aliases(account) &&
                (!token.startsWith("+") || account.type == "MOBILE_WALLET") &&
                (match.groupValues[2].equals("account", true) || match.groupValues[2].equals("a/c", true) ||
                    account.type == "MOBILE_WALLET")
        }.keys
        val peer = peers.singleOrNull()?.takeIf { it != row.accountId } ?: return null
        return Route(row.id, peer, digest(description))
    }

    private fun hasIdentifierContinuation(text: String, endExclusive: Int): Boolean {
        if (endExclusive < text.length && text[endExclusive] in "_/-") return true
        var index = endExclusive
        while (index < text.length && (text[index].isWhitespace() || text[index] in ".()-/_")) index++
        return index < text.length && (text[index].isDigit() || text[index] == '*')
    }

    private fun aliases(account: StatementMatchDecisionPolicy.Ownership): Set<String> {
        val raw = account.identifier?.trim().orEmpty()
        if (!allowedIdentifier.matches(raw)) return emptySet()
        val digits = raw.filter { it in '0'..'9' }
        if (digits.length !in 8..16) return emptySet()
        if (account.type == "BANK") return setOf(digits)
        if (account.type != "MOBILE_WALLET") return emptySet()
        val phone = OwnAccountTransferDetector.normalizeKenyanPhone(raw)?.removePrefix("+") ?: return emptySet()
        return setOf(phone, "0" + phone.drop(3), phone.drop(3))
    }

    internal fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
