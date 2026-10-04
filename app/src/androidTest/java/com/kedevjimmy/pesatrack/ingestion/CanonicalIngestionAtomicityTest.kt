package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CanonicalIngestionAtomicityTest {
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
    fun downstreamFinancialEventFailureRollsBackWholeCanonicalMessage() {
        val db =
            PesaTrackDatabase.get(context)
                .writableDatabase

        db.execSQL(
            """
            CREATE TRIGGER abort_financial_event_for_test
            BEFORE INSERT ON financial_events
            BEGIN
                SELECT RAISE(ABORT, 'forced financial event failure');
            END
            """.trimIndent()
        )

        val coordinator =
            FinancialMessageIngestionCoordinator
                .get(context)

        assertThrows(SQLException::class.java) {
            coordinator.ingest(
                title = "MPESA",
                body = CASH_WITHDRAWAL_MESSAGE,
                sourcePackage =
                    "com.google.android.apps.messaging",
                sourceKind = "TEST_SMS",
                receivedAtEpochMs =
                    TEST_RECEIVED_AT_EPOCH_MS,
                rebuildPayCycles = false,
                diagnosticMode =
                    FinancialDiagnosticMode
                        .RECONCILE_ONLY
            )
        }

        assertEquals(
            0,
            db.rowCount(
                table = "transactions",
                where = "transaction_code = ?",
                args = arrayOf(TRANSACTION_CODE)
            )
        )
        assertEquals(
            0,
            db.rowCount(
                table = "account_balance_observations",
                where = "balance_minor = ?",
                args =
                    arrayOf(
                        MPESA_BALANCE_MINOR.toString()
                    )
            )
        )
        assertEquals(
            0,
            db.rowCount(
                table = "financial_events",
                where = "event_type = ?",
                args = arrayOf("TRANSFER")
            )
        )
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
        const val TRANSACTION_CODE = "TST12CASH1"
        const val MPESA_BALANCE_MINOR = 1_095_000L
        const val TEST_RECEIVED_AT_EPOCH_MS =
            1_790_563_200_000L

        const val CASH_WITHDRAWAL_MESSAGE =
            "TST12CASH1 Confirmed. Ksh 1,000.00 withdrawn from M-PESA " +
                "at SAMPLE AGENT on 28/9/26 at 8:20 AM. " +
                "New M-PESA balance is Ksh 10,950.00."
    }
}
