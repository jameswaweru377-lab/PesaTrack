package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import android.content.ContentValues
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.kedevjimmy.pesatrack.domain.model.AccountType

/** Disposable emulator databases and invented values only. */
@RunWith(AndroidJUnit4::class)
class StatementMatchDecisionStoreTest {
    private lateinit var app: Context
    @Before fun setup() {
        app = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
        app.deleteDatabase("pesatrack_statement_activity.db")
    }
    @After fun cleanup() {
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
        app.deleteDatabase("pesatrack_statement_activity.db")
    }
    @Test fun nonexistentSuggestionCannotBeConfirmedOrChangeActivity() {
        val store = StatementMatchDecisionStore(app)
        assertEquals(StatementMatchDecisionStore.Result.STALE,
            store.decide(2026, "a".repeat(64), StatementMatchDecisionStore.Kind.TRANSFER,
                null, StatementMatchDecisionStore.Decision.CONFIRMED))
        val helper = StatementArchiveDb(app)
        try { assertEquals(0L, helper.readableDatabase.compileStatement(
            "SELECT COUNT(*) FROM statement_match_decisions_v1").simpleQueryForLong()) }
        finally { helper.close() }
        assertEquals(0L, PesaTrackDatabase.get(app).readableDatabase.compileStatement(
            "SELECT COUNT(*) FROM transactions").simpleQueryForLong())
    }

    @Test fun confirmationIsInvalidatedWhenOwnershipRevisionChanges() {
        val accounts = AccountStore.get(app)
        val bank = accounts.save("Invented source", AccountType.BANK)
        val other = accounts.save("Invented destination", AccountType.BANK)
        accounts.updateDetails(bank.id, "001234567890", null)
        accounts.updateDetails(other.id, "009876543210", null)
        val consent = AccountOwnershipConsentStore(app)
        consent.recordConsent(bank.id, true)
        consent.recordConsent(other.id, true)
        val helper = StatementArchiveDb(app)
        try {
            val db = helper.writableDatabase
            fun insert(id: String, account: String, movement: Long) {
                db.insertOrThrow("statement_sources_v2", null, ContentValues().apply {
                    put("id", id); put("account_id", account); put("provider_key", "KENYAN_BANK_EQUITY")
                    val number = if (account == bank.id) "001234567890" else "009876543210"
                    val identityHash = java.security.MessageDigest.getInstance("SHA-256")
                        .digest(number.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
                    put("account_identity_hash", identityHash); put("currency", "KES"); put("document_format", "PDF")
                    put("imported_at", 1_780_000_000_000L); put("first_at", 1_780_000_000_000L)
                    put("last_at", 1_780_000_000_000L); put("row_count", 1)
                    put("money_in_minor", if (movement > 0) movement else 0L)
                    put("money_out_minor", if (movement < 0) -movement else 0L); put("totals_verified", 1)
                })
                db.insertOrThrow("statement_legs_v2", null, ContentValues().apply {
                    put("source_id", id); put("leg_number", 0); put("page_number", 1); put("row_number", 1)
                    put("occurred_at", 1_780_000_000_000L); put("description", "Invented movement")
                    put("reference", "ABC1234567"); put("movement_minor", movement); put("running_balance_minor", 1_000L)
                    put("source_cells_json", "{\"headers\":[\"Date\"],\"cells\":[\"Invented\"]}")
                })
            }
            insert("a".repeat(64), bank.id, -500L)
            insert("b".repeat(64), other.id, 500L)
        } finally { helper.close() }
        val writerStarted = java.util.concurrent.CountDownLatch(1)
        val writerFinished = java.util.concurrent.CountDownLatch(1)
        val writerFailure = java.util.concurrent.atomic.AtomicReference<Throwable?>()
        var writer: Thread? = null
        var testedLock = false
        val store = StatementMatchDecisionStore(app) {
            if (!testedLock) {
                testedLock = true
                writer = Thread {
                    val other = StatementArchiveDb(app)
                    try {
                        val otherDb = other.writableDatabase
                        writerStarted.countDown()
                        otherDb.execSQL("UPDATE statement_sources_v2 SET imported_at = imported_at + 1")
                    } catch (t: Throwable) { writerFailure.set(t) }
                    finally { other.close(); writerFinished.countDown() }
                }.also { it.start() }
                assertTrue(writerStarted.await(3, java.util.concurrent.TimeUnit.SECONDS))
                assertFalse("Another archive writer must not finish while evidence is being saved",
                    writerFinished.await(300, java.util.concurrent.TimeUnit.MILLISECONDS))
            }
        }
        val candidate = store.read(2026).candidates.single()
        assertEquals(StatementMatchDecisionStore.Result.SAVED,
            store.decide(2026, candidate.hash, candidate.kind, null, StatementMatchDecisionStore.Decision.CONFIRMED))
        assertTrue(writerFinished.await(5, java.util.concurrent.TimeUnit.SECONDS))
        writer?.join()
        assertEquals(null, writerFailure.get())
        assertEquals(StatementMatchDecisionStore.Decision.CONFIRMED, store.read(2026).decisions[candidate.hash])
        assertEquals(StatementMatchDecisionStore.Result.STALE,
            store.decide(2026, candidate.hash, candidate.kind, null, StatementMatchDecisionStore.Decision.NOT_SAME))
        assertEquals(StatementMatchDecisionStore.Result.STALE,
            store.decide(2026, candidate.hash, candidate.kind, null, null))
        assertEquals(StatementMatchDecisionStore.Decision.CONFIRMED, store.read(2026).decisions[candidate.hash])
        accounts.updateDetails(other.id, "008888888888", null)
        assertTrue(store.read(2026).decisions.isEmpty())
        assertEquals(StatementMatchDecisionStore.Result.STALE,
            store.decide(2026, candidate.hash, candidate.kind, StatementMatchDecisionStore.Decision.CONFIRMED, null))
        accounts.updateDetails(other.id, "009876543210", null)
        consent.recordConsent(other.id, true)
        assertTrue(store.read(2026).decisions.isEmpty())
        assertEquals(StatementMatchDecisionStore.Result.STALE,
            store.decide(2026, candidate.hash, candidate.kind, null, StatementMatchDecisionStore.Decision.CONFIRMED))
        assertEquals(0L, PesaTrackDatabase.get(app).readableDatabase.compileStatement(
            "SELECT COUNT(*) FROM transactions").simpleQueryForLong())
    }

    @Test fun walletStatementNeedsExplicitAccountAttestationAndInvalidatesOnNumberChange() {
        val accounts = AccountStore.get(app)
        val bank = accounts.save("Invented bank", AccountType.BANK)
        val wallet = accounts.save("Invented wallet", AccountType.MOBILE_WALLET)
        accounts.updateDetails(bank.id, "001234567890", null)
        accounts.updateDetails(wallet.id, "0712345678", null)
        AccountOwnershipConsentStore(app).apply { recordConsent(bank.id, true); recordConsent(wallet.id, true) }
        val helper = StatementArchiveDb(app)
        try {
            val db = helper.writableDatabase
            val bankId = "a".repeat(64)
            val walletId = "b".repeat(64)
            val identityHash = java.security.MessageDigest.getInstance("SHA-256")
                .digest("001234567890".toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            db.insertOrThrow("statement_sources_v2", null, ContentValues().apply {
                put("id", bankId); put("account_id", bank.id); put("provider_key", "KENYAN_BANK_EQUITY")
                put("account_identity_hash", identityHash); put("currency", "KES"); put("document_format", "PDF")
                put("imported_at", 1_780_000_000_000L); put("first_at", 1_780_000_000_000L)
                put("last_at", 1_780_000_000_000L); put("row_count", 1)
                put("money_in_minor", 0L); put("money_out_minor", 500L); put("totals_verified", 1)
            })
            db.insertOrThrow("statement_legs_v2", null, ContentValues().apply {
                put("source_id", bankId); put("leg_number", 0); put("page_number", 1); put("row_number", 1)
                put("occurred_at", 1_780_000_000_000L); put("description", "Invented transfer")
                put("reference", "ABC1234567"); put("movement_minor", -500L); put("running_balance_minor", 1_000L)
                put("source_cells_json", "{\"headers\":[\"Date\"],\"cells\":[\"Invented\"]}")
            })
            db.insertOrThrow("statement_batches", null, ContentValues().apply {
                put("id", walletId); put("account_id", wallet.id); put("recorded_at", 1_780_000_000_000L)
                put("row_count", 1); put("movement_count", 1); put("informational_count", 0)
                put("paid_in_minor", 500L); put("paid_out_minor", 0L); put("totals_verified", 1)
            })
            db.insertOrThrow("statement_activity_lines", null, ContentValues().apply {
                put("batch_id", walletId); put("line_number", 0); put("page_number", 1); put("row_number", 1)
                put("reference", "ABC1234567"); put("occurred_at", 1_780_000_000_000L)
                put("description", "Invented receipt"); put("paid_in_minor", 500L); put("withdrawn_minor", 0L)
                put("balance_minor", 1_000L); put("source_cells_json", "[]")
            })
        } finally { helper.close() }
        val store = StatementMatchDecisionStore(app)
        val candidate = store.read(2026).candidates.single()
        assertTrue(candidate.needsAccountAttestation)
        val decisionDb = StatementArchiveDb(app)
        try { decisionDb.writableDatabase.insertOrThrow("statement_match_decisions_v1", null, ContentValues().apply {
            put("evidence_hash", candidate.hash); put("kind", candidate.kind.name)
            put("decision", "CONFIRMED"); put("account_attested", 0); put("updated_at", 1_780_000_000_000L)
        }) } finally { decisionDb.close() }
        assertTrue(store.read(2026).decisions.isEmpty())
        assertEquals(StatementMatchDecisionStore.Result.ACCOUNT_CHECK_REQUIRED,
            store.decide(2026, candidate.hash, candidate.kind, null, StatementMatchDecisionStore.Decision.CONFIRMED))
        assertTrue(store.read(2026).decisions.isEmpty())
        assertEquals(StatementMatchDecisionStore.Result.SAVED,
            store.decide(2026, candidate.hash, candidate.kind, null, StatementMatchDecisionStore.Decision.CONFIRMED, true))
        assertEquals(StatementMatchDecisionStore.Decision.CONFIRMED, store.read(2026).decisions[candidate.hash])
        accounts.updateDetails(wallet.id, "0799999999", null)
        assertTrue(store.read(2026).decisions.isEmpty())
        assertEquals(StatementMatchDecisionStore.Result.STALE,
            store.decide(2026, candidate.hash, candidate.kind, StatementMatchDecisionStore.Decision.CONFIRMED, null))
    }
}
