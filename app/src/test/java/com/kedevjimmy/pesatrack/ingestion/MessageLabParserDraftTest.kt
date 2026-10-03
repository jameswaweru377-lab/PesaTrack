package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageLabParserDraftTest {
    @Test
    fun requiresEvidenceReadyShape() {
        val shape =
            shape(
                occurrences = 1
            )

        assertNull(
            MessageLabParserDraftGenerator
                .generate(shape)
        )
    }

    @Test
    fun mshwariRepaymentDraftProposesUsefulFields() {
        val shape =
            shape(
                body =
                    "<TX_CODE> Confirmed. Loan of Ksh <AMOUNT> repaid from M-PESA on <DATE> at <TIME>. Loan balance is Ksh <AMOUNT>. Transaction cost Kshs <AMOUNT>",
                semantic =
                    MessageLabSemanticLabel
                        .LOAN_REPAYMENT
            )

        val draft =
            MessageLabParserDraftGenerator
                .generate(shape)!!

        assertEquals(
            "DEBT_REPAYMENT",
            draft.suggestedEventKind
        )
        assertTrue(
            "Confirmed. Loan of Ksh" in
                draft.candidateAnchors
        )
        assertTrue(
            "repaid from M-PESA on" in
                draft.candidateAnchors
        )
        assertTrue(
            "Loan balance is Ksh" in
                draft.candidateAnchors
        )
        assertTrue(
            "Transaction cost Kshs" in
                draft.candidateAnchors
        )

        assertEquals(
            listOf(
                "TRANSACTION_CODE",
                "AMOUNT",
                "DATE",
                "TIME",
                "OUTSTANDING",
                "FEE"
            ),
            draft.fields.map {
                it.suggestedField
            }
        )
    }

    @Test
    fun transferEntityAfterToBecomesDestination() {
        val shape =
            shape(
                body =
                    "KES <AMOUNT> transferred to <ENTITY> on <DATE>.",
                semantic =
                    MessageLabSemanticLabel
                        .TRANSFER
            )

        val draft =
            MessageLabParserDraftGenerator
                .generate(shape)!!

        assertEquals(
            "TRANSFER",
            draft.suggestedEventKind
        )
        assertEquals(
            "DESTINATION",
            draft.fields
                .first {
                    it.placeholder ==
                        "<ENTITY>"
                }
                .suggestedField
        )
    }

    @Test
    fun generatedDraftStaysInactiveAndReviewOnly() {
        val draft =
            MessageLabParserDraftGenerator
                .generate(
                    shape()
                )!!
                .toJson()

        assertTrue(
            draft.contains(
                "\"active\": false"
            )
        )
        assertTrue(
            draft.contains(
                "\"requiresManualReview\": true"
            )
        )
        assertTrue(
            draft.contains(
                "Draft only"
            )
        )
        assertFalse(
            draft.contains(
                "\"extractors\""
            )
        )
    }

    @Test
    fun bundleContainsOnlyEvidenceReadyShapes() {
        val ready =
            shape(
                id = "ready-shape"
            )
        val notReady =
            shape(
                id = "not-ready",
                occurrences = 1
            )

        val bundle =
            MessageLabParserDraftGenerator
                .bundleJson(
                    listOf(
                        notReady,
                        ready
                    )
                )!!

        assertTrue(
            bundle.contains(
                "ready-shape"
            )
        )
        assertFalse(
            bundle.contains(
                "not-ready"
            )
        )
    }

    private fun shape(
        id: String = "abcdef1234567890",
        body: String =
            "KES <AMOUNT> paid to <ENTITY>.",
        semantic:
            MessageLabSemanticLabel =
            MessageLabSemanticLabel
                .PAYMENT,
        occurrences: Int = 2
    ) =
        UnsupportedMessageShape(
            shapeId = id,
            sender = "MPESA",
            sourcePackage = null,
            sourceKind =
                "historical_financial_message",
            redactedBody = body,
            firstSeenAtEpochMs = 1L,
            lastSeenAtEpochMs = 2L,
            occurrenceCount =
                occurrences,
            userDecision =
                MessageLabUserDecision
                    .CONFIRMED_FINANCIAL,
            semanticLabel =
                semantic
        )
}
