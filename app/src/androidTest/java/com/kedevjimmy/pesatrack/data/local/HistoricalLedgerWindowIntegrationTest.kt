package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.rules.HistoryImportWindow
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P0/A1: deterministic 1- and 3-month history/accounting boundary checks.
 *
 * Inserts synthetic canonical rows into an isolated test database; no real
 * SMS or device data are collected. SMS-provider scanning is covered by
 * separate import/device acceptance tests (not claimed by this test).
 */
@RunWith(AndroidJUnit4::class)
class HistoricalLedgerWindowIntegrationTest {
    private lateinit var context: Context
    private val nairobi = TimeZone.getTimeZone("Africa/Nairobi")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @After
    fun tearDown() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @Test
    fun historicalOneAndThreeMonthSnapshotsDoNotLeakAcrossBoundaries() {
        // Exactly at the chosen month edges, with adjacent months outside.
        insert("june-outside", 60_000L, MoneyDirection.EXPENSE, Calendar.JUNE, 30, 23, 59)
        insert("july-transfer", 70_000L, MoneyDirection.TRANSFER, Calendar.JULY, 1, 0, 0)
        insert("august-income", 40_000L, MoneyDirection.INCOME, Calendar.AUGUST, 31, 23, 59)
        insert("sept-expense", 20_000L, MoneyDirection.EXPENSE, Calendar.SEPTEMBER, 1, 0, 0, fee = 200L)
        insert("sept-income", 50_000L, MoneyDirection.INCOME, Calendar.SEPTEMBER, 26, 12, 0)
        insert("oct-outside", 80_000L, MoneyDirection.EXPENSE, Calendar.OCTOBER, 1, 0, 0)

        val now = epoch(Calendar.SEPTEMBER, 27, 12, 0)
        val one = HistoryImportWindow.months(1, now, nairobi, Locale.ENGLISH)
        val three = HistoryImportWindow.months(3, now, nairobi, Locale.ENGLISH)
        val store = TransactionStore.get(context)

        assertEquals(epoch(Calendar.SEPTEMBER, 1, 0, 0), one.last().startEpochMs)
        assertEquals(epoch(Calendar.JULY, 1, 0, 0), three.last().startEpochMs)

        val september = store.activitySnapshotBetween(
            startEpochMs = one.last().startEpochMs,
            endEpochMsExclusive = one.first().endEpochMs,
            nowEpochMs = now,
            timeZone = nairobi,
            performMaintenance = false
        )
        assertEquals(2, september.trustedTransactions.size)
        assertEquals(50_000L, september.summary.incomeMinor)
        assertEquals(20_000L, september.summary.spentMinor)
        assertEquals(0L, september.summary.transferMinor)
        assertEquals(200L, september.summary.feesMinor)
        assertEquals(29_800L, september.summary.netMinor)

        val threeMonths = store.activitySnapshotBetween(
            startEpochMs = three.last().startEpochMs,
            endEpochMsExclusive = three.first().endEpochMs,
            nowEpochMs = now,
            timeZone = nairobi,
            performMaintenance = false
        )
        assertEquals(4, threeMonths.trustedTransactions.size)
        assertEquals(90_000L, threeMonths.summary.incomeMinor)
        assertEquals(20_000L, threeMonths.summary.spentMinor)
        assertEquals(70_000L, threeMonths.summary.transferMinor)
        assertEquals(200L, threeMonths.summary.feesMinor)
        assertEquals(69_800L, threeMonths.summary.netMinor)

        // Explicit conservation: moving money between owned accounts
        // does not increase income or reduce spendable cash as consumption.
        assertEquals(
            threeMonths.summary.incomeMinor -
                threeMonths.summary.spentMinor -
                threeMonths.summary.feesMinor,
            threeMonths.summary.netMinor
        )
    }

    @Test
    fun excludedAndUnresolvedRowsStayOutsideTrustedMoneyTotals() {
        insert(
            "needs-review", 300_000L, MoneyDirection.INCOME,
            Calendar.SEPTEMBER, 2, 10, 0,
            reviewState = ReviewState.NEEDS_REVIEW
        )
        insert(
            "excluded", 700_000L, MoneyDirection.INCOME,
            Calendar.SEPTEMBER, 3, 10, 0,
            reviewState = ReviewState.EXCLUDED
        )

        val snapshot = TransactionStore.get(context).activitySnapshot(
            nowEpochMs = epoch(Calendar.SEPTEMBER, 27, 12, 0),
            timeZone = nairobi,
            performMaintenance = false
        )
        assertEquals(2, snapshot.transactions.size)
        assertEquals(0, snapshot.trustedTransactions.size)
        assertEquals(0L, snapshot.summary.incomeMinor)
        assertEquals(0L, snapshot.summary.netMinor)
        assertEquals(1, snapshot.summary.needsReviewCount)
    }

    private fun insert(
        code: String,
        amount: Long,
        direction: MoneyDirection,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        fee: Long = 0L,
        reviewState: ReviewState = ReviewState.USER_CLASSIFIED
    ) {
        val at = epoch(month, day, hour, minute)
        val values = ContentValues().apply {
            put("transaction_code", code.uppercase())
            put("fingerprint", "synthetic-$code")
            put("type", "OTHER")
            put("amount_minor", amount)
            put("fee_minor", fee)
            put("occurred_at_epoch_ms", at)
            put("received_at_epoch_ms", at)
            put("created_at_epoch_ms", at)
            put("source_kind", "TEST_SYNTHETIC_HISTORY")
            put("direction", direction.name)
            put("category", "OTHER")
            put("review_state", reviewState.name)
        }
        PesaTrackDatabase.get(context).writableDatabase.insertOrThrow(
            "transactions", null, values
        )
    }

    private fun epoch(month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(nairobi).apply {
            clear()
            set(2026, month, day, hour, minute, 0)
        }.timeInMillis
}
