package com.kedevjimmy.pesatrack.domain.everyday

import org.junit.Assert.*
import org.junit.Test

class StatementExpenseCatalogTest {
    private val labels = mapOf(
        "EATING_OUT" to "Eating out",
        "GROCERIES" to "Groceries",
        "WATER" to "Water bill",
        "TRANSFER" to "Transfer"
    )
    private fun row(id: String, money: Long, flow: StatementFlow): StatementClassified =
        StatementClassified(
            "test-source",
            StatementActivityLine(
                1,1,"QAB1234569",1_780_000_000_000L,
                "Synthetic source: $id",0L,money,100_000L
            ),
            flow, id
        )

    @Test fun eatingOutAndGroceriesNeverCollapseIntoFoodAndDining() {
        val rows = listOf(
            row("EATING_OUT",6_000L,StatementFlow.EXPENSE),
            row("GROCERIES",10_000L,StatementFlow.EXPENSE),
            row("EATING_OUT",3_000L,StatementFlow.EXPENSE)
        )
        val report = StatementExpenseCatalog.breakdown(rows,labels).toMap()
        assertEquals(9_000L,report["Eating out"])
        assertEquals(10_000L,report["Groceries"])
        assertFalse(report.containsKey("Food & dining"))
        assertEquals(2, StatementExpenseCatalog.matchingExpenseRows(
            rows,"Eating out",labels).size)
    }

    @Test fun transferIsNotIncludedInExpenseBreakdownOrDrillDown() {
        val rows = listOf(
            row("TRANSFER",40_000L,StatementFlow.TRANSFER),
            row("EATING_OUT",8_000L,StatementFlow.EXPENSE)
        )
        assertEquals(8_000L, StatementExpenseCatalog.breakdown(rows,labels)
            .sumOf { it.second })
        assertEquals(1,StatementExpenseCatalog.matchingExpenseRows(
            rows,null,labels).size)
        assertTrue(StatementExpenseCatalog.matchingExpenseRows(
            rows,"Transfer",labels).isEmpty())
    }

    @Test fun customAndUtilityCategoriesAreShownSeparately() {
        assertEquals("Water bill",StatementExpenseCatalog.name("WATER", labels))
        assertEquals("New lunch place",
            StatementExpenseCatalog.name("CUSTOM_1", labels + ("CUSTOM_1" to "New lunch place")))
    }
}
