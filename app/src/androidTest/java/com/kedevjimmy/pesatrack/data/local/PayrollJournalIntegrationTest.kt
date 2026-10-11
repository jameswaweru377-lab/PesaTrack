package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.payroll.*
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.security.EncryptedBackupFixture
import com.kedevjimmy.pesatrack.security.PesaBackupManager
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PayrollJournalIntegrationTest {
    private lateinit var context: Context
    private val password="synthetic-payroll-backup-passphrase"
    private fun draft(net: Long=5200000)=PayrollDraft("TEST SCHOOL LTD","2026-09",7000000,net,PayrollSource.MANUAL,
        listOf(PayrollLine("PAYE",PayrollLineType.TAX,PayrollLineRole.EMPLOYEE_DEDUCTION,7000000-net)))
    @Before fun setup() {context=ApplicationProvider.getApplicationContext();PesaTrackDatabase.resetForTests();context.deleteDatabase("pesatrack.db")}
    @After fun cleanup() {PesaTrackDatabase.resetForTests();context.deleteDatabase("pesatrack.db")}
    private fun seedCash(): String {
        val account=AccountStore.get(context).save("Synthetic Payroll Bank",AccountType.BANK)
        val db=PesaTrackDatabase.get(context).writableDatabase
        db.insertOrThrow("transactions",null,ContentValues().apply {
            put("fingerprint","payroll-existing-income");put("type","OTHER");put("amount_minor",5200000);put("fee_minor",100)
            put("occurred_at_epoch_ms",1000);put("received_at_epoch_ms",2000);put("created_at_epoch_ms",3000)
            put("source_kind","manual_entry");put("direction","INCOME");put("category","INCOME");put("review_state","USER_CLASSIFIED");put("account_id",account.id)
        });return account.id
    }
    private fun assertCash(id: String) {
        PesaTrackDatabase.get(context).readableDatabase.rawQuery("SELECT amount_minor,fee_minor,account_id,review_state FROM transactions WHERE fingerprint='payroll-existing-income'",null).use {
            assertTrue(it.moveToFirst());assertEquals(5200000L,it.getLong(0));assertEquals(100L,it.getLong(1));assertEquals(id,it.getString(2));assertEquals("USER_CLASSIFIED",it.getString(3))
        }
    }
    @Test fun payrollOnlyAndReplayNeverCreateCashOrBalances() {
        val account=seedCash();val store=PayrollJournalStore(context)
        val before=AccountStore.get(context).listAccounts()
        val record=store.save(draft(),true)
        assertEquals(record,store.save(draft(),true));assertEquals(listOf(record),store.records())
        assertEquals(before,AccountStore.get(context).listAccounts());assertCash(account)
        PesaTrackDatabase.get(context).readableDatabase.rawQuery("SELECT COUNT(*) FROM transactions",null).use {it.moveToFirst();assertEquals(1,it.getInt(0))}
        assertTrue(runCatching {store.save(draft(),false)}.isFailure)
    }
    @Test fun conflictingReplayAndStaleEditsCannotOverwriteUserCorrections() {
        val store=PayrollJournalStore(context);val first=store.save(draft(),true)
        assertTrue(runCatching {store.save(draft(5100000),true)}.isFailure)
        val corrected=store.save(draft(5100000),true,first)
        assertEquals(2L,corrected.revision)
        assertTrue(runCatching {store.save(draft(),true,first)}.isFailure)
        assertTrue(runCatching {store.remove(first)}.isFailure)
        assertEquals(corrected,store.records().single())
    }
    @Test fun restoredRevisionCannotAuthorizeAStaleSnapshot() {
        val store=PayrollJournalStore(context);val first=store.save(draft(),true)
        val corrected=store.save(draft(5100000),true,first)
        PesaTrackDatabase.get(context).writableDatabase.execSQL("UPDATE payroll_records SET revision=1 WHERE id=?",arrayOf(first.id))
        assertTrue(runCatching {store.save(draft(),true,first)}.isFailure)
        assertTrue(runCatching {store.remove(first)}.isFailure)
        assertEquals(corrected.draft,store.records().single().draft)
    }
    @Test fun failedLineInsertRollsBackHeaderAndFailedEditPreservesOldLines() {
        val store=PayrollJournalStore(context);val first=store.save(draft(),true)
        val db=PesaTrackDatabase.get(context).writableDatabase
        db.execSQL("CREATE TRIGGER synthetic_payroll_failure BEFORE INSERT ON payroll_lines BEGIN SELECT RAISE(ABORT,'synthetic'); END")
        assertTrue(runCatching {store.save(draft(5100000),true,first)}.isFailure)
        assertEquals(first,store.records().single())
        assertTrue(runCatching {store.save(draft().copy(period="2026-10"),true)}.isFailure)
        assertEquals(listOf(first),store.records())
    }
    @Test fun deleteOnlyJournalAndCurrentEncryptedRestorePreservesLabels() {
        val account=seedCash();val store=PayrollJournalStore(context)
        val data=draft().copy(lines=listOf(PayrollLine("NHIF",PayrollLineType.HEALTH,PayrollLineRole.EMPLOYEE_DEDUCTION,1800000)))
        val record=store.save(data,true);val manager=PesaBackupManager(context);val backup=manager.exportEncryptedBytes(password)
        store.remove(record);assertTrue(store.records().isEmpty());assertCash(account)
        manager.restoreEncryptedBytes(backup,password);assertEquals(record,store.records().single());assertCash(account)
    }
    @Test fun populatedV38UpgradePreservesCashAndConsentAndStartsEmptyJournal() {
        val account=seedCash();AccountOwnershipConsentStore(context).recordConsent(account,true)
        val legacy=PesaTrackDatabase.get(context);val db=legacy.writableDatabase
        db.execSQL("DROP TABLE payroll_lines");db.execSQL("DROP TABLE payroll_records");db.version=38
        legacy.close();PesaTrackDatabase.resetForTests()
        assertEquals(40,PesaTrackDatabase.get(context).writableDatabase.version)
        assertTrue(PayrollJournalStore(context).records().isEmpty());assertCash(account)
        assertEquals(1L,AccountOwnershipConsentStore(context).confirmedRevisions()[account])
    }
    @Test fun encryptedV34ThroughV38RestoresHaveNoInventedPayroll() {
        val account=seedCash();val manager=PesaBackupManager(context);val exported=manager.exportEncryptedBytes(password)
        for(version in 34..38) {
            val old=EncryptedBackupFixture.rewrite(exported,password) {payload ->
                payload.addProperty("schemaVersion",version);val t=payload.getAsJsonObject("tables")
                t.remove("payroll_records");t.remove("payroll_lines")
                t.remove("evidence_observations");t.remove("evidence_links");t.remove("evidence_link_audit")
                if(version<38){t.remove("account_ownership_consents");t.remove("owned_transfer_review_decisions")}
                if(version<37)t.remove("personal_receivable_review_exclusions")
                if(version<36){t.remove("personal_receivable_people");t.remove("personal_receivable_events")}
                if(version<35){t.remove("transaction_allocations");t.remove("flexible_budget_options");t.remove("statement_import_entries")}
            }
            PayrollJournalStore(context).save(draft(),true)
            assertEquals(40,manager.previewEncryptedBytes(old,password).schemaVersion)
            manager.restoreEncryptedBytes(old,password);assertTrue(PayrollJournalStore(context).records().isEmpty());assertCash(account)
        }
    }
    @Test fun malformedPayrollBackupRefusedBeforeChangingHistory() {
        val account=seedCash();val store=PayrollJournalStore(context);val original=store.save(draft(),true)
        val manager=PesaBackupManager(context)
        val bad=EncryptedBackupFixture.rewrite(manager.exportEncryptedBytes(password),password) {payload ->
            payload.getAsJsonObject("tables").getAsJsonArray("payroll_lines")[0].asJsonObject.getAsJsonObject("amount_minor").addProperty("value",1700000)
        }
        assertTrue(runCatching {manager.previewEncryptedBytes(bad,password)}.isFailure)
        assertTrue(runCatching {manager.restoreEncryptedBytes(bad,password)}.isFailure)
        assertEquals(original,store.records().single());assertCash(account)
    }
}

