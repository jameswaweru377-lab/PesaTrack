package com.kedevjimmy.pesatrack.domain.intelligence

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.rules.EvidenceConfidence

data class SanitizedEntityDescriptor(
    val value: String
)

object EntityDescriptorSanitizer {
    private val phoneRegex =
        Regex("""(?:\+?254|0)[17]\d{8}""")
    private val transactionCodeRegex =
        Regex("""\b[A-Z0-9]{10}\b""")
    private val moneyRegex =
        Regex(
            """\b(?:KES|KSH)\s*[0-9,]+(?:\.\d{1,2})?\b""",
            RegexOption.IGNORE_CASE
        )

    fun from(
        type: MpesaTransactionType,
        counterparty: String?
    ): SanitizedEntityDescriptor? {
        if (
            type != MpesaTransactionType.PAY_BILL &&
            type != MpesaTransactionType.BUY_GOODS
        ) {
            return null
        }

        val raw =
            counterparty
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: return null

        val cleaned =
            raw.replace(phoneRegex, " ")
                .replace(transactionCodeRegex, " ")
                .replace(moneyRegex, " ")
                .replace(
                    Regex("""[\r\n\t]+"""),
                    " "
                )
                .replace(
                    Regex("""\s+"""),
                    " "
                )
                .trim(
                    ' ',
                    '.',
                    ',',
                    ':',
                    ';',
                    '-',
                    '_'
                )
                .take(96)

        if (
            cleaned.length < 3 ||
            cleaned.count(Char::isLetter) < 2
        ) {
            return null
        }

        return SanitizedEntityDescriptor(
            value = cleaned
        )
    }

    fun key(
        descriptor: String
    ): String =
        descriptor.lowercase()
            .replace(
                Regex("""[^a-z0-9]+"""),
                " "
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()
}

data class OnlineEntityProfile(
    val canonicalName: String,
    val domain: String?,
    val logoUrl: String?,
    val remoteCategory: String?,
    val confidence: EntityMatchConfidence,
    val source: String
)

data class OnlineEntityClassification(
    val direction: MoneyDirection,
    val category: TransactionCategory,
    val reviewState: ReviewState,
    val displayLabel: String,
    val evidenceLabel: String,
    val evidenceSource: String,
    val evidenceConfidence: EvidenceConfidence
)

object OnlineEntityCategoryPolicy {
    fun map(
        remoteCategory: String?,
        canonicalName: String
    ): TransactionCategory? {
        val category =
            normalize(remoteCategory.orEmpty())
        val name =
            normalize(canonicalName)

        if (
            category.contains("finance") ||
            category.contains("bank") ||
            category.contains("loan") ||
            category.contains("lending") ||
            category.contains("insurance")
        ) {
            return null
        }

        return when {
            category.contains("electric") ->
                TransactionCategory.ELECTRICITY

            category.contains("grocery") ||
                category.contains("supermarket") ->
                TransactionCategory.GROCERIES

            category.contains("food") ||
                category.contains("restaurant") ||
                category.contains("dining") ||
                category.contains("cafe") ->
                TransactionCategory.EATING_OUT

            category.contains("transport") ||
                category.contains("taxi") ||
                category.contains("ride") ->
                TransactionCategory.TRANSPORT

            category.contains("shopping") ||
                category.contains("retail") ->
                TransactionCategory.SHOPPING

            category.contains("entertainment") ||
                category.contains("streaming") ->
                TransactionCategory.ENTERTAINMENT

            category.contains("health") ||
                category.contains("medical") ||
                category.contains("pharmacy") ->
                TransactionCategory.HEALTH

            category.contains("education") ||
                category.contains("school") ->
                TransactionCategory.EDUCATION

            category.contains("telecom") ||
                category.contains("mobile") ||
                category.contains("phone") ->
                TransactionCategory.PHONE_AND_DATA

            category.contains("internet") ||
                category.contains("broadband") ->
                TransactionCategory.INTERNET

            category.contains("utility") &&
                (
                    name.contains("electric") ||
                        name.contains("power")
                    ) ->
                TransactionCategory.ELECTRICITY

            category.contains("utility") &&
                name.contains("water") ->
                TransactionCategory.WATER

            else -> null
        }
    }

    private fun normalize(
        value: String
    ): String =
        value.lowercase()
            .replace(
                Regex("""[^a-z0-9]+"""),
                " "
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()
}

object OnlineEntityAutoReviewPolicy {
    fun classify(
        type: MpesaTransactionType,
        profile: OnlineEntityProfile
    ): OnlineEntityClassification? {
        if (
            type != MpesaTransactionType.PAY_BILL &&
            type != MpesaTransactionType.BUY_GOODS
        ) {
            return null
        }

        if (
            profile.confidence !=
            EntityMatchConfidence.HIGH ||
            profile.domain.isNullOrBlank()
        ) {
            return null
        }

        val category =
            OnlineEntityCategoryPolicy.map(
                remoteCategory =
                    profile.remoteCategory,
                canonicalName =
                    profile.canonicalName
            ) ?: return null

        val display =
            profile.canonicalName
                .trim()
                .takeIf(String::isNotBlank)
                ?: return null

        return OnlineEntityClassification(
            direction = MoneyDirection.EXPENSE,
            category = category,
            reviewState =
                ReviewState.AUTO_CLASSIFIED,
            displayLabel = display,
            evidenceLabel = display,
            evidenceSource =
                "entity_intelligence:online:" +
                    profile.source,
            evidenceConfidence =
                EvidenceConfidence.HIGH
        )
    }
}
