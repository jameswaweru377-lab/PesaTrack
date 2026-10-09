package com.kedevjimmy.pesatrack.domain.interpretation

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Development-only bridge. No Android services, SMS, database or ledger access. */
class PythonFinancialLabExportTest {
    private val root: File
        get() = if (File("tools/python_lab").isDirectory) File(".") else File("..")

    private fun approvedCases(): Map<String, String> {
        val manifest = JsonParser.parseString(
            File(root, "tools/python_lab/fixtures/kotlin_corpus_manifest.json")
                .readText(Charsets.UTF_8)
        ).asJsonObject
        assertEquals("FinancialMessageFixtureCorpus-v1", manifest["corpus"].asString)
        val rows = manifest["cases"].asJsonArray
        val approved = rows.associate { element ->
            val row = element.asJsonObject
            val text = row["title"].asString + "\n" + row["text"].asString
            assertEquals(row["sha256"].asString, digest(text))
            row["id"].asString to row["sha256"].asString
        }
        assertEquals("duplicate approved IDs", rows.size(), approved.size)
        return approved
    }

    private fun digest(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    @Test
    fun corpusMatchesReviewedSanitizedManifest() {
        val approved = approvedCases()
        val fixtures = FinancialMessageFixtureCorpus.all
        assertEquals(approved.keys, fixtures.map { it.id }.toSet())
        assertEquals(approved.size, fixtures.size)
        fixtures.forEach { fixture ->
            assertEquals(fixture.id, approved[fixture.id], digest(fixture.title + "\n" + fixture.body))
        }
    }

    @Test
    fun exportsActualKotlinKindsOnlyWhenExplicitlyEnabled() {
        assumeTrue(System.getenv("PESATRACK_PYTHON_LAB_EXPORT") == "1")
        corpusMatchesReviewedSanitizedManifest()
        val sourceSha = System.getenv("PESATRACK_LAB_SOURCE_SHA") ?: error("Checkout SHA required")
        require(sourceSha.matches(Regex("[0-9a-f]{40}")))
        val approved = approvedCases()
        val cases = FinancialMessageFixtureCorpus.all.map { fixture ->
            val canonical = FinancialMessageInterpreter.interpret(
                title = fixture.title,
                body = fixture.body
            ).toCanonicalDraft()
            val kinds = canonical.records.map { it.kind }
            assertEquals(fixture.id, fixture.expectedKinds, kinds)
            mapOf(
                "id" to fixture.id,
                "sha256" to approved.getValue(fixture.id),
                "kinds" to kinds.map { it.name },
                "recognized" to canonical.recognized,
                "requires_legacy_fallback" to canonical.requiresLegacyFallback
            )
        }
        val output = File(root, "app/build/python-lab/kotlin-output.json")
        val directory = checkNotNull(output.parentFile)
        check(directory.mkdirs() || directory.isDirectory)
        output.writeText(
            GsonBuilder().setPrettyPrinting().create().toJson(
                mapOf(
                    "schema_version" to 1,
                    "corpus" to "FinancialMessageFixtureCorpus-v1",
                    "source_sha" to sourceSha,
                    "cases" to cases
                )
            ) + "\n",
            Charsets.UTF_8
        )
    }
}
