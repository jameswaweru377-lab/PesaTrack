package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ReportCsvWriterTest {
    @Test fun `spreadsheet formula and commas stay escaped`() {
        assertEquals("\"'=SUM(1,2)\"", reportCsvCell("=SUM(1,2)"))
        assertEquals("\"Nairobi, shop\"", reportCsvCell("Nairobi, shop"))
        assertEquals("\"Quote \"\"inside\"\"\"", reportCsvCell("Quote \"inside\""))
    }
}
