package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.statements.StatementEvidenceCorrelationPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementMatchDecisionPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementMatchReviewPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementPossibleTransferPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementAccountMentionPolicy
import java.util.Calendar
import java.util.TimeZone

/** Stores review decisions only; never changes financial records or report totals. */
internal class StatementMatchDecisionStore(context: Context, private val afterValidationForTest: (() -> Unit)? = null) {
    private val app = context.applicationContext

    enum class Kind { TRANSFER, SAME_MOVEMENT }
    enum class Decision { CONFIRMED, NOT_SAME }
    enum class Result { SAVED, STALE, CONFLICT, ACCOUNT_CHECK_REQUIRED }
    data class Candidate(val kind: Kind, val rows: List<StatementEvidenceCorrelationPolicy.Observation>, val hash: String) {
        val needsAccountAttestation: Boolean get() = StatementMatchDecisionPolicy.requiresAccountAttestation(rows)
    }
    data class Snapshot(val review: StatementMatchReviewPolicy.Review, val candidates: List<Candidate>,
                        val decisions: Map<String, Decision>, val missingOwnershipConsent: Boolean,
                        val excludedBankSourceCount: Int = 0,
                        val possibleTransfers: List<StatementPossibleTransferPolicy.Suggestion> = emptyList(),
                        val accountMentions: List<StatementAccountMentionPolicy.Hint> = emptyList())

    fun read(year: Int): Snapshot = synchronized(CanonicalStoreLock.monitor) { current(year) }

    /** Re-read evidence and ownership before saving; stale UI selections cannot be confirmed. */
    fun decide(year: Int, expectedHash: String, kind: Kind, expectedDecision: Decision?, decision: Decision?,
               accountAttested: Boolean = false): Result =
        synchronized(CanonicalStoreLock.monitor) {
            require(expectedHash.matches(Regex("[a-f0-9]{64}")))
            val helper = StatementArchiveDb(app)
            try {
                val db = helper.writableDatabase
                db.beginTransactionNonExclusive()
                try {
                    val snapshot = current(year)
                    val selected = snapshot.candidates.filter { it.hash == expectedHash && it.kind == kind }
                    if (selected.size != 1 || snapshot.decisions[expectedHash] != expectedDecision)
                        return@synchronized Result.STALE
                    if (decision == Decision.CONFIRMED && selected.single().needsAccountAttestation && !accountAttested)
                        return@synchronized Result.ACCOUNT_CHECK_REQUIRED
                    if (decision == Decision.CONFIRMED) {
                        val ids = selected.single().rows.map { it.id }.toSet()
                        if (snapshot.candidates.any { other -> other.hash != expectedHash &&
                                snapshot.decisions[other.hash] == Decision.CONFIRMED &&
                                other.rows.any { it.id in ids } }) return@synchronized Result.CONFLICT
                    }
                    afterValidationForTest?.invoke()
                    if (decision == null) {
                        db.delete("statement_match_decisions_v1", "evidence_hash=? AND kind=?",
                            arrayOf(expectedHash, kind.name))
                    } else {
                        val inserted = db.insertWithOnConflict("statement_match_decisions_v1", null, ContentValues().apply {
                            put("evidence_hash", expectedHash); put("kind", kind.name)
                            put("decision", decision.name)
                            put("account_attested", if (decision == Decision.CONFIRMED && accountAttested) 1 else 0)
                            put("updated_at", System.currentTimeMillis())
                        }, SQLiteDatabase.CONFLICT_REPLACE)
                        check(inserted != -1L) { "Could not save the match decision." }
                    }
                    db.setTransactionSuccessful()
                } finally { db.endTransaction() }
            } finally { helper.close() }
            LedgerUpdateBus.notifyChanged()
            Result.SAVED
        }

    private fun current(year: Int): Snapshot {
        require(year in 1970..2100)
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Africa/Nairobi")).apply {
            clear(); set(year, Calendar.JANUARY, 1, 0, 0, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.YEAR, 1)
        val end = calendar.timeInMillis
        val window = StatementMatchReviewPolicy.evidenceWindow(start, end)
        val source = StatementEvidenceReadModel(app).readWithActivityEvidence(window.startInclusive, window.endExclusive)
        val review = StatementMatchReviewPolicy.project(
            source.statements.observations + source.originalActivityMessages, source.eventProof, start, end)
        val revisions = AccountOwnershipConsentStore(app).confirmedRevisions()
        val ownership = AccountStore.get(app).listAccounts().mapNotNull { account ->
            val revision = revisions[account.id] ?: return@mapNotNull null
            account.id to StatementMatchDecisionPolicy.Ownership(revision, account.type.name, account.identifier, account.name)
        }.toMap()
        var missingConsent = false
        fun candidate(kind: Kind, rows: List<StatementEvidenceCorrelationPolicy.Observation>): Candidate? {
            if (rows.any { it.accountId !in ownership }) { missingConsent = true; return null }
            return Candidate(kind, rows, StatementMatchDecisionPolicy.fingerprint(rows, ownership))
        }
        val candidates = review.transfers.mapNotNull { candidate(Kind.TRANSFER, it.outgoing + it.incoming) } +
            review.sameMovements.mapNotNull { candidate(Kind.SAME_MOVEMENT, it) }
        val helper = StatementArchiveDb(app)
        val saved = try {
            val expected = candidates.groupBy { it.hash }
            buildMap {
                for (chunk in expected.keys.chunked(400)) {
                    val marks = List(chunk.size) { "?" }.joinToString(",")
                    helper.readableDatabase.rawQuery(
                        "SELECT evidence_hash, kind, decision, account_attested FROM statement_match_decisions_v1 WHERE evidence_hash IN ($marks)",
                        chunk.toTypedArray()).use { cursor ->
                    while (cursor.moveToNext()) {
                        val hash = cursor.getString(0)
                        val kind = runCatching { Kind.valueOf(cursor.getString(1)) }.getOrNull()
                        val decision = runCatching { Decision.valueOf(cursor.getString(2)) }.getOrNull()
                        val candidate = expected[hash]?.singleOrNull()
                        if (kind != null && decision != null && candidate?.kind == kind &&
                            (decision != Decision.CONFIRMED || !candidate.needsAccountAttestation || cursor.getInt(3) == 1))
                            put(hash, decision)
                    }
                    }
                }
            }
        } finally { helper.close() }
        val possibleTransfers = StatementPossibleTransferPolicy.suggest(
            source.eligibleStatementObservations + source.originalActivityMessages, source.eventProof, start, end)
        val shownIds = review.transfers.flatMap { it.outgoing + it.incoming }.map { it.id } +
            review.sameMovements.flatten().map { it.id } +
            possibleTransfers.flatMap { listOf(it.outgoing.id, it.incoming.id) } + source.eventProof.ambiguousIds
        val bankRecords = source.eligibleStatementObservations.mapNotNull { observation ->
            source.statements.bankDescriptions[observation.id]?.let { description ->
                StatementAccountMentionPolicy.BankRecord(observation, description)
            }
        }
        val confirmedAccounts = ownership.filterValues { it.type == "BANK" || it.type == "MOBILE_WALLET" }
            .map { (id, account) -> StatementAccountMentionPolicy.Account(id, account.identifier) }
        val accountMentions = StatementAccountMentionPolicy.find(bankRecords, confirmedAccounts, shownIds.toSet(), start, end)
        return Snapshot(review, candidates, saved, missingConsent, source.excludedBankSourceCount, possibleTransfers, accountMentions)
    }
}
