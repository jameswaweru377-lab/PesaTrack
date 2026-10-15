package com.kedevjimmy.pesatrack.domain.everyday

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StatementCategoryMemoryPolicyTest {
    private val policy = StatementCategoryMemoryPolicy
    private val kinds = mapOf("EATING_OUT" to "EXPENSE", "GROCERIES" to "EXPENSE",
        "TRANSFER" to "TRANSFER", "DEBT_LOAN" to "ALLOCATION")
    private val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("Africa/Nairobi")
    }
    private fun row(description: String, withdrawn: Long = 10000L) = StatementActivityLine(
        page = 1, row = 1, reference = "QAB1234569",
        occurredAt = requireNotNull(parser.parse("2024-03-15 08:00:00")).time,
        description = description, paidInMinor = 0L, withdrawnMinor = withdrawn,
        balanceMinor = 30000L
    )

    @Test fun existingPhoneBasedActivityMemoryAppliesToNameOnlyHistoricalPdf() {
        val remembered = listOf(StatementCategoryMemoryPolicy.Memory("Elizabeth Mbira", "0722000000", "EATING_OUT"))
        assertEquals("EATING_OUT", policy.learnedFor(
            "Customer Transfer to Elizabeth Mbira", remembered))
        assertEquals("EATING_OUT", policy.learnedFor(
            "Send money to  ELIZABETH   MBIRA", remembered))
        assertTrue(policy.proposedSourceMatches(
            "Elizabeth Mbira", "0722000000", "Customer Transfer to Elizabeth Mbira", remembered))
    }

    @Test fun differentPeopleWithSameNameAndDifferentPhonesDoNotAutoApply() {
        val remembered = listOf(
            StatementCategoryMemoryPolicy.Memory("Elizabeth Mbira", "0722000000", "EATING_OUT"),
            StatementCategoryMemoryPolicy.Memory("ELIZABETH MBIRA", "0711000000", "GROCERIES")
        )
        assertNull(policy.learnedFor("Customer Transfer to Elizabeth Mbira", remembered))
        assertFalse(policy.proposedSourceMatches(
            "Elizabeth Mbira", "0722000000", "Customer Transfer to Elizabeth Mbira", remembered))
    }

    @Test fun partialNamesAndAmbiguousNumericDescriptionsCannotLearn() {
        val memory = listOf(StatementCategoryMemoryPolicy.Memory("Elizabeth Mbira", "0722000000", "EATING_OUT"))
        assertNull(policy.learnedFor("Send money to Elizabeth", memory))
        assertEquals("EATING_OUT", policy.learnedFor(
            "Customer Transfer to 0722000000 Elizabeth Mbira", memory))
        assertNull(policy.learnedFor(
            "Customer Transfer to 0711000000 Elizabeth Mbira", memory))
        assertNull(policy.learnedFor("Send money to 0722000000", memory))
        assertFalse(policy.proposedSourceMatches(
            "Elizabeth Mbira", "0722000000",
            "Customer Transfer to 0711000000 Elizabeth Mbira", memory))
        assertNull(policy.learnedFor("Fuliza repayment", memory))
    }

    @Test fun manualStatementCorrectionWinsAndStructuralRowsCannotChange() {
        val person = row("Customer Transfer to Elizabeth Mbira")
        assertTrue(policy.wouldChange("batch", person,"EATING_OUT",null,false,kinds))
        assertFalse(policy.wouldChange("batch", person,"EATING_OUT",null,true,kinds))
        assertFalse(policy.wouldChange("batch", person,"EATING_OUT","EATING_OUT",false,kinds))
        assertFalse(policy.wouldChange("batch", row("Fuliza loan repayment"),
            "EATING_OUT",null,false,kinds))
        assertFalse(policy.wouldChange("batch", row("Transfer to own Equity account"),
            "EATING_OUT",null,false,kinds))
        assertFalse(policy.wouldChange("batch", row("Customer Transfer to Elizabeth Mbira",0L),
            "EATING_OUT",null,false,kinds))
    }

    @Test fun conflictingMemoriesNeverSilentlyChooseCategory() {
        val memories = listOf(
            StatementCategoryMemoryPolicy.Memory("Elizabeth Mbira", "0722000000", "EATING_OUT"),
            StatementCategoryMemoryPolicy.Memory("Elizabeth Mbira", "0722000000", "GROCERIES")
        )
        assertNull(policy.learnedFor("Customer Transfer to Elizabeth Mbira", memories))
    }

    @Test fun phonePrefixedMpesaDescriptionMatchesFullNameAfterLearning() {
        val memories = listOf(StatementCategoryMemoryPolicy.Memory(
            "Elizabeth Mbira", "0722000000", "EATING_OUT"))
        assertEquals("EATING_OUT", policy.learnedFor(
            "Customer Transfer to 0722000000 - ELIZABETH MBIRA", memories))
        assertEquals("EATING_OUT", policy.learnedFor(
            "Customer Transfer to 254722000000 ELIZABETH MBIRA by - 254746***497 JAMES", memories))
        assertEquals("EATING_OUT", policy.learnedFor(
            "Send money to 0722****00 ELIZABETH MBIRA", memories))
        assertEquals("EATING_OUT", policy.learnedFor(
            "Customer Transfer to 0722000000ELIZABETH MBIRA", memories))
        assertTrue(policy.proposedSourceMatches(
            "Elizabeth Mbira","0722000000",
            "Customer Transfer to 0722000000 - ELIZABETH MBIRA", memories))
    }

    @Test fun phoneOnlyAndUnrelatedPaybillCannotInheritEatingOutMemory() {
        val memories = listOf(StatementCategoryMemoryPolicy.Memory(
            "Elizabeth Mbira", "0722000000", "EATING_OUT"))
        assertNull(policy.learnedFor("Customer Transfer to 0722000000",memories))
        assertNull(policy.learnedFor(
            "Pay Bill to 247247 - Equity Paybill Account Acc. 0766145429", memories))
        assertNull(policy.learnedFor(
            "Customer Transfer to 0722000000 - ELIZABETH 0744332211",memories))
        assertFalse(policy.proposedSourceMatches(
            "Elizabeth Mbira","0722000000",
            "Customer Transfer to 0722000000", memories))
    }


    @Test fun explicitlyRememberedActivityCategoryAppearsInBothPastAndFutureStatementReports() {
        val memory = listOf(StatementCategoryMemoryPolicy.Memory(
            "Elizabeth Mbira", "0722000000", "EATING_OUT"
        ))
        val kinds = mapOf("EATING_OUT" to "EXPENSE")
        val march = row("Customer Transfer to 0722000000 - Elizabeth Mbira")
        val october = march.copy(
            occurredAt = requireNotNull(parser.parse("2026-10-15 13:27:00")).time,
            reference = "QAB5678901", row = 2
        )
        val separatedBySource = listOf(
            "historical-pdf" to march, "future-pdf" to october
        )
        val classified = separatedBySource.map { (batchId, original) ->
            val approved = policy.learnedFor(original.description, memory)
            StatementFinancialPolicy.classify(
                batchId = batchId,
                line = original,
                rememberedCategory = approved,
                categoryKinds = kinds
            )
        }
        assertEquals(2, classified.size)
        classified.forEach {
            assertEquals(StatementFlow.EXPENSE, it.flow)
            assertEquals("EATING_OUT", it.categoryId)
            assertEquals("Remembered category", it.evidence)
        }
        assertEquals(
            20_000L, StatementFinancialPolicy.totals(classified).expenseMinor
        )
        // This is reporting only: the original source references and
        // immutable source descriptions remain unchanged.
        assertEquals("historical-pdf", classified[0].batchId)
        assertEquals("future-pdf", classified[1].batchId)
        assertEquals("Customer Transfer to 0722000000 - Elizabeth Mbira",
            separatedBySource[0].second.description)
    }

    @Test fun learnedExpenseCannotReclassifyProtectedTransferAndManualStatementEditWins() {
        val memories = listOf(
            StatementCategoryMemoryPolicy.Memory("Elizabeth Mbira", "0722000000", "EATING_OUT")
        )
        val kinds = mapOf(
            "EATING_OUT" to "EXPENSE", "GROCERIES" to "EXPENSE"
        )
        val person = row("Customer Transfer to 0722000000 - Elizabeth Mbira")
        val remembered = policy.learnedFor(person.description, memories)
        val manual = StatementFinancialPolicy.classify(
            "pdf-A", person, assignedCategory = "GROCERIES",
            rememberedCategory = remembered, categoryKinds = kinds
        )
        assertEquals(StatementFlow.EXPENSE, manual.flow)
        assertEquals("GROCERIES", manual.categoryId)
        assertEquals("User category", manual.evidence)
        val own = row("Transfer to own Equity account")
        val protected = StatementFinancialPolicy.classify(
            "pdf-B", own, rememberedCategory = "EATING_OUT", categoryKinds = kinds
        )
        assertEquals(StatementFlow.TRANSFER, protected.flow)
        assertEquals(0L, StatementFinancialPolicy.totals(listOf(protected)).expenseMinor)
    }

}
