package com.kedevjimmy.pesatrack.domain.intelligence

import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory

data class RemoteEntityEnrichment(
    val canonicalName: String,
    val domain: String?,
    val logoUrl: String?,
    val categoryHint: TransactionCategory?,
    val confidence: EntityMatchConfidence,
    val source: String
)

object EntityDescriptorPrivacyPolicy {
    private val whitespace =
        Regex("""\s+""")
    private val transactionCode =
        Regex(
            """\b[A-Z0-9]{10}\b""",
            RegexOption.IGNORE_CASE
        )
    private val phoneNumber =
        Regex(
            """(?:\+?254|0)[17]\d{8}"""
        )
    private val longNumber =
        Regex("""\d{5,}""")
    private val email =
        Regex(
            """[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}""",
            RegexOption.IGNORE_CASE
        )

    private val businessSignals = setOf(
        "bank",
        "sacco",
        "supermarket",
        "mart",
        "shop",
        "store",
        "restaurant",
        "cafe",
        "coffee",
        "hotel",
        "pharmacy",
        "hospital",
        "clinic",
        "hardware",
        "electronics",
        "services",
        "limited",
        "ltd",
        "company",
        "insurance",
        "school",
        "college",
        "university",
        "logistics",
        "transport",
        "energy",
        "power",
        "water",
        "telecom",
        "mobile",
        "internet",
        "mall",
        "fuel",
        "petroleum",
        "airways",
        "airline"
    )

    fun sanitize(
        raw: String?
    ): String? {
        val cleaned =
            raw
                ?.trim()
                ?.replace(whitespace, " ")
                ?.takeIf { it.length in 3..80 }
                ?: return null

        if (
            transactionCode.containsMatchIn(cleaned) ||
            phoneNumber.containsMatchIn(cleaned) ||
            longNumber.containsMatchIn(cleaned) ||
            email.containsMatchIn(cleaned)
        ) {
            return null
        }

        if (looksLikePersonalName(cleaned)) {
            return null
        }

        return cleaned
            .replace(
                Regex("""[^A-Za-z0-9&'’()./+\- ]"""),
                " "
            )
            .replace(whitespace, " ")
            .trim()
            .takeIf { it.length in 3..80 }
    }

    fun cacheKey(
        sanitized: String
    ): String =
        sanitized.lowercase()
            .replace(
                Regex("""[^a-z0-9]+"""),
                " "
            )
            .replace(whitespace, " ")
            .trim()

    private fun looksLikePersonalName(
        value: String
    ): Boolean {
        val normalized =
            value.lowercase()
                .replace(
                    Regex("""[^a-z&'’\- ]"""),
                    " "
                )
                .replace(whitespace, " ")
                .trim()

        val words =
            normalized.split(" ")
                .filter(String::isNotBlank)

        if (words.size !in 2..4) {
            return false
        }

        if (
            words.any {
                it in businessSignals
            }
        ) {
            return false
        }

        if (
            value.any(Char::isDigit)
        ) {
            return false
        }

        return words.all { word ->
            word.length >= 2 &&
                word.all {
                    it.isLetter() ||
                        it == '-' ||
                        it == '\'' ||
                        it == '’'
                }
        }
    }
}

object RemoteEntityEligibilityPolicy {
    fun canSendOnline(
        type: String
    ): Boolean =
        type ==
            MpesaTransactionType.PAY_BILL.name ||
            type ==
            MpesaTransactionType.BUY_GOODS.name
}

object RemoteEntityAutoReviewPolicy {
    private val safeCategories = setOf(
        TransactionCategory.GROCERIES,
        TransactionCategory.EATING_OUT,
        TransactionCategory.TRANSPORT,
        TransactionCategory.ELECTRICITY,
        TransactionCategory.WATER,
        TransactionCategory.INTERNET,
        TransactionCategory.HOME_HOUSEHOLD,
        TransactionCategory.SHOPPING,
        TransactionCategory.HEALTH,
        TransactionCategory.EDUCATION,
        TransactionCategory.ENTERTAINMENT,
        TransactionCategory.PERSONAL_CARE,
        TransactionCategory.PHONE_AND_DATA
    )

    fun shouldAutoReview(
        result: RemoteEntityEnrichment
    ): Boolean =
        result.confidence ==
            EntityMatchConfidence.HIGH &&
            result.categoryHint in safeCategories

    fun categoryFromExternalLabel(
        raw: String?
    ): TransactionCategory? {
        val normalized =
            raw
                ?.lowercase()
                ?.replace(
                    Regex("""[^a-z0-9]+"""),
                    " "
                )
                ?.replace(
                    Regex("""\s+"""),
                    " "
                )
                ?.trim()
                ?: return null

        return when {
            "grocery" in normalized ||
                "groceries" in normalized ||
                "supermarket" in normalized ->
                TransactionCategory.GROCERIES

            "food" in normalized ||
                "restaurant" in normalized ||
                "dining" in normalized ||
                "cafe" in normalized ||
                "coffee" in normalized ->
                TransactionCategory.EATING_OUT

            "transport" in normalized ||
                "transportation" in normalized ||
                "taxi" in normalized ||
                "rideshare" in normalized ||
                "ride share" in normalized ->
                TransactionCategory.TRANSPORT

            "entertainment" in normalized ||
                "streaming" in normalized ||
                "cinema" in normalized ->
                TransactionCategory.ENTERTAINMENT

            "shopping" in normalized ||
                "retail" in normalized ->
                TransactionCategory.SHOPPING

            "health" in normalized ||
                "healthcare" in normalized ||
                "pharmacy" in normalized ->
                TransactionCategory.HEALTH

            "education" in normalized ->
                TransactionCategory.EDUCATION

            "personal care" in normalized ||
                "beauty" in normalized ->
                TransactionCategory.PERSONAL_CARE

            "telecom" in normalized ||
                "mobile" in normalized ||
                "communications" in normalized ->
                TransactionCategory.PHONE_AND_DATA

            else -> null
        }
    }
}
