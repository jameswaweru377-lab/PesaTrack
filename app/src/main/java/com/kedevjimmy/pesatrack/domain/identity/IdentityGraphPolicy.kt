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
                .uppercase()
                .replace(
                    Regex("""[^A-Z0-9&'’]+"""),
                    " "
                )
                .replace(
                    Regex("""s+"""),
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
                    digits.startsWith("7") ->
                    "0$digits"

                else -> return null
            }

        return normalized
            .takeIf {
                it.matches(
                    Regex("""0[17]d{8}""")
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
