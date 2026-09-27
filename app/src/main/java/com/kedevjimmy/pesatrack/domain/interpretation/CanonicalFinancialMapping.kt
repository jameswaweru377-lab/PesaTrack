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
            }
        }
    }

    return CanonicalFinancialMessageDraft(
        transactionCode = transactionCode,
        occurredAtText = occurredAtText,
        records = records,
        metadataEvidence = metadataEvidence,
        requiresLegacyFallback =
            !consumesLegacyTransactionParsing
    )
}
