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
        candidates: FinancialMessageCandidates
    ): ResolvedFinancialMessage {
        val balances = candidates.balances
            .mapNotNull(::resolveBalance)
            .distinctBy {
                it.account.entityKey to
                    it.balanceMinor
            }

        val observedWallet =
            balances.firstOrNull {
                it.account.providerKey ==
                    "SAFARICOM_MPESA"
            }?.account ?: if (
                message.title.orEmpty()
                    .contains(
                        "MPESA",
                        ignoreCase = true
                    ) ||
                message.title.orEmpty()
                    .contains(
                        "M-PESA",
                        ignoreCase = true
                    )
            ) {
                FinancialProviderRegistry.mpesaAccount()
            } else {
                null
            }

        val debtRelationship =
            resolveDebt(
                candidates = candidates,
                balances = balances
            )
        val reversalRelationship =
            resolveReversal(
                candidate = candidates.reversal,
                observedAccount = observedWallet
            )

        val prioritized = candidates.actions
            .sortedBy(::priority)

        val relationship =
            debtRelationship
                ?: reversalRelationship
                ?: prioritized.firstNotNullOfOrNull { action ->
                resolveAction(
                    action = action,
                    observedWallet = observedWallet,
                    fee = candidates.fee
                )
            }

        return ResolvedFinancialMessage(
            transactionCode = candidates.transactionCode,
            occurredAt = candidates.occurredAt,
            balances = balances,
            relationship = relationship
        )
    }

    private fun resolveBalance(
        candidate: BalanceCandidate
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
                candidate.rawAccountLabel
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
        balances: List<BalanceObservationFact>
    ): ResolvedFinancialRelationship? {
        candidates.debt.repayment?.let { repayment ->
            val liability =
                resolveLiability(
                    repayment.liabilityLabel
                )
            val funding =
                FinancialProviderRegistry.resolveAccount(
                    repayment.fundingAccountLabel
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
                    drawdown.liabilityLabel
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
        rawLabel: String
    ): FinancialAccountReference {
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

    private fun resolveAction(
        action: FinancialActionCandidate,
        observedWallet: FinancialAccountReference?,
        fee: FeeCandidate?
    ): ResolvedFinancialRelationship? =
        when (action.kind) {
            FinancialActionCandidateKind
                .TRANSFER_FROM_ACCOUNT ->
                resolveTransferFrom(
                    action,
                    observedWallet,
                    fee
                )
            FinancialActionCandidateKind
                .TRANSFER_TO_ACCOUNT ->
                resolveTransferTo(
                    action,
                    observedWallet,
                    fee
                )
            FinancialActionCandidateKind
                .PAYMENT_SENT_TO ->
                resolvePayment(
                    action,
                    observedWallet,
                    fee,
                    PaymentRouteHint.PERSON_TO_PERSON
                )
            FinancialActionCandidateKind
                .PAYMENT_PAID_TO ->
                resolvePayment(
                    action,
                    observedWallet,
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
                .RECEIPT_FROM ->
                resolveReceipt(
                    action,
                    observedWallet,
                    fee
                )
        }

    private fun resolveTransferFrom(
        action: FinancialActionCandidate,
        observedWallet: FinancialAccountReference?,
        fee: FeeCandidate?
    ): ResolvedFinancialRelationship? {
        val destination = observedWallet ?: return null
        val source =
            FinancialProviderRegistry.resolveAccount(
                action.partyLabel
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
        observedWallet: FinancialAccountReference?,
        fee: FeeCandidate?
    ): ResolvedFinancialRelationship? {
        val source = observedWallet ?: return null
        val destination =
            FinancialProviderRegistry.resolveAccount(
                action.partyLabel
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
        observedWallet: FinancialAccountReference?,
        fee: FeeCandidate?,
        routeHint: PaymentRouteHint
    ): ResolvedFinancialRelationship? {
        val source = observedWallet ?: return null
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
        observedWallet: FinancialAccountReference?,
        fee: FeeCandidate?
    ): ResolvedFinancialRelationship? {
        val destination = observedWallet ?: return null

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
