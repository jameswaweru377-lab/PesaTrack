package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Compiled in normal CI; do NOT execute without a separate authorization for instrumentation. */
@RunWith(AndroidJUnit4::class)
class EvidenceJournalIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val epoch = 1_790_000_000_000L

    private fun seed(db: android.database.sqlite.SQLiteDatabase, code: String = "SYNTHETIC01",
        account: String = "account-mpesa", direction: String = "INCOME"): Long {
        return db.insertOrThrow("transactions",null,ContentValues().apply {
            put("fingerprint","phase-b-"+code+"-"+account+"-"+direction)
            put("transaction_code",code);put("type","OTHER");put("amount_minor",5_200_000L)
            put("fee_minor",0L);put("occurred_at_epoch_ms",epoch);put("received_at_epoch_ms",epoch)
            put("source_kind","sms");put("created_at_epoch_ms",epoch)
            put("account_id",account);put("direction",direction)
        })
    }

    @Test fun linkedStatementIsEvidenceOnlyAndReplayIsIdempotent() {
        val helper = PesaTrackDatabase(context)
        try {
            val db=helper.writableDatabase
            val id=seed(db)
            val before=db.rawQuery("SELECT count(*) FROM transactions",null).use {it.moveToFirst();it.getInt(0)}
            repeat(2) {
                db.beginTransaction()
                try {
                    EvidenceJournalStore.observe(db,"STATEMENT","account-mpesa","fixture001",
                        "account-mpesa",5_200_000L,epoch,"SYNTHETIC01",id,"REFERENCE")
                    db.setTransactionSuccessful()
                } finally {db.endTransaction()}
            }
            assertEquals(before,db.rawQuery("SELECT count(*) FROM transactions",null).use {it.moveToFirst();it.getInt(0)})
            assertEquals(1,db.rawQuery("SELECT count(*) FROM evidence_links",null).use {it.moveToFirst();it.getInt(0)})
            assertEquals(1,db.rawQuery("SELECT count(*) FROM evidence_link_audit",null).use {it.moveToFirst();it.getInt(0)})
        } finally {helper.close()}
    }
    @Test fun falseAccountMatchNeverLinksOrAltersCash() {
        val helper=PesaTrackDatabase(context)
        try {
            val db=helper.writableDatabase
            val id=seed(db,"ANOTHER002","account-cash")
            db.beginTransaction()
            try {
                EvidenceJournalStore.observe(db,"STATEMENT","account-mpesa","fixture002",
                    "account-mpesa",5_200_000L,epoch,"ANOTHER002",id,"REFERENCE")
                db.setTransactionSuccessful()
            } finally {db.endTransaction()}
            assertEquals(0,db.rawQuery("SELECT count(*) FROM evidence_links",null).use {it.moveToFirst();it.getInt(0)})
        } finally {helper.close()}
    }
}
