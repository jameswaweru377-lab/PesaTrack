package com.kedevjimmy.pesatrack.domain.parser

import java.math.RoundingMode

data class ParsedFulizaEvent(
    val transactionCode: String?,
    val financedAmountMinor: Long,
    val accessFeeMinor: Long,
    val outstandingAmountMinor: Long?,
    val availableLimitMinor: Long?,
    val rawMessage: String
)

object FulizaParser {
    private val codeRegex = Regex(
        """\b([A-Z0-9]{10})\s+Confirmed\b""",
        RegexOption.IGNORE_CASE
    )
    private val financedRegex = Regex(
        """Fuliza\s+M-PESA\s+amount\s*(?:is\s*)?(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val accessFeeRegex = Regex(
        """Access\s+Fee\s*(?:(?:is\s+)?charged|charge(?:d)?(?:\s+of)?|of)?\s*(?:Ksh|KES)?\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val outstandingRegex = Regex(
        """Total\s+Fuliza\s+M-PESA\s+outstanding\s+amount\s*(?:is\s*)?(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val availableLimitRegex = Regex(
        """Available\s+Fuliza\s+M-PESA\s+limit\s*(?:is\s*)?(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(message: String): ParsedFulizaEvent? {
        val normalized = message.replace(Regex("""\s+"""), " ").trim()
        val financed = financedRegex.find(normalized)?.groupValues?.get(1) ?: return null

        return ParsedFulizaEvent(
            transactionCode = codeRegex.find(normalized)?.groupValues?.get(1)?.uppercase(),
            financedAmountMinor = amountToMinor(financed),
            accessFeeMinor = accessFeeRegex.find(normalized)
                ?.groupValues
                ?.get(1)
                ?.let(::amountToMinor)
                ?: 0L,
            outstandingAmountMinor = outstandingRegex.find(normalized)
                ?.groupValues
                ?.get(1)
                ?.let(::amountToMinor),
            availableLimitMinor = availableLimitRegex.find(normalized)
                ?.groupValues
                ?.get(1)
                ?.let(::amountToMinor),
            rawMessage = message
        )
    }

    fun isFulizaDrawdown(message: String): Boolean =
        financedRegex.containsMatchIn(message)

    private fun amountToMinor(raw: String): Long =
        raw.replace(",", "")
            .toBigDecimal()
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
}
