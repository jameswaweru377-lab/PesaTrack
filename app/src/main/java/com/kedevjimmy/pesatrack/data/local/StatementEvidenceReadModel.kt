package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.statements.StatementEvidenceCorrelationPolicy

/**
 * Phase 3 read-only statement evidence boundary. It does not query or modify
 * Activity, and does not claim that unlinked rows form a combined cash total.
 */
class StatementEvidenceReadModel(context: Context) {
    private val app = context.applicationContext

    data class Snapshot(
        val observations: List<StatementEvidenceCorrelationPolicy.Observation>,
        val correlation: StatementEvidenceCorrelationPolicy.Result,
        val representedAccounts: Set<String>,
        val representedSources: Set<String>
    )

    /** Bounded [startInclusive, endExclusive) source view; call on a worker thread. */
    fun read(startInclusive: Long, endExclusive: Long): Snapshot {
        require(startInclusive > 0L && endExclusive > startInclusive)
        val confirmed = FinancialUnderstandingStore.get(app).confirmedAccountIds() +
            AccountOwnershipConsentStore(app).confirmedRevisions().keys
        val accounts = EverydayMoneyStore(app).ownedAccounts().filter {
            it.enabled && it.id in confirmed &&
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
                    ownershipConfirmed = true
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
                        ownershipConfirmed = true
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
            ordered.map { it.accountId }.toSet(), ordered.map { it.sourceId }.toSet())
    }
}
