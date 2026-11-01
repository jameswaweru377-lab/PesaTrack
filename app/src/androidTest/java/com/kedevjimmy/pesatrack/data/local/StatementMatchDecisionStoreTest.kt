package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import android.content.ContentValues
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
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
        val store = StatementMatchDecisionStore(app)
        val candidate = store.read(2026).candidates.single()
        assertEquals(StatementMatchDecisionStore.Result.SAVED,
            store.decide(2026, candidate.hash, candidate.kind, null, StatementMatchDecisionStore.Decision.CONFIRMED))
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
}
