package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.health.ReviewBurdenAnalysis
import com.kedevjimmy.pesatrack.domain.health.ReviewBurdenObservation
import com.kedevjimmy.pesatrack.domain.health.ReviewBurdenSnapshot
import com.kedevjimmy.pesatrack.domain.ingestion.LegacyTransactionTypeCompatibility
import com.kedevjimmy.pesatrack.domain.intelligence.EntityDescriptorPrivacyPolicy
import com.kedevjimmy.pesatrack.domain.intelligence.RemoteEntityAutoReviewPolicy
import com.kedevjimmy.pesatrack.domain.intelligence.RemoteEntityEligibilityPolicy
import com.kedevjimmy.pesatrack.domain.intelligence.RemoteEntityEnrichment
import com.kedevjimmy.pesatrack.domain.ingestion.TransactionFingerprint
import com.kedevjimmy.pesatrack.domain.interpretation.CanonicalFinancialRecordKind
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentRouteHint
import com.kedevjimmy.pesatrack.domain.interpretation.SemanticInvestmentAction
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.rules.KenyanTransactionRecognizer
import com.kedevjimmy.pesatrack.domain.rules.MirrorTransactionMatcher
import com.kedevjimmy.pesatrack.domain.rules.MirrorTransactionObservation
import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferDetector
import com.kedevjimmy.pesatrack.domain.rules.OwnTransferEvidence
import com.kedevjimmy.pesatrack.domain.rules.PlanningPeriodResolver
import com.kedevjimmy.pesatrack.domain.rules.PossibleDuplicateObservation
import com.kedevjimmy.pesatrack.domain.rules.PossibleDuplicatePolicy
import com.kedevjimmy.pesatrack.domain.rules.ReviewTriagePolicy
import com.kedevjimmy.pesatrack.domain.rules.RefundCandidate
import com.kedevjimmy.pesatrack.domain.rules.RefundMatchingPolicy
import com.kedevjimmy.pesatrack.domain.rules.SmartRuleEngine
import com.kedevjimmy.pesatrack.domain.rules.EvidenceConfidence
import com.kedevjimmy.pesatrack.domain.rules.FinancialMessageEvidence
import com.kedevjimmy.pesatrack.domain.rules.TransactionEvidenceMatch
import com.kedevjimmy.pesatrack.domain.rules.TransactionEvidenceResolver
import java.util.Calendar
import java.util.TimeZone

data class SalaryHistoryCandidate(
    val transactionId: Long,
    val amountMinor: Long,
    val occurredAtEpochMs: Long,
    val counterparty: String?,
    val accountId: String?
)

class TransactionStore private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val database = PesaTrackDatabase.get(appContext)

    /**
     * Provider-neutral entry point for financial movements that were
     * already understood by FinancialMessageInterpreter. The old
     * ParsedMpesaTransaction shape is now confined to this storage
     * compatibility boundary while the legacy transactions table is
     * migrated incrementally.
     */
    fun insertInterpretedMoneyMovementIfNew(
        kind: CanonicalFinancialRecordKind,
        paymentRouteHint: PaymentRouteHint? = null,
        transactionCode: String?,
        amountMinor: Long,
        counterparty: String?,
        counterpartyPhone: String?,
        feeMinor: Long,
        balanceMinor: Long?,
        occurredAtText: String?,
        rawMessage: String,
        sourcePackage: String?,
        sourceKind: String,
        receivedAtEpochMs: Long,
        occurredAtEpochMs: Long = receivedAtEpochMs,
        matchedRule: SmartRule? = null,
        rebuildPayCycles: Boolean = true,
        reversalOfCode: String? = null,
        accountIdOverride: String? = null,
        accountTransferOverride:
            com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferMatch? = null,
        directionOverride: MoneyDirection? = null,
        categoryOverride:
            TransactionCategory? = null,
        reviewStateOverride:
            ReviewState? = null,
        externalReferences: Set<String> = emptySet()
    ): Boolean {
        synchronized(CanonicalStoreLock.monitor) {
        val legacyType =
            LegacyTransactionTypeCompatibility.resolve(
                kind = kind,
                paymentRouteHint = paymentRouteHint,
                sourceIsMpesa =
                    accountTransferOverride?.sourceAccountId ==
                        AccountStore.DEFAULT_MPESA_ID,
                destinationIsMpesa =
                    accountIdOverride ==
                        AccountStore.DEFAULT_MPESA_ID ||
                        accountTransferOverride
                            ?.destinationAccountId ==
                        AccountStore.DEFAULT_MPESA_ID
            )

        val legacyParsed =
            ParsedMpesaTransaction(
                transactionCode = transactionCode,
                type = legacyType,
                amountMinor = amountMinor,
                counterparty = counterparty,
                counterpartyPhone =
                    counterpartyPhone,
                feeMinor = feeMinor,
                balanceMinor = balanceMinor,
                occurredAtText = occurredAtText,
                rawMessage = rawMessage,
                reversalOfCode = reversalOfCode
            )

        return insertIfNew(
            parsed = legacyParsed,
            sourcePackage = sourcePackage,
            sourceKind = sourceKind,
            receivedAtEpochMs =
                receivedAtEpochMs,
            occurredAtEpochMs =
                occurredAtEpochMs,
            matchedRule = matchedRule,
            rebuildPayCycles =
                rebuildPayCycles,
            evidence = null,
            accountTransferOverride =
                accountTransferOverride,
            accountIdOverride =
                accountIdOverride,
            allowLegacyAccountFallback = false,
            directionOverride =
                directionOverride,
            categoryOverride =
                categoryOverride,
            reviewStateOverride =
                reviewStateOverride,
            externalReferences =
                externalReferences
        )
    }
    }

    fun insertIfNew(
        parsed: ParsedMpesaTransaction,
        sourcePackage: String?,
        sourceKind: String,
        receivedAtEpochMs: Long,
        occurredAtEpochMs: Long = receivedAtEpochMs,
        matchedRule: SmartRule? = null,
        rebuildPayCycles: Boolean = true,
        runRelationshipReconciliation: Boolean = true,
        runMirrorCorrelation: Boolean = true,
        evidence: TransactionEvidenceMatch? = null,
        accountTransferOverride:
            com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferMatch? = null,
        accountIdOverride: String? = null,
        allowLegacyAccountFallback: Boolean = true,
        directionOverride: MoneyDirection? = null,
        categoryOverride:
            TransactionCategory? = null,
        reviewStateOverride:
            ReviewState? = null,
        externalReferences: Set<String> = emptySet()
    ): Boolean {
        synchronized(CanonicalStoreLock.monitor) {
        val fingerprint = TransactionFingerprint.of(parsed, occurredAtEpochMs)
        if (parsed.transactionCode.isNullOrBlank()) {
            val legacyFingerprint = TransactionFingerprint.of(parsed)
            if (
                legacyFingerprint != fingerprint &&
                hasLegacyFingerprintAt(legacyFingerprint, occurredAtEpochMs)
            ) {
                return false
            }
        } else {
            val existing = getByTransactionCode(parsed.transactionCode)
            if (existing != null && (evidence != null || existing.sourceKind == "manual_setup")) {
                if (evidence != null) updateExistingDuplicateWithEvidence(
                    stored = existing,
                    parsed = parsed,
                    matchedRule = matchedRule,
                    evidence = evidence
                )
                return false
            }
        }
        val accounts = AccountStore.get(appContext).listAccounts()
        val detectedOwnTransfer =
            OwnAccountTransferDetector.matchMpesa(
                parsed = parsed,
                accounts = accounts,
                observedWalletId =
                    AccountStore.DEFAULT_MPESA_ID
            )
        val ownTransfer =
            accountTransferOverride ?: detectedOwnTransfer
        val useOwnTransfer =
            accountTransferOverride != null ||
                (
                    ownTransfer != null &&
                    (
                        matchedRule == null ||
                            ownTransfer.evidence ==
                                OwnTransferEvidence.EXACT_IDENTIFIER
                        )
                    )

        val classification = if (useOwnTransfer) {
            Classification(
                direction = MoneyDirection.TRANSFER,
                category = TransactionCategory.TRANSFER,
                reviewState = ReviewState.AUTO_CLASSIFIED,
                ruleId = null,
                planningMonth = PlanningPeriodResolver.resolve(occurredAtEpochMs, 0)
            )
        } else {
            classify(
                parsed,
                matchedRule,
                occurredAtEpochMs
            ).let {
                base ->
                if (
                    matchedRule == null &&
                    (
                        directionOverride != null ||
                        categoryOverride != null ||
                        reviewStateOverride != null
                    )
                ) {
                    base.copy(
                        direction =
                            directionOverride
                                ?: base.direction,
                        category =
                            categoryOverride
                                ?: base.category,
                        reviewState =
                            reviewStateOverride
                                ?: base.reviewState
                    )
                } else {
                    base
                }
            }
        }
        val displayCounterparty =
            classification.displayLabel
                ?: parsed.counterparty
                ?: ownTransfer?.destinationName?.takeIf { useOwnTransfer }
            ?: matchedRule?.displayLabel?.takeIf { it.isNotBlank() }
            ?: matchedRule?.name?.takeIf { it.isNotBlank() }

        val defaultMpesa =
            allowLegacyAccountFallback &&
                (
                    sourceKind.contains(
                        "mpesa",
                        ignoreCase = true
                    ) ||
                        parsed.type !=
                        MpesaTransactionType.OTHER
                    )
        val accountId = if (useOwnTransfer) {
            ownTransfer!!.sourceAccountId
        } else {
            accountIdOverride
                ?: matchedRule?.accountId
                ?: when {
                    parsed.type ==
                        MpesaTransactionType.WITHDRAWAL ->
                        AccountStore.DEFAULT_MPESA_ID
                    defaultMpesa ->
                        AccountStore.DEFAULT_MPESA_ID
                    else -> null
                }
        }
        val linkedAccountId = if (useOwnTransfer) {
            ownTransfer!!.destinationAccountId
        } else {
            matchedRule?.linkedAccountId ?: when (parsed.type) {
                MpesaTransactionType.WITHDRAWAL -> AccountStore.DEFAULT_CASH_ID
                else -> null
            }
        }

        if (sourceKind != "manual_setup" && !parsed.transactionCode.isNullOrBlank() &&
            matchedRule?.startsPayCycle == true && !useOwnTransfer &&
            classification.direction == MoneyDirection.INCOME && accountId != null &&
            getByTransactionCode(parsed.transactionCode) == null) {
            val day = Calendar.getInstance().apply {
                timeInMillis = occurredAtEpochMs
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val nextDay = (day.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
            val matches = transactionsBetween(day.timeInMillis, nextDay.timeInMillis).filter {
                it.ruleId == matchedRule.id && it.accountId == accountId &&
                    it.amountMinor == parsed.amountMinor && it.direction == MoneyDirection.INCOME.name &&
                    it.reviewState != ReviewState.EXCLUDED.name
            }
            val manual = matches.singleOrNull()?.takeIf {
                it.sourceKind == "manual_setup" && it.transactionCode == null
            }
            if (manual != null) {
                val values = ContentValues().apply {
                    put("transaction_code", parsed.transactionCode)
                    // Keep the manual fingerprint and date for repeated form submissions.
                    // The ingestion coordinator records the message's balance at its real observation time.
                }
                database.writableDatabase.update("transactions", values, "id = ?", arrayOf(manual.id.toString()))
                attachExternalReferences(manual.id, externalReferences + parsed.transactionCode.orEmpty(), sourceKind, receivedAtEpochMs)
                LedgerUpdateBus.notifyChanged()
                return false
            }
        }

        val existingFingerprint =
            getByFingerprint(fingerprint)
        if (existingFingerprint != null) {
            val preservesUserDecision =
                existingFingerprint.reviewState in
                    setOf(
                        ReviewState.USER_CLASSIFIED.name,
                        ReviewState.EXCLUDED.name
                    )
            val protectedMirror =
                existingFingerprint.evidenceSource ==
                    "owned_mirror_transfer"
            val safeOwnTransferRefresh =
                accountTransferOverride != null &&
                    !preservesUserDecision &&
                    !protectedMirror

            val values =
                ContentValues().apply {
                    if (safeOwnTransferRefresh) {
                        // Account ownership can be confirmed after the original
                        // SMS was imported. Replaying that same message must
                        // promote the old income/expense row into the newly
                        // proven own-account transfer rather than leaving stale
                        // Activity data behind.
                        put(
                            "account_id",
                            ownTransfer!!
                                .sourceAccountId
                        )
                        put(
                            "linked_account_id",
                            ownTransfer
                                .destinationAccountId
                        )
                        put(
                            "direction",
                            MoneyDirection
                                .TRANSFER.name
                        )
                        put(
                            "category",
                            TransactionCategory
                                .TRANSFER.name
                        )
                        put(
                            "review_state",
                            ReviewState
                                .AUTO_CLASSIFIED.name
                        )
                        putNull("rule_id")
                        put(
                            "planning_month",
                            classification
                                .planningMonth
                        )
                    } else {
                        if (
                            existingFingerprint.accountId ==
                                null &&
                            accountId != null
                        ) {
                            put(
                                "account_id",
                                accountId
                            )
                        }
                        if (
                            existingFingerprint.linkedAccountId ==
                                null &&
                            linkedAccountId != null
                        ) {
                            put(
                                "linked_account_id",
                                linkedAccountId
                            )
                        }
                    }

                    val safeSemanticOverride =
                        matchedRule == null &&
                            (
                                directionOverride != null ||
                                categoryOverride != null ||
                                reviewStateOverride != null
                            ) &&
                            existingFingerprint.ruleId ==
                                null &&
                            !preservesUserDecision

                    if (
                        !safeOwnTransferRefresh &&
                        (
                            matchedRule != null ||
                                safeSemanticOverride
                            ) &&
                        !preservesUserDecision &&
                        !protectedMirror
                    ) {
                        if (
                            safeSemanticOverride &&
                            accountId != null
                        ) {
                            put(
                                "account_id",
                                accountId
                            )
                            if (
                                linkedAccountId ==
                                null
                            ) {
                                putNull(
                                    "linked_account_id"
                                )
                            } else {
                                put(
                                    "linked_account_id",
                                    linkedAccountId
                                )
                            }
                        }
                        put(
                            "direction",
                            classification.direction.name
                        )
                        put(
                            "category",
                            classification.category.name
                        )
                        put(
                            "review_state",
                            classification.reviewState.name
                        )
                        if (matchedRule != null) {
                            put(
                                "rule_id",
                                matchedRule.id
                            )
                        }
                        put(
                            "planning_month",
                            classification.planningMonth
                        )
                    }
                }
            if (values.size() > 0) {
                database.writableDatabase
                    .update(
                        "transactions",
                        values,
                        "id = ?",
                        arrayOf(
                            existingFingerprint.id
                                .toString()
                        )
                    )
                LedgerUpdateBus
                    .notifyChanged()
            }
            return false
        }

        val correlationReferences =
            normalizedCorrelationReferences(
                transactionCode =
                    parsed.transactionCode,
                externalReferences =
                    externalReferences
            )
        val correlationAccountIds =
            listOfNotNull(
                accountId,
                linkedAccountId
            ).toSet()

        val mirrorCandidate =
            if (runMirrorCorrelation) {
                findMirrorCandidate(
                    parsed = parsed,
                    sourcePackage = sourcePackage,
                    sourceKind = sourceKind,
                    receivedAtEpochMs =
                        receivedAtEpochMs,
                    occurredAtEpochMs =
                        occurredAtEpochMs,
                    direction =
                        classification.direction.name,
                    externalReferences =
                        correlationReferences,
                    accountIds =
                        correlationAccountIds
                )
            } else {
                null
            }
        if (mirrorCandidate != null) {
            val mirrorTransfer = OwnedMirrorTransferReconciler.resolve(
                existing = mirrorCandidate,
                direction = classification.direction.name,
                accountId = accountId,
                linkedAccountId = linkedAccountId,
                amountMinor = parsed.amountMinor,
                references = correlationReferences,
                existingReferences = externalReferencesForTransaction(mirrorCandidate.id),
                accounts = accounts,
                incomingHasRule = classification.ruleId != null ||
                    classification.category == TransactionCategory.DEBT_LOAN ||
                    parsed.type == MpesaTransactionType.REVERSAL
            )
            fun applyMirrorTransfer() {
                mirrorTransfer?.let {
                    OwnedMirrorTransferReconciler.apply(database.writableDatabase, mirrorCandidate.id, it,
                        maxOf(mirrorCandidate.feeMinor, parsed.feeMinor), accounts)
                }
            }
            attachExternalReferences(
                transactionId = mirrorCandidate.id,
                references = correlationReferences,
                sourceKind = sourceKind,
                observedAtEpochMs = receivedAtEpochMs
            )
            if (correlationReferences.isNotEmpty()) {
                InvestmentStore.get(appContext)
                    .reconcileTransaction(
                        transactionId =
                            mirrorCandidate.id,
                        references =
                            correlationReferences
                    )
            }

            if (parsed.transactionCode.isNullOrBlank()) {
                applyMirrorTransfer()
                if (mirrorTransfer != null && rebuildPayCycles) PayCycleStore.get(appContext).rebuild()
                return false
            }

            mergeMpesaMirrorIntoExisting(
                existing = mirrorCandidate,
                parsed = parsed,
                fingerprint = fingerprint,
                sourcePackage = sourcePackage,
                sourceKind = sourceKind,
                receivedAtEpochMs = receivedAtEpochMs,
                occurredAtEpochMs = occurredAtEpochMs,
                classification = classification,
                displayCounterparty = displayCounterparty,
                accountId = accountId,
                linkedAccountId = linkedAccountId,
                evidence = evidence
            )
            applyMirrorTransfer()
            if (runRelationshipReconciliation) {
                DebtStore.get(appContext)
                    .reconcilePendingDrawdownsForTransaction(
                        transactionId =
                            mirrorCandidate.id,
                        parsed = parsed,
                        occurredAtEpochMs =
                            occurredAtEpochMs
                    )
                if (
                    classification.direction ==
                        MoneyDirection.INCOME ||
                    classification.direction ==
                        MoneyDirection.TRANSFER
                ) {
                    DebtStore.get(appContext)
                        .reconcilePendingRepaymentsAround(
                            transactionId =
                                mirrorCandidate.id,
                            occurredAtEpochMs =
                                occurredAtEpochMs
                        )
                    if (matchedRule == null) {
                        tryLinkRefund(
                            mirrorCandidate.id,
                            parsed.rawMessage
                        )
                    }
                }
            }
            if (rebuildPayCycles) {
                PayCycleStore.get(appContext).rebuild()
            }
            return false
        }

        val values = ContentValues().apply {
            put("transaction_code", parsed.transactionCode)
            put("fingerprint", fingerprint)
            put("type", parsed.type.name)
            put("amount_minor", parsed.amountMinor)
            put("fee_minor", parsed.feeMinor)
            if (parsed.balanceMinor != null) put("balance_minor", parsed.balanceMinor) else putNull("balance_minor")
            put("counterparty", displayCounterparty)
            if (parsed.counterpartyPhone != null) put("counterparty_phone", parsed.counterpartyPhone) else putNull("counterparty_phone")
            put("occurred_at_text", parsed.occurredAtText)
            put("occurred_at_epoch_ms", occurredAtEpochMs)
            put("received_at_epoch_ms", receivedAtEpochMs)
            put("source_package", sourcePackage)
            put("source_kind", sourceKind)
            put("created_at_epoch_ms", System.currentTimeMillis())
            put("direction", classification.direction.name)
            put("category", classification.category.name)
            put("review_state", classification.reviewState.name)
            if (classification.ruleId != null) put("rule_id", classification.ruleId) else putNull("rule_id")
            put("planning_month", classification.planningMonth)
            if (parsed.reversalOfCode != null) put("reversal_of_code", parsed.reversalOfCode) else putNull("reversal_of_code")
            if (accountId != null) put("account_id", accountId) else putNull("account_id")
            if (linkedAccountId != null) put("linked_account_id", linkedAccountId) else putNull("linked_account_id")
            putNull("pay_cycle_id")
            putNull("cycle_candidate_reason")
            putNull("cycle_override")
            putNull("relation_type")
            putNull("related_transaction_id")
            if (evidence != null) {
                put("evidence_label", evidence.label)
                put("evidence_source", evidence.sourceLabel)
                put("evidence_confidence", evidence.confidence.name)
                put("evidence_time_delta_ms", evidence.timeDeltaMs)
            } else if (
                classification.evidenceSource != null
            ) {
                put(
                    "evidence_label",
                    classification.evidenceLabel
                )
                put(
                    "evidence_source",
                    classification.evidenceSource
                )
                put(
                    "evidence_confidence",
                    classification.evidenceConfidence
                        ?.name
                )
                putNull("evidence_time_delta_ms")
            } else {
                putNull("evidence_label")
                putNull("evidence_source")
                putNull("evidence_confidence")
                putNull("evidence_time_delta_ms")
            }
        }

        val db = database.writableDatabase
        val ownsTransaction = !db.inTransaction()
        if (ownsTransaction) {
            db.beginTransaction()
        }

        val rowId =
            try {
                val insertedRowId =
                    db.insertWithOnConflict(
                        "transactions",
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_IGNORE
                    )

                if (insertedRowId != -1L) {
                    attachExternalReferences(
                        transactionId = insertedRowId,
                        references =
                            correlationReferences,
                        sourceKind = sourceKind,
                        observedAtEpochMs =
                            receivedAtEpochMs,
                        db = db
                    )
                }

                if (ownsTransaction) {
                    db.setTransactionSuccessful()
                }
                insertedRowId
            } finally {
                if (ownsTransaction) {
                    db.endTransaction()
                }
            }

        val inserted = rowId != -1L
        if (inserted) {
            if (correlationReferences.isNotEmpty()) {
                InvestmentStore.get(appContext)
                    .reconcileTransaction(
                        transactionId = rowId,
                        references =
                            correlationReferences
                    )
            }
            if (runRelationshipReconciliation) {
                DebtStore.get(appContext)
                    .reconcilePendingDrawdownsForTransaction(
                        transactionId = rowId,
                        parsed = parsed,
                        occurredAtEpochMs =
                            occurredAtEpochMs
                    )
                if (
                    classification.direction ==
                        MoneyDirection.INCOME ||
                    classification.direction ==
                        MoneyDirection.TRANSFER
                ) {
                    DebtStore.get(appContext)
                        .reconcilePendingRepaymentsAround(
                            transactionId =
                                rowId,
                            occurredAtEpochMs =
                                occurredAtEpochMs
                        )
                    if (matchedRule == null) {
                        tryLinkRefund(rowId, parsed.rawMessage)
                    }
                }
            }
            if (matchedRule == null) {
                reconcileReviewTriageForMonth(occurredAtEpochMs)
            }

            if (
                matchedRule == null &&
                RemoteEntityEligibilityPolicy
                    .canSendOnline(
                        parsed.type.name
                    ) &&
                ReviewTriagePolicy
                    .shouldTriageCategory(
                        classification.category.name
                    )
            ) {
                EntityEnrichmentCoordinator
                    .get(appContext)
                    .enrichTransactionAsync(
                        transactionId = rowId,
                        rawDescriptor =
                            parsed.counterparty
                    )
            }
        }
        if (inserted && rebuildPayCycles) {
            PayCycleStore.get(appContext).rebuild()
        }
        return inserted
    }
    }

    fun latestSalaryReceipt(ruleId: String): StoredTransaction? = synchronized(CanonicalStoreLock.monitor) {
        database.readableDatabase.rawQuery(
            BASE_SELECT + " WHERE rule_id = ? AND direction = ? AND review_state != ?" +
                " AND cycle_candidate_reason IS NULL ORDER BY occurred_at_epoch_ms DESC, id DESC LIMIT 1",
            arrayOf(ruleId, MoneyDirection.INCOME.name, ReviewState.EXCLUDED.name)
        ).use { cursor -> if (cursor.moveToFirst()) cursor.toStoredTransaction() else null }
    }

    fun salaryHistoryCandidates(
        payerQuery: String,
        sinceEpochMs: Long,
        accountId: String? = null,
        limit: Int = 24
    ): List<SalaryHistoryCandidate> {
        synchronized(CanonicalStoreLock.monitor) {
        val query =
            payerQuery.trim()
        if (query.isBlank()) {
            return emptyList()
        }

        val accountClause =
            if (accountId == null) {
                ""
            } else {
                " AND account_id = ?"
            }
        val args =
            buildList {
                add(
                    MoneyDirection
                        .INCOME.name
                )
                add(
                    ReviewState
                        .EXCLUDED.name
                )
                add(
                    sinceEpochMs
                        .toString()
                )
                add("%$query%")
                if (
                    accountId != null
                ) {
                    add(accountId)
                }
                add(
                    limit.coerceIn(
                        1,
                        100
                    ).toString()
                )
            }.toTypedArray()

        return database.readableDatabase
            .rawQuery(
                """
                SELECT id,
                       amount_minor,
                       occurred_at_epoch_ms,
                       counterparty,
                       account_id
                FROM transactions
                WHERE direction = ?
                  AND review_state != ?
                  AND occurred_at_epoch_ms >= ?
                  AND COALESCE(
                        counterparty,
                        ''
                      ) LIKE ?
                        COLLATE NOCASE
                  $accountClause
                ORDER BY occurred_at_epoch_ms DESC,
                         id DESC
                LIMIT ?
                """.trimIndent(),
                args
            ).use { cursor ->
                buildList {
                    while (
                        cursor.moveToNext()
                    ) {
                        add(
                            SalaryHistoryCandidate(
                                transactionId =
                                    cursor.getLong(0),
                                amountMinor =
                                    cursor.getLong(1),
                                occurredAtEpochMs =
                                    cursor.getLong(2),
                                counterparty =
                                    if (
                                        cursor.isNull(3)
                                    ) {
                                        null
                                    } else {
                                        cursor.getString(3)
                                    },
                                accountId =
                                    if (
                                        cursor.isNull(4)
                                    ) {
                                        null
                                    } else {
                                        cursor.getString(4)
                                    }
                            )
                        )
                    }
                }
            }
    }
    }

    fun insertManualIncome(
        amountMinor: Long,
        occurredAtEpochMs: Long,
        label: String,
        matchedRule: SmartRule,
        rebuildPayCycles: Boolean = true
    ): Boolean {
        synchronized(CanonicalStoreLock.monitor) {
        require(amountMinor > 0L) { "Income amount must be greater than zero." }

        if (matchedRule.startsPayCycle && matchedRule.accountId != null) {
            val day = Calendar.getInstance().apply {
                timeInMillis = occurredAtEpochMs
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val end = (day.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
            if (transactionsBetween(day.timeInMillis, end.timeInMillis).any {
                it.ruleId == matchedRule.id && it.accountId == matchedRule.accountId &&
                    it.amountMinor == amountMinor && it.direction == MoneyDirection.INCOME.name &&
                    it.reviewState != ReviewState.EXCLUDED.name
            }) return false
        }

        val parsed = ParsedMpesaTransaction(
            transactionCode = null,
            type = MpesaTransactionType.OTHER,
            amountMinor = amountMinor,
            counterparty = label.trim().ifBlank { matchedRule.displayLabel ?: matchedRule.name },
            counterpartyPhone = null,
            feeMinor = 0L,
            balanceMinor = null,
            occurredAtText = null,
            rawMessage = "manual-income|" + matchedRule.id + "|" + occurredAtEpochMs,
            reversalOfCode = null
        )

        val inserted = insertIfNew(
            parsed = parsed,
            sourcePackage = null,
            sourceKind = "manual_setup",
            receivedAtEpochMs = System.currentTimeMillis(),
            occurredAtEpochMs = occurredAtEpochMs,
            matchedRule = matchedRule,
            rebuildPayCycles =
                false,
            runRelationshipReconciliation =
                false,
            runMirrorCorrelation =
                false
        )
        if (inserted && matchedRule.startsPayCycle) {
            val id = getByFingerprint(TransactionFingerprint.of(parsed, occurredAtEpochMs))!!.id
            ContentValues().apply {
                put("cycle_override", PayCycleStore.OVERRIDE_FORCE_START)
                put("review_state", ReviewState.USER_CLASSIFIED.name)
            }.also { database.writableDatabase.update("transactions", it, "id = ?", arrayOf(id.toString())) }
        }
        if (inserted && rebuildPayCycles) PayCycleStore.get(appContext).rebuild()
        return inserted
    }
    }

    fun snapshot(): LedgerSnapshot {
        synchronized(CanonicalStoreLock.monitor) {
        ensureMirrorReconciled()
        ensureBuiltInRecognitionReconciled()
        ensureSemanticReviewStateReconciled()
        ensureRefundsReconciled()
        val db = database.readableDatabase

        val count = db.rawQuery("SELECT COUNT(*) FROM transactions", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }

        val latest = db.rawQuery(
            BASE_SELECT + " ORDER BY occurred_at_epoch_ms DESC, id DESC LIMIT 1",
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else cursor.toStoredTransaction()
        }

        val balance = db.rawQuery(
            """
            SELECT balance_minor, observed_at
            FROM (
                SELECT balance_minor,
                       occurred_at_epoch_ms AS observed_at,
                       0 AS priority
                FROM transactions
                WHERE balance_minor IS NOT NULL
                  AND account_id = ?

                UNION ALL

                SELECT wallet_balance_after_minor AS balance_minor,
                       occurred_at_epoch_ms AS observed_at,
                       1 AS priority
                FROM debt_events
                WHERE wallet_balance_after_minor IS NOT NULL

                UNION ALL

                SELECT balance_minor,
                       observed_at_epoch_ms AS observed_at,
                       2 AS priority
                FROM account_balance_observations
                WHERE account_id = ?
            )
            ORDER BY observed_at DESC, priority DESC
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                AccountStore.DEFAULT_MPESA_ID,
                AccountStore.DEFAULT_MPESA_ID
            )
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(0) to cursor.getLong(1)
            } else {
                null
            }
        }

        return LedgerSnapshot(
            transactionCount = count,
            latestTransaction = latest,
            latestBalanceMinor = balance?.first,
            latestBalanceObservedAtEpochMs = balance?.second
        )
    }
    }

    fun getById(id: Long): StoredTransaction? = synchronized(CanonicalStoreLock.monitor) {
        database.readableDatabase.rawQuery(
            BASE_SELECT + " WHERE id = ? LIMIT 1",
            arrayOf(id.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toStoredTransaction() else null
        }
    }

    fun findByTransactionCode(
        transactionCode: String
    ): StoredTransaction? = synchronized(CanonicalStoreLock.monitor) {
        database.readableDatabase.rawQuery(
            BASE_SELECT +
                " WHERE transaction_code = ? LIMIT 1",
            arrayOf(transactionCode.uppercase())
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.toStoredTransaction()
            } else {
                null
            }
        }
    }

    fun recent(limit: Int = 100): List<StoredTransaction> = synchronized(CanonicalStoreLock.monitor) {
        database.readableDatabase.rawQuery(
            BASE_SELECT + " ORDER BY occurred_at_epoch_ms DESC, id DESC LIMIT ?",
            arrayOf(limit.coerceIn(1, 500).toString())
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toStoredTransaction())
            }
        }
    }

    /** Import order, not message date: old SMS arriving now must receive saved categories. */
    fun recentlyInsertedForLearning(limit: Int = 100): List<StoredTransaction> = synchronized(CanonicalStoreLock.monitor) {
        database.readableDatabase.rawQuery(BASE_SELECT + " ORDER BY id DESC LIMIT ?",
            arrayOf(limit.coerceIn(1, 500).toString())).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toStoredTransaction()) }
        }
    }

    fun reviewBurdenSnapshot(
        nowEpochMs: Long =
            System.currentTimeMillis()
    ): ReviewBurdenSnapshot {
        synchronized(CanonicalStoreLock.monitor) {
        ensureMirrorReconciled()
        ensureBuiltInRecognitionReconciled()
        ensureSemanticReviewStateReconciled()
        ensureRefundsReconciled()
        reconcileRecentReviewTriage(
            nowEpochMs = nowEpochMs
        )

        val transactions =
            allTransactions()
                .filter {
                    it.reviewState !=
                        ReviewState
                            .EXCLUDED.name
                }
        val duplicateWindowStart =
            ReviewBurdenAnalysis
                .windowStartEpochMs(
                    nowEpochMs =
                        nowEpochMs,
                    windowDays =
                        ReviewBurdenAnalysis
                            .WINDOW_DAYS +
                            2
                )
        val duplicateIds =
            buildPossibleDuplicateGroups(
                transactions.filter {
                    it.occurredAtEpochMs >=
                        duplicateWindowStart
                }
            ).flatMap {
                listOf(
                    it.first.id,
                    it.second.id
                )
            }.toSet()

        return ReviewBurdenAnalysis
            .calculate(
                observations =
                    transactions.map {
                            transaction ->
                        ReviewBurdenObservation(
                            id =
                                transaction.id,
                            occurredAtEpochMs =
                                transaction
                                    .occurredAtEpochMs,
                            direction =
                                runCatching {
                                    MoneyDirection
                                        .valueOf(
                                            transaction
                                                .direction
                                        )
                                }.getOrDefault(
                                    MoneyDirection
                                        .NEUTRAL
                                ),
                            category =
                                runCatching {
                                    TransactionCategory
                                        .valueOf(
                                            transaction
                                                .category
                                        )
                                }.getOrDefault(
                                    TransactionCategory
                                        .UNCATEGORIZED
                                ),
                            reviewState =
                                runCatching {
                                    ReviewState
                                        .valueOf(
                                            transaction
                                                .reviewState
                                        )
                                }.getOrDefault(
                                    ReviewState
                                        .NEEDS_REVIEW
                                ),
                            type =
                                runCatching {
                                    MpesaTransactionType
                                        .valueOf(
                                            transaction
                                                .type
                                        )
                                }.getOrDefault(
                                    MpesaTransactionType
                                        .OTHER
                                ),
                            cycleCandidateReason =
                                transaction
                                    .cycleCandidateReason,
                            possibleDuplicate =
                                transaction.id in
                                    duplicateIds
                        )
                    },
                nowEpochMs =
                    nowEpochMs
            )
    }
    }

    fun activityMonths(
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<ActivityMonthBucket> {
        synchronized(CanonicalStoreLock.monitor) {
        val range =
            database.readableDatabase
                .rawQuery(
                    """
                    SELECT
                        MIN(occurred_at_epoch_ms),
                        MAX(occurred_at_epoch_ms)
                    FROM transactions
                    WHERE review_state != ?
                    """.trimIndent(),
                    arrayOf(
                        ReviewState.EXCLUDED.name
                    )
                )
                .use { cursor ->
                    if (
                        !cursor.moveToFirst() ||
                        cursor.isNull(0) ||
                        cursor.isNull(1)
                    ) {
                        null
                    } else {
                        cursor.getLong(0) to
                            cursor.getLong(1)
                    }
                } ?: return emptyList()

        fun monthStart(
            epochMs: Long
        ): Calendar =
            Calendar.getInstance(timeZone)
                .apply {
                    timeInMillis = epochMs
                    set(
                        Calendar.DAY_OF_MONTH,
                        1
                    )
                    set(
                        Calendar.HOUR_OF_DAY,
                        0
                    )
                    set(
                        Calendar.MINUTE,
                        0
                    )
                    set(
                        Calendar.SECOND,
                        0
                    )
                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                }

        val earliest =
            monthStart(range.first)
        val cursorMonth =
            monthStart(range.second)
        val result =
            mutableListOf<ActivityMonthBucket>()

        while (
            cursorMonth.timeInMillis >=
            earliest.timeInMillis
        ) {
            val start =
                cursorMonth.timeInMillis
            val end =
                (cursorMonth.clone()
                    as Calendar)
                    .apply {
                        add(
                            Calendar.MONTH,
                            1
                        )
                    }
                    .timeInMillis

            val count =
                database.readableDatabase
                    .rawQuery(
                        """
                        SELECT COUNT(*)
                        FROM transactions
                        WHERE review_state != ?
                          AND occurred_at_epoch_ms >= ?
                          AND occurred_at_epoch_ms < ?
                        """.trimIndent(),
                        arrayOf(
                            ReviewState.EXCLUDED.name,
                            start.toString(),
                            end.toString()
                        )
                    )
                    .use { cursor ->
                        if (
                            cursor.moveToFirst()
                        ) {
                            cursor.getInt(0)
                        } else {
                            0
                        }
                    }

            if (count > 0) {
                result +=
                    ActivityMonthBucket(
                        monthStartEpochMs =
                            start,
                        transactionCount =
                            count
                    )
            }

            cursorMonth.add(
                Calendar.MONTH,
                -1
            )
        }

        return result
    }
    }

    fun onboardingDuplicateCandidateCount(
        sinceEpochMs: Long
    ): Int {
        synchronized(CanonicalStoreLock.monitor) {
        val recentTransactions =
            recent(500).filter {
                it.receivedAtEpochMs >=
                    sinceEpochMs &&
                    it.reviewState !=
                    ReviewState.EXCLUDED.name
            }

        return buildPossibleDuplicateGroups(
            recentTransactions
        ).size
    }
    }

    fun activityReviewOverview(
        performMaintenance: Boolean = true
    ): ActivityReviewOverview {
        synchronized(CanonicalStoreLock.monitor) {
        if (performMaintenance) {
            ensureMirrorReconciled()
            ensureBuiltInRecognitionReconciled()
            ensureSemanticReviewStateReconciled()
            ensureRefundsReconciled()
        }

        // Activity's attention strip only needs unresolved rows. Loading
        // the full ledger here made tab entry progressively slower as history
        // grew and made duplicate detection unnecessarily quadratic.
        val attentionTransactions =
            reviewAttentionTransactions()

        // Optional enrichment is deliberately bounded to recent activity. It
        // must never make rendering the review summary depend on the full
        // lifetime ledger.
        scheduleRemoteEntityEnrichment(
            recent(
                REVIEW_ENRICHMENT_RECENT_LIMIT
            )
        )

        val possibleDuplicates =
            buildPossibleDuplicateGroups(
                attentionTransactions
            )
        val possibleDuplicateIds =
            possibleDuplicates
                .flatMap {
                    listOf(
                        it.first.id,
                        it.second.id
                    )
                }
                .toSet()

        return ActivityReviewOverview(
            reviewGroups =
                buildReviewGroups(
                    attentionTransactions
                        .filterNot {
                            it.id in
                                possibleDuplicateIds
                        }
                ),
            possibleDuplicates =
                possibleDuplicates
        )
    }
    }

    fun activitySnapshot(
        nowEpochMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        performMaintenance: Boolean = true
    ): ActivityLedgerSnapshot {
        val monthStart = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowEpochMs
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextMonthStart = (monthStart.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        return activitySnapshotBetween(
            monthStart.timeInMillis,
            nextMonthStart.timeInMillis,
            nowEpochMs,
            timeZone,
            performMaintenance
        )
    }

    fun activitySnapshotBetween(
        startEpochMs: Long,
        endEpochMsExclusive: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        performMaintenance: Boolean = true
    ): ActivityLedgerSnapshot {
        synchronized(CanonicalStoreLock.monitor) {
        require(endEpochMsExclusive > startEpochMs)
        if (performMaintenance) {
            val salaryDb = database.writableDatabase
            salaryDb.beginTransaction()
            try {
            val correctedSalary = database.writableDatabase.compileStatement("""
                UPDATE transactions SET direction = 'EXPENSE', category = 'UNCATEGORIZED', rule_id = NULL,
                    cycle_candidate_reason = NULL, pay_cycle_id = NULL, review_state = 'AUTO_CLASSIFIED'
                WHERE type IN ('PAY_BILL','BUY_GOODS','AIRTIME','SENT_MONEY')
                  AND direction = 'INCOME' AND review_state != 'USER_CLASSIFIED' AND review_state != 'EXCLUDED'
                  AND cycle_override IS NULL AND rule_id IN (SELECT id FROM smart_rules WHERE starts_pay_cycle = 1)
            """.trimIndent()).use { it.executeUpdateDelete() }
            if (correctedSalary > 0) {
                PayCycleStore.get(appContext).rebuild()
                LedgerUpdateBus.notifyChanged()
            }
            salaryDb.setTransactionSuccessful()
            } finally {
                salaryDb.endTransaction()
            }
            ensureMirrorReconciled()
            ensureBuiltInRecognitionReconciled()
            ensureSemanticReviewStateReconciled()
            ensureRefundsReconciled()
            reconcileRecentReviewTriage(nowEpochMs, timeZone)
        }

        val reversedCodes = reversedTransactionCodes()
        val monthTransactions = transactionsBetween(
            startEpochMs = startEpochMs,
            endEpochMsExclusive = endEpochMsExclusive
        )
        val possibleDuplicates = buildPossibleDuplicateGroups(
            monthTransactions
        )
        val possibleDuplicateIds = possibleDuplicates
            .flatMap { listOf(it.first.id, it.second.id) }
            .toSet()

        val effective = monthTransactions.filter { item ->
            accountingEligible(item, reversedCodes, possibleDuplicateIds)
        }

        val income = effective
            .filter { it.direction == MoneyDirection.INCOME.name }
            .sumOf { it.amountMinor }

        val grossSpent = effective
            .filter { it.direction == MoneyDirection.EXPENSE.name }
            .sumOf { it.amountMinor }

        val refunds = effective
            .filter {
                it.relationType == RELATION_REFUND &&
                    refundOriginalIsTrusted(it)
            }
            .sumOf { it.amountMinor }

        val spent = maxOf(0L, grossSpent - refunds)

        val transfers = effective
            .filter {
                it.direction == MoneyDirection.TRANSFER.name &&
                    it.relationType != RELATION_REFUND
            }
            .sumOf { it.amountMinor }

        val fees = effective.sumOf { it.feeMinor } +
            DebtStore.get(appContext).sumFeesBetween(
                startEpochMs = startEpochMs,
                endEpochMsExclusive = endEpochMsExclusive
            )

        return ActivityLedgerSnapshot(
            transactions = monthTransactions,
            summary = ActivitySummary(
                incomeMinor = income,
                spentMinor = spent,
                feesMinor = fees,
                transferMinor = transfers,
                netMinor = income + refunds - grossSpent - fees,
                needsReviewCount = monthTransactions.count {
                    it.id !in possibleDuplicateIds &&
                        (
                            it.reviewState == ReviewState.NEEDS_REVIEW.name ||
                                it.cycleCandidateReason != null
                            )
                },
                refundMinor = refunds
            ),
            reviewGroups = buildReviewGroups(
                monthTransactions.filterNot {
                    it.id in possibleDuplicateIds
                }
            ),
            possibleDuplicates = possibleDuplicates,
            trustedTransactions = effective
        )
    }
    }

    fun reconcileRecentReviewTriage(
        nowEpochMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ) {
        synchronized(CanonicalStoreLock.monitor) {
        var changed = 0
        repeat(3) { monthOffset ->
            val calendar = Calendar.getInstance(timeZone).apply {
                timeInMillis = nowEpochMs
                add(Calendar.MONTH, -monthOffset)
            }
            changed += reconcileReviewTriageForMonth(
                calendar.timeInMillis,
                timeZone
            )
        }
        if (changed > 0) {
            PayCycleStore.get(appContext).rebuild()
        }
    }
    }

    fun reconcileReviewTriageForMonth(
        epochMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Int {
        synchronized(CanonicalStoreLock.monitor) {
        val monthStart = Calendar.getInstance(timeZone).apply {
            timeInMillis = epochMs
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextMonthStart = (monthStart.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }

        val eligible = transactionsBetween(
            startEpochMs = monthStart.timeInMillis,
            endEpochMsExclusive = nextMonthStart.timeInMillis
        ).filter { item ->
            if (item.ruleId != null) return@filter false
            if (
                item.reviewState == ReviewState.USER_CLASSIFIED.name ||
                item.reviewState == ReviewState.EXCLUDED.name
            ) return@filter false
            if (item.direction != MoneyDirection.EXPENSE.name) return@filter false
            if (item.type !in OUTGOING_REVIEW_TYPES) return@filter false
            if (
                !ReviewTriagePolicy
                    .shouldTriageCategory(
                        item.category
                    )
            ) {
                return@filter false
            }

            ReviewTriagePolicy.identityKey(
                item.counterparty,
                item.counterpartyPhone
            ) != null
        }

        val db = database.writableDatabase
        var changed = 0
        eligible.groupBy { item ->
            ReviewTriagePolicy.identityKey(
                item.counterparty,
                item.counterpartyPhone
            )!!
        }.values.forEach { group ->
            val oneOff = ReviewTriagePolicy.shouldDefaultToOther(group.size)
            val values = ContentValues().apply {
                put(
                    "category",
                    if (oneOff) {
                        TransactionCategory.OTHER.name
                    } else {
                        TransactionCategory.UNCATEGORIZED.name
                    }
                )
                put(
                    "review_state",
                    ReviewState.AUTO_CLASSIFIED.name
                )
            }

            group.forEach { item ->
                val desiredCategory = values.getAsString("category")
                val desiredReviewState = values.getAsString("review_state")
                if (
                    item.category != desiredCategory ||
                    item.reviewState != desiredReviewState
                ) {
                    changed += db.update(
                        "transactions",
                        values,
                        "id = ?",
                        arrayOf(item.id.toString())
                    )
                }
            }
        }
        return changed
    }
    }

    private fun reviewAttentionTransactions():
        List<StoredTransaction> =
        database.readableDatabase.rawQuery(
            BASE_SELECT +
                """
                 WHERE review_state = ?
                    OR cycle_candidate_reason IS NOT NULL
                 ORDER BY occurred_at_epoch_ms DESC, id DESC
                """.trimIndent(),
            arrayOf(
                ReviewState.NEEDS_REVIEW.name
            )
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        cursor
                            .toStoredTransaction()
                    )
                }
            }
        }

    private fun buildReviewGroups(
        transactions: List<StoredTransaction>
    ): List<ReviewGroup> =
        transactions
            .filter { item ->
                item.reviewState == ReviewState.NEEDS_REVIEW.name ||
                    item.cycleCandidateReason != null
            }
            .groupBy { item ->
                ReviewTriagePolicy.identityKey(
                    item.counterparty,
                    item.counterpartyPhone
                ) ?: "transaction:${item.id}"
            }
            .map { (key, items) ->
                val latest = items.maxBy(StoredTransaction::occurredAtEpochMs)
                ReviewGroup(
                    key = key,
                    label = latest.counterparty
                        ?: latest.counterpartyPhone
                        ?: latest.type.lowercase()
                            .replace('_', ' ')
                            .replaceFirstChar { it.uppercase() },
                    representativeTransactionId = latest.id,
                    transactionCount = items.size,
                    totalMinor = items.sumOf(StoredTransaction::amountMinor),
                    latestEpochMs = latest.occurredAtEpochMs
                )
            }
            .sortedByDescending(ReviewGroup::latestEpochMs)

    fun reapplyCounterpartyRules(rules: List<SmartRule>) {
        synchronized(CanonicalStoreLock.monitor) {
        if (rules.isEmpty()) return

        val db = database.writableDatabase
        learnableHistoricalTransactions().forEach { stored ->
            val type = runCatching { MpesaTransactionType.valueOf(stored.type) }
                .getOrDefault(MpesaTransactionType.OTHER)
            val parsed = ParsedMpesaTransaction(
                transactionCode = stored.transactionCode,
                type = type,
                amountMinor = stored.amountMinor,
                counterparty = stored.counterparty,
                counterpartyPhone = stored.counterpartyPhone,
                feeMinor = stored.feeMinor,
                balanceMinor = stored.balanceMinor,
                occurredAtText = stored.occurredAtText,
                rawMessage = "",
                reversalOfCode = stored.reversalOfCode
            )
            val rule = SmartRuleEngine.matchTransaction(parsed, rules) ?: return@forEach
            val classification = classify(parsed, rule, stored.occurredAtEpochMs)

            ContentValues().apply {
                put("direction", classification.direction.name)
                put("category", classification.category.name)
                put("review_state", ReviewState.AUTO_CLASSIFIED.name)
                put("rule_id", rule.id)
                put("planning_month", classification.planningMonth)
                if (rule.accountId != null) put("account_id", rule.accountId)
                if (rule.linkedAccountId != null) put("linked_account_id", rule.linkedAccountId)
                rule.displayLabel?.takeIf { it.isNotBlank() }?.let { put("counterparty", it) }
            }.also { values ->
                db.update("transactions", values, "id = ?", arrayOf(stored.id.toString()))
            }
        }
        PayCycleStore.get(appContext).rebuild()
    }
    }

    fun matchingPendingCount(
        rule: SmartRule,
        excludingId: Long? = null
    ): Int = synchronized(CanonicalStoreLock.monitor) {
        learnableHistoricalTransactions().count { stored ->
            stored.id != excludingId &&
                SmartRuleEngine.matchTransaction(
                    stored.toParsedTransaction(),
                    listOf(rule)
                ) != null
        }
    }

    fun applyRuleToTransaction(
        transactionId: Long,
        rule: SmartRule
    ): Boolean {
        synchronized(CanonicalStoreLock.monitor) {
        val stored =
            getById(transactionId)
                ?: return false
        val parsed =
            stored.toParsedTransaction()
        val classification =
            classify(
                parsed,
                rule,
                stored.occurredAtEpochMs
            )
        val values =
            ContentValues().apply {
                put(
                    "direction",
                    classification.direction.name
                )
                put(
                    "category",
                    classification.category.name
                )
                put(
                    "review_state",
                    ReviewState.USER_CLASSIFIED.name
                )
                put("rule_id", rule.id)
                put(
                    "planning_month",
                    classification.planningMonth
                )
                if (
                    rule.accountId != null
                ) {
                    put(
                        "account_id",
                        rule.accountId
                    )
                }
                if (
                    rule.linkedAccountId !=
                    null
                ) {
                    put(
                        "linked_account_id",
                        rule.linkedAccountId
                    )
                }
            }
        val changed =
            database.writableDatabase
                .update(
                    "transactions",
                    values,
                    "id = ?",
                    arrayOf(
                        transactionId.toString()
                    )
                ) > 0
        if (changed) {
            PersonalLearningStore.get(appContext)
                .clearApplication(transactionId)
            LedgerUpdateBus
                .notifyChanged()
        }
        return changed
    }
    }

    fun reapplyRuleForCounterparty(
        rule: SmartRule,
        counterpartyQuery: String,
        rebuildPayCycles: Boolean = true
    ): Int {
        synchronized(CanonicalStoreLock.monitor) {
        val query =
            counterpartyQuery.trim()
        if (query.isBlank()) {
            return 0
        }

        val db =
            database.writableDatabase
        val candidates =
            database.readableDatabase
                .rawQuery(
                    BASE_SELECT +
                        " WHERE (" +
                        "review_state = ? OR " +
                        "(review_state = ? AND rule_id IS NULL AND (" +
                        "category = ? OR EXISTS (" +
                        "SELECT 1 FROM personal_learning_applications pla " +
                        "WHERE pla.transaction_id = transactions.id" +
                        ")" +
                        "))" +
                        ") AND COALESCE(counterparty, '') LIKE ? COLLATE NOCASE" +
                        " ORDER BY occurred_at_epoch_ms DESC, id DESC",
                    arrayOf(
                        ReviewState.NEEDS_REVIEW.name,
                        ReviewState.AUTO_CLASSIFIED.name,
                        TransactionCategory.OTHER.name,
                        "%$query%"
                    )
                ).use { cursor ->
                    buildList {
                        while (
                            cursor.moveToNext()
                        ) {
                            add(
                                cursor
                                    .toStoredTransaction()
                            )
                        }
                    }
                }

        var changed = 0
        candidates.forEach { stored ->
            val parsed =
                stored.toParsedTransaction()
            if (
                SmartRuleEngine
                    .matchTransaction(
                        parsed,
                        listOf(rule)
                    ) == null
            ) {
                return@forEach
            }

            val classification =
                classify(
                    parsed,
                    rule,
                    stored.occurredAtEpochMs
                )
            val values =
                ContentValues().apply {
                    put(
                        "direction",
                        classification.direction.name
                    )
                    put(
                        "category",
                        classification.category.name
                    )
                    put(
                        "review_state",
                        ReviewState.AUTO_CLASSIFIED.name
                    )
                    put(
                        "rule_id",
                        rule.id
                    )
                    put(
                        "planning_month",
                        classification.planningMonth
                    )
                    if (
                        rule.accountId != null
                    ) {
                        put(
                            "account_id",
                            rule.accountId
                        )
                    }
                    if (
                        rule.linkedAccountId != null
                    ) {
                        put(
                            "linked_account_id",
                            rule.linkedAccountId
                        )
                    }
                    rule.displayLabel
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                            put(
                                "counterparty",
                                it
                            )
                        }
                }

            val updated =
                db.update(
                    "transactions",
                    values,
                    "id = ?",
                    arrayOf(
                        stored.id.toString()
                    )
                )
            if (updated > 0) {
                PersonalLearningStore
                    .get(appContext)
                    .clearApplication(
                        stored.id
                    )
                changed += updated
            }
        }

        if (
            changed > 0 &&
            rebuildPayCycles
        ) {
            PayCycleStore
                .get(appContext)
                .rebuild()
        }
        return changed
    }
    }

    fun reapplyRule(
        rule: SmartRule,
        rebuildPayCycles: Boolean = true
    ): Int {
        synchronized(CanonicalStoreLock.monitor) {
        val db = database.writableDatabase
        var changed = 0

        learnableHistoricalTransactions().forEach { stored ->
            val parsed = stored.toParsedTransaction()
            if (SmartRuleEngine.matchTransaction(parsed, listOf(rule)) == null) return@forEach

            val classification = classify(parsed, rule, stored.occurredAtEpochMs)
            val values = ContentValues().apply {
                put("direction", classification.direction.name)
                put("category", classification.category.name)
                put("review_state", ReviewState.AUTO_CLASSIFIED.name)
                put("rule_id", rule.id)
                put("planning_month", classification.planningMonth)
                if (rule.accountId != null) put("account_id", rule.accountId)
                if (rule.linkedAccountId != null) put("linked_account_id", rule.linkedAccountId)
                rule.displayLabel?.takeIf { it.isNotBlank() }?.let {
                    put("counterparty", it)
                }
            }
            val updated = db.update(
                "transactions",
                values,
                "id = ?",
                arrayOf(stored.id.toString())
            )
            if (updated > 0) {
                PersonalLearningStore.get(appContext)
                    .clearApplication(stored.id)
                changed += updated
            }
        }

        if (
            changed > 0 &&
            rebuildPayCycles
        ) {
            PayCycleStore.get(appContext).rebuild()
        }
        return changed
    }
    }

    private fun isLearnableHistoricalMatch(
        stored: StoredTransaction
    ): Boolean =
        stored.reviewState == ReviewState.NEEDS_REVIEW.name ||
            (
                stored.reviewState == ReviewState.AUTO_CLASSIFIED.name &&
                    stored.ruleId == null &&
                    stored.category == TransactionCategory.OTHER.name
            )

    private fun learnableHistoricalTransactions(): List<StoredTransaction> =
        database.readableDatabase.rawQuery(
            BASE_SELECT +
                " WHERE review_state = ?" +
                " OR (review_state = ? AND rule_id IS NULL AND (" +
                "category = ? OR EXISTS (" +
                "SELECT 1 FROM personal_learning_applications pla " +
                "WHERE pla.transaction_id = transactions.id" +
                ")" +
                "))" +
                " ORDER BY occurred_at_epoch_ms DESC, id DESC",
            arrayOf(
                ReviewState.NEEDS_REVIEW.name,
                ReviewState.AUTO_CLASSIFIED.name,
                TransactionCategory.OTHER.name
            )
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toStoredTransaction())
            }
        }

    /** All imported months; category learning never changes structural ledger events. */
    fun categoryLearningCandidates(): List<StoredTransaction> = synchronized(CanonicalStoreLock.monitor) {
        database.readableDatabase.rawQuery(
            BASE_SELECT + """
             WHERE direction = 'EXPENSE' AND rule_id IS NULL AND relation_type IS NULL
               AND type != 'REVERSAL' AND reversal_of_code IS NULL
               AND category NOT IN ('DEBT_LOAN', 'SAVINGS', 'INVESTMENT', 'TRANSFER', 'INCOME', 'CASH')
               AND review_state IN ('NEEDS_REVIEW', 'AUTO_CLASSIFIED')
               AND id NOT IN (SELECT linked_transaction_id FROM debt_events
                   WHERE linked_transaction_id IS NOT NULL AND event_type = 'REPAYMENT')
             ORDER BY occurred_at_epoch_ms DESC, id DESC
            """.trimIndent(), null
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.toStoredTransaction()) } }
    }

    fun reconcileMirrorDuplicates(): Int {
        synchronized(CanonicalStoreLock.monitor) {
        val transactions = allTransactions()
        val mpesaCanonical = transactions.filter {
            !it.transactionCode.isNullOrBlank() &&
                it.reviewState != ReviewState.EXCLUDED.name
        }
        val duplicates = transactions.filter {
            it.transactionCode.isNullOrBlank() &&
                it.reviewState != ReviewState.USER_CLASSIFIED.name &&
                it.reviewState != ReviewState.EXCLUDED.name
        }
        val db = database.writableDatabase
        val claimedDuplicateIds = mutableSetOf<Long>()
        var removed = 0

        mpesaCanonical.forEach { canonical ->
            val canonicalReferences =
                externalReferencesForTransaction(
                    canonical.id
                )
            val canonicalObservation =
                canonical.toMirrorObservation(
                    externalReferences =
                        canonicalReferences
                )
            val candidate = duplicates
                .asSequence()
                .filter { it.id != canonical.id }
                .filter { it.id !in claimedDuplicateIds }
                .filter { it.amountMinor == canonical.amountMinor }
                .filter { duplicate ->
                    val duplicateReferences =
                        externalReferencesForTransaction(
                            duplicate.id
                        )
                    val sharedReference =
                        canonicalReferences
                            .intersect(
                                duplicateReferences
                            )
                            .isNotEmpty()
                    sharedReference ||
                        kotlin.math.abs(
                            duplicate.receivedAtEpochMs -
                                canonical.receivedAtEpochMs
                        ) <=
                        MirrorTransactionMatcher
                            .MAX_TIME_DELTA_MS
                }
                .filter { duplicate ->
                    MirrorTransactionMatcher.isMirror(
                        canonicalObservation,
                        duplicate.toMirrorObservation(
                            externalReferences =
                                externalReferencesForTransaction(
                                    duplicate.id
                                )
                        )
                    )
                }
                .minByOrNull {
                    kotlin.math.abs(
                        it.receivedAtEpochMs - canonical.receivedAtEpochMs
                    )
                } ?: return@forEach

            claimedDuplicateIds += candidate.id

            attachExternalReferences(
                transactionId = canonical.id,
                references =
                    externalReferencesForTransaction(
                        candidate.id
                    ),
                sourceKind = candidate.sourceKind,
                observedAtEpochMs =
                    candidate.receivedAtEpochMs
            )

            if (canonical.ruleId == null && candidate.ruleId != null) {
                val values = ContentValues().apply {
                    put("direction", candidate.direction)
                    put("category", candidate.category)
                    put("review_state", candidate.reviewState)
                    put("rule_id", candidate.ruleId)
                    if (candidate.planningMonth != null) {
                        put("planning_month", candidate.planningMonth)
                    }
                }
                db.update(
                    "transactions",
                    values,
                    "id = ?",
                    arrayOf(canonical.id.toString())
                )
            }

            removed += db.delete(
                "transactions",
                "id = ?",
                arrayOf(candidate.id.toString())
            )
        }

        if (removed > 0) {
            PayCycleStore.get(appContext).rebuild()
        }
        return removed
    }
    }

    private fun ensureMirrorReconciled() {
        val preferences = appContext.getSharedPreferences(
            "pesatrack_preferences",
            Context.MODE_PRIVATE
        )
        if (preferences.getBoolean(MIRROR_RECONCILIATION_KEY, false)) return

        reconcileMirrorDuplicates()
        preferences.edit()
            .putBoolean(MIRROR_RECONCILIATION_KEY, true)
            .apply()
    }

    private fun ensureBuiltInRecognitionReconciled() {
        val preferences = appContext.getSharedPreferences(
            "pesatrack_preferences",
            Context.MODE_PRIVATE
        )
        if (
            preferences.getBoolean(
                BUILT_IN_RECOGNITION_RECONCILIATION_KEY,
                false
            )
        ) {
            return
        }

        val db = database.writableDatabase
        var changed = false
        allTransactions().forEach { stored ->
            if (
                stored.reviewState == ReviewState.USER_CLASSIFIED.name ||
                stored.reviewState == ReviewState.EXCLUDED.name ||
                stored.ruleId != null
            ) {
                return@forEach
            }

            val recognized = KenyanTransactionRecognizer.classify(
                stored.toParsedTransaction()
            ) ?: return@forEach

            val values = ContentValues().apply {
                put("direction", recognized.direction.name)
                put("category", recognized.category.name)
                put("review_state", ReviewState.AUTO_CLASSIFIED.name)
                putNull("rule_id")
                recognized.entity?.entity
                    ?.canonicalName
                    ?.let {
                        put("counterparty", it)
                    }
                if (
                    recognized.evidenceSource != null
                ) {
                    put(
                        "evidence_label",
                        recognized.evidenceLabel
                    )
                    put(
                        "evidence_source",
                        recognized.evidenceSource
                    )
                    put(
                        "evidence_confidence",
                        recognized.evidenceConfidence
                            ?.name
                    )
                    putNull(
                        "evidence_time_delta_ms"
                    )
                }
            }
            changed =
                db.update(
                    "transactions",
                    values,
                    "id = ?",
                    arrayOf(stored.id.toString())
                ) > 0 || changed
        }

        if (changed) {
            PayCycleStore.get(appContext).rebuild()
        }

        preferences.edit()
            .putBoolean(
                BUILT_IN_RECOGNITION_RECONCILIATION_KEY,
                true
            )
            .apply()
    }

    private fun ensureSemanticReviewStateReconciled() {
        val preferences = appContext.getSharedPreferences(
            "pesatrack_preferences",
            Context.MODE_PRIVATE
        )
        if (
            preferences.getBoolean(
                SEMANTIC_REVIEW_RECONCILIATION_KEY,
                false
            )
        ) {
            return
        }

        val values = ContentValues().apply {
            put(
                "review_state",
                ReviewState.AUTO_CLASSIFIED.name
            )
        }
        val changed =
            database.writableDatabase.update(
                "transactions",
                values,
                """
                review_state = ?
                AND rule_id IS NULL
                AND cycle_candidate_reason IS NULL
                AND account_id IS NOT NULL
                AND direction IN (?, ?)
                AND type IN (?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    ReviewState.NEEDS_REVIEW.name,
                    MoneyDirection.INCOME.name,
                    MoneyDirection.EXPENSE.name,
                    MpesaTransactionType.RECEIVED.name,
                    MpesaTransactionType.DEPOSIT.name,
                    MpesaTransactionType.SENT_MONEY.name,
                    MpesaTransactionType.PAY_BILL.name,
                    MpesaTransactionType.BUY_GOODS.name
                )
            )

        if (changed > 0) {
            PayCycleStore.get(appContext).rebuild()
        }

        preferences.edit()
            .putBoolean(
                SEMANTIC_REVIEW_RECONCILIATION_KEY,
                true
            )
            .apply()
    }

    private fun refundOriginalIsTrusted(
        refund: StoredTransaction
    ): Boolean {
        val originalId =
            refund.relatedTransactionId ?: return false
        return trustedRefundOriginal(originalId) != null
    }

    /** Cross-period refund eligibility uses the same exclusions as the ledger. */
    fun trustedRefundOriginal(originalId: Long): StoredTransaction? = synchronized(CanonicalStoreLock.monitor) {
        val original = getById(originalId) ?: return@synchronized null
        original.takeIf {
            it.direction == MoneyDirection.EXPENSE.name &&
                accountingEligible(it, reversedTransactionCodes(), emptySet())
        }
    }

    private fun accountingEligible(
        item: StoredTransaction,
        reversedCodes: Set<String>,
        possibleDuplicateIds: Set<Long>
    ): Boolean = item.id !in possibleDuplicateIds &&
        item.transactionCode !in reversedCodes &&
        item.type != MpesaTransactionType.REVERSAL.name &&
        item.reviewState != ReviewState.EXCLUDED.name &&
        item.reviewState != ReviewState.NEEDS_REVIEW.name &&
        item.cycleCandidateReason == null

    private fun ensureRefundsReconciled() {
        val preferences = appContext.getSharedPreferences(
            "pesatrack_preferences",
            Context.MODE_PRIVATE
        )
        if (
            preferences.getBoolean(
                REFUND_RECONCILIATION_KEY,
                false
            )
        ) {
            return
        }

        // Legacy amount/name matches have no retained message proof. Preserve
        // explicit user decisions; send only automatic matches back to review.
        val values = ContentValues().apply {
            putNull("relation_type")
            putNull("related_transaction_id")
            put("direction", MoneyDirection.INCOME.name)
            put("category", TransactionCategory.UNCATEGORIZED.name)
            put("review_state", ReviewState.NEEDS_REVIEW.name)
            putNull("evidence_label")
            putNull("evidence_source")
            putNull("evidence_confidence")
            putNull("evidence_time_delta_ms")
            putNull("cycle_candidate_reason")
        }
        val changed = database.writableDatabase.update(
            "transactions", values,
            "relation_type = ? AND evidence_source = ? AND review_state = ? AND rule_id IS NULL",
            arrayOf(RELATION_REFUND, "refund_match", ReviewState.AUTO_CLASSIFIED.name)
        ) > 0

        if (changed) {
            PayCycleStore.get(appContext).rebuild()
        }

        preferences.edit()
            .putBoolean(REFUND_RECONCILIATION_KEY, true)
            .apply()
    }

    private fun tryLinkRefund(
        transactionId: Long,
        rawMessage: String
    ): Boolean {
        if (!EXPLICIT_COMPLETED_REFUND.containsMatchIn(rawMessage)) return false
        val incoming = getById(transactionId) ?: return false
        if (
            incoming.type != MpesaTransactionType.RECEIVED.name &&
            incoming.type != MpesaTransactionType.DEPOSIT.name
        ) {
            return false
        }
        if (
            incoming.relationType != null ||
            incoming.ruleId != null ||
            incoming.reviewState == ReviewState.USER_CLASSIFIED.name ||
            incoming.reviewState == ReviewState.EXCLUDED.name
        ) {
            return false
        }

        val candidates = database.readableDatabase.rawQuery(
            """
            SELECT id, occurred_at_epoch_ms, amount_minor, counterparty
            FROM transactions
            WHERE id != ?
              AND direction = ?
              AND amount_minor = ?
              AND occurred_at_epoch_ms <= ?
              AND occurred_at_epoch_ms >= ?
              AND review_state NOT IN (?, ?)
              AND relation_type IS NULL
              AND cycle_candidate_reason IS NULL
              AND id NOT IN (
                    SELECT related_transaction_id
                    FROM transactions
                    WHERE relation_type = ?
                      AND related_transaction_id IS NOT NULL
              )
            ORDER BY occurred_at_epoch_ms DESC, id DESC
            """.trimIndent(),
            arrayOf(
                transactionId.toString(),
                MoneyDirection.EXPENSE.name,
                incoming.amountMinor.toString(),
                incoming.occurredAtEpochMs.toString(),
                (
                    incoming.occurredAtEpochMs -
                        RefundMatchingPolicy.MAX_REFUND_DISTANCE_MS
                ).toString(),
                ReviewState.EXCLUDED.name,
                ReviewState.NEEDS_REVIEW.name,
                RELATION_REFUND
            )
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        RefundCandidate(
                            transactionId = cursor.getLong(0),
                            occurredAtEpochMs = cursor.getLong(1),
                            amountMinor = cursor.getLong(2),
                            counterparty =
                                if (cursor.isNull(3)) {
                                    null
                                } else {
                                    cursor.getString(3)
                                }
                        )
                    )
                }
            }
        }

        val originalId = RefundMatchingPolicy.chooseCandidate(
            receivedAmountMinor = incoming.amountMinor,
            receivedAtEpochMs = incoming.occurredAtEpochMs,
            receivedCounterparty = incoming.counterparty,
            candidates = candidates.filter { trustedRefundOriginal(it.transactionId) != null }
        ) ?: return false

        val original = getById(originalId) ?: return false
        val evidenceLabel = buildString {
            append("Refund")
            original.counterparty
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    append(" for ")
                    append(it)
                }
        }

        val values = ContentValues().apply {
            put("direction", MoneyDirection.TRANSFER.name)
            put("category", original.category)
            put("review_state", ReviewState.AUTO_CLASSIFIED.name)
            put("relation_type", RELATION_REFUND)
            put("related_transaction_id", original.id)
            put("evidence_label", evidenceLabel)
            put("evidence_source", "refund_explicit_match")
            put("evidence_confidence", "HIGH")
            put(
                "evidence_time_delta_ms",
                incoming.occurredAtEpochMs -
                    original.occurredAtEpochMs
            )
            putNull("cycle_candidate_reason")
        }

        return database.writableDatabase.update(
            "transactions",
            values,
            "id = ?",
            arrayOf(transactionId.toString())
        ) > 0
    }

    private fun findMirrorCandidate(
        parsed: ParsedMpesaTransaction,
        sourcePackage: String?,
        sourceKind: String,
        receivedAtEpochMs: Long,
        occurredAtEpochMs: Long,
        direction: String,
        externalReferences: Set<String>,
        accountIds: Set<String>
    ): StoredTransaction? {
        if (
            parsed.type ==
                MpesaTransactionType.REVERSAL
        ) {
            return null
        }

        val incoming = MirrorTransactionObservation(
            amountMinor = parsed.amountMinor,
            occurredAtEpochMs = occurredAtEpochMs,
            receivedAtEpochMs = receivedAtEpochMs,
            sourceKind = sourceKind,
            sourcePackage = sourcePackage,
            transactionCode = parsed.transactionCode,
            type = parsed.type.name,
            counterparty = parsed.counterparty,
            counterpartyPhone =
                parsed.counterpartyPhone,
            direction = direction,
            externalReferences =
                externalReferences,
            accountIds = accountIds
        )

        val exactReferenceCandidates =
            findTransactionsByExternalReferences(
                references = externalReferences,
                amountMinor = parsed.amountMinor
            )
                .filterNot(::isProtectedCorrelationTarget)

        if (exactReferenceCandidates.size == 1) {
            return exactReferenceCandidates.single()
        }
        if (exactReferenceCandidates.size > 1) {
            return null
        }

        val matches =
            database.readableDatabase.rawQuery(
                BASE_SELECT +
                    " WHERE amount_minor = ?" +
                    " AND received_at_epoch_ms >= ?" +
                    " AND received_at_epoch_ms <= ?" +
                    " ORDER BY ABS(received_at_epoch_ms - ?) ASC" +
                    " LIMIT 12",
                arrayOf(
                    parsed.amountMinor.toString(),
                    (
                        receivedAtEpochMs -
                            MirrorTransactionMatcher
                                .MAX_TIME_DELTA_MS
                        ).toString(),
                    (
                        receivedAtEpochMs +
                            MirrorTransactionMatcher
                                .MAX_TIME_DELTA_MS
                        ).toString(),
                    receivedAtEpochMs.toString()
                )
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val candidate =
                            cursor.toStoredTransaction()
                        if (
                            isProtectedCorrelationTarget(
                                candidate
                            )
                        ) {
                            continue
                        }

                        val match =
                            MirrorTransactionMatcher.match(
                                incoming,
                                candidate.toMirrorObservation(
                                    externalReferences =
                                        externalReferencesForTransaction(
                                            candidate.id
                                        )
                                )
                            )
                        if (match.autoLink) {
                            add(candidate to match)
                        }
                    }
                }
            }

        if (matches.size != 1) {
            return null
        }
        return matches.single().first
    }

    fun findByAnyExternalReference(
        references: Set<String>
    ): StoredTransaction? {
        synchronized(CanonicalStoreLock.monitor) {
        val normalized =
            normalizedCorrelationReferences(
                transactionCode = null,
                externalReferences = references
            )
        val matches =
            findTransactionsByExternalReferences(
                references = normalized,
                amountMinor = null
            )
        return matches.singleOrNull()
    }
    }

    private fun findTransactionsByExternalReferences(
        references: Set<String>,
        amountMinor: Long?
    ): List<StoredTransaction> {
        if (references.isEmpty()) {
            return emptyList()
        }

        val placeholders =
            references.joinToString(",") { "?" }
        val args =
            buildList {
                addAll(references)
                if (amountMinor != null) {
                    add(amountMinor.toString())
                }
            }.toTypedArray()
        val amountClause =
            if (amountMinor != null) {
                " AND t.amount_minor = ?"
            } else {
                ""
            }

        return database.readableDatabase.rawQuery(
            """
            SELECT DISTINCT
                   t.id, t.transaction_code, t.fingerprint,
                   t.type, t.amount_minor, t.fee_minor,
                   t.balance_minor, t.counterparty,
                   t.counterparty_phone, t.occurred_at_text,
                   t.occurred_at_epoch_ms,
                   t.received_at_epoch_ms, t.source_package,
                   t.source_kind, t.direction, t.category,
                   t.review_state, t.rule_id, t.planning_month,
                   t.reversal_of_code, t.account_id,
                   t.linked_account_id, t.pay_cycle_id,
                   t.cycle_candidate_reason, t.cycle_override,
                   t.fuliza_financed_minor,
                   t.fuliza_access_fee_minor,
                   t.fuliza_outstanding_minor,
                   t.fuliza_event_code, t.evidence_label,
                   t.evidence_source, t.evidence_confidence,
                   t.evidence_time_delta_ms, t.relation_type,
                   t.related_transaction_id
            FROM transactions t
            JOIN transaction_external_references r
              ON r.transaction_id = t.id
            WHERE r.reference_value IN ($placeholders)
            $amountClause
            ORDER BY t.received_at_epoch_ms DESC, t.id DESC
            LIMIT 12
            """.trimIndent(),
            args
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toStoredTransaction())
                }
            }
        }
    }

    private fun isProtectedCorrelationTarget(
        transaction: StoredTransaction
    ): Boolean =
        transaction.reviewState ==
            ReviewState.USER_CLASSIFIED.name ||
            transaction.reviewState ==
                ReviewState.EXCLUDED.name

    private fun attachExternalReferences(
        transactionId: Long,
        references: Set<String>,
        sourceKind: String,
        observedAtEpochMs: Long,
        db: SQLiteDatabase =
            database.writableDatabase
    ) {
        if (references.isEmpty()) {
            return
        }
        references.forEach { reference ->
            val values = ContentValues().apply {
                put("transaction_id", transactionId)
                put("reference_value", reference)
                putNull("reference_label")
                put("source_kind", sourceKind)
                put(
                    "observed_at_epoch_ms",
                    observedAtEpochMs
                )
            }
            db.insertWithOnConflict(
                "transaction_external_references",
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }
    }

    private fun externalReferencesForTransaction(
        transactionId: Long
    ): Set<String> =
        database.readableDatabase.rawQuery(
            """
            SELECT reference_value
            FROM transaction_external_references
            WHERE transaction_id = ?
            """.trimIndent(),
            arrayOf(transactionId.toString())
        ).use { cursor ->
            buildSet {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0))
                }
            }
        }

    private fun normalizedCorrelationReferences(
        transactionCode: String?,
        externalReferences: Set<String>
    ): Set<String> =
        buildSet {
            (
                externalReferences +
                    listOfNotNull(transactionCode)
                )
                .mapNotNull { value ->
                    value.trim()
                        .uppercase()
                        .replace(
                            Regex("""[^A-Z0-9-]"""),
                            ""
                        )
                        .takeIf { it.length >= 5 }
                }
                .forEach(::add)
        }

    private fun mergeMpesaMirrorIntoExisting(
        existing: StoredTransaction,
        parsed: ParsedMpesaTransaction,
        fingerprint: String,
        sourcePackage: String?,
        sourceKind: String,
        receivedAtEpochMs: Long,
        occurredAtEpochMs: Long,
        classification: Classification,
        displayCounterparty: String?,
        accountId: String?,
        linkedAccountId: String?,
        evidence: TransactionEvidenceMatch?
    ) {
        val keepExistingRule =
            existing.ruleId != null && classification.ruleId == null

        val values = ContentValues().apply {
            if (
                existing.transactionCode.isNullOrBlank() &&
                !parsed.transactionCode.isNullOrBlank()
            ) {
                put(
                    "transaction_code",
                    parsed.transactionCode
                )
                put("fingerprint", fingerprint)
            }

            put("type", parsed.type.name)
            put("fee_minor", parsed.feeMinor)
            if (parsed.balanceMinor != null) {
                put("balance_minor", parsed.balanceMinor)
            }
            if (!displayCounterparty.isNullOrBlank()) {
                put("counterparty", displayCounterparty)
            }
            if (!parsed.counterpartyPhone.isNullOrBlank()) {
                put("counterparty_phone", parsed.counterpartyPhone)
            }
            if (!parsed.occurredAtText.isNullOrBlank()) {
                put("occurred_at_text", parsed.occurredAtText)
            }
            put("occurred_at_epoch_ms", occurredAtEpochMs)
            put("received_at_epoch_ms", receivedAtEpochMs)
            put("source_package", sourcePackage)
            put("source_kind", sourceKind)

            if (!keepExistingRule) {
                put("direction", classification.direction.name)
                put("category", classification.category.name)
                put("review_state", classification.reviewState.name)
                if (classification.ruleId != null) {
                    put("rule_id", classification.ruleId)
                } else {
                    putNull("rule_id")
                }
                put("planning_month", classification.planningMonth)
            }

            if (accountId != null) put("account_id", accountId)
            if (linkedAccountId != null) put("linked_account_id", linkedAccountId)
            if (evidence != null) {
                put("evidence_label", evidence.label)
                put("evidence_source", evidence.sourceLabel)
                put("evidence_confidence", evidence.confidence.name)
                put("evidence_time_delta_ms", evidence.timeDeltaMs)
            } else if (
                classification.evidenceSource != null
            ) {
                put(
                    "evidence_label",
                    classification.evidenceLabel
                )
                put(
                    "evidence_source",
                    classification.evidenceSource
                )
                put(
                    "evidence_confidence",
                    classification.evidenceConfidence
                        ?.name
                )
                putNull("evidence_time_delta_ms")
            }
            putNull("pay_cycle_id")
            putNull("cycle_candidate_reason")
        }

        database.writableDatabase.update(
            "transactions",
            values,
            "id = ?",
            arrayOf(existing.id.toString())
        )
    }

    private fun getByTransactionCode(code: String): StoredTransaction? =
        database.readableDatabase.rawQuery(
            BASE_SELECT + " WHERE transaction_code = ? LIMIT 1",
            arrayOf(code.trim().uppercase())
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toStoredTransaction() else null
        }

    private fun updateExistingDuplicateWithEvidence(
        stored: StoredTransaction,
        parsed: ParsedMpesaTransaction,
        matchedRule: SmartRule?,
        evidence: TransactionEvidenceMatch
    ) {
        val protected =
            stored.reviewState == ReviewState.USER_CLASSIFIED.name ||
                stored.reviewState == ReviewState.EXCLUDED.name

        val values = ContentValues().apply {
            put("evidence_label", evidence.label)
            put("evidence_source", evidence.sourceLabel)
            put("evidence_confidence", evidence.confidence.name)
            put("evidence_time_delta_ms", evidence.timeDeltaMs)

            if (
                !protected &&
                evidence.confidence == EvidenceConfidence.HIGH &&
                stored.counterparty.isNullOrBlank()
            ) {
                parsed.counterparty?.let { put("counterparty", it) }
                parsed.counterpartyPhone?.let { put("counterparty_phone", it) }

                if (matchedRule != null) {
                    val classification = classify(
                        parsed = parsed,
                        matchedRule = matchedRule,
                        occurredAtEpochMs = stored.occurredAtEpochMs
                    )
                    put("direction", classification.direction.name)
                    put("category", classification.category.name)
                    put("review_state", classification.reviewState.name)
                    put("rule_id", classification.ruleId)
                    put("planning_month", classification.planningMonth)
                }
            }
        }

        database.writableDatabase.update(
            "transactions",
            values,
            "id = ?",
            arrayOf(stored.id.toString())
        )

        if (
            !protected &&
            evidence.confidence == EvidenceConfidence.HIGH &&
            stored.counterparty.isNullOrBlank()
        ) {
            reconcileReviewTriageForMonth(stored.occurredAtEpochMs)
            PayCycleStore.get(appContext).rebuild()
        }
    }

    fun attachEvidenceToNearbyUnknown(
        evidence: FinancialMessageEvidence,
        rules: List<SmartRule>
    ): Boolean {
        synchronized(CanonicalStoreLock.monitor) {
        val start = evidence.observedAtEpochMs - TransactionEvidenceResolver.MAX_WINDOW_MS
        val end = evidence.observedAtEpochMs + TransactionEvidenceResolver.MAX_WINDOW_MS
        val candidates = database.readableDatabase.rawQuery(
            BASE_SELECT +
                " WHERE transaction_code IS NOT NULL" +
                " AND amount_minor = ?" +
                " AND received_at_epoch_ms >= ?" +
                " AND received_at_epoch_ms <= ?" +
                " AND (counterparty IS NULL OR TRIM(counterparty) = '')" +
                " ORDER BY ABS(received_at_epoch_ms - ?) ASC" +
                " LIMIT 8",
            arrayOf(
                evidence.amountMinor.toString(),
                start.toString(),
                end.toString(),
                evidence.observedAtEpochMs.toString()
            )
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val item = cursor.toStoredTransaction()
                    if (
                        item.reviewState != ReviewState.USER_CLASSIFIED.name &&
                        item.reviewState != ReviewState.EXCLUDED.name
                    ) add(item)
                }
            }
        }

        val matches = candidates.mapNotNull { candidate ->
            TransactionEvidenceResolver.match(
                parsed = candidate.toParsedTransaction(),
                transactionObservedAtEpochMs = candidate.receivedAtEpochMs,
                evidence = evidence
            )?.let { candidate to it }
        }
        if (matches.isEmpty()) return false

        val exactReference = matches.firstOrNull { (candidate, match) ->
            match.confidence == EvidenceConfidence.HIGH &&
                !candidate.transactionCode.isNullOrBlank() &&
                candidate.transactionCode.equals(
                    evidence.referenceCode,
                    ignoreCase = true
                )
        }

        val selected = exactReference ?: when {
            matches.size == 1 -> matches.first()
            else -> {
                val high = matches.filter {
                    it.second.confidence == EvidenceConfidence.HIGH
                }
                if (high.size == 1) high.first() else return false
            }
        }

        applyEvidence(selected.first, selected.second, rules)
        return true
    }
    }

    fun acceptEvidenceIdentity(id: Long): Boolean {
        synchronized(CanonicalStoreLock.monitor) {
        val stored = getById(id) ?: return false
        val label = stored.evidenceLabel?.trim()?.takeIf { it.isNotBlank() }
            ?: return false
        if (!stored.counterparty.isNullOrBlank()) return false

        val rules = RuleStore.get(appContext).listRules(enabledOnly = true)
        applyEvidence(
            stored = stored,
            match = TransactionEvidenceMatch(
                label = label,
                phone = null,
                sourceLabel = stored.evidenceSource ?: "Nearby financial message",
                confidence = EvidenceConfidence.HIGH,
                timeDeltaMs = stored.evidenceTimeDeltaMs ?: 0L
            ),
            rules = rules,
            acceptedByUser = true
        )
        return true
    }
    }

    private fun applyEvidence(
        stored: StoredTransaction,
        match: TransactionEvidenceMatch,
        rules: List<SmartRule>,
        acceptedByUser: Boolean = false
    ) {
        val values = ContentValues().apply {
            put("evidence_label", match.label)
            put("evidence_source", match.sourceLabel)
            put(
                "evidence_confidence",
                if (acceptedByUser) "USER_ACCEPTED" else match.confidence.name
            )
            put("evidence_time_delta_ms", match.timeDeltaMs)
        }

        if (match.confidence == EvidenceConfidence.HIGH || acceptedByUser) {
            val enriched = TransactionEvidenceResolver.enrich(
                stored.toParsedTransaction(),
                match
            )
            values.put("counterparty", enriched.counterparty)
            if (!enriched.counterpartyPhone.isNullOrBlank()) {
                values.put("counterparty_phone", enriched.counterpartyPhone)
            }

            SmartRuleEngine.matchTransaction(enriched, rules)?.let { rule ->
                val classification = classify(enriched, rule, stored.occurredAtEpochMs)
                values.put("direction", classification.direction.name)
                values.put("category", classification.category.name)
                values.put("review_state", classification.reviewState.name)
                values.put("rule_id", classification.ruleId)
                values.put("planning_month", classification.planningMonth)
            }
        }

        database.writableDatabase.update(
            "transactions",
            values,
            "id = ?",
            arrayOf(stored.id.toString())
        )

        if (match.confidence == EvidenceConfidence.HIGH || acceptedByUser) {
            reconcileReviewTriageForMonth(stored.occurredAtEpochMs)
            PayCycleStore.get(appContext).rebuild()
        }
    }

    fun reconcileOwnAccountTransfers(
        rebuildPayCycles: Boolean = true
    ): Int {
        synchronized(CanonicalStoreLock.monitor) {
        val accounts = AccountStore.get(appContext).listAccounts()
        val db = database.writableDatabase
        var changed = 0

        allTransactions().forEach { stored ->
            if (
                stored.reviewState == ReviewState.USER_CLASSIFIED.name ||
                stored.reviewState == ReviewState.EXCLUDED.name
            ) {
                return@forEach
            }

            val parsed = stored.toParsedTransaction()
            val match = OwnAccountTransferDetector.matchMpesa(
                parsed = parsed,
                accounts = accounts,
                observedWalletId = AccountStore.DEFAULT_MPESA_ID
            ) ?: return@forEach

            if (
                stored.ruleId != null &&
                match.evidence != OwnTransferEvidence.EXACT_IDENTIFIER
            ) {
                return@forEach
            }

            val values = ContentValues().apply {
                put("direction", MoneyDirection.TRANSFER.name)
                put("category", TransactionCategory.TRANSFER.name)
                put("review_state", ReviewState.AUTO_CLASSIFIED.name)
                putNull("rule_id")
                put("account_id", match.sourceAccountId)
                put("linked_account_id", match.destinationAccountId)
            }
            changed += db.update(
                "transactions",
                values,
                "id = ?",
                arrayOf(stored.id.toString())
            )
        }

        if (
            changed > 0 &&
            rebuildPayCycles
        ) {
            PayCycleStore.get(appContext).rebuild()
        }
        return changed
    }
    }

    fun resolvePossibleDuplicate(
        pairKey: String,
        sameTransaction: Boolean
    ): Boolean {
        synchronized(CanonicalStoreLock.monitor) {
        val ids = pairKey.split(":").mapNotNull(String::toLongOrNull)
        if (ids.size != 2) return false

        val first = getById(ids[0]) ?: return false
        val second = getById(ids[1]) ?: return false
        val now = System.currentTimeMillis()

        if (sameTransaction) {
            val canonical = chooseDuplicateCanonical(first, second)
            val duplicate = if (canonical.id == first.id) second else first
            val values = ContentValues().apply {
                put("review_state", ReviewState.EXCLUDED.name)
                putNull("cycle_candidate_reason")
            }
            database.writableDatabase.update(
                "transactions",
                values,
                "id = ?",
                arrayOf(duplicate.id.toString())
            )
        }

        val resolution = ContentValues().apply {
            put("pair_key", pairKey)
            put("first_transaction_id", first.id)
            put("second_transaction_id", second.id)
            put(
                "decision",
                if (sameTransaction) {
                    DUPLICATE_DECISION_SAME
                } else {
                    DUPLICATE_DECISION_KEEP_BOTH
                }
            )
            put("created_at_epoch_ms", now)
            put("updated_at_epoch_ms", now)
        }
        database.writableDatabase.insertWithOnConflict(
            "duplicate_resolutions",
            null,
            resolution,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )

        PayCycleStore.get(appContext).rebuild()
        LedgerUpdateBus.notifyChanged()
        return true
    }
    }

    private fun chooseDuplicateCanonical(
        first: StoredTransaction,
        second: StoredTransaction
    ): StoredTransaction {
        fun score(item: StoredTransaction): Int {
            var value = 0
            if (!item.transactionCode.isNullOrBlank()) value += 8
            if (!item.counterparty.isNullOrBlank()) value += 4
            if (!item.counterpartyPhone.isNullOrBlank()) value += 3
            if (!item.evidenceLabel.isNullOrBlank()) value += 2
            if (item.type != MpesaTransactionType.OTHER.name) value += 2
            if (item.direction != MoneyDirection.NEUTRAL.name) value += 1
            return value
        }

        val firstScore = score(first)
        val secondScore = score(second)
        return when {
            firstScore > secondScore -> first
            secondScore > firstScore -> second
            else -> if (first.id <= second.id) first else second
        }
    }

    private fun buildPossibleDuplicateGroups(
        transactions: List<StoredTransaction>
    ): List<PossibleDuplicateGroup> {
        val resolvedKeys = duplicateResolutionKeys()
        val eligible = transactions
            .filter { item ->
                item.reviewState != ReviewState.EXCLUDED.name &&
                    item.reviewState != ReviewState.USER_CLASSIFIED.name
            }
            .sortedByDescending(StoredTransaction::receivedAtEpochMs)

        val claimedIds = mutableSetOf<Long>()
        val groups = mutableListOf<PossibleDuplicateGroup>()

        eligible.forEachIndexed { index, first ->
            if (
                first.id in claimedIds
            ) {
                return@forEachIndexed
            }

            val firstObservation =
                first.toPossibleDuplicateObservation()
            var best:
                Pair<StoredTransaction, Long>? =
                null

            // The policy rejects anything farther than 90 seconds apart.
            // Because eligible is time-sorted, stop as soon as that bound is
            // crossed instead of comparing this row with the whole ledger.
            for (
                candidateIndex in
                (index + 1) until
                    eligible.size
            ) {
                val second =
                    eligible[candidateIndex]
                val delta =
                    first.receivedAtEpochMs -
                        second.receivedAtEpochMs
                if (
                    delta >
                    PossibleDuplicatePolicy
                        .MAX_CANDIDATE_WINDOW_MS
                ) {
                    break
                }
                if (
                    second.id in claimedIds
                ) {
                    continue
                }

                val key =
                    duplicatePairKey(
                        first.id,
                        second.id
                    )
                if (key in resolvedKeys) {
                    continue
                }
                if (
                    !PossibleDuplicatePolicy
                        .isCandidate(
                            firstObservation,
                            second
                                .toPossibleDuplicateObservation()
                        )
                ) {
                    continue
                }

                val absoluteDelta =
                    kotlin.math.abs(delta)
                if (
                    best == null ||
                    absoluteDelta <
                    best!!.second
                ) {
                    best =
                        second to
                            absoluteDelta
                    if (
                        absoluteDelta == 0L
                    ) {
                        break
                    }
                }
            }

            val selected =
                best
                    ?: return@forEachIndexed
            val second =
                selected.first
            val key =
                duplicatePairKey(
                    first.id,
                    second.id
                )
            groups +=
                PossibleDuplicateGroup(
                    key = key,
                    first = first,
                    second = second,
                    timeDeltaMs =
                        selected.second
                )
            claimedIds += first.id
            claimedIds += second.id
        }

        return groups.sortedByDescending {
            maxOf(
                it.first.receivedAtEpochMs,
                it.second.receivedAtEpochMs
            )
        }
    }

    private fun duplicateResolutionKeys(): Set<String> =
        database.readableDatabase.rawQuery(
            "SELECT pair_key FROM duplicate_resolutions",
            null
        ).use { cursor ->
            buildSet {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0))
                }
            }
        }

    private fun duplicatePairKey(
        firstId: Long,
        secondId: Long
    ): String =
        if (firstId <= secondId) {
            firstId.toString() + ":" + secondId
        } else {
            secondId.toString() + ":" + firstId
        }

    fun applyRemoteEntityEnrichmentByDescriptor(
        sanitizedDescriptor: String,
        result: RemoteEntityEnrichment
    ): Int {
        synchronized(CanonicalStoreLock.monitor) {
        if (
            !RemoteEntityAutoReviewPolicy
                .shouldAutoReview(result)
        ) {
            return 0
        }

        val descriptorKey =
            EntityDescriptorPrivacyPolicy
                .cacheKey(
                    sanitizedDescriptor
                )
        if (descriptorKey.isBlank()) {
            return 0
        }

        val category =
            result.categoryHint
                ?: return 0
        val db =
            database.writableDatabase
        var changed = 0

        allTransactions().forEach { current ->
            if (
                current.ruleId != null ||
                current.reviewState ==
                    ReviewState.USER_CLASSIFIED.name ||
                current.reviewState ==
                    ReviewState.EXCLUDED.name ||
                !RemoteEntityEligibilityPolicy
                    .canSendOnline(
                        current.type
                    ) ||
                current.direction !=
                    MoneyDirection.EXPENSE.name ||
                !ReviewTriagePolicy
                    .shouldTriageCategory(
                        current.category
                    )
            ) {
                return@forEach
            }

            val candidate =
                EntityDescriptorPrivacyPolicy
                    .sanitize(
                        current.counterparty
                    )
                    ?: return@forEach
            if (
                EntityDescriptorPrivacyPolicy
                    .cacheKey(candidate) !=
                descriptorKey
            ) {
                return@forEach
            }

            val values =
                ContentValues().apply {
                    put(
                        "counterparty",
                        result.canonicalName
                    )
                    put(
                        "direction",
                        MoneyDirection.EXPENSE.name
                    )
                    put(
                        "category",
                        category.name
                    )
                    put(
                        "review_state",
                        ReviewState.AUTO_CLASSIFIED.name
                    )
                    putNull("rule_id")
                    put(
                        "evidence_label",
                        result.canonicalName
                    )
                    put(
                        "evidence_source",
                        "entity_intelligence:" +
                            result.source
                    )
                    put(
                        "evidence_confidence",
                        EvidenceConfidence.HIGH.name
                    )
                    putNull(
                        "evidence_time_delta_ms"
                    )
                    putNull(
                        "cycle_candidate_reason"
                    )
                }

            changed +=
                db.update(
                    "transactions",
                    values,
                    "id = ?",
                    arrayOf(
                        current.id.toString()
                    )
                )
        }

        if (changed > 0) {
            PayCycleStore.get(appContext)
                .rebuild()
            LedgerUpdateBus.notifyChanged()
        }

        return changed
    }
    }

    fun applyRemoteEntityEnrichment(
        transactionId: Long,
        result: RemoteEntityEnrichment
    ): Boolean {
        synchronized(CanonicalStoreLock.monitor) {
        val current =
            getById(transactionId)
                ?: return false

        if (
            current.ruleId != null ||
            current.reviewState ==
                ReviewState.USER_CLASSIFIED.name ||
            current.reviewState ==
                ReviewState.EXCLUDED.name ||
            !RemoteEntityEligibilityPolicy
                .canSendOnline(
                    current.type
                ) ||
            current.direction !=
                MoneyDirection.EXPENSE.name ||
            !ReviewTriagePolicy
                .shouldTriageCategory(
                    current.category
                ) ||
            !RemoteEntityAutoReviewPolicy
                .shouldAutoReview(result)
        ) {
            return false
        }

        val category =
            result.categoryHint
                ?: return false

        val values = ContentValues().apply {
            put(
                "counterparty",
                result.canonicalName
            )
            put(
                "direction",
                MoneyDirection.EXPENSE.name
            )
            put("category", category.name)
            put(
                "review_state",
                ReviewState.AUTO_CLASSIFIED.name
            )
            putNull("rule_id")
            put(
                "evidence_label",
                result.canonicalName
            )
            put(
                "evidence_source",
                "entity_intelligence:" +
                    result.source
            )
            put(
                "evidence_confidence",
                EvidenceConfidence.HIGH.name
            )
            putNull("evidence_time_delta_ms")
        }

        val changed =
            database.writableDatabase.update(
                "transactions",
                values,
                "id = ?",
                arrayOf(
                    transactionId.toString()
                )
            ) > 0

        if (changed) {
            PayCycleStore.get(appContext)
                .rebuild()
            LedgerUpdateBus.notifyChanged()
        }

        return changed
    }
    }

    private fun scheduleRemoteEntityEnrichment(
        transactions: List<StoredTransaction>
    ) {
        transactions.asSequence()
            .filter { item ->
                item.ruleId == null &&
                    item.reviewState !=
                        ReviewState.USER_CLASSIFIED.name &&
                    item.reviewState !=
                        ReviewState.EXCLUDED.name &&
                    item.direction ==
                        MoneyDirection.EXPENSE.name &&
                    RemoteEntityEligibilityPolicy
                        .canSendOnline(
                            item.type
                        ) &&
                    ReviewTriagePolicy
                        .shouldTriageCategory(
                            item.category
                        ) &&
                    !item.counterparty
                        .isNullOrBlank()
            }
            .sortedByDescending {
                it.occurredAtEpochMs
            }
            .take(50)
            .forEach { item ->
                EntityEnrichmentCoordinator
                    .get(appContext)
                    .enrichTransactionAsync(
                        transactionId = item.id,
                        rawDescriptor =
                            item.counterparty
                    )
            }
    }

    fun classifyManually(
        id: Long,
        category: TransactionCategory,
        direction: MoneyDirection,
        planningMonth: String? = null,
        markTransfer: Boolean = false
    ) = synchronized(CanonicalStoreLock.monitor) { classifyManually(
        id = id,
        categoryId = category.name,
        direction = direction,
        planningMonth = planningMonth,
        markTransfer = markTransfer
    )
    }

    fun classifyManually(
        id: Long,
        categoryId: String,
        direction: MoneyDirection,
        planningMonth: String? = null,
        markTransfer: Boolean = false
    ) {
        synchronized(CanonicalStoreLock.monitor) {
        val canonicalCategory = CategoryStore
            .get(appContext)
            .canonicalId(categoryId)
        val values = ContentValues().apply {
            put(
                "category",
                if (markTransfer) {
                    TransactionCategory.TRANSFER.name
                } else {
                    canonicalCategory
                }
            )
            put(
                "direction",
                if (markTransfer) {
                    MoneyDirection.TRANSFER.name
                } else {
                    direction.name
                }
            )
            put("review_state", ReviewState.USER_CLASSIFIED.name)
            if (planningMonth != null) {
                put("planning_month", planningMonth)
            }
        }
        database.writableDatabase.update(
            "transactions",
            values,
            "id = ?",
            arrayOf(id.toString())
        )
        PersonalLearningStore.get(appContext)
            .clearApplication(id)
        PayCycleStore.get(appContext).rebuild()
        LedgerUpdateBus.notifyChanged()
    }
    }

    fun markInvestmentSettlement(
        transactionId: Long,
        action: SemanticInvestmentAction,
        instrumentLabel: String,
        userConfirmed: Boolean = false
    ): Boolean {
        synchronized(CanonicalStoreLock.monitor) {
        val current =
            getById(transactionId)
                ?: return false
        if (
            current.reviewState ==
                ReviewState.EXCLUDED.name ||
            current.reviewState ==
                ReviewState.USER_CLASSIFIED.name ||
            current.ruleId != null ||
            current.relationType != null
        ) {
            return false
        }

        val expectedDirection =
            when (action) {
                SemanticInvestmentAction.BUY ->
                    MoneyDirection.EXPENSE
                SemanticInvestmentAction.SELL ->
                    MoneyDirection.INCOME
            }
        if (
            current.direction !=
                expectedDirection.name
        ) {
            return false
        }

        val relation =
            when (action) {
                SemanticInvestmentAction.BUY ->
                    RELATION_INVESTMENT_BUY
                SemanticInvestmentAction.SELL ->
                    RELATION_INVESTMENT_SELL
            }
        val label =
            when (action) {
                SemanticInvestmentAction.BUY ->
                    "Investment purchase"
                SemanticInvestmentAction.SELL ->
                    "Investment sale"
            }

        val values = ContentValues().apply {
            put(
                "direction",
                MoneyDirection.TRANSFER.name
            )
            put(
                "category",
                TransactionCategory.INVESTMENT.name
            )
            put(
                "review_state",
                ReviewState.AUTO_CLASSIFIED.name
            )
            put("relation_type", relation)
            putNull("related_transaction_id")
            put(
                "evidence_label",
                "$label • $instrumentLabel"
            )
            put(
                "evidence_source",
                if (userConfirmed) {
                    "investment_user_confirmed"
                } else {
                    "investment_shared_reference"
                }
            )
            put(
                "evidence_confidence",
                if (userConfirmed) {
                    "USER_CONFIRMED"
                } else {
                    "HIGH"
                }
            )
            putNull("evidence_time_delta_ms")
            putNull("cycle_candidate_reason")
        }

        val changed =
            database.writableDatabase.update(
                "transactions",
                values,
                "id = ?",
                arrayOf(transactionId.toString())
            ) > 0

        if (changed) {
            PayCycleStore.get(appContext).rebuild()
            LedgerUpdateBus.notifyChanged()
        }
        return changed
    }
    }

    fun reconcileAutoTransfersWithOwnedAccounts(
        ownedAccountIds: Set<String>
    ): Int {
        synchronized(CanonicalStoreLock.monitor) {
        val protectedOwnedIds =
            ownedAccountIds +
                AccountStore.DEFAULT_MPESA_ID +
                AccountStore.DEFAULT_CASH_ID

        val candidates =
            database.readableDatabase
                .rawQuery(
                    BASE_SELECT +
                        """
                         WHERE direction = ?
                           AND category = ?
                           AND rule_id IS NULL
                           AND review_state NOT IN (?, ?)
                        """.trimIndent(),
                    arrayOf(
                        MoneyDirection.TRANSFER.name,
                        TransactionCategory.TRANSFER.name,
                        ReviewState.USER_CLASSIFIED.name,
                        ReviewState.EXCLUDED.name
                    )
                )
                .use { cursor ->
                    buildList {
                        while (
                            cursor.moveToNext()
                        ) {
                            add(
                                cursor
                                    .toStoredTransaction()
                            )
                        }
                    }
                }

        var changed = 0
        candidates.forEach { row ->
            val sourceOwned =
                row.accountId != null &&
                    row.accountId in
                        protectedOwnedIds
            val destinationOwned =
                row.linkedAccountId != null &&
                    row.linkedAccountId in
                        protectedOwnedIds

            if (
                sourceOwned &&
                destinationOwned
            ) {
                return@forEach
            }

            val values =
                ContentValues().apply {
                    when {
                        sourceOwned -> {
                            // Money left one of my accounts for an external
                            // destination. It is spending, not an own-account
                            // transfer.
                            put(
                                "direction",
                                MoneyDirection
                                    .EXPENSE.name
                            )
                            put(
                                "category",
                                TransactionCategory
                                    .UNCATEGORIZED.name
                            )
                            putNull(
                                "linked_account_id"
                            )
                            put(
                                "review_state",
                                ReviewState
                                    .AUTO_CLASSIFIED.name
                            )
                        }

                        destinationOwned -> {
                            // Money arrived in one of my accounts from an
                            // external account. Keep the owned destination as
                            // the transaction's account and treat it as income.
                            put(
                                "direction",
                                MoneyDirection
                                    .INCOME.name
                            )
                            put(
                                "category",
                                TransactionCategory
                                    .UNCATEGORIZED.name
                            )
                            put(
                                "account_id",
                                row.linkedAccountId
                            )
                            putNull(
                                "linked_account_id"
                            )
                            put(
                                "review_state",
                                ReviewState
                                    .AUTO_CLASSIFIED.name
                            )
                        }

                        else -> {
                            put(
                                "direction",
                                MoneyDirection
                                    .NEUTRAL.name
                            )
                            put(
                                "category",
                                TransactionCategory
                                    .UNCATEGORIZED.name
                            )
                            putNull("account_id")
                            putNull(
                                "linked_account_id"
                            )
                            put(
                                "review_state",
                                ReviewState
                                    .NEEDS_REVIEW.name
                            )
                        }
                    }
                }

            changed +=
                database.writableDatabase
                    .update(
                        "transactions",
                        values,
                        "id = ?",
                        arrayOf(
                            row.id.toString()
                        )
                    )
        }

        if (changed > 0) {
            LedgerUpdateBus
                .notifyChanged()
        }
        return changed
    }
    }

    fun markDebtRepayment(
        transactionId: Long,
        debtEventId: Long,
        debtName: String,
        userConfirmed: Boolean
    ): Boolean {
        synchronized(CanonicalStoreLock.monitor) {
        val current =
            getById(transactionId)
                ?: return false
        if (
            current.reviewState ==
                ReviewState.EXCLUDED.name
        ) {
            return false
        }
        if (
            !userConfirmed &&
            (
                current.reviewState ==
                    ReviewState.USER_CLASSIFIED.name ||
                    current.ruleId != null
                )
        ) {
            return false
        }

        val values = ContentValues().apply {
            put(
                "direction",
                MoneyDirection.TRANSFER.name
            )
            put(
                "category",
                TransactionCategory.DEBT_LOAN.name
            )
            put(
                "review_state",
                if (userConfirmed) {
                    ReviewState.USER_CLASSIFIED.name
                } else {
                    ReviewState.AUTO_CLASSIFIED.name
                }
            )
            put(
                "relation_type",
                RELATION_DEBT_REPAYMENT
            )
            putNull("related_transaction_id")
            put(
                "evidence_label",
                "Debt repayment • $debtName"
            )
            put(
                "evidence_source",
                if (userConfirmed) {
                    "liability_payment_user"
                } else {
                    "liability_payment_match"
                }
            )
            put(
                "evidence_confidence",
                if (userConfirmed) {
                    "USER_CONFIRMED"
                } else {
                    "HIGH"
                }
            )
            putNull("evidence_time_delta_ms")
            putNull("cycle_candidate_reason")
        }

        val changed =
            database.writableDatabase.update(
                "transactions",
                values,
                "id = ?",
                arrayOf(transactionId.toString())
            ) > 0

        if (changed) {
            PayCycleStore.get(appContext).rebuild()
            LedgerUpdateBus.notifyChanged()
        }
        return changed
    }
    }

    fun unlinkRefund(id: Long): Boolean {
        synchronized(CanonicalStoreLock.monitor) {
        val current = getById(id) ?: return false
        if (current.relationType != RELATION_REFUND) {
            return false
        }

        val values = ContentValues().apply {
            putNull("relation_type")
            putNull("related_transaction_id")
            put("direction", MoneyDirection.INCOME.name)
            put("category", TransactionCategory.UNCATEGORIZED.name)
            put("review_state", ReviewState.NEEDS_REVIEW.name)
            if (current.evidenceSource in setOf("refund_match", "refund_explicit_match")) {
                putNull("evidence_label")
                putNull("evidence_source")
                putNull("evidence_confidence")
                putNull("evidence_time_delta_ms")
            }
        }

        val changed = database.writableDatabase.update(
            "transactions",
            values,
            "id = ?",
            arrayOf(id.toString())
        ) > 0

        if (changed) {
            PayCycleStore.get(appContext).rebuild()
            LedgerUpdateBus.notifyChanged()
        }
        return changed
    }
    }

    fun excludeFromBudget(id: Long) {
        synchronized(CanonicalStoreLock.monitor) {
        val values = ContentValues().apply {
            put("review_state", ReviewState.EXCLUDED.name)
        }
        database.writableDatabase.update(
            "transactions",
            values,
            "id = ?",
            arrayOf(id.toString())
        )
        PayCycleStore.get(appContext).rebuild()
    }
    }

    private fun allTransactions(): List<StoredTransaction> =
        database.readableDatabase.rawQuery(
            BASE_SELECT + " ORDER BY occurred_at_epoch_ms DESC, id DESC",
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toStoredTransaction())
            }
        }

    private fun transactionsBetween(
        startEpochMs: Long,
        endEpochMsExclusive: Long
    ): List<StoredTransaction> =
        database.readableDatabase.rawQuery(
            BASE_SELECT +
                " WHERE occurred_at_epoch_ms >= ? AND occurred_at_epoch_ms < ?" +
                " ORDER BY occurred_at_epoch_ms DESC, id DESC",
            arrayOf(startEpochMs.toString(), endEpochMsExclusive.toString())
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toStoredTransaction())
            }
        }

    private fun getByFingerprint(
        fingerprint: String
    ): StoredTransaction? =
        database.readableDatabase
            .rawQuery(
                BASE_SELECT +
                    " WHERE fingerprint = ? LIMIT 1",
                arrayOf(fingerprint)
            )
            .use { cursor ->
                if (
                    cursor.moveToFirst()
                ) {
                    cursor.toStoredTransaction()
                } else {
                    null
                }
            }

    private fun hasLegacyFingerprintAt(
        fingerprint: String,
        occurredAtEpochMs: Long
    ): Boolean =
        database.readableDatabase.rawQuery(
            """
            SELECT 1
            FROM transactions
            WHERE fingerprint = ?
              AND occurred_at_epoch_ms = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(fingerprint, occurredAtEpochMs.toString())
        ).use { cursor ->
            cursor.moveToFirst()
        }

    private fun reversedTransactionCodes(): Set<String> =
        database.readableDatabase.rawQuery(
            "SELECT reversal_of_code FROM transactions WHERE reversal_of_code IS NOT NULL",
            null
        ).use { cursor ->
            buildSet {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }

    private fun classify(
        parsed: ParsedMpesaTransaction,
        matchedRule: SmartRule?,
        occurredAtEpochMs: Long
    ): Classification {
        if (matchedRule != null) {
            return Classification(
                direction = matchedRule.direction,
                category = matchedRule.category,
                reviewState = ReviewState.AUTO_CLASSIFIED,
                ruleId = matchedRule.id,
                planningMonth = PlanningPeriodResolver.resolve(
                    observedAtEpochMs = occurredAtEpochMs,
                    monthOffset = matchedRule.planningMonthOffset
                )
            )
        }

        KenyanTransactionRecognizer.classify(parsed)?.let { recognized ->
            return Classification(
                direction = recognized.direction,
                category = recognized.category,
                reviewState = ReviewState.AUTO_CLASSIFIED,
                ruleId = null,
                planningMonth = PlanningPeriodResolver.resolve(
                    occurredAtEpochMs,
                    0
                ),
                displayLabel =
                    recognized.entity
                        ?.entity
                        ?.canonicalName,
                evidenceLabel =
                    recognized.evidenceLabel,
                evidenceSource =
                    recognized.evidenceSource,
                evidenceConfidence =
                    recognized.evidenceConfidence
            )
        }

        cachedRemoteClassification(
            parsed = parsed,
            occurredAtEpochMs = occurredAtEpochMs
        )?.let {
            return it
        }

        val fallback = when (parsed.type) {
            MpesaTransactionType.WITHDRAWAL -> Triple(
                MoneyDirection.TRANSFER,
                TransactionCategory.CASH,
                ReviewState.AUTO_CLASSIFIED
            )
            MpesaTransactionType.AIRTIME -> Triple(
                MoneyDirection.EXPENSE,
                TransactionCategory.PHONE_AND_DATA,
                ReviewState.AUTO_CLASSIFIED
            )
            MpesaTransactionType.REVERSAL -> Triple(
                MoneyDirection.NEUTRAL,
                TransactionCategory.OTHER,
                if (parsed.reversalOfCode != null) ReviewState.AUTO_CLASSIFIED else ReviewState.NEEDS_REVIEW
            )
            MpesaTransactionType.RECEIVED,
            MpesaTransactionType.DEPOSIT -> Triple(
                MoneyDirection.INCOME,
                TransactionCategory.UNCATEGORIZED,
                ReviewState.AUTO_CLASSIFIED
            )
            MpesaTransactionType.SENT_MONEY,
            MpesaTransactionType.PAY_BILL,
            MpesaTransactionType.BUY_GOODS -> Triple(
                MoneyDirection.EXPENSE,
                TransactionCategory.UNCATEGORIZED,
                ReviewState.AUTO_CLASSIFIED
            )
            MpesaTransactionType.OTHER -> Triple(
                MoneyDirection.NEUTRAL,
                TransactionCategory.UNCATEGORIZED,
                ReviewState.NEEDS_REVIEW
            )
        }

        return Classification(
            direction = fallback.first,
            category = fallback.second,
            reviewState = fallback.third,
            ruleId = null,
            planningMonth = PlanningPeriodResolver.resolve(occurredAtEpochMs, 0)
        )
    }

    private fun cachedRemoteClassification(
        parsed: ParsedMpesaTransaction,
        occurredAtEpochMs: Long
    ): Classification? {
        if (
            !RemoteEntityEligibilityPolicy
                .canSendOnline(
                    parsed.type.name
                )
        ) {
            return null
        }

        val sanitized =
            EntityDescriptorPrivacyPolicy
                .sanitize(
                    parsed.counterparty
                )
                ?: return null
        val key =
            EntityDescriptorPrivacyPolicy
                .cacheKey(sanitized)
        val cached =
            EntityEnrichmentCacheStore
                .get(appContext)
                .get(key)
                ?: return null

        if (
            !RemoteEntityAutoReviewPolicy
                .shouldAutoReview(cached)
        ) {
            return null
        }

        val category =
            cached.categoryHint
                ?: return null

        return Classification(
            direction =
                MoneyDirection.EXPENSE,
            category = category,
            reviewState =
                ReviewState.AUTO_CLASSIFIED,
            ruleId = null,
            planningMonth =
                PlanningPeriodResolver.resolve(
                    occurredAtEpochMs,
                    0
                ),
            displayLabel =
                cached.canonicalName,
            evidenceLabel =
                cached.canonicalName,
            evidenceSource =
                "entity_intelligence:" +
                    cached.source,
            evidenceConfidence =
                EvidenceConfidence.HIGH
        )
    }

    private data class Classification(
        val direction: MoneyDirection,
        val category: TransactionCategory,
        val reviewState: ReviewState,
        val ruleId: String?,
        val planningMonth: String,
        val displayLabel: String? = null,
        val evidenceLabel: String? = null,
        val evidenceSource: String? = null,
        val evidenceConfidence:
            EvidenceConfidence? = null
    )

    companion object {
        private const val MIRROR_RECONCILIATION_KEY =
            "mirror_transaction_reconciliation_v3"
        private const val BUILT_IN_RECOGNITION_RECONCILIATION_KEY =
            "built_in_recognition_reconciliation_v2"
        private const val SEMANTIC_REVIEW_RECONCILIATION_KEY =
            "semantic_review_reconciliation_v1"
        private const val REFUND_RECONCILIATION_KEY =
            "refund_reconciliation_v2"
        private val EXPLICIT_COMPLETED_REFUND = Regex(
            """\b(?:payment|amount|money)\s+(?:(?:has\s+been|was|is)\s+)?(?:refunded|returned)\b|\brefund\s+(?:of|for)\b[^.!?]*\b(?:credited|received|completed)\b""",
            RegexOption.IGNORE_CASE
        )
        const val RELATION_REFUND = "REFUND"
        const val RELATION_DEBT_REPAYMENT =
            "DEBT_REPAYMENT"
        const val RELATION_INVESTMENT_BUY =
            "INVESTMENT_BUY"
        const val RELATION_INVESTMENT_SELL =
            "INVESTMENT_SELL"
        private const val DUPLICATE_DECISION_SAME = "SAME_TRANSACTION"
        private const val DUPLICATE_DECISION_KEEP_BOTH = "KEEP_BOTH"
        private const val REVIEW_ENRICHMENT_RECENT_LIMIT =
            500

        private val OUTGOING_REVIEW_TYPES = setOf(
            MpesaTransactionType.SENT_MONEY.name,
            MpesaTransactionType.PAY_BILL.name,
            MpesaTransactionType.BUY_GOODS.name
        )

        private const val BASE_SELECT = """
            SELECT id, transaction_code, fingerprint, type, amount_minor, fee_minor,
                   balance_minor, counterparty, counterparty_phone, occurred_at_text,
                   occurred_at_epoch_ms, received_at_epoch_ms, source_package, source_kind,
                   direction, category, review_state, rule_id, planning_month, reversal_of_code,
                   account_id, linked_account_id, pay_cycle_id, cycle_candidate_reason,
                   cycle_override, fuliza_financed_minor, fuliza_access_fee_minor,
                   fuliza_outstanding_minor, fuliza_event_code, evidence_label,
                   evidence_source, evidence_confidence, evidence_time_delta_ms,
                   relation_type, related_transaction_id
            FROM transactions
        """

        @Volatile
        private var instance: TransactionStore? = null

        fun get(context: Context): TransactionStore =
            instance ?: synchronized(this) {
                instance ?: TransactionStore(context.applicationContext).also { instance = it }
            }
    }
}

private fun android.database.Cursor.toStoredTransaction(): StoredTransaction =
    StoredTransaction(
        id = getLong(0),
        transactionCode = getStringOrNull(1),
        fingerprint = getString(2),
        type = getString(3),
        amountMinor = getLong(4),
        feeMinor = getLong(5),
        balanceMinor = getLongOrNull(6),
        counterparty = getStringOrNull(7),
        counterpartyPhone = getStringOrNull(8),
        occurredAtText = getStringOrNull(9),
        occurredAtEpochMs = getLong(10),
        receivedAtEpochMs = getLong(11),
        sourcePackage = getStringOrNull(12),
        sourceKind = getString(13),
        direction = getString(14),
        category = getString(15),
        reviewState = getString(16),
        ruleId = getStringOrNull(17),
        planningMonth = getStringOrNull(18),
        reversalOfCode = getStringOrNull(19),
        accountId = getStringOrNull(20),
        linkedAccountId = getStringOrNull(21),
        payCycleId = getStringOrNull(22),
        cycleCandidateReason = getStringOrNull(23),
        cycleOverride = getStringOrNull(24),
        fulizaFinancedMinor = getLong(25),
        fulizaAccessFeeMinor = getLong(26),
        fulizaOutstandingMinor = getLongOrNull(27),
        fulizaEventCode = getStringOrNull(28),
        evidenceLabel = getStringOrNull(29),
        evidenceSource = getStringOrNull(30),
        evidenceConfidence = getStringOrNull(31),
        evidenceTimeDeltaMs = getLongOrNull(32),
        relationType = getStringOrNull(33),
        relatedTransactionId = getLongOrNull(34)
    )

private fun android.database.Cursor.getStringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)

private fun android.database.Cursor.getLongOrNull(index: Int): Long? =
    if (isNull(index)) null else getLong(index)

private fun StoredTransaction.toMirrorObservation(
    externalReferences: Set<String> = emptySet()
): MirrorTransactionObservation =
    MirrorTransactionObservation(
        amountMinor = amountMinor,
        occurredAtEpochMs = occurredAtEpochMs,
        receivedAtEpochMs = receivedAtEpochMs,
        sourceKind = sourceKind,
        sourcePackage = sourcePackage,
        transactionCode = transactionCode,
        type = type,
        counterparty = counterparty,
        counterpartyPhone = counterpartyPhone,
        direction = direction,
        externalReferences = externalReferences,
        accountIds =
            listOfNotNull(
                accountId,
                linkedAccountId
            ).toSet()
    )

private fun StoredTransaction.toPossibleDuplicateObservation():
    PossibleDuplicateObservation =
    PossibleDuplicateObservation(
        id = id,
        amountMinor = amountMinor,
        receivedAtEpochMs = receivedAtEpochMs,
        transactionCode = transactionCode,
        sourcePackage = sourcePackage,
        sourceKind = sourceKind,
        direction = direction,
        reviewState = reviewState,
        category = category,
        counterparty = counterparty,
        evidenceLabel = evidenceLabel,
        type = type
    )

private fun StoredTransaction.toParsedTransaction(): ParsedMpesaTransaction =
    ParsedMpesaTransaction(
        transactionCode = transactionCode,
        type = runCatching { MpesaTransactionType.valueOf(type) }
            .getOrDefault(MpesaTransactionType.OTHER),
        amountMinor = amountMinor,
        counterparty = counterparty,
        counterpartyPhone = counterpartyPhone,
        feeMinor = feeMinor,
        balanceMinor = balanceMinor,
        occurredAtText = occurredAtText,
        rawMessage = "",
        reversalOfCode = reversalOfCode
    )




