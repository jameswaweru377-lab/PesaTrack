package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.kedevjimmy.pesatrack.domain.model.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.TimeZone

/** Informational measurements: ordinary per-record persistence/replay/query paths, invented data only. */
@RunWith(AndroidJUnit4::class)
class SyntheticHistoryMeasurementTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @After fun tearDown() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @Test fun thirtyAndNinetyDayHistoriesHaveStableTotalsAfterDuplicateReplay() {
        measureHistory(days = 30, expectedRows = 3000, expectedSpent = 3148500L, expectedFees = 15000L)
        measureHistory(days = 90, expectedRows = 9000, expectedSpent = 9445500L, expectedFees = 45000L)
    }

    private fun measureHistory(days: Int, expectedRows: Int, expectedSpent: Long, expectedFees: Long) {
        val runs = JsonArray()
        // Stores retain their helper for the process lifetime. Reuse that file/connection rather
        // than unlinking it while TransactionStore/AccountStore singletons still reference it.
        val store = TransactionStore.get(context)
        assertNull(store.findByTransactionCode("SYNTHETIC_INITIALIZATION_ONLY"))
        val database = PesaTrackDatabase.get(context).writableDatabase
        repeat(3) { run ->
            database.beginTransaction()
            try {
                database.delete("transaction_external_references",
                    "transaction_id IN (SELECT id FROM transactions WHERE source_kind = ?)",
                    arrayOf("SYNTHETIC_HISTORY_MEASUREMENT"))
                database.delete("transactions", "source_kind = ?",
                    arrayOf("SYNTHETIC_HISTORY_MEASUREMENT"))
                database.setTransactionSuccessful()
            } finally { database.endTransaction() }
            database.rawQuery("SELECT COUNT(*) FROM transactions", null).use {
                assertTrue(it.moveToFirst())
                assertEquals("The entire invented ledger must be empty before measurement", 0L, it.getLong(0))
            }
            assertTrue(snapshot(store, days).transactions.isEmpty())
            // Rows are generated outside the measured persistence phase.
            val rows = List(expectedRows) { index ->
                ParsedMpesaTransaction(
                    transactionCode = "SYN$days-${index.toString().padStart(5, '0')}",
                    type = MpesaTransactionType.BUY_GOODS,
                    amountMinor = 1000L + index % 100,
                    counterparty = "Invented merchant $index",
                    feeMinor = 5L,
                    balanceMinor = null,
                    occurredAtText = null,
                    rawMessage = "Invented parsed payment fixture $days/$index. No personal message source."
                )
            }
            var inserted = 0
            val importStarted = System.nanoTime()
            rows.forEachIndexed { index, parsed ->
                if (persist(store, parsed, timestamp(index))) inserted++
            }
            val importNs = System.nanoTime() - importStarted
            assertEquals(expectedRows, inserted)

            val coldStarted = System.nanoTime()
            val cold = snapshot(store, days)
            val coldNs = System.nanoTime() - coldStarted
            assertSnapshot(cold, expectedRows, expectedSpent, expectedFees)
            val warmSamples = JsonArray()
            repeat(5) {
                val started = System.nanoTime()
                assertSnapshot(snapshot(store, days), expectedRows, expectedSpent, expectedFees)
                warmSamples.add((System.nanoTime() - started) / 1000000.0)
            }

            var replayInserted = 0
            val replayStarted = System.nanoTime()
            rows.forEachIndexed { index, parsed ->
                if (persist(store, parsed, timestamp(index))) replayInserted++
            }
            val replayNs = System.nanoTime() - replayStarted
            assertEquals(0, replayInserted)
            assertSnapshot(snapshot(store, days), expectedRows, expectedSpent, expectedFees)
            runs.add(JsonObject().apply {
                addProperty("run", run + 1)
                addProperty("persist_ms", importNs / 1000000.0)
                addProperty("duplicate_replay_ms", replayNs / 1000000.0)
                addProperty("first_snapshot_ms", coldNs / 1000000.0)
                add("warm_snapshot_ms", warmSamples)
                addProperty("persisted_rows", inserted)
                addProperty("replay_inserted", replayInserted)
                addProperty("trusted_spent_minor", expectedSpent)
                addProperty("fees_minor", expectedFees)
            })
        }
        val evidence = JsonObject().apply {
            addProperty("fixture", "invented-history-v3")
            addProperty("days", days)
            addProperty("rows", expectedRows)
            addProperty("schema_version", 34)
            addProperty("api", Build.VERSION.SDK_INT)
            addProperty("manufacturer", Build.MANUFACTURER)
            addProperty("model", Build.MODEL)
            addProperty("measurement_scope", "parsed_message_per_record_persistence_duplicate_replay_activity_snapshot")
            addProperty("transaction_mode", "production_per_record")
            addProperty("empty_ledger_runs", 3)
            addProperty("database_lifecycle", "single_file_and_helper_lifetime_with_ledger_reset_between_runs")
            addProperty("thresholds", "informational_only")
            add("runs", runs)
        }
        Log.i("PesaTrackMeasurement", evidence.toString())
        println("PESATRACK_HISTORY_MEASUREMENT " + evidence)
    }

    private fun persist(store: TransactionStore, parsed: ParsedMpesaTransaction, occurred: Long) =
        store.insertIfNew(
            parsed = parsed,
            sourcePackage = null,
            sourceKind = "SYNTHETIC_HISTORY_MEASUREMENT",
            receivedAtEpochMs = occurred,
            occurredAtEpochMs = occurred,
            rebuildPayCycles = false,
            runRelationshipReconciliation = false,
            runMirrorCorrelation = false,
            directionOverride = MoneyDirection.EXPENSE,
            categoryOverride = TransactionCategory.GROCERIES,
            reviewStateOverride = ReviewState.AUTO_CLASSIFIED,
            accountIdOverride = AccountStore.DEFAULT_MPESA_ID
        )

    private fun timestamp(index: Int) = EPOCH + (index / 100) * DAY + (index % 100) * 600000L

    private fun snapshot(store: TransactionStore, days: Int) = store.activitySnapshotBetween(
        EPOCH, EPOCH + days * DAY, nowEpochMs = EPOCH + days * DAY,
        timeZone = TimeZone.getTimeZone("UTC"), performMaintenance = false
    )

    private fun assertSnapshot(snapshot: ActivityLedgerSnapshot, rows: Int, spent: Long, fees: Long) {
        assertEquals(rows, snapshot.transactions.size)
        assertEquals(rows, snapshot.trustedTransactions.size)
        assertTrue(snapshot.possibleDuplicates.isEmpty())
        assertEquals(spent, snapshot.summary.spentMinor)
        assertEquals(fees, snapshot.summary.feesMinor)
        assertEquals(0L, snapshot.summary.incomeMinor)
        assertEquals(0L, snapshot.summary.refundMinor)
        assertEquals(0L, snapshot.summary.transferMinor)
        assertEquals(-spent - fees, snapshot.summary.netMinor)
    }

    companion object {
        private const val EPOCH = 1780272000000L
        private const val DAY = 86400000L
    }
}

