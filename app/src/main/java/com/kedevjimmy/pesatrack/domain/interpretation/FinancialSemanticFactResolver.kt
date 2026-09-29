package com.kedevjimmy.pesatrack.domain.interpretation

/**
 * Converts only high-confidence semantic facts into existing
 * deterministic domain facts when the older grammar did not already
 * resolve them.
 *
 * This is intentionally conservative: semantic understanding does not
 * automatically mean a ledger mutation is safe.
 */
object FinancialSemanticFactResolver {
    fun apply(
        message: NormalizedFinancialMessage,
        semantic: FinancialSemanticAssessment,
        interpretation: FinancialMessageInterpretation
    ): FinancialMessageInterpretation {
        if (!semantic.understood) {
            return interpretation.copy(
                semanticAssessment = semantic
            )
        }

        val balances =
            interpretation.balances.toMutableList()
        val events =
            interpretation.events.toMutableList()

        when (semantic.primaryIntent) {
            FinancialSemanticIntent.LOAN_BALANCE ->
                addLoanBalanceIfSafe(
                    message = message,
                    semantic = semantic,
                    balances = balances
                )

            FinancialSemanticIntent.LOAN_DISBURSEMENT ->
                addLoanDrawdownIfSafe(
                    message = message,
                    semantic = semantic,
                    events = events
                )

            FinancialSemanticIntent.LOAN_REPAYMENT ->
                addLoanRepaymentIfSafe(
                    message = message,
                    semantic = semantic,
                    events = events
                )

            else -> Unit
        }

        return interpretation.copy(
            balances = balances,
            events = events,
            semanticAssessment = semantic
        )
    }

    private fun addLoanBalanceIfSafe(
        message: NormalizedFinancialMessage,
        semantic: FinancialSemanticAssessment,
        balances: MutableList<BalanceObservationFact>
    ) {
        if (
            balances.any {
                it.account.nature ==
                    FinancialAccountNature.LIABILITY
            }
        ) {
            return
        }

        val outstanding =
            semantic.amounts.filter {
                it.role ==
                    SemanticAmountRole.OUTSTANDING
            }.singleOrNull() ?: return

        val liability =
            liabilityForMessageSource(message)
                ?: return

        balances +=
            BalanceObservationFact(
                account = liability,
                balanceMinor =
                    outstanding.amountMinor,
                confidence =
                    InterpretationConfidence.HIGH,
                evidence = listOf(
                    FieldEvidence(
                        field =
                            FinancialField.LIABILITY,
                        origin =
                            EvidenceOrigin
                                .FINANCIAL_GRAMMAR,
                        ruleId =
                            "semantic.loan_balance.liability",
                        span = null,
                        confidence =
                            InterpretationConfidence.HIGH
                    ),
                    FieldEvidence(
                        field =
                            FinancialField
                                .OUTSTANDING_BALANCE,
                        origin =
                            EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            "semantic.loan_balance.amount",
                        span = outstanding.span,
                        confidence =
                            InterpretationConfidence.HIGH
                    )
                )
            )
    }

    private fun addLoanDrawdownIfSafe(
        message: NormalizedFinancialMessage,
        semantic: FinancialSemanticAssessment,
        events: MutableList<FinancialEventFact>
    ) {
        if (
            events.any {
                it is DebtDrawdownEventFact
            }
        ) {
            return
        }

        val amount =
            semantic.amounts.filter {
                it.role ==
                    SemanticAmountRole.PRIMARY
            }.singleOrNull() ?: return

        val liability =
            liabilityForMessageSource(message)
                ?: return
        val destination =
            semantic.accounts
                .firstOrNull {
                    it.role ==
                        SemanticAccountRole.DESTINATION
                }
                ?.label
                ?.let(
                    FinancialProviderRegistry::resolveAccount
                )

        events +=
            DebtDrawdownEventFact(
                amountMinor = amount.amountMinor,
                liability = liability,
                proceedsAccount = destination,
                amountIsNetProceeds = false,
                feeMinor = 0L,
                outstandingAfterMinor = null,
                availableCreditAfterMinor = null,
                confidence =
                    InterpretationConfidence.HIGH,
                evidence = buildList {
                    add(
                        FieldEvidence(
                            field =
                                FinancialField.AMOUNT,
                            origin =
                                EvidenceOrigin.MESSAGE_TEXT,
                            ruleId =
                                "semantic.loan_disbursement.amount",
                            span = amount.span,
                            confidence =
                                InterpretationConfidence.HIGH
                        )
                    )
                    add(
                        FieldEvidence(
                            field =
                                FinancialField.LIABILITY,
                            origin =
                                EvidenceOrigin
                                    .FINANCIAL_GRAMMAR,
                            ruleId =
                                "semantic.loan_disbursement.liability",
                            span = null,
                            confidence =
                                InterpretationConfidence.HIGH
                        )
                    )
                    semantic.accounts
                        .firstOrNull {
                            it.role ==
                                SemanticAccountRole.DESTINATION
                        }
                        ?.let { account ->
                            add(
                                FieldEvidence(
                                    field =
                                        FinancialField
                                            .DESTINATION_ACCOUNT,
                                    origin =
                                        EvidenceOrigin.MESSAGE_TEXT,
                                    ruleId =
                                        "semantic.loan_disbursement.destination",
                                    span = account.span,
                                    confidence =
                                        InterpretationConfidence.HIGH
                                )
                            )
                        }
                }
            )
    }

    private fun addLoanRepaymentIfSafe(
        message: NormalizedFinancialMessage,
        semantic: FinancialSemanticAssessment,
        events: MutableList<FinancialEventFact>
    ) {
        if (
            events.any {
                it is DebtRepaymentEventFact
            }
        ) {
            return
        }

        val amount =
            semantic.amounts.filter {
                it.role ==
                    SemanticAmountRole.PRIMARY
            }.singleOrNull() ?: return
        val liability =
            liabilityForMessageSource(message)
                ?: return
        val funding =
            semantic.accounts
                .firstOrNull {
                    it.role ==
                        SemanticAccountRole.SOURCE
                }
                ?.label
                ?.let(
                    FinancialProviderRegistry::resolveAccount
                )
        val outstanding =
            semantic.amounts
                .filter {
                    it.role ==
                        SemanticAmountRole.OUTSTANDING
                }
                .singleOrNull()

        events +=
            DebtRepaymentEventFact(
                amountMinor = amount.amountMinor,
                liability = liability,
                fundingAccount = funding,
                completion =
                    if (
                        FULL_REPAYMENT_REGEX
                            .containsMatchIn(message.body)
                    ) {
                        DebtCompletion.FULL
                    } else {
                        DebtCompletion.UNKNOWN
                    },
                outstandingAfterMinor =
                    outstanding?.amountMinor,
                availableCreditAfterMinor = null,
                walletBalanceAfterMinor = null,
                confidence =
                    InterpretationConfidence.HIGH,
                evidence = buildList {
                    add(
                        FieldEvidence(
                            field =
                                FinancialField.AMOUNT,
                            origin =
                                EvidenceOrigin.MESSAGE_TEXT,
                            ruleId =
                                "semantic.loan_repayment.amount",
                            span = amount.span,
                            confidence =
                                InterpretationConfidence.HIGH
                        )
                    )
                    add(
                        FieldEvidence(
                            field =
                                FinancialField.LIABILITY,
                            origin =
                                EvidenceOrigin
                                    .FINANCIAL_GRAMMAR,
                            ruleId =
                                "semantic.loan_repayment.liability",
                            span = null,
                            confidence =
                                InterpretationConfidence.HIGH
                        )
                    )
                    outstanding?.let {
                        add(
                            FieldEvidence(
                                field =
                                    FinancialField
                                        .OUTSTANDING_BALANCE,
                                origin =
                                    EvidenceOrigin.MESSAGE_TEXT,
                                ruleId =
                                    "semantic.loan_repayment.outstanding",
                                span = it.span,
                                confidence =
                                    InterpretationConfidence.HIGH
                            )
                        )
                    }
                }
            )
    }

    private fun liabilityForMessageSource(
        message: NormalizedFinancialMessage
    ): FinancialAccountReference? {
        val title =
            message.title
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: return null

        val rawLabel =
            if (
                title.contains(
                    "loan",
                    ignoreCase = true
                )
            ) {
                title
            } else {
                "$title Loan"
            }

        val resolved =
            FinancialProviderRegistry
                .resolveAccount(rawLabel)

        return if (
            resolved.nature ==
                FinancialAccountNature.LIABILITY
        ) {
            resolved
        } else {
            resolved.copy(
                ownershipHint =
                    AccountOwnershipHint.USER_OWNED,
                nature =
                    FinancialAccountNature.LIABILITY
            )
        }
    }

    private val FULL_REPAYMENT_REGEX =
        Regex(
            """\b(?:fully\s+(?:paid|repaid)|loan\s+cleared|cleared\s+the\s+loan)\b""",
            RegexOption.IGNORE_CASE
        )
}
