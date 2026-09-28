package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.DebtStore
import com.kedevjimmy.pesatrack.data.local.FinancialEventStatus
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.ingestion.TransactionDateResolver
import com.kedevjimmy.pesatrack.domain.interpretation.CashMovementEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.DebtDrawdownEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.DebtRepaymentEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpreter
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentRouteHint
import com.kedevjimmy.pesatrack.domain.interpretation.ReceiptEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.ReversalEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.TransferEventFact
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
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

        if (!interpretation.recognized) {
            return FinancialMessageIngestionResult(
                recognized = false,
                consumesLegacyTransactionParsing = false,
                changed = false,
                insertedTransactions = 0,
                insertedFinancialEvents = 0,
                pendingReviewEvents = 0
            )
        }

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

        var changed = false
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
        var insertedFinancialEvents = 0
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

                    val mpesaBalance =
                        interpretation.balances
                            .firstOrNull {
                                it.account.providerKey ==
                                    "SAFARICOM_MPESA"
                            }
                            ?.balanceMinor

                    val parsed = ParsedMpesaTransaction(
                        transactionCode =
                            interpretation.transactionCode,
                        type = transactionTypeFor(
                            sourceAccountId,
                            destinationAccountId
                        ),
                        amountMinor = event.amountMinor,
                        counterparty =
                            if (
                                destinationAccountId ==
                                AccountStore.DEFAULT_MPESA_ID
                            ) {
                                event.source.displayName
                            } else {
                                event.destination.displayName
                            },
                        counterpartyPhone = null,
                        feeMinor = event.feeMinor,
                        balanceMinor = mpesaBalance,
                        occurredAtText =
                            interpretation.occurredAtText,
                        rawMessage = body,
                        reversalOfCode = null
                    )

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

                    val inserted =
                        transactionStore.insertIfNew(
                            parsed = parsed,
                            sourcePackage = sourcePackage,
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
                            evidence = null,
                            accountTransferOverride =
                                ownTransfer
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
                    val mpesaBalance =
                        interpretation.balances
                            .firstOrNull {
                                it.account.providerKey ==
                                    "SAFARICOM_MPESA"
                            }
                            ?.balanceMinor

                    val parsed = ParsedMpesaTransaction(
                        transactionCode =
                            interpretation.transactionCode,
                        type =
                            when (event.routeHint) {
                                PaymentRouteHint.PERSON_TO_PERSON ->
                                    MpesaTransactionType.SENT_MONEY
                                PaymentRouteHint.MERCHANT ->
                                    MpesaTransactionType.BUY_GOODS
                                PaymentRouteHint.BILL ->
                                    MpesaTransactionType.PAY_BILL
                                PaymentRouteHint.AIRTIME ->
                                    MpesaTransactionType.AIRTIME
                            },
                        amountMinor = event.amountMinor,
                        counterparty = event.counterparty,
                        counterpartyPhone =
                            event.counterpartyPhone,
                        feeMinor = event.feeMinor,
                        balanceMinor = mpesaBalance,
                        occurredAtText =
                            interpretation.occurredAtText,
                        rawMessage = body,
                        reversalOfCode = null
                    )

                    val inserted =
                        transactionStore.insertIfNew(
                            parsed = parsed,
                            sourcePackage = sourcePackage,
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
                            evidence = null
                        )

                    if (inserted) {
                        insertedTransactions += 1
                        changed = true
                    }
                }
                is ReceiptEventFact -> {
                    val mpesaBalance =
                        interpretation.balances
                            .firstOrNull {
                                it.account.providerKey ==
                                    "SAFARICOM_MPESA"
                            }
                            ?.balanceMinor

                    val parsed = ParsedMpesaTransaction(
                        transactionCode =
                            interpretation.transactionCode,
                        type =
                            MpesaTransactionType.RECEIVED,
                        amountMinor = event.amountMinor,
                        counterparty = event.counterparty,
                        counterpartyPhone =
                            event.counterpartyPhone,
                        feeMinor = event.feeMinor,
                        balanceMinor = mpesaBalance,
                        occurredAtText =
                            interpretation.occurredAtText,
                        rawMessage = body,
                        reversalOfCode = null
                    )

                    val inserted =
                        transactionStore.insertIfNew(
                            parsed = parsed,
                            sourcePackage = sourcePackage,
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
                            evidence = null
                        )

                    if (inserted) {
                        insertedTransactions += 1
                        changed = true
                    }
                }
                is CashMovementEventFact -> {
                    val observedBalance =
                        interpretation.balances
                            .firstOrNull {
                                val account =
                                    event.source ?:
                                        event.destination
                                account == null ||
                                    it.account.entityKey ==
                                        account.entityKey
                            }
                            ?.balanceMinor

                    val parsed = ParsedMpesaTransaction(
                        transactionCode =
                            interpretation.transactionCode,
                        type =
                            when (event.kind) {
                                com.kedevjimmy.pesatrack.domain.interpretation
                                    .FinancialEventKind.CASH_WITHDRAWAL ->
                                    MpesaTransactionType.WITHDRAWAL
                                com.kedevjimmy.pesatrack.domain.interpretation
                                    .FinancialEventKind.CASH_DEPOSIT ->
                                    MpesaTransactionType.DEPOSIT
                                else ->
                                    MpesaTransactionType.OTHER
                            },
                        amountMinor = event.amountMinor,
                        counterparty = event.counterparty,
                        counterpartyPhone = null,
                        feeMinor = event.feeMinor,
                        balanceMinor = observedBalance,
                        occurredAtText =
                            interpretation.occurredAtText,
                        rawMessage = body,
                        reversalOfCode = null
                    )

                    val inserted =
                        transactionStore.insertIfNew(
                            parsed = parsed,
                            sourcePackage = sourcePackage,
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
                            evidence = null
                        )

                    if (inserted) {
                        insertedTransactions += 1
                        changed = true
                    }
                }
                is ReversalEventFact -> {
                    val observedBalance =
                        interpretation.balances
                            .firstOrNull {
                                event.observedAccount == null ||
                                    it.account.entityKey ==
                                        event
                                            .observedAccount
                                            .entityKey
                            }
                            ?.balanceMinor

                    val parsed = ParsedMpesaTransaction(
                        transactionCode =
                            interpretation.transactionCode,
                        type =
                            MpesaTransactionType.REVERSAL,
                        amountMinor = event.amountMinor,
                        counterparty = null,
                        counterpartyPhone = null,
                        feeMinor = 0L,
                        balanceMinor = observedBalance,
                        occurredAtText =
                            interpretation.occurredAtText,
                        rawMessage = body,
                        reversalOfCode =
                            event.referencedTransactionCode
                    )

                    val inserted =
                        transactionStore.insertIfNew(
                            parsed = parsed,
                            sourcePackage = sourcePackage,
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
                            evidence = null
                        )

                    if (inserted) {
                        insertedTransactions += 1
                        changed = true
                    }
                }
                is DebtDrawdownEventFact -> {
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

        return FinancialMessageIngestionResult(
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
    }

    private fun transactionTypeFor(
        sourceAccountId: String,
        destinationAccountId: String
    ): MpesaTransactionType =
        when {
            sourceAccountId ==
                AccountStore.DEFAULT_MPESA_ID ->
                MpesaTransactionType.SENT_MONEY
            destinationAccountId ==
                AccountStore.DEFAULT_MPESA_ID ->
                MpesaTransactionType.RECEIVED
            else -> MpesaTransactionType.OTHER
        }

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

            val type = when {
                !sourceOwned && destinationOwned ->
                    MpesaTransactionType.RECEIVED
                sourceOwned && !destinationOwned ->
                    MpesaTransactionType.SENT_MONEY
                else -> return@forEach
            }

            val parsed = ParsedMpesaTransaction(
                transactionCode = null,
                type = type,
                amountMinor = event.amountMinor,
                counterparty =
                    if (type == MpesaTransactionType.RECEIVED) {
                        source.displayName
                    } else {
                        destination.displayName
                    },
                counterpartyPhone = null,
                feeMinor = event.feeMinor,
                balanceMinor = null,
                occurredAtText = null,
                rawMessage =
                    "interpreted-external-event|" +
                        event.eventKey,
                reversalOfCode = null
            )

            val inserted =
                transactionStore.insertIfNew(
                    parsed = parsed,
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
                    evidence = null
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

            val parsed = ParsedMpesaTransaction(
                transactionCode = null,
                type = transactionTypeFor(
                    sourceAccountId,
                    destinationAccountId
                ),
                amountMinor = event.amountMinor,
                counterparty =
                    if (
                        destinationAccountId ==
                        AccountStore.DEFAULT_MPESA_ID
                    ) {
                        source.displayName
                    } else {
                        destination.displayName
                    },
                counterpartyPhone = null,
                feeMinor = event.feeMinor,
                balanceMinor = null,
                occurredAtText = null,
                rawMessage =
                    "interpreted-event|" +
                        event.eventKey,
                reversalOfCode = null
            )

            val inserted =
                transactionStore.insertIfNew(
                    parsed = parsed,
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
                    evidence = null,
                    accountTransferOverride =
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

    companion object {
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
