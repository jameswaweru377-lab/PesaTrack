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
    @Test fun ownedAccountMirrorExcludedInActivityStillShowsInStatementTransfers() {
        // Canonical Activity excludes the incoming mirror to avoid double counting.
        // The separately imported bank/wallet statement still contains that cash leg.
        val inbound = row.copy(paidInMinor = 10_000L, withdrawnMinor = 0L)
        val original = StatementFinancialPolicy.classify("pdf", inbound, categoryKinds = kinds)
        val mirror = activity(direction = "INCOME", excluded = true)
            .copy(confirmedOwnedTransferMirror = true)
        val corrected = StatementActivityEvidencePolicy.apply(
            original, "wallet-a", listOf(mirror), kinds, false
        )
        assertEquals(StatementFlow.TRANSFER, corrected.flow)
        assertEquals("TRANSFER", corrected.categoryId)
        assertEquals(0L, StatementFinancialPolicy.totals(listOf(corrected)).incomeMinor)
        assertEquals(10_000L, StatementFinancialPolicy.totals(listOf(corrected)).transferMinor)
        assertEquals(0L, StatementFinancialPolicy.totals(listOf(corrected)).excludedMinor)
    }

    @Test fun ordinaryUserExclusionRemainsExcludedRatherThanTransfer() {
        val ordinary = project(activity(excluded = true))
        assertEquals(StatementFlow.EXCLUDED, ordinary.flow)
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
    @Test fun incomingOwnedAccountTransferIsVerifiedAsTransferNotSalary() {
        val incoming = row.copy(paidInMinor = 10_000L, withdrawnMinor = 0L)
        val own = activity(direction = "TRANSFER",category = "TRANSFER",linked = "bank-b")
        val projected = StatementActivityEvidencePolicy.apply(
            StatementFinancialPolicy.classify("pdf",incoming),"wallet-a",
            listOf(own),kinds,false
        )
        assertEquals(StatementFlow.TRANSFER, projected.flow)
        assertEquals(0L,StatementFinancialPolicy.totals(listOf(projected)).incomeMinor)
    }
    @Test fun investmentFromApprovedActivityLearningIsStillInvestment() {
        val automatic = activity(direction = "TRANSFER", category = "INVESTMENT")
            .copy(reviewState = "AUTO_CLASSIFIED", approvedLearnedCategory = true)
        val projected = project(automatic)
        assertEquals(StatementFlow.INVESTMENT,projected.flow)
        val unapproved = project(automatic.copy(approvedLearnedCategory = false))
        assertNotEquals(StatementFlow.INVESTMENT,unapproved.flow)
    }
    @Test fun rememberedActivityEatingOutMustAppearInStatementExpenses() {
        // Existing confirmed identity/category rule bulk-updated this SMS.
        val memorized = activity(category = "EATING_OUT")
            .copy(reviewState = "AUTO_CLASSIFIED", approvedLearnedCategory = true)
        val result = project(memorized)
        assertEquals(StatementFlow.EXPENSE, result.flow)
        assertEquals("EATING_OUT", result.categoryId)
        assertEquals(10_000L, StatementFinancialPolicy.totals(listOf(result)).expenseMinor)
        val unapproved = project(memorized.copy(approvedLearnedCategory = false))
        assertEquals(StatementFlow.TRANSFER, unapproved.flow)
    }

    @Test fun approvedTransferRequiresMatchingCashLegAndNoInventedOwnership() {
        val approved = activity(direction = "TRANSFER", category = "TRANSFER")
            .copy(reviewState = "AUTO_CLASSIFIED", approvedLearnedCategory = true)
        val result = project(approved)
        assertEquals(StatementFlow.TRANSFER, result.flow)
        assertEquals("TRANSFER", result.categoryId)
        assertEquals("Reviewed transfer category corroborated in Activity", result.evidence)
        val badAccount = project(approved.copy(accountId = "wallet-b"))
        assertFalse(badAccount.activityReceiptVerified)
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

    @Test fun activityNavigationRequiresUniqueExactReceiptIdentity() {
        val matching = activity()
        assertEquals(12L, StatementActivityEvidencePolicy.verified(
            row, "wallet-a", listOf(matching))?.transactionId)
        assertNull(StatementActivityEvidencePolicy.verified(
            row, "wallet-a", listOf(matching, matching)))
        assertNull(StatementActivityEvidencePolicy.verified(
            row, "wallet-a", listOf(matching.copy(accountId = "different-wallet"))))
        assertNull(StatementActivityEvidencePolicy.verified(
            row, "wallet-a", listOf(matching.copy(amountMinor = 9_999L))))
        assertNull(StatementActivityEvidencePolicy.verified(
            row, "wallet-a", listOf(matching.copy(occurredAtEpochMs =
                row.occurredAt + 2 * 86_400_000L))))
        assertNull(StatementActivityEvidencePolicy.verified(
            row, "wallet-a", listOf(matching.copy(structurallyLinked = true))))
    }
}
