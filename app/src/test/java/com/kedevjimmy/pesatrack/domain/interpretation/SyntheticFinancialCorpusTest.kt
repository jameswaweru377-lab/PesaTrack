package com.kedevjimmy.pesatrack.domain.interpretation

import com.google.gson.JsonParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Never opens SMS or a ledger. Template variations are not independent bank coverage. */
class SyntheticFinancialCorpusTest {
    @Test fun generatedScenariosPreserveKindsMoneyAndReplay() {
        val root = if (File("tools/python_lab").isDirectory) File(".") else File("..")
        val input = File(root, "app/build/python-lab/synthetic-corpus.json")
        if (System.getenv("PESATRACK_SYNTHETIC_CORPUS_REQUIRED") == "1") {
            assertTrue("Generate the synthetic corpus before running CI tests", input.isFile)
        }
        assumeTrue(input.isFile)
        val document = JsonParser.parseString(input.readText()).asJsonObject
        assertEquals(1, document["schema_version"].asInt)
        assertTrue(document["synthetic_only"].asBoolean)
        val rows = document["cases"].asJsonArray
        assertEquals(2432, rows.size())
        val fixtures = FinancialMessageFixtureCorpus.all.associateBy { it.id }
        val ids = mutableSetOf<String>()
        rows.forEach { element ->
            val row = element.asJsonObject
            val id = row["id"].asString
            assertTrue(id, ids.add(id))
            val fixture = fixtures.getValue(row["fixture_id"].asString)
            val scale = row["scale"].asLong
            assertTrue(id, scale in 1L..128L)
            val actual = FinancialMessageInterpreter.interpret(row["title"].asString, row["text"].asString).toCanonicalDraft()
            assertEquals(id, fixture.expectedKinds, actual.records.map { it.kind })
            val baseline = FinancialMessageInterpreter.interpret(fixture.title, fixture.body).toCanonicalDraft()
            // Monetary metamorphic check, alongside independently authored fixture kinds/balances.
            assertEquals(id, money(baseline).map { Math.multiplyExact(it, scale) }.sorted(), money(actual).sorted())
            assertEquals(id, fixture.expectedBalances.map { Math.multiplyExact(it.amountMinor, scale) }.sorted(),
                actual.records.filterIsInstance<BalanceObservationDraft>().map { it.balanceMinor }.sorted())
            assertEquals(id, actual, FinancialMessageInterpreter.interpret(row["title"].asString, row["text"].asString).toCanonicalDraft())
        }
    }

    private fun money(draft: CanonicalFinancialMessageDraft): List<Long> = draft.records.flatMap {
        when (it) {
            is MoneyMovementDraft -> listOf(it.amountMinor, it.feeMinor)
            is DebtMovementDraft -> listOfNotNull(it.amountMinor, it.outstandingAfterMinor)
            is FeeDraft -> listOf(it.amountMinor)
            is ReversalDraft -> listOfNotNull(it.amountMinor)
            is UnresolvedMovementDraft -> listOf(it.amountMinor, it.feeMinor)
            is BalanceObservationDraft -> listOf(it.balanceMinor)
            is AvailableCreditObservationDraft -> listOf(it.availableCreditMinor)
        }
    }
}
