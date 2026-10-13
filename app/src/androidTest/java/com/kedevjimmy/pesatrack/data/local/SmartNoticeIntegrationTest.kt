package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.interpretation.*
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.rules.*
import com.kedevjimmy.pesatrack.planning.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Invented local fixtures. CI compiles these; emulator execution remains explicitly opt-in. */
@RunWith(AndroidJUnit4::class)
class SmartNoticeIntegrationTest {
    private lateinit var context: Context
    private val now get() = System.currentTimeMillis()
    private val db get() = PesaTrackDatabase.get(context).writableDatabase
    private val prefs get() = context.getSharedPreferences("review_attention_reminders",Context.MODE_PRIVATE)
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        prefs.edit().clear().commit()
        context.getSharedPreferences("pesatrack_preferences",Context.MODE_PRIVATE).edit().putBoolean("setup_complete",true).commit()
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
        SmartNoticeCoordinator.foreground = true
    }
    @After fun cleanup() {
        prefs.edit().clear().commit()
        SmartNoticeCoordinator.cancel(context)
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
    }
    private fun read() = synchronized(CanonicalStoreLock.monitor) { SmartNoticeCandidates.read(context,now) }
    private fun row(index: Int, direction: String, source: String = "financial_notification"): Long {
        val time = now - 3 * 60 * 60_000L + index
        return db.insertOrThrow("transactions",null,ContentValues().apply {
            put("fingerprint","notice-synthetic-$index"); put("transaction_code","NOTICETEST$index")
            put("type",if (direction == "EXPENSE") "SENT_MONEY" else "RECEIVED")
            put("amount_minor",50_000L); put("fee_minor",0L)
            put("counterparty","Sample Borrower"); put("counterparty_phone","0712000000")
            put("occurred_at_epoch_ms",time); put("received_at_epoch_ms",time); put("created_at_epoch_ms",time)
            put("source_kind",source); put("direction",direction); put("category",if (direction == "INCOME") "INCOME" else "OTHER")
            put("review_state","AUTO_CLASSIFIED"); put("account_id",AccountStore.DEFAULT_MPESA_ID)
        })
    }
    private fun person(): ReceivablePerson {
        val journal = PersonalReceivableStore(context)
        journal.record(null,"Sample Borrower","0712000000","LENT",200_000,now - 2 * ReviewReminderPolicy.DAY_MS)
        prefs.edit().putBoolean("payment_enabled",true).putLong("payment_enabled_at",now - ReviewReminderPolicy.DAY_MS).commit()
        // History existed before the new synthetic payment was observed.
        db.update("personal_receivable_events",ContentValues().apply { put("created_at_epoch_ms",now - 2 * ReviewReminderPolicy.DAY_MS) },null,null)
        return journal.people().single()
    }
    @Test fun legacyActivityPauseSurvivesUpgradeAndPreferencesRestart() {
        val time = now
        prefs.edit().putBoolean("activity_enabled",true).putLong("last_shown",time - 2 * ReviewReminderPolicy.DAY_MS)
            .putString("last_fingerprint","3,7").commit()
        val metadata = SmartNoticePreferences(context)
        assertTrue(metadata.enabled(SmartNoticeKind.ACTIVITY))
        val candidate = SmartNoticeCandidate("activity",SmartNoticePreferences.hash("3,7"),SmartNoticeKind.ACTIVITY,
            time - 3 * 60 * 60_000L)
        assertNull(SmartNotificationPolicy.choose(listOf(candidate),mapOf("activity" to metadata.state("activity")),
            time,11,metadata.lastBanner(),0,true,true,false,false))
        metadata.dismissed("activity",candidate.revision)
        assertTrue(SmartNoticePreferences(context).state("activity").dismissed)
    }
    @Test fun accountNeedsMaterialEvidenceAndCompletesWithoutNotificationWrites() {
        SmartNoticePreferences(context).setEnabled(SmartNoticeKind.ACCOUNT,true)
        val financial = FinancialUnderstandingStore.get(context)
        val account = FinancialAccountReference("synthetic:notice-bank","Sample Bank","SYNTHETIC",
            AccountType.BANK,AccountOwnershipHint.UNKNOWN,FinancialAccountNature.ASSET)
        financial.observeEntity(account,now - ReviewReminderPolicy.DAY_MS)
        assertTrue(read().none { it.candidate.kind == SmartNoticeKind.ACCOUNT })
        financial.recordBalance(BalanceObservationFact(account,100_000,InterpretationConfidence.LOW),"weak",now - 10_000,null,"TEST")
        assertTrue(read().none { it.candidate.kind == SmartNoticeKind.ACCOUNT })
        financial.recordBalance(BalanceObservationFact(account,100_000,InterpretationConfidence.HIGH),"strong",now - 5_000,null,"TEST")
        val notice = read().single { it.candidate.kind == SmartNoticeKind.ACCOUNT }
        assertEquals(account.entityKey,notice.entityKey)
        assertTrue(financial.pendingEntities().any { it.entityKey == account.entityKey })
        assertEquals(notice,SmartNoticeCoordinator.resolve(context,notice.candidate.key,notice.candidate.revision))
        financial.confirmEntityOwned(account.entityKey)
        assertNull(SmartNoticeCoordinator.resolve(context,notice.candidate.key,notice.candidate.revision))
    }
    @Test fun paymentCooldownKeepsPostedReviewUntilConfirmedAndNeverWritesOnRead() {
        val person = person()
        val id = row(1,"INCOME")
        val before = TransactionStore.get(context).getById(id)!!
        val notice = read().single { it.candidate.kind == SmartNoticeKind.PAYMENT }
        val metadata = SmartNoticePreferences(context)
        metadata.shown(notice.candidate,now); metadata.personShown(person.id,now)
        assertTrue(read().single { it.candidate.kind == SmartNoticeKind.PAYMENT }.candidate.enabled)
        SmartNoticeCoordinator.check(context,allowPost = false)
        assertEquals(notice.candidate.key,metadata.postedKey())
        assertEquals(200_000L,PersonalReceivableStore(context).people().single().outstandingMinor)
        assertEquals(before,TransactionStore.get(context).getById(id))
        PersonalReceivableStore(context).record(person.id,person.name,person.phone,"REPAID",50_000,
            before.occurredAtEpochMs,id,expectedTransaction = before)
        SmartNoticeCoordinator.check(context,allowPost = false)
        assertNull(metadata.postedKey())
        assertNull(SmartNoticeCoordinator.resolve(context,notice.candidate.key,notice.candidate.revision))
        assertEquals(150_000L,PersonalReceivableStore(context).people().single().outstandingMinor)
        assertEquals(before,TransactionStore.get(context).getById(id))
    }
    @Test fun historicalReplayStaleAmountAndGiftDecisionCannotChangeLedger() {
        person(); row(2,"EXPENSE","historical_sms")
        assertTrue(read().none { it.candidate.kind == SmartNoticeKind.PAYMENT })
        val id = row(3,"EXPENSE")
        val notice = read().single { it.candidate.kind == SmartNoticeKind.PAYMENT }
        val metadata = SmartNoticePreferences(context)
        metadata.dismissed(notice.candidate.key,notice.candidate.revision)
        assertNull(SmartNoticeCoordinator.resolve(context,notice.candidate.key,notice.candidate.revision))
        assertEquals(200_000L,PersonalReceivableStore(context).people().single().outstandingMinor)
        metadata.undoDismissal(notice.candidate.key)
        db.update("transactions",ContentValues().apply { put("amount_minor",60_000L) },"id = ?",arrayOf(id.toString()))
        assertNull(SmartNoticeCoordinator.resolve(context,notice.candidate.key,notice.candidate.revision))
    }
    @Test fun discoveryVisitAndDismissalSurviveNewPreferenceInstance() {
        val metadata = SmartNoticePreferences(context)
        assertTrue(metadata.discoveryPending())
        metadata.completeDiscovery()
        assertFalse(SmartNoticePreferences(context).discoveryPending())
        metadata.resetDiscovery()
        assertTrue(SmartNoticePreferences(context).discoveryPending())
    }
    @Test fun failedCancelledAndInterruptedImportSuppressPostedAlertsUntilSuccessfulImport() {
        val metadata = SmartNoticePreferences(context)
        metadata.setEnabled(SmartNoticeKind.DISCOVERY,true)
        metadata.importStarted()
        assertFalse(SmartNoticePreferences(context).importReady())
        metadata.importFinished(false)
        metadata.shown(SmartNoticeCandidate("discovery","1",SmartNoticeKind.DISCOVERY,now),now)
        SmartNoticeCoordinator.check(context,allowPost = false)
        assertNull(metadata.postedKey())
        assertFalse(SmartNoticePreferences(context).importReady())
        metadata.importFinished(true)
        assertTrue(SmartNoticePreferences(context).importReady())
    }
}
