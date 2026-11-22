package com.kedevjimmy.pesatrack.data.local

import android.app.Application
import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Existing installations refresh their persisted closed-cycle projections on read. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class PayCycleIncomeEvidenceRefreshTest {
    private lateinit var app: Context
    private val first = 1_700_000_000_000L
    private val second = first + 35L * 24 * 60 * 60 * 1_000

    @Before fun setup() {
        app = RuntimeEnvironment.getApplication()
        PesaTrackDatabase.resetForTests()
        resetSingletons()
        app.deleteDatabase("pesatrack.db")
    }

    @After fun cleanup() {
        PesaTrackDatabase.resetForTests()
        resetSingletons()
        app.deleteDatabase("pesatrack.db")
    }

    private fun resetSingletons() {
        for (type in listOf(AccountStore::class.java, FinancialUnderstandingStore::class.java,
            TransactionStore::class.java, PayCycleStore::class.java, PaydaySnapshotStore::class.java)) {
            type.getDeclaredField("instance").apply { isAccessible = true }.set(null, null)
        }
    }

    @Test fun previouslyPersistedCycleEndingReactsToRepaymentCorrectionAndUndo() {
        val account = AccountStore.get(app).save("Fictional Employer Bank", AccountType.BANK)
        AccountOwnershipConsentStore(app).recordConsent(account.id, true)
        val db = PesaTrackDatabase.get(app).writableDatabase
        db.insertOrThrow("smart_rules", null, ContentValues().apply {
            put("id", "synthetic-salary-rule"); put("name", "Fictional wages")
            put("category", "INCOME"); put("direction", "INCOME")
            put("starts_pay_cycle", 1); put("created_at_epoch_ms", first)
            put("updated_at_epoch_ms", first)
        })
        val startId = insertReceipt("SYN-SALARY-1", first, 10_000_00L, account.id, true)
        insertReceipt("SYN-SALARY-2", second, 10_000_00L, account.id, true)
        val repaymentId = insertReceipt("SYN-CLIENT", first + 7L * 24 * 60 * 60 * 1_000,
            5_000_00L, account.id, false)

        val cycles = PayCycleStore.get(app)
        val before = cycles.listCycles().single { it.startTransactionId == startId }
        assertNotNull(before.endingTrackedMinor)

        db.insertOrThrow("personal_receivable_people", null, ContentValues().apply {
            put("id", "synthetic-person"); put("display_name", "Fictional Friend")
            put("created_at_epoch_ms", first)
        })
        val annotationId = db.insertOrThrow("personal_receivable_events", null, ContentValues().apply {
            put("event_key", "user:synthetic-repayment")
            put("person_id", "synthetic-person"); put("kind", "REPAID")
            put("amount_minor", 3_000_00L); put("linked_transaction_id", repaymentId)
            put("occurred_at_epoch_ms", first + 7L * 24 * 60 * 60 * 1_000)
            put("created_at_epoch_ms", first)
        })
        val corrected = cycles.listCycles().single { it.startTransactionId == startId }
        assertEquals(before.endingTrackedMinor!! - 3_000_00L, corrected.endingTrackedMinor!!)

        db.delete("personal_receivable_events", "id=?", arrayOf(annotationId.toString()))
        val undone = cycles.listCycles().single { it.startTransactionId == startId }
        assertEquals(before.endingTrackedMinor, undone.endingTrackedMinor)
    }

    private fun insertReceipt(code: String, date: Long, amount: Long, accountId: String,
        salary: Boolean): Long = PesaTrackDatabase.get(app).writableDatabase.insertOrThrow(
        "transactions", null, ContentValues().apply {
            put("transaction_code", code); put("fingerprint", code)
            put("type", "RECEIVED"); put("amount_minor", amount); put("fee_minor", 0L)
            put("counterparty", "Fictional Employer"); put("occurred_at_epoch_ms", date)
            put("received_at_epoch_ms", date); put("source_kind", "historical_sms")
            put("created_at_epoch_ms", date); put("direction", "INCOME")
            put("category", "INCOME"); put("review_state", "AUTO_CLASSIFIED")
            put("account_id", accountId)
            if (salary) { put("rule_id", "synthetic-salary-rule"); put("cycle_override", "FORCE_START") }
        })
}
