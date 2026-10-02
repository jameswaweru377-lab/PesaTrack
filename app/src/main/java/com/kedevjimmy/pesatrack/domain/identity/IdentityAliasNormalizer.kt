package com.kedevjimmy.pesatrack.domain.identity

object IdentityAliasNormalizer {
    fun normalize(
        type: IdentityAliasType,
        value: String
    ): String =
        when (type) {
            IdentityAliasType.PHONE ->
                normalizeKenyanPhone(value)
                    ?: normalizeGeneral(value)
            IdentityAliasType.NAME,
            IdentityAliasType.ACCOUNT_REFERENCE,
            IdentityAliasType.PROVIDER_KEY,
            IdentityAliasType.DOMAIN ->
                normalizeGeneral(value)
        }

    fun normalizeKenyanPhone(
        value: String
    ): String? {
        val digits =
            value.filter(Char::isDigit)

        return when {
            digits.length == 12 &&
                digits.startsWith("254") ->
                digits

            digits.length == 10 &&
                digits.startsWith("0") ->
                "254" + digits.drop(1)

            digits.length == 9 &&
                (
                    digits.startsWith("7") ||
                        digits.startsWith("1")
                    ) ->
                "254$digits"

            else -> null
        }
    }

    fun looksLikeKenyanPhone(
        value: String
    ): Boolean =
        normalizeKenyanPhone(value) != null

    private fun normalizeGeneral(
        value: String
    ): String =
        value.trim()
            .uppercase()
            .replace(
                Regex("""[^A-Z0-9]+"""),
                " "
            )
            .replace(
                Regex("""s+"""),
                " "
            )
            .trim()
}
