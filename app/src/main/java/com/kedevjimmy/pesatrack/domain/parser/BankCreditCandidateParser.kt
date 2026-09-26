package com.kedevjimmy.pesatrack.domain.parser

import java.math.RoundingMode

data class ParsedBankCreditCandidate(
    val amountMinor: Long,
    val originator: String?
)

object BankCreditCandidateParser {
    private val amountRegex = Regex(
        """(?:KES|Ksh)\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val incomingWords = listOf(
        "credited",
        "credit of",
        "received",
        "deposit",
        "salary",
        "payment received",
        "money in"
    )

    private val originatorRegexes = listOf(
        Regex("""\bfrom\s+([A-Za-z0-9&.'()\- ]{3,60}?)(?=\s+(?:on|ref|reference|account|a/c|balance)\b|[.,;]|$)""", RegexOption.IGNORE_CASE),
        Regex("""\bby\s+([A-Za-z0-9&.'()\- ]{3,60}?)(?=\s+(?:on|ref|reference|account|a/c|balance)\b|[.,;]|$)""", RegexOption.IGNORE_CASE),
        Regex("""\bsender\s*[:\-]\s*([A-Za-z0-9&.'()\- ]{3,60}?)(?=[.,;]|$)""", RegexOption.IGNORE_CASE),
        Regex("""\bemployer\s*[:\-]\s*([A-Za-z0-9&.'()\- ]{3,60}?)(?=[.,;]|$)""", RegexOption.IGNORE_CASE)
    )

    fun parse(body: String): ParsedBankCreditCandidate? {
        val normalized = body.replace(Regex("""\s+"""), " ").trim()
        val lower = normalized.lowercase()
        if (incomingWords.none(lower::contains)) return null

        val amount = amountRegex.find(normalized)?.groupValues?.get(1) ?: return null
        val originator = originatorRegexes
            .asSequence()
            .mapNotNull { regex -> regex.find(normalized)?.groupValues?.get(1) }
            .map(String::trim)
            .map { it.trimEnd('.', ',', ';') }
            .firstOrNull { it.length >= 3 }

        return ParsedBankCreditCandidate(
            amountMinor = amountToMinor(amount),
            originator = originator
        )
    }

    private fun amountToMinor(raw: String): Long =
        raw.replace(",", "")
            .toBigDecimal()
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
}
