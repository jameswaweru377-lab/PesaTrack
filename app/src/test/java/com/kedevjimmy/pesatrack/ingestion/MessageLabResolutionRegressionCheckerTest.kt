package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.messagepack.MessageParseRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageLabResolutionRegressionCheckerTest {
    @Test
    fun syntheticReplayUsesSafeDeterministicValues() {
        val replay =
            MessageLabResolutionRegressionChecker
                .syntheticReplayBody(
                    "<TX_CODE> paid Ksh <AMOUNT> on <DATE> at <TIME> to <ENTITY>."
                )

        assertEquals(
            "TSTREG0001 paid Ksh 123.45 on 2/10/26 at 7:33 AM to SAMPLE STORE.",
            replay
        )
        assertFalse(
            replay.contains("<")
        )
    }

    @Test
    fun archivedMshwariRepaymentStillMatchesCurrentTemplate() {
        val check =
            MessageLabResolutionRegressionChecker
                .check(
                    resolved(
                        sender = "MPESA",
                        body =
                            "<TX_CODE> Confirmed. Loan of Ksh <AMOUNT> repaid from M-PESA on <DATE> at <TIME>. Loan balance is Ksh <AMOUNT>. Transaction cost Kshs <AMOUNT>",
                        route =
                            "TEMPLATE",
                        templateId =
                            "ke.mshwari.loan.repaid-from-mpesa"
                    )
                )

        assertEquals(
            ResolvedCoverageCheckState
                .STILL_COVERED,
            check.state
        )
        assertEquals(
            "ke.mshwari.loan.repaid-from-mpesa",
            check.currentTemplateId
        )
    }

    @Test
    fun relevanceRetirementCanRemainRetired() {
        val check =
            MessageLabResolutionRegressionChecker
                .check(
                    resolved(
                        body =
                            "Loan services are now available in the app.",
                        resolutionKind =
                            MessageLabResolutionKind
                                .NO_LONGER_UNSUPPORTED,
                        route = null,
                        templateId = null
                    )
                )

        assertEquals(
            ResolvedCoverageCheckState
                .STILL_RETIRED,
            check.state
        )
        assertFalse(
            check.currentWouldEnterMessageLab
        )
    }

    @Test
    fun parserCoverageReturningToMessageLabIsPossibleRegression() {
        val check =
            MessageLabResolutionRegressionChecker
                .classify(
                    record =
                        resolved(),
                    inspection =
                        inspection(
                            recognized = false,
                            understood = false,
                            wouldEnter = true
                        )
                )

        assertEquals(
            ResolvedCoverageCheckState
                .POSSIBLE_REGRESSION,
            check.state
        )
        assertTrue(
            check.currentWouldEnterMessageLab
        )
    }

    @Test
    fun stillUnderstoodThroughAnotherRouteIsNotRegression() {
        val check =
            MessageLabResolutionRegressionChecker
                .classify(
                    record =
                        resolved(
                            route = "TEMPLATE",
                            templateId =
                                "ke.old.template"
                        ),
                    inspection =
                        inspection(
                            recognized = true,
                            understood = false,
                            wouldEnter = false,
                            route =
                                MessageParseRoute
                                    .GENERIC_FALLBACK
                        )
                )

        assertEquals(
            ResolvedCoverageCheckState
                .COVERED_DIFFERENT_ROUTE,
            check.state
        )
    }

    private fun resolved(
        sender: String = "BANK",
        body: String =
            "KES <AMOUNT> unknown financial wording.",
        resolutionKind:
            MessageLabResolutionKind =
            MessageLabResolutionKind
                .PARSER_COVERAGE,
        route: String? = "TEMPLATE",
        templateId: String? =
            "ke.example.template"
    ) =
        ResolvedMessageLabShape(
            shapeId = "shape",
            familyId = "family",
            sender = sender,
            sourcePackage = null,
            sourceKind =
                "historical_financial_message",
            redactedBody = body,
            occurrenceCount = 2,
            familyOccurrenceCount = 2,
            familyVariantCount = 1,
            previousUserDecision =
                MessageLabUserDecision
                    .CONFIRMED_FINANCIAL,
            semanticLabel =
                MessageLabSemanticLabel
                    .PAYMENT,
            implementationReview =
                null,
            resolutionKind =
                resolutionKind,
            resolutionAgreement =
                MessageLabResolutionAgreement
                    .UNKNOWN,
            resolvedSemanticLabels =
                emptySet(),
            parseRoute =
                route,
            templateId =
                templateId,
            templateRevision = 1,
            eventKind = "PAYMENT",
            productIdentity = null,
            provenanceReason = "test",
            resolvedAtEpochMs = 1L
        )

    private fun inspection(
        recognized: Boolean,
        understood: Boolean,
        wouldEnter: Boolean,
        route:
            MessageParseRoute =
            MessageParseRoute
                .GENERIC_FALLBACK
    ) =
        FinancialMessageInspection(
            recognized = recognized,
            semanticallyUnderstood =
                understood,
            relevanceScore = 5,
            relevanceSignals =
                emptySet(),
            parseRoute = route,
            templateId = null,
            templateRevision = null,
            provenanceReason = "test",
            transactionCode = null,
            occurredAtText = null,
            eventSummaries =
                emptyList(),
            balanceSummaries =
                emptyList(),
            semanticIntents =
                emptyList(),
            extractedFieldNames =
                emptyList(),
            heuristicUnsupported =
                wouldEnter,
            wouldEnterMessageLab =
                wouldEnter,
            redactedSender =
                "<SENDER>",
            redactedBody =
                "KES <AMOUNT> test"
        )
}
