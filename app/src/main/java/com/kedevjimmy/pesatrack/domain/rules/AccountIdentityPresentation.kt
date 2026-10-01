package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.KenyanBankDirectory
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount

object AccountIdentityPresentation {
    fun title(
        account: MoneyAccount
    ): String =
        withIdentifier(
            baseName(account),
            account.identifier
        )

    fun title(
        group: TrackedAccountGroup
    ): String {
        val identifier =
            group.accounts
                .firstOrNull {
                    !it.identifier
                        .isNullOrBlank()
                }
                ?.identifier
        return withIdentifier(
            group.displayName,
            identifier
        )
    }

    fun maskedIdentifier(
        raw: String?
    ): String? {
        val value =
            raw?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return null

        val compact =
            value.replace(
                Regex("""\s+"""),
                ""
            )
        val suffix =
            compact.takeLast(
                minOf(
                    4,
                    compact.length
                )
            )
        return "•••• $suffix"
    }

    fun title(
        base: String,
        identifier: String?
    ): String =
        withIdentifier(
            base = base,
            identifier = identifier
        )

    private fun withIdentifier(
        base: String,
        identifier: String?
    ): String {
        val masked =
            maskedIdentifier(identifier)
                ?: return base
        return "$base ($masked)"
    }

    private fun baseName(
        account: MoneyAccount
    ): String =
        KenyanBankDirectory
            .matchSender(
                account.name
            )
            ?.name
            ?: account.name.trim()
}
