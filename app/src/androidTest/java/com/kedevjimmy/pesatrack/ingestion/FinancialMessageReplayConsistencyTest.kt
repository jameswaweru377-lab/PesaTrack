package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinancialMessageReplayConsistencyTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        PesaTrackDatabase.get(context).close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun replayingSameFinancialMessageDoesNotDuplicateCanonicalState() {
        val coordinator =
            FinancialMessageIngestionCoordinator.get(context)

        val first =
            coordinator.ingest(
                title = "MPESA",
                body = PAYMENT_MESSAGE,
                sourcePackage =
                    "com.google.android.apps.messaging",
                sourceKind = "TEST_SMS",
                receivedAtEpochMs =
                    TEST_RECEIVED_AT_EPOCH_MS,
                rebuildPayCycles = false,
                diagnosticMode =
                    FinancialDiagnosticMode.RECONCILE_ONLY
            )

        val replay =
            coordinator.ingest(
                title = "MPESA",
                body = PAYMENT_MESSAGE,
                sourcePackage =
                    "com.google.android.apps.messaging",
                sourceKind = "TEST_SMS",
                receivedAtEpochMs =
                    TEST_RECEIVED_AT_EPOCH_MS,
                rebuildPayCycles = false,
                diagnosticMode =
                    FinancialDiagnosticMode.RECONCILE_ONLY
            )

        assertTrue(first.recognized)
        assertEquals(1, first.insertedTransactions)
        assertEquals(0, replay.insertedTransactions)

        val db =
            PesaTrackDatabase.get(context)
                .readableDatabase

        val transactionCount =
            db.rawQuery(
                """
                SELECT COUNT(*)
                FROM transactions
                WHERE transaction_code = ?
                """.trimIndent(),
                arrayOf(TRANSACTION_CODE)
            ).use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }

        val balanceObservationCount =
            db.rawQuery(
                """
                SELECT COUNT(*)
                FROM account_balance_observations
                WHERE balance_minor = ?
                """.trimIndent(),
                arrayOf(MPESA_BALANCE_MINOR.toString())
            ).use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }

        assertEquals(1, transactionCount)
        assertEquals(1, balanceObservationCount)
    }

    private companion object {
        const val DATABASE_NAME = "pesatrack.db"
        const val TRANSACTION_CODE = "TST12ABC04"
        const val MPESA_BALANCE_MINOR = 765_000L
        const val TEST_RECEIVED_AT_EPOCH_MS =
            1_790_611_800_000L

        const val PAYMENT_MESSAGE =
            "TST12ABC04 Confirmed. Ksh 450.00 sent to SAMPLE PERSON " +
                "0712000000 on 28/9/26 at 7:10 PM. " +
                "New M-PESA balance is Ksh 7,650.00. " +
                "Transaction cost, Ksh 7.00."
    }
}
