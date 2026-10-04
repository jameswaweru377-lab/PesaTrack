package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.data.local.CanonicalStoreLock
import android.content.Context
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.DebtStore
import com.kedevjimmy.pesatrack.data.local.FinancialEventStatus
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.InvestmentStore
import com.kedevjimmy.pesatrack.data.local.IdentityGraphStore
import com.kedevjimmy.pesatrack.data.local.PersonalLearningStore
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.ingestion.TransactionDateResolver
import com.kedevjimmy.pesatrack.domain.interpretation.CashDepositEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.CashWithdrawalEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.CanonicalFinancialRecordKind
import com.kedevjimmy.pesatrack.domain.interpretation.DebtDrawdownEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.DebtRepaymentEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAppSourceRegistry
import com.kedevjimmy.pesatrack.domain.interpretation.FeeEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpretation
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpreter
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageNormalizer
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageRelevanceGate
import com.kedevjimmy.pesatrack.domain.interpretation.UnsupportedFinancialCandidatePolicy
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialProviderRegistry
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialSemanticIntent
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialSemanticRole
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountNature
import com.kedevjimmy.pesatrack.domain.interpretation.DebtCompletion
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentRouteHint
import com.kedevjimmy.pesatrack.domain.interpretation.ReceiptEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.ReversalEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.TransferEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.UnresolvedFinancialEventFact
import com.kedevjimmy.pesatrack.domain.rules.InterpretedRuleResolver
import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferMatch
import com.kedevjimmy.pesatrack.domain.rules.OwnTransferEvidence

enum class FinancialDiagnosticMode {
    RECORD,
    RECONCILE_ONLY
}

data class FinancialMessageIngestionResult(
    val recognized: Boolean,
    val consumesLegacyTransactionParsing: Boolean,
    val changed: Boolean,
    val insertedTransactions: Int,
    val insertedFinancialEvents: Int,
    val pendingReviewEvents: Int,
    val handledWithoutLedgerMovement:
        Boolean = false,
    val semanticallyUnderstood:
        Boolean = false
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
    private val messageLabStore =
        MessageLabStore.get(appContext)
    private val identityGraphStore =
        IdentityGraphStore.get(appContext)
    private val personalLearningStore =
        PersonalLearningStore.get(appContext)

    fun ingest(
        title: String?,
        body: String,
        sourcePackage: String?,
        sourceKind: String,
        receivedAtEpochMs: Long,
        rebuildPayCycles: Boolean = true,
        diagnosticMode:
            FinancialDiagnosticMode =
            FinancialDiagnosticMode.RECORD
    ): FinancialMessageIngestionResult {
        synchronized(CanonicalStoreLock.monitor) {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = title,
                body = body,
                sourcePackage =
                    sourcePackage,
                sourceKind =
                    sourceKind
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
        val canonicalDb =
            PesaTrackDatabase.get(appContext)
                .writableDatabase
        val ownsCanonicalTransaction =
            !canonicalDb.inTransaction()
        var canonicalTransactionOpen =
            ownsCanonicalTransaction

        if (ownsCanonicalTransaction) {
            canonicalDb.beginTransaction()
        }

        fun finishCanonicalTransaction(
            successful: Boolean
        ) {
            if (!canonicalTransactionOpen) {
                return
            }
            if (successful) {
                canonicalDb.setTransactionSuccessful()
            }
            canonicalDb.endTransaction()
            canonicalTransactionOpen = false
        }

        try {
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
                    semanticallyUnderstood =
                        interpretation
                            .semanticallyUnderstood,
                    consumesLegacyTransactionParsing =
                        interpretation
                            .consumesLegacyTransactionParsing,
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
                    pendingReviewEvents = 0,
                    handledWithoutLedgerMovement =
                        interpretation
                            .consumesLegacyTransactionParsing &&
                            investmentWrite == null
                )
            finishCanonicalTransaction(
                successful = true
            )
            recordDiagnostic(
                title = title,
                body = body,
                interpretation = interpretation,
                sourcePackage = sourcePackage,
                sourceKind = sourceKind,
                receivedAtEpochMs =
                    receivedAtEpochMs,
                result = result,
                diagnosticMode =
                    diagnosticMode
            )
            updateIdentityGraph(
                result = result,
                rebuildPayCycles =
                    rebuildPayCycles
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

            if (
                fact.account.nature ==
                FinancialAccountNature.LIABILITY
            ) {
                DebtStore.get(appContext)
                    .recordObservedOutstanding(
                        reference = fact.account,
                        outstandingMinor =
                            fact.balanceMinor,
                        observedAtEpochMs =
                            occurredAtEpochMs
                    )
                changed = true
            }
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
                                reviewStateOverride =
                                    ReviewState
                                        .AUTO_CLASSIFIED,
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
                                reviewStateOverride =
                                    effectiveAccountId
                                        ?.let {
                                            ReviewState
                                                .AUTO_CLASSIFIED
                                        },
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
                                directionOverride =
                                    when (
                                        event.directionHint
                                    ) {
                                        com.kedevjimmy.pesatrack.domain.interpretation
                                            .FinancialMovementDirectionHint
                                            .OUTGOING ->
                                            MoneyDirection.EXPENSE
                                        com.kedevjimmy.pesatrack.domain.interpretation
                                            .FinancialMovementDirectionHint
                                            .INCOMING ->
                                            MoneyDirection.INCOME
                                        null -> null
                                    },
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
                is FeeEventFact -> {
                    understandingStore.observeEntity(
                        reference =
                            event.chargedTo,
                        observedAtEpochMs =
                            occurredAtEpochMs
                    )
                    val accountId =
                        understandingStore.accountIdFor(
                            event.chargedTo
                        )
                    if (accountId == null) {
                        pendingReviewEvents += 1
                        changed = true
                        return@forEach
                    }

                    val observedBalance =
                        balanceForAccount(
                            interpretation,
                            event.chargedTo
                        )
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
                                        .FEE,
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
                                accountIdOverride =
                                    accountId,
                                directionOverride =
                                    MoneyDirection.EXPENSE,
                                categoryOverride =
                                    TransactionCategory.FEES,
                                reviewStateOverride =
                                    ReviewState.AUTO_CLASSIFIED,
                                externalReferences =
                                    correlationReferences
                            )

                    if (inserted) {
                        insertedTransactions += 1
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
                semanticallyUnderstood =
                    interpretation
                        .semanticallyUnderstood,
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
        finishCanonicalTransaction(
            successful = true
        )
        recordDiagnostic(
            title = title,
            body = body,
            interpretation = interpretation,
            sourcePackage = sourcePackage,
            sourceKind = sourceKind,
            receivedAtEpochMs =
                receivedAtEpochMs,
            result = result,
            diagnosticMode =
                diagnosticMode
        )
        updateIdentityGraph(
            result = result,
            rebuildPayCycles =
                rebuildPayCycles
        )
        return result
        } catch (error: Throwable) {
            finishCanonicalTransaction(
                successful = false
            )
            throw error
        }
    }
    }

    private fun updateIdentityGraph(
        result: FinancialMessageIngestionResult,
        rebuildPayCycles: Boolean
    ) {
        if (
            result.insertedTransactions <= 0
        ) {
            return
        }

        // Identity learning is downstream metadata.
        // A graph failure must never invalidate an
        // already-correct financial ledger write.
        runCatching {
            identityGraphStore
                .synchronize(
                    transactionLimit = 100
                )
            personalLearningStore
                .applyHighConfidenceTo(
                    transactionStore.recent(
                        limit = 100
                    ),
                    rebuildPayCycles =
                        rebuildPayCycles
                )
        }
    }

    private fun recordDiagnostic(
        title: String?,
        body: String,
        interpretation: FinancialMessageInterpretation,
        sourcePackage: String?,
        sourceKind: String,
        receivedAtEpochMs: Long,
        result: FinancialMessageIngestionResult,
        diagnosticMode:
            FinancialDiagnosticMode
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
        val heuristicUnsupportedCandidate =
            UnsupportedFinancialCandidatePolicy
                .shouldTrack(relevance)
        val bankLike =
            BANK_SOURCE_LANGUAGE_REGEX
                .containsMatchIn(
                    normalizedMessage.body
                )
        val sourceReference =
            FinancialAppSourceRegistry
                .resolve(
                    sourcePackage
                )
                ?: FinancialProviderRegistry
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

        val safeSenderLabel =
            sourceReference
                ?.takeIf {
                    it.providerKey !=
                        null
                }
                ?.displayName
        val messageShapeIdentity =
            MessageLabShapeIdentityFactory
                .create(
                    safeSenderLabel =
                        safeSenderLabel,
                    body = body,
                    sourcePackage =
                        sourcePackage,
                    sourceKind =
                        sourceKind
                )

        val userDecision =
            messageLabStore
                .userDecision(
                    messageShapeIdentity
                        .shapeId
                )
        val unsupportedCandidate =
            MessageLabDecisionPolicy
                .retainUnsupported(
                    heuristicUnsupported =
                        heuristicUnsupportedCandidate,
                    userDecision =
                        userDecision
                )
        val diagnosticCandidate =
            interpretation.recognized ||
                interpretation
                    .semanticallyUnderstood ||
                unsupportedCandidate

        if (
            diagnosticMode ==
            FinancialDiagnosticMode.RECORD
        ) {
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
                diagnosticCandidate,
            semanticUnderstood =
                interpretation.semanticallyUnderstood,
            linkedTransactionId =
                correlatedTransactionId,
            linkedByCorrelation =
                linkedByCorrelation,
            result = result,
            parseProvenance =
                interpretation
                    .parseProvenance,
            messageShapeId =
                messageShapeIdentity
                    .shapeId
            )
        }

        if (
            unsupportedCandidate &&
            !interpretation.recognized &&
            !interpretation
                .semanticallyUnderstood
        ) {
            if (
                diagnosticMode ==
                FinancialDiagnosticMode.RECORD
            ) {
                messageLabStore.recordUnsupported(
                    safeSenderLabel =
                        safeSenderLabel,
                    body = body,
                    observedAtEpochMs =
                        receivedAtEpochMs,
                    sourcePackage =
                        sourcePackage,
                    sourceKind =
                        sourceKind,
                    relevanceScore =
                        relevance.score,
                    relevanceSignals =
                        relevance.signals
                )
            } else {
                messageLabStore
                    .refreshUnsupportedEvidence(
                        safeSenderLabel =
                            safeSenderLabel,
                        body = body,
                        sourcePackage =
                            sourcePackage,
                        sourceKind =
                            sourceKind,
                        relevanceScore =
                            relevance.score,
                        relevanceSignals =
                            relevance.signals
                    )
            }
        } else if (
            MessageLabDecisionPolicy
                .canRetireAsUnderstood(
                    recognized =
                        interpretation
                            .recognized,
                    semanticallyUnderstood =
                        interpretation
                            .semanticallyUnderstood,
                    userDecision =
                        userDecision
                ) ||
            (
                diagnosticMode ==
                    FinancialDiagnosticMode
                        .RECONCILE_ONLY &&
                userDecision == null &&
                !heuristicUnsupportedCandidate
            )
        ) {
            val resolutionContext =
                MessageLabResolutionPolicy
                    .contextFor(
                        interpretation
                    )
            val retired =
                messageLabStore
                    .markUnderstood(
                        safeSenderLabel =
                            safeSenderLabel,
                        body = body,
                        sourcePackage =
                            sourcePackage,
                        sourceKind =
                            sourceKind,
                        resolutionContext =
                            resolutionContext
                    )
            if (
                retired &&
                (
                    interpretation.recognized ||
                        interpretation
                            .semanticallyUnderstood
                )
            ) {
                messageShapeIdentity
                    .shapeId
                    .let {
                        diagnosticsStore
                            .markShapeCovered(
                                it
                            )
                    }
            }
        }
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
            is FeeEventFact ->
                "Fee"
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
            FinancialSemanticIntent.FAILED_ACTION ->
                "Failed financial action"
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

    fun reconcileExternalEntity(
        entityKey: String
    ): Int {
        synchronized(CanonicalStoreLock.monitor) {
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
    }

    fun reconcilePendingTransfers(
        entityKey: String,
        rebuildPayCycles: Boolean = true
    ): Int {
        synchronized(CanonicalStoreLock.monitor) {
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
