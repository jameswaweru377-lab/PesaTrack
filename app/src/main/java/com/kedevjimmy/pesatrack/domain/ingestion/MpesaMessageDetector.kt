package com.kedevjimmy.pesatrack.domain.ingestion

object MpesaMessageDetector {
    private val confirmationRegex = Regex(
        """\b[A-Z0-9]{10}\s+Confirmed\b""",
        RegexOption.IGNORE_CASE
    )

    fun isLikelyMpesa(title: String?, body: String): Boolean {
        val normalizedTitle = title.orEmpty()
            .replace("-", "")
            .replace(" ", "")
            .uppercase()

        return normalizedTitle == "MPESA" ||
            body.contains("M-PESA", ignoreCase = true) ||
            body.contains("MPESA", ignoreCase = true) ||
            confirmationRegex.containsMatchIn(body)
    }
}
