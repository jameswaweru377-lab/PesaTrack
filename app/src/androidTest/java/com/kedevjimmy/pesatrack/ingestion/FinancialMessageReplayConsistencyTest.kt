package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.AccountStore
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

        // Canonical M-Pesa writes are intentionally gated on confirmed
        // ownership. Mirror the real post-onboarding state instead of
        // weakening production ownership rules for this test.
        AccountStore.get(context).updateDetails(
            id = AccountStore.DEFAULT_MPESA_ID,
            identifier = TEST_MPESA_NUMBER,
            openingBalanceMinor = null
        )
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

    @Test
    fun thirtyInventedPaymentsRemainSingleTransactionsAfterTwoReplays() {
        val coordinator = FinancialMessageIngestionCoordinator.get(context)
        repeat(30) { index ->
            val code = "TST%07d".format(java.util.Locale.ROOT, 9000000 + index)
            val body = PAYMENT_MESSAGE.replace(TRANSACTION_CODE, code)
                .replace("450.00", "${450 + index}.00")
            repeat(3) { replay ->
                val result = coordinator.ingest(
                    title = "MPESA", body = body,
                    sourcePackage = "com.google.android.apps.messaging", sourceKind = "TEST_SMS",
                    receivedAtEpochMs = TEST_RECEIVED_AT_EPOCH_MS + index * 1000L,
                    rebuildPayCycles = false,
                    diagnosticMode = FinancialDiagnosticMode.RECONCILE_ONLY
                )
                assertTrue(code, result.recognized)
                assertEquals(code, if (replay == 0) 1 else 0, result.insertedTransactions)
            }
            PesaTrackDatabase.get(context).readableDatabase.rawQuery(
                "SELECT COUNT(*) FROM transactions WHERE transaction_code = ?", arrayOf(code)
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(code, 1, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "pesatrack.db"
        const val TRANSACTION_CODE = "TST12ABC04"
        const val MPESA_BALANCE_MINOR = 765_000L
        const val TEST_MPESA_NUMBER = "0712345678"
        const val TEST_RECEIVED_AT_EPOCH_MS =
            1_790_611_800_000L

        const val PAYMENT_MESSAGE =
            "TST12ABC04 Confirmed. Ksh 450.00 sent to SAMPLE PERSON " +
                "0712000000 on 28/9/26 at 7:10 PM. " +
                "New M-PESA balance is Ksh 7,650.00. " +
                "Transaction cost, Ksh 7.00."
    }
}
