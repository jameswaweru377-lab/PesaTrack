package com.kedevjimmy.pesatrack.domain.interpretation

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.kedevjimmy.pesatrack.domain.learning.RememberedCategoryMatchPolicy
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.KenyanBankDirectory
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferDetector
import java.io.File
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Test-only invented inputs. Reads no Android services, SMS, database or ledger. */
class FinancialContractLabTest {
    private val root = if (File("tools/python_lab").isDirectory) File(".") else File("..")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val money = Regex("""(\b(?:Ksh|KES)\s+)([0-9][0-9,]*(?:\.[0-9]{1,2})?)""")

    @Test
    fun generatedInventedContractsMatchActualPolicies() {
        val input = File(root, "app/build/python-lab/financial-contracts.json")
        if (System.getenv("PESATRACK_SYNTHETIC_CORPUS_REQUIRED") == "1") {
            check(input.isFile) { "Required invented financial contracts missing" }
        }
        assumeTrue(input.isFile)
        val raw = File(root, "tools/python_lab/fixtures/financial_contracts.json").readBytes()
        val authored = JsonParser.parseString(raw.toString(Charsets.UTF_8)).asJsonObject
        val corpus = JsonParser.parseString(input.readText(Charsets.UTF_8)).asJsonObject
        assertEquals(1, corpus["schema_version"].asInt)
        assertEquals(true, corpus["synthetic_only"].asBoolean)
        assertEquals(1, authored["schema_version"].asInt)
        assertEquals(true, authored["synthetic_only"].asBoolean)
        val hash = MessageDigest.getInstance("SHA-256").digest(raw)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        assertEquals(hash, corpus["fixture_sha256"].asString)
        val fixtures = authored["cases"].asJsonArray.associate { it.asJsonObject["id"].asString to it.asJsonObject }
        assertEquals(28, fixtures.size)
        val rows = corpus["cases"].asJsonArray
        assertEquals(224, rows.size())
        val ids = mutableSetOf<String>()
        val failures = mutableListOf<String>()
        val output = rows.map { item ->
            val row = item.asJsonObject
            val id = row["id"].asString
            check(ids.add(id)) { "Duplicate invented contract" }
            val scale = row["scale"].asInt
            check(scale in 1..8)
            val fixtureId = row["fixture_id"].asString
            val original = fixtures.getValue(fixtureId)
            assertEquals("$fixtureId-${"%02d".format(Locale.ROOT, scale)}", id)
            assertEquals(original["group"], row["group"])
            val steps = row["steps"].asJsonArray
            val authoredSteps = original["steps"].asJsonArray
            assertEquals(authoredSteps.size(), steps.size())
            val results = steps.mapIndexed { index, element ->
                val step = element.asJsonObject
                // Independently reconstruct every allowed input and golden answer.
                val allowed = authoredSteps[index].asJsonObject.deepCopy()
                if (allowed.has("text")) {
                    allowed.addProperty("text", money.replace(allowed["text"].asString) { match ->
                        val scaled = BigDecimal(match.groupValues[2].replace(",", ""))
                            .multiply(BigDecimal.valueOf(scale.toLong()))
                        match.groupValues[1] + String.format(Locale.ROOT, "%,.2f", scaled)
                    })
                }
                val expected = allowed["expected"].asJsonObject
                listOf("amount", "outstanding", "credit").forEach { field ->
                    if (expected.has(field) && expected[field].asLong >= 0L) {
                        expected.addProperty(field, expected[field].asLong * scale)
                    }
                }
                assertEquals("Unreviewed input: $id", allowed, step)
                val actual = gson.toJsonTree(result(step))
                if (actual != expected) failures += "$id step $index expected $expected actual $actual"
                actual
            }
            mapOf("id" to id, "results" to results)
        }
        assertEquals(224, ids.size)
        val sha = System.getenv("PESATRACK_LAB_SOURCE_SHA")
        if (sha != null) {
            check(sha.matches(Regex("[0-9a-f]{40}")))
            File(root, "app/build/python-lab/financial-contract-output.json").writeText(
                gson.toJson(mapOf("schema_version" to 1, "source_sha" to sha,
                    "fixture_sha256" to hash, "cases" to output)) + "\n", Charsets.UTF_8)
        }
        assertEquals(failures.joinToString("\n"), emptyList<String>(), failures)
    }

    private fun result(step: JsonObject): Map<String, Any> = when (step["mode"].asString) {
        "bank_identity" -> mapOf("bank" to (KenyanBankDirectory.matchSender(step["title"].asString)?.id ?: ""))
        "notification" -> {
            val accounts = step["accounts"].asJsonArray.map {
                val a = it.asJsonObject
                MoneyAccount(id = a["id"].asString, name = a["name"].asString,
                    type = AccountType.valueOf(a["type"].asString), identifier = a["identifier"].asString)
            }
            val match = OwnAccountTransferDetector.matchConfiguredNotification(
                step["title"].asString, step["text"].asString, accounts)
            mapOf("match" to (match?.let { "${it.sourceAccountId}>${it.destinationAccountId}" } ?: ""))
        }
        "merchant" -> mapOf("match" to RememberedCategoryMatchPolicy.matches(
            step["source_name"].asString, step["source_phone"].asString,
            step["name"].asString, step["phone"].asString))
        "debt" -> {
            val events = FinancialMessageInterpreter.interpret(step["title"].asString, step["text"].asString).events
            check(events.size <= 1) { "Ambiguous invented debt result" }
            when (val event = events.singleOrNull()) {
                is DebtDrawdownEventFact -> mapOf("kind" to event.kind.name, "amount" to event.amountMinor,
                    "outstanding" to (event.outstandingAfterMinor ?: -1L),
                    "credit" to (event.availableCreditAfterMinor ?: -1L), "completion" to "NA")
                is DebtRepaymentEventFact -> mapOf("kind" to event.kind.name, "amount" to event.amountMinor,
                    "outstanding" to (event.outstandingAfterMinor ?: -1L),
                    "credit" to (event.availableCreditAfterMinor ?: -1L), "completion" to event.completion.name)
                else -> mapOf("kind" to "NONE", "amount" to -1L, "outstanding" to -1L,
                    "credit" to -1L, "completion" to "NA")
            }
        }
        else -> error("Unknown invented contract mode")
    }
}
