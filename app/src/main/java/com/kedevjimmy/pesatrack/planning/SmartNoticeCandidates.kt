package com.kedevjimmy.pesatrack.planning

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.rules.*
import com.kedevjimmy.pesatrack.domain.receivables.*
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnosticsStore
import com.kedevjimmy.pesatrack.ui.screens.shouldOfferOwnershipReview

data class SmartNotice(val candidate: SmartNoticeCandidate, val entityKey: String? = null,
    val transactionId: Long? = null, val personId: String? = null, val incoming: Boolean = false,
    val repaymentAllowed: Boolean = false)

/** Canonical, read-only eligibility. Call while holding CanonicalStoreLock.monitor. */
object SmartNoticeCandidates {
    fun read(c: Context, now: Long): List<SmartNotice> {
        val prefs = SmartNoticePreferences(c)
        val settled = prefs.settledAt()
        val financial = FinancialUnderstandingStore.get(c)
        val evidence = financial.ownershipReviewEvidence()
        val diagnostics = FinancialIngestionDiagnosticsStore.get(c).recent(500)
        val accounts = financial.pendingEntities().filter {
            evidence[it.entityKey]?.canAsk == true && shouldOfferOwnershipReview(it,diagnostics)
        }.map { entity ->
            val proof = evidence.getValue(entity.entityKey)
            SmartNotice(SmartNoticeCandidate("account:" + SmartNoticePreferences.hash(entity.entityKey),
                SmartNoticePreferences.hash("${entity.accountTypeHint}:${proof.hasBalance}:${proof.transferCount}"),
                SmartNoticeKind.ACCOUNT,settled,prefs.enabled(SmartNoticeKind.ACCOUNT)),entityKey = entity.entityKey)
        }
        val snapshot = TransactionStore.get(c).activitySnapshotBetween(
            now - 30 * ReviewReminderPolicy.DAY_MS,now + 1,now,performMaintenance = false)
        val ambiguous = snapshot.possibleDuplicates.flatMap { listOf(it.first.id,it.second.id) }.toSet()
        val activity = snapshot.transactions.filter {
            it.direction == "EXPENSE" && it.reviewState == "NEEDS_REVIEW" &&
                it.category in setOf("OTHER","UNCATEGORIZED") && it.id !in ambiguous &&
                it.reversalOfCode == null && it.relationType == null && it.cycleCandidateReason == null
        }
        val reviews = if (activity.size < ReviewReminderPolicy.MIN_ACTIONABLE) emptyList() else listOf(
            SmartNotice(SmartNoticeCandidate("activity",SmartNoticePreferences.hash(
                ReviewReminderPolicy.fingerprint(activity.map { it.id }.toSet())),SmartNoticeKind.ACTIVITY,
                maxOf(settled,activity.maxOf { it.receivedAtEpochMs }),prefs.enabled(SmartNoticeKind.ACTIVITY))))
        val journal = PersonalReceivableStore(c)
        val people = journal.people().map { person ->
            val history = journal.events(person.id)
            NoticePerson(person.id,person.phone,history.isNotEmpty() && history.all { it.active },person.outstandingMinor)
        }
        val historyRepository = ReceivableHistoryRepository(c)
        val excluded = people.associate { it.id to historyRepository.exclusions(it.id) }
        val payments = snapshot.transactions.mapNotNull { row ->
            val kind = if (row.direction == "EXPENSE") "LENT" else "REPAID"
            val person = ReceivableNotificationPolicy.match(people,row.counterpartyPhone,row.direction,
                row.amountMinor,row.occurredAtEpochMs,row.receivedAtEpochMs,row.sourceKind,
                prefs.paymentEnabledAt(),now,
                row.type in setOf("SENT_MONEY","RECEIVED") && row.category !in setOf("FEES","DEBT_LOAN","TRANSFER","SAVINGS","INVESTMENT") &&
                row.cycleCandidateReason == null && row.linkedAccountId == null &&
                    journal.linkProblem(row.id,kind,row.amountMinor) == null,row.id in ambiguous) ?: return@mapNotNull null
            if (row.id in excluded.getValue(person.id)) return@mapNotNull null
            val last = prefs.personLastShown(person.id)
            SmartNotice(SmartNoticeCandidate("payment:" + SmartNoticePreferences.hash(row.fingerprint),
                SmartNoticePreferences.hash("${row.id}:${row.direction}:${row.amountMinor}:${person.id}"),
                SmartNoticeKind.PAYMENT,maxOf(settled,row.receivedAtEpochMs),
                prefs.enabled(SmartNoticeKind.PAYMENT) && (last == 0L || now - last >= ReviewReminderPolicy.DAY_MS)),
                transactionId = row.id,personId = person.id,incoming = row.direction == "INCOME",
                repaymentAllowed = ReceivableNotificationPolicy.canRepay(person,row.amountMinor))
        }
        // Existing usage completes discovery, even when notifications have never been enabled.
        if (people.isNotEmpty()) prefs.completeDiscovery()
        val discovery = if (!prefs.discoveryPending()) emptyList() else listOf(SmartNotice(
            SmartNoticeCandidate("discovery","1",SmartNoticeKind.DISCOVERY,settled,prefs.enabled(SmartNoticeKind.DISCOVERY))))
        return accounts + reviews + payments + discovery
    }
}
