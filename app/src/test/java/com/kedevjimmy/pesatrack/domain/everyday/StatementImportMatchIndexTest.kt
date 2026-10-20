package com.kedevjimmy.pesatrack.domain.everyday

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StatementImportMatchIndexTest {
    private data class Existing(
        val accountId: String?,
        val amountMinor: Long,
        val direction: String,
        val occurredAt: Long,
        val reference: String?
    )

    private val timeZone = TimeZone.getTimeZone("Africa/Nairobi")
    private val date = Calendar.getInstance(timeZone).apply {
        set(2026, Calendar.OCTOBER, 11, 9, 30, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private val row = StatementRow(date, "Paybill", -2_500L, "QAB1234567")

    private fun index(vararg rows: Existing) = StatementImportMatchIndex.from(
        rows.map {
            StatementImportMatchIndex.ExistingLeg(
                accountId = it.accountId,
                amountMinor = it.amountMinor,
                direction = it.direction,
                occurredAtEpochMs = it.occurredAt,
                reference = it.reference
            )
        },
        timeZone
    )

    @Test
    fun exactReferenceRequiresAccountDirectionAmountAndSameDay() {
        val exact = index(Existing("wallet", 2_500L, "EXPENSE", date + 30_000L, "QAB1234567"))
        assertTrue(exact.hasExactReference("wallet", row))

        assertFalse(index(Existing("bank", 2_500L, "EXPENSE", date, row.reference))
            .hasExactReference("wallet", row))
        assertFalse(index(Existing("wallet", 2_500L, "INCOME", date, row.reference))
            .hasExactReference("wallet", row))
        assertFalse(index(Existing("wallet", 2_501L, "EXPENSE", date, row.reference))
            .hasExactReference("wallet", row))
        assertFalse(index(Existing("wallet", 2_500L, "EXPENSE", date + 86_400_000L, row.reference))
            .hasExactReference("wallet", row))
    }

    @Test
    fun sameDaySameAmountWithoutReferenceIsOnlyAPossibleMatch() {
        val match = index(Existing("wallet", 2_500L, "EXPENSE", date, null))
        assertFalse(match.hasExactReference("wallet", row))
        assertTrue(match.hasPossibleMatch("wallet", row))
    }

    @Test
    fun incomeAndOutgoingTransferUseTheirExistingActivityMatchRules() {
        val income = row.copy(signedMinor = 8_000L)
        assertTrue(index(Existing("wallet", 8_000L, "INCOME", date, null))
            .hasPossibleMatch("wallet", income))
        assertTrue(index(Existing("wallet", 2_500L, "TRANSFER", date, null))
            .hasPossibleMatch("wallet", row))
        assertFalse(index(Existing("wallet", 8_000L, "TRANSFER", date, null))
            .hasPossibleMatch("wallet", income))
    }
}
