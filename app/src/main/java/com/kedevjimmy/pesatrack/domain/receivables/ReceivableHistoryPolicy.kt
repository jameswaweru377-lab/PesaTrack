package com.kedevjimmy.pesatrack.domain.receivables

import java.text.Normalizer
import java.util.Locale

enum class HistoryMatchKind { PHONE, NAME, PARTIAL, CONFLICT, NONE }
data class HistoryMatch(val kind: HistoryMatchKind, val bulkEligible: Boolean) {
    val reason: String get() = when (kind) {
        HistoryMatchKind.PHONE -> "Matching phone number"
        HistoryMatchKind.NAME -> "Matching full name — confirm identity"
        HistoryMatchKind.PARTIAL -> "Partial name — review individually"
        HistoryMatchKind.CONFLICT -> "Same name, different phone — review individually"
        HistoryMatchKind.NONE -> "Search result — confirm this is the same person"
    }
}
object ReceivableHistoryPolicy {
    fun normalizedName(value: String?): String = Normalizer.normalize(value.orEmpty(), Normalizer.Form.NFKC)
        .trim().replace(Regex("[\\s\\p{Z}]+"), " ").lowercase(Locale.ROOT)
    fun normalizedPhone(value: String?): String? {
        val raw = value?.trim()?.takeIf { it.matches(Regex("[+0-9 ()-]+")) } ?: return null
        val digits = raw.filter(Char::isDigit)
        val national = when {
            digits.length == 10 && digits.startsWith("0") -> digits.drop(1)
            digits.length == 12 && digits.startsWith("254") -> digits.drop(3)
            digits.length == 9 -> digits
            else -> return null
        }
        return if (national.length == 9 && national.first() in "17") "254" + national else null
    }
    /**
     * Statement-import transaction descriptions can contain a formatted Kenyan
     * mobile number, but that is weaker evidence than an explicitly parsed phone.
     * Never enable bulk loan selection using an extracted free-text number.
     */
    fun phoneInStatementDescription(description: String?): String? {
        val source = description?.take(300).orEmpty()
        val phonePattern = Regex("""(?<!\d)(?:\+?254|0)[17](?:[\s().-]*\d){8}(?!\d)""")
        val matches = phonePattern.findAll(source)
            .mapNotNull { match -> normalizedPhone(match.value.filter { it.isDigit() || it == '+' }) }
            .distinct().take(2).toList()
        return matches.singleOrNull()
    }

    fun matchStatementDescription(
        name: String, phone: String?, description: String?
    ): HistoryMatch {
        val inferred = phoneInStatementDescription(description)
        val result = match(name, phone, description, inferred)
        // The number came from a PDF description, not a verified identity field.
        return if (result.kind == HistoryMatchKind.PHONE) result.copy(bulkEligible = false) else result
    }

    fun match(name: String, phone: String?, counterparty: String?, counterpartyPhone: String?): HistoryMatch {
        val expectedPhone = normalizedPhone(phone)
        val actualPhone = normalizedPhone(counterpartyPhone)
        val expectedName = normalizedName(name)
        val actualName = normalizedName(counterparty)
        val sameName = expectedName.length >= 2 && expectedName == actualName
        if (expectedPhone != null && actualPhone != null) {
            if (expectedPhone == actualPhone) return HistoryMatch(HistoryMatchKind.PHONE, true)
            if (sameName || (expectedName.length >= 2 && actualName.contains(expectedName)))
                return HistoryMatch(HistoryMatchKind.CONFLICT, false)
            return HistoryMatch(HistoryMatchKind.NONE, false)
        }
        if (sameName) return HistoryMatch(HistoryMatchKind.NAME, false)
        if (expectedName.length >= 2 && actualName.contains(expectedName))
            return HistoryMatch(HistoryMatchKind.PARTIAL, false)
        return HistoryMatch(HistoryMatchKind.NONE, false)
    }
}
