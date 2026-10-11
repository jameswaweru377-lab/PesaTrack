package com.kedevjimmy.pesatrack.domain.identity

import com.kedevjimmy.pesatrack.domain.intelligence.EntityKind
import java.security.MessageDigest

object IdentityGraphPolicy {
    const val SELF_ID = "self:primary"
    const val SELF_NAME_PROMPT_MIN_OBSERVATIONS = 2
    const val CLASSIFY_PROMPT_MIN_OBSERVATIONS = 3

    fun normalizeName(
        raw: String?
    ): String? {
        val normalized =
            raw.orEmpty()
                .uppercase(java.util.Locale.ROOT)
                .replace(
                    Regex("""[^A-Z0-9&'’]+"""),
                    " "
                )
                .replace(
                    Regex("""\s+"""),
                    " "
                )
                .trim()

        return normalized
            .takeIf {
                it.isNotBlank()
            }
    }

    fun normalizeKenyanPhone(
        raw: String?
    ): String? {
        if (raw.isNullOrBlank() || raw.any { !it.isDigit() && it !in " +()-" }) return null
        val digits =
            raw.orEmpty()
                .filter(Char::isDigit)

        val normalized =
            when {
                digits.length == 12 &&
                    digits.startsWith("254") ->
                    "0" +
                        digits.takeLast(9)

                digits.length == 10 &&
                    digits.startsWith("0") ->
                    digits

                digits.length == 9 &&
                    (digits.startsWith("7") || digits.startsWith("1")) ->
                    "0$digits"

                else -> return null
            }

        return normalized
            .takeIf {
                it.matches(
                    Regex("""0[17]\d{8}""")
                )
            }
    }

    fun accountIdentityId(
        accountId: String
    ): String =
        "account:" +
            accountId.trim()

    fun counterpartyIdentityId(
        name: String?,
        phone: String?,
        knownEntityId: String? = null
    ): String {
        knownEntityId
            ?.trim()
            ?.takeIf(
                String::isNotBlank
            )
            ?.let {
                return "catalog:$it"
            }

        normalizeKenyanPhone(phone)
            ?.let {
                return "counterparty:phone:" +
                    shortHash(it)
            }

        val normalizedName =
            normalizeName(name)
                ?: return "counterparty:unknown:" +
                    shortHash(
                        name.orEmpty()
                    )

        return "counterparty:name:" +
            shortHash(normalizedName)
    }

    fun nodeKindFor(
        entityKind: EntityKind?
    ): IdentityNodeKind =
        when (entityKind) {
            EntityKind.UTILITY,
            EntityKind.TELECOM ->
                IdentityNodeKind.BILLER

            EntityKind.BANK,
            EntityKind.SACCO,
            EntityKind.LENDER,
            EntityKind.SAVINGS_PRODUCT ->
                IdentityNodeKind
                    .FINANCIAL_INSTITUTION

            EntityKind.SUPERMARKET,
            EntityKind.RESTAURANT,
            EntityKind.TRANSPORT,
            EntityKind.ENTERTAINMENT,
            EntityKind.MERCHANT ->
                IdentityNodeKind.BUSINESS

            null ->
                IdentityNodeKind.UNKNOWN
        }

    // Naming a recurring recipient is optional personalization, not a financial error.
    fun requiresFinancialAttention(prompt: IdentityReviewPrompt): Boolean =
        prompt.promptType != IdentityReviewPromptType.CLASSIFY_COUNTERPARTY

    fun shouldPresentReviewPrompt(
        prompt: IdentityReviewPrompt,
        decisions: List<IdentityReviewDecision>
    ): Boolean {
        val isSelfPrompt =
            prompt.promptType ==
                IdentityReviewPromptType
                    .CONFIRM_SELF_PHONE ||
                prompt.promptType ==
                IdentityReviewPromptType
                    .CONFIRM_SELF_NAME

        if (
            isSelfPrompt &&
            decisions.any {
                it.identityId ==
                    prompt.identityId &&
                    it.decision ==
                    IdentityReviewDecisionType
                        .REJECTED_SELF
            }
        ) {
            return false
        }

        val matchingDecision =
            decisions.firstOrNull {
                it.identityId ==
                    prompt.identityId &&
                    it.promptType ==
                    prompt.promptType
            }

        return when (
            matchingDecision?.decision
        ) {
            IdentityReviewDecisionType
                .DEFERRED ->
                prompt.observationCount >
                    matchingDecision
                        .evidenceCount

            IdentityReviewDecisionType
                .REJECTED_SELF ->
                !isSelfPrompt

            null -> true
        }
    }

    fun isPlausiblePersonName(
        raw: String?
    ): Boolean {
        val normalized =
            normalizeName(raw)
                ?: return false
        val words =
            normalized.split(" ")
                .filter(
                    String::isNotBlank
                )

        if (
            words.size !in 2..4 ||
            words.any {
                it.any(Char::isDigit)
            }
        ) {
            return false
        }

        val blocked =
            setOf(
                "M PESA",
                "MPESA",
                "KCB",
                "EQUITY BANK",
                "SAFARICOM",
                "PESALINK",
                "ACCOUNT",
                "CASH",
                "FULIZA",
                "M SHWARI"
            )

        return normalized !in blocked
    }

    private fun shortHash(
        raw: String
    ): String {
        val bytes =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    raw.toByteArray(
                        Charsets.UTF_8
                    )
                )
        return bytes
            .take(12)
            .joinToString("") {
                "%02x".format(it)
            }
    }
}

