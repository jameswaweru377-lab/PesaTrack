package com.kedevjimmy.pesatrack.domain.model

data class KenyanBank(
    val id: String,
    val name: String,
    val senderHints: List<String>
)

object KenyanBankDirectory {
    // Kept as an independent offline catalogue so account setup does not depend on network access.
    // "Other bank" remains available for institutions or sender formats not yet listed.
    val banks: List<KenyanBank> = listOf(
        KenyanBank("absa", "Absa Bank Kenya", listOf("ABSA", "ABSA BANK")),
        KenyanBank("bank-of-africa", "Bank of Africa Kenya", listOf("BOA", "BANK OF AFRICA")),
        KenyanBank("citibank", "Citibank N.A. Kenya", listOf("CITI", "CITIBANK")),
        KenyanBank("cooperative", "Co-operative Bank of Kenya", listOf("COOP BANK", "CO-OP BANK", "COOPERATIVE BANK")),
        KenyanBank("dtb", "Diamond Trust Bank Kenya", listOf("DTB", "DTB BANK")),
        KenyanBank("ecobank", "Ecobank Kenya", listOf("ECOBANK")),
        KenyanBank("equity", "Equity Bank Kenya", listOf("EQUITY", "EQUITY BANK")),
        KenyanBank("family", "Family Bank", listOf("FAMILY BANK")),
        KenyanBank("gulf-african", "Gulf African Bank", listOf("GULF AFRICAN", "GAB")),
        KenyanBank("i-and-m", "I&M Bank Kenya", listOf("I&M", "IM BANK")),
        KenyanBank("kcb", "KCB Bank Kenya", listOf("KCB", "KCB BANK")),
        KenyanBank("kingdom", "Kingdom Bank", listOf("KINGDOM BANK")),
        KenyanBank("ncba", "NCBA Bank Kenya", listOf("NCBA", "NCBA BANK")),
        KenyanBank("prime", "Prime Bank Kenya", listOf("PRIME BANK")),
        KenyanBank("sbm", "SBM Bank Kenya", listOf("SBM", "SBM BANK")),
        KenyanBank("sidian", "Sidian Bank", listOf("SIDIAN", "SIDIAN BANK")),
        KenyanBank("stanbic", "Stanbic Bank Kenya", listOf("STANBIC", "STANBIC BANK")),
        KenyanBank("standard-chartered", "Standard Chartered Bank Kenya", listOf("SCB", "STANDARD CHARTERED", "STANCHART")),
        KenyanBank("victoria", "Victoria Commercial Bank", listOf("VICTORIA BANK", "VCB"))
    ).sortedBy(KenyanBank::name)

    fun search(query: String): List<KenyanBank> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return banks
        return banks.filter { bank ->
            bank.name.lowercase().contains(normalized) ||
                bank.senderHints.any { it.lowercase().contains(normalized) }
        }
    }
}
