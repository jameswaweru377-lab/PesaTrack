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
    fun everyActiveTemplateHasAtLeastThreeGoldenFixtures() {
        val templates =
            BundledKenyaMessagePackLoader
                .load()
                .templates
        val fixtureCounts =
            fixtureTemplateCounts()

        val belowTarget =
            templates
                .map { it.id }
                .mapNotNull {
                    templateId ->
                    val count =
                        fixtureCounts
                            .getOrDefault(
                                templateId,
                                0
                            )
                    if (count >= 3) {
                        null
                    } else {
                        templateId to count
                    }
                }

        assertTrue(
            "Active templates below 3-fixture floor: " +
                belowTarget.joinToString {
                    it.first + "=" + it.second
                },
            belowTarget.isEmpty()
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

    @Test
    fun supportedTierABankShapesHaveAtLeastThreeGoldenFixtures() {
        val counts =
            fixtureTemplateCounts()

        val belowTarget =
            TIER_A_BANK_TEMPLATES
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
            "Supported Tier-A bank templates below 3-fixture floor: " +
                belowTarget
                    .joinToString {
                        it.first +
                            "=" +
                            it.second
                    },
            belowTarget.isEmpty()
        )
    }

    @Test
    fun adjacentFinanceShapesHaveAtLeastThreeGoldenFixtures() {
        val counts =
            fixtureTemplateCounts()

        val belowTarget =
            ADJACENT_FINANCE_TEMPLATES
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
            "Adjacent-finance templates below 3-fixture floor: " +
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
            (1..14).map {
                "message-packs/ke/fixtures-v$it.json"
            }

        private val ADJACENT_FINANCE_TEMPLATES =
            setOf(
                "ke.mshwari.lock-savings.deposit",
                "ke.mshwari.lock-savings.release-to-mshwari",
                "ke.mshwari.savings.credit-from-mpesa",
                "ke.mshwari.loan.received-with-fee",
                "ke.etica.payment.account-payment",
                "ke.etica.investment.redemption",
                "ke.etica.investment.withdrawal-to-mpesa",
                "ke.sanlam.investment.fund-credit",
                "ke.sanlam.investment.top-up",
                "ke.branch.loan.disbursed-to-mpesa",
                "ke.branch.loan.payment-due",
                "ke.tala.loan.disbursed-to-mpesa",
                "ke.tala.loan.payment-due",
                "ke.timiza.loan.disbursed",
                "ke.zenka.loan.disbursed-to-mpesa",
                "ke.okash.loan.disbursed",
                "ke.hustler-fund.loan.disbursed-with-savings"
            )

        private val TIER_A_BANK_TEMPLATES =
            setOf(
                "ke.cooperative.account.credit",
                "ke.cooperative.account.debit",
                "ke.dtb.till-moja.merchant-receipt",
                "ke.dtb.till-moja.payer-payment",
                "ke.equity.account.credit",
                "ke.equity.transfer.credit-to-beneficiary",
                "ke.i-and-m.payment.paid-to",
                "ke.kcb.account.credit",
                "ke.kcb.account.salary-credit",
                "ke.kcb.app.received",
                "ke.kcb.card.purchase",
                "ke.kcb.mpesa.received",
                "ke.kcb.mpesa.sent",
                "ke.kcb.payment.paid-to",
                "ke.kcb.transfer.send-to-mpesa",
                "ke.kcb.transfer.send-to-pesalink",
                "ke.standard-chartered.card.purchase"
            )

        private val CORE_AMBIGUITY_TEMPLATES =
            setOf(
                "ke.mpesa.wallet.send-money",
                "ke.mpesa.wallet.receive-money",
                "ke.mpesa.wallet.paybill",
                "ke.mshwari.savings.to-mpesa",
                "ke.mshwari.savings.from-mpesa",
                "ke.mshwari.loan.approved-to-mpesa",
                "ke.mshwari.loan.balance",
                "ke.mshwari.loan.repaid-from-mpesa",
                "ke.fuliza.loan.drawdown",
                "ke.fuliza.loan.partial-repayment",
                "ke.fuliza.loan.full-repayment"
            )
    }
}
