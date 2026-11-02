package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.statements.StatementEvidenceCorrelationPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementMatchDecisionPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementMatchReviewPolicy
import com.kedevjimmy.pesatrack.domain.statements.TransferDecisionPolicy
import com.kedevjimmy.pesatrack.domain.statements.TransferDecisionPolicy.Decision
import com.kedevjimmy.pesatrack.domain.statements.TransferDecisionPolicy.Result
import com.kedevjimmy.pesatrack.domain.statements.TransferReconciliationPolicy
import java.util.Calendar
import java.util.TimeZone

/** Local review metadata only. No migration, financial writes or maintenance-bearing snapshots. */
internal class TransferReconciliationStore(context: Context) {
    private val app = context.applicationContext
    class Snapshot(val reconciliation: TransferReconciliationPolicy.Result, val decisions: Map<String, Decision>,
                   val legacyConfirmedIds: Set<String>, val excludedBankSources: Int) {
        override fun toString(): String = "[private transfer review]"
    }

    fun read(year: Int): Snapshot = synchronized(CanonicalStoreLock.monitor) { current(year, true) }

    /** Called by the legacy review writer inside its lock; deliberately avoids reading it recursively. */
    fun confirmedObservationIds(year: Int): Set<String> = synchronized(CanonicalStoreLock.monitor) {
        val snapshot = current(year, false)
        snapshot.reconciliation.pairs.filter { snapshot.decisions[it.fingerprint] == Decision.CONFIRMED }
            .flatMap { it.rows }.mapTo(mutableSetOf()) { it.id }
    }

    fun decide(year: Int, fingerprint: String, expected: Decision?, decision: Decision?, attested: Boolean): Result =
        synchronized(CanonicalStoreLock.monitor) {
            require(fingerprint.matches(Regex("[a-f0-9]{64}")))
            val helper = StatementArchiveDb(app)
            try {
                val db = helper.writableDatabase
                db.beginTransactionNonExclusive()
                try {
                    val snapshot = current(year, true)
                    val result = TransferDecisionPolicy.validate(fingerprint, expected, decision,
                        snapshot.reconciliation.pairs, snapshot.decisions, snapshot.legacyConfirmedIds, attested)
                    if (result != Result.SAVED) return@synchronized result
                    if (decision == null) {
                        db.delete("statement_match_decisions_v1", "evidence_hash=? AND kind='TRANSFER'", arrayOf(fingerprint))
                    } else {
                        val values = ContentValues().apply {
                            put("evidence_hash", fingerprint); put("kind", "TRANSFER"); put("decision", decision.name)
                            put("account_attested", if (decision == Decision.CONFIRMED && attested) 1 else 0)
                            put("updated_at", System.currentTimeMillis())
                        }
                        check(db.insertWithOnConflict("statement_match_decisions_v1", null, values,
                            SQLiteDatabase.CONFLICT_REPLACE) != -1L) { "Review choice could not be saved." }
                    }
                    db.setTransactionSuccessful()
                } finally { db.endTransaction() }
            } finally { helper.close() }
            LedgerUpdateBus.notifyChanged()
            Result.SAVED
        }

    private fun current(year: Int, includeLegacy: Boolean): Snapshot {
        require(year in 1970..2100)
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Africa/Nairobi")).apply {
            clear(); set(year, Calendar.JANUARY, 1, 0, 0, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.YEAR, 1)
        val end = calendar.timeInMillis
        val window = StatementMatchReviewPolicy.evidenceWindow(start, end)
        val source = StatementEvidenceReadModel(app).readWithActivityEvidence(window.startInclusive, window.endExclusive)
        val revisions = AccountOwnershipConsentStore(app).confirmedRevisions()
        val ownership = AccountStore.get(app).listAccounts().filter { it.enabled }.mapNotNull { account ->
            val revision = revisions[account.id] ?: return@mapNotNull null
            account.id to StatementMatchDecisionPolicy.Ownership(revision, account.type.name, account.identifier, account.name)
        }.toMap()
        val original = source.originalActivityMessages
        val protectedIds = protectedActivityIds(original)
        val statements = TransferStatementEvidenceReader(app).read(source.eligibleStatementObservations)
        val rows = source.eligibleStatementObservations.map {
            if (it.id in statements.protectedIds) it.copy(roleProtected = true) else it
        } + original.map {
            if (it.id in protectedIds) it.copy(roleProtected = true) else it
        }
        val descriptions = statements.descriptions
        val reconciliation = TransferReconciliationPolicy.reconcile(rows, ownership, descriptions, start, end)
        val saved = readDecisions(reconciliation.pairs.map { it.fingerprint })
        val legacy = if (includeLegacy) legacyConfirmedIds(year) else emptySet()
        return Snapshot(reconciliation, saved, legacy, source.excludedBankSourceCount)
    }

    private fun protectedActivityIds(rows: List<StatementEvidenceCorrelationPolicy.Observation>): Set<String> {
        val db = PesaTrackDatabase.get(app).readableDatabase
        return buildSet {
            for (chunk in rows.map { it.id.removePrefix("activity:") }.chunked(400)) {
                val marks = chunk.joinToString(",") { "?" }
                db.rawQuery("SELECT id FROM transactions WHERE id IN ($marks) AND review_state IN ('USER_CLASSIFIED','NEEDS_REVIEW','EXCLUDED')",
                    chunk.toTypedArray()).use { cursor ->
                    while (cursor.moveToNext()) add("activity:${cursor.getLong(0)}")
                }
            }
        }
    }

    private fun legacyConfirmedIds(year: Int): Set<String> {
        return StatementMatchDecisionStore(app).confirmedObservationIds(year)
    }

    private fun readDecisions(hashes: List<String>): Map<String, Decision> {
        val helper = StatementArchiveDb(app)
        try {
            return buildMap {
                for (chunk in hashes.chunked(400)) {
                    val marks = chunk.joinToString(",") { "?" }
                    helper.readableDatabase.rawQuery(
                        "SELECT evidence_hash,decision,account_attested FROM statement_match_decisions_v1 WHERE kind='TRANSFER' AND evidence_hash IN ($marks)",
                        chunk.toTypedArray()).use { cursor ->
                        while (cursor.moveToNext()) {
                            val decision = runCatching { Decision.valueOf(cursor.getString(1)) }.getOrNull() ?: continue
                            if (decision != Decision.CONFIRMED || cursor.getInt(2) == 1) put(cursor.getString(0), decision)
                        }
                    }
                }
            }
        } finally { helper.close() }
    }
}
