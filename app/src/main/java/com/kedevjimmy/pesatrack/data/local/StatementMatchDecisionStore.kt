package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.statements.StatementEvidenceCorrelationPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementMatchDecisionPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementMatchReviewPolicy
import java.util.Calendar
import java.util.TimeZone

/** Stores review decisions only; never changes financial records or report totals. */
internal class StatementMatchDecisionStore(context: Context) {
    private val app = context.applicationContext

    enum class Kind { TRANSFER, SAME_MOVEMENT }
    enum class Decision { CONFIRMED, NOT_SAME }
    enum class Result { SAVED, STALE, CONFLICT }
    data class Candidate(val kind: Kind, val rows: List<StatementEvidenceCorrelationPolicy.Observation>, val hash: String)
    data class Snapshot(val review: StatementMatchReviewPolicy.Review, val candidates: List<Candidate>,
                        val decisions: Map<String, Decision>, val missingOwnershipConsent: Boolean)

    fun read(year: Int): Snapshot = synchronized(CanonicalStoreLock.monitor) { current(year) }

    /** Re-read evidence and ownership before saving; stale UI selections cannot be confirmed. */
    fun decide(year: Int, expectedHash: String, kind: Kind, decision: Decision?): Result =
        synchronized(CanonicalStoreLock.monitor) {
            require(expectedHash.matches(Regex("[a-f0-9]{64}")))
            val snapshot = current(year)
            val selected = snapshot.candidates.filter { it.hash == expectedHash && it.kind == kind }
            if (selected.size != 1) return@synchronized Result.STALE
            if (decision == Decision.CONFIRMED) {
                val ids = selected.single().rows.map { it.id }.toSet()
                if (snapshot.candidates.any { other -> other.hash != expectedHash &&
                        snapshot.decisions[other.hash] == Decision.CONFIRMED &&
                        other.rows.any { it.id in ids } }) return@synchronized Result.CONFLICT
            }
            val helper = StatementArchiveDb(app)
            try {
                val db = helper.writableDatabase
                if (decision == null) {
                    db.delete("statement_match_decisions_v1", "evidence_hash=? AND kind=?",
                        arrayOf(expectedHash, kind.name))
                } else {
                    val inserted = db.insertWithOnConflict("statement_match_decisions_v1", null, ContentValues().apply {
                        put("evidence_hash", expectedHash); put("kind", kind.name)
                        put("decision", decision.name); put("updated_at", System.currentTimeMillis())
                    }, SQLiteDatabase.CONFLICT_REPLACE)
                    check(inserted != -1L) { "Could not save the match decision." }
                }
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
        val edge = 48L * 60L * 60L * 1000L
        val source = StatementEvidenceReadModel(app).readWithActivityEvidence(maxOf(1L, start - edge), end + edge)
        val review = StatementMatchReviewPolicy.project(
            source.statements.observations + source.originalActivityMessages, source.eventProof, start, end)
        val revisions = AccountOwnershipConsentStore(app).confirmedRevisions()
        var missingConsent = false
        fun candidate(kind: Kind, rows: List<StatementEvidenceCorrelationPolicy.Observation>): Candidate? {
            if (rows.any { it.accountId !in revisions }) { missingConsent = true; return null }
            return Candidate(kind, rows, StatementMatchDecisionPolicy.fingerprint(rows, revisions))
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
                        "SELECT evidence_hash, kind, decision FROM statement_match_decisions_v1 WHERE evidence_hash IN ($marks)",
                        chunk.toTypedArray()).use { cursor ->
                    while (cursor.moveToNext()) {
                        val hash = cursor.getString(0)
                        val kind = runCatching { Kind.valueOf(cursor.getString(1)) }.getOrNull()
                        val decision = runCatching { Decision.valueOf(cursor.getString(2)) }.getOrNull()
                        if (kind != null && decision != null && expected[hash]?.singleOrNull()?.kind == kind)
                            put(hash, decision)
                    }
                    }
                }
            }
        } finally { helper.close() }
        return Snapshot(review, candidates, saved, missingConsent)
    }
}
