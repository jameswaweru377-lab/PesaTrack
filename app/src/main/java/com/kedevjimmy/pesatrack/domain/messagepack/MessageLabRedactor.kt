package com.kedevjimmy.pesatrack.domain.messagepack

data class RedactedMessageShape(
    val sender: String,
    val body: String
)

object MessageLabRedactor {
    private val transactionCodeRegex =
        Regex("""\b[A-Z0-9]{10}\b""")
    private val phoneRegex =
        Regex(
            """(?<![0-9])(?:\+?254|0)7[0-9*]{8,9}(?![0-9])"""
        )
    private val emailRegex =
        Regex(
            """[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"""
        )
    private val moneyRegex =
        Regex(
            """(?i)\b(KSHS?[.]?|KES|SHS?[.]?)[ ]*([0-9][0-9,]*(?:[.][0-9]{1,2})?)"""
        )
    private val dateRegex =
        Regex(
            """(?<![0-9])[0-9]{1,2}[/\-][0-9]{1,2}[/\-][0-9]{2,4}(?![0-9])"""
        )
    private val timeRegex =
        Regex(
            """(?i)(?<![0-9])[0-9]{1,2}:[0-9]{2}(?:[ ]*[AP]M)?(?![0-9])"""
        )
    private val longNumberRegex =
        Regex(
            """(?<![0-9])[0-9]{4,}(?![0-9])"""
        )
    private val contextualEntityRegex =
        Regex(
            """(?i)\b(sent to|paid to|received from|transferred from|transferred to|credited to|debited from|from|to)[ ]+(.+?)(?=[ ]+(?:<PHONE>|on|for|at|ref|reference|account|balance|has|was|is)\b|[.,;]|$)"""
        )
    private val salutationRegex =
        Regex(
            """(?i)\b(dear|hello|hi)[ ]+([A-Z][A-Za-z'’.-]+(?:[ ]+[A-Z][A-Za-z'’.-]+){0,2})(?=[,.:;])"""
        )

    private val protectedProducts =
        listOf(
            Regex(
                """(?i)Fuliza[ ]+M-PESA"""
            ) to "<PRODUCT_FULIZA>",
            Regex(
                """(?i)M-Shwari[ ]+Lock[ ]+Savings"""
            ) to "<PRODUCT_MSHWARI_LOCK>",
            Regex(
                """(?i)KCB[ ]+M-PESA"""
            ) to "<PRODUCT_KCB_MPESA>",
            Regex(
                """(?i)M-Shwari"""
            ) to "<PRODUCT_MSHWARI>",
            Regex(
                """(?i)M-PESA"""
            ) to "<PRODUCT_MPESA>",
            Regex(
                """(?i)PesaLink"""
            ) to "<PRODUCT_PESALINK>"
        )

    fun redact(
        safeSenderLabel: String?,
        body: String
    ): RedactedMessageShape {
        val sender =
            safeSenderLabel
                ?.trim()
                ?.takeIf(
                    String::isNotBlank
                )
                ?.take(80)
                ?: "<SENDER>"

        var redacted =
            body

        protectedProducts.forEach {
            (pattern, replacement) ->
            redacted =
                redacted.replace(
                    pattern,
                    replacement
                )
        }

        redacted =
            redacted.replace(
                transactionCodeRegex,
                "<TX_CODE>"
            )
                .replace(
                    phoneRegex,
                    "<PHONE>"
                )
                .replace(
                    emailRegex,
                    "<EMAIL>"
                )
                .replace(
                    moneyRegex
                ) { match ->
                    match.groupValues[1] +
                        " <AMOUNT>"
                }
                .replace(
                    dateRegex,
                    "<DATE>"
                )
                .replace(
                    timeRegex,
                    "<TIME>"
                )
                .replace(
                    longNumberRegex,
                    "<NUMBER>"
                )

        redacted =
            salutationRegex
                .replace(redacted) {
                    match ->
                    match.groupValues[1] +
                        " <PERSON>"
                }

        redacted =
            contextualEntityRegex
                .replace(redacted) {
                    match ->
                    val entity =
                        match.groupValues[2]
                    if (
                        "<PRODUCT_" in
                        entity
                    ) {
                        match.value
                    } else {
                        match.groupValues[1] +
                            " <ENTITY>"
                    }
                }

        protectedProducts.forEach {
            (_, replacement) ->
            val display =
                when (replacement) {
                    "<PRODUCT_FULIZA>" ->
                        "Fuliza M-PESA"
                    "<PRODUCT_MSHWARI_LOCK>" ->
                        "M-Shwari Lock Savings"
                    "<PRODUCT_KCB_MPESA>" ->
                        "KCB M-PESA"
                    "<PRODUCT_MSHWARI>" ->
                        "M-Shwari"
                    "<PRODUCT_MPESA>" ->
                        "M-PESA"
                    "<PRODUCT_PESALINK>" ->
                        "PesaLink"
                    else ->
                        replacement
                }
            redacted =
                redacted.replace(
                    replacement,
                    display
                )
        }

        redacted =
            redacted
                .replace(
                    Regex("""[ ]{2,}"""),
                    " "
                )
                .trim()

        return RedactedMessageShape(
            sender = sender,
            body = redacted
        )
    }
}
