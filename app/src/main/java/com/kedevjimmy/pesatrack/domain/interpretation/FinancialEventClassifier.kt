package com.kedevjimmy.pesatrack.domain.interpretation

object FinancialEventClassifier {
    fun classify(
        resolved: ResolvedFinancialMessage,
        suppressMovementEvents: Boolean = false
    ): FinancialMessageInterpretation {
        val relationship = resolved.relationship
        val events =
            if (suppressMovementEvents) {
                emptyList()
            } else when (relationship) {
            is ResolvedTransferRelationship ->
                listOf(
                    TransferEventFact(
                        amountMinor =
                            relationship.amountMinor,
                        source = relationship.source,
                        destination =
                            relationship.destination,
                        feeMinor =
                            relationship.feeMinor,
                        confidence =
                            relationship.confidence,
                        evidence =
                            relationship.evidence
                    )
                )
            is ResolvedPaymentRelationship ->
                listOf(
                    PaymentEventFact(
                        amountMinor =
                            relationship.amountMinor,
                        source = relationship.source,
                        counterparty =
                            relationship.counterparty,
                        counterpartyPhone =
                            relationship
                                .counterpartyPhone,
                        routeHint =
                            relationship.routeHint,
                        accountReference =
                            relationship.accountReference,
                        feeMinor =
                            relationship.feeMinor,
                        confidence =
                            relationship.confidence,
                        evidence =
                            relationship.evidence
                    )
                )
            is ResolvedReceiptRelationship ->
                listOf(
                    ReceiptEventFact(
                        amountMinor =
                            relationship.amountMinor,
                        destination =
                            relationship.destination,
                        counterparty =
                            relationship.counterparty,
                        counterpartyPhone =
                            relationship
                                .counterpartyPhone,
                        feeMinor =
                            relationship.feeMinor,
                        confidence =
                            relationship.confidence,
                        evidence =
                            relationship.evidence
                    )
                )
            is ResolvedCashWithdrawalRelationship ->
                listOf(
                    CashWithdrawalEventFact(
                        amountMinor =
                            relationship.amountMinor,
                        source = relationship.source,
                        cashDestination =
                            relationship.cashDestination,
                        counterparty =
                            relationship.counterparty,
                        feeMinor = relationship.feeMinor,
                        confidence =
                            relationship.confidence,
                        evidence =
                            relationship.evidence
                    )
                )
            is ResolvedCashDepositRelationship ->
                listOf(
                    CashDepositEventFact(
                        amountMinor =
                            relationship.amountMinor,
                        cashSource =
                            relationship.cashSource,
                        destination =
                            relationship.destination,
                        counterparty =
                            relationship.counterparty,
                        feeMinor = relationship.feeMinor,
                        confidence =
                            relationship.confidence,
                        evidence =
                            relationship.evidence
                    )
                )
            is ResolvedUnresolvedMovementRelationship ->
                listOf(
                    UnresolvedFinancialEventFact(
                        amountMinor =
                            relationship.amountMinor,
                        observedAccount =
                            relationship.observedAccount,
                        feeMinor =
                            relationship.feeMinor,
                        confidence =
                            relationship.confidence,
                        evidence =
                            relationship.evidence
                    )
                )
            is ResolvedReversalRelationship ->
                listOf(
                    ReversalEventFact(
                        amountMinor =
                            relationship.amountMinor,
                        referencedTransactionCode =
                            relationship
                                .referencedTransactionCode,
                        observedAccount =
                            relationship.observedAccount,
                        confidence =
                            relationship.confidence,
                        evidence =
                            relationship.evidence
                    )
                )
            is ResolvedDebtDrawdownRelationship ->
                listOf(
                    DebtDrawdownEventFact(
                        amountMinor =
                            relationship.amountMinor,
                        liability =
                            relationship.liability,
                        proceedsAccount =
                            relationship.proceedsAccount,
                        amountIsNetProceeds =
                            relationship
                                .amountIsNetProceeds,
                        feeMinor =
                            relationship.feeMinor,
                        outstandingAfterMinor =
                            relationship
                                .outstandingAfterMinor,
                        availableCreditAfterMinor =
                            relationship
                                .availableCreditAfterMinor,
                        confidence =
                            relationship.confidence,
                        evidence =
                            relationship.evidence
                    )
                )
            is ResolvedDebtRepaymentRelationship ->
                listOf(
                    DebtRepaymentEventFact(
                        amountMinor =
                            relationship.amountMinor,
                        liability =
                            relationship.liability,
                        fundingAccount =
                            relationship
                                .fundingAccount,
                        completion =
                            relationship.completion,
                        outstandingAfterMinor =
                            relationship
                                .outstandingAfterMinor,
                        availableCreditAfterMinor =
                            relationship
                                .availableCreditAfterMinor,
                        walletBalanceAfterMinor =
                            relationship
                                .walletBalanceAfterMinor,
                        confidence =
                            relationship.confidence,
                        evidence =
                            relationship.evidence
                    )
                )
            null -> emptyList()
        }

        return FinancialMessageInterpretation(
            transactionCode =
                resolved.transactionCode
                    ?.value
                    ?.uppercase(),
            occurredAtText =
                resolved.occurredAt?.value,
            balances = resolved.balances,
            events = events,
            metadataEvidence = buildList {
                resolved.transactionCode?.let {
                    add(
                        FieldEvidence(
                            field =
                                FinancialField
                                    .TRANSACTION_CODE,
                            origin =
                                EvidenceOrigin.MESSAGE_TEXT,
                            ruleId = it.ruleId,
                            span = it.span,
                            confidence =
                                InterpretationConfidence.HIGH
                        )
                    )
                }
                resolved.occurredAt?.let {
                    add(
                        FieldEvidence(
                            field =
                                FinancialField.OCCURRED_AT,
                            origin =
                                EvidenceOrigin.MESSAGE_TEXT,
                            ruleId = it.ruleId,
                            span = it.span,
                            confidence =
                                InterpretationConfidence.HIGH
                        )
                    )
                }
            }
        )
    }
}
