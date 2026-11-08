package com.kedevjimmy.pesatrack.data.local

import android.app.Application
import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.statements.StatementActivityObservationPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementCrossSourceEventPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementEvidenceCorrelationPolicy
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Synthetic ledger relationships; never reads personal SMS or statement files. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class StatementOriginalMessageEvidenceReaderTest {
    private lateinit var app: Context
    private val at = 1_780_000_000_000L
    private var nextTransaction = 0

    @Before fun setup() {
        app = RuntimeEnvironment.getApplication()
        nextTransaction = 0
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
        app.deleteDatabase("pesatrack_statement_activity.db")
    }

    @After fun cleanup() {
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
        app.deleteDatabase("pesatrack_statement_activity.db")
    }

    private fun transaction(source: String = "historical_sms", relationship: Long? = null): Long =
        PesaTrackDatabase.get(app).writableDatabase.insertOrThrow("transactions", null, ContentValues().apply {
            nextTransaction += 1
            put("transaction_code", "SYN123456$nextTransaction")
            put("fingerprint", "synthetic-$nextTransaction")
            put("type", "SENT_MONEY")
            put("amount_minor", 50_000L)
            put("fee_minor", 0L)
            put("occurred_at_epoch_ms", at)
            put("received_at_epoch_ms", at)
            put("source_kind", source)
            put("created_at_epoch_ms", at)
            put("direction", "EXPENSE")
            put("category", "OTHER")
            put("review_state", "AUTO_CLASSIFIED")
            put("account_id", "synthetic-wallet")
            relationship?.let { put("related_transaction_id", it); put("relation_type", "REFUND") }
        })

    private fun original(): StatementEvidenceCorrelationPolicy.Observation {
        val rows = StatementOriginalMessageEvidenceReader(app).read(at - 1, at + 1,
            setOf("synthetic-wallet"), 10)
        return StatementActivityObservationPolicy.project(rows, setOf("synthetic-wallet")).single()
    }

    private fun canCorroborateCashLeg(): Boolean {
        val activity = original()
        val statement = activity.copy(
            id = "statement-copy", source = StatementEvidenceCorrelationPolicy.Source.MPESA_STATEMENT,
            sourceId = "verified-statement", sourceTotalsVerified = true, roleProtected = false)
        return StatementCrossSourceEventPolicy.reconcile(
            listOf(activity, statement), setOf("synthetic-wallet")).sameLegGroups.isNotEmpty()
    }

    @Test fun linkedDebtRepaymentCannotBecomeOrdinaryStatementCashProofAndUndoRestoresEligibility() {
        val cash = transaction()
        assertTrue(canCorroborateCashLeg())
        val debt = PesaTrackDatabase.get(app).writableDatabase.insertOrThrow("debt_events", null,
            ContentValues().apply {
                put("event_key", "synthetic-repayment")
                put("debt_account_id", "debt-fuliza-mpesa")
                put("event_type", "REPAYMENT")
                put("amount_minor", 50_000L)
                put("linked_transaction_id", cash)
                put("source_kind", "synthetic")
                put("occurred_at_epoch_ms", at)
                put("created_at_epoch_ms", at)
            })
        assertTrue(original().roleProtected)
        assertFalse(canCorroborateCashLeg())
        PesaTrackDatabase.get(app).writableDatabase.delete("debt_events", "id=?", arrayOf(debt.toString()))
        assertFalse(original().roleProtected)
        assertTrue(canCorroborateCashLeg())
    }

    @Test fun feeAndFulizaMetadataProtectSpecializedMovementWithoutChangingItsCashLeg() {
        val cash = transaction()
        val db = PesaTrackDatabase.get(app).writableDatabase
        val originalAmount = original().movementMinor
        for ((field, value) in listOf(
            "fee_minor" to 100L,
            "fuliza_financed_minor" to 50_000L,
            "fuliza_access_fee_minor" to 50L
        )) {
            db.update("transactions", ContentValues().apply { put(field, value) }, "id=?", arrayOf(cash.toString()))
            assertTrue("Specialized $field must withhold same-leg proof", original().roleProtected)
            assertFalse(canCorroborateCashLeg())
            assertTrue("Cash amount must be preserved", original().movementMinor == originalAmount)
            db.update("transactions", ContentValues().apply { put(field, 0L) }, "id=?", arrayOf(cash.toString()))
            assertTrue("Ordinary cash remains reviewable", canCorroborateCashLeg())
        }
        db.update("transactions", ContentValues().apply { put("fuliza_event_code", "SYNFULIZA01") },
            "id=?", arrayOf(cash.toString()))
        assertTrue(original().roleProtected)
        assertFalse(canCorroborateCashLeg())
    }

    @Test fun laterLinkedRefundProtectsOriginalOutflowWithoutChangingStoredEvidence() {
        val cash = transaction()
        assertTrue(canCorroborateCashLeg())
        val refund = transaction(source = "manual_entry", relationship = cash)
        assertTrue(original().roleProtected)
        assertFalse(canCorroborateCashLeg())
        PesaTrackDatabase.get(app).writableDatabase.delete("transactions", "id=?", arrayOf(refund.toString()))
        assertFalse(original().roleProtected)
        assertTrue(canCorroborateCashLeg())
    }

    @Test fun laterReversalByReceiptCodeProtectsOriginalUntilReversalIsRemoved() {
        transaction()
        assertTrue(canCorroborateCashLeg())
        val reversal = transaction(source = "manual_entry")
        val db = PesaTrackDatabase.get(app).writableDatabase
        db.update("transactions", ContentValues().apply {
            put("reversal_of_code", "SYN1234561")
        }, "id=?", arrayOf(reversal.toString()))
        assertTrue(original().roleProtected)
        assertFalse(canCorroborateCashLeg())
        db.delete("transactions", "id=?", arrayOf(reversal.toString()))
        assertFalse(original().roleProtected)
        assertTrue(canCorroborateCashLeg())
    }
}
