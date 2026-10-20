package com.kedevjimmy.pesatrack.domain.interpretation

import com.kedevjimmy.pesatrack.domain.messagepack.MessagePackMatch
import java.math.BigDecimal
import java.math.RoundingMode

object MessagePackCandidateBridge {
    fun enrich(
        candidates: FinancialMessageCandidates,
        match: MessagePackMatch?,
        message: NormalizedFinancialMessage
    ): FinancialMessageCandidates {
        if (match == null) {
            return candidates
        }

        val eventKind =
            match.eventKind
                .uppercase()

        val guardedCandidates =
            if (
                eventKind ==
                "DEBT_DRAWDOWN_WITH_SAVINGS"
            ) {
                candidates.copy(
                    debt =
                        candidates.debt.copy(
                            drawdown = null
                        )
                )
            } else {
                candidates
            }

        val withObservedBalances =
            enrichVerifiedAccountBalances(
                candidates = guardedCandidates,
                match = match,
                message = message
            )

        val repaymentEnriched =
            enrichDebtRepayment(
                candidates = withObservedBalances,
                match = match,
                message = message
            )

        val debtEnriched =
            enrichDebtDrawdown(
                candidates =
                    repaymentEnriched,
                match = match,
                message = message
            )

        if (
            debtEnriched.actions
                .isNotEmpty()
        ) {
            return debtEnriched
        }

        val amountField =
            match.fields.firstOrNull {
                it.field.equals(
                    "AMOUNT",
                    ignoreCase = true
                )
            } ?: return debtEnriched

        val amountMinor =
            amountToMinor(
                amountField.value
            ) ?: return debtEnriched

        val amountSpan =
            spanForValue(
                body = message.body,
                value = amountField.value
            ) ?: return debtEnriched

        val action =
            when (
                match.eventKind
                    .uppercase()
            ) {
                "PAYMENT" ->
                    actionForParty(
                        kind =
                            FinancialActionCandidateKind
                                .PAYMENT_PAID_TO,
                        partyField =
                            "COUNTERPARTY",
                        amountMinor =
                            amountMinor,
                        amountSpan =
                            amountSpan,
                        match = match,
                        message = message
                    )

                "RECEIPT" ->
                    actionForParty(
                        kind =
                            FinancialActionCandidateKind
                                .RECEIPT_FROM,
                        partyField =
                            "COUNTERPARTY",
                        amountMinor =
                            amountMinor,
                        amountSpan =
                            amountSpan,
                        match = match,
                        message = message
                    )

                "TRANSFER" ->
                    transferAction(
                        amountMinor =
                            amountMinor,
                        amountSpan =
                            amountSpan,
                        match = match,
                        message = message
                    )

                else -> null
            } ?: return debtEnriched

        return debtEnriched.copy(
            actions =
                debtEnriched.actions +
                    action
        )
    }

    /**
     * Savings 'balance is' is a snapshot, not the amount of an overpayment.
     * The full-repayment M-Shwari text never specifies the savings deposit;
     * do not create a transfer or credit from the ending balance.
     */
    private fun enrichVerifiedAccountBalances(
        candidates: FinancialMessageCandidates,
        match: MessagePackMatch,
        message: NormalizedFinancialMessage
    ): FinancialMessageCandidates {
        if (match.templateId != "ke.mshwari.loan.full-repayment-overpayment")
            return candidates
        var current = candidates
        listOf(
            "SAVINGS_BALANCE" to "M-Shwari",
            "WALLET_BALANCE" to "M-PESA"
        ).forEach { (fieldName, accountName) ->
            val field = match.fields.firstOrNull {
                it.field.equals(fieldName, ignoreCase = true)
            } ?: return@forEach
            val amount = amountToMinor(field.value) ?: return@forEach
            val resolved = FinancialProviderRegistry.resolveAccount(accountName)
            val exists = current.balances.any { observed ->
                FinancialProviderRegistry.resolveAccount(observed.rawAccountLabel).entityKey ==
                    resolved.entityKey && observed.amountMinor == amount
            }
            if (!exists) {
                val span = spanForValue(message.body, field.value) ?: return@forEach
                current = current.copy(
                    balances = current.balances + BalanceCandidate(
                        rawAccountLabel = accountName,
                        accountSpan = spanForValue(message.body, accountName),
                        amountMinor = amount,
                        amountSpan = span,
                        ruleId = "message_pack." + match.templateId + ".observed_balance"
                    )
                )
            }
        }
        return current
    }

    private fun enrichDebtRepayment(
        candidates: FinancialMessageCandidates,
        match: MessagePackMatch,
        message: NormalizedFinancialMessage
    ): FinancialMessageCandidates {
        if (
            match.eventKind.uppercase() !=
            "DEBT_REPAYMENT"
        ) {
            return candidates
        }

        val product =
            debtProduct(
                match.productIdentity
            ) ?: return candidates

        val amountField =
            match.fields.firstOrNull {
                it.field.equals(
                    "AMOUNT",
                    ignoreCase = true
                )
            } ?: return candidates
        val amountMinor =
            amountToMinor(
                amountField.value
            ) ?: return candidates
        val amountSpan =
            spanForValue(
                body = message.body,
                value = amountField.value
            ) ?: return candidates

        val outstandingField =
            match.fields.firstOrNull {
                it.field.equals(
                    "OUTSTANDING",
                    ignoreCase = true
                )
            }
        val outstandingMinor =
            outstandingField
                ?.value
                ?.let(::amountToMinor)
        val explicitlyFull =
            match.fieldValue("COMPLETION")?.equals(
                "loan is repaid in full",
                ignoreCase = true
            ) == true
        val observedOutstanding =
            outstandingMinor ?: if (explicitlyFull) 0L else null
        val outstandingSpan =
            outstandingField
                ?.value
                ?.let {
                    spanForValue(
                        body = message.body,
                        value = it
                    )
                } ?: if (explicitlyFull) {
                    spanForValue(message.body, "loan is repaid in full")
                } else null

        val sourceField =
            match.fields.firstOrNull {
                it.field.equals(
                    "SOURCE",
                    ignoreCase = true
                )
            }
        val fundingLabel =
            sourceField?.value
        val fundingSpan =
            fundingLabel?.let {
                spanForValue(
                    body = message.body,
                    value = it
                )
            }

        val liabilitySpan =
            spanForKeyword(
                body = message.body,
                keyword = "Loan"
            ) ?: return candidates

        val repayment =
            DebtRepaymentCandidate(
                amountMinor = amountMinor,
                amountSpan = amountSpan,
                fundingAccountLabel =
                    fundingLabel,
                fundingAccountSpan =
                    fundingSpan,
                liabilityLabel =
                    product.liabilityLabel,
                liabilitySpan =
                    liabilitySpan,
                completion =
                    if (observedOutstanding == 0L) {
                        DebtCompletion.FULL
                    } else {
                        DebtCompletion.UNKNOWN
                    },
                outstandingAfterMinor =
                    observedOutstanding,
                outstandingSpan =
                    outstandingSpan,
                availableCreditAfterMinor =
                    null,
                availableCreditSpan =
                    null,
                ruleId =
                    "message_pack." +
                        match.templateId +
                        "@" +
                        match.revision +
                        ".debt_repayment"
            )

        val correctedBalances =
            candidates.balances.map {
                balance ->
                if (
                    balance.rawAccountLabel.equals(
                        "Loan",
                        ignoreCase = true
                    ) ||
                    balance.ruleId ==
                    "balance.generic_loan"
                ) {
                    balance.copy(
                        rawAccountLabel =
                            product.liabilityLabel
                    )
                } else {
                    balance
                }
            }

        return candidates.copy(
            balances = correctedBalances,
            debt =
                candidates.debt.copy(
                    repayment = repayment
                )
        )
    }

    private fun enrichDebtDrawdown(
        candidates: FinancialMessageCandidates,
        match: MessagePackMatch,
        message: NormalizedFinancialMessage
    ): FinancialMessageCandidates {
        val eventKind =
            match.eventKind
                .uppercase()

        if (
            eventKind !=
            "DEBT_DRAWDOWN"
        ) {
            return candidates
        }

        val withFee =
            if (candidates.fee == null) {
                templateFee(
                    match = match,
                    message = message
                )?.let {
                    candidates.copy(
                        fee = it
                    )
                } ?: candidates
            } else {
                candidates
            }

        val product =
            debtProduct(
                match.productIdentity
            ) ?: return withFee

        val outstandingField =
            match.fields.firstOrNull {
                it.field.equals(
                    "REPAY_AMOUNT",
                    ignoreCase = true
                )
            } ?: match.fields
                .firstOrNull {
                    it.field.equals(
                        "TOTAL_DUE",
                        ignoreCase = true
                    )
                }
        val outstandingMinor =
            outstandingField
                ?.value
                ?.let(::amountToMinor)
        val outstandingSpan =
            outstandingField
                ?.value
                ?.let {
                    spanForValue(
                        body = message.body,
                        value = it
                    )
                }

        val proceedsSpan =
            product.proceedsAccountLabel
                ?.let {
                    spanForValue(
                        body = message.body,
                        value = it
                    )
                }

        withFee.debt.drawdown
            ?.let { existing ->
                return withFee.copy(
                    debt =
                        withFee.debt.copy(
                            drawdown =
                                existing.copy(
                                    outstandingAfterMinor =
                                        existing
                                            .outstandingAfterMinor
                                            ?: outstandingMinor,
                                    outstandingSpan =
                                        existing
                                            .outstandingSpan
                                            ?: outstandingSpan,
                                    proceedsAccountLabel =
                                        existing
                                            .proceedsAccountLabel
                                            ?: if (
                                                proceedsSpan !=
                                                null
                                            ) {
                                                product
                                                    .proceedsAccountLabel
                                            } else {
                                                null
                                            },
                                    proceedsAccountSpan =
                                        existing
                                            .proceedsAccountSpan
                                            ?: proceedsSpan
                                )
                        )
                )
            }

        val amountField =
            match.fields.firstOrNull {
                it.field.equals(
                    "AMOUNT",
                    ignoreCase = true
                )
            } ?: return withFee
        val amountMinor =
            amountToMinor(
                amountField.value
            ) ?: return withFee
        val amountSpan =
            spanForValue(
                body = message.body,
                value = amountField.value
            ) ?: return withFee

        val liabilitySpan =
            product.bodyMarkers
                .asSequence()
                .mapNotNull {
                    spanForValue(
                        body = message.body,
                        value = it
                    )
                }
                .firstOrNull()
                ?: spanForKeyword(
                    body = message.body,
                    keyword = "loan"
                )
                ?: return withFee

        val drawdown =
            DebtDrawdownCandidate(
                amountMinor = amountMinor,
                amountSpan = amountSpan,
                liabilityLabel =
                    product.liabilityLabel,
                liabilitySpan =
                    liabilitySpan,
                outstandingAfterMinor =
                    outstandingMinor,
                outstandingSpan =
                    outstandingSpan,
                availableCreditAfterMinor =
                    null,
                availableCreditSpan =
                    null,
                proceedsAccountLabel =
                    if (
                        proceedsSpan != null
                    ) {
                        product
                            .proceedsAccountLabel
                    } else {
                        null
                    },
                proceedsAccountSpan =
                    proceedsSpan,
                amountIsNetProceeds =
                    false,
                ruleId =
                    "message_pack." +
                        match.templateId +
                        "@" +
                        match.revision +
                        ".debt_drawdown"
            )

        return withFee.copy(
            debt =
                withFee.debt.copy(
                    drawdown = drawdown
                )
        )
    }

    private fun templateFee(
        match: MessagePackMatch,
        message: NormalizedFinancialMessage
    ): FeeCandidate? {
        val field =
            match.fields.firstOrNull {
                it.field.equals(
                    "FEE",
                    ignoreCase = true
                )
            } ?: return null
        val amountMinor =
            amountToMinor(
                field.value
            ) ?: return null
        val span =
            spanForValue(
                body = message.body,
                value = field.value
            ) ?: return null

        return FeeCandidate(
            amountMinor = amountMinor,
            amountSpan = span,
            ruleId =
                "message_pack." +
                    match.templateId +
                    "@" +
                    match.revision +
                    ".fee"
        )
    }

    private fun debtProduct(
        productIdentity: String?
    ): DebtProductBridge? =
        when (
            productIdentity
                ?.uppercase()
        ) {
            "NCBA_MSHWARI_LOAN" ->
                DebtProductBridge(
                    liabilityLabel =
                        "M-Shwari Loan",
                    bodyMarkers =
                        listOf(
                            "M-Shwari",
                            "M Shwari"
                        ),
                    proceedsAccountLabel =
                        "M-Pesa"
                )

            "ABSA_TIMIZA_LOAN" ->
                DebtProductBridge(
                    liabilityLabel =
                        "Timiza Loan",
                    bodyMarkers =
                        listOf("Timiza"),
                    proceedsAccountLabel =
                        "Timiza"
                )

            "TALA_LOAN" ->
                DebtProductBridge(
                    liabilityLabel =
                        "Tala Loan",
                    bodyMarkers =
                        listOf("Tala"),
                    proceedsAccountLabel =
                        "M-Pesa"
                )

            "BRANCH_LOAN" ->
                DebtProductBridge(
                    liabilityLabel =
                        "Branch Loan",
                    bodyMarkers =
                        listOf("Branch"),
                    proceedsAccountLabel =
                        "M-Pesa"
                )

            "ZENKA_LOAN" ->
                DebtProductBridge(
                    liabilityLabel =
                        "Zenka Loan",
                    bodyMarkers =
                        listOf("Zenka"),
                    proceedsAccountLabel =
                        "M-Pesa"
                )

            "OKASH_LOAN" ->
                DebtProductBridge(
                    liabilityLabel =
                        "OKash Loan",
                    bodyMarkers =
                        listOf(
                            "OKash",
                            "O-Kash"
                        ),
                    proceedsAccountLabel =
                        null
                )

            else -> null
        }

    private fun transferAction(
        amountMinor: Long,
        amountSpan: MessageSpan,
        match: MessagePackMatch,
        message: NormalizedFinancialMessage
    ): FinancialActionCandidate? {
        val destination =
            match.fields.firstOrNull {
                it.field.equals(
                    "DESTINATION",
                    ignoreCase = true
                )
            }
        if (destination != null) {
            return action(
                kind =
                    FinancialActionCandidateKind
                        .TRANSFER_TO_ACCOUNT,
                partyValue =
                    destination.value,
                amountMinor =
                    amountMinor,
                amountSpan =
                    amountSpan,
                match = match,
                message = message
            )
        }

        val source =
            match.fields.firstOrNull {
                it.field.equals(
                    "SOURCE",
                    ignoreCase = true
                )
            }
        if (source != null) {
            return action(
                kind =
                    FinancialActionCandidateKind
                        .TRANSFER_FROM_ACCOUNT,
                partyValue =
                    source.value,
                amountMinor =
                    amountMinor,
                amountSpan =
                    amountSpan,
                match = match,
                message = message
            )
        }

        // A transfer-network notification such as
        // PesaLink can contain a counterparty but no
        // authoritative source/destination account.
        // Do not invent an account relationship here.
        return null
    }

    private fun actionForParty(
        kind: FinancialActionCandidateKind,
        partyField: String,
        amountMinor: Long,
        amountSpan: MessageSpan,
        match: MessagePackMatch,
        message: NormalizedFinancialMessage
    ): FinancialActionCandidate? {
        val party =
            match.fields.firstOrNull {
                it.field.equals(
                    partyField,
                    ignoreCase = true
                )
            } ?: return null

        return action(
            kind = kind,
            partyValue =
                party.value,
            amountMinor =
                amountMinor,
            amountSpan =
                amountSpan,
            match = match,
            message = message
        )
    }

    private fun action(
        kind: FinancialActionCandidateKind,
        partyValue: String,
        amountMinor: Long,
        amountSpan: MessageSpan,
        match: MessagePackMatch,
        message: NormalizedFinancialMessage
    ): FinancialActionCandidate? {
        val partySpan =
            spanForValue(
                body = message.body,
                value = partyValue
            ) ?: return null

        val accountReference =
            match.fieldValue(
                "ACCOUNT_REFERENCE"
            )

        return FinancialActionCandidate(
            kind = kind,
            amountMinor = amountMinor,
            amountSpan = amountSpan,
            partyLabel =
                partyValue.trim(),
            partySpan = partySpan,
            partyPhone = null,
            accountReference =
                accountReference,
            accountReferenceSpan =
                accountReference?.let {
                    spanForValue(
                        body =
                            message.body,
                        value = it
                    )
                },
            ruleId =
                "message_pack." +
                    match.templateId +
                    "@" +
                    match.revision
        )
    }

    private fun spanForValue(
        body: String,
        value: String
    ): MessageSpan? {
        val trimmed =
            value.trim()
        if (trimmed.isBlank()) {
            return null
        }

        val start =
            body.indexOf(
                string = trimmed,
                ignoreCase = true
            )
        if (start < 0) {
            return null
        }

        return MessageSpan(
            startOffset = start,
            endOffsetExclusive =
                start + trimmed.length
        )
    }

    private fun spanForKeyword(
        body: String,
        keyword: String
    ): MessageSpan? {
        val start =
            body.indexOf(
                string = keyword,
                ignoreCase = true
            )
        if (start < 0) {
            return null
        }

        return MessageSpan(
            startOffset = start,
            endOffsetExclusive =
                start + keyword.length
        )
    }

    private fun amountToMinor(
        raw: String
    ): Long? =
        runCatching {
            BigDecimal(
                raw.replace(
                    ",",
                    ""
                ).trim()
            ).setScale(
                2,
                RoundingMode.HALF_UP
            ).movePointRight(2)
                .longValueExact()
        }.getOrNull()

    private data class DebtProductBridge(
        val liabilityLabel: String,
        val bodyMarkers: List<String>,
        val proceedsAccountLabel: String?
    )
}
