package com.kedevjimmy.pesatrack.data.local

import android.app.Application
import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.everyday.StatementActivityLine
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Disposable, invented financial evidence only. No personal messages are read. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class StatementReceiptEvidenceReaderTest {
    private lateinit var app: Context
    private val at = 1_780_000_000_000L
    private var count = 0

    @Before fun setup() {
        app = RuntimeEnvironment.getApplication()
        count = 0
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
        app.deleteDatabase("pesatrack_statement_activity.db")
    }

    @After fun cleanup() {
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
        app.deleteDatabase("pesatrack_statement_activity.db")
    }

    private fun transaction(source: String = "historical_sms", code: String? = null): Long {
        count += 1
        return PesaTrackDatabase.get(app).writableDatabase.insertOrThrow(
            "transactions", null, ContentValues().apply {
                put("transaction_code", code ?: "SYN123456$count")
                put("fingerprint", "legacy-evidence-$count")
                put("type", "SENT_MONEY")
                put("amount_minor", 50_000L)
                put("fee_minor", 0L)
                put("occurred_at_epoch_ms", at)
                put("received_at_epoch_ms", at)
                put("created_at_epoch_ms", at)
                put("source_kind", source)
                put("direction", "EXPENSE")
                put("category", "OTHER")
                put("review_state", "AUTO_CLASSIFIED")
                put("account_id", "synthetic-wallet")
            })
    }

    private fun observed(code: String = "SYN1234561") =
        StatementActivityLine(1, 1, code, at, "Customer transfer to Sample Person",
            0L, 50_000L, 100_000L)

    private fun receipt(code: String = "SYN1234561") =
        StatementReceiptEvidenceReader(app).read(listOf(observed(code)))[code]?.singleOrNull()

    @Test fun onlyOriginalMessageEvidenceIsAvailableForAnExactMpesaReceipt() {
        transaction(source = "manual_entry")
        assertTrue(StatementReceiptEvidenceReader(app).read(listOf(observed())).isEmpty())
        val code = "SYN1234562"
        transaction(code = code)
        assertEquals(code, receipt(code)?.code)
        assertFalse(receipt(code)!!.structurallyLinked)
    }

    @Test fun persistedDebtEventProtectsOriginalAndRemovingItRestoresOrdinaryEvidence() {
        val cash = transaction()
        assertFalse(receipt()!!.structurallyLinked)
        val debt = PesaTrackDatabase.get(app).writableDatabase.insertOrThrow(
            "debt_events", null, ContentValues().apply {
                put("event_key", "legacy-synthetic-debt")
                put("debt_account_id", "debt-fuliza-mpesa")
                put("event_type", "REPAYMENT")
                put("amount_minor", 50_000L)
                put("linked_transaction_id", cash)
                put("source_kind", "synthetic")
                put("occurred_at_epoch_ms", at)
                put("created_at_epoch_ms", at)
            })
        assertTrue(receipt()!!.structurallyLinked)
        PesaTrackDatabase.get(app).writableDatabase.delete(
            "debt_events", "id=?", arrayOf(debt.toString()))
        assertFalse(receipt()!!.structurallyLinked)
    }

    @Test fun incomingRefundOrReversalProtectsOriginalWithoutChangingItsAmount() {
        val cash = transaction()
        val other = transaction(source = "manual_entry")
        val db = PesaTrackDatabase.get(app).writableDatabase
        db.update("transactions", ContentValues().apply {
            put("related_transaction_id", cash)
            put("relation_type", "REFUND")
        }, "id=?", arrayOf(other.toString()))
        assertTrue(receipt()!!.structurallyLinked)
        db.update("transactions", ContentValues().apply {
            putNull("related_transaction_id")
            putNull("relation_type")
            put("reversal_of_code", "SYN1234561")
        }, "id=?", arrayOf(other.toString()))
        assertTrue(receipt()!!.structurallyLinked)
        db.delete("transactions", "id=?", arrayOf(other.toString()))
        assertFalse(receipt()!!.structurallyLinked)
        assertEquals(50_000L, receipt()!!.amountMinor)
    }

    @Test fun feeAndFulizaBreakdownWithholdOrdinaryReceiptProjection() {
        val cash = transaction()
        val db = PesaTrackDatabase.get(app).writableDatabase
        for ((field, amount) in listOf(
            "fee_minor" to 100L,
            "fuliza_financed_minor" to 40_000L,
            "fuliza_access_fee_minor" to 50L
        )) {
            db.update("transactions", ContentValues().apply {
                put(field, amount)
            }, "id=?", arrayOf(cash.toString()))
            assertTrue("Protected by $field", receipt()!!.structurallyLinked)
            db.update("transactions", ContentValues().apply {
                put(field, 0L)
            }, "id=?", arrayOf(cash.toString()))
            assertFalse(receipt()!!.structurallyLinked)
        }
        db.update("transactions", ContentValues().apply {
            put("fuliza_event_code", "SYNFULIZA01")
        }, "id=?", arrayOf(cash.toString()))
        assertTrue(receipt()!!.structurallyLinked)
    }
}
