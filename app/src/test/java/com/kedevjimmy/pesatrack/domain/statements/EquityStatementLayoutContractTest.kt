package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

/** Invented money, dates and descriptions: only the six-column layout is from the screenshot. */
class EquityStatementLayoutContractTest {
    private val headers = listOf(
        "Date", "Value", "Particulars", "Money Out", "Money In", "Balance"
    )
    private val profile = StatementFormatProfile(
        aliases = mapOf("Value" to StatementField.VALUE_DATE),
        allowCreditDebitBalanceSuffix = true
    )

    @Test fun twoPagesPreserveSeparateBankChargesAndCreditBalances() {
        val pages = listOf(
            StatementTablePage(1, headers, listOf(
                listOf("01-09-2026", "", "Invented deposit", "", "1,000.00", "1,000.00 Cr"),
                listOf("02-09-2026", "", "APP/MPESA invented", "200.00", "", "800.00 Cr"),
                listOf("02-09-2026", "", "TRANSACTION + SMS CHARGE", "10.00", "", "790.00 Cr")
            )),
            StatementTablePage(2, headers, listOf(
                listOf("03-09-2026", "", "Invented salary", "", "500.00", "1,290.00 Cr"),
                listOf("04-09-2026", "", "Cash withdrawal", "300.00", "", "990.00 Cr")
            ))
        )
        val result = UniversalStatementTableInterpreter.interpret(
            pages, openingBalanceMinor = 0L, profile = profile
        )
        assertTrue(result.toString(), result is StatementTableResult.Accepted)
        val rows = (result as StatementTableResult.Accepted).legs
        assertEquals(5, rows.size)
        assertEquals(listOf(100_000L, -20_000L, -1_000L, 50_000L, -30_000L),
            rows.map { it.movementMinor })
        assertEquals(listOf(100_000L, 80_000L, 79_000L, 129_000L, 99_000L),
            rows.map { it.runningBalanceMinor })
        assertEquals("TRANSACTION + SMS CHARGE", rows[2].description)
    }

    @Test fun debitBalanceSuffixProducesNegativeBalanceOnlyWhenProven() {
        val pages = listOf(StatementTablePage(1, headers, listOf(
            listOf("02-09-2026", "", "Invented outgoing transfer", "20.00", "", "10.00 Dr"),
            listOf("03-09-2026", "", "Invented deposit", "", "30.00", "20.00 Cr")
        )))
        val accepted = UniversalStatementTableInterpreter.interpret(
            pages, openingBalanceMinor = 1_000L, profile = profile
        ) as StatementTableResult.Accepted
        assertEquals(listOf(-1_000L, 2_000L),
            accepted.legs.map { it.runningBalanceMinor })
    }

    @Test fun creditBalanceSuffixDoesNotChangeOtherProvidersWithoutOptIn() {
        val pages = listOf(StatementTablePage(1, headers, listOf(
            listOf("02-09-2026", "", "Invented deposit", "", "10.00", "10.00 Cr"),
            listOf("03-09-2026", "", "Invented withdrawal", "5.00", "", "5.00 Cr")
        )))
        val result = UniversalStatementTableInterpreter.interpret(
            pages, openingBalanceMinor = 0L,
            profile = StatementFormatProfile(mapOf("Value" to StatementField.VALUE_DATE))
        )
        assertEquals(StatementBlockReason.INVALID_AMOUNT,
            (result as StatementTableResult.Blocked).reason)
    }

    @Test fun valueColumnCannotHideUnexplainedMoney() {
        val pages = listOf(StatementTablePage(1, headers, listOf(
            listOf("02-09-2026", "250.00", "Invented deposit", "", "10.00", "10.00 Cr"),
            listOf("03-09-2026", "", "Invented withdrawal", "5.00", "", "5.00 Cr")
        )))
        val result = UniversalStatementTableInterpreter.interpret(
            pages, openingBalanceMinor = 0L, profile = profile
        )
        assertEquals(StatementBlockReason.INVALID_DATE,
            (result as StatementTableResult.Blocked).reason)
    }
}
