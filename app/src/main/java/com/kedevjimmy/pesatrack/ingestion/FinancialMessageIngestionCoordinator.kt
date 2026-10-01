package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.DebtStore
import com.kedevjimmy.pesatrack.data.local.FinancialEventStatus
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.InvestmentStore
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.ingestion.TransactionDateResolver
import com.kedevjimmy.pesatrack.domain.interpretation.CashDepositEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.CashWithdrawalEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.CanonicalFinancialRecordKind
import com.kedevjimmy.pesatrack.domain.interpretation.DebtDrawdownEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.DebtRepaymentEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpretation
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpreter
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageNormalizer
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageRelevanceGate
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialProviderRegistry
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialSemanticIntent
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialSemanticRole
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountNature
import com.kedevjimmy.pesatrack.domain.interpretation.DebtCompletion
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentRouteHint
import com.kedevjimmy.pesatrack.domain.interpretation.ReceiptEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.ReversalEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.TransferEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.UnresolvedFinancialEventFact
import com.kedevjimmy.pesatrack.domain.rules.InterpretedRuleResolver
import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferMatch
import com.kedevjimmy.pesatrack.domain.rules.OwnTransferEvidence

data class FinancialMessageIngestionResult(
    val recognized: Boolean,
    val consumesLegacyTransactionParsing: Boolean,
    val changed: Boolean,
    val insertedTransactions: Int,
    val insertedFinancialEvents: Int,
    val pendingReviewEvents: Int
)

class FinancialMessageIngestionCoordinator private constructor(
    context: Context
) {
    private val appContext = context.applicationContext
    private val understandingStore =
        FinancialUnderstandingStore.get(appContext)
    private val transactionStore =
        TransactionStore.get(appContext)
    private val diagnosticsStore =
        FinancialIngestionDiagnosticsStore.get(
            appContext
        )

    @Synchronized
    fun ingest(
        title: String?,
        body: String,
        sourcePackage: String?,
        sourceKind: String,
        receivedAtEpochMs: Long,
        rebuildPayCycles: Boolean = true
    ): FinancialMessageIngestionResult {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = title,
                body = body
            )

        val occurredAtEpochMs =
            TransactionDateResolver.resolve(
                occurredAtText =
                    interpretation.occurredAtText,
                fallbackEpochMs = receivedAtEpochMs
            )
        val fingerprint =
            FinancialUnderstandingStore.fingerprint(
                title = title,
                body = body
            )
        val correlationReferences =
            interpretation.semanticAssessment
                ?.references
                ?.map { it.value }
                ?.toSet()
                .orEmpty()
        val investmentWrite =
            interpretation.semanticAssessment
                ?.investment
                ?.let { investment ->
                    InvestmentStore.get(appContext)
                        .record(
                            fact = investment,
                            references =
                                correlationReferences,
                            messageFingerprint =
                                fingerprint,
                            sourcePackage =
                                sourcePackage,
                            sourceKind = sourceKind,
                            occurredAtEpochMs =
                                occurredAtEpochMs
                        )
                }

        if (!interpretation.recognized) {
            val result =
                FinancialMessageIngestionResult(
                    recognized = false,
                    consumesLegacyTransactionParsing =
                        false,
                    changed =
                        investmentWrite?.changed ==
                            true,
                    insertedTransactions = 0,
                    insertedFinancialEvents =
                        if (
                            investmentWrite
                                ?.inserted == true
                        ) {
                            1
                        } else {
                            0
                        },
                    pendingReviewEvents = 0
                )
            recordDiagnostic(
                title = title,
                body = body,
                interpretation = interpretation,
                sourcePackage = sourcePackage,
                sourceKind = sourceKind,
                receivedAtEpochMs =
                    receivedAtEpochMs,
                result = result
            )
            return result
        }

        val rules =
            RuleStore.get(appContext)
                .listRules(enabledOnly = true)

        var changed =
            investmentWrite?.changed == true
        interpretation.balances.forEach { fact ->
            changed =
                understandingStore.recordBalance(
                    fact = fact,
                    messageFingerprint = fingerprint,
                    observedAtEpochMs = occurredAtEpochMs,
                    sourcePackage = sourcePackage,
                    sourceKind = sourceKind
                ) || changed
        }

        var insertedTransactions = 0
        var insertedFinancialEvents =
            if (
                investmentWrite?.inserted ==
                    true
            ) {
                1
            } else {
                0
            }
        var pendingReviewEvents = 0

        interpretation.events.forEach { event ->
            when (event) {
                is TransferEventFact -> {
                    understandingStore.observeEntity(
                        reference = event.source,
                        observedAtEpochMs =
                            occurredAtEpochMs
                    )
                    understandingStore.observeEntity(
                        reference = event.destination,
                        observedAtEpochMs =
                            occurredAtEpochMs
                    )
                    val sourceAccountId =
                        understandingStore.accountIdFor(
                            event.source
                        )
                    val destinationAccountId =
                        understandingStore.accountIdFor(
                            event.destination
                        )

                    val canApply =
                        understandingStore.isUserOwned(
                            event.source.entityKey
                        ) &&
                            understandingStore.isUserOwned(
                                event.destination.entityKey
                            ) &&
                            sourceAccountId != null &&
                            destinationAccountId != null

                    if (!canApply) {
                        understandingStore.recordTransfer(
                            event = event,
                            messageFingerprint = fingerprint,
                            occurredAtEpochMs =
                                occurredAtEpochMs,
                            sourcePackage = sourcePackage,
                            sourceKind = sourceKind,
                            status =
                                FinancialEventStatus.PENDING_REVIEW
                        )
                        pendingReviewEvents += 1
                        changed = true
                        return@forEach
                    }

                    val legacyBalanceAccount =
                        when {
                            sourceAccountId ==
                                AccountStore.DEFAULT_MPESA_ID ->
                                event.source
                            destinationAccountId ==
                                AccountStore.DEFAULT_MPESA_ID ->
                                event.destination
                            else -> null
                        }
                    val legacyWalletBalance =
                        legacyBalanceAccount?.let {
                            balanceForAccount(
                                interpretation,
                                it
                            )
                        }

                    val ownTransfer =
                        OwnAccountTransferMatch(
                            sourceAccountId =
                                sourceAccountId,
                            sourceName =
                                event.source.displayName,
                            destinationAccountId =
                                destinationAccountId,
                            destinationName =
                                event.destination.displayName,
                            evidence =
                                OwnTransferEvidence.ACCOUNT_NAME
                        )

                    val counterparty =
                        if (
                            destinationAccountId ==
                            AccountStore.DEFAULT_MPESA_ID
                        ) {
                            event.source.displayName
                        } else {
                            event.destination.displayName
                        }

                    val inserted =
                        transactionStore
                            .insertInterpretedMoneyMovementIfNew(
                                kind =
                                    CanonicalFinancialRecordKind
                                        .TRANSFER,
                                transactionCode =
                                    interpretation.transactionCode,
                                amountMinor =
                                    event.amountMinor,
                                counterparty =
                                    counterparty,
                                counterpartyPhone = null,
                                feeMinor =
                                    event.feeMinor,
                                balanceMinor =
                                    legacyWalletBalance,
                                occurredAtText =
                                    interpretation.occurredAtText,
                                rawMessage = body,
                                sourcePackage =
                                    sourcePackage,
                                sourceKind =
                                    SOURCE_INTERPRETED_PREFIX +
                                        sourceKind,
                                receivedAtEpochMs =
                                    receivedAtEpochMs,
                                occurredAtEpochMs =
                                    occurredAtEpochMs,
                                matchedRule = null,
                                rebuildPayCycles =
                                    rebuildPayCycles,
                                accountTransferOverride =
                                    ownTransfer,
                                externalReferences =
                                    correlationReferences
                            )

                    if (inserted) {
                        insertedTransactions += 1
                        changed = true
                    }

                    val linkedTransactionId =
                        interpretation.transactionCode
                            ?.let(
                                transactionStore::
                                    findByTransactionCode
                            )
                            ?.id
                            ?: transactionStore
                                .findByAnyExternalReference(
                                    correlationReferences
                                )
                                ?.id

                    understandingStore.recordTransfer(
                        event = event,
                        messageFingerprint = fingerprint,
                        occurredAtEpochMs =
                            occurredAtEpochMs,
                        sourcePackage = sourcePackage,
                        sourceKind = sourceKind,
                        status =
                            FinancialEventStatus.APPLIED,
                        linkedTransactionId =
                            linkedTransactionId
                    )
                }
            is PaymentEventFact -> {
                    understandingStore.observeEntity(
                        reference = event.source,
                        observedAtEpochMs =
                            occurredAtEpochMs
                    )
                    val sourceAccountId =
                        understandingStore.accountIdFor(
                            event.source
                        )
                    if (sourceAccountId == null) {
                        pendingReviewEvents += 1
                        changed = true
                        return@forEach
                    }

                    val observedBalance =
                        balanceForAccount(
                            interpretation,
                            event.source
                        )
                    val legacyWalletBalance =
                        if (
                            sourceAccountId ==
                            AccountStore.DEFAULT_MPESA_ID
                        ) {
                            observedBalance
                        } else {
                            null
                        }

                    val matchedRule =
                        InterpretedRuleResolver.match(
                            counterparty =
                                event.counterparty,
                            counterpartyPhone =
                                event.counterpartyPhone,
                            amountMinor =
                                event.amountMinor,
                            title = title,
                            body = body,
                            rules = rules
                        )

                    val inserted =
                        transactionStore
                            .insertInterpretedMoneyMovementIfNew(
                                kind =
                                    CanonicalFinancialRecordKind
                                        .PAYMENT,
                                paymentRouteHint =
                                    event.routeHint,
                                transactionCode =
                                    interpretation.transactionCode,
                                amountMinor =
                                    event.amountMinor,
                                counterparty =
                                    event.counterparty,
                                counterpartyPhone =
                                    event.counterpartyPhone,
                                feeMinor =
                                    event.feeMinor,
                                balanceMinor =
                                    legacyWalletBalance,
                                occurredAtText =
                                    interpretation.occurredAtText,
                                rawMessage = body,
                                sourcePackage =
                                    sourcePackage,
                                sourceKind =
                                    SOURCE_INTERPRETED_PREFIX +
                                        sourceKind,
                                receivedAtEpochMs =
                                    receivedAtEpochMs,
                                occurredAtEpochMs =
                                    occurredAtEpochMs,
                                matchedRule =
                                    matchedRule,
                                rebuildPayCycles =
                                    rebuildPayCycles,
                                accountIdOverride =
                                    sourceAccountId,
                                externalReferences =
                                    correlationReferences
                            )

                    if (inserted) {
                        insertedTransactions += 1
                        changed = true
                    }

                    val paymentTransactionId =
                        interpretation.transactionCode
                            ?.let(
                                transactionStore::
                                    findByTransactionCode
                            )
                            ?.id
                            ?: transactionStore
                                .findByAnyExternalReference(
                                    correlationReferences
                                )
                                ?.id

                    if (
                        paymentTransactionId != null &&
                        DebtStore.get(appContext)
                            .tryAutoLinkOutgoingPayment(
                                paymentTransactionId
                            )
                    ) {
                        changed = true
                    }
                }
                is ReceiptEventFact -> {
                    understandingStore.observeEntity(
                        reference = event.destination,
                        observedAtEpochMs =
                            occurredAtEpochMs
                    )

                    val observedBalance =
                        balanceForAccount(
                            interpretation,
                            event.destination
                        )

                    val matchedRule =
                        InterpretedRuleResolver.match(
                            counterparty =
                                event.counterparty,
                            counterpartyPhone =
                                event.counterpartyPhone,
                            amountMinor =
                                event.amountMinor,
                            title = title,
                            body = body,
                            rules = rules
                        )

                    val destinationAccountId =
                        understandingStore.accountIdFor(
                            event.destination
                        ) ?: matchedRule?.accountId

                    val isMpesaDestination =
                        destinationAccountId ==
                            AccountStore.DEFAULT_MPESA_ID ||
                            event.destination.providerKey ==
                                "SAFARICOM_MPESA"
                    val effectiveAccountId =
                        destinationAccountId ?: if (
                            isMpesaDestination
                        ) {
                            AccountStore.DEFAULT_MPESA_ID
                        } else {
                            null
                        }

                    val inserted =
                        transactionStore
                            .insertInterpretedMoneyMovementIfNew(
                                kind =
                                    CanonicalFinancialRecordKind
                                        .RECEIPT,
                                transactionCode =
                                    interpretation.transactionCode,
                                amountMinor =
                                    event.amountMinor,
                                counterparty =
                                    event.counterparty,
                                counterpartyPhone =
                                    event.counterpartyPhone,
                                feeMinor =
                                    event.feeMinor,
                                balanceMinor =
                                    if (isMpesaDestination) {
                                        observedBalance
                                    } else {
                                        null
                                    },
                                occurredAtText =
                                    interpretation.occurredAtText,
                                rawMessage = body,
                                sourcePackage =
                                    sourcePackage,
                                sourceKind =
                                    SOURCE_INTERPRETED_PREFIX +
                                        sourceKind,
                                receivedAtEpochMs =
                                    receivedAtEpochMs,
                                occurredAtEpochMs =
                                    occurredAtEpochMs,
                                matchedRule =
                                    matchedRule,
                                rebuildPayCycles =
                                    rebuildPayCycles,
                                accountIdOverride =
                                    effectiveAccountId,
                                externalReferences =
                                    correlationReferences
                            )

                    if (inserted) {
                        insertedTransactions += 1
                        if (
                            effectiveAccountId == null
                        ) {
                            pendingReviewEvents += 1
                        }
                        changed = true
                    }
                }
                is CashWithdrawalEventFact -> {
                    understandingStore.observeEntity(
                        reference = event.source,
                        observedAtEpochMs =
                            occurredAtEpochMs
                    )
                    understandingStore.observeEntity(
                        reference = event.cashDestination,
                        observedAtEpochMs =
                            occurredAtEpochMs
                    )
                    val sourceAccountId =
                        understandingStore.accountIdFor(
                            event.source
                        )
                    val destinationAccountId =
                        understandingStore.accountIdFor(
                            event.cashDestination
                        )

                    val transfer =
                        TransferEventFact(
                            amountMinor = event.amountMinor,
                            source = event.source,
                            destination =
                                event.cashDestination,
                            feeMinor = event.feeMinor,
                            confidence =
                                event.confidence,
                            evidence = event.evidence
                        )

                    val canApply =
                        sourceAccountId != null &&
                            destinationAccountId != null &&
                            understandingStore.isUserOwned(
                                event.source.entityKey
                            ) &&
                            understandingStore.isUserOwned(
                                event.cashDestination.entityKey
                            )

                    if (!canApply) {
                        understandingStore.recordTransfer(
                            event = transfer,
                            messageFingerprint =
                                fingerprint,
                            occurredAtEpochMs =
                                occurredAtEpochMs,
                            sourcePackage =
                                sourcePackage,
                            sourceKind =
                                sourceKind,
                            status =
                                FinancialEventStatus
                                    .PENDING_REVIEW
                        )
                        pendingReviewEvents += 1
                        changed = true
                        return@forEach
                    }

                    val observedBalance =
                        balanceForAccount(
                            interpretation,
                            event.source
                        )
                    val legacyWalletBalance =
                        if (
                            sourceAccountId ==
                            AccountStore.DEFAULT_MPESA_ID
                        ) {
                            observedBalance
                        } else {
                            null
                        }
                    val ownTransfer =
                        OwnAccountTransferMatch(
                            sourceAccountId =
                                sourceAccountId,
                            sourceName =
                                event.source.displayName,
                            destinationAccountId =
                                destinationAccountId,
                            destinationName =
                                event
                                    .cashDestination
                                    .displayName,
                            evidence =
                                OwnTransferEvidence
                                    .ACCOUNT_NAME
                        )

                    val inserted =
                        transactionStore
                            .insertInterpretedMoneyMovementIfNew(
                                kind =
                                    CanonicalFinancialRecordKind
                                        .CASH_WITHDRAWAL,
                                transactionCode =
                                    interpretation.transactionCode,
                                amountMinor =
                                    event.amountMinor,
                                counterparty =
                                    event.counterparty,
                                counterpartyPhone = null,
                                feeMinor =
                                    event.feeMinor,
                                balanceMinor =
                                    legacyWalletBalance,
                                occurredAtText =
                                    interpretation.occurredAtText,
                                rawMessage = body,
                                sourcePackage =
                                    sourcePackage,
                                sourceKind =
                                    SOURCE_INTERPRETED_PREFIX +
                                        sourceKind,
                                receivedAtEpochMs =
                                    receivedAtEpochMs,
                                occurredAtEpochMs =
                                    occurredAtEpochMs,
                                matchedRule = null,
                                rebuildPayCycles =
                                    rebuildPayCycles,
                                accountTransferOverride =
                                    ownTransfer,
                                externalReferences =
                                    correlationReferences
                            )

                    if (inserted) {
                        insertedTransactions += 1
                        changed = true
                    }

                    val linkedTransactionId =
                        interpretation.transactionCode
                            ?.let(
                                transactionStore::
                                    findByTransactionCode
                            )
                            ?.id
                            ?: transactionStore
                                .findByAnyExternalReference(
                                    correlationReferences
                                )
                                ?.id
                    understandingStore.recordTransfer(
                        event = transfer,
                        messageFingerprint =
                            fingerprint,
                        occurredAtEpochMs =
                            occurredAtEpochMs,
                        sourcePackage = sourcePackage,
                        sourceKind = sourceKind,
                        status =
                            FinancialEventStatus.APPLIED,
                        linkedTransactionId =
                            linkedTransactionId
                    )
                }
                is CashDepositEventFact -> {
                    understandingStore.observeEntity(
                        reference = event.cashSource,
                        observedAtEpochMs =
                            occurredAtEpochMs
                    )
                    understandingStore.observeEntity(
                        reference = event.destination,
                        observedAtEpochMs =
                            occurredAtEpochMs
                    )
                    val sourceAccountId =
                        understandingStore.accountIdFor(
                            event.cashSource
                        )
                    val destinationAccountId =
                        understandingStore.accountIdFor(
                            event.destination
                        )

                    val transfer =
                        TransferEventFact(
                            amountMinor = event.amountMinor,
                            source = event.cashSource,
                            destination =
                                event.destination,
                            feeMinor = event.feeMinor,
                            confidence =
                                event.confidence,
                            evidence = event.evidence
                        )

                    val canApply =
                        sourceAccountId != null &&
                            destinationAccountId != null &&
                            understandingStore.isUserOwned(
                                event.cashSource.entityKey
                            ) &&
                            understandingStore.isUserOwned(
                                event.destination.entityKey
                            )

                    if (!canApply) {
                        understandingStore.recordTransfer(
                            event = transfer,
                            messageFingerprint =
                                fingerprint,
                            occurredAtEpochMs =
                                occurredAtEpochMs,
                            sourcePackage =
                                sourcePackage,
                            sourceKind =
                                sourceKind,
                            status =
                                FinancialEventStatus
                                    .PENDING_REVIEW
                        )
                        pendingReviewEvents += 1
                        changed = true
                        return@forEach
                    }

                    val observedBalance =
                        balanceForAccount(
                            interpretation,
                            event.destination
                        )
                    val legacyWalletBalance =
                        if (
                            destinationAccountId ==
                            AccountStore.DEFAULT_MPESA_ID
                        ) {
                            observedBalance
                        } else {
                            null
                        }
                    val ownTransfer =
                        OwnAccountTransferMatch(
                            sourceAccountId =
                                sourceAccountId,
                            sourceName =
                                event.cashSource.displayName,
                            destinationAccountId =
                                destinationAccountId,
                            destinationName =
                                event.destination.displayName,
                            evidence =
                                OwnTransferEvidence
                                    .ACCOUNT_NAME
                        )

                    val inserted =
                        transactionStore
                            .insertInterpretedMoneyMovementIfNew(
                                kind =
                                    CanonicalFinancialRecordKind
                                        .CASH_DEPOSIT,
                                transactionCode =
                                    interpretation.transactionCode,
                                amountMinor =
                                    event.amountMinor,
                                counterparty =
                                    event.counterparty,
                                counterpartyPhone = null,
                                feeMinor =
                                    event.feeMinor,
                                balanceMinor =
                                    legacyWalletBalance,
                                occurredAtText =
                                    interpretation.occurredAtText,
                                rawMessage = body,
                                sourcePackage =
                                    sourcePackage,
                                sourceKind =
                                    SOURCE_INTERPRETED_PREFIX +
                                        sourceKind,
                                receivedAtEpochMs =
                                    receivedAtEpochMs,
                                occurredAtEpochMs =
                                    occurredAtEpochMs,
                                matchedRule = null,
                                rebuildPayCycles =
                                    rebuildPayCycles,
                                accountTransferOverride =
                                    ownTransfer,
                                externalReferences =
                                    correlationReferences
                            )

                    if (inserted) {
                        insertedTransactions += 1
                        changed = true
                    }

                    val linkedTransactionId =
                        interpretation.transactionCode
                            ?.let(
                                transactionStore::
                                    findByTransactionCode
                            )
                            ?.id
                            ?: transactionStore
                                .findByAnyExternalReference(
                                    correlationReferences
                                )
                                ?.id
                    understandingStore.recordTransfer(
                        event = transfer,
                        messageFingerprint =
                            fingerprint,
                        occurredAtEpochMs =
                            occurredAtEpochMs,
                        sourcePackage = sourcePackage,
                        sourceKind = sourceKind,
                        status =
                            FinancialEventStatus.APPLIED,
                        linkedTransactionId =
                            linkedTransactionId
                    )
                }
                is UnresolvedFinancialEventFact -> {
                    val accountId =
                        event.observedAccount?.let { account ->
                            understandingStore.observeEntity(
                                reference = account,
                                observedAtEpochMs =
                                    occurredAtEpochMs
                            )
                            understandingStore.accountIdFor(
                                account
                            )
                        }
                    val observedBalance =
                        event.observedAccount?.let { account ->
                            balanceForAccount(
                                interpretation,
                                account
                            )
                        }
                    val legacyWalletBalance =
                        if (
                            accountId ==
                            AccountStore.DEFAULT_MPESA_ID
                        ) {
                            observedBalance
                        } else {
                            null
                        }

                    val matchedRule =
                        InterpretedRuleResolver.match(
                            counterparty = null,
                            counterpartyPhone = null,
                            amountMinor =
                                event.amountMinor,
                            title = title,
                            body = body,
                            rules = rules
                        )

                    val inserted =
                        transactionStore
                            .insertInterpretedMoneyMovementIfNew(
                                kind =
                                    CanonicalFinancialRecordKind
                                        .UNRESOLVED_MOVEMENT,
                                transactionCode =
                                    interpretation.transactionCode,
                                amountMinor =
                                    event.amountMinor,
                                counterparty = null,
                                counterpartyPhone = null,
                                feeMinor =
                                    event.feeMinor,
                                balanceMinor =
                                    legacyWalletBalance,
                                occurredAtText =
                                    interpretation.occurredAtText,
                                rawMessage = body,
                                sourcePackage =
                                    sourcePackage,
                                sourceKind =
                                    SOURCE_INTERPRETED_PREFIX +
                                        sourceKind,
                                receivedAtEpochMs =
                                    receivedAtEpochMs,
                                occurredAtEpochMs =
                                    occurredAtEpochMs,
                                matchedRule =
                                    matchedRule,
                                rebuildPayCycles =
                                    rebuildPayCycles,
                                accountIdOverride =
                                    accountId,
                                externalReferences =
                                    correlationReferences
                            )

                    if (inserted) {
                        insertedTransactions += 1
                        if (matchedRule == null) {
                            pendingReviewEvents += 1
                        }
                        changed = true
                    }
                }
                is ReversalEventFact -> {
                    val accountId =
                        event.observedAccount?.let { account ->
                            understandingStore.observeEntity(
                                reference = account,
                                observedAtEpochMs =
                                    occurredAtEpochMs
                            )
                            understandingStore.accountIdFor(
                                account
                            )
                        }
                    val observedBalance =
                        event.observedAccount?.let { account ->
                            balanceForAccount(
                                interpretation,
                                account
                            )
                        }
                    val legacyWalletBalance =
                        if (
                            accountId ==
                            AccountStore.DEFAULT_MPESA_ID
                        ) {
                            observedBalance
                        } else {
                            null
                        }

                    val inserted =
                        transactionStore
                            .insertInterpretedMoneyMovementIfNew(
                                kind =
                                    CanonicalFinancialRecordKind
                                        .REVERSAL,
                                transactionCode =
                                    interpretation.transactionCode,
                                amountMinor =
                                    event.amountMinor,
                                counterparty = null,
                                counterpartyPhone = null,
                                feeMinor = 0L,
                                balanceMinor =
                                    legacyWalletBalance,
                                occurredAtText =
                                    interpretation.occurredAtText,
                                rawMessage = body,
                                sourcePackage =
                                    sourcePackage,
                                sourceKind =
                                    SOURCE_INTERPRETED_PREFIX +
                                        sourceKind,
                                receivedAtEpochMs =
                                    receivedAtEpochMs,
                                occurredAtEpochMs =
                                    occurredAtEpochMs,
                                matchedRule = null,
                                rebuildPayCycles =
                                    rebuildPayCycles,
                                reversalOfCode =
                                    event.referencedTransactionCode,
                                accountIdOverride =
                                    accountId,
                                externalReferences =
                                    correlationReferences
                            )

                    if (inserted) {
                        insertedTransactions += 1
                        changed = true
                    }
                }
                is DebtDrawdownEventFact -> {
                    event.proceedsAccount?.let { account ->
                        understandingStore.observeEntity(
                            reference = account,
                            observedAtEpochMs =
                                occurredAtEpochMs
                        )
                    }

                    val result =
                        DebtStore.get(appContext)
                            .recordDrawdown(
                                event = event,
                                transactionCode =
                                    interpretation
                                        .transactionCode,
                                messageFingerprint =
                                    fingerprint,
                                sourcePackage =
                                    sourcePackage,
                                sourceKind =
                                    SOURCE_INTERPRETED_PREFIX +
                                        sourceKind,
                                occurredAtEpochMs =
                                    occurredAtEpochMs
                            )
                    if (result.inserted) {
                        insertedFinancialEvents += 1
                    }
                    changed =
                        result.inserted ||
                            result.linkedTransactionId != null ||
                            changed
                }
                is DebtRepaymentEventFact -> {
                    event.fundingAccount?.let { account ->
                        understandingStore.observeEntity(
                            reference = account,
                            observedAtEpochMs =
                                occurredAtEpochMs
                        )
                    }
                    val result =
                        DebtStore.get(appContext)
                            .recordRepayment(
                                event = event,
                                transactionCode =
                                    interpretation
                                        .transactionCode,
                                messageFingerprint =
                                    fingerprint,
                                sourcePackage =
                                    sourcePackage,
                                sourceKind =
                                    SOURCE_INTERPRETED_PREFIX +
                                        sourceKind,
                                occurredAtEpochMs =
                                    occurredAtEpochMs
                            )
                    if (result.inserted) {
                        insertedFinancialEvents += 1
                    }
                    changed =
                        result.inserted ||
                            result.linkedTransactionId != null ||
                            changed
                }
            }
        }

        val result =
            FinancialMessageIngestionResult(
                recognized = true,
                consumesLegacyTransactionParsing =
                    interpretation
                        .consumesLegacyTransactionParsing,
                changed = changed,
                insertedTransactions =
                    insertedTransactions,
                insertedFinancialEvents =
                    insertedFinancialEvents,
                pendingReviewEvents =
                    pendingReviewEvents
            )
        recordDiagnostic(
            title = title,
            body = body,
            interpretation = interpretation,
            sourcePackage = sourcePackage,
            sourceKind = sourceKind,
            receivedAtEpochMs =
                receivedAtEpochMs,
            result = result
        )
        return result
    }

    private fun recordDiagnostic(
        title: String?,
        body: String,
        interpretation: FinancialMessageInterpretation,
        sourcePackage: String?,
        sourceKind: String,
        receivedAtEpochMs: Long,
        result: FinancialMessageIngestionResult
    ) {
        val normalizedMessage =
            FinancialMessageNormalizer.normalize(
                title = title,
                body = body
            )
        val relevance =
            FinancialMessageRelevanceGate.assess(
                normalizedMessage
            )
        val bankLike =
            BANK_SOURCE_LANGUAGE_REGEX
                .containsMatchIn(
                    normalizedMessage.body
                )
        val sourceReference =
            FinancialProviderRegistry
                .resolveMessageSourceAccount(
                    title = title,
                    bankLike = bankLike
                )
        val sourceType =
            when {
                interpretation.events.any {
                    it is DebtDrawdownEventFact ||
                        it is DebtRepaymentEventFact
                } ||
                    interpretation.balances.any {
                        it.account.nature ==
                            FinancialAccountNature.LIABILITY
                    } ||
                    interpretation.semanticAssessment
                        ?.intents
                        ?.any {
                            it in setOf(
                                FinancialSemanticIntent
                                    .LOAN_DISBURSEMENT,
                                FinancialSemanticIntent
                                    .LOAN_REPAYMENT,
                                FinancialSemanticIntent
                                    .LOAN_BALANCE
                            )
                        } == true -> "Lender"
                sourceReference?.accountTypeHint ==
                    AccountType.BANK -> "Bank"
                sourceReference?.accountTypeHint ==
                    AccountType.MOBILE_WALLET -> "Wallet"
                sourceReference?.accountTypeHint ==
                    AccountType.SAVINGS -> "Savings"
                else -> "Financial"
            }
        val eventLabel =
            diagnosticEventLabel(
                interpretation = interpretation,
                sourceType = sourceType,
                sourceDisplayName =
                    sourceReference?.displayName
            )
        val correlationReferences =
            interpretation.semanticAssessment
                ?.references
                ?.map { it.value }
                ?.toSet()
                .orEmpty()

        val correlatedTransactionId =
            transactionStore.findByAnyExternalReference(
                correlationReferences
            )?.id
        val linkedByCorrelation =
            correlatedTransactionId != null &&
                result.insertedTransactions == 0 &&
                correlationReferences.isNotEmpty()

        diagnosticsStore.record(
            title = title,
            sourceDisplayName =
                sourceReference?.displayName,
            sourceType = sourceType,
            eventLabel = eventLabel,
            sourcePackage = sourcePackage,
            sourceKind = sourceKind,
            receivedAtEpochMs =
                receivedAtEpochMs,
            financialCandidate =
                relevance.isRelevant,
            semanticUnderstood =
                interpretation.semanticallyUnderstood,
            linkedTransactionId =
                correlatedTransactionId,
            linkedByCorrelation =
                linkedByCorrelation,
            result = result
        )
    }

    private fun diagnosticEventLabel(
        interpretation: FinancialMessageInterpretation,
        sourceType: String,
        sourceDisplayName: String?
    ): String {
        val event = interpretation.events.firstOrNull()

        return when (event) {
            is DebtDrawdownEventFact -> "Loan"
            is DebtRepaymentEventFact ->
                if (
                    event.completion ==
                        DebtCompletion.FULL ||
                    event.outstandingAfterMinor == 0L
                ) {
                    "Loan repaid"
                } else {
                    "Loan repayment"
                }
            is TransferEventFact ->
                when {
                    sourceDisplayName.equals(
                        "M-Pesa",
                        ignoreCase = true
                    ) -> "M-Pesa transfer"
                    sourceType == "Bank" ->
                        "Bank transfer"
                    else -> "Transfer"
                }
            is PaymentEventFact ->
                if (sourceType == "Bank") {
                    "Bank payment"
                } else {
                    "Payment"
                }
            is ReceiptEventFact ->
                if (sourceType == "Bank") {
                    "Bank credit"
                } else {
                    "Money received"
                }
            is CashWithdrawalEventFact ->
                "Cash withdrawal"
            is CashDepositEventFact ->
                "Cash deposit"
            is ReversalEventFact -> "Reversal"
            is UnresolvedFinancialEventFact ->
                "Needs review"
            null ->
                when {
                    interpretation.balances.any {
                        it.account.nature ==
                            FinancialAccountNature.LIABILITY
                    } -> "Loan balance"
                    interpretation.balances.isNotEmpty() ->
                        "Balance update"
                    interpretation.semanticAssessment
                        ?.understood == true ->
                        semanticDiagnosticEventLabel(
                            interpretation
                                .semanticAssessment
                        )
                    else -> "Not understood"
                }
        }
    }

    private fun semanticDiagnosticEventLabel(
        semantic:
            com.kedevjimmy.pesatrack.domain.interpretation
                .FinancialSemanticAssessment
    ): String {
        if (
            semantic.role ==
                FinancialSemanticRole.REMINDER
        ) {
            return when (semantic.primaryIntent) {
                FinancialSemanticIntent.LOAN_BALANCE,
                FinancialSemanticIntent.LOAN_REPAYMENT ->
                    "Loan reminder"
                else -> "Financial reminder"
            }
        }

        return when (semantic.primaryIntent) {
            FinancialSemanticIntent.PAYMENT ->
                "Payment"
            FinancialSemanticIntent.RECEIPT ->
                "Money received"
            FinancialSemanticIntent.TRANSFER ->
                if (
                    semantic.role ==
                        FinancialSemanticRole
                            .CONFIRMATION
                ) {
                    "Transfer confirmation"
                } else {
                    "Transfer"
                }
            FinancialSemanticIntent.LOAN_DISBURSEMENT ->
                "Loan"
            FinancialSemanticIntent.LOAN_REPAYMENT ->
                "Loan repayment"
            FinancialSemanticIntent.LOAN_BALANCE ->
                "Loan balance"
            FinancialSemanticIntent.BALANCE_UPDATE ->
                "Balance update"
            FinancialSemanticIntent.REVERSAL ->
                "Reversal"
            FinancialSemanticIntent.REFUND ->
                "Refund"
            FinancialSemanticIntent.FEE ->
                "Fee"
            FinancialSemanticIntent.INVESTMENT_ORDER ->
                "Investment order"
            FinancialSemanticIntent.INVESTMENT_EXECUTED ->
                "Investment executed"
            FinancialSemanticIntent.CONFIRMATION_ONLY ->
                when {
                    FinancialSemanticIntent.TRANSFER in
                        semantic.intents ->
                        "Transfer confirmation"
                    FinancialSemanticIntent.PAYMENT in
                        semantic.intents ->
                        "Payment confirmation"
                    FinancialSemanticIntent.RECEIPT in
                        semantic.intents ->
                        "Receipt confirmation"
                    else -> "Confirmation"
                }
            FinancialSemanticIntent.UNKNOWN ->
                "Needs review"
        }
    }

    private fun balanceForAccount(
        interpretation: FinancialMessageInterpretation,
        account: FinancialAccountReference
    ): Long? =
        interpretation.balances
            .firstOrNull {
                it.account.entityKey ==
                    account.entityKey
            }
            ?.balanceMinor

    @Synchronized
    fun reconcileExternalEntity(
        entityKey: String
    ): Int {
        val pending =
            understandingStore.pendingTransfersForEntity(
                entityKey
            )
        var applied = 0

        pending.forEach { event ->
            val source =
                understandingStore.entityByKey(
                    event.sourceEntityKey
                ) ?: return@forEach
            val destination =
                understandingStore.entityByKey(
                    event.destinationEntityKey
                ) ?: return@forEach

            val sourceOwned =
                understandingStore.isUserOwned(
                    source.entityKey
                )
            val destinationOwned =
                understandingStore.isUserOwned(
                    destination.entityKey
                )

            val kind: CanonicalFinancialRecordKind
            val ownedAccountId: String
            val counterparty: String
            val routeHint: PaymentRouteHint?

            when {
                !sourceOwned && destinationOwned -> {
                    kind =
                        CanonicalFinancialRecordKind.RECEIPT
                    ownedAccountId =
                        destination.confirmedAccountId
                            ?: return@forEach
                    counterparty =
                        source.displayName
                    routeHint = null
                }

                sourceOwned && !destinationOwned -> {
                    kind =
                        CanonicalFinancialRecordKind.PAYMENT
                    ownedAccountId =
                        source.confirmedAccountId
                            ?: return@forEach
                    counterparty =
                        destination.displayName
                    routeHint =
                        PaymentRouteHint.PERSON_TO_PERSON
                }

                else -> return@forEach
            }

            val inserted =
                transactionStore
                    .insertInterpretedMoneyMovementIfNew(
                        kind = kind,
                        paymentRouteHint =
                            routeHint,
                        transactionCode = null,
                        amountMinor =
                            event.amountMinor,
                        counterparty =
                            counterparty,
                        counterpartyPhone = null,
                        feeMinor =
                            event.feeMinor,
                        balanceMinor = null,
                        occurredAtText = null,
                        rawMessage =
                            "interpreted-external-event|" +
                                event.eventKey,
                        sourcePackage =
                            event.sourcePackage,
                        sourceKind =
                            SOURCE_INTERPRETED_PREFIX +
                                event.sourceKind,
                        receivedAtEpochMs =
                            event.occurredAtEpochMs,
                        occurredAtEpochMs =
                            event.occurredAtEpochMs,
                        matchedRule = null,
                        rebuildPayCycles = false,
                        accountIdOverride =
                            ownedAccountId
                    )

            understandingStore.markEventApplied(
                eventKey = event.eventKey,
                linkedTransactionId = null
            )
            if (inserted) {
                applied += 1
            }
        }

        if (pending.isNotEmpty()) {
            com.kedevjimmy.pesatrack.data.local.PayCycleStore
                .get(appContext)
                .rebuild()
        }

        return applied
    }

    @Synchronized
    fun reconcilePendingTransfers(
        entityKey: String,
        rebuildPayCycles: Boolean = true
    ): Int {
        val pending =
            understandingStore.pendingTransfersForEntity(
                entityKey
            )
        var applied = 0

        pending.forEach { event ->
            val source =
                understandingStore.entityByKey(
                    event.sourceEntityKey
                ) ?: return@forEach
            val destination =
                understandingStore.entityByKey(
                    event.destinationEntityKey
                ) ?: return@forEach

            if (
                !understandingStore.isUserOwned(
                    source.entityKey
                ) ||
                !understandingStore.isUserOwned(
                    destination.entityKey
                )
            ) {
                return@forEach
            }

            val sourceAccountId =
                source.confirmedAccountId
                    ?: return@forEach
            val destinationAccountId =
                destination.confirmedAccountId
                    ?: return@forEach

            val ownTransfer =
                OwnAccountTransferMatch(
                    sourceAccountId =
                        sourceAccountId,
                    sourceName =
                        source.displayName,
                    destinationAccountId =
                        destinationAccountId,
                    destinationName =
                        destination.displayName,
                    evidence =
                        OwnTransferEvidence.ACCOUNT_NAME
                )

            val counterparty =
                if (
                    destinationAccountId ==
                    AccountStore.DEFAULT_MPESA_ID
                ) {
                    source.displayName
                } else {
                    destination.displayName
                }

            val inserted =
                transactionStore
                    .insertInterpretedMoneyMovementIfNew(
                        kind =
                            CanonicalFinancialRecordKind.TRANSFER,
                        transactionCode = null,
                        amountMinor =
                            event.amountMinor,
                        counterparty =
                            counterparty,
                        counterpartyPhone = null,
                        feeMinor =
                            event.feeMinor,
                        balanceMinor = null,
                        occurredAtText = null,
                        rawMessage =
                            "interpreted-event|" +
                                event.eventKey,
                        sourcePackage =
                            event.sourcePackage,
                        sourceKind =
                            SOURCE_INTERPRETED_PREFIX +
                                event.sourceKind,
                        receivedAtEpochMs =
                            event.occurredAtEpochMs,
                        occurredAtEpochMs =
                            event.occurredAtEpochMs,
                        matchedRule = null,
                        rebuildPayCycles = false,
                        accountTransferOverride =
                            ownTransfer
                    )

            understandingStore.markEventApplied(
                eventKey = event.eventKey,
                linkedTransactionId = null
            )
            if (inserted) {
                applied += 1
            }
        }

        if (
            pending.isNotEmpty() &&
            rebuildPayCycles
        ) {
            com.kedevjimmy.pesatrack.data.local.PayCycleStore
                .get(appContext)
                .rebuild()
        }

        return applied
    }

    companion object {
        private val BANK_SOURCE_LANGUAGE_REGEX =
            Regex(
                """\b(?:a/?c|acct|account|debited|credited|available\s+balance|book\s+balance|bank\s+balance)\b""",
                RegexOption.IGNORE_CASE
            )

        const val SOURCE_INTERPRETED_PREFIX =
            "interpreted_"

        @Volatile
        private var instance:
            FinancialMessageIngestionCoordinator? = null

        fun get(
            context: Context
        ): FinancialMessageIngestionCoordinator =
            instance ?: synchronized(this) {
                instance ?:
                    FinancialMessageIngestionCoordinator(
                        context.applicationContext
                    ).also {
                        instance = it
                    }
            }
    }
}
