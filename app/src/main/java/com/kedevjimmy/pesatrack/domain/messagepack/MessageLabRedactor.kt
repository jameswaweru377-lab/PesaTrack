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
    private val uppercaseEntityRegex =
        Regex(
            """\b(sent to|paid to|received from|from|to)[ ]+([A-Z][A-Z0-9 &'’.-]{2,60}?)(?=[ ]+(?:<PHONE>|on|for|at|ref|reference|account)\b|[.,;]|$)"""
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
            body.replace(
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
            uppercaseEntityRegex
                .replace(redacted) {
                    match ->
                    match.groupValues[1] +
                        " <ENTITY>"
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
