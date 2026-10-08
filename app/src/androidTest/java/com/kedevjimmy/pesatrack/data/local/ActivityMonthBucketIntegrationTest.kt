package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import java.util.Calendar
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityMonthBucketIntegrationTest {
    private lateinit var context: Context
    private val timeZone =
        TimeZone.getTimeZone("Africa/Nairobi")

    @Before
    fun setUp() {
        context =
            ApplicationProvider
                .getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @After
    fun tearDown() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @Test
    fun monthBucketsUseIndexedRangesAndIgnoreExcludedRows() {
        val db =
            PesaTrackDatabase.get(context)
                .writableDatabase
        val october =
            epoch(
                year = 2026,
                month = Calendar.OCTOBER,
                day = 8
            )
        val september =
            epoch(
                year = 2026,
                month = Calendar.SEPTEMBER,
                day = 20
            )

        insert(
            db = db,
            fingerprint = "october-1",
            occurredAt = october,
            reviewState =
                ReviewState.AUTO_CLASSIFIED.name
        )
        insert(
            db = db,
            fingerprint = "october-2",
            occurredAt = october + 1_000L,
            reviewState =
                ReviewState.NEEDS_REVIEW.name
        )
        insert(
            db = db,
            fingerprint = "october-excluded",
            occurredAt = october + 2_000L,
            reviewState =
                ReviewState.EXCLUDED.name
        )
        insert(
            db = db,
            fingerprint = "september-1",
            occurredAt = september,
            reviewState =
                ReviewState.AUTO_CLASSIFIED.name
        )

        val buckets =
            TransactionStore.get(context)
                .activityMonths(timeZone)

        assertEquals(2, buckets.size)
        assertEquals(
            monthStart(october),
            buckets[0].monthStartEpochMs
        )
        assertEquals(
            2,
            buckets[0].transactionCount
        )
        assertEquals(
            monthStart(september),
            buckets[1].monthStartEpochMs
        )
        assertEquals(
            1,
            buckets[1].transactionCount
        )
    }

    private fun insert(
        db: android.database.sqlite.SQLiteDatabase,
        fingerprint: String,
        occurredAt: Long,
        reviewState: String
    ) {
        val values =
            ContentValues().apply {
                put(
                    "fingerprint",
                    fingerprint
                )
                put("type", "OTHER")
                put("amount_minor", 100L)
                put("fee_minor", 0L)
                put(
                    "occurred_at_epoch_ms",
                    occurredAt
                )
                put(
                    "received_at_epoch_ms",
                    occurredAt
                )
                put(
                    "source_kind",
                    "TEST"
                )
                put(
                    "created_at_epoch_ms",
                    occurredAt
                )
                put(
                    "direction",
                    "EXPENSE"
                )
                put(
                    "category",
                    "OTHER"
                )
                put(
                    "review_state",
                    reviewState
                )
            }
        db.insertOrThrow(
            "transactions",
            null,
            values
        )
    }

    private fun epoch(
        year: Int,
        month: Int,
        day: Int
    ): Long =
        Calendar.getInstance(timeZone)
            .apply {
                clear()
                set(
                    year,
                    month,
                    day,
                    12,
                    0,
                    0
                )
            }
            .timeInMillis

    private fun monthStart(
        epochMs: Long
    ): Long =
        Calendar.getInstance(timeZone)
            .apply {
                timeInMillis = epochMs
                set(
                    Calendar.DAY_OF_MONTH,
                    1
                )
                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )
                set(
                    Calendar.MINUTE,
                    0
                )
                set(
                    Calendar.SECOND,
                    0
                )
                set(
                    Calendar.MILLISECOND,
                    0
                )
            }
            .timeInMillis
}
