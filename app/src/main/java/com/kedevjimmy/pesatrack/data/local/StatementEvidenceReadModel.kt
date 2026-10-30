package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.statements.StatementActivityObservationPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementCrossSourceEventPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementEvidenceCorrelationPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementSourceCoveragePolicy

/**
 * Phase 3 read-only statement evidence boundary. It does not query or modify
 * Activity, and does not claim that unlinked rows form a combined cash total.
 */
class StatementEvidenceReadModel(context: Context) {
    private val app = context.applicationContext

    data class Snapshot(
        val observations: List<StatementEvidenceCorrelationPolicy.Observation>,
        val correlation: StatementEvidenceCorrelationPolicy.Result,
        val coverage: List<StatementSourceCoveragePolicy.MonthCoverage>,
        val representedAccounts: Set<String>,
        val representedSources: Set<String>
    )

    /**
     * An opt-in cross-product proof preview, separate from the regular
     * Statements coverage and gross cash totals. No ledger or archive writes.
     */
    data class ActivityEvidenceSnapshot(
        val statements: Snapshot,
        val originalActivityMessages: List<StatementEvidenceCorrelationPolicy.Observation>,
        val eventProof: StatementCrossSourceEventPolicy.Result
    )

    /**
     * For future Insights/review consumers. In contrast to
     * activitySnapshotBetween(), this uses the Activity store's raw read-only
     * range query and never triggers its maintenance/reclassification path.
     */
    fun readWithActivityEvidence(
        startInclusive: Long,
        endExclusive: Long,
        accountFilter: String? = null
    ): ActivityEvidenceSnapshot {
        val statements = read(startInclusive, endExclusive, accountFilter)
        val confirmed = FinancialUnderstandingStore.get(app).confirmedAccountIds() +
            AccountOwnershipConsentStore(app).confirmedRevisions().keys
        val owned = EverydayMoneyStore(app).ownedAccounts().filter {
            it.enabled && it.id in confirmed &&
                (accountFilter == null || it.id == accountFilter) &&
                it.type in setOf(AccountType.BANK, AccountType.MOBILE_WALLET)
        }.map { it.id }.toSet()
        val activity = synchronized(CanonicalStoreLock.monitor) {
            TransactionStore.get(app).transactionsBetween(startInclusive, endExclusive)
        }
        require(activity.size + statements.observations.size <= 20_000) {
            "This evidence range is too large. Choose a shorter period."
        }
        val messages = StatementActivityObservationPolicy.project(activity.map { row ->
            StatementActivityObservationPolicy.ActivityLeg(
                transactionId = row.id, accountId = row.accountId,
                sourceKind = row.sourceKind, type = row.type,
                direction = row.direction, currency = "KES",
                amountMinor = row.amountMinor, occurredAt = row.occurredAtEpochMs,
                reference = row.transactionCode, reviewState = row.reviewState,
                category = row.category,
                structurallyLinked = row.linkedAccountId != null ||
                    row.relationType != null || row.relatedTransactionId != null,
                reversalOfCode = row.reversalOfCode
            )
        }, owned)
        return ActivityEvidenceSnapshot(
            statements = statements,
            originalActivityMessages = messages,
            eventProof = StatementCrossSourceEventPolicy.reconcile(
                statements.observations + messages, owned
            )
        )
    }

    /** Bounded [startInclusive, endExclusive) source view; call on a worker thread. */
    fun read(startInclusive: Long, endExclusive: Long, accountFilter: String? = null): Snapshot {
        require(startInclusive > 0L && endExclusive > startInclusive)
        require(accountFilter == null || accountFilter.isNotBlank())
        val confirmed = FinancialUnderstandingStore.get(app).confirmedAccountIds() +
            AccountOwnershipConsentStore(app).confirmedRevisions().keys
        val accounts = EverydayMoneyStore(app).ownedAccounts().filter {
            it.enabled && it.id in confirmed && (accountFilter == null || it.id == accountFilter) &&
                it.type in setOf(AccountType.BANK, AccountType.MOBILE_WALLET)
        }
        val evidence = mutableListOf<StatementEvidenceCorrelationPolicy.Observation>()
        fun add(observation: StatementEvidenceCorrelationPolicy.Observation) {
            require(evidence.size < 20_000) {
                "This evidence range is too large. Choose a shorter period."
            }
            evidence.add(observation)
        }
        val wallets = accounts.filter { it.type == AccountType.MOBILE_WALLET }.map { it.id }.toSet()
        val walletStore = StatementActivityStore(app)
        for (batch in walletStore.batches()) {
            if (batch.accountId !in wallets) continue
            for (line in walletStore.lines(batch.id)) {
                if (line.informational || line.occurredAt !in startInclusive until endExclusive) continue
                add(StatementEvidenceCorrelationPolicy.Observation(
                    id = "mpesa:${batch.id}:${line.page}:${line.row}",
                    source = StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT,
                    sourceId = batch.id, accountId = batch.accountId,
                    currency = "KES", occurredAt = line.occurredAt,
                    movementMinor = line.movementMinor, reference = line.reference,
                    ownershipConfirmed = true,
                    sourceTotalsVerified = batch.totalsIndependentlyVerified
                ))
            }
        }
        val archive = StatementBankArchiveStore(app)
        for (account in accounts.filter { it.type == AccountType.BANK }) {
            for (source in archive.sourcesFor(account.id)) {
                if (source.lastAt < startInclusive || source.firstAt >= endExclusive) continue
                for (leg in archive.legsFor(source.id)) {
                    if (leg.occurredAt !in startInclusive until endExclusive) continue
                    add(StatementEvidenceCorrelationPolicy.Observation(
                        id = "bank:${source.id}:${leg.number}",
                        source = StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
                        sourceId = source.id, accountId = source.accountId,
                        currency = source.currency, occurredAt = leg.occurredAt,
                        movementMinor = leg.movementMinor, reference = leg.reference,
                        ownershipConfirmed = true, sourceTotalsVerified = true
                    ))
                }
            }
        }
        val ordered = evidence.sortedWith(
            compareBy<StatementEvidenceCorrelationPolicy.Observation> { it.occurredAt }
                .thenBy { it.id }
        )
        return Snapshot(ordered,
            StatementEvidenceCorrelationPolicy.reconcile(ordered, confirmed),
            StatementSourceCoveragePolicy.analyze(ordered),
            ordered.map { it.accountId }.toSet(), ordered.map { it.sourceId }.toSet())
    }
}
