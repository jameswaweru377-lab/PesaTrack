package com.kedevjimmy.pesatrack.domain.interpretation

data class NormalizedFinancialMessage(
    val title: String?,
    val body: String,
    val hasCurrencyHint: Boolean
)

object FinancialMessageNormalizer {
    private val whitespaceRegex = Regex("""\s+""")
    private val currencyHintRegex = Regex(
        """\b(?:Ksh|KES)\b""",
        RegexOption.IGNORE_CASE
    )

    fun normalize(
        title: String?,
        body: String
    ): NormalizedFinancialMessage {
        val normalizedBody = body
            .replace(whitespaceRegex, " ")
            .trim()
        val normalizedTitle = title
            ?.replace(whitespaceRegex, " ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        return NormalizedFinancialMessage(
            title = normalizedTitle,
            body = normalizedBody,
            hasCurrencyHint =
                currencyHintRegex.containsMatchIn(
                    normalizedBody
                )
        )
    }
}
