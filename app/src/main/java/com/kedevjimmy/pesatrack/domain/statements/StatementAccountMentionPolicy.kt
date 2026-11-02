package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferDetector

/** Read-only clues, never a recipient assertion or permission to change financial records. */
object StatementAccountMentionPolicy {
    class Account(val id: String, val identifier: String?, val isKenyanWallet: Boolean = false)
    class BankRecord(val row: StatementEvidenceCorrelationPolicy.Observation, val description: String)
    data class Hint(val row: StatementEvidenceCorrelationPolicy.Observation, val mentionedAccountId: String) {
        override fun toString(): String = "[private account mention]"
    }

    private val numberToken = Regex("(?<![A-Za-z0-9])[0-9]{8,16}(?![A-Za-z0-9])")
    private val allowedIdentifier = Regex("[+0-9 ()-]+")

    fun find(records: List<BankRecord>, accounts: List<Account>, excludedIds: Set<String>,
             startInclusive: Long = Long.MIN_VALUE, endExclusive: Long = Long.MAX_VALUE): List<Hint> {
        require(endExclusive > startInclusive)
        require(records.size <= 20_000 && accounts.size <= 200)
        require(accounts.map { it.id }.distinct().size == accounts.size)
        require(records.map { it.row.id }.distinct().size == records.size)
        val ownedIds = accounts.mapTo(mutableSetOf()) { it.id }
        val identifiers = accounts.flatMap { account ->
            val raw = account.identifier?.trim().orEmpty()
            if (account.id.isBlank() || !allowedIdentifier.matches(raw)) return@flatMap emptyList()
            val digits = raw.filter { it in '0'..'9' }
            if (digits.length !in 8..16) return@flatMap emptyList()
            val phone = if (account.isKenyanWallet) OwnAccountTransferDetector.normalizeKenyanPhone(raw) else null
            if (account.isKenyanWallet && phone == null) return@flatMap emptyList()
            val aliases = if (phone == null) setOf(digits) else {
                val canonical = phone.removePrefix("+")
                setOf(canonical, "0" + canonical.drop(3), canonical.drop(3))
            }
            aliases.map { it to account }
        }.groupBy({ it.first }, { it.second })
        return records.mapNotNull { record ->
            val row = record.row
            if (row.id.isBlank() || row.sourceId.isBlank() || row.currency.isBlank() || row.id in excludedIds || row.accountId !in ownedIds ||
                row.source != StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT ||
                !row.ownershipConfirmed || !row.sourceTotalsVerified || row.roleProtected ||
                row.occurredAt <= 0 || row.occurredAt !in startInclusive until endExclusive ||
                row.movementMinor == 0L || row.movementMinor == Long.MIN_VALUE) return@mapNotNull null
            val matches = numberToken.findAll(record.description).mapNotNull tokenMatch@ { token ->
                var suffix = token.range.last + 1
                while (suffix < record.description.length && record.description[suffix] in ".-()") suffix++
                if (suffix > token.range.last + 1 && suffix < record.description.length &&
                    record.description[suffix] in '0'..'9') return@tokenMatch null
                var preceding = token.range.first - 1
                val reversedPrefixDigits = StringBuilder()
                while (preceding >= 0) {
                    val char = record.description[preceding]
                    if (!(char in '0'..'9' || char.isWhitespace() || char in "().-")) break
                    if (char in '0'..'9') reversedPrefixDigits.append(char)
                    preceding--
                }
                val internationalPrefix = preceding >= 0 && record.description[preceding] == '+'
                var before = token.range.first - 1
                while (before >= 0 && record.description[before] in ".-()") before--
                if (!internationalPrefix && before < token.range.first - 1 && before >= 0 &&
                    record.description[before] in '0'..'9') return@tokenMatch null
                val prefixDigits = reversedPrefixDigits.reverse().toString()
                val number = when {
                    !internationalPrefix -> token.value
                    prefixDigits.isEmpty() && token.value.length == 12 && token.value.startsWith("254") -> token.value
                    prefixDigits == "254" && token.value.length == 9 && token.value.first() in setOf('1', '7') -> "254" + token.value
                    else -> return@tokenMatch null
                }
                val candidate = identifiers[number].orEmpty().singleOrNull() ?: return@tokenMatch null
                if (internationalPrefix && !candidate.isKenyanWallet) return@tokenMatch null
                candidate.id.takeIf { it != row.accountId }
            }.distinct().toList()
            matches.singleOrNull()?.let { Hint(row, it) }
        }.sortedWith(compareByDescending<Hint> { it.row.occurredAt }.thenBy { it.row.id })
    }
}
