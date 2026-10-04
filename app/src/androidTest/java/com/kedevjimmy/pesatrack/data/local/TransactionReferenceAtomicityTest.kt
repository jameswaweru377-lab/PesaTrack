package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.interpretation.CanonicalFinancialRecordKind
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentRouteHint
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionReferenceAtomicityTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun failedReferenceInsertRollsBackCanonicalTransaction() {
        val db = PesaTrackDatabase.get(context).writableDatabase
        db.execSQL(
            """
            CREATE TRIGGER abort_external_reference_for_test
            BEFORE INSERT ON transaction_external_references
            BEGIN
                SELECT RAISE(ABORT, 'forced reference failure');
            END
            """.trimIndent()
        )

        val store = newTransactionStore(context)

        assertThrows(SQLException::class.java) {
            store.insertInterpretedMoneyMovementIfNew(
                kind = CanonicalFinancialRecordKind.PAYMENT,
                paymentRouteHint = PaymentRouteHint.MERCHANT,
                transactionCode = TRANSACTION_CODE,
                amountMinor = 2_500L,
                counterparty = "Atomicity fixture",
                counterpartyPhone = null,
                feeMinor = 0L,
                balanceMinor = null,
                occurredAtText = "4/10/2026 at 2:00 PM",
                rawMessage = "Atomicity fixture message",
                sourcePackage = "test.package",
                sourceKind = "TEST_ATOMICITY",
                receivedAtEpochMs = TEST_EPOCH_MS,
                occurredAtEpochMs = TEST_EPOCH_MS,
                rebuildPayCycles = false,
                accountIdOverride = AccountStore.DEFAULT_MPESA_ID,
                externalReferences = setOf(EXTERNAL_REFERENCE)
            )
        }

        assertEquals(
            0,
            db.rowCount(
                "transactions",
                "transaction_code = ?",
                arrayOf(TRANSACTION_CODE)
            )
        )
        assertEquals(
            0,
            db.rowCount(
                "transaction_external_references",
                "reference_value = ?",
                arrayOf(EXTERNAL_REFERENCE)
            )
        )
    }

    private fun newTransactionStore(
        context: Context
    ): TransactionStore {
        val constructor =
            TransactionStore::class.java
                .getDeclaredConstructor(Context::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(context)
    }

    private fun SQLiteDatabase.rowCount(
        table: String,
        where: String,
        args: Array<String>
    ): Int =
        rawQuery(
            "SELECT COUNT(*) FROM $table WHERE $where",
            args
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private companion object {
        const val DATABASE_NAME = "pesatrack.db"
        const val TRANSACTION_CODE = "ATOMIC0001"
        const val EXTERNAL_REFERENCE = "BANK-REF-ATOMIC-1"
        const val TEST_EPOCH_MS = 1_780_582_400_000L
    }
}
