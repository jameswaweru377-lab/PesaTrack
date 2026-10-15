package com.kedevjimmy.pesatrack.domain.everyday

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StatementMatchScanPolicyTest {
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("Africa/Nairobi")
    }
    private fun row(
        batch: String,
        description: String,
        row: Int,
        visible: Boolean,
        manual: Boolean = false,
        out: Long = 10_000L
    ) = StatementMatchScanPolicy.Row(
        batchId = batch,
        accountId = "wallet-a",
        entry = StatementActivityLine(
            1, row, "QAB1234569", requireNotNull(fmt.parse("2026-10-13 13:00:00")).time,
            description, 0L, out, 100_000L
        ),
        visibleInReport = visible,
        manuallyCorrected = manual
    )
    private val kinds = mapOf("EATING_OUT" to "EXPENSE", "GROCERIES" to "EXPENSE")

    @Test fun countsAllSavedPdfsButNotOverlappingPdfInReportTotals() {
        val raw = listOf(
            row("pdf-a", "Customer Transfer to 0722000000 - Elizabeth Mbira",1, true),
            row("pdf-a", "Customer Transfer to 254722000000 ELIZABETH MBIRA",2, true),
            row("pdf-b", "Customer Transfer to 0722000000 - Elizabeth Mbira",1, false),
            row("pdf-b", "Customer Transfer of Funds to Elizabeth Mbira",2, false),
            row("pdf-c", "Pay Bill to 247247 Equity bank",1,true),
            row("pdf-c", "Customer Transfer to 0711000000 - Elizabeth Mbira",2,true),
            row("pdf-c", "Customer Transfer to 0722000000 - Elizabeth Mbira",3,true,true)
        )
        val result = StatementMatchScanPolicy.scan(
            raw, "ELIZABETH MBIRA", "0722000000","EATING_OUT",
            emptyList(),kinds,emptyMap()
        )
        assertEquals(7,result.scannedRows)
        assertEquals(3,result.documents)
        assertEquals(6,result.namedMatches)
        assertEquals(4,result.eligibleAllSources)
        assertEquals(2,result.eligibleInReports)
        assertEquals(2,result.hiddenBySourceSelection)
        assertEquals(2,result.protectedOrAlreadyCorrect)
    }

    @Test fun alreadyCategorizedAndProtectedTransactionsAreNotPromisesToUpdate() {
        val raw = listOf(
            row("pdf-a","Customer Transfer to 0722000000 - Elizabeth Mbira",1,true),
            row("pdf-a","Customer Transfer to 0722000000 - Elizabeth Mbira",2,true,true)
        )
        val result = StatementMatchScanPolicy.scan(
            raw,"Elizabeth Mbira","0722000000","EATING_OUT",
            listOf(StatementCategoryMemoryPolicy.Memory(
                "Elizabeth Mbira","0722000000","EATING_OUT"
            )),kinds,emptyMap()
        )
        assertEquals(2,result.namedMatches)
        assertEquals(0,result.eligibleAllSources)
        assertEquals(2,result.protectedOrAlreadyCorrect)
    }

    @Test fun noSavedRowsIsDistinctFromScanThatFoundNoIdentity() {
        val none = StatementMatchScanPolicy.scan(
            emptyList(),"Elizabeth Mbira","0722000000","EATING_OUT",
            emptyList(),kinds,emptyMap()
        )
        assertEquals(0,none.scannedRows)
        val missing = StatementMatchScanPolicy.scan(
            listOf(row("pdf-a","Send money to a different person",1,true)),
            "Elizabeth Mbira","0722000000","EATING_OUT",
            emptyList(),kinds,emptyMap()
        )
        assertEquals(1,missing.scannedRows)
        assertEquals(0,missing.namedMatches)
    }
}
