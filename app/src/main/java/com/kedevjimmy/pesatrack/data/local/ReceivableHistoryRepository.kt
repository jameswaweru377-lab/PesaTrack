package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.evidence.EvidenceCanonicalFact
import com.kedevjimmy.pesatrack.domain.receivables.*
import java.util.UUID

data class ReceivableHistoryCandidate(
    val row: StoredTransaction,
    val match: HistoryMatch,
    val problem: String?,
    val giftExcluded: Boolean,
    val accountName: String,
    val sourceLabel: String
)
data class ReceivableHistoryPage(val candidates: List<ReceivableHistoryCandidate>, val nextOffset: Int?)
enum class ReceivableHistorySourceFilter { ALL, MESSAGES, STATEMENTS }
data class ReceivableHistorySelection(val row: StoredTransaction, val kind: String, val amountMinor: Long)

/** Bounded canonical-history queries and a single atomic user decision. No SMS/network access. */
class ReceivableHistoryRepository(context: Context) {
    private val app = context.applicationContext
    private val database = PesaTrackDatabase.get(app)
    private val transactions = TransactionStore.get(app)
    private val journal = PersonalReceivableStore(app)

    fun search(query: String, personId: String?, name: String, phone: String?,
               limit: Int = 50, offset: Int = 0,
               sourceFilter: ReceivableHistorySourceFilter = ReceivableHistorySourceFilter.ALL,
               fromEpochMs: Long? = null, throughEpochMs: Long? = null): List<ReceivableHistoryCandidate> =
        searchPage(query,personId,name,phone,limit,offset,sourceFilter,fromEpochMs,throughEpochMs).candidates

    fun searchPage(query: String, personId: String?, name: String, phone: String?,
               limit: Int = 50, offset: Int = 0,
               sourceFilter: ReceivableHistorySourceFilter = ReceivableHistorySourceFilter.ALL,
               fromEpochMs: Long? = null, throughEpochMs: Long? = null): ReceivableHistoryPage =
        synchronized(CanonicalStoreLock.monitor) {
            require(limit in 1..50 && offset >= 0)
            require(fromEpochMs == null || throughEpochMs == null || fromEpochMs <= throughEpochMs) {
                "Start date must be before the end date."
            }
            val clean = query.trim()
            require(clean.length in 2..100) { "Enter at least two characters to search." }
            fun escaped(value: String) = value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            val words = clean.split(Regex("[\\s\\p{Z}]+")).filter { it.isNotBlank() }
            val namePattern = "%" + words.joinToString("%") { escaped(it) } + "%"
            val digits = clean.filter(Char::isDigit)
            val normalized = ReceivableHistoryPolicy.normalizedPhone(clean)
            val phonePattern = "%" + (normalized?.takeLast(9) ?: digits.takeIf { it.length >= 4 } ?: "__no_phone_match__") + "%"
            val embeddedStatementPhone = normalized?.takeLast(9)?.let { "%$it%" }
            val accounts = AccountStore.get(app).listAccounts(enabledOnly = false).associate { it.id to it.name }
            val excluded = if (personId == null) emptySet() else exclusions(personId)
            val sourceClause = when (sourceFilter) {
                ReceivableHistorySourceFilter.ALL -> ""
                ReceivableHistorySourceFilter.MESSAGES -> """
                    AND (LOWER(t.source_kind) LIKE '%sms%' OR LOWER(t.source_kind) LIKE '%notification%'
                      OR LOWER(t.source_kind) LIKE '%historical%financial_message%'
                      OR EXISTS (SELECT 1 FROM evidence_links l JOIN evidence_observations o ON o.id=l.observation_id
                          WHERE l.transaction_id=t.id AND o.status='LINKED' AND o.source_kind IN ('SMS','NOTIFICATION')))
                """.trimIndent()
                ReceivableHistorySourceFilter.STATEMENTS -> """
                    AND (t.source_kind='statement_import'
                      OR EXISTS (SELECT 1 FROM evidence_links l JOIN evidence_observations o ON o.id=l.observation_id
                          WHERE l.transaction_id=t.id AND o.status='LINKED' AND o.source_kind='STATEMENT'))
                """.trimIndent()
            }
            val dateClause = buildString {
                if (fromEpochMs != null) append(" AND t.occurred_at_epoch_ms >= ?")
                if (throughEpochMs != null) append(" AND t.occurred_at_epoch_ms <= ?")
            }
            fun queryIds(batchLimit: Int, batchOffset: Int): List<Long> {
                val args = buildList {
                    add(namePattern); add(phonePattern)
                    if (embeddedStatementPhone != null) add(embeddedStatementPhone)
                    if (fromEpochMs != null) add(fromEpochMs.toString())
                    if (throughEpochMs != null) add(throughEpochMs.toString())
                    add(batchLimit.toString()); add(batchOffset.toString())
                }.toTypedArray()
                return database.readableDatabase.rawQuery(
                    """SELECT t.id FROM transactions t
                       WHERE (LOWER(COALESCE(t.counterparty,'')) LIKE LOWER(?) ESCAPE '\'
                          OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(COALESCE(t.counterparty_phone,''),' ',''),'-',''),'+',''),'(',''),')','') LIKE ?
                          ${if (embeddedStatementPhone != null) "OR (t.source_kind='statement_import' AND REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(COALESCE(t.counterparty,''),' ',''),'-',''),'+',''),'(',''),')',''),'.','') LIKE ?)" else ""})
                          )
                          $sourceClause
                          $dateClause
                       ORDER BY t.occurred_at_epoch_ms DESC, t.id DESC LIMIT ? OFFSET ?""",
                    args
                ).use { c -> buildList { while (c.moveToNext()) add(c.getLong(0)) } }
            }
            val candidates = mutableListOf<ReceivableHistoryCandidate>()
            var cursor = offset
            var reachedEnd = false
            while (candidates.size < limit && !reachedEnd) {
                val ids = queryIds(50,cursor)
                if (ids.isEmpty()) break
                val batchStart = cursor
                cursor += ids.size
                if (ids.size < 50) reachedEnd = true
                val rowsById = ids.mapNotNull { id -> transactions.getById(id)?.let { id to it } }.toMap()
                val evidenceByTransaction = EvidenceJournalStore.sourcesFor(app,rowsById.map { (id,row) ->
                    id to EvidenceCanonicalFact(row.accountId,row.direction,row.amountMinor,row.occurredAtEpochMs,
                        row.transactionCode,row.sourceKind)
                })
                for ((index,id) in ids.withIndex()) {
                    val row = rowsById[id] ?: continue
                    val linkedSources = evidenceByTransaction[id].orEmpty().filterNot { it.needsReview }.map { it.sourceKind }
                    val isStatement = row.sourceKind.equals("statement_import",true) || "STATEMENT" in linkedSources
                    val isMessage = row.sourceKind.contains("sms",true) || row.sourceKind.contains("notification",true) ||
                        row.sourceKind.contains("historical_financial_message",true) ||
                        "SMS" in linkedSources || "NOTIFICATION" in linkedSources
                    val matchesSource = when (sourceFilter) {
                        ReceivableHistorySourceFilter.ALL -> true
                        ReceivableHistorySourceFilter.MESSAGES -> isMessage
                        ReceivableHistorySourceFilter.STATEMENTS -> isStatement
                    }
                    if (!matchesSource) continue
                    val sourceLabel = when {
                        isStatement && isMessage -> "Messages · Statements"
                        isStatement -> "Statements"
                        isMessage -> "Messages"
                        else -> "Activity record"
                    }
                    val kind = if (row.direction == "EXPENSE") "LENT" else "REPAID"
                    candidates += ReceivableHistoryCandidate(row,
                        if (isStatement && row.counterpartyPhone.isNullOrBlank())
                            ReceivableHistoryPolicy.matchStatementDescription(name,phone,row.counterparty)
                        else ReceivableHistoryPolicy.match(name,phone,row.counterparty,row.counterpartyPhone),
                        journal.linkProblem(row.id,kind,row.amountMinor), id in excluded,
                        accounts[row.accountId] ?: "Imported account",sourceLabel)
                    if (candidates.size == limit) {
                        cursor = batchStart + index + 1
                        break
                    }
                }
            }
            val hasMore = candidates.size == limit && queryIds(1,cursor).isNotEmpty()
            ReceivableHistoryPage(candidates,cursor.takeIf { hasMore })
        }

    fun exclusions(personId: String): Set<Long> = synchronized(CanonicalStoreLock.monitor) {
        database.readableDatabase.rawQuery(
            "SELECT transaction_id FROM personal_receivable_review_exclusions WHERE person_id = ?",arrayOf(personId)
        ).use { c -> buildSet { while (c.moveToNext()) add(c.getLong(0)) } }
    }

    fun preview(personId: String?, selections: List<ReceivableHistorySelection>): BatchTotals =
        synchronized(CanonicalStoreLock.monitor) {
            val existing = personId?.let(journal::events).orEmpty()
            require(existing.all { it.active }) { "Review changed source evidence before adding history." }
            ReceivableBatchPolicy.validate(existing.map { DebtMovement(it.transactionId,it.kind,it.amountMinor,it.occurredAt) },
                selections.map { DebtMovement(it.row.id,it.kind,it.amountMinor,it.row.occurredAtEpochMs) })
        }

    fun confirm(personId: String?, name: String, phone: String?,
                selections: List<ReceivableHistorySelection>, gifts: List<StoredTransaction>): String =
        synchronized(CanonicalStoreLock.monitor) {
            require(name.trim().length in 2..100) { "Enter the person's full name." }
            require(personId != null || phone.isNullOrBlank() || ReceivableHistoryPolicy.normalizedPhone(phone) != null) { "Check the phone number." }
            require(selections.isNotEmpty() || gifts.isNotEmpty()) { "Select records to confirm." }
            require(selections.size + gifts.size <= 500) { "Confirm up to 500 records at a time." }
            val ids = selections.map { it.row.id } + gifts.map { it.id }
            require(ids.distinct().size == ids.size) { "Choose one decision per transaction." }
            val db = database.writableDatabase
            db.beginTransaction()
            try {
                if (personId != null) require(journal.nameFor(personId) != null) { "Person is no longer available." }
                selections.forEach { selected ->
                    require(transactions.getById(selected.row.id) == selected.row) { "A source transaction changed. Search again before confirming." }
                    require(journal.linkProblem(selected.row.id,selected.kind,selected.amountMinor) == null) {
                        journal.linkProblem(selected.row.id,selected.kind,selected.amountMinor) ?: "Record cannot be linked."
                    }
                }
                gifts.forEach { row ->
                    require(row.direction == "EXPENSE" && transactions.getById(row.id) == row) { "A gift source changed. Search again." }
                    require(journal.linkProblem(row.id,"LENT",row.amountMinor) == null) { "This gift source cannot be reviewed." }
                }
                preview(personId,selections)
                val id = personId ?: UUID.randomUUID().toString().also { generated ->
                    db.insertOrThrow("personal_receivable_people",null,ContentValues().apply {
                        put("id",generated); put("display_name",name.trim())
                        put("phone",ReceivableHistoryPolicy.normalizedPhone(phone))
                        put("created_at_epoch_ms",System.currentTimeMillis())
                    })
                }
                selections.forEach { selected ->
                    db.delete("personal_receivable_review_exclusions","person_id = ? AND transaction_id = ?",arrayOf(id,selected.row.id.toString()))
                    db.insertOrThrow("personal_receivable_events",null,ContentValues().apply {
                        put("event_key","user:" + UUID.randomUUID()); put("person_id",id)
                        put("kind",selected.kind); put("amount_minor",selected.amountMinor)
                        put("occurred_at_epoch_ms",selected.row.occurredAtEpochMs)
                        put("linked_transaction_id",selected.row.id)
                        put("memo","Confirmed from transaction history"); put("created_at_epoch_ms",System.currentTimeMillis())
                    })
                }
                gifts.forEach { row ->
                    db.insertWithOnConflict("personal_receivable_review_exclusions",null,ContentValues().apply {
                        put("person_id",id); put("transaction_id",row.id); put("created_at_epoch_ms",System.currentTimeMillis())
                    },android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE)
                }
                db.setTransactionSuccessful()
                id
            } finally {
                db.endTransaction()
                LedgerUpdateBus.notifyChanged()
            }
        }
}
