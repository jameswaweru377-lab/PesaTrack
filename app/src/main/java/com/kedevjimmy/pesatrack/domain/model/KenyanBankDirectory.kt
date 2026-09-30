package com.kedevjimmy.pesatrack.domain.model

data class KenyanBank(
    val id: String,
    val name: String,
    val senderHints: List<String>
)

object KenyanBankDirectory {
    // Independent offline catalogue used only to simplify account setup.
    // The user can always choose "type bank name" when an institution or
    // sender format is missing, so financial logic never depends on this list.
    val banks: List<KenyanBank> = listOf(
        KenyanBank("equity", "Equity Bank Kenya", listOf("EQUITY", "EQUITY BANK")),
        KenyanBank("cooperative", "Co-operative Bank of Kenya", listOf("COOP BANK", "CO-OP BANK", "COOPERATIVE BANK", "COOPBANK")),
        KenyanBank("kcb", "KCB Bank Kenya", listOf("KCB", "KCB BANK")),
        KenyanBank("ncba", "NCBA Bank Kenya", listOf("NCBA", "NCBA BANK")),
        KenyanBank("absa", "Absa Bank Kenya", listOf("ABSA", "ABSA BANK")),
        KenyanBank("stanbic", "Stanbic Bank Kenya", listOf("STANBIC", "STANBIC BANK")),
        KenyanBank("standard-chartered", "Standard Chartered Bank Kenya", listOf("SCB", "STANDARD CHARTERED", "STANCHART")),
        KenyanBank("i-and-m", "I&M Bank Kenya", listOf("I&M", "I&M BANK", "IM BANK", "IMBANK", "IANDBANK", "IANDM BANK")),
        KenyanBank("dtb", "Diamond Trust Bank Kenya", listOf("DTB", "DTB BANK")),
        KenyanBank("family", "Family Bank", listOf("FAMILY BANK")),
        KenyanBank("ecobank", "Ecobank Kenya", listOf("ECOBANK")),
        KenyanBank("bank-of-africa", "Bank of Africa Kenya", listOf("BOA", "BANK OF AFRICA")),
        KenyanBank("citibank", "Citibank N.A. Kenya", listOf("CITI", "CITIBANK")),
        KenyanBank("gulf-african", "Gulf African Bank", listOf("GULF AFRICAN", "GAB")),
        KenyanBank("kingdom", "Kingdom Bank", listOf("KINGDOM BANK")),
        KenyanBank("prime", "Prime Bank Kenya", listOf("PRIME BANK")),
        KenyanBank("sbm", "SBM Bank Kenya", listOf("SBM", "SBM BANK")),
        KenyanBank("sidian", "Sidian Bank", listOf("SIDIAN", "SIDIAN BANK")),
        KenyanBank("victoria", "Victoria Commercial Bank", listOf("VICTORIA BANK", "VCB"))
    )

    fun search(query: String): List<KenyanBank> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return banks
        return banks.filter { bank ->
            bank.name.lowercase().contains(normalized) ||
                bank.senderHints.any { it.lowercase().contains(normalized) }
        }
    }

    fun matchSender(sender: String?): KenyanBank? {
        val normalized =
            sender
                ?.let(::normalizeSender)
                ?.takeIf { it.isNotBlank() }
                ?: return null

        return banks.firstOrNull { bank ->
            bank.senderHints.any { hint ->
                val normalizedHint =
                    normalizeSender(hint)
                if (
                    normalized ==
                    normalizedHint
                ) {
                    true
                } else if (
                    normalized.startsWith(
                        normalizedHint
                    )
                ) {
                    normalized
                        .removePrefix(
                            normalizedHint
                        ) in
                        ALLOWED_SENDER_SUFFIXES
                } else {
                    false
                }
            }
        }
    }

    private fun normalizeSender(
        value: String
    ): String =
        value.uppercase()
            .replace(
                Regex("""[^A-Z0-9]+"""),
                ""
            )

    private val ALLOWED_SENDER_SUFFIXES =
        setOf(
            "",
            "BANK",
            "KENYA",
            "BANKKENYA",
            "BULK",
            "SMS",
            "BULKSMS",
            "ALERT",
            "ALERTS",
            "NOTIFY",
            "NOTIFICATION",
            "MOBILE"
        )

}
