package com.kedevjimmy.pesatrack.domain.model

data class KenyanBank(
    val id: String,
    val name: String,
    val senderHints: List<String>
)

object KenyanBankDirectory {
    // Offline catalogue aligned with the Central Bank of Kenya's July 2026
    // directory of licensed commercial banks. PesaTrack keeps this local so
    // account setup and sender matching still work without a network.
    val banks: List<KenyanBank> = listOf(
        KenyanBank("absa", "ABSA Bank Kenya PLC", listOf("ABSA", "ABSA BANK")),
        KenyanBank("access", "Access Bank (Kenya) PLC", listOf("ACCESS", "ACCESS BANK")),
        KenyanBank("abc", "African Banking Corporation Limited", listOf("ABC BANK", "AFRICAN BANKING CORPORATION")),
        KenyanBank("bank-of-africa", "Bank of Africa Kenya Limited", listOf("BOA", "BANK OF AFRICA", "BOA KENYA")),
        KenyanBank("bank-of-baroda", "Bank of Baroda (Kenya) Limited", listOf("BANK OF BARODA", "BARODA")),
        KenyanBank("bank-of-india", "Bank of India", listOf("BANK OF INDIA", "BOI")),
        KenyanBank("citibank", "Citibank N.A Kenya", listOf("CITI", "CITIBANK")),
        KenyanBank("cib", "Commercial International Bank (CIB) Kenya Limited", listOf("CIB", "CIB KENYA", "COMMERCIAL INTERNATIONAL BANK")),
        KenyanBank("consolidated", "Consolidated Bank of Kenya Limited", listOf("CONSOLIDATED BANK", "CONSOLIDATED")),
        KenyanBank("cooperative", "Co-operative Bank of Kenya Limited", listOf("COOP BANK", "CO-OP BANK", "COOPERATIVE BANK", "COOPBANK")),
        KenyanBank("credit", "Credit Bank PLC", listOf("CREDIT BANK", "CREDITBANK")),
        KenyanBank("development", "Development Bank of Kenya Limited", listOf("DEVELOPMENT BANK", "DBK")),
        KenyanBank("dtb", "Diamond Trust Bank Kenya Limited", listOf("DTB", "DTB BANK", "DIAMOND TRUST BANK")),
        KenyanBank("dib", "DIB Bank Kenya Limited", listOf("DIB", "DIB BANK")),
        KenyanBank("ecobank", "Ecobank Kenya Limited", listOf("ECOBANK", "ECO BANK")),
        KenyanBank("equity", "Equity Bank Kenya Limited", listOf("EQUITY", "EQUITY BANK")),
        KenyanBank("family", "Family Bank Limited", listOf("FAMILY BANK", "FAMILYBANK")),
        KenyanBank("gtbank", "Guaranty Trust Bank (K) Ltd", listOf("GTBANK", "GT BANK", "GUARANTY TRUST BANK")),
        KenyanBank("guardian", "Guardian Bank Limited", listOf("GUARDIAN BANK", "GUARDIAN")),
        KenyanBank("gulf-african", "Gulf African Bank Limited", listOf("GULF AFRICAN", "GULF AFRICAN BANK", "GAB")),
        KenyanBank("habib", "Habib Bank A.G Zurich", listOf("HABIB BANK", "HABIB")),
        KenyanBank("i-and-m", "I&M Bank Limited", listOf("I&M", "I&M BANK", "IM BANK", "IMBANK", "IANDBANK", "IANDM BANK")),
        KenyanBank("kcb", "KCB Bank Kenya Limited", listOf("KCB", "KCB BANK")),
        KenyanBank("kingdom", "Kingdom Bank Limited", listOf("KINGDOM BANK", "KINGDOM")),
        KenyanBank("middle-east", "Middle East Bank (K) Limited", listOf("MIDDLE EAST BANK", "MEB")),
        KenyanBank("m-oriental", "M-Oriental Bank Limited", listOf("M-ORIENTAL", "M ORIENTAL", "MORIENTAL")),
        KenyanBank("national", "National Bank of Kenya Limited", listOf("NATIONAL BANK", "NBK")),
        KenyanBank("ncba", "NCBA Bank Kenya PLC", listOf("NCBA", "NCBA BANK")),
        KenyanBank("paramount", "Paramount Bank Limited", listOf("PARAMOUNT BANK", "PARAMOUNT")),
        KenyanBank("premier", "Premier Bank Kenya Limited", listOf("PREMIER BANK", "PREMIER")),
        KenyanBank("prime", "Prime Bank Limited", listOf("PRIME BANK", "PRIMEBANK")),
        KenyanBank("sbm", "SBM Bank Kenya Limited", listOf("SBM", "SBM BANK")),
        KenyanBank("sidian", "Sidian Bank Limited", listOf("SIDIAN", "SIDIAN BANK")),
        KenyanBank("spire", "Spire Bank Ltd", listOf("SPIRE", "SPIRE BANK")),
        KenyanBank("stanbic", "Stanbic Bank Kenya Limited", listOf("STANBIC", "STANBIC BANK")),
        KenyanBank("standard-chartered", "Standard Chartered Bank Kenya Limited", listOf("SCB", "STANDARD CHARTERED", "STANCHART")),
        KenyanBank("uba", "UBA Kenya Bank Limited", listOf("UBA", "UBA KENYA", "UBA BANK")),
        KenyanBank("victoria", "Victoria Commercial Bank PLC", listOf("VICTORIA BANK", "VICTORIA COMMERCIAL BANK", "VCB"))
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
