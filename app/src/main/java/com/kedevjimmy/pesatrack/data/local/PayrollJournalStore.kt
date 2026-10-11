package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.payroll.*
import java.util.UUID

/** Only the payroll journal is writable here; no TransactionStore, account or pay-cycle side effects. */
class PayrollJournalStore(context: Context) {
    private val database=PesaTrackDatabase.get(context.applicationContext)
    fun records(limit: Int = 50): List<PayrollRecord> = synchronized(CanonicalStoreLock.monitor) {
        val db=database.readableDatabase
        db.rawQuery("SELECT id FROM payroll_records ORDER BY payroll_month DESC,updated_at_epoch_ms DESC LIMIT ?",
            arrayOf(limit.coerceIn(1,200).toString())).use { cursor ->
            buildList { while(cursor.moveToNext()) add(checkNotNull(load(db,cursor.getString(0)))) }
        }
    }
    fun save(draft: PayrollDraft, userReviewed: Boolean, expected: PayrollRecord? = null): PayrollRecord = synchronized(CanonicalStoreLock.monitor) {
        require(userReviewed) { "Review the payroll fields before saving." }
        val normalized=draft.copy(employer=draft.employer.trim(),lines=draft.lines.map {it.copy(label=it.label.trim())})
        PayrollJournalPolicy.validate(normalized)
        val existingId=expected?.id
        val key=PayrollJournalPolicy.periodKey(normalized);val content=PayrollJournalPolicy.contentKey(normalized)
        val db=database.writableDatabase
        db.beginTransaction()
        try {
            val old=existingId?.let {load(db,it)}
            if(expected!=null) PayrollJournalPolicy.requireUnchanged(old,expected)
            val other=db.rawQuery("SELECT id FROM payroll_records WHERE period_key=?",arrayOf(key)).use {if(it.moveToFirst()) it.getString(0) else null}
            if(existingId==null && other!=null) {
                val current=checkNotNull(load(db,other))
                require(PayrollJournalPolicy.contentKey(current.draft)==content) { "Payroll already exists for this employer and month. Edit the existing record; it was not overwritten." }
                db.setTransactionSuccessful(); return@synchronized current
            }
            require(other==null || other==existingId) { "Another payroll record already uses this employer and month." }
            val now=System.currentTimeMillis(); val id=existingId ?: UUID.randomUUID().toString()
            val revision=try {Math.addExact(old?.revision ?: 0,1)} catch(_: ArithmeticException) { throw IllegalArgumentException("This record cannot be revised further.") }
            val values=ContentValues().apply {
                put("id",id);put("revision",revision);put("employer",normalized.employer);put("payroll_month",normalized.period)
                put("gross_minor",normalized.grossMinor);put("net_minor",normalized.netMinor);put("source",normalized.source.name)
                put("period_key",key);put("content_key",content);put("created_at_epoch_ms",old?.createdAtEpochMs ?: now)
                put("updated_at_epoch_ms",now);put("reviewed",1)
            }
            if(old==null) db.insertOrThrow("payroll_records",null,values)
            else {
                check(db.update("payroll_records",values,"id=? AND revision=?",arrayOf(id,expected!!.revision.toString()))==1)
                db.delete("payroll_lines","payroll_id=?",arrayOf(id))
            }
            normalized.lines.forEachIndexed {position,line -> db.insertOrThrow("payroll_lines",null,ContentValues().apply {
                put("payroll_id",id);put("position",position);put("source_label",line.label);put("semantic_type",line.type.name)
                put("role",line.role.name);put("amount_minor",line.amountMinor)
            }) }
            val saved=checkNotNull(load(db,id));db.setTransactionSuccessful();saved
        } finally { db.endTransaction() }
    }
    fun remove(expected: PayrollRecord): Boolean = synchronized(CanonicalStoreLock.monitor) {
        val id=expected.id
        val db=database.writableDatabase;db.beginTransaction()
        try {
            val old=load(db,id)
            PayrollJournalPolicy.requireUnchanged(old,expected)
            db.delete("payroll_lines","payroll_id=?",arrayOf(id))
            val removed=db.delete("payroll_records","id=? AND revision=?",arrayOf(id,expected.revision.toString()))==1
            check(removed);db.setTransactionSuccessful();removed
        } finally {db.endTransaction()}
    }
    private fun load(db: SQLiteDatabase,id: String): PayrollRecord? = db.rawQuery("SELECT * FROM payroll_records WHERE id=?",arrayOf(id)).use { c ->
        if(!c.moveToFirst()) return@use null
        fun text(name: String)=c.getString(c.getColumnIndexOrThrow(name))
        fun number(name: String)=c.getLong(c.getColumnIndexOrThrow(name))
        val lines=db.rawQuery("SELECT * FROM payroll_lines WHERE payroll_id=? ORDER BY position",arrayOf(id)).use {l -> buildList {
            while(l.moveToNext()) {
                check(l.getInt(l.getColumnIndexOrThrow("position"))==size) { "Payroll lines are incomplete." }
                add(PayrollLine(l.getString(l.getColumnIndexOrThrow("source_label")),
                    PayrollLineType.valueOf(l.getString(l.getColumnIndexOrThrow("semantic_type"))),
                    PayrollLineRole.valueOf(l.getString(l.getColumnIndexOrThrow("role"))),l.getLong(l.getColumnIndexOrThrow("amount_minor"))))
            }
        } }
        val draft=PayrollDraft(text("employer"),text("payroll_month"),number("gross_minor"),number("net_minor"),PayrollSource.valueOf(text("source")),lines)
        PayrollJournalPolicy.validate(draft)
        check(PayrollJournalPolicy.periodKey(draft)==text("period_key") && PayrollJournalPolicy.contentKey(draft)==text("content_key")) { "Payroll record needs recovery; no cash was changed." }
        PayrollRecord(id,number("revision"),draft,number("created_at_epoch_ms"),number("updated_at_epoch_ms"))
    }
}

