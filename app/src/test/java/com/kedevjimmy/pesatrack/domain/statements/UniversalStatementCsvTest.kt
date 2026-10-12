package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

/** Provider-neutral synthetic bank/wallet CSVs only. */
class UniversalStatementCsvTest {
    @Test fun multilineQuotedMemoStaysOneCellAndReconciles() {
        val text = "Posting Date,Narration,Debit (KES),Credit (KES),Running Balance,Channel\r\n" +
            "2026-10-08,\"Transfer from\ninternal savings\",,1000.00,1500.00,Online\r\n" +
            "2026-10-09,\"Pay, bill\",250.00,,1250.00,USSD\r\n" +
            "2026-10-10,Refund,,100.00,1350.00,Online\r\n"
        val result = UniversalStatementCsv.inspect(text, openingBalanceMinor = 50000L)
        assertTrue(result.toString(), result is StatementTableResult.Accepted)
        val accepted = result as StatementTableResult.Accepted
        assertEquals(listOf(100000L, -25000L, 10000L), accepted.legs.map { it.movementMinor })
        assertEquals("Transfer from\ninternal savings", accepted.legs.first().description)
        // The engine preserves wallet cash movements; it does NOT call the
        // savings transfer income or make a duplicate entry in the ledger.
    }

    @Test fun malformedQuotesFailWithoutPartialImport() {
        val text = "Date,Description,Debit,Credit,Balance\n" +
            "2026-10-08,\"Unclosed narrative,,100.00,500.00\n"
        assertNull(UniversalStatementCsv.decode(text))
        assertEquals(StatementBlockReason.INVALID_STRUCTURE,
            (UniversalStatementCsv.inspect(text) as StatementTableResult.Blocked).reason)
    }

    @Test fun unknownMoneyColumnCannotBeQuietlyDropped() {
        val text = "Date,Narration,Debit,Credit,Balance,Service Adjustment\n" +
            "2026-10-08,Deposit,,100,510,KES 2.00\n" +
            "2026-10-09,Payment,20,,490,0.00"
        val result = UniversalStatementCsv.inspect(text, openingBalanceMinor = 41000)
        assertEquals(StatementBlockReason.UNKNOWN_MONETARY_COLUMN,
            (result as StatementTableResult.Blocked).reason)
    }

    @Test fun repeatedCalendarDateTransactionsCanStillReconcileByBalances() {
        val text = "Date,Narration,Debit,Credit,Balance\n" +
            "2026-10-09,First,,100,510\n" +
            "2026-10-09,Second,20,,490\n" +
            "2026-10-09,Third,,10,500"
        val result = UniversalStatementCsv.inspect(text, openingBalanceMinor = 41000L)
        assertTrue(result.toString(), result is StatementTableResult.Accepted)
        assertEquals(3,(result as StatementTableResult.Accepted).legs.size)
    }

    @Test fun unknownDirectionWithUnprovenBalancesCannotImport() {
        val text = "Date,Description,Amount,Balance\n" +
            "2026-10-08,Payment,20.00,490.00\n" +
            "2026-10-09,Payment,20.00,470.00"
        val result = UniversalStatementCsv.inspect(text)
        assertEquals(StatementBlockReason.BALANCE_MISMATCH,
            (result as StatementTableResult.Blocked).reason)
    }

    @Test fun noAmountOrBalanceTableCannotAccidentallyQualify() {
        val text = "Date,Description,Amount\n" +
            "2026-10-08,Loan received,100.00\n2026-10-09,Payment,-20.00"
        val result = UniversalStatementCsv.inspect(text)
        assertEquals(StatementBlockReason.INVALID_STRUCTURE,
            (result as StatementTableResult.Blocked).reason)
    }

    @Test fun csvColumnCountMismatchNeverShiftsMoneyIntoBalance() {
        val text = "Date,Description,Debit,Credit,Balance\n" +
            "2026-10-08,Deposit,,100,510\n" +
            "2026-10-09,Payment,20,490"
        val result = UniversalStatementCsv.inspect(text)
        assertEquals(StatementBlockReason.INVALID_STRUCTURE,
            (result as StatementTableResult.Blocked).reason)
    }
}
