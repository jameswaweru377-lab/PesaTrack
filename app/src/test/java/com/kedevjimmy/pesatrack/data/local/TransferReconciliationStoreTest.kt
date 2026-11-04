package com.kedevjimmy.pesatrack.data.local

import android.app.Application
import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.statements.TransferDecisionPolicy.Decision
import com.kedevjimmy.pesatrack.domain.statements.TransferDecisionPolicy.Result
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.security.MessageDigest

/** No device: Robolectric owns disposable synthetic databases for every test. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class TransferReconciliationStoreTest {
    private lateinit var app: Context
    private lateinit var bank: String
    private lateinit var destination: String
    private val at = 1_780_000_000_000L

    @Before fun setup() {
        app = RuntimeEnvironment.getApplication()
        PesaTrackDatabase.resetForTests()
        resetCachedStores()
        app.deleteDatabase("pesatrack.db")
        app.deleteDatabase("pesatrack_statement_activity.db")
        val accounts = AccountStore.get(app)
        bank = accounts.save("Synthetic source", AccountType.BANK).id
        destination = accounts.save("Synthetic destination", AccountType.BANK).id
        accounts.updateDetails(bank, "001234567890", null)
        accounts.updateDetails(destination, "009876543210", null)
        AccountOwnershipConsentStore(app).recordConsent(bank, true)
        AccountOwnershipConsentStore(app).recordConsent(destination, true)
        insert("a".repeat(64), bank, "001234567890", -50000, "Transfer to account 009876543210", null)
        insert("b".repeat(64), destination, "009876543210", 50000, "Receipt", null)
    }

    @After fun cleanup() {
        PesaTrackDatabase.resetForTests()
        resetCachedStores()
        app.deleteDatabase("pesatrack.db")
        app.deleteDatabase("pesatrack_statement_activity.db")
    }

    private fun resetCachedStores() {
        // Robolectric recreates the Application between tests, unlike a running app.
        // These existing process singletons otherwise retain a retired fixture database.
        for (type in listOf(AccountStore::class.java, FinancialUnderstandingStore::class.java)) {
            type.getDeclaredField("instance").apply { isAccessible = true }.set(null, null)
        }
    }

    @Test fun confirmReopenRejectUndoPreservesFinancialEvidence() {
        val store = TransferReconciliationStore(app)
        val pair = store.read(2026).reconciliation.pairs.single()
        val before = financialEvidence()
        assertEquals(Result.ACCOUNT_CHECK_REQUIRED, store.decide(2026, pair.fingerprint, null, Decision.CONFIRMED, false))
        assertEquals(Result.SAVED, store.decide(2026, pair.fingerprint, null, Decision.CONFIRMED, true))
        assertEquals(Decision.CONFIRMED, TransferReconciliationStore(app).read(2026).decisions[pair.fingerprint])
        assertEquals(Result.STALE, store.decide(2026, pair.fingerprint, null, Decision.NOT_SAME, false))
        assertEquals(Result.SAVED, store.decide(2026, pair.fingerprint, Decision.CONFIRMED, null, false))
        assertEquals(Result.SAVED, store.decide(2026, pair.fingerprint, null, Decision.NOT_SAME, false))
        assertEquals(Decision.NOT_SAME, store.read(2026).decisions[pair.fingerprint])
        assertEquals(Result.SAVED, store.decide(2026, pair.fingerprint, Decision.NOT_SAME, null, false))
        assertTrue(store.read(2026).decisions.isEmpty())
        assertEquals(before, financialEvidence())
    }

    @Test fun narrationChangeInvalidatesSavedDecision() {
        val store = TransferReconciliationStore(app)
        val pair = store.read(2026).reconciliation.pairs.single()
        assertEquals(Result.SAVED, store.decide(2026, pair.fingerprint, null, Decision.CONFIRMED, true))
        archive { it.execSQL("UPDATE statement_legs_v2 SET description=description || ' revised' WHERE movement_minor<0") }
        assertTrue(store.read(2026).decisions.isEmpty())
        assertEquals(Result.STALE, store.decide(2026, pair.fingerprint, Decision.CONFIRMED, null, false))
    }

    @Test fun ownershipRevisionInvalidatesSavedDecision() {
        val store = TransferReconciliationStore(app)
        val pair = store.read(2026).reconciliation.pairs.single()
        assertEquals(Result.SAVED, store.decide(2026, pair.fingerprint, null, Decision.CONFIRMED, true))
        AccountOwnershipConsentStore(app).recordConsent(destination, true)
        assertTrue(store.read(2026).decisions.isEmpty())
    }

    @Test fun legacyAndNewReviewsCannotBothConsumeMovement() {
        archive { it.execSQL("UPDATE statement_legs_v2 SET reference='SYN123456789'") }
        val store = TransferReconciliationStore(app)
        val pair = store.read(2026).reconciliation.pairs.single()
        val legacy = StatementMatchDecisionStore(app)
        val candidate = legacy.read(2026).candidates.single()
        assertEquals(Result.SAVED, store.decide(2026, pair.fingerprint, null, Decision.CONFIRMED, true))
        assertEquals(StatementMatchDecisionStore.Result.CONFLICT, legacy.decide(2026, candidate.hash,
            candidate.kind, null, StatementMatchDecisionStore.Decision.CONFIRMED, true))
        assertEquals(Result.SAVED, store.decide(2026, pair.fingerprint, Decision.CONFIRMED, null, false))
        assertEquals(StatementMatchDecisionStore.Result.SAVED, legacy.decide(2026, candidate.hash,
            candidate.kind, null, StatementMatchDecisionStore.Decision.CONFIRMED, true))
        assertEquals(Result.CONFLICT, store.decide(2026, pair.fingerprint, null, Decision.CONFIRMED, true))
    }

    @Test fun amountAndTimeWithoutRouteOrReferenceDoNotCreateCandidate() {
        archive { it.execSQL("UPDATE statement_legs_v2 SET description='Synthetic ordinary transaction'") }
        assertTrue(TransferReconciliationStore(app).read(2026).reconciliation.pairs.isEmpty())
    }

    @Test fun changedBankIdentifierNeedsReviewButDoesNotEraseHistoricalSourceOrAmounts() {
        val reader = StatementBankReportReader(app)
        val beforeMonths = reader.monthsFor(bank)
        val original = reader.identityReviewFor(bank, "0012 3456-7890", 2026)
        assertEquals(setOf("a".repeat(64)), original.matchedSourceIds)
        assertTrue(original.mismatchedSourceIds.isEmpty())

        AccountStore.get(app).updateDetails(bank, "001111222233", null)
        val changed = reader.identityReviewFor(bank, "001111222233", 2026)
        assertEquals(setOf("a".repeat(64)), changed.mismatchedSourceIds)
        assertTrue(changed.unverifiableSourceIds.isEmpty())
        assertFalse(reader.identityReviewFor(bank, "001111222233", 2025).needsReview)
        assertEquals(setOf("a".repeat(64)),
            reader.identityReviewFor(bank, null, 2026).unverifiableSourceIds)
        assertEquals(beforeMonths, reader.monthsFor(bank))
        assertEquals(1, StatementBankArchiveStore(app).sourcesFor(bank).size)
        assertTrue(TransferReconciliationStore(app).read(2026).reconciliation.pairs.isEmpty())
    }

    @Test fun previousYearCopyConfirmationBlocksJanuaryTransferUsingSameLeg() {
        insert("c".repeat(64), bank, "001234567890", -50000, "Transfer to account 009876543210", null)
        val december = java.time.ZonedDateTime.of(2025, 12, 31, 23, 50, 0, 0,
            java.time.ZoneId.of("Africa/Nairobi")).toInstant().toEpochMilli()
        archive { db ->
            db.execSQL("UPDATE statement_legs_v2 SET occurred_at=?,reference='SYN123456789' WHERE movement_minor<0", arrayOf(december))
            db.execSQL("UPDATE statement_legs_v2 SET occurred_at=? WHERE movement_minor>0", arrayOf(december + 20 * 60 * 1000))
            db.execSQL("UPDATE statement_sources_v2 SET first_at=(SELECT occurred_at FROM statement_legs_v2 WHERE source_id=id),last_at=(SELECT occurred_at FROM statement_legs_v2 WHERE source_id=id)")
        }
        val legacy = StatementMatchDecisionStore(app)
        val copy = legacy.read(2025).candidates.single()
        assertEquals(StatementMatchDecisionStore.Kind.SAME_MOVEMENT, copy.kind)
        assertEquals(StatementMatchDecisionStore.Result.SAVED, legacy.decide(2025, copy.hash,
            copy.kind, null, StatementMatchDecisionStore.Decision.CONFIRMED, true))
        val store = TransferReconciliationStore(app)
        val pair = store.read(2026).reconciliation.pairs.single()
        assertEquals(Result.CONFLICT, store.decide(2026, pair.fingerprint, null, Decision.CONFIRMED, true))
    }

    @Test fun userCategoryAndLoanNarrationWithholdCandidate() {
        archive { db -> db.execSQL("UPDATE statement_legs_v2 SET description='Loan repayment to account 009876543210' WHERE movement_minor<0") }
        assertTrue(TransferReconciliationStore(app).read(2026).reconciliation.pairs.isEmpty())
        archive { db ->
            db.execSQL("UPDATE statement_legs_v2 SET description='Transfer to account 009876543210' WHERE movement_minor<0")
            db.insertOrThrow("statement_category_labels_v2", null, ContentValues().apply {
                put("source_id", "a".repeat(64)); put("leg_number", 0); put("category_id", "user-choice")
            })
        }
        assertTrue(TransferReconciliationStore(app).read(2026).reconciliation.pairs.isEmpty())
    }

    @Test fun specializedStatementPaymentsCannotBeSuggestedAsOwnTransfersInEitherReview() {
        archive { it.execSQL("UPDATE statement_legs_v2 SET reference='SYN123456789'") }
        val originalFinancialEvidence = financialEvidence()
        val specializedNarrations = listOf(
            "Fuliza repayment to account 009876543210",
            "M-Shwari savings deposit to account 009876543210",
            "Transaction fee to account 009876543210",
            "Payment refund to account 009876543210",
            "Reversal to account 009876543210",
            "Clearance payment to account 009876543210",
            "Settlement payment to account 009876543210"
        )
        for (description in specializedNarrations) {
            archive { db ->
                db.execSQL("UPDATE statement_legs_v2 SET description=? WHERE movement_minor<0", arrayOf(description))
            }
            val before = financialEvidence()
            val legacy = StatementMatchDecisionStore(app).read(2026)
            assertTrue("Legacy review offered transfer for $description",
                legacy.candidates.none { it.kind == StatementMatchDecisionStore.Kind.TRANSFER })
            assertTrue("Legacy review offered weak transfer lead for $description",
                legacy.possibleTransfers.isEmpty())
            assertTrue("New review offered transfer for $description",
                TransferReconciliationStore(app).read(2026).reconciliation.pairs.isEmpty())
            assertEquals("Review must not post or rewrite financial evidence", before, financialEvidence())
        }
        assertEquals(originalFinancialEvidence.size, financialEvidence().size)
    }

    @Test fun userClassifiedStatementCannotBecomeLegacyOwnTransfer() {
        archive { db ->
            db.execSQL("UPDATE statement_legs_v2 SET reference='SYN123456789'")
            db.insertOrThrow("statement_category_labels_v2", null, ContentValues().apply {
                put("source_id", "a".repeat(64)); put("leg_number", 0); put("category_id", "user-choice")
            })
        }
        val before = financialEvidence()
        val legacy = StatementMatchDecisionStore(app).read(2026)
        assertTrue(legacy.candidates.none { it.kind == StatementMatchDecisionStore.Kind.TRANSFER })
        assertTrue(legacy.possibleTransfers.isEmpty())
        assertEquals(before, financialEvidence())
    }

    @Test fun feeAndChargebackStatementRowsCannotBecomeLegacyOrNewTransferSuggestions() {
        archive { it.execSQL("UPDATE statement_legs_v2 SET reference='SYN123456789'") }
        for (description in listOf(
            "Commission charged to account 009876543210",
            "Excise duty to account 009876543210",
            "Chargeback to account 009876543210",
            "Bank levy to account 009876543210"
        )) {
            archive { it.execSQL("UPDATE statement_legs_v2 SET description=? WHERE movement_minor<0",
                arrayOf(description)) }
            val before = financialEvidence()
            val legacy = StatementMatchDecisionStore(app).read(2026)
            assertTrue("Legacy transfer match from $description",
                legacy.candidates.none { it.kind == StatementMatchDecisionStore.Kind.TRANSFER })
            assertTrue("Legacy weak lead from $description", legacy.possibleTransfers.isEmpty())
            assertTrue("New transfer match from $description",
                TransferReconciliationStore(app).read(2026).reconciliation.pairs.isEmpty())
            assertEquals(before, financialEvidence())
        }
    }

    @Test fun lenderDisbursementAndOverdraftCannotBecomeLegacyOrNewTransferLeads() {
        archive { it.execSQL("UPDATE statement_legs_v2 SET reference='SYN123456789'") }
        for (description in listOf(
            "Funds disbursed to account 009876543210",
            "Disbursement to account 009876543210",
            "Overdraft to account 009876543210",
            "Write-off to account 009876543210"
        )) {
            archive { it.execSQL("UPDATE statement_legs_v2 SET description=? WHERE movement_minor<0",
                arrayOf(description)) }
            val before = financialEvidence()
            val legacy = StatementMatchDecisionStore(app).read(2026)
            assertTrue("Legacy false transfer from $description",
                legacy.candidates.none { it.kind == StatementMatchDecisionStore.Kind.TRANSFER })
            assertTrue("Legacy false weak lead from $description", legacy.possibleTransfers.isEmpty())
            val modern = TransferReconciliationStore(app).read(2026).reconciliation
            assertTrue("Modern false transfer from $description", modern.pairs.isEmpty())
            assertTrue("Modern false route from $description", modern.unmatchedRoutes.isEmpty())
            assertEquals(before, financialEvidence())
        }
    }

    @Test fun terseRepaidAndWrittenOffSourceLegsAreWithheldInBothReviewWorkspaces() {
        archive { it.execSQL("UPDATE statement_legs_v2 SET reference='SYN123456789'") }
        for (outgoing in listOf(true, false)) for (description in listOf("Repaid in full", "Written off")) {
            archive { db ->
                db.execSQL("""UPDATE statement_legs_v2 SET description=
                    CASE WHEN movement_minor<0 THEN 'Transfer to account 009876543210' ELSE 'Receipt' END""")
                db.execSQL("UPDATE statement_legs_v2 SET description=? WHERE movement_minor${if (outgoing) "<" else ">"}0",
                    arrayOf(description))
            }
            val before = financialEvidence()
            val legacy = StatementMatchDecisionStore(app).read(2026)
            assertTrue("Legacy false transfer: $description ($outgoing)",
                legacy.candidates.none { it.kind == StatementMatchDecisionStore.Kind.TRANSFER })
            assertTrue("Legacy false weak lead: $description ($outgoing)", legacy.possibleTransfers.isEmpty())
            val modern = TransferReconciliationStore(app).read(2026).reconciliation
            assertTrue("New false transfer: $description ($outgoing)", modern.pairs.isEmpty())
            val protectedAccount = if (outgoing) bank else destination
            assertTrue("Protected leg cannot become a route lead: $description ($outgoing)",
                modern.unmatchedRoutes.none { it.row.accountId == protectedAccount })
            assertEquals(before, financialEvidence())
        }
    }

    private fun insert(id: String, account: String, identifier: String, movement: Long, description: String, reference: String?) {
        archive { db ->
            db.insertOrThrow("statement_sources_v2", null, ContentValues().apply {
                put("id", id); put("account_id", account); put("provider_key", "KENYAN_BANK_EQUITY")
                put("account_identity_hash", MessageDigest.getInstance("SHA-256").digest(identifier.toByteArray())
                    .joinToString("") { "%02x".format(it) })
                put("currency", "KES"); put("document_format", "PDF"); put("imported_at", at)
                put("first_at", at); put("last_at", at); put("row_count", 1)
                put("money_in_minor", if (movement > 0) movement else 0L)
                put("money_out_minor", if (movement < 0) -movement else 0L); put("totals_verified", 1)
            })
            db.insertOrThrow("statement_legs_v2", null, ContentValues().apply {
                put("source_id", id); put("leg_number", 0); put("page_number", 1); put("row_number", 1)
                put("occurred_at", at); put("description", description); put("reference", reference)
                put("movement_minor", movement); put("running_balance_minor", 100000L)
                put("source_cells_json", "{\"headers\":[\"Date\"],\"cells\":[\"Synthetic\"]}")
            })
        }
    }

    private fun financialEvidence(): List<String> = buildList {
        archive { db ->
            for (table in listOf("statement_sources_v2", "statement_legs_v2")) {
                db.rawQuery("SELECT * FROM $table ORDER BY rowid", null).use { cursor ->
                    while (cursor.moveToNext()) add((0 until cursor.columnCount).joinToString("|") {
                        if (cursor.isNull(it)) "NULL" else cursor.getString(it)
                    })
                }
            }
        }
        add(PesaTrackDatabase.get(app).readableDatabase.compileStatement("SELECT COUNT(*) FROM transactions").simpleQueryForLong().toString())
    }

    private fun archive(block: (android.database.sqlite.SQLiteDatabase) -> Unit) {
        val helper = StatementArchiveDb(app)
        try { block(helper.writableDatabase) } finally { helper.close() }
    }
}
