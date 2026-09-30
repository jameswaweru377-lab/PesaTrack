package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.ingestion.LegacyTransactionTypeCompatibility
import com.kedevjimmy.pesatrack.domain.intelligence.EntityDescriptorPrivacyPolicy
import com.kedevjimmy.pesatrack.domain.intelligence.RemoteEntityAutoReviewPolicy
import com.kedevjimmy.pesatrack.domain.intelligence.RemoteEntityEligibilityPolicy
import com.kedevjimmy.pesatrack.domain.intelligence.RemoteEntityEnrichment
import com.kedevjimmy.pesatrack.domain.ingestion.TransactionFingerprint
import com.kedevjimmy.pesatrack.domain.interpretation.CanonicalFinancialRecordKind
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentRouteHint
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

class TransactionStore private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val database = PesaTrackDatabase(appContext)

    /**
     * Provider-neutral entry point for financial movements that were
     * already understood by FinancialMessageInterpreter. The old
     * ParsedMpesaTransaction shape is now confined to this storage
     * compatibility boundary while the legacy transactions table is
     * migrated incrementally.
     */
    @Synchronized
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
        externalReferences: Set<String> = emptySet()
    ): Boolean {
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
            externalReferences =
                externalReferences
        )
    }

    @Synchronized
    fun insertIfNew(
        parsed: ParsedMpesaTransaction,
        sourcePackage: String?,
        sourceKind: String,
        receivedAtEpochMs: Long,
        occurredAtEpochMs: Long = receivedAtEpochMs,
        matchedRule: SmartRule? = null,
        rebuildPayCycles: Boolean = true,
        evidence: TransactionEvidenceMatch? = null,
        accountTransferOverride:
            com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferMatch? = null,
        accountIdOverride: String? = null,
        allowLegacyAccountFallback: Boolean = true,
        externalReferences: Set<String> = emptySet()
    ): Boolean {
        val fingerprint = TransactionFingerprint.of(parsed, occurredAtEpochMs)
        if (parsed.transactionCode.isNullOrBlank()) {
            val legacyFingerprint = TransactionFingerprint.of(parsed)
            if (
                legacyFingerprint != fingerprint &&
                hasLegacyFingerprintAt(legacyFingerprint, occurredAtEpochMs)
            ) {
                return false
            }
        } else if (evidence != null) {
            val existing = getByTransactionCode(parsed.transactionCode)
            if (existing != null) {
                updateExistingDuplicateWithEvidence(
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
            classify(parsed, matchedRule, occurredAtEpochMs)
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

        val mirrorCandidate = findMirrorCandidate(
            parsed = parsed,
            sourcePackage = sourcePackage,
            sourceKind = sourceKind,
            receivedAtEpochMs = receivedAtEpochMs,
            occurredAtEpochMs = occurredAtEpochMs,
            direction = classification.direction.name,
            externalReferences =
                correlationReferences,
            accountIds =
                correlationAccountIds
        )
        if (mirrorCandidate != null) {
            attachExternalReferences(
                transactionId = mirrorCandidate.id,
                references = correlationReferences,
                sourceKind = sourceKind,
                observedAtEpochMs = receivedAtEpochMs
            )

            if (parsed.transactionCode.isNullOrBlank()) {
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
            DebtStore.get(appContext)
                .reconcilePendingDrawdownsForTransaction(
                    transactionId = mirrorCandidate.id,
                    parsed = parsed,
                    occurredAtEpochMs = occurredAtEpochMs
                )
            if (
                parsed.type == MpesaTransactionType.RECEIVED ||
                parsed.type == MpesaTransactionType.DEPOSIT
            ) {
                DebtStore.get(appContext)
                    .reconcilePendingRepaymentsAround(
                        occurredAtEpochMs =
                            occurredAtEpochMs
                    )
                if (matchedRule == null) {
                    tryLinkRefund(mirrorCandidate.id)
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

        val rowId = database.writableDatabase.insertWithOnConflict(
            "transactions",
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )

        val inserted = rowId != -1L
        if (inserted) {
            attachExternalReferences(
                transactionId = rowId,
                references = correlationReferences,
                sourceKind = sourceKind,
                observedAtEpochMs = receivedAtEpochMs
            )
            DebtStore.get(appContext)
                .reconcilePendingDrawdownsForTransaction(
                    transactionId = rowId,
                    parsed = parsed,
                    occurredAtEpochMs = occurredAtEpochMs
                )
            if (
                parsed.type == MpesaTransactionType.RECEIVED ||
                parsed.type == MpesaTransactionType.DEPOSIT
            ) {
                DebtStore.get(appContext)
                    .reconcilePendingRepaymentsAround(
                        occurredAtEpochMs =
                            occurredAtEpochMs
                    )
                if (matchedRule == null) {
                    tryLinkRefund(rowId)
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

    @Synchronized
    fun insertManualIncome(
        amountMinor: Long,
        occurredAtEpochMs: Long,
        label: String,
        matchedRule: SmartRule
    ): Boolean {
        require(amountMinor > 0L) { "Income amount must be greater than zero." }

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

        return insertIfNew(
            parsed = parsed,
            sourcePackage = null,
            sourceKind = "manual_setup",
            receivedAtEpochMs = System.currentTimeMillis(),
            occurredAtEpochMs = occurredAtEpochMs,
            matchedRule = matchedRule
        )
    }

    @Synchronized
    fun snapshot(): LedgerSnapshot {
        ensureMirrorReconciled()
        ensureBuiltInRecognitionReconciled()
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

    @Synchronized
    fun getById(id: Long): StoredTransaction? =
        database.readableDatabase.rawQuery(
            BASE_SELECT + " WHERE id = ? LIMIT 1",
            arrayOf(id.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toStoredTransaction() else null
        }

    @Synchronized
    fun findByTransactionCode(
        transactionCode: String
    ): StoredTransaction? =
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

    @Synchronized
    fun recent(limit: Int = 100): List<StoredTransaction> =
        database.readableDatabase.rawQuery(
            BASE_SELECT + " ORDER BY occurred_at_epoch_ms DESC, id DESC LIMIT ?",
            arrayOf(limit.coerceIn(1, 500).toString())
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toStoredTransaction())
            }
        }

    @Synchronized
    fun activityMonths(
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<ActivityMonthBucket> {
        val counts = linkedMapOf<Long, Int>()

        database.readableDatabase.rawQuery(
            "SELECT occurred_at_epoch_ms FROM transactions WHERE review_state != ? ORDER BY occurred_at_epoch_ms DESC, id DESC",
            arrayOf(ReviewState.EXCLUDED.name)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val monthStart = Calendar.getInstance(timeZone).apply {
                    timeInMillis = cursor.getLong(0)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                counts[monthStart] = (counts[monthStart] ?: 0) + 1
            }
        }

        return counts.map { (monthStart, count) ->
            ActivityMonthBucket(
                monthStartEpochMs = monthStart,
                transactionCount = count
            )
        }
    }

    @Synchronized
    fun activityReviewOverview(): ActivityReviewOverview {
        ensureMirrorReconciled()
        ensureBuiltInRecognitionReconciled()
        ensureRefundsReconciled()

        val transactions = allTransactions()
            .filter { it.reviewState != ReviewState.EXCLUDED.name }
        scheduleRemoteEntityEnrichment(
            transactions
        )
        val possibleDuplicates = buildPossibleDuplicateGroups(
            transactions
        )
        val possibleDuplicateIds = possibleDuplicates
            .flatMap { listOf(it.first.id, it.second.id) }
            .toSet()

        return ActivityReviewOverview(
            reviewGroups = buildReviewGroups(
                transactions.filterNot {
                    it.id in possibleDuplicateIds
                }
            ),
            possibleDuplicates = possibleDuplicates
        )
    }

    @Synchronized
    fun activitySnapshot(
        nowEpochMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): ActivityLedgerSnapshot {
        ensureMirrorReconciled()
        ensureBuiltInRecognitionReconciled()
        ensureRefundsReconciled()
        reconcileRecentReviewTriage(nowEpochMs, timeZone)

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
        val reversedCodes = reversedTransactionCodes()
        val monthTransactions = transactionsBetween(
            startEpochMs = monthStart.timeInMillis,
            endEpochMsExclusive = nextMonthStart.timeInMillis
        )
        val possibleDuplicates = buildPossibleDuplicateGroups(
            monthTransactions
        )
        val possibleDuplicateIds = possibleDuplicates
            .flatMap { listOf(it.first.id, it.second.id) }
            .toSet()

        val effective = monthTransactions.filter { item ->
            item.id !in possibleDuplicateIds &&
                item.transactionCode !in reversedCodes &&
                item.type != MpesaTransactionType.REVERSAL.name &&
                item.reviewState != ReviewState.EXCLUDED.name &&
                item.reviewState != ReviewState.NEEDS_REVIEW.name &&
                item.cycleCandidateReason == null
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
                startEpochMs = monthStart.timeInMillis,
                endEpochMsExclusive = nextMonthStart.timeInMillis
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
            possibleDuplicates = possibleDuplicates
        )
    }

    @Synchronized
    fun reconcileRecentReviewTriage(
        nowEpochMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ) {
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

    @Synchronized
    fun reconcileReviewTriageForMonth(
        epochMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Int {
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
                    if (oneOff) {
                        ReviewState.AUTO_CLASSIFIED.name
                    } else {
                        ReviewState.NEEDS_REVIEW.name
                    }
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
                val latest = items.maxByOrNull(StoredTransaction::occurredAtEpochMs)!!
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

    @Synchronized
    fun reapplyCounterpartyRules(rules: List<SmartRule>) {
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

    @Synchronized
    fun matchingPendingCount(
        rule: SmartRule,
        excludingId: Long? = null
    ): Int =
        learnableHistoricalTransactions().count { stored ->
            stored.id != excludingId &&
                SmartRuleEngine.matchTransaction(
                    stored.toParsedTransaction(),
                    listOf(rule)
                ) != null
        }

    @Synchronized
    fun reapplyRule(rule: SmartRule): Int {
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
            changed += db.update(
                "transactions",
                values,
                "id = ?",
                arrayOf(stored.id.toString())
            )
        }

        if (changed > 0) {
            PayCycleStore.get(appContext).rebuild()
        }
        return changed
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
                " OR (review_state = ? AND rule_id IS NULL AND category = ?)" +
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

    @Synchronized
    fun reconcileMirrorDuplicates(): Int {
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
            val canonicalObservation = canonical.toMirrorObservation()
            val candidate = duplicates
                .asSequence()
                .filter { it.id != canonical.id }
                .filter { it.id !in claimedDuplicateIds }
                .filter { it.amountMinor == canonical.amountMinor }
                .filter {
                    kotlin.math.abs(
                        it.receivedAtEpochMs - canonical.receivedAtEpochMs
                    ) <= MirrorTransactionMatcher.MAX_TIME_DELTA_MS
                }
                .filter {
                    MirrorTransactionMatcher.isMirror(
                        canonicalObservation,
                        it.toMirrorObservation()
                    )
                }
                .minByOrNull {
                    kotlin.math.abs(
                        it.receivedAtEpochMs - canonical.receivedAtEpochMs
                    )
                } ?: return@forEach

            claimedDuplicateIds += candidate.id

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

    private fun refundOriginalIsTrusted(
        refund: StoredTransaction
    ): Boolean {
        val originalId =
            refund.relatedTransactionId ?: return false
        val original = getById(originalId) ?: return false
        return original.direction == MoneyDirection.EXPENSE.name &&
            original.reviewState != ReviewState.EXCLUDED.name &&
            original.reviewState != ReviewState.NEEDS_REVIEW.name &&
            original.type != MpesaTransactionType.REVERSAL.name
    }

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

        var changed = false
        database.readableDatabase.rawQuery(
            """
            SELECT id
            FROM transactions
            WHERE type IN (?, ?)
              AND relation_type IS NULL
              AND rule_id IS NULL
              AND review_state != ?
              AND review_state != ?
            ORDER BY occurred_at_epoch_ms DESC, id DESC
            LIMIT 400
            """.trimIndent(),
            arrayOf(
                MpesaTransactionType.RECEIVED.name,
                MpesaTransactionType.DEPOSIT.name,
                ReviewState.USER_CLASSIFIED.name,
                ReviewState.EXCLUDED.name
            )
        ).use { cursor ->
            val ids = mutableListOf<Long>()
            while (cursor.moveToNext()) {
                ids += cursor.getLong(0)
            }
            ids.forEach { id ->
                changed = tryLinkRefund(id) || changed
            }
        }

        if (changed) {
            PayCycleStore.get(appContext).rebuild()
        }

        preferences.edit()
            .putBoolean(REFUND_RECONCILIATION_KEY, true)
            .apply()
    }

    private fun tryLinkRefund(
        transactionId: Long
    ): Boolean {
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
              AND review_state != ?
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
            candidates = candidates
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
            put("evidence_source", "refund_match")
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

    @Synchronized
    fun findByAnyExternalReference(
        references: Set<String>
    ): StoredTransaction? {
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
        observedAtEpochMs: Long
    ) {
        if (references.isEmpty()) {
            return
        }
        val db = database.writableDatabase
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
            put("transaction_code", parsed.transactionCode)
            put("fingerprint", fingerprint)
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

    @Synchronized
    fun attachEvidenceToNearbyUnknown(
        evidence: FinancialMessageEvidence,
        rules: List<SmartRule>
    ): Boolean {
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

    @Synchronized
    fun acceptEvidenceIdentity(id: Long): Boolean {
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

    @Synchronized
    fun reconcileOwnAccountTransfers(): Int {
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

        if (changed > 0) {
            PayCycleStore.get(appContext).rebuild()
        }
        return changed
    }

    @Synchronized
    fun resolvePossibleDuplicate(
        pairKey: String,
        sameTransaction: Boolean
    ): Boolean {
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
            if (first.id in claimedIds) return@forEachIndexed

            val firstObservation = first.toPossibleDuplicateObservation()
            val best = eligible
                .asSequence()
                .drop(index + 1)
                .filter { it.id !in claimedIds }
                .mapNotNull { second ->
                    val key = duplicatePairKey(first.id, second.id)
                    if (key in resolvedKeys) return@mapNotNull null
                    if (
                        !PossibleDuplicatePolicy.isCandidate(
                            firstObservation,
                            second.toPossibleDuplicateObservation()
                        )
                    ) {
                        return@mapNotNull null
                    }

                    second to kotlin.math.abs(
                        first.receivedAtEpochMs -
                            second.receivedAtEpochMs
                    )
                }
                .minByOrNull { it.second }
                ?: return@forEachIndexed

            val second = best.first
            val key = duplicatePairKey(first.id, second.id)
            groups += PossibleDuplicateGroup(
                key = key,
                first = first,
                second = second,
                timeDeltaMs = best.second
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

    @Synchronized
    fun applyRemoteEntityEnrichmentByDescriptor(
        sanitizedDescriptor: String,
        result: RemoteEntityEnrichment
    ): Int {
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

    @Synchronized
    fun applyRemoteEntityEnrichment(
        transactionId: Long,
        result: RemoteEntityEnrichment
    ): Boolean {
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

    @Synchronized
    fun classifyManually(
        id: Long,
        category: TransactionCategory,
        direction: MoneyDirection,
        planningMonth: String? = null,
        markTransfer: Boolean = false
    ) = classifyManually(
        id = id,
        categoryId = category.name,
        direction = direction,
        planningMonth = planningMonth,
        markTransfer = markTransfer
    )

    @Synchronized
    fun classifyManually(
        id: Long,
        categoryId: String,
        direction: MoneyDirection,
        planningMonth: String? = null,
        markTransfer: Boolean = false
    ) {
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
        PayCycleStore.get(appContext).rebuild()
        LedgerUpdateBus.notifyChanged()
    }

    @Synchronized
    fun unlinkRefund(id: Long): Boolean {
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
            if (current.evidenceSource == "refund_match") {
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

    @Synchronized
    fun excludeFromBudget(id: Long) {
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
                ReviewState.NEEDS_REVIEW
            )
            MpesaTransactionType.SENT_MONEY,
            MpesaTransactionType.PAY_BILL,
            MpesaTransactionType.BUY_GOODS -> Triple(
                MoneyDirection.EXPENSE,
                TransactionCategory.UNCATEGORIZED,
                ReviewState.NEEDS_REVIEW
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
            "mirror_transaction_reconciliation_v2"
        private const val BUILT_IN_RECOGNITION_RECONCILIATION_KEY =
            "built_in_recognition_reconciliation_v2"
        private const val REFUND_RECONCILIATION_KEY =
            "refund_reconciliation_v1"
        const val RELATION_REFUND = "REFUND"
        private const val DUPLICATE_DECISION_SAME = "SAME_TRANSACTION"
        private const val DUPLICATE_DECISION_KEEP_BOTH = "KEEP_BOTH"

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
