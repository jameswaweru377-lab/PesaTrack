package com.kedevjimmy.pesatrack.domain.interpretation

fun FinancialMessageInterpretation.toCanonicalDraft():
    CanonicalFinancialMessageDraft {
    val records = buildList<CanonicalFinancialRecord> {
        balances.forEach { balance ->
            add(
                BalanceObservationDraft(
                    account = balance.account,
                    balanceMinor = balance.balanceMinor,
                    confidence = balance.confidence,
                    evidence = balance.evidence
                )
            )
        }

        events.forEach { event ->
            when (event) {
                is TransferEventFact -> {
                    add(
                        MoneyMovementDraft(
                            kind =
                                CanonicalFinancialRecordKind.TRANSFER,
                            amountMinor = event.amountMinor,
                            source = event.source,
                            destination = event.destination,
                            feeMinor = event.feeMinor,
                            confidence = event.confidence,
                            evidence = event.evidence
                        )
                    )
                    if (event.feeMinor > 0L) {
                        add(
                            FeeDraft(
                                amountMinor = event.feeMinor,
                                chargedTo = event.source,
                                confidence = event.confidence,
                                evidence = event.evidence.filter {
                                    it.field == FinancialField.FEE
                                }
                            )
                        )
                    }
                }
                is PaymentEventFact -> {
                    add(
                        MoneyMovementDraft(
                            kind =
                                CanonicalFinancialRecordKind.PAYMENT,
                            amountMinor = event.amountMinor,
                            source = event.source,
                            destination = null,
                            counterparty = event.counterparty,
                            feeMinor = event.feeMinor,
                            confidence = event.confidence,
                            evidence = event.evidence
                        )
                    )
                    if (event.feeMinor > 0L) {
                        add(
                            FeeDraft(
                                amountMinor = event.feeMinor,
                                chargedTo = event.source,
                                confidence = event.confidence,
                                evidence = event.evidence.filter {
                                    it.field == FinancialField.FEE
                                }
                            )
                        )
                    }
                }
                is ReceiptEventFact -> {
                    add(
                        MoneyMovementDraft(
                            kind =
                                CanonicalFinancialRecordKind.RECEIPT,
                            amountMinor = event.amountMinor,
                            source = null,
                            destination = event.destination,
                            counterparty = event.counterparty,
                            feeMinor = event.feeMinor,
                            confidence = event.confidence,
                            evidence = event.evidence
                        )
                    )
                    if (event.feeMinor > 0L) {
                        add(
                            FeeDraft(
                                amountMinor = event.feeMinor,
                                chargedTo = event.destination,
                                confidence = event.confidence,
                                evidence = event.evidence.filter {
                                    it.field == FinancialField.FEE
                                }
                            )
                        )
                    }
                }
                is CashWithdrawalEventFact -> {
                    add(
                        MoneyMovementDraft(
                            kind =
                                CanonicalFinancialRecordKind.CASH_WITHDRAWAL,
                            amountMinor = event.amountMinor,
                            source = event.source,
                            destination =
                                event.cashDestination,
                            counterparty =
                                event.counterparty,
                            feeMinor = event.feeMinor,
                            confidence =
                                event.confidence,
                            evidence = event.evidence
                        )
                    )
                    if (event.feeMinor > 0L) {
                        add(
                            FeeDraft(
                                amountMinor =
                                    event.feeMinor,
                                chargedTo = event.source,
                                confidence =
                                    event.confidence,
                                evidence =
                                    event.evidence.filter {
                                        it.field ==
                                            FinancialField.FEE
                                    }
                            )
                        )
                    }
                }
                is CashDepositEventFact -> {
                    add(
                        MoneyMovementDraft(
                            kind =
                                CanonicalFinancialRecordKind.CASH_DEPOSIT,
                            amountMinor = event.amountMinor,
                            source = event.cashSource,
                            destination =
                                event.destination,
                            counterparty =
                                event.counterparty,
                            feeMinor = event.feeMinor,
                            confidence =
                                event.confidence,
                            evidence = event.evidence
                        )
                    )
                    if (event.feeMinor > 0L) {
                        add(
                            FeeDraft(
                                amountMinor =
                                    event.feeMinor,
                                chargedTo =
                                    event.destination,
                                confidence =
                                    event.confidence,
                                evidence =
                                    event.evidence.filter {
                                        it.field ==
                                            FinancialField.FEE
                                    }
                            )
                        )
                    }
                }
                is UnresolvedFinancialEventFact -> {
                    add(
                        UnresolvedMovementDraft(
                            amountMinor = event.amountMinor,
                            observedAccount =
                                event.observedAccount,
                            feeMinor = event.feeMinor,
                            confidence =
                                event.confidence,
                            evidence = event.evidence
                        )
                    )
                    if (event.feeMinor > 0L) {
                        add(
                            FeeDraft(
                                amountMinor =
                                    event.feeMinor,
                                chargedTo =
                                    event.observedAccount,
                                confidence =
                                    event.confidence,
                                evidence =
                                    event.evidence.filter {
                                        it.field ==
                                            FinancialField.FEE
                                    }
                            )
                        )
                    }
                }
                is ReversalEventFact -> {
                    add(
                        ReversalDraft(
                            amountMinor = event.amountMinor,
                            referencedTransactionCode =
                                event.referencedTransactionCode,
                            observedAccount =
                                event.observedAccount,
                            confidence = event.confidence,
                            evidence = event.evidence
                        )
                    )
                }
                is DebtDrawdownEventFact -> {
                    add(
                        DebtMovementDraft(
                            kind =
                                CanonicalFinancialRecordKind.DEBT_DRAWDOWN,
                            amountMinor = event.amountMinor,
                            liability = event.liability,
                            fundingAccount = null,
                            proceedsAccount =
                                event.proceedsAccount,
                            amountIsNetProceeds =
                                event.amountIsNetProceeds,
                            completion =
                                DebtCompletion.UNKNOWN,
                            outstandingAfterMinor =
                                event.outstandingAfterMinor,
                            confidence = event.confidence,
                            evidence = event.evidence
                        )
                    )
                    if (event.feeMinor > 0L) {
                        add(
                            FeeDraft(
                                amountMinor = event.feeMinor,
                                chargedTo = event.liability,
                                confidence = event.confidence,
                                evidence = event.evidence.filter {
                                    it.field == FinancialField.FEE
                                }
                            )
                        )
                    }
                    event.availableCreditAfterMinor
                        ?.let { available ->
                            add(
                                AvailableCreditObservationDraft(
                                    liability = event.liability,
                                    availableCreditMinor =
                                        available,
                                    confidence =
                                        event.confidence,
                                    evidence =
                                        event.evidence.filter {
                                            it.field ==
                                                FinancialField
                                                    .AVAILABLE_CREDIT
                                        }
                                )
                            )
                        }
                }
                is DebtRepaymentEventFact -> {
                    add(
                        DebtMovementDraft(
                            kind =
                                CanonicalFinancialRecordKind.DEBT_REPAYMENT,
                            amountMinor = event.amountMinor,
                            liability = event.liability,
                            fundingAccount =
                                event.fundingAccount,
                            completion =
                                event.completion,
                            outstandingAfterMinor =
                                event.outstandingAfterMinor,
                            confidence = event.confidence,
                            evidence = event.evidence
                        )
                    )
                    event.availableCreditAfterMinor
                        ?.let { available ->
                            add(
                                AvailableCreditObservationDraft(
                                    liability = event.liability,
                                    availableCreditMinor =
                                        available,
                                    confidence =
                                        event.confidence,
                                    evidence =
                                        event.evidence.filter {
                                            it.field ==
                                                FinancialField
                                                    .AVAILABLE_CREDIT
                                        }
                                )
                            )
                        }
                }
            }
        }
    }

    return CanonicalFinancialMessageDraft(
        transactionCode = transactionCode,
        occurredAtText = occurredAtText,
        records = records,
        metadataEvidence = metadataEvidence,
        requiresLegacyFallback =
            !consumesLegacyTransactionParsing,
        semanticAssessment =
            semanticAssessment
    )
}
