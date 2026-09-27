package com.kedevjimmy.pesatrack.domain.interpretation

object FinancialEventClassifier {
    fun classify(
        resolved: ResolvedFinancialMessage
    ): FinancialMessageInterpretation {
        val relationship = resolved.relationship
        val events = buildList<FinancialEventFact> {
            when (relationship) {
                is ResolvedTransferRelationship ->
                    add(
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
                    add(
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
                    add(
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
                is ResolvedDebtDrawdownRelationship ->
                    add(
                        DebtDrawdownEventFact(
                            amountMinor =
                                relationship.amountMinor,
                            liability =
                                relationship.liability,
                            feeMinor =
                                relationship.feeMinor,
                            outstandingAfterMinor =
                                relationship
                                    .outstandingAfterMinor,
                            confidence =
                                relationship.confidence,
                            evidence =
                                relationship.evidence
                        )
                    )
                is ResolvedDebtRepaymentRelationship ->
                    add(
                        DebtRepaymentEventFact(
                            amountMinor =
                                relationship.amountMinor,
                            liability =
                                relationship.liability,
                            fundingAccount =
                                relationship.fundingAccount,
                            completion =
                                relationship.completion,
                            outstandingAfterMinor =
                                relationship
                                    .outstandingAfterMinor,
                            confidence =
                                relationship.confidence,
                            evidence =
                                relationship.evidence
                        )
                    )
                null -> Unit
            }

            resolved.availableCreditObservations
                .forEach { observation ->
                    add(
                        AvailableCreditObservationFact(
                            liability =
                                observation.liability,
                            availableCreditMinor =
                                observation
                                    .availableCreditMinor,
                            confidence =
                                observation.confidence,
                            evidence =
                                observation.evidence
                        )
                    )
                }
        }

        val runtimeSupported =
            relationship is ResolvedTransferRelationship ||
                relationship is ResolvedPaymentRelationship ||
                relationship is ResolvedReceiptRelationship

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
            },
            legacyFallbackRequired =
                !runtimeSupported
        )
    }
}
