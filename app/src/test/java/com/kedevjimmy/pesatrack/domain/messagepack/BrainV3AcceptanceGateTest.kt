package com.kedevjimmy.pesatrack.domain.messagepack

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageNormalizer
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageRelevanceGate
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader

class BrainV3AcceptanceGateTest {
    @Test
    fun supportedFinancialFixtureRelevanceMeetsBlueprintTarget() {
        val router =
            MessagePackRouter(
                BundledKenyaMessagePackLoader
                    .load()
                    .templates
            )
        val fixtures =
            loadMessagePackFixtures()

        val correctlyAccepted =
            fixtures.count { fixture ->
                val title =
                    fixture
                        .stringOrNull(
                            "title"
                        )
                val body =
                    fixture
                        .get("body")
                        .asString
                val sourcePackage =
                    fixture.stringOrNull(
                        "sourcePackage"
                    )
                val sourceKind =
                    fixture.stringOrNull(
                        "sourceKind"
                    )

                val templateMatch =
                    router.route(
                        title = title,
                        body = body,
                        sourcePackage =
                            sourcePackage,
                        sourceKind =
                            sourceKind
                    ) != null

                val genericRelevance =
                    FinancialMessageRelevanceGate
                        .assess(
                            FinancialMessageNormalizer
                                .normalize(
                                    title = title,
                                    body = body
                                )
                        )
                        .isRelevant

                templateMatch ||
                    genericRelevance
            }

        val accuracy =
            ratio(
                numerator =
                    correctlyAccepted,
                denominator =
                    fixtures.size
            )

        assertTrue(
            "Brain v3 relevance acceptance " +
                percentage(accuracy) +
                " is below blueprint target " +
                percentage(
                    MIN_RELEVANCE_ACCEPTANCE
                ) +
                " (" +
                correctlyAccepted +
                "/" +
                fixtures.size +
                " supported financial fixtures accepted)",
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
            (1..13).map {
                "message-packs/ke/fixtures-v$it.json"
            }
    }
}
