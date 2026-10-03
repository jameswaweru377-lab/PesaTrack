package com.kedevjimmy.pesatrack.domain.messagepack

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialIntelligenceV2FoundationCorpus
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageFixtureCorpus
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpreter
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageNormalizer
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageRelevanceGate
import com.kedevjimmy.pesatrack.domain.interpretation.FixtureCoverage
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader

class BrainV3AcceptanceGateTest {
    @Test
    fun relevanceAccuracyMeetsBlueprintTargetAcrossLabeledCorpora() {
        var total = 0
        var correct = 0

        FinancialIntelligenceV2FoundationCorpus
            .all
            .forEach { fixture ->
                val actual =
                    FinancialMessageRelevanceGate
                        .assess(
                            FinancialMessageNormalizer
                                .normalize(
                                    title =
                                        fixture.title,
                                    body =
                                        fixture.body
                                )
                        )
                        .isRelevant

                total += 1
                if (
                    actual ==
                    fixture.expectedRelevant
                ) {
                    correct += 1
                }
            }

        FinancialMessageFixtureCorpus
            .all
            .forEach { fixture ->
                val actual =
                    FinancialMessageRelevanceGate
                        .assess(
                            FinancialMessageNormalizer
                                .normalize(
                                    title =
                                        fixture.title,
                                    body =
                                        fixture.body
                                )
                        )
                        .isRelevant
                val expected =
                    fixture.coverage !=
                        FixtureCoverage
                            .NON_FINANCIAL

                total += 1
                if (actual == expected) {
                    correct += 1
                }
            }

        val accuracy =
            ratio(
                numerator = correct,
                denominator = total
            )

        assertTrue(
            "Brain v3 relevance accuracy " +
                percentage(accuracy) +
                " is below blueprint target " +
                percentage(
                    MIN_RELEVANCE_ACCEPTANCE
                ) +
                " (" +
                correct +
                "/" +
                total +
                " labeled fixtures correct)",
            accuracy >=
                MIN_RELEVANCE_ACCEPTANCE
        )
    }

    @Test
    fun declaredStructuredExtractionMeetsBlueprintTarget() {
        val router =
            MessagePackRouter(
                BundledKenyaMessagePackLoader
                    .load()
                    .templates
            )
        val fixtures =
            loadMessagePackFixtures()

        var expectedFieldCount = 0
        var correctFieldCount = 0

        fixtures.forEach { fixture ->
            val title =
                fixture.stringOrNull(
                    "title"
                )
            val body =
                fixture
                    .get("body")
                    .asString
            val match =
                router.route(
                    title = title,
                    body = body,
                    sourcePackage =
                        fixture.stringOrNull(
                            "sourcePackage"
                        ),
                    sourceKind =
                        fixture.stringOrNull(
                            "sourceKind"
                        )
                )

            val expectedFields =
                fixture.getAsJsonObject(
                    "expectedFields"
                )

            expectedFields
                .entrySet()
                .forEach {
                    (field, value) ->
                    expectedFieldCount += 1
                    if (
                        match?.fieldValue(field) ==
                        value.asString
                    ) {
                        correctFieldCount += 1
                    }
                }
        }

        val accuracy =
            ratio(
                numerator =
                    correctFieldCount,
                denominator =
                    expectedFieldCount
            )

        assertTrue(
            "Brain v3 structured extraction " +
                percentage(accuracy) +
                " is below blueprint target " +
                percentage(
                    MIN_EXTRACTION_ACCURACY
                ) +
                " (" +
                correctFieldCount +
                "/" +
                expectedFieldCount +
                " declared fields correct)",
            accuracy >=
                MIN_EXTRACTION_ACCURACY
        )
    }

    @Test
    fun everySupportedFixtureHasDeterministicExplainableProvenance() {
        loadMessagePackFixtures()
            .forEach { fixture ->
                val interpretation =
                    FinancialMessageInterpreter
                        .interpret(
                            title =
                                fixture
                                    .stringOrNull(
                                        "title"
                                    ),
                            body =
                                fixture
                                    .get("body")
                                    .asString,
                            sourcePackage =
                                fixture
                                    .stringOrNull(
                                        "sourcePackage"
                                    ),
                            sourceKind =
                                fixture
                                    .stringOrNull(
                                        "sourceKind"
                                    )
                        )

                assertTrue(
                    fixture.get("id")
                        .asString +
                        ": expected TEMPLATE provenance",
                    interpretation
                        .parseProvenance
                        .route ==
                        MessageParseRoute.TEMPLATE
                )
                assertTrue(
                    fixture.get("id")
                        .asString +
                        ": wrong template provenance",
                    interpretation
                        .parseProvenance
                        .templateId ==
                        fixture
                            .get("templateId")
                            .asString
                )
                assertTrue(
                    fixture.get("id")
                        .asString +
                        ": missing human-readable why",
                    interpretation
                        .parseProvenance
                        .reason
                        .isNotBlank()
                )
            }
    }

    @Test
    fun canonicalFinancialCorpusHasNoLegacyParserDependency() {
        assertTrue(
            "Canonical financial corpus still depends on legacy transaction parsing",
            FinancialMessageFixtureCorpus
                .all
                .none {
                    it.coverage ==
                        FixtureCoverage
                            .LEGACY_REQUIRED ||
                        it.coverage ==
                        FixtureCoverage
                            .GENERIC_SEMANTICS_LEGACY_PERSISTENCE
                }
        )
    }

    @Test
    fun nonFinancialCoreFixturesRemainRejected() {
        val nonFinancial =
            com.kedevjimmy.pesatrack
                .domain
                .interpretation
                .FinancialMessageFixtureCorpus
                .all
                .filter {
                    it.coverage ==
                        com.kedevjimmy.pesatrack
                            .domain
                            .interpretation
                            .FixtureCoverage
                            .NON_FINANCIAL
                }

        assertTrue(
            "Acceptance corpus needs at least one non-financial control",
            nonFinancial.isNotEmpty()
        )

        val correctlyRejected =
            nonFinancial.count {
                fixture ->
                !FinancialMessageRelevanceGate
                    .assess(
                        FinancialMessageNormalizer
                            .normalize(
                                title =
                                    fixture.title,
                                body =
                                    fixture.body
                            )
                    )
                    .isRelevant
            }

        assertTrue(
            "Non-financial relevance controls regressed (" +
                correctlyRejected +
                "/" +
                nonFinancial.size +
                " correctly rejected)",
            correctlyRejected ==
                nonFinancial.size
        )
    }

    private fun loadMessagePackFixtures():
        List<JsonObject> =
        FIXTURE_RESOURCES
            .flatMap { resource ->
                val stream =
                    javaClass
                        .classLoader
                        ?.getResourceAsStream(
                            resource
                        )
                        ?: error(
                            "Missing fixture resource: " +
                                resource
                        )

                stream.use { input ->
                    JsonParser
                        .parseReader(
                            InputStreamReader(
                                input,
                                Charsets.UTF_8
                            )
                        )
                        .asJsonArray
                        .map {
                            it.asJsonObject
                        }
                }
            }

    private fun JsonObject.stringOrNull(
        field: String
    ): String? {
        if (
            !has(field) ||
            get(field).isJsonNull
        ) {
            return null
        }

        return get(field)
            .asString
            .trim()
            .takeIf(
                String::isNotBlank
            )
    }

    private fun ratio(
        numerator: Int,
        denominator: Int
    ): Double =
        if (denominator <= 0) {
            0.0
        } else {
            numerator.toDouble() /
                denominator.toDouble()
        }

    private fun percentage(
        value: Double
    ): String =
        String.format(
            java.util.Locale.US,
            "%.1f%%",
            value * 100.0
        )

    companion object {
        private const val
            MIN_RELEVANCE_ACCEPTANCE =
            0.95

        private const val
            MIN_EXTRACTION_ACCURACY =
            0.90

        private val FIXTURE_RESOURCES =
            (1..14).map {
                "message-packs/ke/fixtures-v$it.json"
            }
    }
}
