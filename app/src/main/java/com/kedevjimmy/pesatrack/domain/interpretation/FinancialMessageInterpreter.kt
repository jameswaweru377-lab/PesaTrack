package com.kedevjimmy.pesatrack.domain.interpretation

import java.math.RoundingMode

object FinancialMessageInterpreter {
    private val codeRegex = Regex(
        """\b([A-Z0-9]{10})\s+Confirmed\b""",
        RegexOption.IGNORE_CASE
    )

    private val occurredAtRegex = Regex(
        """\bon\s+(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\s+at\s+\d{1,2}:\d{2}\s*(?:AM|PM)?)""",
        RegexOption.IGNORE_CASE
    )

    private val balanceRegex = Regex(
        """(?:^|[.!?]\s+)(?:New\s+|Your\s+)?([A-Za-z][A-Za-z0-9&'’.-]*(?:\s+[A-Za-z0-9&'’.-]+){0,4})\s+balance\s+is\s*(?:(?:Ksh|KES)\s*)?([0-9,]+(?:\.\d{1,2})?)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.MULTILINE
        )
    )

    private val transferFromRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:has\s+been\s+)?transferred\s+from\s+(.+?)\s+account\b""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val transferToRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:has\s+been\s+)?transferred\s+to\s+(.+?)\s+account\b""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val feeRegex = Regex(
        """(?:transaction\s+(?:cost|fee)|access\s+fee|charge)\s*[,.:]?\s*(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    fun interpret(
        title: String?,
        body: String
    ): FinancialMessageInterpretation {
        val normalized = body
            .replace(Regex("""\s+"""), " ")
            .trim()

        val balances = parseBalances(normalized)
        val observedMpesa = balances
            .map(BalanceObservationFact::account)
            .firstOrNull {
                it.providerKey == "SAFARICOM_MPESA"
            } ?: if (
                title.orEmpty().contains("MPESA", ignoreCase = true) ||
                title.orEmpty().contains("M-PESA", ignoreCase = true)
            ) {
                FinancialProviderRegistry.mpesaAccount()
            } else {
                null
            }

        val event = parseTransferFrom(
            normalized,
            observedMpesa
        ) ?: parseTransferTo(
            normalized,
            observedMpesa
        )

        return FinancialMessageInterpretation(
            transactionCode = codeRegex
                .find(normalized)
                ?.groupValues
                ?.get(1)
                ?.uppercase(),
            occurredAtText = occurredAtRegex
                .find(normalized)
                ?.groupValues
                ?.get(1),
            balances = balances,
            events = listOfNotNull(event)
        )
    }

    private fun parseBalances(
        message: String
    ): List<BalanceObservationFact> =
        balanceRegex.findAll(message)
            .mapNotNull { match ->
                val rawLabel = match.groupValues[1]
                if (
                    FinancialProviderRegistry
                        .looksLikeNonFinancialBalanceLabel(rawLabel)
                ) {
                    return@mapNotNull null
                }

                val account =
                    FinancialProviderRegistry.resolveAccount(rawLabel)
                BalanceObservationFact(
                    account = account,
                    balanceMinor = amountToMinor(
                        match.groupValues[2]
                    ),
                    confidence =
                        InterpretationConfidence.HIGH
                )
            }
            .distinctBy {
                it.account.entityKey to it.balanceMinor
            }
            .toList()

    private fun parseTransferFrom(
        message: String,
        observedAccount: FinancialAccountReference?
    ): TransferEventFact? {
        val match = transferFromRegex.find(message)
            ?: return null
        val destination = observedAccount ?: return null
        val source = FinancialProviderRegistry.resolveAccount(
            match.groupValues[2]
        )

        if (source.entityKey == destination.entityKey) {
            return null
        }

        return TransferEventFact(
            amountMinor = amountToMinor(match.groupValues[1]),
            source = source,
            destination = destination,
            feeMinor = feeMinor(message),
            confidence = confidenceFor(source, destination)
        )
    }

    private fun parseTransferTo(
        message: String,
        observedAccount: FinancialAccountReference?
    ): TransferEventFact? {
        val match = transferToRegex.find(message)
            ?: return null
        val source = observedAccount ?: return null
        val destination =
            FinancialProviderRegistry.resolveAccount(
                match.groupValues[2]
            )

        if (source.entityKey == destination.entityKey) {
            return null
        }

        return TransferEventFact(
            amountMinor = amountToMinor(match.groupValues[1]),
            source = source,
            destination = destination,
            feeMinor = feeMinor(message),
            confidence = confidenceFor(source, destination)
        )
    }

    private fun confidenceFor(
        source: FinancialAccountReference,
        destination: FinancialAccountReference
    ): InterpretationConfidence =
        if (
            source.ownershipHint ==
                AccountOwnershipHint.USER_OWNED &&
            destination.ownershipHint ==
                AccountOwnershipHint.USER_OWNED
        ) {
            InterpretationConfidence.HIGH
        } else {
            InterpretationConfidence.MEDIUM
        }

    private fun feeMinor(message: String): Long =
        feeRegex.find(message)
            ?.groupValues
            ?.get(1)
            ?.let(::amountToMinor)
            ?: 0L

    private fun amountToMinor(raw: String): Long =
        raw.replace(",", "")
            .toBigDecimal()
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
}
