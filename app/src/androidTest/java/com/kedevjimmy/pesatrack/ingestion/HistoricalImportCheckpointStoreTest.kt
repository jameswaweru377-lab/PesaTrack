package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class HistoricalImportCheckpointStoreTest {
    private lateinit var context: Context
    private lateinit var store:
        HistoricalImportCheckpointStore

    @Before
    fun setUp() {
        context =
            ApplicationProvider
                .getApplicationContext()
        store =
            HistoricalImportCheckpointStore(
                context
            )
        store.clear()
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun checkpointPersistsForMatchingResumeKey() {
        val now = 1_000_000L

        store.save(
            resumeKey = "LAST_90_DAYS",
            sinceEpochMs = 100_000L,
            lastSmsDateEpochMs = 200_000L,
            lastSmsId = 42L,
            nowEpochMs = now
        )

        val checkpoint =
            store.load(
                resumeKey = "LAST_90_DAYS",
                nowEpochMs = now + 1_000L
            )

        assertNotNull(checkpoint)
        assertEquals(
            "LAST_90_DAYS",
            checkpoint?.resumeKey
        )
        assertEquals(
            100_000L,
            checkpoint?.sinceEpochMs
        )
        assertEquals(
            200_000L,
            checkpoint?.lastSmsDateEpochMs
        )
        assertEquals(
            42L,
            checkpoint?.lastSmsId
        )
    }

    @Test
    fun differentWindowDoesNotReuseCheckpoint() {
        val now = 1_000_000L

        store.save(
            resumeKey = "LAST_90_DAYS",
            sinceEpochMs = 100_000L,
            lastSmsDateEpochMs = 200_000L,
            lastSmsId = 42L,
            nowEpochMs = now
        )

        assertNull(
            store.load(
                resumeKey = "LAST_30_DAYS",
                nowEpochMs = now + 1_000L
            )
        )
    }

    @Test
    fun staleCheckpointIsDiscarded() {
        val now = 1_000_000L
        val eightDaysMs =
            8L * 24L * 60L * 60L * 1_000L

        store.save(
            resumeKey = "LAST_90_DAYS",
            sinceEpochMs = 100_000L,
            lastSmsDateEpochMs = 200_000L,
            lastSmsId = 42L,
            nowEpochMs = now
        )

        assertNull(
            store.load(
                resumeKey = "LAST_90_DAYS",
                nowEpochMs =
                    now + eightDaysMs
            )
        )
    }
}
