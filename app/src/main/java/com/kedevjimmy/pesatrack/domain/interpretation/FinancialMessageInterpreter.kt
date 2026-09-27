package com.kedevjimmy.pesatrack.domain.interpretation

import java.math.RoundingMode

object FinancialMessageInterpreter {
    private val currencyHintRegex = Regex(
        """\b(?:Ksh|KES)\b""",
        RegexOption.IGNORE_CASE
    )

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
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:has\s+been\s+)?(?:transferred|withdrawn|moved)\s+from\s+(.+?)\s+account\b""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val receivedFromAccountRegex = Regex(
        """(?:received\s+)?(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+from\s+(.+?)\s+account\b""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val transferToRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:has\s+been\s+)?(?:transferred|deposited|moved)\s+to\s+(.+?)\s+account\b""",
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

        if (!currencyHintRegex.containsMatchIn(normalized)) {
            return FinancialMessageInterpretation(
                transactionCode = null,
                occurredAtText = null,
                balances = emptyList(),
                events = emptyList()
            )
        }

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

        val codeMatch = codeRegex.find(normalized)
        val occurredAtMatch =
            occurredAtRegex.find(normalized)

        return FinancialMessageInterpretation(
            transactionCode = codeMatch
                ?.groupValues
                ?.get(1)
                ?.uppercase(),
            occurredAtText = occurredAtMatch
                ?.groupValues
                ?.get(1),
            balances = balances,
            events = listOfNotNull(event),
            metadataEvidence = buildList {
                codeMatch
                    ?.groups
                    ?.get(1)
                    ?.range
                    ?.let { range ->
                        add(
                            evidence(
                                field =
                                    FinancialField.TRANSACTION_CODE,
                                ruleId =
                                    "transaction_code.confirmed",
                                range = range,
                                confidence =
                                    InterpretationConfidence.HIGH
                            )
                        )
                    }
                occurredAtMatch
                    ?.groups
                    ?.get(1)
                    ?.range
                    ?.let { range ->
                        add(
                            evidence(
                                field =
                                    FinancialField.OCCURRED_AT,
                                ruleId =
                                    "transaction_time.on_at",
                                range = range,
                                confidence =
                                    InterpretationConfidence.HIGH
                            )
                        )
                    }
            }
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
                        InterpretationConfidence.HIGH,
                    evidence = buildList {
                        match.groups[1]?.range?.let {
                            add(
                                evidence(
                                    field =
                                        FinancialField.SOURCE_ACCOUNT,
                                    ruleId =
                                        "balance.account_label",
                                    range = it,
                                    confidence =
                                        InterpretationConfidence.HIGH
                                )
                            )
                        }
                        match.groups[2]?.range?.let {
                            add(
                                evidence(
                                    field =
                                        FinancialField.BALANCE,
                                    ruleId =
                                        "balance.amount",
                                    range = it,
                                    confidence =
                                        InterpretationConfidence.HIGH
                                )
                            )
                        }
                    }
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
        val match =
            transferFromRegex.find(message)
                ?: receivedFromAccountRegex.find(message)
                ?: return null
        val destination = observedAccount ?: return null
        val source = FinancialProviderRegistry.resolveAccount(
            match.groupValues[2]
        )

        if (source.entityKey == destination.entityKey) {
            return null
        }

        val confidence =
            confidenceFor(source, destination)
        val fee = feeMatch(message)

        return TransferEventFact(
            amountMinor = amountToMinor(match.groupValues[1]),
            source = source,
            destination = destination,
            feeMinor = fee?.first ?: 0L,
            confidence = confidence,
            evidence = buildList {
                match.groups[1]?.range?.let {
                    add(
                        evidence(
                            field = FinancialField.AMOUNT,
                            ruleId = "transfer_from.amount",
                            range = it,
                            confidence = confidence
                        )
                    )
                }
                match.groups[2]?.range?.let {
                    add(
                        evidence(
                            field =
                                FinancialField.SOURCE_ACCOUNT,
                            ruleId =
                                "transfer_from.source_account",
                            range = it,
                            confidence = confidence
                        )
                    )
                }
                add(
                    evidence(
                        field =
                            FinancialField.DESTINATION_ACCOUNT,
                        ruleId =
                            "transfer_from.observed_destination",
                        range = null,
                        confidence = confidence,
                        origin =
                            EvidenceOrigin.PROVIDER_PROFILE
                    )
                )
                fee?.second?.let { add(it) }
            }
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

        val confidence =
            confidenceFor(source, destination)
        val fee = feeMatch(message)

        return TransferEventFact(
            amountMinor = amountToMinor(match.groupValues[1]),
            source = source,
            destination = destination,
            feeMinor = fee?.first ?: 0L,
            confidence = confidence,
            evidence = buildList {
                match.groups[1]?.range?.let {
                    add(
                        evidence(
                            field = FinancialField.AMOUNT,
                            ruleId = "transfer_to.amount",
                            range = it,
                            confidence = confidence
                        )
                    )
                }
                add(
                    evidence(
                        field =
                            FinancialField.SOURCE_ACCOUNT,
                        ruleId =
                            "transfer_to.observed_source",
                        range = null,
                        confidence = confidence,
                        origin =
                            EvidenceOrigin.PROVIDER_PROFILE
                    )
                )
                match.groups[2]?.range?.let {
                    add(
                        evidence(
                            field =
                                FinancialField.DESTINATION_ACCOUNT,
                            ruleId =
                                "transfer_to.destination_account",
                            range = it,
                            confidence = confidence
                        )
                    )
                }
                fee?.second?.let { add(it) }
            }
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

    private fun feeMatch(
        message: String
    ): Pair<Long, FieldEvidence>? {
        val match = feeRegex.find(message) ?: return null
        val amount = match.groupValues
            .getOrNull(1)
            ?.let(::amountToMinor)
            ?: return null
        val range = match.groups[1]?.range
        return amount to evidence(
            field = FinancialField.FEE,
            ruleId = "fee.amount",
            range = range,
            confidence = InterpretationConfidence.HIGH
        )
    }

    private fun evidence(
        field: FinancialField,
        ruleId: String,
        range: IntRange?,
        confidence: InterpretationConfidence,
        origin: EvidenceOrigin =
            EvidenceOrigin.MESSAGE_TEXT
    ): FieldEvidence =
        FieldEvidence(
            field = field,
            origin = origin,
            ruleId = ruleId,
            span = range?.let {
                MessageSpan(
                    startOffset = it.first,
                    endOffsetExclusive = it.last + 1
                )
            },
            confidence = confidence
        )

    private fun amountToMinor(raw: String): Long =
        raw.replace(",", "")
            .toBigDecimal()
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
}
