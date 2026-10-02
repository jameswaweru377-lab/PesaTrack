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
            62,
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
    fun sourceAwareTemplateRequiresMatchingPackageAndChannel() {
        val template =
            MessageTemplateDefinition(
                id =
                    "ke.test.bank-app.debit",
                revision = 1,
                country = "KE",
                senderIds =
                    listOf(
                        "Sample Bank"
                    ),
                requiredAnchors =
                    listOf(
                        "debited with KES"
                    ),
                anyAnchors =
                    emptyList(),
                eventKind = "PAYMENT",
                productIdentity =
                    "TEST_BANK",
                active = true,
                extractors =
                    listOf(
                        MessagePackFieldExtractor(
                            field = "AMOUNT",
                            pattern =
                                "debited with KES[ ]+([0-9,]+(?:[.][0-9]{1,2})?)",
                            required = true
                        )
                    ),
                sourcePackages =
                    listOf(
                        "ke.example.bank"
                    ),
                sourceKinds =
                    listOf(
                        "financial_notification"
                    )
            )
        val router =
            MessagePackRouter(
                listOf(template)
            )
        val body =
            "Your account was debited with KES 1,250.00."

        assertEquals(
            null,
            router.route(
                title = "Sample Bank",
                body = body
            )
        )
        assertEquals(
            null,
            router.route(
                title = "Sample Bank",
                body = body,
                sourcePackage =
                    "ke.other.bank",
                sourceKind =
                    "financial_notification"
            )
        )

        val match =
            router.route(
                title = "Sample Bank",
                body = body,
                sourcePackage =
                    "KE.EXAMPLE.BANK",
                sourceKind =
                    " financial_notification "
            )

        assertNotNull(match)
        assertEquals(
            "ke.test.bank-app.debit",
            match?.templateId
        )
        assertEquals(
            "1,250.00",
            match?.fieldValue(
                "AMOUNT"
            )
        )
        assertTrue(
            match?.sourcePackageMatched ==
                true
        )
        assertTrue(
            match?.sourceKindMatched ==
                true
        )
    }

    @Test
    fun unconstrainedTemplatesRemainBackwardCompatible() {
        val template =
            MessageTemplateDefinition(
                id =
                    "ke.test.sms.credit",
                revision = 1,
                country = "KE",
                senderIds =
                    listOf("TESTBANK"),
                requiredAnchors =
                    listOf(
                        "credited with KES"
                    ),
                anyAnchors =
                    emptyList(),
                eventKind = "RECEIPT",
                productIdentity =
                    "TEST_BANK",
                active = true,
                extractors =
                    emptyList()
            )
        val router =
            MessagePackRouter(
                listOf(template)
            )

        val match =
            router.route(
                title = "TESTBANK",
                body =
                    "Your account was credited with KES 500.",
                sourcePackage =
                    "com.android.messaging",
                sourceKind =
                    "historical_financial_message"
            )

        assertNotNull(match)
        assertEquals(
            false,
            match?.sourcePackageMatched
        )
        assertEquals(
            false,
            match?.sourceKindMatched
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
    fun phase6BankAccountAlertsRecordStructuredProvenanceWithoutInventingCounterparties() {
        val cooperative =
            FinancialMessageInterpreter
                .interpret(
                    title = "CO-OP BANK",
                    body =
                        "Co-operative Bank A/C XXX789 CR KES 15,000.00 on 17/02/26. Bal KES 60,000.00. Ref: COOP123456"
                )

        assertEquals(
            MessageParseRoute.TEMPLATE,
            cooperative.parseProvenance.route
        )
        assertEquals(
            "ke.cooperative.account.credit",
            cooperative.parseProvenance.templateId
        )
        assertEquals(
            "ACCOUNT_CREDIT",
            cooperative.parseProvenance.eventKind
        )
        assertEquals(
            "15,000.00",
            cooperative.parseProvenance
                .extractedFields
                .first {
                    it.field == "AMOUNT"
                }
                .value
        )
        assertEquals(
            "60,000.00",
            cooperative.parseProvenance
                .extractedFields
                .first {
                    it.field == "BALANCE"
                }
                .value
        )

        val equity =
            FinancialMessageInterpreter
                .interpret(
                    title = "EQUITY BANK",
                    body =
                        "Your Equity a/c XXX1234 has been credited with KES 10,000.00 on 17/02/26. Bal: KES 50,000.00. Ref: FT26048RKH"
                )

        assertEquals(
            MessageParseRoute.TEMPLATE,
            equity.parseProvenance.route
        )
        assertEquals(
            "ke.equity.account.credit",
            equity.parseProvenance.templateId
        )
        assertEquals(
            "ACCOUNT_CREDIT",
            equity.parseProvenance.eventKind
        )
        assertEquals(
            "XXX1234",
            equity.parseProvenance
                .extractedFields
                .first {
                    it.field == "ACCOUNT_REFERENCE"
                }
                .value
        )
    }

    @Test
    fun phase6LenderDueNoticesStayProvenanceOnly() {
        val tala =
            FinancialMessageInterpreter
                .interpret(
                    title = "TALA",
                    body =
                        "Tala: Your loan payment of KES 3,300.00 is due on 31 Oct 2026. Pay via Paybill 851201, Acc: 0712345678."
                )

        assertEquals(
            MessageParseRoute.TEMPLATE,
            tala.parseProvenance.route
        )
        assertEquals(
            "ke.tala.loan.payment-due",
            tala.parseProvenance.templateId
        )
        assertEquals(
            "DEBT_DUE_NOTICE",
            tala.parseProvenance.eventKind
        )
        assertTrue(
            tala.events.none {
                it is PaymentEventFact
            }
        )

        val branch =
            FinancialMessageInterpreter
                .interpret(
                    title = "Branch",
                    body =
                        "Branch: Reminding you that your loan payment of KES 2,300.00 is due. Use Paybill 999999 to clear."
                )

        assertEquals(
            "ke.branch.loan.payment-due",
            branch.parseProvenance.templateId
        )
        assertEquals(
            "DEBT_DUE_NOTICE",
            branch.parseProvenance.eventKind
        )
        assertTrue(
            branch.events.none {
                it is PaymentEventFact
            }
        )
    }

    @Test
    fun phase6LenderDrawdownsExposeStructuredProvenance() {
        val mshwari =
            FinancialMessageInterpreter
                .interpret(
                    title = "M-Shwari",
                    body =
                        "M-Shwari: You have received a loan of KES 5,000.00. KES 150.00 facility fee charged. Amount sent to M-Pesa. Repay KES 5,150.00 by 31 Oct 2026."
                )

        assertEquals(
            MessageParseRoute.TEMPLATE,
            mshwari.parseProvenance.route
        )
        assertEquals(
            "ke.mshwari.loan.received-with-fee",
            mshwari.parseProvenance.templateId
        )
        assertEquals(
            "DEBT_DRAWDOWN",
            mshwari.parseProvenance.eventKind
        )
        assertEquals(
            "150.00",
            mshwari.parseProvenance
                .extractedFields
                .first {
                    it.field == "FEE"
                }
                .value
        )
        assertEquals(
            "5,150.00",
            mshwari.parseProvenance
                .extractedFields
                .first {
                    it.field ==
                        "REPAY_AMOUNT"
                }
                .value
        )

        val hustler =
            FinancialMessageInterpreter
                .interpret(
                    title = "HUSTLERFUND",
                    body =
                        "Hustler Fund: Your loan of Ksh 5,000.00 has been approved. Ksh 4,750.00 sent to M-Pesa, Ksh 250.00 to your savings account. Repay Ksh 5,000.00 by 31 Oct 2026."
                )

        assertEquals(
            "ke.hustler-fund.loan.disbursed-with-savings",
            hustler.parseProvenance.templateId
        )
        assertEquals(
            "DEBT_DRAWDOWN_WITH_SAVINGS",
            hustler.parseProvenance.eventKind
        )
        assertEquals(
            "4,750.00",
            hustler.parseProvenance
                .extractedFields
                .first {
                    it.field ==
                        "DISBURSED_AMOUNT"
                }
                .value
        )
        assertEquals(
            "250.00",
            hustler.parseProvenance
                .extractedFields
                .first {
                    it.field ==
                        "SAVINGS_AMOUNT"
                }
                .value
        )
    }

    @Test
    fun phase6SaccoShapesStayProvenanceFirst() {
        val withdrawal =
            FinancialMessageInterpreter
                .interpret(
                    title = "",
                    body =
                        "Dear Member,You have withdrawn Ksh. 800 from Account 0101-001-00*** .Thank you for using FUNDILIMA SACCO Mobile Banking Service."
                )

        assertEquals(
            MessageParseRoute.TEMPLATE,
            withdrawal.parseProvenance.route
        )
        assertEquals(
            "ke.fundilima-sacco.account.withdrawal",
            withdrawal.parseProvenance.templateId
        )
        assertEquals(
            "SACCO_WITHDRAWAL",
            withdrawal.parseProvenance.eventKind
        )

        val deposit =
            FinancialMessageInterpreter
                .interpret(
                    title = "",
                    body =
                        "Dear Member, you have deposited 1400.00 to A/C No. xxxxxx6291, of SAMPLE WELFARE GROUP on 2025/09/28 12:01:55. For help 0709000000"
                )

        assertEquals(
            MessageParseRoute.TEMPLATE,
            deposit.parseProvenance.route
        )
        assertEquals(
            "ke.generic-sacco.account.deposit",
            deposit.parseProvenance.templateId
        )
        assertEquals(
            "SACCO_DEPOSIT",
            deposit.parseProvenance.eventKind
        )
    }

    @Test
    fun phase6BankAppShapesExposeStructuredProvenance() {
        val mobileCredit =
            FinancialMessageInterpreter
                .interpret(
                    title = "",
                    body =
                        "Your transaction of Kshs. 6000.0 has been credited to 254710000000 SAMPLE PERSON. Ref. EQA7FA49D0DDCBF. MPESA Ref. UDJP31HD79 on 19/04/2026 at 20:59:50."
                )

        assertEquals(
            MessageParseRoute.TEMPLATE,
            mobileCredit.parseProvenance.route
        )
        assertEquals(
            "BANK_TO_MOBILE_TRANSFER",
            mobileCredit.parseProvenance.eventKind
        )
        assertEquals(
            "254710000000",
            mobileCredit.parseProvenance
                .extractedFields
                .first {
                    it.field == "DESTINATION"
                }
                .value
        )

        val billPayment =
            FinancialMessageInterpreter
                .interpret(
                    title = "",
                    body =
                        "Confirmed, Bill payment to SAMPLE COLLECTIONS of KES. 320.00 for account 101417 and Ref. UDIP31BYCB on 18-04-2026 at 15:57. Thank you."
                )

        assertEquals(
            MessageParseRoute.TEMPLATE,
            billPayment.parseProvenance.route
        )
        assertEquals(
            "ke.generic-bank.payment.bill-payment",
            billPayment.parseProvenance.templateId
        )
        assertEquals(
            "PAYMENT",
            billPayment.parseProvenance.eventKind
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
            "message-packs/ke/fixtures-v6.json",
            "message-packs/ke/fixtures-v7.json",
            "message-packs/ke/fixtures-v8.json",
            "message-packs/ke/fixtures-v9.json",
            "message-packs/ke/fixtures-v10.json"
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
