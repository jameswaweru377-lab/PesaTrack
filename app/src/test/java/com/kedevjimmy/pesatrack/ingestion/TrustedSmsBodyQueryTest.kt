package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class TrustedSmsBodyQueryTest {
    @Test fun referenceMatchesAreRestrictedToPreviouslyApprovedIdAndSenderPairs() {
        val query = trustedSmsBodyQuery(
            listOf(12L to "EQUITY", 13L to "MPESA"),
            reference = "AB_%\\C"
        )
        assertEquals(
            "((_id = ? AND address = ?) OR (_id = ? AND address = ?)) AND body LIKE ? ESCAPE '\\'",
            query.selection
        )
        assertArrayEquals(
            arrayOf("12", "EQUITY", "13", "MPESA", "%AB\\_\\%\\\\C%"),
            query.selectionArgs
        )
    }

    @Test fun ordinaryImporterKeepsOnlyExactIdAndSenderFilters() {
        val query = trustedSmsBodyQuery(listOf(7L to "M-PESA"), reference = null)
        assertEquals("(_id = ? AND address = ?)", query.selection)
        assertArrayEquals(arrayOf("7", "M-PESA"), query.selectionArgs)
    }
}
