package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.FinancialEventStatus
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.ingestion.TransactionDateResolver
import com.kedevjimmy.pesatrack.domain.interpretation.AccountOwnershipHint
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpreter
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
        var pendingReviewEvents = 0

        interpretation.events.forEach { event ->
            when (event) {
                is TransferEventFact -> {
                    val sourceAccountId =
                        understandingStore.observeEntity(
                            reference = event.source,
                            observedAtEpochMs =
                                occurredAtEpochMs
                        )
                    val destinationAccountId =
                        understandingStore.observeEntity(
                            reference = event.destination,
                            observedAtEpochMs =
                                occurredAtEpochMs
                        )

                    val canApply =
                        event.source.ownershipHint ==
                            AccountOwnershipHint.USER_OWNED &&
                            event.destination.ownershipHint ==
                            AccountOwnershipHint.USER_OWNED &&
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
