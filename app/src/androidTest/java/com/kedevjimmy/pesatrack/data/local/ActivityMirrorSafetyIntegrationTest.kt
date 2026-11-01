package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable emulator database and invented records only. */
@RunWith(AndroidJUnit4::class)
class ActivityMirrorSafetyIntegrationTest {
    private lateinit var app: Context
    private lateinit var store: TransactionStore
    private val time = 1_780_000_000_000L

    @Before fun setup() {
        app = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
        store = TransactionStore.get(app)
    }
    @After fun cleanup() {
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
    }

    private fun insert(coded: Boolean, mirror: Boolean = true): Boolean {
        val parsed = ParsedMpesaTransaction(
            transactionCode = if (coded) "ABC1234567" else null,
            type = MpesaTransactionType.SENT_MONEY, amountMinor = 50_000L,
            counterparty = "Invented shop", counterpartyPhone = "0712345678",
            feeMinor = 0L, balanceMinor = null, occurredAtText = null,
            rawMessage = if (coded) "Invented coded payment" else "Invented separate payment")
        return store.insertIfNew(parsed, sourcePackage = if (coded) "test.sms" else "test.notification",
            sourceKind = if (coded) "SMS" else "NOTIFICATION", receivedAtEpochMs = time + if (coded) 0 else 1_000,
            rebuildPayCycles = false, runRelationshipReconciliation = false, runMirrorCorrelation = mirror,
            directionOverride = MoneyDirection.EXPENSE, categoryOverride = TransactionCategory.OTHER,
            reviewStateOverride = ReviewState.AUTO_CLASSIFIED)
    }

    private fun count(sql: String): Long = PesaTrackDatabase.get(app).readableDatabase.compileStatement(sql).simpleQueryForLong()

    @Test fun codedThenCodelessPreservesBothIndependentPayments() {
        assertTrue(insert(true)); assertTrue(insert(false))
        assertEquals(2L, count("SELECT COUNT(*) FROM transactions"))
        assertEquals(100_000L, count("SELECT SUM(amount_minor) FROM transactions"))
    }

    @Test fun codelessThenCodedPreservesBothIndependentPayments() {
        assertTrue(insert(false)); assertTrue(insert(true))
        assertEquals(2L, count("SELECT COUNT(*) FROM transactions"))
        assertEquals(100_000L, count("SELECT SUM(amount_minor) FROM transactions"))
    }

    @Test fun historicalCleanupPreservesRowsAndTheirDependentAllocations() {
        assertTrue(insert(true, mirror = false)); assertTrue(insert(false, mirror = false))
        val db = PesaTrackDatabase.get(app).writableDatabase
        val codelessId = db.rawQuery("SELECT id FROM transactions WHERE transaction_code IS NULL", null).use {
            assertTrue(it.moveToFirst()); it.getLong(0)
        }
        db.insertOrThrow("transaction_allocations", null, ContentValues().apply {
            put("transaction_id", codelessId); put("position", 0); put("category", "OTHER"); put("amount_minor", 50_000L)
        })
        val before = count("SELECT SUM(amount_minor) FROM transactions")
        assertEquals(0, store.reconcileMirrorDuplicates())
        assertEquals(2L, count("SELECT COUNT(*) FROM transactions"))
        assertEquals(before, count("SELECT SUM(amount_minor) FROM transactions"))
        assertEquals(1L, count("SELECT COUNT(*) FROM transaction_allocations"))
        assertEquals(2L, count("SELECT COUNT(*) FROM transactions WHERE direction = 'EXPENSE' AND category = 'OTHER'"))
        store.activitySnapshot(time)
        assertEquals(2L, count("SELECT COUNT(*) FROM transactions"))
        assertEquals(before, count("SELECT SUM(amount_minor) FROM transactions"))
        assertEquals(1L, count("SELECT COUNT(*) FROM transaction_allocations"))
    }

    @Test fun reusedReferenceWithConflictingAmountsDoesNotMerge() {
        fun insertReference(code: String, amount: Long, mirror: Boolean): Boolean = store.insertIfNew(
            ParsedMpesaTransaction(code, MpesaTransactionType.SENT_MONEY, amount, "Invented shop", null,
                0L, null, null, "Invented payment $code"), sourcePackage = "test.sms", sourceKind = "SMS",
            receivedAtEpochMs = time, rebuildPayCycles = false, runRelationshipReconciliation = false,
            runMirrorCorrelation = mirror, directionOverride = MoneyDirection.EXPENSE,
            categoryOverride = TransactionCategory.OTHER, externalReferences = setOf("SHARED1234"))
        assertTrue(insertReference("ABC1234567", 50_000L, false))
        assertTrue(insertReference("DEF1234567", 60_000L, false))
        assertTrue(insertReference("GHI1234567", 50_000L, true))
        assertEquals(3L, count("SELECT COUNT(*) FROM transactions"))
        assertEquals(160_000L, count("SELECT SUM(amount_minor) FROM transactions"))
    }

    @Test fun protectedReferenceCompetitorPreventsAutomaticLinking() {
        fun insertReference(code: String, state: ReviewState, mirror: Boolean): Boolean = store.insertIfNew(
            ParsedMpesaTransaction(code, MpesaTransactionType.SENT_MONEY, 50_000L, "Invented shop", null,
                0L, null, null, "Invented payment $code"), sourcePackage = "test.sms", sourceKind = "SMS",
            receivedAtEpochMs = time, rebuildPayCycles = false, runRelationshipReconciliation = false,
            runMirrorCorrelation = mirror, directionOverride = MoneyDirection.EXPENSE,
            categoryOverride = TransactionCategory.OTHER, reviewStateOverride = state,
            externalReferences = setOf("SHARED1234"))
        assertTrue(insertReference("ABC1234567", ReviewState.AUTO_CLASSIFIED, false))
        assertTrue(insertReference("DEF1234567", ReviewState.USER_CLASSIFIED, false))
        assertTrue(insertReference("GHI1234567", ReviewState.AUTO_CLASSIFIED, true))
        assertEquals(3L, count("SELECT COUNT(*) FROM transactions"))
        assertEquals(1L, count("SELECT COUNT(*) FROM transactions WHERE review_state = 'USER_CLASSIFIED'"))
    }

    @Test fun truncatedReferenceSearchCannotClaimAUniqueMatch() {
        fun insertReference(code: String, state: ReviewState, mirror: Boolean): Boolean = store.insertIfNew(
            ParsedMpesaTransaction(code, MpesaTransactionType.SENT_MONEY, 50_000L, "Invented shop", null,
                0L, null, null, "Invented payment $code"), sourcePackage = "test.sms", sourceKind = "SMS",
            receivedAtEpochMs = time, rebuildPayCycles = false, runRelationshipReconciliation = false,
            runMirrorCorrelation = mirror, directionOverride = MoneyDirection.EXPENSE,
            categoryOverride = TransactionCategory.OTHER, reviewStateOverride = state,
            externalReferences = setOf("SHARED1234"))
        repeat(13) { index ->
            assertTrue(insertReference("SYNTH%05d".format(index),
                if (index < 2) ReviewState.AUTO_CLASSIFIED else ReviewState.USER_CLASSIFIED, false))
        }
        assertTrue(insertReference("INCOMING01", ReviewState.AUTO_CLASSIFIED, true))
        assertEquals(14L, count("SELECT COUNT(*) FROM transactions"))
        assertEquals(700_000L, count("SELECT SUM(amount_minor) FROM transactions"))
    }

    @Test fun legacyCodeWithoutReferenceTableEntryStillCountsAsACompetitor() {
        fun insertReference(code: String, state: ReviewState, mirror: Boolean, references: Set<String>): Boolean = store.insertIfNew(
            ParsedMpesaTransaction(code, MpesaTransactionType.SENT_MONEY, 50_000L, "Invented shop", null,
                0L, null, null, "Invented payment $code"), sourcePackage = "test.sms", sourceKind = "SMS",
            receivedAtEpochMs = time, rebuildPayCycles = false, runRelationshipReconciliation = false,
            runMirrorCorrelation = mirror, directionOverride = MoneyDirection.EXPENSE,
            categoryOverride = TransactionCategory.OTHER, reviewStateOverride = state, externalReferences = references)
        assertTrue(insertReference("SHARED1234", ReviewState.USER_CLASSIFIED, false, emptySet()))
        val db = PesaTrackDatabase.get(app).writableDatabase
        db.delete("transaction_external_references", "reference_value = ?", arrayOf("SHARED1234"))
        db.execSQL("UPDATE transactions SET transaction_code = 'shared.1234' WHERE transaction_code = 'SHARED1234'")
        assertTrue(insertReference("OTHER00001", ReviewState.AUTO_CLASSIFIED, false, setOf("SHARED1234")))
        assertTrue(insertReference("INCOMING01", ReviewState.AUTO_CLASSIFIED, true, setOf("SHARED1234")))
        assertEquals(3L, count("SELECT COUNT(*) FROM transactions"))
        assertEquals(1L, count("SELECT COUNT(*) FROM transactions WHERE review_state = 'USER_CLASSIFIED'"))
    }

    @Test fun differentlyFormattedLegacyReferenceStillCountsAsACompetitor() {
        fun insertReference(code: String, state: ReviewState, mirror: Boolean, references: Set<String>): Boolean = store.insertIfNew(
            ParsedMpesaTransaction(code, MpesaTransactionType.SENT_MONEY, 50_000L, "Invented shop", null,
                0L, null, null, "Invented payment $code"), sourcePackage = "test.sms", sourceKind = "SMS",
            receivedAtEpochMs = time, rebuildPayCycles = false, runRelationshipReconciliation = false,
            runMirrorCorrelation = mirror, directionOverride = MoneyDirection.EXPENSE,
            categoryOverride = TransactionCategory.OTHER, reviewStateOverride = state, externalReferences = references)
        assertTrue(insertReference("LEGACY0001", ReviewState.USER_CLASSIFIED, false, setOf("SHARED1234")))
        val db = PesaTrackDatabase.get(app).writableDatabase
        db.execSQL("UPDATE transaction_external_references SET reference_value = 'shared.1234' WHERE reference_value = 'SHARED1234'")
        assertTrue(insertReference("OTHER00001", ReviewState.AUTO_CLASSIFIED, false, setOf("SHARED1234")))
        assertTrue(insertReference("INCOMING01", ReviewState.AUTO_CLASSIFIED, true, setOf("SHARED1234")))
        assertEquals(3L, count("SELECT COUNT(*) FROM transactions"))
        assertEquals(1L, count("SELECT COUNT(*) FROM transactions WHERE review_state = 'USER_CLASSIFIED'"))
    }
}
