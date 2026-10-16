package com.kedevjimmy.pesatrack.domain.everyday

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StatementReceiptEvidenceTest {
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("Africa/Nairobi")
    }
    private val at = requireNotNull(fmt.parse("2026-10-15 13:27:00")).time
    private val line = StatementActivityLine(
        1,1,"QAB1234569",at,"Customer Transfer to 0722XXX Elizabeth M.",
        0L,10_000L,23_000L
    )
    private fun activity(
        amount: Long = 10_000L, account: String? = "wallet-a",
        time: Long = at, phone: String? = "0722000000",
        direction: String = "EXPENSE"
    ) = StatementReceiptEvidence.ActivityReceipt(
        "QAB1234569",account,amount,time,direction,
        "Elizabeth Mbira",phone,excluded=false,structurallyLinked=false
    )
    private val memories = listOf(StatementCategoryMemoryPolicy.Memory(
        "Elizabeth Mbira","0722000000","EATING_OUT"
    ))

    @Test fun exactReceiptAmountTimeAndAccountClassifyOpaquePdfDescription() {
        val evidence = mapOf("QAB1234569" to listOf(activity()))
        assertNull(StatementCategoryMemoryPolicy.learnedFor(line.description, memories))
        assertEquals("EATING_OUT", StatementReceiptEvidence.categoryFor(
            line,"wallet-a",evidence,memories
        ))
    }

    @Test fun duplicateCodeMismatchAndWrongAccountAreRejected() {
        assertNull(StatementReceiptEvidence.corroborated(
            line,"wallet-a",listOf(activity(),activity())))
        assertNull(StatementReceiptEvidence.corroborated(
            line,"wallet-a",listOf(activity(amount=11_000L))))
        assertNull(StatementReceiptEvidence.corroborated(
            line,"wallet-b",listOf(activity())))
        assertNull(StatementReceiptEvidence.corroborated(
            line,"wallet-a",listOf(activity(time=at - 2*24*60*60*1000L))))
        assertNull(StatementReceiptEvidence.corroborated(
            line,"wallet-a",listOf(activity(direction="TRANSFER"))))
    }

    @Test fun differentRecipientPhoneOrNameCannotInheritMemory() {
        val conflicting = mapOf("QAB1234569" to listOf(activity(phone="0711000000")))
        assertNull(StatementReceiptEvidence.categoryFor(
            line,"wallet-a",conflicting,memories
        ))
        val other = listOf(StatementCategoryMemoryPolicy.Memory(
            "Someone Else","0722000000","EATING_OUT"
        ))
        assertNull(StatementReceiptEvidence.categoryFor(
            line,"wallet-a",mapOf("QAB1234569" to listOf(activity())),other
        ))
    }

    @Test fun scannerAndReportingIdentifySameReceiptWithoutSourceName() {
        val row = StatementMatchScanPolicy.Row(
            "pdf-a","wallet-a",line,true,false
        )
        val receipts = mapOf("QAB1234569" to listOf(activity()))
        val result = StatementMatchScanPolicy.scan(
            listOf(row),"Elizabeth Mbira","0722000000","EATING_OUT",
            memories, mapOf("EATING_OUT" to "EXPENSE"),emptyMap(),receipts
        )
        // Existing memory already means the row is correctly classified in the
        // report; the preview must not promise to re-categorize it.
        assertEquals(1,result.receiptMatches)
        assertEquals(0,result.eligibleAllSources)
        val classified = StatementFinancialPolicy.classify(
            "pdf-a",line,
            rememberedCategory=StatementReceiptEvidence.categoryFor(
                line,"wallet-a",receipts,memories
            ),categoryKinds=mapOf("EATING_OUT" to "EXPENSE")
        )
        assertEquals(StatementFlow.EXPENSE,classified.flow)
        assertEquals("EATING_OUT",classified.categoryId)
    }

    @Test fun matchingReceiptCannotOverrideClearlyDifferentPrintedPerson() {
        val otherName = line.copy(
            description = "Customer Transfer to 0722000000 - Another Person"
        )
        val wrongPhone = line.copy(
            description = "Customer Transfer to 0711000000 - Elizabeth Mbira"
        )
        assertNull(StatementReceiptEvidence.corroborated(
            otherName,"wallet-a",listOf(activity())))
        assertNull(StatementReceiptEvidence.corroborated(
            wrongPhone,"wallet-a",listOf(activity())))
    }

}
