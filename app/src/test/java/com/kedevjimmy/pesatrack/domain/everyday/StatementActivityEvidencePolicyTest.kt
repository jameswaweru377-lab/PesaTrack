package com.kedevjimmy.pesatrack.domain.everyday

import org.junit.Assert.*
import org.junit.Test

class StatementActivityEvidencePolicyTest {
    private val row = StatementActivityLine(
        1,1,"QAB1234569",1_780_000_000_000L,
        "Customer Transfer to Elizabeth Mbira",0L,10_000L,100_000L
    )
    private val kinds=mapOf("EATING_OUT" to "EXPENSE")
    private fun activity(
        direction:String="EXPENSE",category:String?=null,excluded:Boolean=false,
        account:String?="wallet-a",linked:String?=null,lent:Long=0L,
        shared:Boolean=false
    )=StatementReceiptEvidence.ActivityReceipt(
        "QAB1234569",account,10_000L,row.occurredAt,direction,
        "Elizabeth Mbira","0722000000",excluded,false,12L,
        "USER_CLASSIFIED",category,linked,lent,0L,shared
    )
    private fun project(a:StatementReceiptEvidence.ActivityReceipt,manual:Boolean=false)=
        StatementActivityEvidencePolicy.apply(
            StatementFinancialPolicy.classify("pdf",row,categoryKinds=kinds),
            "wallet-a",listOf(a),kinds,manual
        )
    @Test fun excludedIsNotSpending() {
        val result=project(activity(excluded=true))
        assertEquals(StatementFlow.EXCLUDED,result.flow)
        assertEquals(10_000L,StatementFinancialPolicy.totals(listOf(result)).excludedMinor)
        assertEquals(10_000L,result.amountMinor)
    }
    @Test fun verifiedOwnTransferIsNotSpending() {
        val result=project(activity(direction="TRANSFER",category="TRANSFER",linked="bank-b"))
        assertEquals(StatementFlow.TRANSFER,result.flow)
        assertEquals(0L,StatementFinancialPolicy.totals(listOf(result)).expenseMinor)
    }
    @Test fun confirmedPersonalLoanAndSharedBillUseCorrectEconomics() {
        val lent=project(activity(lent=10_000L))
        assertEquals(StatementFlow.TRANSFER,lent.flow)
        assertEquals("MONEY_LENT",lent.categoryId)
        val shared=project(activity(category="EATING_OUT",lent=4_000L,shared=true))
        assertEquals(6_000L,shared.reportAmountMinor)
        assertEquals(10_000L,shared.amountMinor)
        assertEquals(6_000L,StatementFinancialPolicy.totals(listOf(shared)).expenseMinor)
    }
    @Test fun trustedCategoryAndManualSourcePriority() {
        val correct=project(activity(category="EATING_OUT"))
        assertEquals(StatementFlow.EXPENSE,correct.flow)
        assertEquals("EATING_OUT",correct.categoryId)
        val manual = project(activity(category="EATING_OUT"),manual=true)
        assertEquals(StatementFlow.TRANSFER,manual.flow)
        assertTrue(manual.activityReceiptVerified)
    }
    @Test fun unsafeAccountAndDateEvidenceNeverChangeSource() {
        assertFalse(project(activity(excluded=true,account="bank-b")).activityReceiptVerified)
        val late=row.copy(occurredAt=row.occurredAt-3*86_400_000L)
        val output=StatementActivityEvidencePolicy.apply(
            StatementFinancialPolicy.classify("pdf",late),"wallet-a",
            listOf(activity(excluded=true)),kinds,false
        )
        assertFalse(output.activityReceiptVerified)
    }
}
