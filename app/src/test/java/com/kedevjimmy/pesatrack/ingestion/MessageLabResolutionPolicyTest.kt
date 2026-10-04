package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpreter
import com.kedevjimmy.pesatrack.domain.messagepack.MessageParseProvenance
import com.kedevjimmy.pesatrack.domain.messagepack.MessageParseRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun realMshwariRepaymentInterpretationResolvesAsLoanRepayment() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "MPESA",
                    body =
                        "TSTV14M001 Confirmed. Loan of Ksh575.00 repaid from M-PESA on 2/10/26 at 7:33 AM. Loan balance is Ksh0.00. Transaction cost Kshs 0.00"
                )

        val context =
            MessageLabResolutionPolicy
                .contextFor(
                    interpretation
                )

        assertTrue(
            MessageLabSemanticLabel
                .LOAN_REPAYMENT in
                context.resolvedSemanticLabels
        )
        assertEquals(
            MessageLabResolutionKind
                .PARSER_COVERAGE,
            context.kind
        )
    }

    @Test
    fun conflictingParserMeaningDoesNotRetireUserConfirmedEvidence() {
        val shape =
            confirmedShape(
                MessageLabSemanticLabel
                    .LOAN_REPAYMENT
            )
        val context =
            MessageLabResolutionContext(
                kind =
                    MessageLabResolutionKind
                        .PARSER_COVERAGE,
                resolvedSemanticLabels =
                    setOf(
                        MessageLabSemanticLabel
                            .PAYMENT
                    )
            )

        assertFalse(
            MessageLabResolutionPolicy
                .canRetire(
                    shape = shape,
                    context = context
                )
        )
        assertEquals(
            MessageLabResolutionAgreement
                .DISAGREES,
            MessageLabResolutionPolicy
                .agreementFor(
                    previous =
                        shape.semanticLabel,
                    current =
                        context
                            .resolvedSemanticLabels
                )
        )
    }

    @Test
    fun matchingParserMeaningCanRetireUserConfirmedEvidence() {
        val shape =
            confirmedShape(
                MessageLabSemanticLabel
                    .LOAN_REPAYMENT
            )
        val context =
            MessageLabResolutionContext(
                kind =
                    MessageLabResolutionKind
                        .PARSER_COVERAGE,
                resolvedSemanticLabels =
                    setOf(
                        MessageLabSemanticLabel
                            .LOAN_REPAYMENT,
                        MessageLabSemanticLabel
                            .BALANCE_UPDATE
                    )
            )

        assertTrue(
            MessageLabResolutionPolicy
                .canRetire(
                    shape = shape,
                    context = context
                )
        )
        assertEquals(
            MessageLabResolutionAgreement
                .AGREES,
            MessageLabResolutionPolicy
                .agreementFor(
                    previous =
                        shape.semanticLabel,
                    current =
                        context
                            .resolvedSemanticLabels
                )
        )
    }

    @Test
    fun broadOrUncomparableMeaningDoesNotBlockRetirement() {
        val broad =
            confirmedShape(
                MessageLabSemanticLabel
                    .OTHER_FINANCIAL
            )
        val ambiguous =
            confirmedShape(
                MessageLabSemanticLabel
                    .TRANSFER
            )

        assertTrue(
            MessageLabResolutionPolicy
                .canRetire(
                    shape = broad,
                    context =
                        MessageLabResolutionContext(
                            kind =
                                MessageLabResolutionKind
                                    .PARSER_COVERAGE,
                            resolvedSemanticLabels =
                                setOf(
                                    MessageLabSemanticLabel
                                        .PAYMENT
                                )
                        )
                )
        )
        assertTrue(
            MessageLabResolutionPolicy
                .canRetire(
                    shape = ambiguous,
                    context =
                        MessageLabResolutionContext(
                            kind =
                                MessageLabResolutionKind
                                    .PARSER_COVERAGE
                        )
                )
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
                    "DEBT_REPAYMENT",
                resolvedSemanticLabels =
                    setOf(
                        MessageLabSemanticLabel
                            .LOAN_REPAYMENT
                    )
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
            MessageLabResolutionAgreement
                .AGREES,
            record.resolutionAgreement
        )
        assertEquals(
            setOf(
                MessageLabSemanticLabel
                    .LOAN_REPAYMENT
            ),
            record.resolvedSemanticLabels
        )
        assertEquals(
            99L,
            record.resolvedAtEpochMs
        )
    }

    private fun confirmedShape(
        semanticLabel:
            MessageLabSemanticLabel
    ) =
        UnsupportedMessageShape(
            shapeId = "shape",
            familyId = "family",
            sender = "Source",
            sourcePackage = null,
            sourceKind =
                "historical_financial_message",
            redactedBody =
                "<AMOUNT>",
            firstSeenAtEpochMs = 1L,
            lastSeenAtEpochMs = 2L,
            occurrenceCount = 2,
            userDecision =
                MessageLabUserDecision
                    .CONFIRMED_FINANCIAL,
            semanticLabel =
                semanticLabel,
            familyOccurrenceCount = 2,
            familyVariantCount = 1
        )
}
