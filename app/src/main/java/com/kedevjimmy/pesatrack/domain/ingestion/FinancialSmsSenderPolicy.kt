package com.kedevjimmy.pesatrack.domain.ingestion

import com.kedevjimmy.pesatrack.domain.model.KenyanBankDirectory
import com.kedevjimmy.pesatrack.domain.model.SafeSmsSenderAddress
import java.util.Locale

/** Sender metadata only: never inspect a message body to admit an SMS. */
object FinancialSmsSenderPolicy {
    // Deliberately exact: a legal lender name alone is not evidence of a real SMS sender.
    // Additional sender IDs can be admitted only by an explicit user decision.
    private val establishedSenders = setOf(
        "MPESA", "SAFARICOM", "MSHWARI", "KCBMPESA", "FULIZA",
        "AIRTELMONEY", "AIRTEL", "TKASH", "TIMIZA", "TALA", "BRANCH",
        "ZENKA", "OKASH", "HUSTLERFUND", "HUSTLERFND", "KWAJAHAZI",
        // Exact sender aliases in the shipped Kenyan message packs.
        "DTBK", "PESALINK", "GLOBALPAY", "EVOPAY", "ETICA", "ETICALTD",
        "SANLAM", "SANLAMUT"
    )
    private val senderPunctuation = Regex("[^A-Z0-9]")

    fun normalize(sender: String?): String =
        sender.takeIf(SafeSmsSenderAddress::isValid).orEmpty()
            .trim()
            .uppercase(Locale.ROOT)
            .replace(senderPunctuation, "")

    fun canApprove(sender: String?): Boolean {
        val normalized = normalize(sender)
        return normalized.length in 3..24 &&
            (normalized.any(Char::isLetter) || normalized.length <= 6)
    }

    fun isTrusted(sender: String?, additional: Set<String> = emptySet()): Boolean {
        val normalized = normalize(sender)
        if (normalized.isEmpty()) return false
        return normalized in establishedSenders ||
            KenyanBankDirectory.matchSender(sender) != null ||
            additional.any { canApprove(it) && normalize(it) == normalized }
    }
}
