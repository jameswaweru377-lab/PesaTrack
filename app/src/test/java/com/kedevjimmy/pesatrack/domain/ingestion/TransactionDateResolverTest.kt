package com.kedevjimmy.pesatrack.domain.ingestion

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TransactionDateResolverTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun parsesMpesaDateIntoCanonicalTimestamp() {
        val resolved = TransactionDateResolver.resolve(
            occurredAtText = "8/9/26 at 1:24 PM",
            fallbackEpochMs = 1L,
            timeZone = utc
        )

        val calendar = Calendar.getInstance(utc).apply {
            timeInMillis = resolved
        }

        assertEquals(2026, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, calendar.get(Calendar.MONTH))
        assertEquals(8, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(13, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(24, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun usesProviderTimestampWhenMessageDateCannotBeParsed() {
        val fallback = 1_700_000_000_000L

        assertEquals(
            fallback,
            TransactionDateResolver.resolve(
                occurredAtText = "not a date",
                fallbackEpochMs = fallback,
                timeZone = utc
            )
        )
    }
    @Test
    fun parsesTransactionDateWhenMessageOmitsAtKeyword() {
        val resolved = TransactionDateResolver.resolve(
            occurredAtText = "23/9/26 5:27 PM",
            fallbackEpochMs = 1L,
            timeZone = utc
        )

        val calendar = Calendar.getInstance(utc).apply {
            timeInMillis = resolved
        }

        assertEquals(2026, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, calendar.get(Calendar.MONTH))
        assertEquals(23, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(17, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(27, calendar.get(Calendar.MINUTE))
    }


    @Test
    fun parsesBankTimestampWithSeconds() {
        val resolved = TransactionDateResolver.resolve(
            occurredAtText = "29/09/2026 at 20:54:36",
            fallbackEpochMs = 1L,
            timeZone = utc
        )

        val calendar = Calendar.getInstance(utc).apply {
            timeInMillis = resolved
        }

        assertEquals(2026, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, calendar.get(Calendar.MONTH))
        assertEquals(29, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(20, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(54, calendar.get(Calendar.MINUTE))
        assertEquals(36, calendar.get(Calendar.SECOND))
    }
}
