package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.messagepack.MessageParseProvenance
import com.kedevjimmy.pesatrack.domain.messagepack.MessageParseRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessageLabResolutionPolicyTest {
    @Test
    fun recognizedTemplateResolutionKeepsTemplateProvenance() {
        val context =
            MessageLabResolutionPolicy
                .contextFor(
                    recognized = true,
                    semanticallyUnderstood =
                        false,
                    provenance =
                        MessageParseProvenance(
                            route =
                                MessageParseRoute
                                    .TEMPLATE,
                            templateId =
                                "ke.example.payment",
                            templateRevision = 3,
                            eventKind =
                                "PAYMENT",
                            productIdentity =
                                "EXAMPLE_ACCOUNT",
                            reason =
                                "Matched reviewed template"
                        )
                )

        assertEquals(
            MessageLabResolutionKind
                .PARSER_COVERAGE,
            context.kind
        )
        assertEquals(
            "TEMPLATE",
            context.parseRoute
        )
        assertEquals(
            "ke.example.payment",
            context.templateId
        )
        assertEquals(
            3,
            context.templateRevision
        )
        assertEquals(
            "PAYMENT",
            context.eventKind
        )
    }

    @Test
    fun semanticGenericResolutionIsStillParserCoverage() {
        val context =
            MessageLabResolutionPolicy
                .contextFor(
                    recognized = false,
                    semanticallyUnderstood =
                        true,
                    provenance =
                        MessageParseProvenance
                            .genericFallback()
                )

        assertEquals(
            MessageLabResolutionKind
                .PARSER_COVERAGE,
            context.kind
        )
        assertEquals(
            "GENERIC_FALLBACK",
            context.parseRoute
        )
        assertNull(
            context.templateId
        )
    }

    @Test
    fun relevanceRetirementIsNotMisreportedAsParserCoverage() {
        val context =
            MessageLabResolutionPolicy
                .contextFor(
                    recognized = false,
                    semanticallyUnderstood =
                        false,
                    provenance =
                        MessageParseProvenance
                            .genericFallback()
                )

        assertEquals(
            MessageLabResolutionKind
                .NO_LONGER_UNSUPPORTED,
            context.kind
        )
        assertNull(
            context.parseRoute
        )
        assertNull(
            context.templateId
        )
    }

    @Test
    fun resolvedRecordPreservesSanitizedEvidenceAndReviewState() {
        val shape =
            UnsupportedMessageShape(
                shapeId = "shape",
                familyId = "family",
                sender = "MPESA",
                sourcePackage = null,
                sourceKind =
                    "historical_financial_message",
                redactedBody =
                    "<TX_CODE> Confirmed. Loan of Ksh <AMOUNT> repaid.",
                firstSeenAtEpochMs = 1L,
                lastSeenAtEpochMs = 2L,
                occurrenceCount = 2,
                userDecision =
                    MessageLabUserDecision
                        .CONFIRMED_FINANCIAL,
                semanticLabel =
                    MessageLabSemanticLabel
                        .LOAN_REPAYMENT,
                familyOccurrenceCount = 5,
                familyVariantCount = 3,
                implementationReview =
                    MessageLabImplementationReviewState
                        .READY_FOR_PARSER_WORK
            )
        val context =
            MessageLabResolutionContext(
                kind =
                    MessageLabResolutionKind
                        .PARSER_COVERAGE,
                parseRoute = "TEMPLATE",
                templateId =
                    "ke.mshwari.loan.repaid-from-mpesa",
                templateRevision = 1,
                eventKind =
                    "DEBT_REPAYMENT"
            )

        val record =
            MessageLabResolutionPolicy
                .resolvedRecord(
                    shape = shape,
                    context = context,
                    resolvedAtEpochMs = 99L
                )

        assertEquals(
            "family",
            record.familyId
        )
        assertEquals(
            5,
            record.familyOccurrenceCount
        )
        assertEquals(
            3,
            record.familyVariantCount
        )
        assertEquals(
            MessageLabSemanticLabel
                .LOAN_REPAYMENT,
            record.semanticLabel
        )
        assertEquals(
            MessageLabImplementationReviewState
                .READY_FOR_PARSER_WORK,
            record.implementationReview
        )
        assertEquals(
            "ke.mshwari.loan.repaid-from-mpesa",
            record.templateId
        )
        assertEquals(
            99L,
            record.resolvedAtEpochMs
        )
    }
}
