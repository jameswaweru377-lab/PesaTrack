package com.kedevjimmy.pesatrack.domain.interpretation

data class NormalizedFinancialMessage(
    val title: String?,
    val body: String,
    val hasCurrencyHint: Boolean
)

object FinancialMessageNormalizer {
    private val whitespaceRegex = Regex("""\s+""")

    /*
     * Normalize Kenyan currency typography before any semantic
     * interpretation. Provider-specific parsers must not depend on
     * whether an institution wrote KES, Kshs, Sh or Kenya Shillings.
     *
     * The look-ahead for a numeric amount prevents ordinary words
     * beginning with "sh" from being treated as currency.
     */
    private val prefixedKenyanCurrencyRegex = Regex(
        """\b(?:KES|KSHS?\.?|SHS?\.?|KENYA\s+SHILLINGS?)\s*[:\-]?\s*(?=[0-9])""",
        RegexOption.IGNORE_CASE
    )

    private val suffixedKenyanCurrencyRegex = Regex(
        """\b([0-9][0-9,]*(?:\.\d{1,2})?)\s*(?:KES|KSHS?\.?|SHS?\.?|KENYA\s+SHILLINGS?)\b""",
        RegexOption.IGNORE_CASE
    )

    private val canonicalCurrencyHintRegex = Regex(
        """\bKES(?=\s*[0-9])""",
        RegexOption.IGNORE_CASE
    )

    fun normalize(
        title: String?,
        body: String
    ): NormalizedFinancialMessage {
        val normalizedBody =
            normalizeKenyanCurrency(body)
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
                canonicalCurrencyHintRegex
                    .containsMatchIn(normalizedBody)
        )
    }

    fun normalizeKenyanCurrency(
        text: String
    ): String {
        val suffixNormalized =
            suffixedKenyanCurrencyRegex.replace(
                text
            ) { match ->
                "KES ${match.groupValues[1]}"
            }

        return prefixedKenyanCurrencyRegex.replace(
            suffixNormalized
        ) {
            "KES "
        }
    }
}
