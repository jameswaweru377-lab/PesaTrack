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

}
