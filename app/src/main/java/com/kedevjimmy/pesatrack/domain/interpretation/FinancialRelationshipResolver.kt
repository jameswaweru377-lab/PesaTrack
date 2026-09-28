package com.kedevjimmy.pesatrack.domain.interpretation

sealed interface ResolvedFinancialRelationship {
    val amountMinor: Long
    val feeMinor: Long
    val confidence: InterpretationConfidence
    val evidence: List<FieldEvidence>
}

data class ResolvedTransferRelationship(
    override val amountMinor: Long,
    val source: FinancialAccountReference,
    val destination: FinancialAccountReference,
    override val feeMinor: Long,
    override val confidence: InterpretationConfidence,
    override val evidence: List<FieldEvidence>
) : ResolvedFinancialRelationship

data class ResolvedPaymentRelationship(
    override val amountMinor: Long,
    val source: FinancialAccountReference,
    val counterparty: String,
    val counterpartyPhone: String?,
    val routeHint: PaymentRouteHint,
    val accountReference: String?,
    override val feeMinor: Long,
    override val confidence: InterpretationConfidence,
    override val evidence: List<FieldEvidence>
) : ResolvedFinancialRelationship

data class ResolvedReceiptRelationship(
    override val amountMinor: Long,
    val destination: FinancialAccountReference,
    val counterparty: String,
    val counterpartyPhone: String?,
    override val feeMinor: Long,
    override val confidence: InterpretationConfidence,
    override val evidence: List<FieldEvidence>
) : ResolvedFinancialRelationship

data class ResolvedCashWithdrawalRelationship(
    override val amountMinor: Long,
    val source: FinancialAccountReference,
    val cashDestination: FinancialAccountReference,
    val counterparty: String?,
    override val feeMinor: Long,
    override val confidence: InterpretationConfidence,
    override val evidence: List<FieldEvidence>
) : ResolvedFinancialRelationship

data class ResolvedCashDepositRelationship(
    override val amountMinor: Long,
    val cashSource: FinancialAccountReference,
    val destination: FinancialAccountReference,
    val counterparty: String?,
    override val feeMinor: Long,
    override val confidence: InterpretationConfidence,
    override val evidence: List<FieldEvidence>
) : ResolvedFinancialRelationship

data class ResolvedUnresolvedMovementRelationship(
    override val amountMinor: Long,
    val observedAccount: FinancialAccountReference?,
    override val feeMinor: Long,
    override val confidence: InterpretationConfidence,
    override val evidence: List<FieldEvidence>
) : ResolvedFinancialRelationship

data class ResolvedReversalRelationship(
    override val amountMinor: Long,
    val referencedTransactionCode: String,
    val observedAccount: FinancialAccountReference?,
    override val feeMinor: Long = 0L,
    override val confidence: InterpretationConfidence,
    override val evidence: List<FieldEvidence>
) : ResolvedFinancialRelationship

data class ResolvedDebtDrawdownRelationship(
    override val amountMinor: Long,
    val liability: FinancialAccountReference,
    override val feeMinor: Long,
    val outstandingAfterMinor: Long?,
    val availableCreditAfterMinor: Long?,
    override val confidence: InterpretationConfidence,
    override val evidence: List<FieldEvidence>
) : ResolvedFinancialRelationship

data class ResolvedDebtRepaymentRelationship(
    override val amountMinor: Long,
    val liability: FinancialAccountReference,
    val fundingAccount: FinancialAccountReference?,
    override val feeMinor: Long,
    val completion: DebtCompletion,
    val outstandingAfterMinor: Long?,
    val availableCreditAfterMinor: Long?,
    val walletBalanceAfterMinor: Long?,
    override val confidence: InterpretationConfidence,
    override val evidence: List<FieldEvidence>
) : ResolvedFinancialRelationship

data class ResolvedFinancialMessage(
    val transactionCode: TextValueCandidate?,
    val occurredAt: TextValueCandidate?,
    val balances: List<BalanceObservationFact>,
    val relationship: ResolvedFinancialRelationship?
)

object FinancialRelationshipResolver {
    fun resolve(
        message: NormalizedFinancialMessage,
        candidates: FinancialMessageCandidates,
        context: FinancialInterpretationContext =
            FinancialInterpretationContext.EMPTY
    ): ResolvedFinancialMessage {
        val balances = candidates.balances
            .mapNotNull {
                resolveBalance(
                    candidate = it,
                    context = context
                )
            }
            .distinctBy {
                it.account.entityKey to
                    it.balanceMinor
            }

        val observedAccount =
            FinancialProviderRegistry
                .resolveObservedAccount(
                    title = message.title,
                    balances = balances,
                    context = context
                )

        val debtRelationship =
            resolveDebt(
                candidates = candidates,
                balances = balances,
                context = context
            )
        val reversalRelationship =
            resolveReversal(
                candidate = candidates.reversal,
                observedAccount = observedAccount
            )
        val cashRelationship =
            resolveCashMovement(
                candidate = candidates.cashMovements
                    .firstOrNull(),
                fee = candidates.fee,
                context = context
            )

        val prioritized = candidates.actions
            .sortedBy(::priority)

        val knownRelationship =
            debtRelationship
                ?: reversalRelationship
                ?: cashRelationship
                ?: prioritized.firstNotNullOfOrNull { action ->
                    resolveAction(
                        action = action,
                        observedAccount = observedAccount,
                        fee = candidates.fee,
                        context = context
                    )
                }

        val relationship =
            knownRelationship
                ?: resolveUnresolvedMovement(
                    candidates = candidates,
                    observedAccount = observedAccount
                )

        return ResolvedFinancialMessage(
            transactionCode = candidates.transactionCode,
            occurredAt = candidates.occurredAt,
            balances = balances,
            relationship = relationship
        )
    }

    private fun resolveUnresolvedMovement(
        candidates: FinancialMessageCandidates,
        observedAccount: FinancialAccountReference?
    ): ResolvedFinancialRelationship? {
        // A transaction reference is our minimum safety signal:
        // do not turn arbitrary currency text into a ledger row.
        if (candidates.transactionCode == null) {
            return null
        }

        val explainedSpans = buildList {
            candidates.balances.forEach {
                add(it.amountSpan)
            }
            candidates.fee?.let {
                add(it.amountSpan)
            }
            candidates.reversal?.let {
                add(it.amountSpan)
            }
            candidates.cashMovements.forEach {
                add(it.amountSpan)
            }
            candidates.actions.forEach {
                add(it.amountSpan)
            }
            candidates.debt.drawdown?.let {
                add(it.amountSpan)
                it.outstandingSpan?.let(::add)
                it.availableCreditSpan?.let(::add)
            }
            candidates.debt.repayment?.let {
                add(it.amountSpan)
                it.availableCreditSpan?.let(::add)
            }
        }

        val unexplained =
            candidates.moneyValues.filter { money ->
                explainedSpans.none { span ->
                    money.span.overlaps(span)
                }
            }

        if (unexplained.size != 1) {
            return null
        }

        val amount = unexplained.single()
        val fee = candidates.fee?.amountMinor ?: 0L

        return ResolvedUnresolvedMovementRelationship(
            amountMinor = amount.amountMinor,
            observedAccount = observedAccount,
            feeMinor = fee,
            confidence = InterpretationConfidence.LOW,
            evidence = buildList {
                add(
                    evidence(
                        field = FinancialField.AMOUNT,
                        origin = EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            "unresolved.unique_amount",
                        span = amount.span,
                        confidence =
                            InterpretationConfidence.LOW
                    )
                )
                if (observedAccount != null) {
                    add(
                        evidence(
                            field = FinancialField.ACCOUNT,
                            origin =
                                EvidenceOrigin.PROVIDER_PROFILE,
                            ruleId =
                                "unresolved.observed_account",
                            span = null,
                            confidence =
                                InterpretationConfidence.LOW
                        )
                    )
                }
                candidates.fee?.let {
                    add(
                        evidence(
                            field = FinancialField.FEE,
                            origin =
                                EvidenceOrigin.MESSAGE_TEXT,
                            ruleId = it.ruleId,
                            span = it.amountSpan,
                            confidence =
                                InterpretationConfidence.HIGH
                        )
                    )
                }
            }
        )
    }

    private fun MessageSpan.overlaps(
        other: MessageSpan
    ): Boolean =
        startOffset < other.endOffsetExclusive &&
            other.startOffset < endOffsetExclusive

    private fun resolveBalance(
        candidate: BalanceCandidate,
        context: FinancialInterpretationContext
    ): BalanceObservationFact? {
        if (
            FinancialProviderRegistry
                .looksLikeNonFinancialBalanceLabel(
                    candidate.rawAccountLabel
                )
        ) {
            return null
        }

        val account =
            FinancialProviderRegistry.resolveAccount(
                rawLabel = candidate.rawAccountLabel,
                context = context
            )

        return BalanceObservationFact(
            account = account,
            balanceMinor = candidate.amountMinor,
            confidence = InterpretationConfidence.HIGH,
            evidence = listOf(
                evidence(
                    field = FinancialField.ACCOUNT,
                    origin = EvidenceOrigin.MESSAGE_TEXT,
                    ruleId =
                        candidate.ruleId +
                            ".account",
                    span = candidate.accountSpan,
                    confidence =
                        InterpretationConfidence.HIGH
                ),
                evidence(
                    field = FinancialField.BALANCE,
                    origin = EvidenceOrigin.MESSAGE_TEXT,
                    ruleId =
                        candidate.ruleId +
                            ".amount",
                    span = candidate.amountSpan,
                    confidence =
                        InterpretationConfidence.HIGH
                )
            )
        )
    }

    private fun resolveCashMovement(
        candidate: CashMovementCandidate?,
        fee: FeeCandidate?,
        context: FinancialInterpretationContext
    ): ResolvedFinancialRelationship? {
        candidate ?: return null
        val account =
            FinancialProviderRegistry.resolveAccount(
                rawLabel = candidate.accountLabel,
                context = context
            )
        val cash =
            FinancialProviderRegistry.cashAccount()
        val confidence =
            if (
                account.ownershipHint ==
                    AccountOwnershipHint.USER_OWNED
            ) {
                InterpretationConfidence.HIGH
            } else {
                InterpretationConfidence.MEDIUM
            }

        val evidence = buildList {
            add(
                evidence(
                    field = FinancialField.AMOUNT,
                    origin = EvidenceOrigin.MESSAGE_TEXT,
                    ruleId =
                        candidate.ruleId + ".amount",
                    span = candidate.amountSpan,
                    confidence = confidence
                )
            )
            add(
                evidence(
                    field =
                        if (
                            candidate.kind ==
                                CashMovementCandidateKind.WITHDRAWAL
                        ) {
                            FinancialField.SOURCE_ACCOUNT
                        } else {
                            FinancialField.DESTINATION_ACCOUNT
                        },
                    origin = EvidenceOrigin.MESSAGE_TEXT,
                    ruleId =
                        candidate.ruleId + ".account",
                    span = candidate.accountSpan,
                    confidence = confidence
                )
            )
            add(
                evidence(
                    field =
                        if (
                            candidate.kind ==
                                CashMovementCandidateKind.WITHDRAWAL
                        ) {
                            FinancialField.DESTINATION_ACCOUNT
                        } else {
                            FinancialField.SOURCE_ACCOUNT
                        },
                    origin =
                        EvidenceOrigin.FINANCIAL_GRAMMAR,
                    ruleId =
                        candidate.ruleId + ".cash",
                    span = null,
                    confidence = confidence
                )
            )
            candidate.counterpartySpan?.let {
                add(
                    evidence(
                        field =
                            FinancialField.COUNTERPARTY,
                        origin =
                            EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            candidate.ruleId +
                                ".counterparty",
                        span = it,
                        confidence = confidence
                    )
                )
            }
            fee?.let {
                add(
                    evidence(
                        field = FinancialField.FEE,
                        origin =
                            EvidenceOrigin.MESSAGE_TEXT,
                        ruleId = it.ruleId,
                        span = it.amountSpan,
                        confidence =
                            InterpretationConfidence.HIGH
                    )
                )
            }
        }

        return when (candidate.kind) {
            CashMovementCandidateKind.WITHDRAWAL ->
                ResolvedCashWithdrawalRelationship(
                    amountMinor = candidate.amountMinor,
                    source = account,
                    cashDestination = cash,
                    counterparty =
                        candidate.counterpartyLabel,
                    feeMinor = fee?.amountMinor ?: 0L,
                    confidence = confidence,
                    evidence = evidence
                )
            CashMovementCandidateKind.DEPOSIT ->
                ResolvedCashDepositRelationship(
                    amountMinor = candidate.amountMinor,
                    cashSource = cash,
                    destination = account,
                    counterparty =
                        candidate.counterpartyLabel,
                    feeMinor = fee?.amountMinor ?: 0L,
                    confidence = confidence,
                    evidence = evidence
                )
        }
    }

    private fun resolveReversal(
        candidate: ReversalCandidate?,
        observedAccount: FinancialAccountReference?
    ): ResolvedFinancialRelationship? {
        candidate ?: return null
        val confidence =
            if (observedAccount != null) {
                InterpretationConfidence.HIGH
            } else {
                InterpretationConfidence.MEDIUM
            }

        return ResolvedReversalRelationship(
            amountMinor = candidate.amountMinor,
            referencedTransactionCode =
                candidate.referencedTransactionCode,
            observedAccount = observedAccount,
            confidence = confidence,
            evidence = buildList {
                add(
                    evidence(
                        field = FinancialField.AMOUNT,
                        origin = EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            candidate.ruleId + ".amount",
                        span = candidate.amountSpan,
                        confidence = confidence
                    )
                )
                add(
                    evidence(
                        field =
                            FinancialField
                                .RELATED_TRANSACTION_CODE,
                        origin = EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            candidate.ruleId +
                                ".referenced_transaction",
                        span =
                            candidate.referencedCodeSpan,
                        confidence = confidence
                    )
                )
                if (observedAccount != null) {
                    add(
                        evidence(
                            field = FinancialField.ACCOUNT,
                            origin =
                                EvidenceOrigin.PROVIDER_PROFILE,
                            ruleId =
                                candidate.ruleId +
                                    ".observed_account",
                            span = null,
                            confidence = confidence
                        )
                    )
                }
            }
        )
    }

    private fun resolveDebt(
        candidates: FinancialMessageCandidates,
        balances: List<BalanceObservationFact>,
        context: FinancialInterpretationContext
    ): ResolvedFinancialRelationship? {
        candidates.debt.repayment?.let { repayment ->
            val liability =
                resolveLiability(
                    rawLabel =
                        repayment.liabilityLabel,
                    context = context
                )
            val funding =
                FinancialProviderRegistry.resolveAccount(
                    rawLabel =
                        repayment.fundingAccountLabel,
                    context = context
                )
            val walletBalance =
                balances.firstOrNull {
                    it.account.entityKey ==
                        funding.entityKey
                }?.balanceMinor

            val evidence = buildList {
                add(
                    evidence(
                        field = FinancialField.AMOUNT,
                        origin = EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            repayment.ruleId + ".amount",
                        span = repayment.amountSpan,
                        confidence =
                            InterpretationConfidence.HIGH
                    )
                )
                add(
                    evidence(
                        field =
                            FinancialField.SOURCE_ACCOUNT,
                        origin =
                            EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            repayment.ruleId +
                                ".funding_account",
                        span =
                            repayment.fundingAccountSpan,
                        confidence =
                            InterpretationConfidence.HIGH
                    )
                )
                add(
                    evidence(
                        field = FinancialField.LIABILITY,
                        origin =
                            EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            repayment.ruleId +
                                ".liability",
                        span = repayment.liabilitySpan,
                        confidence =
                            InterpretationConfidence.HIGH
                    )
                )
                add(
                    evidence(
                        field =
                            FinancialField.COMPLETION,
                        origin =
                            EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            repayment.ruleId +
                                ".completion",
                        span = null,
                        confidence =
                            InterpretationConfidence.HIGH
                    )
                )
                repayment.availableCreditSpan?.let {
                    add(
                        evidence(
                            field =
                                FinancialField
                                    .AVAILABLE_CREDIT,
                            origin =
                                EvidenceOrigin.MESSAGE_TEXT,
                            ruleId =
                                repayment.ruleId +
                                    ".available_credit",
                            span = it,
                            confidence =
                                InterpretationConfidence.HIGH
                        )
                    )
                }
            }

            return ResolvedDebtRepaymentRelationship(
                amountMinor =
                    repayment.amountMinor,
                liability = liability,
                fundingAccount = funding,
                feeMinor = 0L,
                completion =
                    repayment.completion,
                outstandingAfterMinor =
                    if (
                        repayment.completion ==
                        DebtCompletion.FULL
                    ) {
                        0L
                    } else {
                        null
                    },
                availableCreditAfterMinor =
                    repayment
                        .availableCreditAfterMinor,
                walletBalanceAfterMinor =
                    walletBalance,
                confidence =
                    InterpretationConfidence.HIGH,
                evidence = evidence
            )
        }

        candidates.debt.drawdown?.let { drawdown ->
            val liability =
                resolveLiability(
                    rawLabel =
                        drawdown.liabilityLabel,
                    context = context
                )
            val fee =
                candidates.fee?.amountMinor
                    ?: 0L

            val evidence = buildList {
                add(
                    evidence(
                        field = FinancialField.AMOUNT,
                        origin =
                            EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            drawdown.ruleId + ".amount",
                        span = drawdown.amountSpan,
                        confidence =
                            InterpretationConfidence.HIGH
                    )
                )
                add(
                    evidence(
                        field = FinancialField.LIABILITY,
                        origin =
                            EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            drawdown.ruleId +
                                ".liability",
                        span = drawdown.liabilitySpan,
                        confidence =
                            InterpretationConfidence.HIGH
                    )
                )
                drawdown.outstandingSpan?.let {
                    add(
                        evidence(
                            field =
                                FinancialField
                                    .OUTSTANDING_BALANCE,
                            origin =
                                EvidenceOrigin.MESSAGE_TEXT,
                            ruleId =
                                drawdown.ruleId +
                                    ".outstanding",
                            span = it,
                            confidence =
                                InterpretationConfidence.HIGH
                        )
                    )
                }
                drawdown.availableCreditSpan?.let {
                    add(
                        evidence(
                            field =
                                FinancialField
                                    .AVAILABLE_CREDIT,
                            origin =
                                EvidenceOrigin.MESSAGE_TEXT,
                            ruleId =
                                drawdown.ruleId +
                                    ".available_credit",
                            span = it,
                            confidence =
                                InterpretationConfidence.HIGH
                        )
                    )
                }
                candidates.fee?.let {
                    add(
                        evidence(
                            field = FinancialField.FEE,
                            origin =
                                EvidenceOrigin.MESSAGE_TEXT,
                            ruleId = it.ruleId,
                            span = it.amountSpan,
                            confidence =
                                InterpretationConfidence.HIGH
                        )
                    )
                }
            }

            return ResolvedDebtDrawdownRelationship(
                amountMinor =
                    drawdown.amountMinor,
                liability = liability,
                feeMinor = fee,
                outstandingAfterMinor =
                    drawdown
                        .outstandingAfterMinor,
                availableCreditAfterMinor =
                    drawdown
                        .availableCreditAfterMinor,
                confidence =
                    InterpretationConfidence.HIGH,
                evidence = evidence
            )
        }

        return null
    }

    private fun resolveLiability(
        rawLabel: String,
        context: FinancialInterpretationContext
    ): FinancialAccountReference {
        val resolved =
            FinancialProviderRegistry
                .resolveAccount(
                    rawLabel = rawLabel,
                    context = context
                )

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

    private fun resolveAction(
        action: FinancialActionCandidate,
        observedAccount: FinancialAccountReference?,
        fee: FeeCandidate?,
        context: FinancialInterpretationContext
    ): ResolvedFinancialRelationship? =
        when (action.kind) {
            FinancialActionCandidateKind
                .TRANSFER_FROM_ACCOUNT ->
                resolveTransferFrom(
                    action,
                    observedAccount,
                    fee,
                    context
                )
            FinancialActionCandidateKind
                .TRANSFER_TO_ACCOUNT ->
                resolveTransferTo(
                    action,
                    observedAccount,
                    fee,
                    context
                )
            FinancialActionCandidateKind
                .PAYMENT_SENT_TO ->
                resolvePayment(
                    action,
                    observedAccount,
                    fee,
                    PaymentRouteHint.PERSON_TO_PERSON
                )
            FinancialActionCandidateKind
                .PAYMENT_PAID_TO ->
                resolvePayment(
                    action,
                    observedAccount,
                    fee,
                    if (
                        action.accountReference
                            .isNullOrBlank()
                    ) {
                        PaymentRouteHint.MERCHANT
                    } else {
                        PaymentRouteHint.BILL
                    }
                )
            FinancialActionCandidateKind
                .AIRTIME_PURCHASE ->
                resolvePayment(
                    action,
                    observedAccount,
                    fee,
                    PaymentRouteHint.AIRTIME
                )
            FinancialActionCandidateKind
                .RECEIPT_FROM ->
                resolveReceipt(
                    action,
                    observedAccount,
                    fee
                )
        }

    private fun resolveTransferFrom(
        action: FinancialActionCandidate,
        observedAccount: FinancialAccountReference?,
        fee: FeeCandidate?,
        context: FinancialInterpretationContext
    ): ResolvedFinancialRelationship? {
        val destination = observedAccount ?: return null
        val source =
            FinancialProviderRegistry.resolveAccount(
                rawLabel = action.partyLabel,
                context = context
            )
        if (source.entityKey == destination.entityKey) {
            return null
        }
        val confidence =
            accountRelationshipConfidence(
                source,
                destination
            )
        return ResolvedTransferRelationship(
            amountMinor = action.amountMinor,
            source = source,
            destination = destination,
            feeMinor = fee?.amountMinor ?: 0L,
            confidence = confidence,
            evidence = relationshipEvidence(
                action = action,
                accountField =
                    FinancialField.SOURCE_ACCOUNT,
                confidence = confidence,
                observedField =
                    FinancialField.DESTINATION_ACCOUNT,
                fee = fee
            )
        )
    }

    private fun resolveTransferTo(
        action: FinancialActionCandidate,
        observedAccount: FinancialAccountReference?,
        fee: FeeCandidate?,
        context: FinancialInterpretationContext
    ): ResolvedFinancialRelationship? {
        val source = observedAccount ?: return null
        val destination =
            FinancialProviderRegistry.resolveAccount(
                rawLabel = action.partyLabel,
                context = context
            )
        if (source.entityKey == destination.entityKey) {
            return null
        }
        val confidence =
            accountRelationshipConfidence(
                source,
                destination
            )
        return ResolvedTransferRelationship(
            amountMinor = action.amountMinor,
            source = source,
            destination = destination,
            feeMinor = fee?.amountMinor ?: 0L,
            confidence = confidence,
            evidence = relationshipEvidence(
                action = action,
                accountField =
                    FinancialField.DESTINATION_ACCOUNT,
                confidence = confidence,
                observedField =
                    FinancialField.SOURCE_ACCOUNT,
                fee = fee
            )
        )
    }

    private fun resolvePayment(
        action: FinancialActionCandidate,
        observedAccount: FinancialAccountReference?,
        fee: FeeCandidate?,
        routeHint: PaymentRouteHint
    ): ResolvedFinancialRelationship? {
        val source = observedAccount ?: return null
        return ResolvedPaymentRelationship(
            amountMinor = action.amountMinor,
            source = source,
            counterparty = action.partyLabel,
            counterpartyPhone = action.partyPhone,
            routeHint = routeHint,
            accountReference =
                action.accountReference,
            feeMinor = fee?.amountMinor ?: 0L,
            confidence = InterpretationConfidence.HIGH,
            evidence = counterpartyEvidence(
                action = action,
                observedField =
                    FinancialField.SOURCE_ACCOUNT,
                fee = fee
            )
        )
    }

    private fun resolveReceipt(
        action: FinancialActionCandidate,
        observedAccount: FinancialAccountReference?,
        fee: FeeCandidate?
    ): ResolvedFinancialRelationship? {
        val destination = observedAccount ?: return null

        // Explicit "from X account" is an account transfer, not
        // a generic receipt. Transfer candidates have higher
        // priority, so this guard protects unusual overlaps too.
        if (
            action.partyLabel.trim()
                .endsWith(
                    " account",
                    ignoreCase = true
                )
        ) {
            return null
        }

        return ResolvedReceiptRelationship(
            amountMinor = action.amountMinor,
            destination = destination,
            counterparty = action.partyLabel,
            counterpartyPhone = action.partyPhone,
            feeMinor = fee?.amountMinor ?: 0L,
            confidence = InterpretationConfidence.HIGH,
            evidence = counterpartyEvidence(
                action = action,
                observedField =
                    FinancialField.DESTINATION_ACCOUNT,
                fee = fee
            )
        )
    }

    private fun relationshipEvidence(
        action: FinancialActionCandidate,
        accountField: FinancialField,
        confidence: InterpretationConfidence,
        observedField: FinancialField,
        fee: FeeCandidate?
    ): List<FieldEvidence> =
        buildList {
            add(
                evidence(
                    field = FinancialField.AMOUNT,
                    origin = EvidenceOrigin.MESSAGE_TEXT,
                    ruleId =
                        action.ruleId + ".amount",
                    span = action.amountSpan,
                    confidence = confidence
                )
            )
            add(
                evidence(
                    field = accountField,
                    origin = EvidenceOrigin.MESSAGE_TEXT,
                    ruleId =
                        action.ruleId + ".account",
                    span = action.partySpan,
                    confidence = confidence
                )
            )
            add(
                evidence(
                    field = observedField,
                    origin =
                        EvidenceOrigin.PROVIDER_PROFILE,
                    ruleId =
                        action.ruleId +
                            ".observed_wallet",
                    span = null,
                    confidence = confidence
                )
            )
            fee?.let {
                add(
                    evidence(
                        field = FinancialField.FEE,
                        origin =
                            EvidenceOrigin.MESSAGE_TEXT,
                        ruleId = it.ruleId,
                        span = it.amountSpan,
                        confidence =
                            InterpretationConfidence.HIGH
                    )
                )
            }
        }

    private fun counterpartyEvidence(
        action: FinancialActionCandidate,
        observedField: FinancialField,
        fee: FeeCandidate?
    ): List<FieldEvidence> =
        buildList {
            add(
                evidence(
                    field = FinancialField.AMOUNT,
                    origin = EvidenceOrigin.MESSAGE_TEXT,
                    ruleId =
                        action.ruleId + ".amount",
                    span = action.amountSpan,
                    confidence =
                        InterpretationConfidence.HIGH
                )
            )
            add(
                evidence(
                    field = FinancialField.COUNTERPARTY,
                    origin = EvidenceOrigin.MESSAGE_TEXT,
                    ruleId =
                        action.ruleId +
                            ".counterparty",
                    span = action.partySpan,
                    confidence =
                        InterpretationConfidence.HIGH
                )
            )
            add(
                evidence(
                    field = observedField,
                    origin =
                        EvidenceOrigin.PROVIDER_PROFILE,
                    ruleId =
                        action.ruleId +
                            ".observed_wallet",
                    span = null,
                    confidence =
                        InterpretationConfidence.HIGH
                )
            )
            if (
                !action.accountReference.isNullOrBlank()
            ) {
                add(
                    evidence(
                        field =
                            FinancialField.ACCOUNT_REFERENCE,
                        origin =
                            EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            action.ruleId +
                                ".account_reference",
                        span =
                            action.accountReferenceSpan,
                        confidence =
                            InterpretationConfidence.HIGH
                    )
                )
            }
            fee?.let {
                add(
                    evidence(
                        field = FinancialField.FEE,
                        origin =
                            EvidenceOrigin.MESSAGE_TEXT,
                        ruleId = it.ruleId,
                        span = it.amountSpan,
                        confidence =
                            InterpretationConfidence.HIGH
                    )
                )
            }
        }

    private fun accountRelationshipConfidence(
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

    private fun priority(
        action: FinancialActionCandidate
    ): Int =
        when (action.kind) {
            FinancialActionCandidateKind
                .TRANSFER_FROM_ACCOUNT -> 0
            FinancialActionCandidateKind
                .TRANSFER_TO_ACCOUNT -> 1
            FinancialActionCandidateKind
                .PAYMENT_SENT_TO -> 2
            FinancialActionCandidateKind
                .PAYMENT_PAID_TO -> 2
            FinancialActionCandidateKind
                .AIRTIME_PURCHASE -> 2
            FinancialActionCandidateKind
                .RECEIPT_FROM -> 3
        }

    private fun evidence(
        field: FinancialField,
        origin: EvidenceOrigin,
        ruleId: String,
        span: MessageSpan?,
        confidence: InterpretationConfidence
    ): FieldEvidence =
        FieldEvidence(
            field = field,
            origin = origin,
            ruleId = ruleId,
            span = span,
            confidence = confidence
        )
}
