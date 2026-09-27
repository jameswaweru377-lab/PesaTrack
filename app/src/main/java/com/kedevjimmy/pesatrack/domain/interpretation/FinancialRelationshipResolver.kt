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

data class ResolvedDebtDrawdownRelationship(
    override val amountMinor: Long,
    val liability: FinancialAccountReference,
    val outstandingAfterMinor: Long?,
    override val feeMinor: Long,
    override val confidence: InterpretationConfidence,
    override val evidence: List<FieldEvidence>
) : ResolvedFinancialRelationship

data class ResolvedDebtRepaymentRelationship(
    override val amountMinor: Long,
    val liability: FinancialAccountReference,
    val fundingAccount: FinancialAccountReference?,
    val completion: DebtCompletion,
    val outstandingAfterMinor: Long?,
    override val feeMinor: Long = 0L,
    override val confidence: InterpretationConfidence,
    override val evidence: List<FieldEvidence>
) : ResolvedFinancialRelationship

data class ResolvedAvailableCreditObservation(
    val liability: FinancialAccountReference,
    val availableCreditMinor: Long,
    val confidence: InterpretationConfidence,
    val evidence: List<FieldEvidence>
)

data class ResolvedFinancialMessage(
    val transactionCode: TextValueCandidate?,
    val occurredAt: TextValueCandidate?,
    val balances: List<BalanceObservationFact>,
    val relationship: ResolvedFinancialRelationship?,
    val availableCreditObservations:
        List<ResolvedAvailableCreditObservation>
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
            candidates.debtRepayments
                .firstNotNullOfOrNull {
                    resolveDebtRepayment(
                        candidate = it,
                        outstandingCandidates =
                            candidates.outstandingDebts
                    )
                }
                ?: candidates.debtDrawdowns
                    .firstNotNullOfOrNull {
                        resolveDebtDrawdown(
                            candidate = it,
                            outstandingCandidates =
                                candidates.outstandingDebts,
                            fee = candidates.fee
                        )
                    }

        val prioritized = candidates.actions
            .sortedBy(::priority)

        val relationship =
            debtRelationship ?:
                prioritized
                    .firstNotNullOfOrNull { action ->
                        resolveAction(
                            action = action,
                            observedWallet =
                                observedWallet,
                            fee = candidates.fee
                        )
                    }

        val creditObservations =
            candidates.availableCredits
                .mapNotNull(
                    ::resolveAvailableCredit
                )

        return ResolvedFinancialMessage(
            transactionCode = candidates.transactionCode,
            occurredAt = candidates.occurredAt,
            balances = balances,
            relationship = relationship,
            availableCreditObservations =
                creditObservations
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

    private fun resolveDebtDrawdown(
        candidate: DebtDrawdownCandidate,
        outstandingCandidates:
            List<OutstandingDebtCandidate>,
        fee: FeeCandidate?
    ): ResolvedDebtDrawdownRelationship? {
        val liability =
            FinancialProviderRegistry.resolveAccount(
                candidate.rawLiabilityLabel
            )
        if (
            liability.nature ==
            FinancialAccountNature.ASSET
        ) {
            return null
        }

        val outstanding =
            matchingOutstanding(
                liability = liability,
                candidates = outstandingCandidates
            )
        val confidence =
            if (
                liability.nature ==
                FinancialAccountNature.LIABILITY
            ) {
                InterpretationConfidence.HIGH
            } else {
                InterpretationConfidence.MEDIUM
            }

        return ResolvedDebtDrawdownRelationship(
            amountMinor = candidate.amountMinor,
            liability = liability,
            outstandingAfterMinor =
                outstanding?.outstandingMinor,
            feeMinor = fee?.amountMinor ?: 0L,
            confidence = confidence,
            evidence = buildList {
                add(
                    evidence(
                        field = FinancialField.LIABILITY,
                        origin =
                            EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            candidate.ruleId +
                                ".liability",
                        span = candidate.liabilitySpan,
                        confidence = confidence
                    )
                )
                add(
                    evidence(
                        field = FinancialField.AMOUNT,
                        origin =
                            EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            candidate.ruleId +
                                ".amount",
                        span = candidate.amountSpan,
                        confidence = confidence
                    )
                )
                outstanding?.let {
                    add(
                        evidence(
                            field =
                                FinancialField
                                    .OUTSTANDING_BALANCE,
                            origin =
                                EvidenceOrigin.MESSAGE_TEXT,
                            ruleId = it.ruleId,
                            span = it.amountSpan,
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
        )
    }

    private fun resolveDebtRepayment(
        candidate: DebtRepaymentCandidate,
        outstandingCandidates:
            List<OutstandingDebtCandidate>
    ): ResolvedDebtRepaymentRelationship? {
        val liability =
            FinancialProviderRegistry.resolveAccount(
                candidate.rawLiabilityLabel
            )
        if (
            liability.nature ==
            FinancialAccountNature.ASSET
        ) {
            return null
        }
        val funding =
            FinancialProviderRegistry.resolveAccount(
                candidate.rawFundingLabel
            )
        val outstanding =
            if (
                candidate.completion ==
                DebtCompletion.FULL
            ) {
                0L
            } else {
                matchingOutstanding(
                    liability = liability,
                    candidates = outstandingCandidates
                )?.outstandingMinor
            }
        val confidence =
            if (
                liability.nature ==
                    FinancialAccountNature.LIABILITY &&
                funding.nature ==
                    FinancialAccountNature.ASSET
            ) {
                InterpretationConfidence.HIGH
            } else {
                InterpretationConfidence.MEDIUM
            }

        return ResolvedDebtRepaymentRelationship(
            amountMinor = candidate.amountMinor,
            liability = liability,
            fundingAccount = funding,
            completion = candidate.completion,
            outstandingAfterMinor = outstanding,
            confidence = confidence,
            evidence = buildList {
                add(
                    evidence(
                        field = FinancialField.AMOUNT,
                        origin =
                            EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            candidate.ruleId +
                                ".amount",
                        span = candidate.amountSpan,
                        confidence = confidence
                    )
                )
                add(
                    evidence(
                        field =
                            FinancialField.SOURCE_ACCOUNT,
                        origin =
                            EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            candidate.ruleId +
                                ".funding_account",
                        span = candidate.fundingSpan,
                        confidence = confidence
                    )
                )
                add(
                    evidence(
                        field = FinancialField.LIABILITY,
                        origin =
                            EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            candidate.ruleId +
                                ".liability",
                        span = candidate.liabilitySpan,
                        confidence = confidence
                    )
                )
                add(
                    evidence(
                        field = FinancialField.COMPLETION,
                        origin =
                            EvidenceOrigin.MESSAGE_TEXT,
                        ruleId =
                            candidate.ruleId +
                                ".completion",
                        span = null,
                        confidence = confidence
                    )
                )
            }
        )
    }

    private fun resolveAvailableCredit(
        candidate: AvailableCreditCandidate
    ): ResolvedAvailableCreditObservation? {
        val liability =
            FinancialProviderRegistry.resolveAccount(
                candidate.rawLiabilityLabel
            )
        if (
            liability.nature ==
            FinancialAccountNature.ASSET
        ) {
            return null
        }
        val confidence =
            if (
                liability.nature ==
                FinancialAccountNature.LIABILITY
            ) {
                InterpretationConfidence.HIGH
            } else {
                InterpretationConfidence.MEDIUM
            }
        return ResolvedAvailableCreditObservation(
            liability = liability,
            availableCreditMinor =
                candidate.availableCreditMinor,
            confidence = confidence,
            evidence = listOf(
                evidence(
                    field = FinancialField.LIABILITY,
                    origin =
                        EvidenceOrigin.MESSAGE_TEXT,
                    ruleId =
                        candidate.ruleId +
                            ".liability",
                    span = candidate.liabilitySpan,
                    confidence = confidence
                ),
                evidence(
                    field =
                        FinancialField.AVAILABLE_CREDIT,
                    origin =
                        EvidenceOrigin.MESSAGE_TEXT,
                    ruleId =
                        candidate.ruleId +
                            ".amount",
                    span = candidate.amountSpan,
                    confidence = confidence
                )
            )
        )
    }

    private fun matchingOutstanding(
        liability: FinancialAccountReference,
        candidates: List<OutstandingDebtCandidate>
    ): OutstandingDebtCandidate? =
        candidates.firstOrNull { candidate ->
            val candidateLiability =
                FinancialProviderRegistry.resolveAccount(
                    candidate.rawLiabilityLabel
                )
            candidateLiability.entityKey ==
                liability.entityKey
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
