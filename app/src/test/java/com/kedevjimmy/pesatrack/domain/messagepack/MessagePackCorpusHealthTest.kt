package com.kedevjimmy.pesatrack.domain.messagepack

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader

class MessagePackCorpusHealthTest {
    @Test
    fun everyActiveTemplateHasAtLeastOneGoldenFixture() {
        val templates =
            BundledKenyaMessagePackLoader
                .load()
                .templates
        val fixtureCounts =
            fixtureTemplateCounts()

        val missing =
            templates
                .map { it.id }
                .filter {
                    fixtureCounts
                        .getOrDefault(
                            it,
                            0
                        ) == 0
                }

        assertTrue(
            "Active templates missing golden fixtures: " +
                missing.joinToString(),
            missing.isEmpty()
        )
    }

    @Test
    fun fixtureIdsAreUniqueAndReferenceActiveTemplates() {
        val templates =
            BundledKenyaMessagePackLoader
                .load()
                .templates
                .map { it.id }
                .toSet()
        val fixtures =
            loadFixtures()

        assertEquals(
            fixtures.size,
            fixtures
                .map { it.id }
                .distinct()
                .size
        )

        val unknownTemplates =
            fixtures
                .map {
                    it.templateId
                }
                .filterNot(
                    templates::contains
                )
                .distinct()

        assertTrue(
            "Fixtures reference inactive or unknown templates: " +
                unknownTemplates
                    .joinToString(),
            unknownTemplates.isEmpty()
        )
    }

    @Test
    fun coreAmbiguityShapesHaveAtLeastThreeGoldenFixtures() {
        val counts =
            fixtureTemplateCounts()

        val belowTarget =
            CORE_AMBIGUITY_TEMPLATES
                .mapNotNull {
                    templateId ->
                    val count =
                        counts
                            .getOrDefault(
                                templateId,
                                0
                            )
                    if (count >= 3) {
                        null
                    } else {
                        templateId to
                            count
                    }
                }

        assertTrue(
            "Core ambiguity templates below 3-fixture floor: " +
                belowTarget
                    .joinToString {
                        it.first +
                            "=" +
                            it.second
                    },
            belowTarget.isEmpty()
        )
    }

    private fun fixtureTemplateCounts():
        Map<String, Int> =
        loadFixtures()
            .groupingBy {
                it.templateId
            }
            .eachCount()

    private fun loadFixtures():
        List<FixtureReference> =
        FIXTURE_RESOURCES
            .flatMap { resource ->
                val stream =
                    javaClass.classLoader
                        ?.getResourceAsStream(
                            resource
                        )
                        ?: error(
                            "Missing fixture resource: " +
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
                    FixtureReference(
                        id =
                            obj.get("id")
                                .asString,
                        templateId =
                            obj.get(
                                "templateId"
                            ).asString
                    )
                }
            }

    private data class FixtureReference(
        val id: String,
        val templateId: String
    )

    companion object {
        private val FIXTURE_RESOURCES =
            (1..10).map {
                "message-packs/ke/fixtures-v$it.json"
            }

        private val CORE_AMBIGUITY_TEMPLATES =
            setOf(
                "ke.mpesa.wallet.send-money",
                "ke.mpesa.wallet.receive-money",
                "ke.mpesa.wallet.paybill",
                "ke.mshwari.savings.to-mpesa",
                "ke.mshwari.savings.from-mpesa",
                "ke.mshwari.loan.approved-to-mpesa",
                "ke.mshwari.loan.balance",
                "ke.fuliza.loan.drawdown",
                "ke.fuliza.loan.partial-repayment",
                "ke.fuliza.loan.full-repayment"
            )
    }
}
