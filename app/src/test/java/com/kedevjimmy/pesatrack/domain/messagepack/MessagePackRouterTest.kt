package com.kedevjimmy.pesatrack.domain.messagepack

import com.google.gson.JsonParser
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpreter
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentEventFact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader

class MessagePackRouterTest {
    @Test
    fun bundledKenyaPackLoadsCleanly() {
        val result =
            BundledKenyaMessagePackLoader
                .load()

        assertTrue(
            result.errors.joinToString(
                separator = "\n"
            ),
            result.errors.isEmpty()
        )
        assertEquals(
            45,
            result.templates.size
        )
        assertEquals(
            result.templates.size,
            result.templates
                .map { it.id }
                .distinct()
                .size
        )
    }

    @Test
    fun sanitizedGoldenFixturesRouteAndExtract() {
        val router =
            MessagePackRouter(
                BundledKenyaMessagePackLoader
                    .load()
                    .templates
            )

        val fixtures =
            loadFixtures()

        fixtures.forEach { item ->
            val fixture =
                item.asJsonObject
            val id =
                fixture.get("id").asString
            val match =
                router.route(
                    title =
                        fixture.get(
                            "title"
                        ).asString,
                    body =
                        fixture.get(
                            "body"
                        ).asString
                )

            assertNotNull(
                "No Message Pack match for $id",
                match
            )
            assertEquals(
                id,
                fixture.get(
                    "templateId"
                ).asString,
                match?.templateId
            )

            val expected =
                fixture.getAsJsonObject(
                    "expectedFields"
                )
            expected.entrySet()
                .forEach {
                    (field, value) ->
                    assertEquals(
                        "$id field $field",
                        value.asString,
                        match?.fieldValue(field)
                    )
                }
        }
    }


    @Test
    fun everyActiveTemplateHasAtLeastOneSanitizedFixture() {
        val templateIds =
            BundledKenyaMessagePackLoader
                .load()
                .templates
                .map { it.id }
                .toSet()
        val fixtureTemplateIds =
            loadFixtures()
                .map {
                    it.asJsonObject
                        .get("templateId")
                        .asString
                }
                .toSet()

        assertEquals(
            templateIds,
            fixtureTemplateIds
        )
    }

    @Test
    fun corpusAlreadyHasMultipleExamplesForCoreShapes() {
        val counts =
            loadFixtures()
                .map {
                    it.asJsonObject
                        .get("templateId")
                        .asString
                }
                .groupingBy { it }
                .eachCount()

        assertTrue(
            counts.values.count {
                it >= 2
            } >= 5
        )
    }


    @Test
    fun phase6InvestmentShapesRecordProvenanceWithoutForcingOrdinaryLedgerMeaning() {
        val sanlam =
            FinancialMessageInterpreter
                .interpret(
                    title = "SANLAM_UT",
                    body =
                        "Dear SAMPLE PERSON, you have successfully topped up your account with KES. 2000.00, your account will be updated soon, Thank You."
                )

        assertEquals(
            MessageParseRoute.TEMPLATE,
            sanlam.parseProvenance.route
        )
        assertEquals(
            "ke.sanlam.investment.top-up",
            sanlam.parseProvenance.templateId
        )
        assertEquals(
            "INVESTMENT_TOP_UP",
            sanlam.parseProvenance.eventKind
        )
        assertEquals(
            "KENYAN_INVESTMENT_SANLAM",
            sanlam.parseProvenance.productIdentity
        )

        val etica =
            FinancialMessageInterpreter
                .interpret(
                    title = "ETICALTD",
                    body =
                        "You have sent KES 111,111.00 from your account 11111MC in EMMF - KES to Mr. SAMPLE PERSON (11111F)"
                )

        assertEquals(
            MessageParseRoute.TEMPLATE,
            etica.parseProvenance.route
        )
        assertEquals(
            "ke.etica.investment.redemption",
            etica.parseProvenance.templateId
        )
        assertEquals(
            "INVESTMENT_WITHDRAWAL",
            etica.parseProvenance.eventKind
        )
    }

    @Test
    fun phase6PaymentShapesExposeStructuredPaymentProvenance() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "EVOPAY",
                    body =
                        "Dear customer, you have paid parking fee of KES 200 for LPN KXX000A. You have 15 min to exit. Thank you for visiting SAMPLE CENTRE."
                )

        assertEquals(
            MessageParseRoute.TEMPLATE,
            interpretation.parseProvenance.route
        )
        assertEquals(
            "ke.evopay.parking.lpn-payment",
            interpretation.parseProvenance.templateId
        )
        assertEquals(
            "PAYMENT",
            interpretation.parseProvenance.eventKind
        )
        assertEquals(
            "200",
            interpretation.parseProvenance
                .extractedFields
                .first {
                    it.field == "AMOUNT"
                }
                .value
        )
        assertEquals(
            "SAMPLE CENTRE",
            interpretation.parseProvenance
                .extractedFields
                .first {
                    it.field == "COUNTERPARTY"
                }
                .value
        )
    }

    @Test
    fun equityTemplateAddsProvenanceWhileV2StillOwnsTransferMeaning() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "EQUITY BANK",
                    body =
                        "Your transaction of Kenya Shillings 20,000.00 has been credited to SAMPLE BENEFICIARY. Equity Ref EQA12345. M-Pesa Ref UFT12345."
                )

        assertTrue(
            interpretation.events
                .single() is
                PaymentEventFact
        )
        assertEquals(
            MessageParseRoute.TEMPLATE,
            interpretation
                .parseProvenance
                .route
        )
        assertEquals(
            "ke.equity.transfer.credit-to-beneficiary",
            interpretation
                .parseProvenance
                .templateId
        )
        assertEquals(
            "KENYAN_BANK_EQUITY",
            interpretation
                .parseProvenance
                .productIdentity
        )
    }

    @Test
    fun knownShapeRecordsTemplateProvenanceWithoutReplacingV2() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "MPESA",
                    body =
                        "TST12ABC04 Confirmed. Ksh 450.00 sent to SAMPLE PERSON 0712000000 on 28/9/26 at 7:10 PM. New M-PESA balance is Ksh 7,650.00. Transaction cost, Ksh 7.00."
                )

        assertTrue(
            interpretation.recognized
        )
        assertEquals(
            MessageParseRoute.TEMPLATE,
            interpretation
                .parseProvenance
                .route
        )
        assertEquals(
            "ke.mpesa.wallet.send-money",
            interpretation
                .parseProvenance
                .templateId
        )
        assertEquals(
            1,
            interpretation
                .parseProvenance
                .templateRevision
        )
    }

    @Test
    fun unknownShapeKeepsFinancialIntelligenceV2AsFallback() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "Future Finance",
                    body =
                        "Your loan of KES 4,607 has been disbursed to your Mobile Wallet account."
                )

        assertTrue(
            interpretation.recognized
        )
        assertEquals(
            MessageParseRoute
                .GENERIC_FALLBACK,
            interpretation
                .parseProvenance
                .route
        )
    }

    private fun loadFixtures() =
        listOf(
            "message-packs/ke/fixtures-v1.json",
            "message-packs/ke/fixtures-v2.json",
            "message-packs/ke/fixtures-v3.json",
            "message-packs/ke/fixtures-v4.json",
            "message-packs/ke/fixtures-v5.json",
            "message-packs/ke/fixtures-v6.json"
        ).flatMap { path ->
            val stream =
                javaClass.classLoader
                    ?.getResourceAsStream(
                        path
                    )
            assertNotNull(
                "Missing fixture resource $path",
                stream
            )

            InputStreamReader(
                stream!!,
                Charsets.UTF_8
            ).use {
                JsonParser
                    .parseReader(it)
                    .asJsonArray
                    .toList()
            }
        }

}
