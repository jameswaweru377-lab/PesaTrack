package com.kedevjimmy.pesatrack.domain.messagepack

import com.google.gson.JsonParser
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialIntelligenceV2FoundationCorpus
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageFixtureCorpus
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpreter
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageNormalizer
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageRelevanceGate
import com.kedevjimmy.pesatrack.domain.interpretation.FixtureCoverage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader

class Phase6ReadinessGateTest {
    @Test
    fun relevanceAccuracyMeetsBlueprintGate() {
        var total = 0
        var correct = 0

        FinancialIntelligenceV2FoundationCorpus
            .all
            .forEach { fixture ->
                val normalized =
                    FinancialMessageNormalizer
                        .normalize(
                            title =
                                fixture.title,
                            body =
                                fixture.body
                        )
                val actual =
                    FinancialMessageRelevanceGate
                        .assess(normalized)
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
                val normalized =
                    FinancialMessageNormalizer
                        .normalize(
                            title =
                                fixture.title,
                            body =
                                fixture.body
                        )
                val actual =
                    FinancialMessageRelevanceGate
                        .assess(normalized)
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

        assertPercentAtLeast(
            label =
                "financial relevance accuracy",
            correct = correct,
            total = total,
            minimumPercent =
                MIN_RELEVANCE_PERCENT
        )
    }

    @Test
    fun supportedFieldExtractionMeetsBlueprintGate() {
        val fixtures =
            loadMessagePackFixtures()
        var totalFields = 0
        var correctFields = 0

        fixtures.forEach { fixture ->
            val match =
                KenyaMessagePack.route(
                    title = fixture.title,
                    body = fixture.body,
                    sourcePackage =
                        fixture.sourcePackage,
                    sourceKind =
                        fixture.sourceKind
                )

            totalFields +=
                fixture.expectedFields.size

            if (match != null) {
                fixture.expectedFields
                    .forEach {
                        (field, expected) ->
                        if (
                            match.fieldValue(
                                field
                            ) == expected
                        ) {
                            correctFields += 1
                        }
                    }
            }
        }

        assertPercentAtLeast(
            label =
                "supported field extraction accuracy",
            correct = correctFields,
            total = totalFields,
            minimumPercent =
                MIN_EXTRACTION_PERCENT
        )
    }

    @Test
    fun everyActiveTemplateMeetsThreeFixtureFloor() {
        val templates =
            BundledKenyaMessagePackLoader
                .load()
                .templates
        val counts =
            loadMessagePackFixtures()
                .groupingBy {
                    it.templateId
                }
                .eachCount()

        val belowFloor =
            templates
                .mapNotNull {
                    template ->
                    val count =
                        counts.getOrDefault(
                            template.id,
                            0
                        )
                    if (
                        count >=
                        MIN_FIXTURES_PER_TEMPLATE
                    ) {
                        null
                    } else {
                        template.id to count
                    }
                }

        assertTrue(
            "Phase 6 fixture floor failed: " +
                belowFloor.joinToString {
                    it.first +
                        "=" +
                        it.second
                },
            belowFloor.isEmpty()
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
                                fixture.title,
                            body =
                                fixture.body,
                            sourcePackage =
                                fixture
                                    .sourcePackage,
                            sourceKind =
                                fixture
                                    .sourceKind
                        )

                assertEquals(
                    fixture.id +
                        ": parse route",
                    MessageParseRoute.TEMPLATE,
                    interpretation
                        .parseProvenance
                        .route
                )
                assertEquals(
                    fixture.id +
                        ": template provenance",
                    fixture.templateId,
                    interpretation
                        .parseProvenance
                        .templateId
                )
                assertTrue(
                    fixture.id +
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
        assertFalse(
            FinancialMessageFixtureCorpus
                .all
                .any {
                    it.coverage ==
                        FixtureCoverage
                            .LEGACY_REQUIRED ||
                        it.coverage ==
                        FixtureCoverage
                            .GENERIC_SEMANTICS_LEGACY_PERSISTENCE
                }
        )
    }

    private fun loadMessagePackFixtures():
        List<ReadinessFixture> =
        FIXTURE_RESOURCES
            .flatMap {
                resource ->
                val stream =
                    javaClass.classLoader
                        ?.getResourceAsStream(
                            resource
                        )
                        ?: error(
                            "Missing Phase 6 fixture resource: " +
                                resource
                        )
                val array =
                    stream.use {
                        input ->
                        JsonParser
                            .parseReader(
                                InputStreamReader(
                                    input
                                )
                            )
                            .asJsonArray
                    }

                array.map {
                    element ->
                    val obj =
                        element
                            .asJsonObject
                    val expected =
                        obj.getAsJsonObject(
                            "expectedFields"
                        )
                        .entrySet()
                        .associate {
                            entry ->
                            entry.key to
                                entry.value
                                    .asString
                        }

                    ReadinessFixture(
                        id =
                            obj.get("id")
                                .asString,
                        title =
                            obj.get("title")
                                ?.asString,
                        body =
                            obj.get("body")
                                .asString,
                        templateId =
                            obj.get(
                                "templateId"
                            ).asString,
                        sourcePackage =
                            obj.get(
                                "sourcePackage"
                            )
                                ?.takeUnless {
                                    it.isJsonNull
                                }
                                ?.asString,
                        sourceKind =
                            obj.get(
                                "sourceKind"
                            )
                                ?.takeUnless {
                                    it.isJsonNull
                                }
                                ?.asString,
                        expectedFields =
                            expected
                    )
                }
            }

    private fun assertPercentAtLeast(
        label: String,
        correct: Int,
        total: Int,
        minimumPercent: Int
    ) {
        assertTrue(
            label +
                " has no samples",
            total > 0
        )

        val percent =
            correct * 100.0 /
                total.toDouble()

        assertTrue(
            label +
                " was " +
                String.format(
                    "%.2f",
                    percent
                ) +
                "% (" +
                correct +
                "/" +
                total +
                "), below " +
                minimumPercent +
                "%",
            percent >=
                minimumPercent
        )
    }

    private data class ReadinessFixture(
        val id: String,
        val title: String?,
        val body: String,
        val templateId: String,
        val sourcePackage: String?,
        val sourceKind: String?,
        val expectedFields:
            Map<String, String>
    )

    companion object {
        private const val
            MIN_RELEVANCE_PERCENT = 95
        private const val
            MIN_EXTRACTION_PERCENT = 90
        private const val
            MIN_FIXTURES_PER_TEMPLATE = 3

        private val FIXTURE_RESOURCES =
            (1..13).map {
                "message-packs/ke/fixtures-v$it.json"
            }
    }
}
