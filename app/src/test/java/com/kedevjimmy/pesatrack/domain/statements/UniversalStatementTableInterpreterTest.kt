package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

/** Invented test data only. These cases do not contain any real statement information. */
class UniversalStatementTableInterpreterTest {
    private val zone = TimeZone.getTimeZone("Africa/Nairobi")
    private val walletHeaders = listOf(
        "Receipt No.", "Completion Time", "Details", "Transaction Status",
        "Paid In", "Withdrawn", "Balance"
    )
    private val walletRows = listOf(
        listOf("ABC1234569", "2026-10-10 10:30:00", "Shop payment", "Completed", "", "-20.00", "490.00"),
        listOf("ABC1234568", "2026-10-09 10:30:00", "Money returned", "Completed", "100.00", "", "510.00"),
        listOf("ABC1234567", "2026-10-08 10:30:00", "Transport", "Completed", "", "-50.00", "410.00")
    )
    private fun page(headers: List<String> = walletHeaders, rows: List<List<String>> = walletRows, n: Int = 1) =
        StatementTablePage(n, headers, rows)

    private fun accepted(
        pages: List<StatementTablePage>, opening: Long? = null,
        profile: StatementFormatProfile = StatementFormatProfile()
    ): StatementTableResult.Accepted {
        val result = UniversalStatementTableInterpreter.interpret(pages, zone, opening, profile)
        assertTrue("Expected accepted, got " + result, result is StatementTableResult.Accepted)
        return result as StatementTableResult.Accepted
    }

    private fun blocked(
        pages: List<StatementTablePage>, expected: StatementBlockReason,
        opening: Long? = null
    ) {
        val result = UniversalStatementTableInterpreter.interpret(pages, zone, opening)
        assertTrue("Expected blocked, got " + result, result is StatementTableResult.Blocked)
        assertEquals(expected, (result as StatementTableResult.Blocked).reason)
    }

    @Test fun mpesaStyleNegativeWithdrawnAndPositivePaidInReconcileWithoutExpenseClassification() {
        val result = accepted(listOf(page()), 46_000L)
        assertEquals(3, result.sourceRows)
        assertEquals(listOf(-2000L, 10000L, -5000L), result.legs.map { it.movementMinor })
        assertFalse(result.oldestLegUnanchored)
        assertEquals("Money returned", result.legs[1].description)
    }

    @Test fun bankStyleDebitAndCreditWithOptionalBranchColumnWorks() {
        val headers = listOf("Date", "Narration", "Debit", "Credit", "Running Bal.", "Branch")
        val rows = listOf(
            listOf("08/10/2026", "Opening deposit", "", "100.00", "1,100.00", "Central"),
            listOf("09/10/2026", "Cash withdrawal", "200.00", "", "900.00", "North"),
            listOf("10/10/2026", "Refund to account", "", "150.00", "1,050.00", "Central")
        )
        val result = accepted(listOf(page(headers, rows)), 100_000L)
        assertEquals(listOf(10000L, -20000L, 15000L), result.legs.map { it.movementMinor })
        assertEquals(105000L, result.legs.last().runningBalanceMinor)
    }

    @Test fun reorderedColumnsCanChangeAtPageBoundaryWithoutLosingReceipt() {
        val first = page(rows = walletRows.take(1))
        val swappedHeaders = listOf("Balance", "Withdrawn", "Details", "Paid In", "Completion Time", "Receipt No.", "Status")
        val swapped = walletRows.drop(1).map { row ->
            listOf(row[6], row[5], row[2], row[4], row[1], row[0], row[3])
        }
        val result = accepted(listOf(first, page(swappedHeaders, swapped, 2)), 46_000L)
        assertEquals(listOf(1, 2, 2), result.legs.map { it.pageNumber })
        assertEquals(3, result.sourceRows)
    }

    @Test fun signedAmountsWithDrCrIndicatorAreNotAssumedToBeIncome() {
        val headers = listOf("Transaction Date", "Description", "Amount", "Dr/Cr", "Closing Balance")
        val rows = listOf(
            listOf("2026-10-08", "Salary deposit", "1000.00", "CR", "3,000.00"),
            listOf("2026-10-09", "ATM withdrawal", "500.00", "DR", "2,500.00"),
            listOf("2026-10-10", "Charge reversal", "10.00", "CR", "2,510.00")
        )
        val result = accepted(listOf(page(headers, rows)), 200_000L)
        assertEquals(listOf(100000L, -50000L, 1000L), result.legs.map { it.movementMinor })
    }

    @Test fun validatedProfileAliasesAreReusableAcrossIssuers() {
        val headers = listOf("Event Day", "Memo", "Outflow", "Inflow", "Remaining")
        val rows = listOf(
            listOf("2026-10-08", "Funds added", "", "100", "510"),
            listOf("2026-10-09", "Payment", "20", "", "490")
        )
        val profile = StatementFormatProfile(mapOf(
            "Event Day" to StatementField.DATE,
            "Memo" to StatementField.DESCRIPTION,
            "Outflow" to StatementField.DEBIT,
            "Inflow" to StatementField.CREDIT,
            "Remaining" to StatementField.BALANCE
        ))
        val result = accepted(listOf(page(headers, rows)), 41_000L, profile)
        assertEquals(listOf(10000L, -2000L), result.legs.map { it.movementMinor })
    }

    @Test fun repeatedReferenceCanRepresentTwoDifferentRealAccountLegs() {
        val rows = listOf(
            listOf("ABC1234569", "2026-10-10 10:30:00", "Payment", "Completed", "", "-20.00", "490.00"),
            listOf("ABC1234569", "2026-10-09 10:30:00", "Separate fee", "Completed", "", "-10.00", "510.00"),
            listOf("ABC1234567", "2026-10-08 10:30:00", "Deposit", "Completed", "100.00", "", "520.00")
        )
        val result = accepted(listOf(page(rows = rows)), 42_000L)
        assertEquals(2, result.legs.count { it.reference == "ABC1234569" })
    }

    @Test fun explicitlyZeroInformationalLegRemainsBalanceEvidence() {
        val rows = listOf(
            walletRows[0],
            listOf("ABC1234561", "2026-10-09 11:00:00", "Notice", "Completed", "0.00", "0.00", "510.00"),
            walletRows[1].toMutableList().also { it[1] = "2026-10-09 10:30:00" },
            walletRows[2]
        )
        val result = accepted(listOf(page(rows = rows)), 46_000L)
        assertEquals(4, result.sourceRows)
        assertEquals(1, result.legs.count { it.informationalZero })
    }

    @Test fun unknownNumericColumnCannotBeSilentlyIgnored() {
        blocked(listOf(page(walletHeaders + "Mystery Amount",
            walletRows.map { it + "3.00" })), StatementBlockReason.UNKNOWN_MONETARY_COLUMN)
    }

    @Test fun nonzeroFeeColumnNeedsExplicitAccountingContract() {
        blocked(listOf(page(walletHeaders + "Fee",
            walletRows.map { it + "5.00" })), StatementBlockReason.UNSUPPORTED_FINANCIAL_COLUMN)
    }

    @Test fun badlyNamedDuplicateCreditColumnIsAmbiguous() {
        blocked(listOf(page(walletHeaders + "Credit",
            walletRows.map { it + "" })), StatementBlockReason.AMBIGUOUS_HEADER)
    }

    @Test fun incorrectRunningBalanceBlocksWholeDocument() {
        val wrong = walletRows.mapIndexed { index, row ->
            if (index == 1) row.toMutableList().also { it[6] = "610.00" } else row
        }
        blocked(listOf(page(rows = wrong)), StatementBlockReason.BALANCE_MISMATCH)
    }

    @Test fun wrongOpeningBalanceCannotBeInventedFromLaterMovements() {
        blocked(listOf(page()), StatementBlockReason.BALANCE_MISMATCH, 20_000L)
    }

    @Test fun missingCashMovementCannotMasqueradeAsZero() {
        val altered = walletRows.toMutableList()
        altered[1] = altered[1].toMutableList().also { it[4] = ""; it[5] = "" }
        blocked(listOf(page(rows = altered)), StatementBlockReason.INVALID_AMOUNT)
    }

    @Test fun ambiguousFutureOrInvalidDateRejectsAllRows() {
        val altered = walletRows.toMutableList()
        altered[1] = altered[1].toMutableList().also { it[1] = "2026-13-44 10:30:00" }
        blocked(listOf(page(rows = altered)), StatementBlockReason.INVALID_DATE)
    }

    @Test fun dateReorderingCannotBeMisreadAsCorrectFinancialSequence() {
        val altered = walletRows.toMutableList()
        altered[1] = altered[1].toMutableList().also { it[1] = "2026-10-12 10:30:00" }
        blocked(listOf(page(rows = altered)), StatementBlockReason.INCONSISTENT_ORDER)
    }

    @Test fun pendingAndFailedEntriesCannotBecomeWalletMovements() {
        val altered = walletRows.toMutableList()
        altered[1] = altered[1].toMutableList().also { it[3] = "Pending" }
        blocked(listOf(page(rows = altered)), StatementBlockReason.UNKNOWN_STATUS)
    }

    @Test fun exactDuplicateRowsDoNotSlipThroughASecondPage() {
        val first = page(rows = walletRows.take(2))
        val second = page(rows = listOf(walletRows[1], walletRows[2]), n = 2)
        blocked(listOf(first, second), StatementBlockReason.DUPLICATE_COMPLETE_ROW)
    }

    @Test fun unanchoredOldestRowMustRemainExplicitlyUnverified() {
        val result = accepted(listOf(page()))
        assertTrue(result.oldestLegUnanchored)
    }

    @Test fun singleRowNeedsExternalOpeningBalance() {
        blocked(listOf(page(rows = walletRows.take(1))), StatementBlockReason.INSUFFICIENT_BALANCE_PROOF)
    }

    @Test fun invalidColumnCountsCannotCauseIndexErrorOrSkipRows() {
        blocked(listOf(page(rows = listOf(walletRows[0], walletRows[1].dropLast(1)))),
            StatementBlockReason.INVALID_STRUCTURE)
    }

    @Test fun contradictorySignedCreditDirectionIsRejected() {
        val headers = listOf("Date", "Description", "Amount", "Dr/Cr", "Balance")
        val rows = listOf(
            listOf("2026-10-08", "Money in", "-100.00", "CR", "510.00"),
            listOf("2026-10-09", "Payment", "20.00", "DR", "490.00")
        )
        blocked(listOf(page(headers, rows)), StatementBlockReason.AMBIGUOUS_DIRECTION)
    }
}
